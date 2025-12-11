package com.tibbo.aggregate.mcp.tools.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for updating user information
 */
public class UpdateUserTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_update_user";
    }
    
    @Override
    public String getDescription() {
        return "Update user information (email, firstname, lastname, etc.)";
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
        
        ObjectNode email = instance.objectNode();
        email.put("type", "string");
        email.put("description", "Email (optional)");
        properties.set("email", email);
        
        ObjectNode firstname = instance.objectNode();
        firstname.put("type", "string");
        firstname.put("description", "First name (optional)");
        properties.set("firstname", firstname);
        
        ObjectNode lastname = instance.objectNode();
        lastname.put("type", "string");
        lastname.put("description", "Last name (optional)");
        properties.set("lastname", lastname);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("username"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("username")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Username parameter is required"
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
            ContextManager cm = connection.getContextManager();
            String userContextPath = ContextUtils.userContextPath(username);
            Context userContext = cm.get(userContextPath);
            
            DataTable userInfo = userContext.getVariable("childInfo");
            
            if (params.has("email")) {
                userInfo.rec().setValue("email", params.get("email").asText());
            }
            if (params.has("firstname")) {
                userInfo.rec().setValue("firstname", params.get("firstname").asText());
            }
            if (params.has("lastname")) {
                userInfo.rec().setValue("lastname", params.get("lastname").asText());
            }
            
            userContext.setVariable("childInfo", userInfo);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "User updated successfully");
            
            return result;
        } catch (ContextException e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to update user: " + e.getMessage()
            );
        }
    }
}

