package com.tibbo.aggregate.mcp.tools.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.agent.AgentContext;
import com.tibbo.aggregate.common.event.EventLevel;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.SimpleDataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.mcp.connection.AgentConnection;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Simplified tool for sending events from an agent
 * Accepts simple key-value pairs instead of full DataTable structure
 */
public class SendAgentEventSimpleTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_send_agent_event_simple";
    }
    
    @Override
    public String getDescription() {
        return "Send an event from an agent using simplified parameters. " +
               "Accepts simple key-value pairs instead of full DataTable JSON structure.";
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
        level.put("description", "Event level (INFO, WARNING, ERROR, FATAL, NOTICE)");
        level.put("default", "INFO");
        properties.set("level", level);
        
        ObjectNode data = instance.objectNode();
        data.put("type", "object");
        data.put("description", "Event data as simple key-value pairs (optional). " +
               "Values can be strings, numbers, booleans, or arrays of these types.");
        properties.set("data", data);
        
        ObjectNode waitForReady = instance.objectNode();
        waitForReady.put("type", "boolean");
        waitForReady.put("description", "Wait for agent to be ready before sending (default: true)");
        waitForReady.put("default", true);
        properties.set("waitForReady", waitForReady);
        
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
        boolean waitForReady = params.has("waitForReady") ? params.get("waitForReady").asBoolean() : true;
        
        AgentConnection agentConnection = connectionManager.getAgentConnection(agentName);
        
        if (agentConnection == null || !agentConnection.isConnected()) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONNECTION_ERROR,
                "Agent not connected: " + agentName
            );
        }
        
        AgentContext context = agentConnection.getContext();
        
        // Wait for synchronization if requested
        if (waitForReady) {
            int maxRetries = 10;
            int retryDelay = 500;
            boolean isSynchronized = false;
            
            for (int retry = 0; retry < maxRetries; retry++) {
                if (context.isSynchronized()) {
                    isSynchronized = true;
                    break;
                }
                if (retry < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            if (!isSynchronized) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Agent not synchronized yet. Please wait and try again."
                );
            }
        }
        
        try {
            String eventName = params.get("eventName").asText();
            String levelStr = params.has("level") ? params.get("level").asText().toUpperCase() : "INFO";
            
            int level;
            switch (levelStr) {
                case "WARNING":
                case "WARN":
                    level = EventLevel.WARNING;
                    break;
                case "ERROR":
                    level = EventLevel.ERROR;
                    break;
                case "FATAL":
                    level = EventLevel.FATAL;
                    break;
                case "NOTICE":
                    level = EventLevel.NOTICE;
                    break;
                case "INFO":
                default:
                    level = EventLevel.INFO;
                    break;
            }
            
            DataTable eventData = null;
            if (params.has("data") && params.get("data").isObject()) {
                // Convert simple object to DataTable
                ObjectNode dataObj = (ObjectNode) params.get("data");
                
                // Build format from data fields
                TableFormat format = new TableFormat(0, Integer.MAX_VALUE);
                java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = dataObj.fields();
                while (fields.hasNext()) {
                    java.util.Map.Entry<String, JsonNode> entry = fields.next();
                    String fieldName = entry.getKey();
                    JsonNode valueNode = entry.getValue();
                    
                    // Infer type from value
                    String type = "S"; // Default to String
                    if (valueNode.isNumber()) {
                        if (valueNode.isInt() || valueNode.isLong()) {
                            type = "I";
                        } else {
                            type = "E";
                        }
                    } else if (valueNode.isBoolean()) {
                        type = "B";
                    } else if (valueNode.isArray()) {
                        type = "T"; // DataTable for arrays
                    }
                    format.addField("<" + fieldName + "><" + type + ">");
                }
                
                eventData = new SimpleDataTable(format);
                DataRecord record = eventData.addRecord();
                
                // Set values
                fields = dataObj.fields();
                while (fields.hasNext()) {
                    java.util.Map.Entry<String, JsonNode> entry = fields.next();
                    String fieldName = entry.getKey();
                    JsonNode valueNode = entry.getValue();
                    
                    if (valueNode.isNull()) {
                        record.setValue(fieldName, null);
                    } else if (valueNode.isTextual()) {
                        record.setValue(fieldName, valueNode.asText());
                    } else if (valueNode.isInt()) {
                        record.setValue(fieldName, valueNode.asInt());
                    } else if (valueNode.isLong()) {
                        record.setValue(fieldName, valueNode.asLong());
                    } else if (valueNode.isDouble() || valueNode.isFloat()) {
                        record.setValue(fieldName, valueNode.asDouble());
                    } else if (valueNode.isBoolean()) {
                        record.setValue(fieldName, valueNode.asBoolean());
                    } else if (valueNode.isArray()) {
                        // For arrays, create a simple DataTable
                        ArrayNode array = (ArrayNode) valueNode;
                        TableFormat arrayFormat = new TableFormat(0, Integer.MAX_VALUE);
                        arrayFormat.addField("<value><S>");
                        DataTable arrayTable = new SimpleDataTable(arrayFormat);
                        for (JsonNode item : array) {
                            DataRecord arrayRec = arrayTable.addRecord();
                            arrayRec.setValue("value", item.asText());
                        }
                        record.setValue(fieldName, arrayTable);
                    }
                }
            }
            
            context.fireEvent(eventName, level, eventData);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Event sent successfully");
            result.put("agentName", agentName);
            result.put("eventName", eventName);
            result.put("level", levelStr);
            
            return result;
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to send event: " + e.getMessage()
            );
        }
    }
}
