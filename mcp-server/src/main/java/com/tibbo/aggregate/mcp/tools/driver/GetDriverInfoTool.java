package com.tibbo.aggregate.mcp.tools.driver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for getting driver information
 */
public class GetDriverInfoTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_get_driver_info";
    }
    
    @Override
    public String getDescription() {
        return "Get information about a device driver";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode driverId = instance.objectNode();
        driverId.put("type", "string");
        driverId.put("description", "Driver identifier");
        properties.set("driverId", driverId);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("driverId"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("driverId")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "DriverId parameter is required"
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
            String driverId = params.get("driverId").asText();
            
            // Get driver info through server API or action
            ObjectNode result = instance.objectNode();
            result.put("driverId", driverId);
            
            // Note: Actual implementation depends on AggreGate API
            // Driver info may be accessed through server methods or actions
            
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to get driver info: " + errorMessage
            );
        }
    }
}
