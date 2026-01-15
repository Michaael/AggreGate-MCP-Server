package com.tibbo.aggregate.mcp.tools.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for listing available templates
 */
public class ListTemplatesTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_list_templates";
    }
    
    @Override
    public String getDescription() {
        return "List available templates";
    }
    
    @Override
    public JsonNode getInputSchema() {
        com.fasterxml.jackson.databind.node.ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        com.fasterxml.jackson.databind.node.ObjectNode properties = instance.objectNode();
        
        com.fasterxml.jackson.databind.node.ObjectNode parentPath = instance.objectNode();
        parentPath.put("type", "string");
        parentPath.put("description", "Parent context path (optional, if not specified returns all templates)");
        properties.set("parentPath", parentPath);
        
        com.fasterxml.jackson.databind.node.ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode());
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
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
            String parentPath = params.has("parentPath") ? ContextPathParser.parsePath(params.get("parentPath").asText()) : null;
            
            // Templates in AggreGate are typically stored in a special context or accessed through actions
            ArrayNode result = instance.arrayNode();
            
            // Note: Actual implementation depends on AggreGate API
            // Templates may be stored in a special context like "templates" or accessed through "listTemplates" action
            // For now, return empty array with note that action execution may be needed
            
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to list templates: " + errorMessage
            );
        }
    }
}
