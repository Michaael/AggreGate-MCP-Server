package com.tibbo.aggregate.mcp.tools.variable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for setting a specific field of a variable
 */
public class SetVariableFieldTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_set_variable_field";
    }
    
    @Override
    public String getDescription() {
        return "Set a specific field value of a context variable";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Context path");
        properties.set("path", path);
        
        ObjectNode variableName = instance.objectNode();
        variableName.put("type", "string");
        variableName.put("description", "Variable name");
        properties.set("variableName", variableName);
        
        ObjectNode fieldName = instance.objectNode();
        fieldName.put("type", "string");
        fieldName.put("description", "Field name");
        properties.set("fieldName", fieldName);
        
        ObjectNode value = instance.objectNode();
        value.put("description", "Field value (can be string, number, boolean, or null)");
        properties.set("value", value);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("variableName").add("fieldName").add("value"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("variableName") || !params.has("fieldName") || !params.has("value")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path, variableName, fieldName, and value parameters are required"
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
            String variableName = params.get("variableName").asText();
            String fieldName = params.get("fieldName").asText();
            JsonNode valueNode = params.get("value");
            
            Context context = connection.getContextManager().get(path);
            
            Object value;
            if (valueNode.isNull()) {
                value = null;
            } else if (valueNode.isTextual()) {
                value = valueNode.asText();
            } else if (valueNode.isInt()) {
                value = valueNode.asInt();
            } else if (valueNode.isLong()) {
                value = valueNode.asLong();
            } else if (valueNode.isDouble()) {
                value = valueNode.asDouble();
            } else if (valueNode.isBoolean()) {
                value = valueNode.asBoolean();
            } else {
                value = valueNode.asText();
            }
            
            context.setVariableField(variableName, fieldName, value, null);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Variable field set successfully");
            
            return result;
        } catch (ContextException e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to set variable field: " + e.getMessage()
            );
        }
    }
}

