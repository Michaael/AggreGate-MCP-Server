package com.tibbo.aggregate.mcp.tools.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for getting server statistics
 */
public class GetServerStatisticsTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_get_server_statistics";
    }
    
    @Override
    public String getDescription() {
        return "Get overall server statistics (number of contexts, devices, users, load, etc.)";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode connectionKey = instance.objectNode();
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
            ObjectNode result = instance.objectNode();
            
            // Get server info
            try {
                // Server info may be available through connection or context
                // For now, try to get from server context
                com.tibbo.aggregate.common.context.Context<?> serverContext = 
                    connection.getContextManager().get("server");
                if (serverContext != null) {
                    result.put("serverName", serverContext.getName() != null ? serverContext.getName() : "unknown");
                }
                result.put("serverVersion", "unknown");
            } catch (Exception e) {
                // Ignore
                result.put("serverVersion", "unknown");
                result.put("serverName", "unknown");
            }
            
            // Count users
            try {
                com.tibbo.aggregate.common.context.Context<?> usersContext = 
                    connection.getContextManager().get("users");
                if (usersContext != null) {
                    java.util.List<?> users = usersContext.getChildren();
                    result.put("userCount", users != null ? users.size() : 0);
                } else {
                    result.put("userCount", 0);
                }
            } catch (Exception e) {
                result.put("userCount", 0);
            }
            
            // Count devices (may need to iterate through users)
            try {
                int deviceCount = 0;
                com.tibbo.aggregate.common.context.Context<?> usersContext = 
                    connection.getContextManager().get("users");
                if (usersContext != null) {
                    java.util.List<?> users = usersContext.getChildren();
                    if (users != null) {
                        for (Object userObj : users) {
                            if (userObj instanceof com.tibbo.aggregate.common.context.Context) {
                                com.tibbo.aggregate.common.context.Context<?> userContext = 
                                    (com.tibbo.aggregate.common.context.Context<?>) userObj;
                                com.tibbo.aggregate.common.context.Context<?> devicesContext = 
                                    userContext.getChild("devices");
                                if (devicesContext != null) {
                                    java.util.List<?> devices = devicesContext.getChildren();
                                    if (devices != null) {
                                        deviceCount += devices.size();
                                    }
                                }
                            }
                        }
                    }
                }
                result.put("deviceCount", deviceCount);
            } catch (Exception e) {
                result.put("deviceCount", 0);
            }
            
            // Note: Additional statistics like load, memory usage, etc. may require server-specific API calls
            
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to get server statistics: " + errorMessage
            );
        }
    }
}
