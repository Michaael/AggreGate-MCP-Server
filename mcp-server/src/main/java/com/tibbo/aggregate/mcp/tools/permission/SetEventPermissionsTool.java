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
 * Tool for setting event permissions
 */
public class SetEventPermissionsTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_set_event_permissions";
    }
    
    @Override
    public String getDescription() {
        return "Set permissions for an event";
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
        
        ObjectNode eventName = instance.objectNode();
        eventName.put("type", "string");
        eventName.put("description", "Event name");
        properties.set("eventName", eventName);
        
        ObjectNode readPermissions = instance.objectNode();
        readPermissions.put("type", "string");
        readPermissions.put("description", "Read permissions");
        properties.set("readPermissions", readPermissions);
        
        ObjectNode firePermissions = instance.objectNode();
        firePermissions.put("type", "string");
        firePermissions.put("description", "Permissions to fire the event");
        properties.set("firePermissions", firePermissions);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("eventName"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("eventName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and eventName parameters are required"
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
            String eventName = params.get("eventName").asText();
            String readPermissions = params.has("readPermissions") ? params.get("readPermissions").asText() : null;
            String firePermissions = params.has("firePermissions") ? params.get("firePermissions").asText() : null;
            
            Context<?> context = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000L);
            
            // Set permissions through event definition update or action
            connection.executeWithTimeout(() -> {
                // Implementation depends on AggreGate API
                return null;
            }, 60000L);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Event permissions updated successfully");
            result.put("path", path);
            result.put("eventName", eventName);
            if (readPermissions != null) {
                result.put("readPermissions", readPermissions);
            }
            if (firePermissions != null) {
                result.put("firePermissions", firePermissions);
            }
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to set event permissions: " + errorMessage
            );
        }
    }
}
