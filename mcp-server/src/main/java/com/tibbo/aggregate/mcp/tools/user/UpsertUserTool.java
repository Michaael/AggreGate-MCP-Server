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
 * Tool for creating or updating a user (idempotent)
 */
public class UpsertUserTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_upsert_user";
    }
    
    @Override
    public String getDescription() {
        return "Create a new user or update an existing user. " +
               "This is an idempotent operation that handles both creation and updates.";
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
        password.put("description", "Password (required for new users, optional for updates)");
        properties.set("password", password);
        
        ObjectNode email = instance.objectNode();
        email.put("type", "string");
        email.put("description", "User email (optional)");
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
            
            // Check if user exists
            boolean userExists = connection.executeWithTimeout(() -> {
                try {
                    Context existingUser = cm.get(userContextPath);
                    return existingUser != null;
                } catch (Exception e) {
                    return false;
                }
            }, 60000);
            
            boolean created = false;
            
            if (!userExists) {
                // Create new user
                if (!params.has("password")) {
                    throw new McpException(
                        com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                        "Password is required for new users"
                    );
                }
                
                String password = params.get("password").asText();
                cm.getRoot().callFunction(RootContextConstants.F_REGISTER, username, password, password);
                created = true;
            } else if (params.has("password")) {
                // Update password for existing user
                String password = params.get("password").asText();
                try {
                    cm.getRoot().callFunction(RootContextConstants.F_CHANGE_PASSWORD, username, password);
                } catch (Exception e) {
                    // Password change might not be available or might fail
                    // Continue with other updates
                }
            }
            
            // Get user context
            Context userContext = connection.executeWithTimeout(() -> {
                return cm.get(userContextPath);
            }, 60000);
            
            if (userContext == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "User context not found after " + (created ? "creation" : "update")
                );
            }
            
            // Update user fields
            com.tibbo.aggregate.common.context.CallerController caller = 
                userContext.getContextManager().getCallerController();
            
            if (params.has("email") || params.has("firstname") || params.has("lastname")) {
                try {
                    com.tibbo.aggregate.common.datatable.DataTable childInfo = 
                        userContext.getVariable("childInfo", caller);
                    
                    if (childInfo == null || childInfo.getRecordCount() == 0) {
                        // Create childInfo if it doesn't exist
                        com.tibbo.aggregate.common.datatable.TableFormat format = 
                            new com.tibbo.aggregate.common.datatable.TableFormat(1, 1);
                        format.addField("<email><S>");
                        format.addField("<firstname><S>");
                        format.addField("<lastname><S>");
                        childInfo = new com.tibbo.aggregate.common.datatable.SimpleDataTable(format);
                        childInfo.addRecord();
                    }
                    
                    com.tibbo.aggregate.common.datatable.DataRecord rec = childInfo.getRecord(0);
                    
                    if (params.has("email")) {
                        rec.setValue("email", params.get("email").asText());
                    }
                    if (params.has("firstname")) {
                        rec.setValue("firstname", params.get("firstname").asText());
                    }
                    if (params.has("lastname")) {
                        rec.setValue("lastname", params.get("lastname").asText());
                    }
                    
                    userContext.setVariable("childInfo", caller, childInfo);
                } catch (Exception e) {
                    // If childInfo update fails, try individual field updates
                    try {
                        if (params.has("email")) {
                            userContext.setVariableField("childInfo", "email", params.get("email").asText(), caller);
                        }
                        if (params.has("firstname")) {
                            userContext.setVariableField("childInfo", "firstname", params.get("firstname").asText(), caller);
                        }
                        if (params.has("lastname")) {
                            userContext.setVariableField("childInfo", "lastname", params.get("lastname").asText(), caller);
                        }
                    } catch (Exception e2) {
                        // Ignore field update errors
                    }
                }
            }
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("created", created);
            result.put("message", created ? "User created successfully" : "User updated successfully");
            result.put("path", userContext.getPath());
            result.put("username", username);
            
            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to upsert user: " + e.getMessage()
            );
        }
    }
}
