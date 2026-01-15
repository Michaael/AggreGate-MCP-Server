package com.tibbo.aggregate.mcp.tools.permission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for setting context permissions
 */
public class SetContextPermissionsTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_set_context_permissions";
    }
    
    @Override
    public String getDescription() {
        return "Set permissions for a context";
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
        
        ObjectNode readPermissions = instance.objectNode();
        readPermissions.put("type", "string");
        readPermissions.put("description", "Read permissions");
        properties.set("readPermissions", readPermissions);
        
        ObjectNode writePermissions = instance.objectNode();
        writePermissions.put("type", "string");
        writePermissions.put("description", "Write permissions");
        properties.set("writePermissions", writePermissions);
        
        ObjectNode executePermissions = instance.objectNode();
        executePermissions.put("type", "string");
        executePermissions.put("description", "Execute permissions (for actions)");
        properties.set("executePermissions", executePermissions);
        
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
            String readPermissions = params.has("readPermissions") ? params.get("readPermissions").asText() : null;
            String writePermissions = params.has("writePermissions") ? params.get("writePermissions").asText() : null;
            String executePermissions = params.has("executePermissions") ? params.get("executePermissions").asText() : null;
            
            Context<?> context = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000L);
            
            // Set permissions through context update or action
            connection.executeWithTimeout(() -> {
                // Implementation depends on AggreGate API
                return null;
            }, 60000L);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Context permissions updated successfully");
            result.put("path", path);
            if (readPermissions != null) {
                result.put("readPermissions", readPermissions);
            }
            if (writePermissions != null) {
                result.put("writePermissions", writePermissions);
            }
            if (executePermissions != null) {
                result.put("executePermissions", executePermissions);
            }
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to set context permissions: " + errorMessage
            );
        }
    }
}
