package com.tibbo.aggregate.mcp.tools.variable;

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
 * Tool for setting a variable value
 */
public class SetVariableTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_set_variable";
    }
    
    @Override
    public String getDescription() {
        return "Set the value of a context variable";
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
        
        ObjectNode name = instance.objectNode();
        name.put("type", "string");
        name.put("description", "Variable name");
        properties.set("name", name);
        
        ObjectNode value = instance.objectNode();
        value.put("type", "object");
        value.put("description", "Variable value as DataTable JSON");
        properties.set("value", value);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("name").add("value"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("name") || !params.has("value")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path, name, and value parameters are required"
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
            String name = params.get("name").asText();
            JsonNode valueJson = params.get("value");
            
            Context context = connection.executeWithTimeout(() -> {
                return connection.getContextManager().get(path);
            }, 60000);
            
            if (context == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Context not found: " + path
                );
            }
            
            // Use CallerController for setting variable
            connection.executeWithTimeout(() -> {
                com.tibbo.aggregate.common.context.CallerController caller = 
                    context.getContextManager().getCallerController();
                
                // Try to get existing variable to check if it exists and get its format
                com.tibbo.aggregate.common.datatable.DataTable existingValue = null;
                boolean variableExists = false;
                try {
                    existingValue = context.getVariableClone(name, caller);
                    variableExists = true;
                } catch (ContextException e) {
                    // Variable doesn't exist - will create new one
                }
                
                com.tibbo.aggregate.common.datatable.DataTable value;
                
                if (variableExists && existingValue != null) {
                    // Variable exists - ALWAYS check maxRecords and use setVariableField if maxRecords=1
                    com.tibbo.aggregate.common.datatable.TableFormat existingFormat = existingValue.getFormat();
                    int maxRecords = existingFormat.getMaxRecords();
                    int existingRecordCount = existingValue.getRecordCount();
                    
                    // Parse new value
                    com.tibbo.aggregate.common.datatable.DataTable newValue;
                    if (!valueJson.has("format")) {
                        ObjectNode valueWithFormat = (ObjectNode) valueJson.deepCopy();
                        valueWithFormat.set("format", DataTableConverter.formatToJson(existingFormat));
                        newValue = DataTableConverter.fromJson(valueWithFormat);
                    } else {
                        newValue = DataTableConverter.fromJson(valueJson);
                    }
                    
                    int newRecordCount = newValue.getRecordCount();
                    
                    // For variables with maxRecords=1, ALWAYS use setVariableField to avoid "maximum number of records is reached" error
                    // Check both maxRecords and if it's a single-record format
                    if ((maxRecords == 1 || existingFormat.isSingleRecord()) && newRecordCount > 0) {
                        if (existingRecordCount > 0) {
                            // Update existing record using setVariableField
                            com.tibbo.aggregate.common.datatable.DataRecord newRecord = newValue.getRecord(0);
                            for (int fieldIndex = 0; fieldIndex < existingFormat.getFieldCount(); fieldIndex++) {
                                String fieldName = existingFormat.getField(fieldIndex).getName();
                                if (newRecord.hasField(fieldName)) {
                                    Object fieldValue = newRecord.getValue(fieldName);
                                    context.setVariableField(name, fieldName, fieldValue, caller);
                                }
                            }
                        } else {
                            // No existing record - create one using setVariable with a new table (this is safe for first time)
                            com.tibbo.aggregate.common.datatable.DataTable tempValue = new com.tibbo.aggregate.common.datatable.SimpleDataTable(existingFormat);
                            com.tibbo.aggregate.common.datatable.DataRecord tempRecord = tempValue.addRecord();
                            com.tibbo.aggregate.common.datatable.DataRecord newRecord = newValue.getRecord(0);
                            for (int fieldIndex = 0; fieldIndex < existingFormat.getFieldCount(); fieldIndex++) {
                                String fieldName = existingFormat.getField(fieldIndex).getName();
                                if (newRecord.hasField(fieldName)) {
                                    Object fieldValue = newRecord.getValue(fieldName);
                                    tempRecord.setValue(fieldName, fieldValue);
                                }
                            }
                            context.setVariable(name, caller, tempValue);
                        }
                        return null; // Already handled, no need to call setVariable again
                    } else {
                        // For variables with multiple records allowed
                        // Update existing records by copying field values
                        for (int i = 0; i < Math.min(existingRecordCount, newRecordCount); i++) {
                            com.tibbo.aggregate.common.datatable.DataRecord existingRecord = existingValue.getRecord(i);
                            com.tibbo.aggregate.common.datatable.DataRecord newRecord = newValue.getRecord(i);
                            
                            // Copy all field values from new record to existing record
                            for (int fieldIndex = 0; fieldIndex < existingFormat.getFieldCount(); fieldIndex++) {
                                String fieldName = existingFormat.getField(fieldIndex).getName();
                                if (newRecord.hasField(fieldName)) {
                                    Object fieldValue = newRecord.getValue(fieldName);
                                    existingRecord.setValue(fieldName, fieldValue);
                                }
                            }
                        }
                        
                        // Add new records if needed (only if maxRecords allows)
                        for (int i = existingRecordCount; i < newRecordCount; i++) {
                            if (maxRecords > existingValue.getRecordCount()) {
                                com.tibbo.aggregate.common.datatable.DataRecord newRecord = newValue.getRecord(i);
                                com.tibbo.aggregate.common.datatable.DataRecord addedRecord = existingValue.addRecord();
                                
                                // Copy all field values
                                for (int fieldIndex = 0; fieldIndex < existingFormat.getFieldCount(); fieldIndex++) {
                                    String fieldName = existingFormat.getField(fieldIndex).getName();
                                    if (newRecord.hasField(fieldName)) {
                                        Object fieldValue = newRecord.getValue(fieldName);
                                        addedRecord.setValue(fieldName, fieldValue);
                                    }
                                }
                            }
                        }
                        
                        // Remove excess records if new value has fewer records
                        while (existingValue.getRecordCount() > newRecordCount) {
                            existingValue.removeRecord(existingValue.getRecordCount() - 1);
                        }
                        
                        value = existingValue;
                    }
                } else {
                    // Variable doesn't exist - create new one
                    value = DataTableConverter.fromJson(valueJson);
                }
                
                // Try to set variable, but catch "maximum number of records is reached" error
                try {
                    context.setVariable(name, caller, value);
                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && (errorMsg.contains("maximum number of records is reached") || 
                        errorMsg.contains("Невозможно добавить запись"))) {
                        // Fallback: use setVariableField for maxRecords=1 variables
                        if (variableExists && existingValue != null) {
                            com.tibbo.aggregate.common.datatable.TableFormat existingFormat = existingValue.getFormat();
                            if (existingFormat.getMaxRecords() == 1 && value.getRecordCount() > 0) {
                                com.tibbo.aggregate.common.datatable.DataRecord newRecord = value.getRecord(0);
                                for (int fieldIndex = 0; fieldIndex < existingFormat.getFieldCount(); fieldIndex++) {
                                    String fieldName = existingFormat.getField(fieldIndex).getName();
                                    if (newRecord.hasField(fieldName)) {
                                        Object fieldValue = newRecord.getValue(fieldName);
                                        context.setVariableField(name, fieldName, fieldValue, caller);
                                    }
                                }
                                return null; // Successfully updated using setVariableField
                            }
                        }
                    }
                    // Re-throw if we couldn't handle it
                    throw e;
                }
                return null;
            }, 60000);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Variable set successfully");
            
            return result;
        } catch (McpException e) {
            // Check if this is a "maximum number of records is reached" error
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.toLowerCase().contains("maximum number of records") || 
                errorMessage.contains("Невозможно добавить запись") ||
                errorMessage.contains("maximum number of records is reached") ||
                errorMessage.contains("records is reached"))) {
                // Fallback: try to use setVariableField
                try {
                    String path = ContextPathParser.parsePath(params.get("path").asText());
                    String name = params.get("name").asText();
                    JsonNode valueJson = params.get("value");
                    
                    Context context = connection.executeWithTimeout(() -> {
                        return connection.getContextManager().get(path);
                    }, 60000);
                    
                    if (context != null) {
                        connection.executeWithTimeout(() -> {
                            com.tibbo.aggregate.common.context.CallerController caller = 
                                context.getContextManager().getCallerController();
                            
                            // Get existing variable to get format
                            com.tibbo.aggregate.common.datatable.DataTable existingValue = context.getVariableClone(name, caller);
                            com.tibbo.aggregate.common.datatable.TableFormat existingFormat = existingValue.getFormat();
                            
                            // Parse new value
                            com.tibbo.aggregate.common.datatable.DataTable newValue;
                            if (!valueJson.has("format")) {
                                ObjectNode valueWithFormat = (ObjectNode) valueJson.deepCopy();
                                valueWithFormat.set("format", DataTableConverter.formatToJson(existingFormat));
                                newValue = DataTableConverter.fromJson(valueWithFormat);
                            } else {
                                newValue = DataTableConverter.fromJson(valueJson);
                            }
                            
                            // Use setVariableField to update each field
                            if (newValue.getRecordCount() > 0) {
                                com.tibbo.aggregate.common.datatable.DataRecord newRecord = newValue.getRecord(0);
                                for (int fieldIndex = 0; fieldIndex < existingFormat.getFieldCount(); fieldIndex++) {
                                    String fieldName = existingFormat.getField(fieldIndex).getName();
                                    if (newRecord.hasField(fieldName)) {
                                        Object fieldValue = newRecord.getValue(fieldName);
                                        context.setVariableField(name, fieldName, fieldValue, caller);
                                    }
                                }
                            }
                            return null;
                        }, 60000);
                        
                        ObjectNode result = instance.objectNode();
                        result.put("success", true);
                        result.put("message", "Variable set successfully (using setVariableField fallback)");
                        return result;
                    }
                } catch (Exception fallbackException) {
                    // If fallback also fails, throw original error
                    throw e;
                }
            }
            throw e;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            
            // Check if this is a "maximum number of records is reached" error
            if (errorMessage != null && (errorMessage.toLowerCase().contains("maximum number of records") || 
                errorMessage.contains("Невозможно добавить запись") ||
                errorMessage.contains("maximum number of records is reached") ||
                errorMessage.contains("records is reached"))) {
                // Fallback: try to use setVariableField
                try {
                    String path = ContextPathParser.parsePath(params.get("path").asText());
                    String name = params.get("name").asText();
                    JsonNode valueJson = params.get("value");
                    
                    Context context = connection.executeWithTimeout(() -> {
                        return connection.getContextManager().get(path);
                    }, 60000);
                    
                    if (context != null) {
                        connection.executeWithTimeout(() -> {
                            com.tibbo.aggregate.common.context.CallerController caller = 
                                context.getContextManager().getCallerController();
                            
                            // Get existing variable to get format
                            com.tibbo.aggregate.common.datatable.DataTable existingValue = context.getVariableClone(name, caller);
                            com.tibbo.aggregate.common.datatable.TableFormat existingFormat = existingValue.getFormat();
                            
                            // Parse new value
                            com.tibbo.aggregate.common.datatable.DataTable newValue;
                            if (!valueJson.has("format")) {
                                ObjectNode valueWithFormat = (ObjectNode) valueJson.deepCopy();
                                valueWithFormat.set("format", DataTableConverter.formatToJson(existingFormat));
                                newValue = DataTableConverter.fromJson(valueWithFormat);
                            } else {
                                newValue = DataTableConverter.fromJson(valueJson);
                            }
                            
                            // Use setVariableField to update each field
                            if (newValue.getRecordCount() > 0) {
                                com.tibbo.aggregate.common.datatable.DataRecord newRecord = newValue.getRecord(0);
                                for (int fieldIndex = 0; fieldIndex < existingFormat.getFieldCount(); fieldIndex++) {
                                    String fieldName = existingFormat.getField(fieldIndex).getName();
                                    if (newRecord.hasField(fieldName)) {
                                        Object fieldValue = newRecord.getValue(fieldName);
                                        context.setVariableField(name, fieldName, fieldValue, caller);
                                    }
                                }
                            }
                            return null;
                        }, 60000);
                        
                        ObjectNode result = instance.objectNode();
                        result.put("success", true);
                        result.put("message", "Variable set successfully (using setVariableField fallback)");
                        return result;
                    }
                } catch (Exception fallbackException) {
                    // If fallback also fails, throw original error
                }
            }
            
            com.tibbo.aggregate.mcp.util.ErrorHandler.ErrorDetails errorDetails = 
                com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorDetails(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to set variable: " + errorMessage,
                errorDetails
            );
        }
    }
}

