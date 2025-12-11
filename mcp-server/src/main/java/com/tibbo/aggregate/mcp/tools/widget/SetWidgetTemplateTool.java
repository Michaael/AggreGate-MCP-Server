package com.tibbo.aggregate.mcp.tools.widget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.SimpleDataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.server.WidgetContextConstants;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for setting widget template XML
 */
public class SetWidgetTemplateTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_set_widget_template";
    }
    
    @Override
    public String getDescription() {
        return "Set XML template for a widget";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Widget context path (e.g., 'users.admin.widgets.myWidget')");
        properties.set("path", path);
        
        ObjectNode template = instance.objectNode();
        template.put("type", "string");
        template.put("description", "Widget XML template");
        properties.set("template", template);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("template"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("template")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and template parameters are required"
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
            String template = params.get("template").asText();
            
            Context widgetContext = connection.getContextManager().get(path);
            if (widgetContext == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Widget context not found: " + path
                );
            }
            
            connection.executeWithTimeout(() -> {
                try {
                    com.tibbo.aggregate.common.context.CallerController caller = 
                        widgetContext.getContextManager().getCallerController();
                    // Try to set the value field directly
                    try {
                        widgetContext.setVariableField(
                            WidgetContextConstants.V_TEMPLATE, 
                            WidgetContextConstants.F_VALUE, 
                            0, 
                            template, 
                            caller
                        );
                    } catch (ContextException e) {
                        // If that fails, try setting the whole variable
                        DataTable templateTable = new SimpleDataTable(
                            new TableFormat(1, 1, 
                                "<" + WidgetContextConstants.F_VALUE + "><S>"),
                            template
                        );
                        widgetContext.setVariable(WidgetContextConstants.V_TEMPLATE, caller, templateTable);
                    }
                    return null;
                } catch (ContextException e) {
                    throw new RuntimeException("Failed to set template: " + e.getMessage(), e);
                }
            }, 60000);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Widget template set successfully");
            result.put("path", path);
            
            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to set widget template: " + e.getMessage()
            );
        }
    }
}

