package com.tibbo.aggregate.mcp.tools.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.server.DashboardContextConstants;
import com.tibbo.aggregate.common.server.EditableChildrenContextConstants;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for creating a new dashboard context
 */
public class CreateDashboardTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_create_dashboard";
    }
    
    @Override
    public String getDescription() {
        return "Create a new dashboard context in AggreGate";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode parentPath = instance.objectNode();
        parentPath.put("type", "string");
        parentPath.put("description", "Parent dashboards context path (e.g., 'users.admin.dashboards')");
        properties.set("parentPath", parentPath);
        
        ObjectNode name = instance.objectNode();
        name.put("type", "string");
        name.put("description", "Name of the new dashboard");
        properties.set("name", name);
        
        ObjectNode description = instance.objectNode();
        description.put("type", "string");
        description.put("description", "Description of the dashboard (optional)");
        properties.set("description", description);
        
        ObjectNode layout = instance.objectNode();
        layout.put("type", "string");
        layout.put("description", "Dashboard layout type: dockable, scrollable, grid, absolute (optional, default: dockable)");
        properties.set("layout", layout);
        
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
            String layoutStr = params.has("layout") ? params.get("layout").asText() : null;
            
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
            
            // Check if dashboard already exists
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
                    "Dashboard already exists: " + newPath
                );
            }
            
            // Create dashboard context using the "create" function
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
            
            // Verify dashboard was created
            Context dashboardContext = connection.executeWithTimeout(() -> {
                try {
                    return cm.get(pathToCheck);
                } catch (Exception e) {
                    return null;
                }
            }, 60000);
            
            if (dashboardContext == null) {
                // Try a few more times
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(200);
                    dashboardContext = connection.executeWithTimeout(() -> {
                        try {
                            return cm.get(pathToCheck);
                        } catch (Exception e) {
                            return null;
                        }
                    }, 60000);
                    if (dashboardContext != null) {
                        break;
                    }
                }
            }
            
            if (dashboardContext == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Dashboard was not created. Path: " + pathToCheck
                );
            }
            
            // Set layout if provided
            final Context finalDashboardContext = dashboardContext;
            if (layoutStr != null && !layoutStr.isEmpty()) {
                connection.executeWithTimeout(() -> {
                    try {
                        com.tibbo.aggregate.common.context.CallerController caller = 
                            finalDashboardContext.getContextManager().getCallerController();
                        DataTable layoutTable = new com.tibbo.aggregate.common.datatable.SimpleDataTable(
                            new com.tibbo.aggregate.common.datatable.TableFormat(1, 1, 
                                "<" + DashboardContextConstants.FIELD_LAYOUT + "><S>"),
                            layoutStr
                        );
                        finalDashboardContext.setVariable(DashboardContextConstants.V_LAYOUT, caller, layoutTable);
                        return null;
                    } catch (ContextException e) {
                        throw new RuntimeException("Failed to set layout: " + e.getMessage(), e);
                    }
                }, 60000);
            }
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Dashboard created successfully");
            result.put("path", dashboardContext.getPath());
            result.put("name", dashboardContext.getName());
            String dashboardDescription = dashboardContext.getDescription();
            if (dashboardDescription != null) {
                result.put("description", dashboardDescription);
            }
            
            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to create dashboard: " + e.getMessage()
            );
        }
    }
}

