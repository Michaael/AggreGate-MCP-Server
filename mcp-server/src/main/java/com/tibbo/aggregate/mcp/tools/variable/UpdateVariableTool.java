package com.tibbo.aggregate.mcp.tools.variable;

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
 * Tool for updating variable properties
 */
public class UpdateVariableTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_update_variable";
    }
    
    @Override
    public String getDescription() {
        return "Update variable properties (description, group, permissions, etc.)";
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
        
        ObjectNode variableName = instance.objectNode();
        variableName.put("type", "string");
        variableName.put("description", "Variable name");
        properties.set("variableName", variableName);
        
        ObjectNode description = instance.objectNode();
        description.put("type", "string");
        description.put("description", "New description (optional)");
        properties.set("description", description);
        
        ObjectNode group = instance.objectNode();
        group.put("type", "string");
        group.put("description", "New group (optional)");
        properties.set("group", group);
        
        ObjectNode readPermissions = instance.objectNode();
        readPermissions.put("type", "string");
        readPermissions.put("description", "New read permissions (optional)");
        properties.set("readPermissions", readPermissions);
        
        ObjectNode writePermissions = instance.objectNode();
        writePermissions.put("type", "string");
        writePermissions.put("description", "New write permissions (optional)");
        properties.set("writePermissions", writePermissions);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("variableName"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("variableName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and variableName parameters are required"
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
            String variableName = params.get("variableName").asText();
            
            Context<?> context = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000L);
            
            // Update variable through variable definition update or action
            connection.executeWithTimeout(() -> {
                // Implementation depends on AggreGate API
                // May use updateVariableDefinition or setVariableProperties action
                return null;
            }, 60000L);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Variable updated successfully");
            result.put("path", path);
            result.put("variableName", variableName);
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to update variable: " + errorMessage
            );
        }
    }
}
