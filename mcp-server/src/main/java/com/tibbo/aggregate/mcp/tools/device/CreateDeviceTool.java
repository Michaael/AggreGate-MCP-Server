package com.tibbo.aggregate.mcp.tools.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for creating a device
 */
public class CreateDeviceTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_create_device";
    }
    
    @Override
    public String getDescription() {
        return "Create a new device account";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode username = instance.objectNode();
        username.put("type", "string");
        username.put("description", "Username who owns the device");
        properties.set("username", username);
        
        ObjectNode deviceName = instance.objectNode();
        deviceName.put("type", "string");
        deviceName.put("description", "Device name");
        properties.set("deviceName", deviceName);
        
        ObjectNode description = instance.objectNode();
        description.put("type", "string");
        description.put("description", "Device description");
        properties.set("description", description);
        
        ObjectNode driverId = instance.objectNode();
        driverId.put("type", "string");
        driverId.put("description", "Device driver ID (e.g., 'com.tibbo.linkserver.plugin.device.virtual')");
        properties.set("driverId", driverId);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("username").add("deviceName").add("description").add("driverId"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("username") || !params.has("deviceName") || !params.has("description") || !params.has("driverId")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Username, deviceName, description, and driverId parameters are required"
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
            String username = params.get("username").asText();
            String deviceName = params.get("deviceName").asText();
            String description = params.get("description").asText();
            String driverId = params.get("driverId").asText();
            
            ContextManager cm = connection.getContextManager();
            String devicesContextPath = ContextUtils.devicesContextPath(username);
            
            // Try to get devices context, create if it doesn't exist
            Context devicesContext = connection.executeWithTimeout(() -> {
                try {
                    return cm.get(devicesContextPath);
                } catch (Exception e) {
                    // If devices context doesn't exist, try to get user context and create devices context
                    try {
                        String userContextPath = ContextUtils.userContextPath(username);
                        Context userContext = cm.get(userContextPath);
                        if (userContext != null) {
                            // Try to create devices context by calling create function
                            try {
                                userContext.callFunction("create", "devices", "Devices");
                                return cm.get(devicesContextPath);
                            } catch (Exception ex) {
                                // If create doesn't work, devices context should exist automatically
                                // Try getting it again
                                return cm.get(devicesContextPath);
                            }
                        }
                        throw new RuntimeException("User context not found: " + userContextPath);
                    } catch (Exception ex) {
                        throw new RuntimeException("Failed to get or create devices context: " + e.getMessage(), ex);
                    }
                }
            }, 60000);
            
            if (devicesContext == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Devices context not found and could not be created: " + devicesContextPath
                );
            }
            
            connection.executeWithTimeout(() -> {
                try {
                    devicesContext.callFunction("add", driverId, deviceName, description);
                    return null;
                } catch (ContextException e) {
                    throw new RuntimeException("Failed to call add function: " + e.getMessage(), e);
                }
            }, 60000);
            
            String deviceContextPath = ContextUtils.deviceContextPath(username, deviceName);
            Context deviceContext = connection.executeWithTimeout(() -> {
                try {
                    return cm.get(deviceContextPath);
                } catch (Exception e) {
                    throw new RuntimeException("Device context not found after creation: " + deviceContextPath, e);
                }
            }, 60000);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Device created successfully");
            result.put("path", deviceContext.getPath());
            result.put("name", deviceContext.getName());
            
            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            com.tibbo.aggregate.mcp.util.ErrorHandler.ErrorDetails errorDetails = 
                com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorDetails(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to create device: " + errorMessage,
                errorDetails
            );
        }
    }
}

