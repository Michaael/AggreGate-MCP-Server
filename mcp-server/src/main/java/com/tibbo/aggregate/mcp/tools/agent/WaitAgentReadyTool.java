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
 * Tool for waiting until an agent is ready (synchronized)
 */
public class WaitAgentReadyTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_wait_agent_ready";
    }
    
    @Override
    public String getDescription() {
        return "Wait until an agent is ready (synchronized with server). " +
               "This tool automatically handles retries and timeouts.";
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
        
        ObjectNode maxWaitTime = instance.objectNode();
        maxWaitTime.put("type", "integer");
        maxWaitTime.put("description", "Maximum wait time in milliseconds (default: 10000)");
        maxWaitTime.put("default", 10000);
        properties.set("maxWaitTime", maxWaitTime);
        
        ObjectNode retryDelay = instance.objectNode();
        retryDelay.put("type", "integer");
        retryDelay.put("description", "Delay between retries in milliseconds (default: 500)");
        retryDelay.put("default", 500);
        properties.set("retryDelay", retryDelay);
        
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
        long maxWaitTime = params.has("maxWaitTime") ? params.get("maxWaitTime").asLong() : 10000L;
        long retryDelay = params.has("retryDelay") ? params.get("retryDelay").asLong() : 500L;
        
        AgentConnection agentConnection = connectionManager.getAgentConnection(agentName);
        
        if (agentConnection == null) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONNECTION_ERROR,
                "Agent not found: " + agentName
            );
        }
        
        if (!agentConnection.isConnected()) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONNECTION_ERROR,
                "Agent not connected: " + agentName
            );
        }
        
        AgentContext context = agentConnection.getContext();
        long startTime = System.currentTimeMillis();
        int retryCount = 0;
        
        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            if (context.isSynchronized()) {
                ObjectNode result = instance.objectNode();
                result.put("success", true);
                result.put("message", "Agent is ready");
                result.put("agentName", agentName);
                result.put("synchronized", true);
                result.put("waitTime", System.currentTimeMillis() - startTime);
                result.put("retryCount", retryCount);
                return result;
            }
            
            try {
                Thread.sleep(retryDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Wait interrupted"
                );
            }
            retryCount++;
        }
        
        throw new McpException(
            com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
            String.format("Agent not synchronized after %d ms (retries: %d)", maxWaitTime, retryCount)
        );
    }
}
