package com.tibbo.aggregate.mcp.tools.variable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Idempotent tool to get an existing variable or create it if it doesn't exist
 */
public class GetOrCreateVariableTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_get_or_create_variable";
    }
    
    @Override
    public String getDescription() {
        return "Get an existing variable or create it if it doesn't exist (idempotent operation).";
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
        
        ObjectNode format = instance.objectNode();
        format.put("type", "string");
        format.put("description", "Variable format (required if creating)");
        properties.set("format", format);
        
        ObjectNode description = instance.objectNode();
        description.put("type", "string");
        description.put("description", "Variable description (optional)");
        properties.set("description", description);
        
        ObjectNode writable = instance.objectNode();
        writable.put("type", "boolean");
        writable.put("description", "Is variable writable (default: true)");
        properties.set("writable", writable);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("variableName"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("variableName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and variableName are required"
            );
        }
        
        String path = params.get("path").asText();
        String variableName = params.get("variableName").asText();
        String connectionKey = params.has("connectionKey") ? params.get("connectionKey").asText() : null;
        
        // Try to get variable first
        GetVariableTool getTool = new GetVariableTool();
        try {
            ObjectNode getParams = instance.objectNode();
            getParams.put("path", path);
            getParams.put("name", variableName);  // GetVariableTool uses "name", not "variableName"
            if (connectionKey != null) {
                getParams.put("connectionKey", connectionKey);
            }
            
            JsonNode existingVar = getTool.execute(getParams, connectionManager);
            
            // Variable exists
            ObjectNode result = instance.objectNode();
            result.put("path", path);
            result.put("variableName", variableName);
            result.put("created", false);
            result.put("exists", true);
            if (existingVar != null && existingVar.has("format")) {
                result.set("format", existingVar.get("format"));
            }
            return result;
        } catch (McpException e) {
            // Check if error is "not found" or "недоступна" - then create
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("not found") || errorMsg.contains("does not exist") || 
                errorMsg.contains("недоступна") || errorMsg.contains("unavailable"))) {
                // Variable doesn't exist - create it
                if (!params.has("format")) {
                    throw new McpException(
                        com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                        "Format is required when creating a new variable"
                    );
                }
                
                CreateVariableTool createTool = new CreateVariableTool();
                ObjectNode createParams = instance.objectNode();
                createParams.put("path", path);
                createParams.put("variableName", variableName);
                createParams.put("format", params.get("format").asText());
                
                if (params.has("description")) {
                    createParams.put("description", params.get("description").asText());
                }
                if (params.has("writable")) {
                    createParams.put("writable", params.get("writable").asBoolean());
                } else {
                    createParams.put("writable", true);
                }
                if (connectionKey != null) {
                    createParams.put("connectionKey", connectionKey);
                }
                
                try {
                    JsonNode createResult = createTool.execute(createParams, connectionManager);
                    
                    ObjectNode result = instance.objectNode();
                    result.put("path", path);
                    result.put("variableName", variableName);
                    result.put("created", true);
                    result.put("exists", true);
                    if (createResult != null && createResult.has("format")) {
                        result.set("format", createResult.get("format"));
                    }
                    return result;
                } catch (McpException createEx) {
                    // If creation fails with "already exists", treat as success
                    if (createEx.getMessage() != null && createEx.getMessage().contains("already exists")) {
                        // Try to get it again
                        ObjectNode getParams2 = instance.objectNode();
                        getParams2.put("path", path);
                        getParams2.put("name", variableName);  // GetVariableTool uses "name", not "variableName"
                        if (connectionKey != null) {
                            getParams2.put("connectionKey", connectionKey);
                        }
                        JsonNode existingVar = getTool.execute(getParams2, connectionManager);
                        ObjectNode result = instance.objectNode();
                        result.put("path", path);
                        result.put("variableName", variableName);
                        result.put("created", false);
                        result.put("exists", true);
                        if (existingVar != null && existingVar.has("format")) {
                            result.set("format", existingVar.get("format"));
                        }
                        return result;
                    }
                    throw createEx;
                }
            } else {
                // Other error - rethrow
                throw e;
            }
        }
    }
}
