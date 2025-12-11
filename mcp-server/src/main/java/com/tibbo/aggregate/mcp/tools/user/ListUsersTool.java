package com.tibbo.aggregate.mcp.tools.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for listing users
 */
public class ListUsersTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_list_users";
    }
    
    @Override
    public String getDescription() {
        return "List all user accounts";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
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
        
        String mask = ContextUtils.userContextPath(ContextUtils.CONTEXT_GROUP_MASK);
        List<Context> userContexts = ContextUtils.expandMaskToContexts(mask, connection.getContextManager(), null);
        
        ArrayNode result = instance.arrayNode();
        for (Context userContext : userContexts) {
            ObjectNode userNode = instance.objectNode();
            userNode.put("path", userContext.getPath());
            userNode.put("name", userContext.getName());
            
            try {
                DataTable status = userContext.getVariable("status");
                Date creationTime = status.rec().getDate("creationTime");
                userNode.put("creationTime", creationTime != null ? creationTime.getTime() : 0);
            } catch (Exception e) {
                userNode.put("creationTime", 0);
            }
            
            result.add(userNode);
        }
        
        return result;
    }
}

