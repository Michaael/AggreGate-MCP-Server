package com.tibbo.aggregate.mcp.tools.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.event.EventLevel;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;
import com.tibbo.aggregate.mcp.util.DataTableConverter;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for firing an event from a context (not just agent)
 */
public class FireContextEventTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_fire_context_event";
    }
    
    @Override
    public String getDescription() {
        return "Fire an event from a context (model, device, etc.). For agents, use aggregate_fire_event instead.";
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
        data.put("description", "Event data as DataTable JSON (optional)");
        properties.set("data", data);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("eventName"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("eventName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and eventName parameters are required"
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
            
            Context<?> context = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000L);
            
            final com.tibbo.aggregate.common.datatable.DataTable eventData;
            if (params.has("data") && !params.get("data").isNull()) {
                eventData = DataTableConverter.fromJson(params.get("data"));
            } else {
                eventData = null;
            }
            
            // Fire event using context's fireEvent method
            // Try different fireEvent signatures
            connection.executeWithTimeout(() -> {
                try {
                    com.tibbo.aggregate.common.context.CallerController caller = 
                        context.getContextManager().getCallerController();
                    
                    // Try fireEvent with level, caller, null (request), data
                    try {
                        context.fireEvent(eventName, level, caller, null, eventData);
                        return null;
                    } catch (Exception e1) {
                        // Try fireEvent with name, caller, data (without level)
                        try {
                            context.fireEvent(eventName, caller, eventData);
                            return null;
                        } catch (Exception e2) {
                            // Try fireEvent with name and data only
                            if (eventData != null && eventData.getRecordCount() > 0) {
                                // Extract values from DataTable
                                final com.tibbo.aggregate.common.datatable.DataTable finalEventData = eventData;
                                com.tibbo.aggregate.common.datatable.DataRecord rec = finalEventData.rec();
                                java.util.List<Object> dataValues = new java.util.ArrayList<>();
                                for (int i = 0; i < finalEventData.getFormat().getFieldCount(); i++) {
                                    dataValues.add(rec.getValue(i));
                                }
                                context.fireEvent(eventName, dataValues.toArray());
                            } else {
                                context.fireEvent(eventName);
                            }
                            return null;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to fire event: " + e.getMessage(), e);
                }
            }, 60000L);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Event fired successfully");
            result.put("path", path);
            result.put("eventName", eventName);
            result.put("level", levelStr);
            
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to fire event: " + errorMessage
            );
        }
    }
}
