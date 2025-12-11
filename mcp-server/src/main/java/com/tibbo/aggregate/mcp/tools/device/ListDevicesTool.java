package com.tibbo.aggregate.mcp.tools.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.context.VariableDefinition;
import com.tibbo.aggregate.common.datatable.FieldFormat;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for listing devices
 */
public class ListDevicesTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_list_devices";
    }
    
    @Override
    public String getDescription() {
        return "List all devices for a user";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode username = instance.objectNode();
        username.put("type", "string");
        username.put("description", "Username");
        properties.set("username", username);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("username"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("username")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Username parameter is required"
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
        
        String username = params.get("username").asText();
        ContextManager cm = connection.getContextManager();
        String mask = ContextUtils.deviceContextPath(username, ContextUtils.CONTEXT_GROUP_MASK);
        List<Context> deviceContexts = ContextUtils.expandMaskToContexts(mask, cm, null);
        
        ArrayNode result = instance.arrayNode();
        for (Context deviceContext : deviceContexts) {
            ObjectNode deviceNode = instance.objectNode();
            deviceNode.put("path", deviceContext.getPath());
            deviceNode.put("name", deviceContext.getName());
            
            try {
                String driverId = deviceContext.getVariable("status").rec().getString("driver");
                deviceNode.put("driverId", driverId);
                
                VariableDefinition statusVarDef = deviceContext.getVariableDefinition("status");
                TableFormat statusFormat = statusVarDef.getFormat();
                FieldFormat driverFieldFormat = statusFormat.getField("driver");
                Map<Object, String> driverSelectionValues = driverFieldFormat.getSelectionValues();
                String driverDescription = driverSelectionValues.get(driverId);
                deviceNode.put("driverDescription", driverDescription != null ? driverDescription : "");
            } catch (Exception e) {
                deviceNode.put("driverId", "");
                deviceNode.put("driverDescription", "");
            }
            
            result.add(deviceNode);
        }
        
        return result;
    }
}

