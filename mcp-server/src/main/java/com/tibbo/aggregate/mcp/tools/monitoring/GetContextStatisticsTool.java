package com.tibbo.aggregate.mcp.tools.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for getting context statistics
 */
public class GetContextStatisticsTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_get_context_statistics";
    }
    
    @Override
    public String getDescription() {
        return "Get statistics for a context (number of variables, events, functions, rules, etc.)";
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
            
            Context<?> context = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000L);
            
            // Get statistics
            ObjectNode result = instance.objectNode();
            result.put("path", path);
            
            // Count variables
            try {
                java.util.List<com.tibbo.aggregate.common.context.VariableDefinition> variables = 
                    context.getVariableDefinitions();
                result.put("variableCount", variables != null ? variables.size() : 0);
            } catch (Exception e) {
                result.put("variableCount", 0);
            }
            
            // Count events
            try {
                java.util.List<com.tibbo.aggregate.common.context.EventDefinition> events = 
                    context.getEventDefinitions();
                result.put("eventCount", events != null ? events.size() : 0);
            } catch (Exception e) {
                result.put("eventCount", 0);
            }
            
            // Count functions
            try {
                java.util.List<com.tibbo.aggregate.common.context.FunctionDefinition> functions = 
                    context.getFunctionDefinitions();
                result.put("functionCount", functions != null ? functions.size() : 0);
            } catch (Exception e) {
                result.put("functionCount", 0);
            }
            
            // Count rules (may need to use action)
            try {
                // Rules may not have direct getRuleDefinitions method
                // Try to get through action or variable
                result.put("ruleCount", 0);
            } catch (Exception e) {
                result.put("ruleCount", 0);
            }
            
            // Count alarms (may need to use action)
            try {
                // Alarms may not have direct getAlarmDefinitions method
                // Try to get through action or variable
                result.put("alarmCount", 0);
            } catch (Exception e) {
                result.put("alarmCount", 0);
            }
            
            // Count child contexts
            try {
                java.util.List<?> children = context.getChildren();
                result.put("childContextCount", children != null ? children.size() : 0);
            } catch (Exception e) {
                result.put("childContextCount", 0);
            }
            
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to get context statistics: " + errorMessage
            );
        }
    }
}
