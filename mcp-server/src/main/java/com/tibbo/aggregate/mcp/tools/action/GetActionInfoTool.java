package com.tibbo.aggregate.mcp.tools.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.action.ActionDefinition;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for getting detailed information about an action
 */
public class GetActionInfoTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_get_action_info";
    }
    
    @Override
    public String getDescription() {
        return "Get detailed information about an action (parameters, description)";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Context path");
        properties.set("path", path);
        
        ObjectNode actionName = instance.objectNode();
        actionName.put("type", "string");
        actionName.put("description", "Action name");
        properties.set("actionName", actionName);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("actionName"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("actionName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and actionName parameters are required"
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
            String path = ContextPathParser.parsePath(params.get("path").asText());
            String actionName = params.get("actionName").asText();
            
            Context<?> context = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000L);
            
            ActionDefinition actionDef = connection.executeWithTimeout(() -> {
                ActionDefinition ad = context.getActionDefinition(actionName);
                if (ad == null) {
                    throw new RuntimeException("Action not found: " + actionName);
                }
                return ad;
            }, 60000L);
            
            ObjectNode result = instance.objectNode();
            result.put("name", actionDef.getName() != null ? actionDef.getName() : "");
            result.put("description", actionDef.getDescription() != null ? actionDef.getDescription() : "");
            result.put("group", actionDef.getGroup() != null ? actionDef.getGroup() : "");
            
            // Note: ActionDefinition may not have direct getInputFormat/getOutputFormat methods
            // Format information may need to be obtained through action execution or other means
            // For now, we provide basic information
            // To get format details, consider using aggregate_execute_action with a test call
            // or checking the action definition through reflection if needed
            
            return result;
        } catch (ContextException e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to get action info: " + e.getMessage()
            );
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to get action info: " + errorMessage
            );
        }
    }
}
