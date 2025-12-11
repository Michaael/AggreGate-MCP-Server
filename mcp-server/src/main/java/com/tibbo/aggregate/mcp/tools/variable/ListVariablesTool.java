package com.tibbo.aggregate.mcp.tools.variable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.context.VariableDefinition;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import java.util.List;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for listing variables in a context
 */
public class ListVariablesTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_list_variables";
    }
    
    @Override
    public String getDescription() {
        return "List all variables in a context";
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
        
        ObjectNode group = instance.objectNode();
        group.put("type", "string");
        group.put("description", "Optional variable group filter");
        properties.set("group", group);
        
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
        
        String path = ContextPathParser.parsePath(params.get("path").asText());
        String group = params.has("group") ? params.get("group").asText() : null;
        
        // Execute with timeout
        Context context;
        List<VariableDefinition> variables;
        try {
            context = connection.executeWithTimeout(() -> {
                return connection.getContextManager().get(path);
            }, 60000);
            
            final String finalGroup = group;
            variables = connection.executeWithTimeout(() -> {
                return finalGroup != null 
                    ? context.getVariableDefinitions(finalGroup)
                    : context.getVariableDefinitions();
            }, 60000);
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to list variables: " + e.getMessage()
            );
        }
        
        ArrayNode result = instance.arrayNode();
        for (VariableDefinition vd : variables) {
            ObjectNode varNode = instance.objectNode();
            varNode.put("name", vd.getName());
            varNode.put("description", vd.getDescription());
            varNode.put("group", vd.getGroup());
            varNode.put("readable", vd.isReadable());
            varNode.put("writable", vd.isWritable());
            result.add(varNode);
        }
        
        return result;
    }
}

