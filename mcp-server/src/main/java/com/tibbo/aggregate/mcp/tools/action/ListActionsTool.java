package com.tibbo.aggregate.mcp.tools.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.action.ActionDefinition;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import java.util.List;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for listing available actions in a context
 */
public class ListActionsTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_list_actions";
    }
    
    @Override
    public String getDescription() {
        return "List all available actions in a context";
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
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path parameter is required"
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
            
            Context<?> context = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000L);
            
            @SuppressWarnings("unchecked")
            List<ActionDefinition> actions = connection.executeWithTimeout(() -> {
                return (List<ActionDefinition>) context.getActionDefinitions();
            }, 60000L);
            
            ArrayNode result = instance.arrayNode();
            if (actions != null) {
                for (ActionDefinition ad : actions) {
                    if (ad == null) {
                        continue;
                    }
                    ObjectNode action = instance.objectNode();
                    action.put("name", ad.getName() != null ? ad.getName() : "");
                    action.put("description", ad.getDescription() != null ? ad.getDescription() : "");
                    action.put("group", ad.getGroup() != null ? ad.getGroup() : "");
                    
                    // Note: ActionDefinition may not have direct getInputFormat/getOutputFormat methods
                    // Format information may need to be obtained through other means or action execution
                    // For now, we provide basic information
                    
                    result.add(action);
                }
            }
            
            return result;
        } catch (ContextException e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to list actions: " + e.getMessage()
            );
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to list actions: " + errorMessage
            );
        }
    }
}
