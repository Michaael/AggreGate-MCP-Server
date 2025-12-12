package com.tibbo.aggregate.mcp.tools.connection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for disconnecting from AggreGate server
 */
public class DisconnectTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_disconnect";
    }
    
    @Override
    public String getDescription() {
        return "Disconnect from an AggreGate server";
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
        
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        String connectionKey = params.has("connectionKey") ? params.get("connectionKey").asText() : null;
        
        ServerConnection connection = connectionKey != null 
            ? connectionManager.getServerConnection(connectionKey)
            : connectionManager.getDefaultServerConnection();
        
        if (connection == null) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONNECTION_ERROR,
                "No connection found"
            );
        }
        
        try {
            // Проверяем, подключен ли сервер, перед отключением
            try {
                if (connection.isConnected() || connection.isLoggedIn()) {
                    connection.disconnect();
                }
            } catch (Exception e) {
                // Игнорируем ошибки при проверке подключения - просто пытаемся отключиться
                try {
                    connection.disconnect();
                } catch (Exception e2) {
                    // Игнорируем ошибки отключения
                }
            }
            
            // Удаляем соединение из менеджера
            if (connectionKey != null) {
                connectionManager.removeServerConnection(connectionKey);
            } else {
                connectionManager.removeDefaultServerConnection();
            }
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Disconnected from server");
            
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONNECTION_ERROR,
                "Failed to disconnect: " + errorMessage
            );
        }
    }
}

