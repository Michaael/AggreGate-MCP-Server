package com.tibbo.aggregate.mcp.tools.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.FunctionDefinition;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.FieldFormat;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;
import com.tibbo.aggregate.mcp.util.DataTableConverter;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for getting detailed information about a function in a context
 */
public class GetFunctionTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_get_function";
    }

    @Override
    public String getDescription() {
        return "Get detailed definition of a function in a context, including formats and type";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();

        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Context path");
        properties.set("path", path);

        ObjectNode functionName = instance.objectNode();
        functionName.put("type", "string");
        functionName.put("description", "Function name");
        properties.set("functionName", functionName);

        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);

        schema.set("required", instance.arrayNode().add("path").add("functionName"));
        schema.set("properties", properties);
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("functionName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and functionName parameters are required"
            );
        }

        String connectionKey = params.has("connectionKey") ? params.get("connectionKey").asText() : null;
        ServerConnection connection = connectionKey != null
            ? connectionManager.getServerConnection(connectionKey)
            : connectionManager.getDefaultServerConnection();

        if (connection == null || !connection.isLoggedIn()) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONNECTION_ERROR,
                "Not connected or not logged in"
            );
        }

        try {
            String path = ContextPathParser.parsePath(params.get("path").asText());
            String functionName = params.get("functionName").asText();

            Context context = connection.executeWithTimeout(() -> {
                Context ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000);

            // Try to get definition from context
            FunctionDefinition def = connection.executeWithTimeout(() -> {
                return context.getFunctionDefinition(functionName);
            }, 60000);

            ObjectNode result = instance.objectNode();
            result.put("path", path);
            result.put("name", functionName);

            if (def != null) {
                result.put("description", def.getDescription() != null ? def.getDescription() : "");
                result.put("group", def.getGroup() != null ? def.getGroup() : "");

                // Try to get formats from FunctionDefinition first
                TableFormat inputFormat = def.getInputFormat();
                TableFormat outputFormat = def.getOutputFormat();
                
                // For model contexts, FunctionDefinition may not have the correct format
                // We'll try to get it from V_MODEL_FUNCTIONS below
                boolean useDefFormats = true;
                
                if (inputFormat != null && inputFormat.getFieldCount() > 0) {
                    result.set("inputFormat", DataTableConverter.formatToJson(inputFormat));
                    result.set("inputFields", buildFieldsArray(inputFormat));
                } else {
                    useDefFormats = false;
                }
                if (outputFormat != null && outputFormat.getFieldCount() > 0) {
                    result.set("outputFormat", DataTableConverter.formatToJson(outputFormat));
                    result.set("outputFields", buildFieldsArray(outputFormat));
                }

                // Try to detect implementation type (java/expression/query) from modelFunctions, if present
                int functionType = com.tibbo.aggregate.common.server.ModelContextConstants.FUNCTION_TYPE_JAVA;
                String expression = null;
                String query = null;
                boolean isModelContext = false;
                TableFormat modelInputFormat = null;
                TableFormat modelOutputFormat = null;

                try {
                    com.tibbo.aggregate.common.context.CallerController caller =
                        context.getContextManager().getCallerController();
                    DataTable modelFunctions =
                        context.getVariable(com.tibbo.aggregate.common.server.ModelContextConstants.V_MODEL_FUNCTIONS, caller);
                    isModelContext = true;

                    for (int i = 0; i < modelFunctions.getRecordCount(); i++) {
                        com.tibbo.aggregate.common.datatable.DataRecord rec = modelFunctions.getRecord(i);
                        String name = rec.getString(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_NAME);
                        if (functionName.equals(name)) {
                            functionType = rec.getInt(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_FD_TYPE);
                            
                            // Read inputFormat from V_MODEL_FUNCTIONS if available
                            if (rec.hasField(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_INPUTFORMAT)) {
                                try {
                                    DataTable inputFormatTable = rec.getDataTable(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_INPUTFORMAT);
                                    if (inputFormatTable != null) {
                                        System.err.println("[MCP] Reading inputFormat from V_MODEL_FUNCTIONS: recordCount=" + 
                                            inputFormatTable.getRecordCount() + ", formatFieldCount=" + 
                                            (inputFormatTable.getFormat() != null ? inputFormatTable.getFormat().getFieldCount() : 0));
                                        
                                        // DataTable created from formatToTable stores the format in the DataTable's format
                                        // The formatToTable method creates a DataTable where the format represents the original TableFormat structure
                                        // However, formatToTable may create a DataTable with a different structure
                                        // Try to use DataTableBuilding.tableToFormat if available, or use the DataTable's format directly
                                        
                                        // First, try to get format from DataTable itself
                                        // formatToTable creates a DataTable where the format represents the original TableFormat structure
                                        // The format of the DataTable itself should contain the field definitions
                                        TableFormat formatFromTable = inputFormatTable.getFormat();
                                        
                                        System.err.println("[MCP] inputFormatTable.getFormat() returned: " + 
                                            (formatFromTable != null ? formatFromTable.getFieldCount() + " fields" : "null"));
                                        
                                        // formatToFieldsTable creates a DataTable where each record represents a field definition
                                        // The format of the DataTable contains metadata, not the original fields
                                        // We need to extract fields from records
                                        if (inputFormatTable.getRecordCount() > 0) {
                                            // formatToFieldsTable stores each field as a record
                                            System.err.println("[MCP] Extracting fields from " + inputFormatTable.getRecordCount() + " records");
                                            
                                            // Get the format of records to understand structure
                                            TableFormat recordFormat = inputFormatTable.getFormat();
                                            if (recordFormat != null) {
                                                System.err.println("[MCP] Record format has " + recordFormat.getFieldCount() + " fields:");
                                                for (int k = 0; k < recordFormat.getFieldCount(); k++) {
                                                    FieldFormat rf = recordFormat.getField(k);
                                                    System.err.println("[MCP]   Field " + k + ": " + rf.getName() + " (" + rf.getType() + ")");
                                                }
                                            }
                                            
                                            // Build format from records
                                            TableFormat reconstructedFormat = new TableFormat(0, Integer.MAX_VALUE);
                                            for (int j = 0; j < inputFormatTable.getRecordCount(); j++) {
                                                com.tibbo.aggregate.common.datatable.DataRecord formatRec = inputFormatTable.getRecord(j);
                                                
                                                System.err.println("[MCP] Processing record " + j + ":");
                                                
                                                // formatToFieldsTable creates records where each record IS a field definition
                                                // But when saved to V_MODEL_FUNCTIONS, it may be wrapped in a metadata structure
                                                // Try to extract fields directly from the record
                                                String fieldName = null;
                                                String fieldType = null;
                                                String fieldDesc = null;
                                                
                                                // First, check if record has "fields" DataTable (wrapped format)
                                                if (formatRec.hasField("fields")) {
                                                    try {
                                                        com.tibbo.aggregate.common.datatable.DataTable fieldsTable = 
                                                            formatRec.getDataTable("fields");
                                                        if (fieldsTable != null && fieldsTable.getRecordCount() > 0) {
                                                            System.err.println("[MCP]   Found 'fields' DataTable with " + 
                                                                fieldsTable.getRecordCount() + " records");
                                                            
                                                            // Extract fields from the fields DataTable
                                                            for (int fieldIdx = 0; fieldIdx < fieldsTable.getRecordCount(); fieldIdx++) {
                                                                com.tibbo.aggregate.common.datatable.DataRecord fieldRec = 
                                                                    fieldsTable.getRecord(fieldIdx);
                                                                
                                                                fieldName = null;
                                                                fieldType = null;
                                                                fieldDesc = null;
                                                                
                                                                // Get field name
                                                                if (fieldRec.hasField("name")) {
                                                                    fieldName = fieldRec.getString("name");
                                                                }
                                                                
                                                                // Get field type
                                                                if (fieldRec.hasField("type")) {
                                                                    Object typeObj = fieldRec.getValue("type");
                                                                    if (typeObj != null) {
                                                                        fieldType = typeObj.toString();
                                                                        if (fieldType.length() > 1) {
                                                                            fieldType = String.valueOf(fieldType.charAt(0));
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                // Get field description
                                                                if (fieldRec.hasField("description")) {
                                                                    fieldDesc = fieldRec.getString("description");
                                                                }
                                                                
                                                                if (fieldName != null && fieldType != null) {
                                                                    String fieldDef = "<" + fieldName + "><" + fieldType + ">";
                                                                    if (fieldDesc != null && !fieldDesc.isEmpty()) {
                                                                        fieldDef += "<D=" + fieldDesc + ">";
                                                                    }
                                                                    reconstructedFormat.addField(fieldDef);
                                                                    System.err.println("[MCP] ✓ Extracted field " + fieldIdx + " from 'fields' table: " + fieldDef);
                                                                }
                                                            }
                                                        } else {
                                                            System.err.println("[MCP]   'fields' DataTable is empty or null, trying direct extraction");
                                                            // Fall through to direct extraction
                                                        }
                                                    } catch (Exception e) {
                                                        System.err.println("[MCP]   Error reading 'fields' DataTable: " + e.getMessage() + ", trying direct extraction");
                                                        // Fall through to direct extraction
                                                    }
                                                }
                                                
                                                // If we haven't extracted fields yet, try direct extraction from record
                                                // (formatToFieldsTable may store fields directly in records)
                                                if (reconstructedFormat.getFieldCount() == 0) {
                                                    System.err.println("[MCP]   Trying direct field extraction from record");
                                                    
                                                    // Try to get field information directly from record
                                                    if (recordFormat != null) {
                                                        for (int k = 0; k < recordFormat.getFieldCount(); k++) {
                                                            FieldFormat rf = recordFormat.getField(k);
                                                            String rfName = rf.getName();
                                                            String rfNameLower = rfName.toLowerCase();
                                                            
                                                            if (formatRec.hasField(rfName)) {
                                                                Object value = formatRec.getValue(rfName);
                                                                String valueStr = value != null ? value.toString() : null;
                                                                System.err.println("[MCP]   Record field '" + rfName + "' = " + valueStr);
                                                                
                                                                if (rfNameLower.equals("name") && fieldName == null) {
                                                                    fieldName = valueStr;
                                                                } else if (rfNameLower.equals("type") && fieldType == null) {
                                                                    fieldType = valueStr;
                                                                    if (fieldType != null && fieldType.length() > 1) {
                                                                        fieldType = String.valueOf(fieldType.charAt(0));
                                                                    }
                                                                } else if (rfNameLower.equals("description") && fieldDesc == null) {
                                                                    fieldDesc = valueStr;
                                                                }
                                                            }
                                                        }
                                                    }
                                                    
                                                    // Fallback: direct field access
                                                    if (fieldName == null && formatRec.hasField("name")) {
                                                        fieldName = formatRec.getString("name");
                                                    }
                                                    if (fieldType == null && formatRec.hasField("type")) {
                                                        Object typeObj = formatRec.getValue("type");
                                                        if (typeObj != null) {
                                                            fieldType = typeObj.toString();
                                                            if (fieldType.length() > 1) {
                                                                fieldType = String.valueOf(fieldType.charAt(0));
                                                            }
                                                        }
                                                    }
                                                    if (fieldDesc == null && formatRec.hasField("description")) {
                                                        fieldDesc = formatRec.getString("description");
                                                    }
                                                    
                                                    if (fieldName != null && fieldType != null) {
                                                        String fieldDef = "<" + fieldName + "><" + fieldType + ">";
                                                        if (fieldDesc != null && !fieldDesc.isEmpty()) {
                                                            fieldDef += "<D=" + fieldDesc + ">";
                                                        }
                                                        reconstructedFormat.addField(fieldDef);
                                                        System.err.println("[MCP] ✓ Extracted field directly: " + fieldDef);
                                                    }
                                                }
                                            }
                                            
                                            if (reconstructedFormat.getFieldCount() > 0) {
                                                modelInputFormat = reconstructedFormat;
                                                System.err.println("[MCP] ✓ Successfully extracted inputFormat: " + 
                                                    reconstructedFormat.getFieldCount() + " fields");
                                            } else {
                                                System.err.println("[MCP] ✗ Failed to extract inputFormat - no valid fields found");
                                            }
                                        } else {
                                            System.err.println("[MCP] inputFormatTable has no records");
                                        }
                                    } else {
                                        System.err.println("[MCP] inputFormatTable is null");
                                    }
                                } catch (Exception e) {
                                    System.err.println("[MCP] Failed to read inputFormat from V_MODEL_FUNCTIONS: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            } else {
                                System.err.println("[MCP] FIELD_FD_INPUTFORMAT field not found in V_MODEL_FUNCTIONS record");
                            }
                            
                            // Read outputFormat from V_MODEL_FUNCTIONS if available
                            if (rec.hasField(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_OUTPUTFORMAT)) {
                                try {
                                    DataTable outputFormatTable = rec.getDataTable(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_OUTPUTFORMAT);
                                    if (outputFormatTable != null) {
                                        TableFormat formatFromTable = outputFormatTable.getFormat();
                                        
                                        if (formatFromTable != null && formatFromTable.getFieldCount() > 0) {
                                            modelOutputFormat = formatFromTable;
                                            System.err.println("[MCP] Successfully read outputFormat from V_MODEL_FUNCTIONS: " + 
                                                formatFromTable.getFieldCount() + " fields");
                                        } else {
                                            // Try to reconstruct from DataTable structure
                                            if (outputFormatTable.getRecordCount() > 0) {
                                                TableFormat reconstructedFormat = new TableFormat(0, Integer.MAX_VALUE);
                                                for (int j = 0; j < outputFormatTable.getRecordCount(); j++) {
                                                    com.tibbo.aggregate.common.datatable.DataRecord formatRec = outputFormatTable.getRecord(j);
                                                    if (formatRec.hasField("name") && formatRec.hasField("type")) {
                                                        String fieldName = formatRec.getString("name");
                                                        String fieldType = formatRec.getString("type");
                                                        String fieldDesc = formatRec.hasField("description") ? formatRec.getString("description") : null;
                                                        String fieldDef = "<" + fieldName + "><" + fieldType + ">";
                                                        if (fieldDesc != null) {
                                                            fieldDef += "<D=" + fieldDesc + ">";
                                                        }
                                                        reconstructedFormat.addField(fieldDef);
                                                    }
                                                }
                                                if (reconstructedFormat.getFieldCount() > 0) {
                                                    modelOutputFormat = reconstructedFormat;
                                                    System.err.println("[MCP] Reconstructed outputFormat from V_MODEL_FUNCTIONS records: " + 
                                                        reconstructedFormat.getFieldCount() + " fields");
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    System.err.println("[MCP] Failed to read outputFormat from V_MODEL_FUNCTIONS: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                            
                            if (functionType == com.tibbo.aggregate.common.server.ModelContextConstants.FUNCTION_TYPE_EXPRESSION &&
                                rec.hasField(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_FD_EXPRESSION)) {
                                expression = rec.getString(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_FD_EXPRESSION);
                            }
                            if (functionType == com.tibbo.aggregate.common.server.ModelContextConstants.FUNCTION_TYPE_QUERY &&
                                rec.hasField(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_FD_QUERY)) {
                                query = rec.getString(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_FD_QUERY);
                            }
                            break;
                        }
                    }
                } catch (Exception ignore) {
                    // Not a model context or variable not available; we still have basic info
                }
                
                // For model contexts, use formats from V_MODEL_FUNCTIONS if FunctionDefinition doesn't have them
                if (isModelContext) {
                    if (modelInputFormat != null && modelInputFormat.getFieldCount() > 0) {
                        // Always use format from V_MODEL_FUNCTIONS for model contexts, as it's more reliable
                        inputFormat = modelInputFormat;
                        result.set("inputFormat", DataTableConverter.formatToJson(inputFormat));
                        result.set("inputFields", buildFieldsArray(inputFormat));
                        System.err.println("[MCP] Using inputFormat from V_MODEL_FUNCTIONS: " + inputFormat.getFieldCount() + " fields");
                    }
                    if (modelOutputFormat != null && modelOutputFormat.getFieldCount() > 0) {
                        outputFormat = modelOutputFormat;
                        result.set("outputFormat", DataTableConverter.formatToJson(outputFormat));
                        result.set("outputFields", buildFieldsArray(outputFormat));
                        System.err.println("[MCP] Using outputFormat from V_MODEL_FUNCTIONS: " + outputFormat.getFieldCount() + " fields");
                    }
                }

                result.put("isModelContextFunction", isModelContext);
                result.put("functionType", functionType);
                if (expression != null) {
                    result.put("expression", expression);
                }
                if (query != null) {
                    result.put("query", query);
                }

                // Simple recommendations for calling
                result.put("recommendedCallTool", "aggregate_call_function");
                result.put("hasImplementation",
                    def.getImplementation() != null ||
                    (functionType != com.tibbo.aggregate.common.server.ModelContextConstants.FUNCTION_TYPE_JAVA));
            } else {
                result.put("note", "Function definition not found in context");
            }

            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to get function: " + errorMessage
            );
        }
    }

    private ArrayNode buildFieldsArray(TableFormat format) {
        ArrayNode fields = instance.arrayNode();
        for (int i = 0; i < format.getFieldCount(); i++) {
            FieldFormat ff = format.getField(i);
            ObjectNode field = instance.objectNode();
            field.put("name", ff.getName());
            field.put("type", String.valueOf(ff.getType()));
            if (ff.getDescription() != null) {
                field.put("description", ff.getDescription());
            }
            fields.add(field);
        }
        return fields;
    }
}

