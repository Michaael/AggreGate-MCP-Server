package com.tibbo.aggregate.mcp.tools.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for getting detailed user information
 */
public class GetUserTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_get_user";
    }
    
    @Override
    public String getDescription() {
        return "Get detailed information about a user account";
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
            
            Context userContext = connection.executeWithTimeout(() -> {
                try {
                    return cm.get(userContextPath);
                } catch (Exception e) {
                    return null;
                }
            }, 60000);
            
            if (userContext == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "User not found: " + username
                );
            }
            
            ObjectNode result = instance.objectNode();
            result.put("username", username);
            result.put("path", userContext.getPath());
            result.put("name", userContext.getName());
            
            String description = userContext.getDescription();
            if (description != null) {
                result.put("description", description);
            }
            
            // Try to get email from childInfo variable
            try {
                com.tibbo.aggregate.common.context.CallerController caller = 
                    userContext.getContextManager().getCallerController();
                com.tibbo.aggregate.common.datatable.DataTable childInfo = 
                    userContext.getVariable("childInfo", caller);
                if (childInfo != null && childInfo.getRecordCount() > 0) {
                    com.tibbo.aggregate.common.datatable.DataRecord rec = childInfo.getRecord(0);
                    if (rec.hasField("email")) {
                        String email = rec.getString("email");
                        if (email != null && !email.isEmpty()) {
                            result.put("email", email);
                        }
                    }
                }
            } catch (Exception e) {
                // Email not available or not set
            }
            
            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to get user: " + e.getMessage()
            );
        }
    }
}
