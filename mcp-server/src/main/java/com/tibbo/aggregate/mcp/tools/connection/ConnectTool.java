package com.tibbo.aggregate.mcp.tools.connection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.protocol.RemoteServer;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for connecting to AggreGate server
 */
public class ConnectTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_connect";
    }
    
    @Override
    public String getDescription() {
        return "Connect to an AggreGate server";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode host = instance.objectNode();
        host.put("type", "string");
        host.put("description", "Server hostname or IP address");
        host.put("default", "localhost");
        properties.set("host", host);
        
        ObjectNode port = instance.objectNode();
        port.put("type", "integer");
        port.put("description", "Server port");
        port.put("default", RemoteServer.DEFAULT_PORT);
        properties.set("port", port);
        
        ObjectNode username = instance.objectNode();
        username.put("type", "string");
        username.put("description", "Username");
        username.put("default", RemoteServer.DEFAULT_USERNAME);
        properties.set("username", username);
        
        ObjectNode password = instance.objectNode();
        password.put("type", "string");
        password.put("description", "Password");
        password.put("default", RemoteServer.DEFAULT_PASSWORD);
        properties.set("password", password);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key for multiple connections");
        properties.set("connectionKey", connectionKey);
        
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        try {
            String host = params.has("host") ? params.get("host").asText() : "localhost";
            int port = params.has("port") ? params.get("port").asInt() : RemoteServer.DEFAULT_PORT;
            String username = params.has("username") ? params.get("username").asText() : RemoteServer.DEFAULT_USERNAME;
            String password = params.has("password") ? params.get("password").asText() : RemoteServer.DEFAULT_PASSWORD;
            String connectionKey = params.has("connectionKey") ? params.get("connectionKey").asText() : null;
            
            Log.start();
            
            ServerConnection connection;
            if (connectionKey != null) {
                connection = connectionManager.getServerConnection(connectionKey, host, port, username, password);
            } else {
                connection = connectionManager.getServerConnection(host, port, username, password);
            }
            
            connection.connect();
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Connected to server");
            result.put("host", host);
            result.put("port", port);
            result.put("username", username);
            
            return result;
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONNECTION_ERROR,
                "Failed to connect: " + e.getMessage()
            );
        }
    }
}

