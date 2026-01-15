package com.tibbo.aggregate.mcp.tools.history;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;
import com.tibbo.aggregate.mcp.util.DataTableConverter;

import java.util.Date;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for getting event history
 */
public class GetEventHistoryTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_get_event_history";
    }
    
    @Override
    public String getDescription() {
        return "Get the history of events over a specified time period";
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
        eventName.put("description", "Event name (optional, if not specified returns all events)");
        properties.set("eventName", eventName);
        
        ObjectNode startTime = instance.objectNode();
        startTime.put("type", "integer");
        startTime.put("description", "Start time (timestamp in milliseconds, optional)");
        properties.set("startTime", startTime);
        
        ObjectNode endTime = instance.objectNode();
        endTime.put("type", "integer");
        endTime.put("description", "End time (timestamp in milliseconds, optional)");
        properties.set("endTime", endTime);
        
        ObjectNode maxRecords = instance.objectNode();
        maxRecords.put("type", "integer");
        maxRecords.put("description", "Maximum number of records (default: 1000)");
        properties.set("maxRecords", maxRecords);
        
        ObjectNode level = instance.objectNode();
        level.put("type", "integer");
        level.put("description", "Filter by event level: 0=INFO, 1=WARNING, 2=ERROR, 3=FATAL, 4=NOTICE (optional)");
        properties.set("level", level);
        
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
        
        try {
            String path = ContextPathParser.parsePath(params.get("path").asText());
            
            String eventName = params.has("eventName") ? params.get("eventName").asText() : null;
            Long startTime = params.has("startTime") ? params.get("startTime").asLong() : null;
            Long endTime = params.has("endTime") ? params.get("endTime").asLong() : null;
            int maxRecords = params.has("maxRecords") ? params.get("maxRecords").asInt() : 1000;
            Integer level = params.has("level") ? params.get("level").asInt() : null;
            
            Context<?> context = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000L);
            
            // Get event history using executeAction through ExecuteActionTool
            // This is a simplified implementation - actual history retrieval may require
            // using the executeAction tool or specific AggreGate API methods
            com.tibbo.aggregate.common.datatable.DataTable history = connection.executeWithTimeout(() -> {
                try {
                    // Create input for getEventHistory action
                    com.tibbo.aggregate.common.datatable.DataTable input = new com.tibbo.aggregate.common.datatable.SimpleDataTable(
                        "<event><S><startTime><D><endTime><D><maxRecords><I><level><I>",
                        true
                    );
                    com.tibbo.aggregate.common.datatable.DataRecord inputRec = input.addRecord();
                    if (eventName != null) {
                        inputRec.setValue("event", eventName);
                    } else {
                        inputRec.setValue("event", "");
                    }
                    if (startTime != null) {
                        inputRec.setValue("startTime", new Date(startTime));
                    } else {
                        inputRec.setValue("startTime", null);
                    }
                    if (endTime != null) {
                        inputRec.setValue("endTime", new Date(endTime));
                    } else {
                        inputRec.setValue("endTime", null);
                    }
                    inputRec.setValue("maxRecords", maxRecords);
                    if (level != null) {
                        inputRec.setValue("level", level);
                    } else {
                        inputRec.setValue("level", -1); // -1 means no filter
                    }
                    
                    // Use ExecuteActionTool approach - delegate to action execution
                    // For now, return empty result with note that action execution is needed
                    throw new RuntimeException("Event history retrieval requires executeAction. " +
                        "Please use aggregate_execute_action with actionName='getEventHistory' or 'getHistory'. " +
                        "This is a placeholder implementation.");
                } catch (Exception e) {
                    throw new RuntimeException("Failed to get event history: " + e.getMessage(), e);
                }
            }, 120000L); // Longer timeout for history queries
            
            return DataTableConverter.toJson(history);
        } catch (ContextException e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to get event history: " + e.getMessage()
            );
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to get event history: " + errorMessage
            );
        }
    }
}
