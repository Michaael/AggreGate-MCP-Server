package com.tibbo.aggregate.mcp.tools.function;

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

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for calling a context function
 */
public class CallFunctionTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_call_function";
    }
    
    @Override
    public String getDescription() {
        return "Call a function on a context";
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
        
        ObjectNode functionName = instance.objectNode();
        functionName.put("type", "string");
        functionName.put("description", "Function name");
        properties.set("functionName", functionName);
        
        ObjectNode parameters = instance.objectNode();
        parameters.put("type", "object");
        parameters.put("description", "Function parameters as DataTable JSON (optional)");
        properties.set("parameters", parameters);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("functionName"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("functionName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and functionName parameters are required"
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
            String functionName = params.get("functionName").asText();
            
            // Execute with timeout
            Context context;
            try {
                context = connection.executeWithTimeout(() -> {
                    Context ctx = connection.getContextManager().get(path);
                    if (ctx == null) {
                        throw new RuntimeException("Context not found: " + path);
                    }
                    return ctx;
                }, 60000);
            } catch (Exception e) {
                String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Failed to get context: " + errorMessage
                );
            }
            
            com.tibbo.aggregate.common.datatable.DataTable result;
            try {
                if (params.has("parameters")) {
                    JsonNode parametersJson = params.get("parameters");
                    
                    // Check if parameters is a simple object (key-value pairs) or a DataTable structure
                    if (parametersJson.isObject() && !parametersJson.has("format") && !parametersJson.has("records")) {
                        // Simple object - convert to varargs like in examples (ManageDevices, ManageUsers)
                        // Extract values in order and pass as varargs
                        ObjectNode paramsObj = (ObjectNode) parametersJson;
                        java.util.List<Object> paramValues = new java.util.ArrayList<>();
                        
                        // Get parameter values in order (if we can determine order)
                        // For now, just collect all values
                        java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = paramsObj.fields();
                        while (fields.hasNext()) {
                            java.util.Map.Entry<String, JsonNode> entry = fields.next();
                            JsonNode valueNode = entry.getValue();
                            
                            // Convert JSON value to appropriate Java type
                            if (valueNode.isTextual()) {
                                paramValues.add(valueNode.asText());
                            } else if (valueNode.isInt()) {
                                paramValues.add(valueNode.asInt());
                            } else if (valueNode.isLong()) {
                                paramValues.add(valueNode.asLong());
                            } else if (valueNode.isDouble() || valueNode.isFloat()) {
                                paramValues.add(valueNode.asDouble());
                            } else if (valueNode.isBoolean()) {
                                paramValues.add(valueNode.asBoolean());
                            } else {
                                paramValues.add(valueNode.asText());
                            }
                        }
                        
                        // Call function with varargs (like in examples)
                        final Object[] varArgs = paramValues.toArray();
                        result = connection.executeWithTimeout(() -> {
                            try {
                                return context.callFunction(functionName, varArgs);
                            } catch (ContextException e) {
                                throw new RuntimeException(e);
                            }
                        }, 60000);
                    } else {
                        // DataTable structure - use DataTable approach
                        com.tibbo.aggregate.common.datatable.DataTable parameters;
                        try {
                            parameters = DataTableConverter.fromJson(parametersJson);
                        } catch (IllegalArgumentException e) {
                            // If format is missing, try to get it from function definition
                            if (e.getMessage() != null && e.getMessage().contains("TableFormat is required")) {
                                // Get function definition to extract input format
                                com.tibbo.aggregate.common.context.FunctionDefinition funcDef = 
                                    connection.executeWithTimeout(() -> {
                                        try {
                                            java.util.List<com.tibbo.aggregate.common.context.FunctionDefinition> funcs = 
                                                context.getFunctionDefinitions();
                                            for (com.tibbo.aggregate.common.context.FunctionDefinition fd : funcs) {
                                                if (functionName.equals(fd.getName())) {
                                                    return fd;
                                                }
                                            }
                                            return null;
                                        } catch (Exception ex) {
                                            return null;
                                        }
                                    }, 60000);
                                
                                if (funcDef != null && funcDef.getInputFormat() != null) {
                                    // Use function's input format
                                    com.tibbo.aggregate.common.datatable.TableFormat inputFormat = funcDef.getInputFormat();
                                    // Create DataTable with function's input format
                                    com.tibbo.aggregate.common.datatable.DataTable paramsTable = 
                                        new com.tibbo.aggregate.common.datatable.SimpleDataTable(inputFormat);
                                    
                                    // If parametersJson has records, try to populate
                                    if (parametersJson.has("records") && parametersJson.get("records").isArray()) {
                                        com.fasterxml.jackson.databind.node.ArrayNode records = 
                                            (com.fasterxml.jackson.databind.node.ArrayNode) parametersJson.get("records");
                                        if (records.size() > 0) {
                                            com.fasterxml.jackson.databind.node.ObjectNode firstRecord = 
                                                (com.fasterxml.jackson.databind.node.ObjectNode) records.get(0);
                                            com.tibbo.aggregate.common.datatable.DataRecord rec = paramsTable.addRecord();
                                            // Set values from JSON record
                                            java.util.Iterator<java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = 
                                                firstRecord.fields();
                                            while (fields.hasNext()) {
                                                java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = fields.next();
                                                String fieldName = entry.getKey();
                                                com.fasterxml.jackson.databind.JsonNode valueNode = entry.getValue();
                                                if (rec.hasField(fieldName)) {
                                                    Object value = convertJsonValue(valueNode);
                                                    rec.setValue(fieldName, value);
                                                }
                                            }
                                        }
                                    }
                                    parameters = paramsTable;
                                } else {
                                    throw new McpException(
                                        com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                                        "TableFormat is required to create DataRecord from JSON. Function definition not found or has no input format."
                                    );
                                }
                            } else {
                                throw e;
                            }
                        }
                        // Create final copy for lambda
                        final com.tibbo.aggregate.common.datatable.DataTable finalParameters = parameters;
                        result = connection.executeWithTimeout(() -> {
                            try {
                                return context.callFunction(functionName, finalParameters);
                            } catch (ContextException e) {
                                throw new RuntimeException(e);
                            }
                        }, 60000);
                    }
                } else {
                    result = connection.executeWithTimeout(() -> {
                        try {
                            return context.callFunction(functionName);
                        } catch (ContextException e) {
                            throw new RuntimeException(e);
                        }
                    }, 60000);
                }
            } catch (Exception e) {
                Throwable cause = e.getCause();
                if (cause instanceof ContextException) {
                    throw new McpException(
                        com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                        "Failed to call function: " + cause.getMessage()
                    );
                }
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Failed to call function: " + e.getMessage()
                );
            }
            
            return DataTableConverter.toJson(result);
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to call function: " + e.getMessage()
            );
        }
    }
    
    private static Object convertJsonValue(com.fasterxml.jackson.databind.JsonNode valueNode) {
        if (valueNode.isNull()) {
            return null;
        } else if (valueNode.isTextual()) {
            return valueNode.asText();
        } else if (valueNode.isInt()) {
            return valueNode.asInt();
        } else if (valueNode.isLong()) {
            return valueNode.asLong();
        } else if (valueNode.isDouble() || valueNode.isFloat()) {
            return valueNode.asDouble();
        } else if (valueNode.isBoolean()) {
            return valueNode.asBoolean();
        } else {
            return valueNode.asText();
        }
    }
}

