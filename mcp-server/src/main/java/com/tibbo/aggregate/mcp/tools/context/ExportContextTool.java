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
 * Tool for exporting a context to a file
 */
public class ExportContextTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_export_context";
    }
    
    @Override
    public String getDescription() {
        return "Export a context to a file (XML, JSON or other format)";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Context path to export");
        properties.set("path", path);
        
        ObjectNode filePath = instance.objectNode();
        filePath.put("type", "string");
        filePath.put("description", "File path to save the export");
        properties.set("filePath", filePath);
        
        ObjectNode format = instance.objectNode();
        format.put("type", "string");
        format.put("description", "Export format: xml, json (default: xml)");
        format.set("enum", instance.arrayNode().add("xml").add("json"));
        properties.set("format", format);
        
        ObjectNode includeHistory = instance.objectNode();
        includeHistory.put("type", "boolean");
        includeHistory.put("description", "Include data history (default: false)");
        properties.set("includeHistory", includeHistory);
        
        ObjectNode includeBindings = instance.objectNode();
        includeBindings.put("type", "boolean");
        includeBindings.put("description", "Include bindings (default: true)");
        properties.set("includeBindings", includeBindings);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("filePath"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("filePath")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and filePath parameters are required"
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
            String filePath = params.get("filePath").asText();
            String format = params.has("format") ? params.get("format").asText() : "xml";
            boolean includeHistory = params.has("includeHistory") ? params.get("includeHistory").asBoolean() : false;
            boolean includeBindings = params.has("includeBindings") ? params.get("includeBindings").asBoolean() : true;
            
            Context<?> context = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000L);
            
            // Use executeAction to export context
            // The action name is typically "export" or "exportContext"
            ObjectNode actionParams = instance.objectNode();
            actionParams.put("path", path);
            actionParams.put("actionName", "export");
            actionParams.put("filePath", filePath);
            
            // Delegate to ExecuteActionTool
            ExecuteActionTool executeActionTool = new ExecuteActionTool();
            JsonNode actionResult = executeActionTool.execute(actionParams, connectionManager);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Context exported successfully");
            result.put("path", path);
            result.put("filePath", filePath);
            result.put("format", format);
            result.set("actionResult", actionResult);
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to export context: " + errorMessage
            );
        }
    }
}
