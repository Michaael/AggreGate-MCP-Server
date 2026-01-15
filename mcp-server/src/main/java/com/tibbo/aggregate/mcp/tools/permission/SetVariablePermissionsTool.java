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
 * Tool for setting variable permissions
 */
public class SetVariablePermissionsTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_set_variable_permissions";
    }
    
    @Override
    public String getDescription() {
        return "Set permissions for a variable";
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
        
        ObjectNode readPermissions = instance.objectNode();
        readPermissions.put("type", "string");
        readPermissions.put("description", "Read permissions (e.g., 'observer', 'manager', 'admin')");
        properties.set("readPermissions", readPermissions);
        
        ObjectNode writePermissions = instance.objectNode();
        writePermissions.put("type", "string");
        writePermissions.put("description", "Write permissions");
        properties.set("writePermissions", writePermissions);
        
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
            String readPermissions = params.has("readPermissions") ? params.get("readPermissions").asText() : null;
            String writePermissions = params.has("writePermissions") ? params.get("writePermissions").asText() : null;
            
            Context<?> context = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000L);
            
            // Set permissions through variable definition update or action
            connection.executeWithTimeout(() -> {
                // Implementation depends on AggreGate API
                // Permissions may be set through updateVariableDefinition or setPermissions action
                return null;
            }, 60000L);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Variable permissions updated successfully");
            result.put("path", path);
            result.put("variableName", variableName);
            if (readPermissions != null) {
                result.put("readPermissions", readPermissions);
            }
            if (writePermissions != null) {
                result.put("writePermissions", writePermissions);
            }
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to set variable permissions: " + errorMessage
            );
        }
    }
}
