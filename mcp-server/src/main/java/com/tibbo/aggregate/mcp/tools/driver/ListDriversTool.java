package com.tibbo.aggregate.mcp.tools.driver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for listing available device drivers
 */
public class ListDriversTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_list_drivers";
    }
    
    @Override
    public String getDescription() {
        return "List available device drivers";
    }
    
    @Override
    public JsonNode getInputSchema() {
        com.fasterxml.jackson.databind.node.ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        com.fasterxml.jackson.databind.node.ObjectNode properties = instance.objectNode();
        
        com.fasterxml.jackson.databind.node.ObjectNode category = instance.objectNode();
        category.put("type", "string");
        category.put("description", "Driver category (e.g., 'virtual', 'modbus', 'opc')");
        properties.set("category", category);
        
        com.fasterxml.jackson.databind.node.ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode());
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
            String category = params.has("category") ? params.get("category").asText() : null;
            
            // Get drivers list through server API or action
            ArrayNode result = instance.arrayNode();
            
            // Note: Actual implementation depends on AggreGate API
            // Drivers may be accessed through server methods or actions
            // For now, return empty array with note that server API may be needed
            
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to list drivers: " + errorMessage
            );
        }
    }
}
