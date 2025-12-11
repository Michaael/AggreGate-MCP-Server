package com.tibbo.aggregate.mcp.tools.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import java.util.List;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for listing contexts by mask
 */
public class ListContextsTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_list_contexts";
    }
    
    @Override
    public String getDescription() {
        return "List contexts matching a mask pattern";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode mask = instance.objectNode();
        mask.put("type", "string");
        mask.put("description", "Context mask (e.g., 'users.*' or 'users.admin.devices.*')");
        properties.set("mask", mask);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
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
        
        String mask = params.has("mask") 
            ? ContextPathParser.expandMask(params.get("mask").asText())
            : ContextUtils.CONTEXT_GROUP_MASK;
        
        // Execute operation with timeout
        List<Context> contexts;
        try {
            contexts = connection.executeWithTimeout(() -> {
                return ContextUtils.expandMaskToContexts(mask, connection.getContextManager(), null);
            }, 60000); // 60 seconds timeout
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to list contexts: " + e.getMessage()
            );
        }
        
        ArrayNode result = instance.arrayNode();
        for (Context context : contexts) {
            ObjectNode contextNode = instance.objectNode();
            contextNode.put("path", context.getPath());
            contextNode.put("name", context.getName());
            contextNode.put("description", context.getDescription());
            result.add(contextNode);
        }
        
        return result;
    }
}

