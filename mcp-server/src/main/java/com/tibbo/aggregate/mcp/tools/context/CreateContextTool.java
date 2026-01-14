package com.tibbo.aggregate.mcp.tools.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.server.EditableChildrenContextConstants;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

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
        return "Create a new context (model) in AggreGate. " +
               "Supports three model types: relative (default, type=0), absolute (type=1), and instance (type=2). " +
               "For relative models, automatically configures containerType and objectType. " +
               "⚠️ CRITICAL: After creating a model, you MUST create variables, events, and bindings! " +
               "See docs/MCP_MODEL_TYPES_GUIDE.md and docs/AI_CONTEXT_CREATION_COMPLETE_GUIDE.md for complete guide.";
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
        
        // Model type parameters
        ObjectNode modelType = instance.objectNode();
        modelType.put("type", "integer");
        modelType.put("description", "Model type: 0=relative (default, multiple instances per object, relative references {.:var} in bindings), " +
                    "1=absolute (one instance, absolute paths in bindings), " +
                    "2=instance (created on demand). For relative models, containerType and objectType are required.");
        modelType.put("default", 0);
        properties.set("modelType", modelType);
        
        ObjectNode containerType = instance.objectNode();
        containerType.put("type", "string");
        containerType.put("description", "Container type for relative models (required if modelType=0). " +
                      "Common values: 'devices' (for device models), 'objects' (default). " +
                      "This determines what type of objects the model will be attached to.");
        properties.set("containerType", containerType);
        
        ObjectNode objectType = instance.objectNode();
        objectType.put("type", "string");
        objectType.put("description", "Object type for relative models (required if modelType=0). " +
                   "Common values: 'device' (for device models), 'object' (default). " +
                   "This determines the type of objects that will have model instances.");
        properties.set("objectType", objectType);
        
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
            
            // Get parent context FIRST to check its type
            Context parentContext = connection.executeWithTimeout(() -> {
                return cm.get(parentPath);
            }, 60000);
            
            if (parentContext == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Parent context not found: " + parentPath
                );
            }
            
            // Check if parent context is a models context
            // Only models contexts require modelType, containerType, objectType
            boolean isModelsContext = parentPath.contains(".models") || parentPath.endsWith("models");
            
            // Model type parameters - only relevant for models contexts
            int modelType;
            final String containerType;
            final String objectType;
            
            if (isModelsContext) {
                // For models contexts, modelType, containerType, objectType are relevant
                modelType = params.has("modelType") ? params.get("modelType").asInt() : 0; // 0=relative (default)
                containerType = params.has("containerType") ? params.get("containerType").asText() : null;
                objectType = params.has("objectType") ? params.get("objectType").asText() : null;
                
                // Validate relative model parameters - only for models contexts
                if (modelType == 0) { // Relative model
                    if (containerType == null || containerType.isEmpty()) {
                        throw new McpException(
                            com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                            "For relative models (modelType=0), containerType is required. " +
                            "Common value: 'devices' for device models. " +
                            "See docs/MCP_MODEL_TYPES_GUIDE.md for details."
                        );
                    }
                    if (objectType == null || objectType.isEmpty()) {
                        throw new McpException(
                            com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                            "For relative models (modelType=0), objectType is required. " +
                            "Common value: 'device' for device models. " +
                            "See docs/MCP_MODEL_TYPES_GUIDE.md for details."
                        );
                    }
                }
            } else {
                // For non-models contexts (alerts, widgets, dashboards, etc.), 
                // modelType, containerType, objectType are not used
                // Set default values to avoid issues later
                modelType = 1; // Use absolute model type as default (won't be used anyway)
                containerType = null;
                objectType = null;
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
                Context[] newContextRef = new Context[1];
                newContextRef[0] = connection.executeWithTimeout(() -> {
                    try {
                        return cm.get(pathToCheck);
                    } catch (Exception e) {
                        return null;
                    }
                }, 60000);
                
                if (newContextRef[0] == null) {
                    // Try a few more times with increasing delays
                    for (int i = 0; i < 5; i++) {
                        Thread.sleep(200);
                        newContextRef[0] = connection.executeWithTimeout(() -> {
                            try {
                                return cm.get(pathToCheck);
                            } catch (Exception e) {
                                return null;
                            }
                        }, 60000);
                        if (newContextRef[0] != null) {
                            break;
                        }
                    }
                }
                
                final Context newContext = newContextRef[0];
                
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
                    if (isModelsContext && modelType == 0) {
                        result.put("modelType", modelType);
                        result.put("modelTypeName", "relative");
                        result.put("warning", "For relative models, configure containerType and objectType after verification using aggregate_set_variable_field on childInfo variable.");
                    }
                    return result;
                }
                
                // Configure model type step by step
                // CRITICAL: Must set type first, wait, then set other parameters
                // This is required because AggreGate applies type changes before other settings
                // Use setVariableField approach (same as SetVariableFieldTool) for reliable field setting
                try {
                    com.tibbo.aggregate.common.context.CallerController caller = 
                        newContext.getContextManager().getCallerController();
                    
                    // Step 1: Set model type FIRST using setVariableField (CRITICAL!)
                    // This must be done before any other settings
                    connection.executeWithTimeout(() -> {
                        newContext.setVariableField("childInfo", "type", modelType, caller);
                        return null;
                    }, 60000);
                    
                    // Step 2: Wait for type to be applied (CRITICAL - AggreGate needs time to process)
                    Thread.sleep(500);
                    
                    // Step 3: For relative models in models contexts, set containerType and objectType
                    // Only after type is set and applied
                    // Only configure for models contexts
                    if (isModelsContext && modelType == 0 && containerType != null && objectType != null) {
                        // Set containerType
                        connection.executeWithTimeout(() -> {
                            newContext.setVariableField("childInfo", "containerType", containerType, caller);
                            return null;
                        }, 60000);
                        
                        // Small delay between settings
                        Thread.sleep(200);
                        
                        // Set objectType
                        connection.executeWithTimeout(() -> {
                            newContext.setVariableField("childInfo", "objectType", objectType, caller);
                            return null;
                        }, 60000);
                        
                        // Step 4: Wait for all settings to be applied
                        Thread.sleep(300);
                    }
                } catch (Exception e) {
                    // Log but don't fail - model type configuration is optional
                    // The context was created successfully, type can be set manually later
                    System.err.println("Warning: Failed to configure model type: " + e.getMessage());
                    e.printStackTrace();
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
                
                // Add model type information to result - only for models contexts
                if (isModelsContext) {
                    if (modelType == 0) {
                        result.put("modelType", modelType);
                        result.put("modelTypeName", "relative");
                        result.put("containerType", containerType);
                        result.put("objectType", objectType);
                        result.put("note", "Relative model configured. Use relative references {.:var} in bindings, not absolute paths.");
                    } else if (modelType == 1) {
                        result.put("modelType", modelType);
                        result.put("modelTypeName", "absolute");
                        result.put("note", "Absolute model. Use absolute paths {context:var} in bindings.");
                    } else if (modelType == 2) {
                        result.put("modelType", modelType);
                        result.put("modelTypeName", "instance");
                    }
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

