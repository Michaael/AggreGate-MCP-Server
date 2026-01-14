package com.tibbo.aggregate.mcp.tools.variable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;
import com.tibbo.aggregate.mcp.util.DataTableConverter;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Smart tool for setting a variable value.
 *
 * It:
 * - Inspects existing variable format (if available)
 * - Accepts simple scalar/object values for single-record variables
 * - Delegates actual write logic to aggregate_set_variable implementation
 */
public class SetVariableSmartTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_set_variable_smart";
    }

    @Override
    public String getDescription() {
        return "Smart setter for context variables that automatically chooses the correct write strategy";
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

        ObjectNode value = instance.objectNode();
        value.put("description",
            "Value to set. For simple variables can be a scalar. " +
            "For multi-field or multi-record variables use DataTable JSON (records/format).");
        properties.set("value", value);

        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);

        schema.set("required", instance.arrayNode().add("path").add("variableName").add("value"));
        schema.set("properties", properties);
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("variableName") || !params.has("value")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path, variableName, and value parameters are required"
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
            JsonNode valueNode = params.get("value");

            Context context = connection.executeWithTimeout(() -> {
                return connection.getContextManager().get(path);
            }, 60000);

            if (context == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Context not found: " + path
                );
            }

            // Try to get existing variable to know its format
            TableFormat existingFormat = connection.executeWithTimeout(() -> {
                try {
                    com.tibbo.aggregate.common.context.CallerController caller =
                        context.getContextManager().getCallerController();
                    DataTable existing = context.getVariableClone(variableName, caller);
                    return existing != null ? existing.getFormat() : null;
                } catch (ContextException e) {
                    // Variable may not exist yet
                    return null;
                }
            }, 60000);

            ObjectNode valueForSetTool;

            if (existingFormat != null && !isDataTableJson(valueNode)) {
                // We know the format and value is not already a DataTable JSON.
                // Build a single-record DataTable JSON that matches existing format.
                valueForSetTool = instance.objectNode();
                valueForSetTool.set("format", DataTableConverter.formatToJson(existingFormat));

                ArrayNode records = instance.arrayNode();
                ObjectNode record = instance.objectNode();

                if (valueNode.isObject()) {
                    // Multi-field value: { field1: v1, field2: v2, ... }
                    for (int i = 0; i < existingFormat.getFieldCount(); i++) {
                        String fieldName = existingFormat.getField(i).getName();
                        if (valueNode.has(fieldName)) {
                            record.set(fieldName, valueNode.get(fieldName));
                        }
                    }
                } else {
                    // Scalar value: only valid for single-field variables
                    if (existingFormat.getFieldCount() != 1) {
                        throw new McpException(
                            com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                            "Scalar value is only allowed for single-field variables"
                        );
                    }
                    String fieldName = existingFormat.getField(0).getName();
                    record.set(fieldName, valueNode);
                }

                records.add(record);
                valueForSetTool.set("records", records);
            } else if (valueNode.isObject()) {
                // Assume caller already provided DataTable JSON
                valueForSetTool = (ObjectNode) valueNode;
            } else {
                // No format information and not an object -> cannot reliably construct DataTable
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                    "Value must be an object or there must be an existing variable format"
                );
            }

            // Delegate actual write logic to aggregate_set_variable implementation
            ObjectNode delegateParams = instance.objectNode();
            delegateParams.put("path", path);
            delegateParams.put("name", variableName);
            delegateParams.set("value", valueForSetTool);
            if (connectionKey != null) {
                delegateParams.put("connectionKey", connectionKey);
            }

            SetVariableTool delegate = new SetVariableTool();
            return delegate.execute(delegateParams, connectionManager);
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to set variable (smart): " + errorMessage
            );
        }
    }

    private boolean isDataTableJson(JsonNode node) {
        if (node == null || !node.isObject()) {
            return false;
        }
        // Heuristic: DataTable JSON normally has "records" array, and optionally "format"
        return node.has("records") || node.has("format") || node.has("recordCount");
    }
}

