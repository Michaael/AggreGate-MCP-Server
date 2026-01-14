package com.tibbo.aggregate.mcp.tools.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for idempotently getting or creating a context (model)
 */
public class GetOrCreateContextTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_get_or_create_context";
    }

    @Override
    public String getDescription() {
        return "Get an existing context by full path or create it if it does not exist";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();

        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Full context path (e.g. users.admin.models.my_model)");
        properties.set("path", path);

        ObjectNode description = instance.objectNode();
        description.put("type", "string");
        description.put("description", "Description of the context (optional, used on creation)");
        properties.set("description", description);

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
            String fullPath = ContextPathParser.parsePath(params.get("path").asText());
            String description = params.has("description") ? params.get("description").asText() : null;

            ContextManager cm = connection.getContextManager();

            // Try to get existing context
            Context existing = connection.executeWithTimeout(() -> {
                try {
                    return cm.get(fullPath);
                } catch (Exception e) {
                    return null;
                }
            }, 60000);

            if (existing != null) {
                ObjectNode result = instance.objectNode();
                result.put("success", true);
                result.put("created", false);
                result.put("path", existing.getPath());
                result.put("name", existing.getName());
                if (existing.getDescription() != null) {
                    result.put("description", existing.getDescription());
                }
                result.put("message", "Context already exists");
                return result;
            }

            // Need to create: split fullPath into parentPath and name
            int lastDot = fullPath.lastIndexOf('.');
            if (lastDot <= 0) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Full path must contain a parent path (e.g. users.admin.models.my_model)"
                );
            }

            String parentPath = fullPath.substring(0, lastDot);
            String name = fullPath.substring(lastDot + 1);

            ObjectNode createParams = instance.objectNode();
            createParams.put("parentPath", parentPath);
            createParams.put("name", name);
            if (description != null) {
                createParams.put("description", description);
            }
            if (connectionKey != null) {
                createParams.put("connectionKey", connectionKey);
            }

            CreateContextTool createTool = new CreateContextTool();
            JsonNode createResult = createTool.execute(createParams, connectionManager);

            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("created", true);

            if (createResult.has("path")) {
                result.put("path", createResult.get("path").asText());
            } else {
                result.put("path", fullPath);
            }
            if (createResult.has("name")) {
                result.put("name", createResult.get("name").asText());
            } else {
                result.put("name", name);
            }
            if (createResult.has("description")) {
                result.put("description", createResult.get("description").asText());
            } else if (description != null) {
                result.put("description", description);
            }

            result.put("message", "Context created successfully (idempotent get_or_create)");
            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to get or create context: " + errorMessage
            );
        }
    }
}

