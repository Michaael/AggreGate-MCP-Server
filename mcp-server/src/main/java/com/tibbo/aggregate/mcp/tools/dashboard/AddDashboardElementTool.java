package com.tibbo.aggregate.mcp.tools.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.server.DashboardContextConstants;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;
import com.tibbo.aggregate.mcp.util.DataTableConverter;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for adding elements to a dashboard
 */
public class AddDashboardElementTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_add_dashboard_element";
    }
    
    @Override
    public String getDescription() {
        return "Add an element to a dashboard";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Dashboard context path (e.g., 'users.admin.dashboards.myDashboard')");
        properties.set("path", path);
        
        ObjectNode name = instance.objectNode();
        name.put("type", "string");
        name.put("description", "Element name");
        properties.set("name", name);
        
        ObjectNode type = instance.objectNode();
        type.put("type", "string");
        type.put("description", "Element type (e.g., 'launchWidget', 'showEventLog', 'panel', etc.)");
        properties.set("type", type);
        
        ObjectNode parameters = instance.objectNode();
        parameters.put("type", "object");
        parameters.put("description", "Element parameters as DataTable JSON");
        properties.set("parameters", parameters);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("name").add("type"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("name") || !params.has("type")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path, name, and type parameters are required"
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
            String elementName = params.get("name").asText();
            String elementType = params.get("type").asText();
            
            Context dashboardContext = connection.getContextManager().get(path);
            if (dashboardContext == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Dashboard context not found: " + path
                );
            }
            
            // Get existing elements or create format if empty
            DataTable existingElements = connection.executeWithTimeout(() -> {
                try {
                    DataTable elements = dashboardContext.getVariable(DashboardContextConstants.V_ELEMENTS, null);
                    if (elements == null || elements.getRecordCount() == 0) {
                        // Create format for elements if empty
                        com.tibbo.aggregate.common.datatable.TableFormat elementsFormat = 
                            new com.tibbo.aggregate.common.datatable.TableFormat(0, Integer.MAX_VALUE);
                        elementsFormat.addField("<" + DashboardContextConstants.ELEMENT_FIELD_NAME + "><S>");
                        elementsFormat.addField("<" + DashboardContextConstants.ELEMENT_FIELD_TYPE + "><S>");
                        elementsFormat.addField("<" + DashboardContextConstants.ELEMENT_FIELD_PARAMETERS + "><T><F=N>");
                        return new com.tibbo.aggregate.common.datatable.SimpleDataTable(elementsFormat);
                    }
                    return elements;
                } catch (ContextException e) {
                    throw new RuntimeException("Failed to get elements: " + e.getMessage(), e);
                }
            }, 60000);
            
            // Create new element record
            com.tibbo.aggregate.common.datatable.DataRecord newElement = 
                new com.tibbo.aggregate.common.datatable.DataRecord(existingElements.getFormat());
            newElement.setValue(DashboardContextConstants.ELEMENT_FIELD_NAME, elementName);
            newElement.setValue(DashboardContextConstants.ELEMENT_FIELD_TYPE, elementType);
            
            // Set parameters if provided
            if (params.has("parameters") && params.get("parameters").isObject()) {
                DataTable parametersTable = DataTableConverter.fromJson(params.get("parameters"));
                newElement.setValue(DashboardContextConstants.ELEMENT_FIELD_PARAMETERS, parametersTable);
            } else {
                // Set empty parameters table if not provided
                com.tibbo.aggregate.common.datatable.TableFormat emptyFormat = 
                    new com.tibbo.aggregate.common.datatable.TableFormat(0, Integer.MAX_VALUE);
                newElement.setValue(DashboardContextConstants.ELEMENT_FIELD_PARAMETERS, 
                    new com.tibbo.aggregate.common.datatable.SimpleDataTable(emptyFormat));
            }
            
            // Add new element to existing elements
            DataTable updatedElements = existingElements.clone();
            updatedElements.addRecord(newElement);
            
            // Set updated elements back
            connection.executeWithTimeout(() -> {
                try {
                    com.tibbo.aggregate.common.context.CallerController caller = 
                        dashboardContext.getContextManager().getCallerController();
                    dashboardContext.setVariable(DashboardContextConstants.V_ELEMENTS, caller, updatedElements);
                    return null;
                } catch (ContextException e) {
                    throw new RuntimeException("Failed to set elements: " + e.getMessage(), e);
                }
            }, 60000);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Dashboard element added successfully");
            result.put("path", path);
            result.put("elementName", elementName);
            
            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to add dashboard element: " + e.getMessage()
            );
        }
    }
}

