package com.tibbo.aggregate.mcp.tools.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import java.util.List;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for listing a context tree starting from a root path.
 */
public class ListContextTreeTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_list_context_tree";
    }

    @Override
    public String getDescription() {
        return "List a tree of contexts starting from a given root path, with optional depth limit.";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();

        ObjectNode rootPath = instance.objectNode();
        rootPath.put("type", "string");
        rootPath.put("description", "Root context path to start from (default: root context)");
        properties.set("rootPath", rootPath);

        ObjectNode maxDepth = instance.objectNode();
        maxDepth.put("type", "integer");
        maxDepth.put("description", "Maximum depth to traverse (0 or omitted = unlimited)");
        properties.set("maxDepth", maxDepth);

        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);

        schema.set("properties", properties);
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
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
            ContextManager cm = connection.getContextManager();
            String rootPath = params.has("rootPath")
                ? ContextPathParser.parsePath(params.get("rootPath").asText())
                : cm.getRoot().getPath();

            int maxDepth = params.has("maxDepth") ? params.get("maxDepth").asInt() : 0;

            Context rootContext = connection.executeWithTimeout(() -> cm.get(rootPath), 60000);
            if (rootContext == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Root context not found: " + rootPath
                );
            }

            return buildNode(connection, rootContext, 0, maxDepth);
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to list context tree: " + errorMessage
            );
        }
    }

    private ObjectNode buildNode(ServerConnection connection, Context context, int depth, int maxDepth) throws ContextException {
        ObjectNode node = instance.objectNode();
        node.put("path", context.getPath());
        node.put("name", context.getName());
        if (context.getDescription() != null) {
            node.put("description", context.getDescription());
        }

        // Type hints for easier navigation by AI
        String path = context.getPath();
        if (path.contains(".models.")) {
            node.put("kindHint", "model");
        } else if (path.contains(".devices.") || path.contains("/devices/")) {
            node.put("kindHint", "device");
        } else if (path.contains(".widgets.") || path.contains("/widgets/")) {
            node.put("kindHint", "widget");
        } else if (path.contains(".dashboards.") || path.contains("/dashboards/")) {
            node.put("kindHint", "dashboard");
        }

        // Stop if depth limit reached (0 = unlimited)
        if (maxDepth > 0 && depth >= maxDepth) {
            return node;
        }

        List<Context> children;
        try {
            children = connection.executeWithTimeout(() -> context.getChildren(), 60000);
        } catch (Exception e) {
            // If children cannot be loaded, just return node without children
            return node;
        }

        if (children != null && !children.isEmpty()) {
            ArrayNode childrenArray = instance.arrayNode();
            for (Context child : children) {
                try {
                    childrenArray.add(buildNode(connection, child, depth + 1, maxDepth));
                } catch (Exception ignored) {
                    // Skip problematic child
                }
            }
            node.set("children", childrenArray);
        }

        return node;
    }
}

