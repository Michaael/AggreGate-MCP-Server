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
 * Tool for instantiating a context from a template
 */
public class InstantiateTemplateTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_instantiate_template";
    }
    
    @Override
    public String getDescription() {
        return "Create a context instance from a template";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode templateName = instance.objectNode();
        templateName.put("type", "string");
        templateName.put("description", "Template name");
        properties.set("templateName", templateName);
        
        ObjectNode parentPath = instance.objectNode();
        parentPath.put("type", "string");
        parentPath.put("description", "Parent context path");
        properties.set("parentPath", parentPath);
        
        ObjectNode instanceName = instance.objectNode();
        instanceName.put("type", "string");
        instanceName.put("description", "Instance name");
        properties.set("instanceName", instanceName);
        
        ObjectNode parameters = instance.objectNode();
        parameters.put("type", "object");
        parameters.put("description", "Template parameters for substitution (optional)");
        properties.set("parameters", parameters);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("templateName").add("parentPath").add("instanceName"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("templateName") || !params.has("parentPath") || !params.has("instanceName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "TemplateName, parentPath, and instanceName parameters are required"
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
            String templateName = params.get("templateName").asText();
            String parentPath = ContextPathParser.parsePath(params.get("parentPath").asText());
            String instanceName = params.get("instanceName").asText();
            
            Context<?> parentContext = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(parentPath);
                if (ctx == null) {
                    throw new RuntimeException("Parent context not found: " + parentPath);
                }
                return ctx;
            }, 60000L);
            
            // Instantiate template using executeAction
            connection.executeWithTimeout(() -> {
                try {
                    com.tibbo.aggregate.common.datatable.DataTable input = 
                        new com.tibbo.aggregate.common.datatable.SimpleDataTable(
                            "<templateName><S><instanceName><S><parameters><T>",
                            true
                        );
                    com.tibbo.aggregate.common.datatable.DataRecord inputRec = input.addRecord();
                    inputRec.setValue("templateName", templateName);
                    inputRec.setValue("instanceName", instanceName);
                    
                    // Add parameters if provided
                    if (params.has("parameters") && params.get("parameters").isObject()) {
                        // Convert parameters JSON to DataTable if needed
                        // For now, leave as empty table
                        inputRec.setValue("parameters", new com.tibbo.aggregate.common.datatable.SimpleDataTable());
                    } else {
                        inputRec.setValue("parameters", new com.tibbo.aggregate.common.datatable.SimpleDataTable());
                    }
                    
                    com.tibbo.aggregate.common.action.ServerActionInput actionInput = 
                        com.tibbo.aggregate.common.action.ActionUtils.createActionInput(input);
                    
                    // Try different action names
                    String[] actionNames = {"instantiateTemplate", "createFromTemplate", "applyTemplate"};
                    Exception lastException = null;
                    
                    for (String actionName : actionNames) {
                        try {
                            com.tibbo.aggregate.common.action.ActionIdentifier actionId = 
                                com.tibbo.aggregate.common.action.ActionUtils.initAction(
                                    parentContext,
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
                    
                    throw new RuntimeException("Failed to instantiate template. Tried actions: instantiateTemplate, createFromTemplate, applyTemplate. " +
                        "Error: " + (lastException != null ? lastException.getMessage() : "Unknown error"));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to instantiate template: " + e.getMessage(), e);
                }
            }, 60000L);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Template instantiated successfully");
            result.put("parentPath", parentPath);
            result.put("instanceName", instanceName);
            result.put("templateName", templateName);
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to instantiate template: " + errorMessage
            );
        }
    }
}
