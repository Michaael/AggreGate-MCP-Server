package com.tibbo.aggregate.mcp.tools.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.tools.action.ExecuteActionTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for importing a context from a file
 */
public class ImportContextTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_import_context";
    }
    
    @Override
    public String getDescription() {
        return "Import a context from a file";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode filePath = instance.objectNode();
        filePath.put("type", "string");
        filePath.put("description", "File path to import from");
        properties.set("filePath", filePath);
        
        ObjectNode parentPath = instance.objectNode();
        parentPath.put("type", "string");
        parentPath.put("description", "Parent context path");
        properties.set("parentPath", parentPath);
        
        ObjectNode contextName = instance.objectNode();
        contextName.put("type", "string");
        contextName.put("description", "Context name (optional, if not specified taken from file)");
        properties.set("contextName", contextName);
        
        ObjectNode overwrite = instance.objectNode();
        overwrite.put("type", "boolean");
        overwrite.put("description", "Overwrite existing context (default: false)");
        properties.set("overwrite", overwrite);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("filePath").add("parentPath"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("filePath") || !params.has("parentPath")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "FilePath and parentPath parameters are required"
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
            String filePath = params.get("filePath").asText();
            String parentPath = ContextPathParser.parsePath(params.get("parentPath").asText());
            String contextName = params.has("contextName") ? params.get("contextName").asText() : null;
            boolean overwrite = params.has("overwrite") ? params.get("overwrite").asBoolean() : false;
            
            Context<?> parentContext = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(parentPath);
                if (ctx == null) {
                    throw new RuntimeException("Parent context not found: " + parentPath);
                }
                return ctx;
            }, 60000L);
            
            // Use executeAction to import context
            // The action name is typically "import" or "importContext"
            ObjectNode actionParams = instance.objectNode();
            actionParams.put("path", parentPath);
            actionParams.put("actionName", "import");
            actionParams.put("filePath", filePath);
            if (contextName != null) {
                actionParams.put("contextName", contextName);
            }
            if (overwrite) {
                actionParams.put("overwrite", overwrite);
            }
            
            // Delegate to ExecuteActionTool
            ExecuteActionTool executeActionTool = new ExecuteActionTool();
            JsonNode actionResult = executeActionTool.execute(actionParams, connectionManager);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Context imported successfully");
            result.put("parentPath", parentPath);
            result.put("filePath", filePath);
            if (contextName != null) {
                result.put("contextName", contextName);
            }
            result.set("actionResult", actionResult);
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to import context: " + errorMessage
            );
        }
    }
}
