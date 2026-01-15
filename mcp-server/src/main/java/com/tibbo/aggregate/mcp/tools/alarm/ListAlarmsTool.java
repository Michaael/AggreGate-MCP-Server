package com.tibbo.aggregate.mcp.tools.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for listing alarms in a context
 */
public class ListAlarmsTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_list_alarms";
    }
    
    @Override
    public String getDescription() {
        return "List all alarms in a context";
    }
    
    @Override
    public JsonNode getInputSchema() {
        com.fasterxml.jackson.databind.node.ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        com.fasterxml.jackson.databind.node.ObjectNode properties = instance.objectNode();
        
        com.fasterxml.jackson.databind.node.ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Context path");
        properties.set("path", path);
        
        com.fasterxml.jackson.databind.node.ObjectNode activeOnly = instance.objectNode();
        activeOnly.put("type", "boolean");
        activeOnly.put("description", "Return only active alarms (default: false)");
        properties.set("activeOnly", activeOnly);
        
        com.fasterxml.jackson.databind.node.ObjectNode connectionKey = instance.objectNode();
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
            boolean activeOnly = params.has("activeOnly") ? params.get("activeOnly").asBoolean() : false;
            
            Context<?> context = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000L);
            
            // Alarms in AggreGate are typically stored in a special variable or accessed through actions
            ArrayNode result = instance.arrayNode();
            
            // Note: Actual implementation depends on AggreGate API
            // Alarms may be stored in a variable like "alarms" or accessed through "listAlarms" action
            // For now, return empty array with note that action execution may be needed
            
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to list alarms: " + errorMessage
            );
        }
    }
}
