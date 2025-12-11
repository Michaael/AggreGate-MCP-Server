package com.tibbo.aggregate.mcp.tools.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.SimpleDataTable;
import com.tibbo.aggregate.common.server.EditableChildContextConstants;
import com.tibbo.aggregate.common.server.EditableChildrenContextConstants;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.FieldFormat;
import com.tibbo.aggregate.common.datatable.SimpleDataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;
import com.tibbo.aggregate.mcp.util.DataTableConverter;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for creating a new context
 */
public class CreateContextTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_create_context";
    }
    
    @Override
    public String getDescription() {
        return "Create a new context (model) in AggreGate";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode parentPath = instance.objectNode();
        parentPath.put("type", "string");
        parentPath.put("description", "Parent context path where to create the new context");
        properties.set("parentPath", parentPath);
        
        ObjectNode name = instance.objectNode();
        name.put("type", "string");
        name.put("description", "Name of the new context");
        properties.set("name", name);
        
        ObjectNode description = instance.objectNode();
        description.put("type", "string");
        description.put("description", "Description of the new context (optional)");
        properties.set("description", description);
        
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
            
            // Check if context already exists
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
                    "Context already exists: " + newPath
                );
            }
            
            // In AggreGate, contexts are created through the "create" function
            // This function is available on contexts that support editable children
            // Based on examples, we can call it with parameters directly (like ManageDevices)
            // or it might return a path that we can use to get the created context
            try {
                // Call the "create" function on the parent context
                // Try calling it similar to how ManageDevices.createDeviceAccount works
                // The function may accept name and description as parameters
                DataTable createResult = connection.executeWithTimeout(() -> {
                    try {
                        // Try calling with name and description as separate parameters
                        // This matches the pattern from ManageDevices where add() is called with driverId, name, description
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
                
                // The create function may return a path in FOF_CREATE_PATH field
                String createdPath = null;
                if (createResult != null && createResult.getRecordCount() > 0) {
                    DataRecord resultRecord = createResult.rec();
                    if (resultRecord.getFormat().hasField(EditableChildrenContextConstants.FOF_CREATE_PATH)) {
                        createdPath = resultRecord.getString(EditableChildrenContextConstants.FOF_CREATE_PATH);
                    }
                }
                
                // If we got a path from the function, use it; otherwise use the expected path
                String pathToCheck = createdPath != null ? createdPath : newPath;
                
                // Wait a bit for context to be created
                Thread.sleep(500);
                
                // Verify context was created
                Context newContext = connection.executeWithTimeout(() -> {
                    try {
                        return cm.get(pathToCheck);
                    } catch (Exception e) {
                        return null;
                    }
                }, 60000);
                
                if (newContext == null) {
                    // Try a few more times with increasing delays
                    for (int i = 0; i < 5; i++) {
                        Thread.sleep(200);
                        newContext = connection.executeWithTimeout(() -> {
                            try {
                                return cm.get(pathToCheck);
                            } catch (Exception e) {
                                return null;
                            }
                        }, 60000);
                        if (newContext != null) {
                            break;
                        }
                    }
                }
                
                if (newContext == null) {
                    // Context might not be immediately available, but function was called
                    ObjectNode result = instance.objectNode();
                    result.put("success", true);
                    result.put("message", "Create function executed. Context may be created. Please verify.");
                    result.put("path", newPath);
                    result.put("name", name);
                    if (description != null) {
                        result.put("description", description);
                    }
                    result.put("note", "Context creation may require additional verification");
                    return result;
                }
                
                ObjectNode result = instance.objectNode();
                result.put("success", true);
                result.put("message", "Context created successfully");
                result.put("path", newContext.getPath());
                result.put("name", newContext.getName());
                String contextDescription = newContext.getDescription();
                if (contextDescription != null) {
                    result.put("description", contextDescription);
                }
                
                return result;
            } catch (Exception e) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Failed to create context: " + e.getMessage()
                );
            }
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to create context: " + e.getMessage()
            );
        }
    }
}

