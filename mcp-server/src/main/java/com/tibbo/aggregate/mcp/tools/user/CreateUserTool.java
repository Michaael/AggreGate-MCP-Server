package com.tibbo.aggregate.mcp.tools.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.server.RootContextConstants;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for creating a user
 */
public class CreateUserTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_create_user";
    }
    
    @Override
    public String getDescription() {
        return "Create a new user account";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode username = instance.objectNode();
        username.put("type", "string");
        username.put("description", "Username");
        properties.set("username", username);
        
        ObjectNode password = instance.objectNode();
        password.put("type", "string");
        password.put("description", "Password");
        properties.set("password", password);
        
        ObjectNode email = instance.objectNode();
        email.put("type", "string");
        email.put("description", "User email (optional)");
        properties.set("email", email);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("username").add("password"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("username") || !params.has("password")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Username and password parameters are required"
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
            String username = params.get("username").asText();
            String password = params.get("password").asText();
            
            ContextManager cm = connection.getContextManager();
            cm.getRoot().callFunction(RootContextConstants.F_REGISTER, username, password, password);
            
            String userContextPath = ContextUtils.userContextPath(username);
            Context userContext = cm.get(userContextPath);
            
            if (params.has("email")) {
                userContext.setVariableField("childInfo", "email", params.get("email").asText(), null);
            }
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "User created successfully");
            result.put("path", userContext.getPath());
            result.put("username", username);
            
            return result;
        } catch (ContextException e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to create user: " + e.getMessage()
            );
        }
    }
}

