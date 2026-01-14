package com.tibbo.aggregate.mcp.tools.variable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.VariableDefinition;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.datatable.FieldFormat;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;
import com.tibbo.aggregate.mcp.util.DataTableConverter;

import java.util.List;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for getting a normalized description of a variable
 */
public class DescribeVariableTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_describe_variable";
    }

    @Override
    public String getDescription() {
        return "Get a normalized description of a context variable (format, records, fields with descriptions, recommended write tool). " +
               "CRITICAL for AI: Always call this tool before setting a variable to understand field descriptions and requirements. " +
               "This tool provides detailed field information including descriptions, types, nullable status, and default values.";
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

        ObjectNode variableName = instance.objectNode();
        variableName.put("type", "string");
        variableName.put("description", "Variable name");
        properties.set("variableName", variableName);

        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);

        schema.set("required", instance.arrayNode().add("path").add("variableName"));
        schema.set("properties", properties);
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("variableName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and variableName parameters are required"
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
            String variableName = params.get("variableName").asText();

            Context context = connection.executeWithTimeout(() -> {
                return connection.getContextManager().get(path);
            }, 60000);

            if (context == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Context not found: " + path
                );
            }

            // Try to find variable definition for metadata
            VariableDefinition targetDef = null;
            List<VariableDefinition> variables = connection.executeWithTimeout(() -> {
                return context.getVariableDefinitions();
            }, 60000);
            for (VariableDefinition vd : variables) {
                if (vd.getName().equals(variableName)) {
                    targetDef = vd;
                    break;
                }
            }

            if (targetDef == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Variable not found: " + variableName
                );
            }

            // Try to get current value to inspect format
            DataTable value = connection.executeWithTimeout(() -> {
                try {
                    com.tibbo.aggregate.common.context.CallerController caller =
                        context.getContextManager().getCallerController();
                    return context.getVariableClone(variableName, caller);
                } catch (ContextException e) {
                    // Variable may exist but have no value yet; ignore
                    return null;
                }
            }, 60000);

            TableFormat format = null;
            if (value != null) {
                format = value.getFormat();
            }

            ObjectNode result = instance.objectNode();
            result.put("path", path);
            result.put("name", variableName);

            if (targetDef.getDescription() != null) {
                result.put("description", targetDef.getDescription());
            }
            if (targetDef.getGroup() != null) {
                result.put("group", targetDef.getGroup());
            }
            result.put("readable", targetDef.isReadable());
            result.put("writable", targetDef.isWritable());

            if (format != null) {
                // Basic format info
                result.set("format", DataTableConverter.formatToJson(format));
                result.put("minRecords", format.getMinRecords());
                result.put("maxRecords", format.getMaxRecords());
                result.put("fieldCount", format.getFieldCount());

                // Fields summary with detailed information for AI
                ArrayNode fieldsArray = instance.arrayNode();
                for (int i = 0; i < format.getFieldCount(); i++) {
                    FieldFormat ff = format.getField(i);
                    ObjectNode f = instance.objectNode();
                    f.put("name", ff.getName());
                    f.put("type", String.valueOf(ff.getType()));
                    
                    // Type name for better AI understanding
                    String typeName = getTypeName(ff.getType());
                    f.put("typeName", typeName);
                    
                    // Description is CRITICAL for AI to understand field purpose
                    if (ff.getDescription() != null && !ff.getDescription().isEmpty()) {
                        f.put("description", ff.getDescription());
                    } else {
                        f.put("description", ""); // Empty string to indicate no description
                    }
                    
                    // Additional field properties
                    f.put("nullable", ff.isNullable());
                    try {
                        Object defaultValue = ff.getDefaultValue();
                        if (defaultValue != null) {
                            f.put("hasDefault", true);
                            f.put("defaultValue", defaultValue.toString());
                        } else {
                            f.put("hasDefault", false);
                        }
                    } catch (Exception e) {
                        // Ignore if default value cannot be retrieved
                        f.put("hasDefault", false);
                    }
                    
                    // Field format string for reference
                    f.put("formatString", ff.toString());
                    
                    fieldsArray.add(f);
                }
                result.set("fields", fieldsArray);
                
                // Add AI guidance for setting this variable
                ObjectNode aiGuidance = instance.objectNode();
                aiGuidance.put("note", "Before setting this variable, review all field descriptions to understand their purpose and requirements.");
                if (format.getFieldCount() > 0) {
                    ArrayNode requiredFields = instance.arrayNode();
                    ArrayNode optionalFields = instance.arrayNode();
                    for (int i = 0; i < format.getFieldCount(); i++) {
                        FieldFormat ff = format.getField(i);
                        try {
                            Object defaultValue = ff.getDefaultValue();
                            if (!ff.isNullable() && defaultValue == null) {
                                requiredFields.add(ff.getName());
                            } else {
                                optionalFields.add(ff.getName());
                            }
                        } catch (Exception e) {
                            // If we can't determine default, assume required if not nullable
                            if (!ff.isNullable()) {
                                requiredFields.add(ff.getName());
                            } else {
                                optionalFields.add(ff.getName());
                            }
                        }
                    }
                    if (requiredFields.size() > 0) {
                        aiGuidance.set("requiredFields", requiredFields);
                    }
                    if (optionalFields.size() > 0) {
                        aiGuidance.set("optionalFields", optionalFields);
                    }
                }
                result.set("aiGuidance", aiGuidance);

                boolean singleRecord = format.getMaxRecords() == 1 || format.isSingleRecord();
                boolean singleField = format.getFieldCount() == 1;

                result.put("isSingleRecord", singleRecord);
                result.put("isSingleField", singleField);
                result.put("isSimple", singleRecord && singleField);

                // Recommended write tool
                String recommendedWriteTool = singleRecord
                    ? "aggregate_set_variable_field"
                    : "aggregate_set_variable";
                result.put("recommendedWriteTool", recommendedWriteTool);

                // Also recommend smart wrapper
                result.put("recommendedSmartTool", "aggregate_set_variable_smart");
            } else {
                result.put("note", "Variable definition found but format could not be determined (no value yet?)");
            }

            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to describe variable: " + errorMessage
            );
        }
    }
    
    /**
     * Get human-readable type name for AI understanding
     */
    private String getTypeName(char type) {
        switch (type) {
            case FieldFormat.STRING_FIELD:
                return "String";
            case FieldFormat.INTEGER_FIELD:
                return "Integer";
            case FieldFormat.LONG_FIELD:
                return "Long";
            case FieldFormat.FLOAT_FIELD:
                return "Float";
            case FieldFormat.DOUBLE_FIELD:
                return "Number";
            case FieldFormat.BOOLEAN_FIELD:
                return "Boolean";
            case FieldFormat.DATATABLE_FIELD:
                return "DataTable";
            case FieldFormat.DATE_FIELD:
                return "Date";
            default:
                // Try to get type name from FieldFormat if available
                try {
                    return String.valueOf(type);
                } catch (Exception e) {
                    return "Unknown";
                }
        }
    }
}

