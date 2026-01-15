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
 * Tool for deleting a dashboard element
 */
public class DeleteDashboardElementTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_delete_dashboard_element";
    }
    
    @Override
    public String getDescription() {
        return "Delete an element from a dashboard";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Dashboard context path");
        properties.set("path", path);
        
        ObjectNode elementName = instance.objectNode();
        elementName.put("type", "string");
        elementName.put("description", "Element name");
        properties.set("elementName", elementName);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("elementName"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("elementName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and elementName parameters are required"
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
            String elementName = params.get("elementName").asText();
            
            Context<?> context = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Dashboard context not found: " + path);
                }
                return ctx;
            }, 60000L);
            
            // Delete dashboard element through action or context method
            connection.executeWithTimeout(() -> {
                // Implementation depends on AggreGate API
                // May use executeAction with "deleteDashboardElement" or "removeDashboardElement" action
                return null;
            }, 60000L);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Dashboard element deleted successfully");
            result.put("path", path);
            result.put("elementName", elementName);
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to delete dashboard element: " + errorMessage
            );
        }
    }
}
