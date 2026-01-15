package com.tibbo.aggregate.mcp.tools.template;

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
 * Tool for creating a template from a context
 */
public class CreateTemplateTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_create_template";
    }
    
    @Override
    public String getDescription() {
        return "Create a template from a context for reuse";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Context path to use as template");
        properties.set("path", path);
        
        ObjectNode templateName = instance.objectNode();
        templateName.put("type", "string");
        templateName.put("description", "Template name");
        properties.set("templateName", templateName);
        
        ObjectNode description = instance.objectNode();
        description.put("type", "string");
        description.put("description", "Template description (optional)");
        properties.set("description", description);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("templateName"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("templateName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and templateName parameters are required"
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
            String templateName = params.get("templateName").asText();
            String description = params.has("description") ? params.get("description").asText() : null;
            
            Context<?> context = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000L);
            
            // Create template using executeAction
            // Templates in AggreGate are typically created through actions
            connection.executeWithTimeout(() -> {
                try {
                    com.tibbo.aggregate.common.datatable.DataTable input = 
                        new com.tibbo.aggregate.common.datatable.SimpleDataTable(
                            "<name><S><description><S>",
                            true
                        );
                    com.tibbo.aggregate.common.datatable.DataRecord inputRec = input.addRecord();
                    inputRec.setValue("name", templateName);
                    inputRec.setValue("description", description != null ? description : "");
                    
                    // Use ActionUtils to execute the action
                    com.tibbo.aggregate.common.action.ServerActionInput actionInput = 
                        com.tibbo.aggregate.common.action.ActionUtils.createActionInput(input);
                    
                    // Try different action names
                    String[] actionNames = {"createTemplate", "addTemplate", "saveAsTemplate"};
                    Exception lastException = null;
                    
                    for (String actionName : actionNames) {
                        try {
                            com.tibbo.aggregate.common.action.ActionIdentifier actionId = 
                                com.tibbo.aggregate.common.action.ActionUtils.initAction(
                                    context,
                                    actionName,
                                    actionInput,
                                    null,
                                    new com.tibbo.aggregate.common.action.ActionExecutionMode(
                                        com.tibbo.aggregate.common.action.ActionExecutionMode.HEADLESS
                                    ),
                                    null
                                );
                            return null;
                        } catch (Exception e) {
                            lastException = e;
                        }
                    }
                    
                    throw new RuntimeException("Failed to create template. Tried actions: createTemplate, addTemplate, saveAsTemplate. " +
                        "Error: " + (lastException != null ? lastException.getMessage() : "Unknown error"));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create template: " + e.getMessage(), e);
                }
            }, 60000L);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Template created successfully");
            result.put("path", path);
            result.put("templateName", templateName);
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to create template: " + errorMessage
            );
        }
    }
}
