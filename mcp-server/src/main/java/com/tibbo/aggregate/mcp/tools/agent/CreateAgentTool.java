package com.tibbo.aggregate.mcp.tools.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.protocol.RemoteServer;
import com.tibbo.aggregate.mcp.connection.AgentConnection;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for creating and connecting an agent
 */
public class CreateAgentTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_create_agent";
    }
    
    @Override
    public String getDescription() {
        return "Create and connect an agent to AggreGate server";
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
        
        ObjectNode agentName = instance.objectNode();
        agentName.put("type", "string");
        agentName.put("description", "Agent name");
        properties.set("agentName", agentName);
        
        ObjectNode eventConfirmation = instance.objectNode();
        eventConfirmation.put("type", "boolean");
        eventConfirmation.put("description", "Enable event confirmation");
        eventConfirmation.put("default", true);
        properties.set("eventConfirmation", eventConfirmation);
        
        schema.set("required", instance.arrayNode().add("agentName"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("agentName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "AgentName parameter is required"
            );
        }
        
        try {
            String host = params.has("host") ? params.get("host").asText() : "localhost";
            int port = params.has("port") ? params.get("port").asInt() : RemoteServer.DEFAULT_PORT;
            String username = params.has("username") ? params.get("username").asText() : RemoteServer.DEFAULT_USERNAME;
            String password = params.has("password") ? params.get("password").asText() : RemoteServer.DEFAULT_PASSWORD;
            String agentName = params.get("agentName").asText();
            boolean eventConfirmation = params.has("eventConfirmation") ? params.get("eventConfirmation").asBoolean() : true;
            
            AgentConnection agentConnection = connectionManager.getAgentConnection(
                host, port, username, password, agentName, eventConfirmation
            );
            
            agentConnection.connect();
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Agent created and connected");
            result.put("agentName", agentName);
            result.put("connected", agentConnection.isConnected());
            
            return result;
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONNECTION_ERROR,
                "Failed to create agent: " + e.getMessage()
            );
        }
    }
}

