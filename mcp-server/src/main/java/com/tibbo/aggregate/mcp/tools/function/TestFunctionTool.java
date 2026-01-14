package com.tibbo.aggregate.mcp.tools.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.FunctionDefinition;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.datatable.SimpleDataTable;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;
import com.tibbo.aggregate.mcp.util.DataTableConverter;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for safely testing a function with sample parameters.
 * It returns both the result and additional diagnostics about formats and errors.
 */
public class TestFunctionTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_test_function";
    }

    @Override
    public String getDescription() {
        return "Safely test a function call with sample parameters and return diagnostics";
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

        ObjectNode parameters = instance.objectNode();
        parameters.put("type", "object");
        parameters.put("description", "Sample function parameters. Can be simple object or DataTable JSON.");
        properties.set("parameters", parameters);

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

        ObjectNode result = instance.objectNode();

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

            FunctionDefinition def = connection.executeWithTimeout(() -> {
                return context.getFunctionDefinition(functionName);
            }, 60000);

            if (def == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Function not found: " + functionName
                );
            }

            result.put("path", path);
            result.put("name", functionName);
            result.put("description", def.getDescription() != null ? def.getDescription() : "");
            result.put("group", def.getGroup() != null ? def.getGroup() : "");

            TableFormat inputFormat = def.getInputFormat();
            TableFormat outputFormat = def.getOutputFormat();

            if (inputFormat != null) {
                result.set("inputFormat", DataTableConverter.formatToJson(inputFormat));
            }
            if (outputFormat != null) {
                result.set("outputFormat", DataTableConverter.formatToJson(outputFormat));
            }

            // Build parameters DataTable if provided
            DataTable paramTable = null;
            if (params.has("parameters")) {
                JsonNode paramJson = params.get("parameters");
                if (paramJson.isObject() && !paramJson.has("format") && !paramJson.has("records") && inputFormat != null) {
                    // Simple object -> single record with function's input format
                    SimpleDataTable simple = new SimpleDataTable(inputFormat);
                    DataRecord rec = simple.addRecord();

                    // First, collect all provided parameters
                    java.util.Map<String, JsonNode> providedParams = new java.util.HashMap<>();
                    java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = ((com.fasterxml.jackson.databind.node.ObjectNode) paramJson).fields();
                    while (fields.hasNext()) {
                        java.util.Map.Entry<String, JsonNode> entry = fields.next();
                        providedParams.put(entry.getKey(), entry.getValue());
                    }
                    
                    // Set values for all fields in inputFormat
                    java.util.List<String> missingFields = new java.util.ArrayList<>();
                    java.util.List<String> setFields = new java.util.ArrayList<>();
                    
                    for (int i = 0; i < inputFormat.getFieldCount(); i++) {
                        com.tibbo.aggregate.common.datatable.FieldFormat fieldFormat = inputFormat.getField(i);
                        String fieldName = fieldFormat.getName();
                        
                        if (providedParams.containsKey(fieldName)) {
                            // Field is provided - set it
                            JsonNode valueNode = providedParams.get(fieldName);
                            Object value = convertJsonValue(valueNode);
                            rec.setValue(fieldName, value);
                            setFields.add(fieldName);
                        } else {
                            // Field is missing - add to missing list
                            missingFields.add(fieldName);
                            // Set default value based on field type
                            Object defaultValue = getDefaultValue(fieldFormat);
                            if (defaultValue != null) {
                                rec.setValue(fieldName, defaultValue);
                            }
                        }
                    }
                    
                    // Add diagnostic information
                    if (!missingFields.isEmpty()) {
                        result.put("parameterWarning", true);
                        result.put("missingFields", instance.arrayNode().addAll(
                            missingFields.stream()
                                .map(f -> instance.textNode(f))
                                .collect(java.util.stream.Collectors.toList())
                        ));
                        result.put("providedFields", instance.arrayNode().addAll(
                            setFields.stream()
                                .map(f -> instance.textNode(f))
                                .collect(java.util.stream.Collectors.toList())
                        ));
                    }
                    
                    paramTable = simple;
                } else {
                    // Assume proper DataTable JSON
                    paramTable = DataTableConverter.fromJson(paramJson);
                }
            } else if (inputFormat != null && inputFormat.getFieldCount() > 0) {
                // No parameters provided but function expects them
                result.put("parameterWarning", true);
                result.put("missingFields", instance.arrayNode());
                java.util.List<String> requiredFields = new java.util.ArrayList<>();
                for (int i = 0; i < inputFormat.getFieldCount(); i++) {
                    requiredFields.add(inputFormat.getField(i).getName());
                }
                result.set("requiredFields", instance.arrayNode().addAll(
                    requiredFields.stream()
                        .map(f -> instance.textNode(f))
                        .collect(java.util.stream.Collectors.toList())
                ));
            }

            // Call function and capture result / diagnostics
            try {
                final DataTable finalParams = paramTable;
                DataTable callResult = connection.executeWithTimeout(() -> {
                    try {
                        if (finalParams != null) {
                            return context.callFunction(functionName, finalParams);
                        } else {
                            return context.callFunction(functionName);
                        }
                    } catch (ContextException e) {
                        throw new RuntimeException(e);
                    }
                }, 60000);

                result.put("success", true);
                result.set("result", callResult != null ? DataTableConverter.toJson(callResult) : instance.nullNode());
            } catch (Exception callError) {
                String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(callError);
                result.put("success", false);
                result.put("error", errorMessage);
            }

            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to test function: " + errorMessage
            );
        }
    }

    private static Object convertJsonValue(JsonNode valueNode) {
        if (valueNode.isNull()) {
            return null;
        } else if (valueNode.isTextual()) {
            return valueNode.asText();
        } else if (valueNode.isInt()) {
            return valueNode.asInt();
        } else if (valueNode.isLong()) {
            return valueNode.asLong();
        } else if (valueNode.isDouble() || valueNode.isFloat()) {
            return valueNode.asDouble();
        } else if (valueNode.isBoolean()) {
            return valueNode.asBoolean();
        } else {
            return valueNode.asText();
        }
    }
    
    /**
     * Get default value for a field based on its type
     */
    private static Object getDefaultValue(com.tibbo.aggregate.common.datatable.FieldFormat fieldFormat) {
        char fieldType = fieldFormat.getType();
        switch (fieldType) {
            case com.tibbo.aggregate.common.datatable.FieldFormat.STRING_FIELD:
                return "";
            case com.tibbo.aggregate.common.datatable.FieldFormat.INTEGER_FIELD:
            case com.tibbo.aggregate.common.datatable.FieldFormat.LONG_FIELD:
                return 0;
            case com.tibbo.aggregate.common.datatable.FieldFormat.FLOAT_FIELD:
            case com.tibbo.aggregate.common.datatable.FieldFormat.DOUBLE_FIELD:
                return 0.0;
            case com.tibbo.aggregate.common.datatable.FieldFormat.BOOLEAN_FIELD:
                return false;
            default:
                return null;
        }
    }
}

