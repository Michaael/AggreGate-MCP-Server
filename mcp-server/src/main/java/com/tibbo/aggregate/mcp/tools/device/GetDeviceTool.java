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
 * Tool for getting detailed device information
 */
public class GetDeviceTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_get_device";
    }
    
    @Override
    public String getDescription() {
        return "Get detailed information about a device";
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
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("username").add("deviceName"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("username") || !params.has("deviceName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Username and deviceName parameters are required"
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
            ContextManager cm = connection.getContextManager();
            String deviceContextPath = ContextUtils.deviceContextPath(username, deviceName);
            
            Context deviceContext = connection.executeWithTimeout(() -> {
                try {
                    return cm.get(deviceContextPath);
                } catch (Exception e) {
                    return null;
                }
            }, 60000);
            
            if (deviceContext == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Device not found: " + deviceName + " for user: " + username
                );
            }
            
            ObjectNode result = instance.objectNode();
            result.put("username", username);
            result.put("deviceName", deviceName);
            result.put("path", deviceContext.getPath());
            result.put("name", deviceContext.getName());
            
            String description = deviceContext.getDescription();
            if (description != null) {
                result.put("description", description);
            }
            
            // Try to get device driver and status information
            try {
                com.tibbo.aggregate.common.context.CallerController caller = 
                    deviceContext.getContextManager().getCallerController();
                
                // Try to get driver information
                try {
                    com.tibbo.aggregate.common.datatable.DataTable driverInfo = 
                        deviceContext.getVariable("driverInfo", caller);
                    if (driverInfo != null && driverInfo.getRecordCount() > 0) {
                        com.tibbo.aggregate.common.datatable.DataRecord rec = driverInfo.getRecord(0);
                        if (rec.hasField("driverId")) {
                            result.put("driverId", rec.getString("driverId"));
                        }
                    }
                } catch (Exception e) {
                    // Driver info not available
                }
                
                // Try to get status
                try {
                    com.tibbo.aggregate.common.datatable.DataTable status = 
                        deviceContext.getVariable("status", caller);
                    if (status != null && status.getRecordCount() > 0) {
                        com.tibbo.aggregate.common.datatable.DataRecord rec = status.getRecord(0);
                        if (rec.hasField("status")) {
                            result.put("status", rec.getString("status"));
                        }
                    }
                } catch (Exception e) {
                    // Status not available
                }
            } catch (Exception e) {
                // Additional info not available
            }
            
            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to get device: " + e.getMessage()
            );
        }
    }
}
