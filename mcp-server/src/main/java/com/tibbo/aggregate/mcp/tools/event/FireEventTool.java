package com.tibbo.aggregate.mcp.tools.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.agent.AgentContext;
import com.tibbo.aggregate.common.event.EventLevel;
import com.tibbo.aggregate.mcp.connection.AgentConnection;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.DataTableConverter;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for firing an event from an agent
 */
public class FireEventTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_fire_event";
    }
    
    @Override
    public String getDescription() {
        return "Fire an event from an agent context";
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
        
        ObjectNode eventName = instance.objectNode();
        eventName.put("type", "string");
        eventName.put("description", "Event name");
        properties.set("eventName", eventName);
        
        ObjectNode level = instance.objectNode();
        level.put("type", "string");
        level.put("description", "Event level (INFO, WARNING, ERROR, etc.)");
        level.put("default", "INFO");
        properties.set("level", level);
        
        ObjectNode data = instance.objectNode();
        data.put("type", "object");
        data.put("description", "Event data as DataTable JSON (optional)");
        properties.set("data", data);
        
        schema.set("required", instance.arrayNode().add("agentName").add("eventName"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("agentName") || !params.has("eventName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "AgentName and eventName parameters are required"
            );
        }
        
        String agentName = params.get("agentName").asText();
        AgentConnection agentConnection = connectionManager.getAgentConnection(agentName);
        
        if (agentConnection == null || !agentConnection.isConnected()) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONNECTION_ERROR,
                "Agent not connected: " + agentName
            );
        }
        
        try {
            String eventName = params.get("eventName").asText();
            String levelStr = params.has("level") ? params.get("level").asText().toUpperCase() : "INFO";
            
            // Convert string to EventLevel constant
            int level;
            if ("INFO".equals(levelStr)) {
                level = EventLevel.INFO;
            } else if ("WARNING".equals(levelStr) || "WARN".equals(levelStr)) {
                level = EventLevel.WARNING;
            } else if ("ERROR".equals(levelStr)) {
                level = EventLevel.ERROR;
            } else if ("FATAL".equals(levelStr)) {
                level = EventLevel.FATAL;
            } else if ("NOTICE".equals(levelStr)) {
                level = EventLevel.NOTICE;
            } else {
                level = EventLevel.INFO; // default
            }
            
            AgentContext context = agentConnection.getContext();
            if (!context.isSynchronized()) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Agent not synchronized yet"
                );
            }
            
            if (params.has("data")) {
                com.tibbo.aggregate.common.datatable.DataTable eventData = com.tibbo.aggregate.mcp.util.DataTableConverter.fromJson(params.get("data"));
                context.fireEvent(eventName, level, eventData);
            } else {
                context.fireEvent(eventName, level, null);
            }
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Event fired successfully");
            
            return result;
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to fire event: " + e.getMessage()
            );
        }
    }
}

