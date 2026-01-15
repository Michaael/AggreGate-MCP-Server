package com.tibbo.aggregate.mcp.tools.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.FunctionDefinition;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import java.util.List;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for listing functions in a context
 */
public class ListFunctionsTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_list_functions";
    }
    
    @Override
    public String getDescription() {
        return "List all functions available in a context";
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
            }, 60000);
            
            // Check if this is a model context
            boolean isModelContext = connection.executeWithTimeout(() -> {
                try {
                    context.getVariable(com.tibbo.aggregate.common.server.ModelContextConstants.V_MODEL_FUNCTIONS);
                    return true;
                } catch (ContextException e) {
                    return false;
                }
            }, 60000);
            
            ArrayNode result = instance.arrayNode();
            
            if (isModelContext) {
                // For model context: get functions from V_MODEL_FUNCTIONS variable
                try {
                    com.tibbo.aggregate.common.context.CallerController caller = 
                        context.getContextManager().getCallerController();
                    com.tibbo.aggregate.common.datatable.DataTable modelFunctions = 
                        context.getVariable(com.tibbo.aggregate.common.server.ModelContextConstants.V_MODEL_FUNCTIONS, caller);
                    
                    for (int i = 0; i < modelFunctions.getRecordCount(); i++) {
                        com.tibbo.aggregate.common.datatable.DataRecord rec = modelFunctions.getRecord(i);
                        ObjectNode funcNode = instance.objectNode();
                        funcNode.put("name", rec.getString(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_NAME));
                        funcNode.put("description", rec.hasField(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_DESCRIPTION) 
                            ? rec.getString(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_DESCRIPTION) : "");
                        funcNode.put("group", rec.hasField(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_GROUP) 
                            ? rec.getString(com.tibbo.aggregate.common.context.AbstractContext.FIELD_FD_GROUP) : "");
                        result.add(funcNode);
                    }
                } catch (Exception e) {
                    // If V_MODEL_FUNCTIONS is not available, fall back to getFunctionDefinitions
                    List<FunctionDefinition> functions = connection.executeWithTimeout(() -> {
                        return context.getFunctionDefinitions();
                    }, 60000);
                    
                    if (functions != null) {
                        for (FunctionDefinition fd : functions) {
                            if (fd != null) {
                                ObjectNode funcNode = instance.objectNode();
                                funcNode.put("name", fd.getName() != null ? fd.getName() : "");
                                funcNode.put("description", fd.getDescription() != null ? fd.getDescription() : "");
                                funcNode.put("group", fd.getGroup() != null ? fd.getGroup() : "");
                                result.add(funcNode);
                            }
                        }
                    }
                }
            } else {
                // For regular context: use getFunctionDefinitions
                List<FunctionDefinition> functions = connection.executeWithTimeout(() -> {
                    return context.getFunctionDefinitions();
                }, 60000);
                
                if (functions == null) {
                    functions = new java.util.ArrayList<>();
                }
                
                for (FunctionDefinition fd : functions) {
                    if (fd != null) {
                        ObjectNode funcNode = instance.objectNode();
                        funcNode.put("name", fd.getName() != null ? fd.getName() : "");
                        funcNode.put("description", fd.getDescription() != null ? fd.getDescription() : "");
                        funcNode.put("group", fd.getGroup() != null ? fd.getGroup() : "");
                        result.add(funcNode);
                    }
                }
            }
            
            return result;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to list functions: " + errorMessage
            );
        }
    }
}

