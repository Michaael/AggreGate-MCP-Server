package com.tibbo.aggregate.mcp.tools.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.agent.AgentContext;
import com.tibbo.aggregate.mcp.connection.AgentConnection;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for getting agent status
 */
public class GetAgentStatusTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_agent_get_status";
    }
    
    @Override
    public String getDescription() {
        return "Get the status of an agent";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode agentName = instance.objectNode();
        agentName.put("type", "string");
        agentName.put("description", "Agent name");
        properties.set("agentName", agentName);
        
        schema.set("required", instance.arrayNode().add("agentName"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("agentName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "AgentName parameter is required"
            );
        }
        
        String agentName = params.get("agentName").asText();
        AgentConnection agentConnection = connectionManager.getAgentConnection(agentName);
        
        if (agentConnection == null) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONNECTION_ERROR,
                "Agent not found: " + agentName
            );
        }
        
        ObjectNode result = instance.objectNode();
        result.put("agentName", agentName);
        result.put("connected", agentConnection.isConnected());
        
        if (agentConnection.isConnected()) {
            AgentContext context = agentConnection.getContext();
            if (context != null) {
                result.put("synchronized", context.isSynchronized());
            }
        }
        
        return result;
    }
}

