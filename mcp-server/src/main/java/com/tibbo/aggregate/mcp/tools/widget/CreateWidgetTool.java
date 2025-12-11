package com.tibbo.aggregate.mcp.tools.widget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.server.EditableChildrenContextConstants;
import com.tibbo.aggregate.common.server.WidgetContextConstants;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for creating a new widget context
 */
public class CreateWidgetTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_create_widget";
    }
    
    @Override
    public String getDescription() {
        return "Create a new widget context in AggreGate";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode parentPath = instance.objectNode();
        parentPath.put("type", "string");
        parentPath.put("description", "Parent widgets context path (e.g., 'users.admin.widgets')");
        properties.set("parentPath", parentPath);
        
        ObjectNode name = instance.objectNode();
        name.put("type", "string");
        name.put("description", "Name of the new widget");
        properties.set("name", name);
        
        ObjectNode description = instance.objectNode();
        description.put("type", "string");
        description.put("description", "Description of the widget (optional)");
        properties.set("description", description);
        
        ObjectNode template = instance.objectNode();
        template.put("type", "string");
        template.put("description", "Widget template XML (optional, can be set later)");
        properties.set("template", template);
        
        ObjectNode defaultContext = instance.objectNode();
        defaultContext.put("type", "string");
        defaultContext.put("description", "Default context for the widget (optional)");
        properties.set("defaultContext", defaultContext);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("parentPath").add("name"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("parentPath") || !params.has("name")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "ParentPath and name parameters are required"
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
            String parentPath = ContextPathParser.parsePath(params.get("parentPath").asText());
            String name = params.get("name").asText();
            String description = params.has("description") ? params.get("description").asText() : null;
            String template = params.has("template") ? params.get("template").asText() : null;
            String defaultContext = params.has("defaultContext") ? params.get("defaultContext").asText() : null;
            
            ContextManager cm = connection.getContextManager();
            
            // Get parent context
            Context parentContext = connection.executeWithTimeout(() -> {
                return cm.get(parentPath);
            }, 60000);
            
            if (parentContext == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Parent context not found: " + parentPath
                );
            }
            
            // Check if widget already exists
            String newPath = parentPath + "." + name;
            Context existingContext = connection.executeWithTimeout(() -> {
                try {
                    return cm.get(newPath);
                } catch (Exception e) {
                    return null;
                }
            }, 60000);
            
            if (existingContext != null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Widget already exists: " + newPath
                );
            }
            
            // Create widget context using the "create" function
            DataTable createResult = connection.executeWithTimeout(() -> {
                try {
                    if (description != null) {
                        return parentContext.callFunction(
                            EditableChildrenContextConstants.F_CREATE,
                            name,
                            description
                        );
                    } else {
                        return parentContext.callFunction(
                            EditableChildrenContextConstants.F_CREATE,
                            name
                        );
                    }
                } catch (ContextException e) {
                    throw new RuntimeException("Failed to call create function: " + e.getMessage(), e);
                }
            }, 60000);
            
            // Get the created path
            String createdPath = null;
            if (createResult != null && createResult.getRecordCount() > 0) {
                DataRecord resultRecord = createResult.rec();
                if (resultRecord.getFormat().hasField(EditableChildrenContextConstants.FOF_CREATE_PATH)) {
                    createdPath = resultRecord.getString(EditableChildrenContextConstants.FOF_CREATE_PATH);
                }
            }
            
            String pathToCheck = createdPath != null ? createdPath : newPath;
            
            // Wait for context to be created
            Thread.sleep(500);
            
            // Verify widget was created
            Context widgetContext = connection.executeWithTimeout(() -> {
                try {
                    return cm.get(pathToCheck);
                } catch (Exception e) {
                    return null;
                }
            }, 60000);
            
            if (widgetContext == null) {
                // Try a few more times
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(200);
                    widgetContext = connection.executeWithTimeout(() -> {
                        try {
                            return cm.get(pathToCheck);
                        } catch (Exception e) {
                            return null;
                        }
                    }, 60000);
                    if (widgetContext != null) {
                        break;
                    }
                }
            }
            
            if (widgetContext == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Widget was not created. Path: " + pathToCheck
                );
            }
            
            // Set template if provided
            final Context finalWidgetContext = widgetContext;
            if (template != null && !template.isEmpty()) {
                connection.executeWithTimeout(() -> {
                    try {
                        com.tibbo.aggregate.common.context.CallerController caller = 
                            finalWidgetContext.getContextManager().getCallerController();
                        DataTable templateTable = new com.tibbo.aggregate.common.datatable.SimpleDataTable(
                            new com.tibbo.aggregate.common.datatable.TableFormat(1, 1, 
                                "<" + WidgetContextConstants.F_VALUE + "><S>"),
                            template
                        );
                        finalWidgetContext.setVariable(WidgetContextConstants.V_TEMPLATE, caller, templateTable);
                        return null;
                    } catch (ContextException e) {
                        throw new RuntimeException("Failed to set template: " + e.getMessage(), e);
                    }
                }, 60000);
            }
            
            // Set default context if provided
            if (defaultContext != null && !defaultContext.isEmpty()) {
                connection.executeWithTimeout(() -> {
                    try {
                        com.tibbo.aggregate.common.context.CallerController caller = 
                            finalWidgetContext.getContextManager().getCallerController();
                        finalWidgetContext.setVariable(
                            WidgetContextConstants.FIELD_WIDGET_DEFAULT_CONTEXT,
                            caller,
                            new com.tibbo.aggregate.common.datatable.SimpleDataTable(
                                new com.tibbo.aggregate.common.datatable.TableFormat(1, 1, "<value><S>"),
                                defaultContext
                            )
                        );
                        return null;
                    } catch (ContextException e) {
                        throw new RuntimeException("Failed to set default context: " + e.getMessage(), e);
                    }
                }, 60000);
            }
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Widget created successfully");
            result.put("path", widgetContext.getPath());
            result.put("name", widgetContext.getName());
            String widgetDescription = widgetContext.getDescription();
            if (widgetDescription != null) {
                result.put("description", widgetDescription);
            }
            
            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to create widget: " + e.getMessage()
            );
        }
    }
}

