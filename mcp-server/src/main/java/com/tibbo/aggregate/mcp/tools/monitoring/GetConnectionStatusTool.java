package com.tibbo.aggregate.mcp.tools.monitoring;

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
 * Tool for getting connection status of a device or agent
 */
public class GetConnectionStatusTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_get_connection_status";
    }
    
    @Override
    public String getDescription() {
        return "Get connection status of a device or agent";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Path to device or agent");
        properties.set("path", path);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path parameter is required"
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
            
            Context<?> context = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000L);
            
            ObjectNode result = instance.objectNode();
            result.put("path", path);
            
            // Get status variable if available
            try {
                com.tibbo.aggregate.common.datatable.DataTable status = context.getVariable("status");
                if (status != null && status.getRecordCount() > 0) {
                    // Try to get status value
                    Object statusValue = status.rec().getValue("status");
                    if (statusValue != null) {
                        result.put("status", statusValue.toString());
                    }
                }
            } catch (Exception e) {
                // Status variable may not exist
            }
            
            // Get connection status through device status or agent status
            try {
                // For devices, try to get device status
                com.tibbo.aggregate.common.datatable.DataTable deviceStatus = context.getVariable("deviceStatus");
                if (deviceStatus != null && deviceStatus.getRecordCount() > 0) {
                    Object connected = deviceStatus.rec().getValue("connected");
                    if (connected != null) {
                        result.put("connected", connected.toString());
                    }
                }
            } catch (Exception e) {
                // Device status may not exist
            }
            
            // Try to get through getDeviceStatus action if available
            try {
                com.tibbo.aggregate.common.datatable.DataTable input = 
                    new com.tibbo.aggregate.common.datatable.SimpleDataTable();
                
                com.tibbo.aggregate.common.action.ServerActionInput actionInput = 
                    com.tibbo.aggregate.common.action.ActionUtils.createActionInput(input);
                
                com.tibbo.aggregate.common.action.ActionIdentifier actionId = 
                    com.tibbo.aggregate.common.action.ActionUtils.initAction(
                        context,
                        "getStatus",
                        actionInput,
                        null,
                        new com.tibbo.aggregate.common.action.ActionExecutionMode(
                            com.tibbo.aggregate.common.action.ActionExecutionMode.HEADLESS
                        ),
                        null
                    );
                
                // Note: Would need to step through action to get result
                // For now, return basic info
            } catch (Exception e) {
                // Action may not be available
            }
            
            // If no specific status found, return context existence as status
            if (!result.has("status")) {
                result.put("status", "exists");
                result.put("connected", "unknown");
            }
            
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to get connection status: " + errorMessage
            );
        }
    }
}
