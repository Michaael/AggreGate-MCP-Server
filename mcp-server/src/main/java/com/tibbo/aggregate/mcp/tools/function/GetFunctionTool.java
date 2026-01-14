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

                TableFormat inputFormat = def.getInputFormat();
                TableFormat outputFormat = def.getOutputFormat();

                if (inputFormat != null) {
                    result.set("inputFormat", DataTableConverter.formatToJson(inputFormat));
                    result.set("inputFields", buildFieldsArray(inputFormat));
                }
                if (outputFormat != null) {
                    result.set("outputFormat", DataTableConverter.formatToJson(outputFormat));
                    result.set("outputFields", buildFieldsArray(outputFormat));
                }

                // Try to detect implementation type (java/expression/query) from modelFunctions, if present
                int functionType = com.tibbo.aggregate.common.server.ModelContextConstants.FUNCTION_TYPE_JAVA;
                String expression = null;
                String query = null;
                boolean isModelContext = false;

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

