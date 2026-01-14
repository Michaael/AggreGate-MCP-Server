package com.tibbo.aggregate.mcp.tools.widget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.datatable.FieldFormat;
import com.tibbo.aggregate.common.server.WidgetContextConstants;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for getting widget template XML
 */
public class GetWidgetTemplateTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_get_widget_template";
    }

    @Override
    public String getDescription() {
        return "Get XML template for a widget";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();

        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Widget context path (e.g., 'users.admin.widgets.myWidget')");
        properties.set("path", path);

        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);

        schema.set("required", instance.arrayNode().add("path"));
        schema.set("properties", properties);
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path parameter is required"
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

            Context widgetContext = connection.getContextManager().get(path);
            if (widgetContext == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Widget context not found: " + path
                );
            }

            DataTable templateTable = connection.executeWithTimeout(() -> {
                try {
                    com.tibbo.aggregate.common.context.CallerController caller =
                        widgetContext.getContextManager().getCallerController();
                    return widgetContext.getVariable(WidgetContextConstants.V_TEMPLATE, caller);
                } catch (ContextException e) {
                    throw new RuntimeException("Failed to get template: " + e.getMessage(), e);
                }
            }, 60000);

            String template = null;
            if (templateTable != null && templateTable.getRecordCount() > 0) {
                TableFormat format = templateTable.getFormat();
                if (format != null) {
                    int valueFieldIndex = -1;
                    for (int i = 0; i < format.getFieldCount(); i++) {
                        FieldFormat ff = format.getField(i);
                        if (WidgetContextConstants.F_VALUE.equals(ff.getName())) {
                            valueFieldIndex = i;
                            break;
                        }
                    }
                    if (valueFieldIndex >= 0) {
                        template = templateTable.getRecord(0).getString(WidgetContextConstants.F_VALUE);
                    }
                }
            }

            ObjectNode result = instance.objectNode();
            result.put("path", path);
            if (template != null) {
                result.put("template", template);
            } else {
                result.put("template", "");
                result.put("note", "Template variable is empty or not in expected format");
            }

            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to get widget template: " + errorMessage
            );
        }
    }
}

