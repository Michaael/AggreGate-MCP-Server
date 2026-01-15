package com.tibbo.aggregate.mcp.tools.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for setting default dashboard for a user
 */
public class SetDefaultDashboardTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_set_default_dashboard";
    }
    
    @Override
    public String getDescription() {
        return "Set default dashboard for a user";
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
        
        ObjectNode dashboardPath = instance.objectNode();
        dashboardPath.put("type", "string");
        dashboardPath.put("description", "Dashboard context path");
        properties.set("dashboardPath", dashboardPath);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("username").add("dashboardPath"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("username") || !params.has("dashboardPath")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Username and dashboardPath parameters are required"
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
            String dashboardPath = ContextPathParser.parsePath(params.get("dashboardPath").asText());
            
            // Get user context
            String userPath = "users." + username;
            Context<?> userContext = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(userPath);
                if (ctx == null) {
                    throw new RuntimeException("User context not found: " + userPath);
                }
                return ctx;
            }, 60000L);
            
            // Set default dashboard through user preferences variable or action
            connection.executeWithTimeout(() -> {
                try {
                    // Try to set through user preferences variable
                    com.tibbo.aggregate.common.datatable.DataTable preferences = userContext.getVariable("preferences");
                    if (preferences != null && preferences.getRecordCount() > 0) {
                        preferences.rec().setValue("defaultDashboard", dashboardPath);
                        userContext.setVariable("preferences", preferences);
                        return null;
                    }
                    
                    // If preferences variable doesn't exist, try action
                    com.tibbo.aggregate.common.datatable.DataTable input = 
                        new com.tibbo.aggregate.common.datatable.SimpleDataTable(
                            "<dashboardPath><S>",
                            true
                        );
                    input.addRecord().setValue("dashboardPath", dashboardPath);
                    
                    com.tibbo.aggregate.common.action.ServerActionInput actionInput = 
                        com.tibbo.aggregate.common.action.ActionUtils.createActionInput(input);
                    
                    com.tibbo.aggregate.common.action.ActionIdentifier actionId = 
                        com.tibbo.aggregate.common.action.ActionUtils.initAction(
                            userContext,
                            "setDefaultDashboard",
                            actionInput,
                            null,
                            new com.tibbo.aggregate.common.action.ActionExecutionMode(
                                com.tibbo.aggregate.common.action.ActionExecutionMode.HEADLESS
                            ),
                            null
                        );
                    
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to set default dashboard: " + e.getMessage(), e);
                }
            }, 60000L);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Default dashboard set successfully");
            result.put("username", username);
            result.put("dashboardPath", dashboardPath);
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to set default dashboard: " + errorMessage
            );
        }
    }
}
