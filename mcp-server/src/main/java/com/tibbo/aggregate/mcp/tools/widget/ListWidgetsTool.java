package com.tibbo.aggregate.mcp.tools.widget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.server.WidgetContextConstants;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import java.util.List;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for listing widgets under a widgets container context
 */
public class ListWidgetsTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_list_widgets";
    }

    @Override
    public String getDescription() {
        return "List widgets under a widgets parent context";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();

        ObjectNode parentPath = instance.objectNode();
        parentPath.put("type", "string");
        parentPath.put("description", "Widgets parent context path (e.g., 'users.admin.widgets')");
        properties.set("parentPath", parentPath);

        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);

        schema.set("required", instance.arrayNode().add("parentPath"));
        schema.set("properties", properties);
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("parentPath")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "ParentPath parameter is required"
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
            String parentPath = ContextPathParser.parsePath(params.get("parentPath").asText());
            ContextManager cm = connection.getContextManager();

            Context parentContext = connection.executeWithTimeout(() -> cm.get(parentPath), 60000);
            if (parentContext == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Parent context not found: " + parentPath
                );
            }

            List<Context> children = connection.executeWithTimeout(() -> parentContext.getChildren(), 60000);

            ArrayNode result = instance.arrayNode();
            if (children != null) {
                for (Context child : children) {
                    if (child == null) {
                        continue;
                    }
                    // Heuristic: widgets should have V_TEMPLATE variable
                    boolean isWidget = connection.executeWithTimeout(() -> {
                        try {
                            child.getVariableDefinition(WidgetContextConstants.V_TEMPLATE);
                            return true;
                        } catch (Exception e) {
                            return false;
                        }
                    }, 60000);

                    if (!isWidget) {
                        continue;
                    }

                    ObjectNode widget = instance.objectNode();
                    widget.put("path", child.getPath());
                    widget.put("name", child.getName());
                    if (child.getDescription() != null) {
                        widget.put("description", child.getDescription());
                    }
                    result.add(widget);
                }
            }

            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to list widgets: " + errorMessage
            );
        }
    }
}

