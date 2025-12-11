package com.tibbo.aggregate.mcp.tools.variable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.VariableDefinition;
import com.tibbo.aggregate.common.security.Permissions;
import com.tibbo.aggregate.common.security.ServerPermissionChecker;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.datatable.encoding.ClassicEncodingSettings;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;
import com.tibbo.aggregate.mcp.util.ErrorHandler;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for creating a variable in a context (including model context)
 */
public class CreateVariableTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_create_variable";
    }
    
    @Override
    public String getDescription() {
        return "Create a variable in a context (supports model context)";
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
        
        ObjectNode variableName = instance.objectNode();
        variableName.put("type", "string");
        variableName.put("description", "Variable name");
        properties.set("variableName", variableName);
        
        ObjectNode format = instance.objectNode();
        format.put("type", "string");
        format.put("description", "Variable format as TableFormat string (required)");
        properties.set("format", format);
        
        ObjectNode description = instance.objectNode();
        description.put("type", "string");
        description.put("description", "Variable description (optional)");
        properties.set("description", description);
        
        ObjectNode group = instance.objectNode();
        group.put("type", "string");
        group.put("description", "Variable group (optional)");
        properties.set("group", group);
        
        ObjectNode writable = instance.objectNode();
        writable.put("type", "boolean");
        writable.put("description", "Is variable writable (default: true)");
        writable.put("default", true);
        properties.set("writable", writable);
        
        ObjectNode readPermissions = instance.objectNode();
        readPermissions.put("type", "string");
        readPermissions.put("description", "Read permissions (optional, default: observer)");
        properties.set("readPermissions", readPermissions);
        
        ObjectNode writePermissions = instance.objectNode();
        writePermissions.put("type", "string");
        writePermissions.put("description", "Write permissions (optional, default: manager)");
        properties.set("writePermissions", writePermissions);
        
        ObjectNode storageMode = instance.objectNode();
        storageMode.put("type", "integer");
        storageMode.put("description", "Storage mode: 0=database, 1=memory (default: 0)");
        properties.set("storageMode", storageMode);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("variableName").add("format"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("variableName") || !params.has("format")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path, variableName, and format parameters are required"
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
            String variableName = params.get("variableName").asText();
            String formatStr = params.get("format").asText();
            String description = params.has("description") ? params.get("description").asText() : null;
            String group = params.has("group") ? params.get("group").asText() : null;
            // Default writable is false (read-only) unless explicitly set to true
            boolean writable = params.has("writable") ? params.get("writable").asBoolean() : false;
            String readPermissions = params.has("readPermissions") ? params.get("readPermissions").asText() : null;
            String writePermissions = params.has("writePermissions") ? params.get("writePermissions").asText() : null;
            int storageMode = params.has("storageMode") ? params.get("storageMode").asInt() : 
                com.tibbo.aggregate.common.server.ModelContextConstants.STORAGE_DATABASE;
            
            Context context = connection.executeWithTimeout(() -> {
                Context ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000);
            
            if (context == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Context not found: " + path
                );
            }
            
            // Parse format
            // For simple single-value variables, use (1, 1) - one record, one field
            // Format string can specify minRecords and maxRecords using M and X elements
            TableFormat format;
            try {
                if (formatStr != null && !formatStr.isEmpty()) {
                    // Check if format string contains M (minRecords) or X (maxRecords) elements
                    // If not, assume it's a simple single-value variable with (1, 1)
                    if (!formatStr.contains("<M") && !formatStr.contains("<X")) {
                        // Simple format - use (1, 1) for single value variables
                        format = new TableFormat(1, 1, formatStr);
                    } else {
                        // Format string contains M or X elements, parse it with default settings
                        // Use default ClassicEncodingSettings (useVisibleSeparators = false)
                        ClassicEncodingSettings settings = new ClassicEncodingSettings(false);
                        format = new TableFormat(formatStr, settings);
                    }
                } else {
                    // Default empty format
                    format = new TableFormat();
                }
            } catch (Exception e) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                    "Invalid format: " + e.getMessage()
                );
            }
            
            // Check if this is a model context
            boolean isModelContext = connection.executeWithTimeout(() -> {
                try {
                    context.getVariable(com.tibbo.aggregate.common.server.ModelContextConstants.V_MODEL_VARIABLES);
                    return true;
                } catch (ContextException e) {
                    return false; // Not a model context
                }
            }, 60000);
            
            if (isModelContext) {
                // For model context: update V_MODEL_VARIABLES variable directly
                connection.executeWithTimeout(() -> {
                    try {
                        CallerController caller = context.getContextManager().getCallerController();
                        
                        // Get mutable clone of modelVariables
                        DataTable modelVariables = 
                            context.getVariableClone(com.tibbo.aggregate.common.server.ModelContextConstants.V_MODEL_VARIABLES, caller);
                        
                        // Check if variable already exists
                        boolean exists = false;
                        for (int i = 0; i < modelVariables.getRecordCount(); i++) {
                            DataRecord rec = modelVariables.getRecord(i);
                            if (variableName.equals(rec.getString(com.tibbo.aggregate.common.context.AbstractContext.FIELD_VD_NAME))) {
                                exists = true;
                                break;
                            }
                        }
                        
                        if (exists) {
                            throw new RuntimeException("Variable already exists: " + variableName);
                        }
                        
                        // Add new record
                        DataRecord newRec = modelVariables.addRecord();
                        
                        // Set required fields using correct field names
                        newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_VD_NAME, variableName);
                        
                        if (description != null) {
                            newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_VD_DESCRIPTION, description);
                        }
                        
                        if (group != null) {
                            newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_VD_GROUP, group);
                        }
                        
                        // Set format as DataTable
                        DataTable formatTable = com.tibbo.aggregate.common.datatable.DataTableBuilding.formatToTable(
                            format, 
                            new com.tibbo.aggregate.common.datatable.encoding.ClassicEncodingSettings(true), false);
                        newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_VD_FORMAT, formatTable);
                        
                        // Set writable
                        newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_VD_WRITABLE, writable);
                        
                        // Set permissions
                        if (readPermissions != null) {
                            newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_VD_READ_PERMISSIONS, readPermissions);
                        } else {
                            newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_VD_READ_PERMISSIONS, 
                                ServerPermissionChecker.OBSERVER_PERMISSIONS);
                        }
                        
                        if (writePermissions != null) {
                            newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_VD_WRITE_PERMISSIONS, writePermissions);
                        } else {
                            newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_VD_WRITE_PERMISSIONS, 
                                ServerPermissionChecker.MANAGER_PERMISSIONS);
                        }
                        
                        // Set storage mode
                        newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_VD_STORAGE_MODE, storageMode);
                        
                        // Set default values for optional fields
                        newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_VD_UPDATE_HISTORY_STORAGE_TIME, 0L);
                        newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_VD_HISTORY_RATE, 
                            com.tibbo.aggregate.common.context.VariableDefinition.HISTORY_RATE_ALL);
                        newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_VD_CACHE_TIME, 0L);
                        newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_VD_SERVER_CACHING_MODE, 
                            com.tibbo.aggregate.common.context.VariableDefinition.CACHING_HARD);
                        newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_VD_ADD_PREVIOUS_VALUE_TO_VARIABLE_UPDATE_EVENT, false);
                        
                        // Update the variable - this will trigger setVmodelVariables and update all instances
                        context.setVariable(com.tibbo.aggregate.common.server.ModelContextConstants.V_MODEL_VARIABLES, caller, modelVariables);
                        
                        return null;
                    } catch (ContextException e) {
                        throw new RuntimeException("Failed to add variable to model context: " + e.getMessage(), e);
                    }
                }, 60000);
                
                // Verify variable was added
                VariableDefinition verifyVd = connection.executeWithTimeout(() -> {
                    return context.getVariableDefinition(variableName);
                }, 60000);
                
                if (verifyVd == null) {
                    throw new McpException(
                        com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                        "Variable was not created in model context - verification failed"
                    );
                }
            } else {
                // For regular context: use addVariableDefinition
                connection.executeWithTimeout(() -> {
                    VariableDefinition vd = new VariableDefinition(
                        variableName, 
                        format, 
                        true, // readable
                        writable, 
                        description != null ? description : variableName,
                        group
                    );
                    
                    if (readPermissions != null) {
                        vd.setReadPermissions(new Permissions(readPermissions));
                    }
                    
                    if (writePermissions != null) {
                        vd.setWritePermissions(new Permissions(writePermissions));
                    }
                    
                    context.addVariableDefinition(vd);
                    
                    return null;
                }, 60000);
                
                // Verify variable exists
                VariableDefinition verifyVd = connection.executeWithTimeout(() -> {
                    return context.getVariableDefinition(variableName);
                }, 60000);
                
                if (verifyVd == null) {
                    throw new McpException(
                        com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                        "Variable was not created - verification failed"
                    );
                }
            }
            
            // Get final variable definition for result
            VariableDefinition verifyVd = connection.executeWithTimeout(() -> {
                return context.getVariableDefinition(variableName);
            }, 60000);
            
            if (verifyVd == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Variable was not created - verification failed"
                );
            }
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Variable created successfully");
            result.put("path", path);
            result.put("variableName", variableName);
            result.put("description", verifyVd.getDescription());
            result.put("group", verifyVd.getGroup());
            result.put("writable", verifyVd.isWritable());
            
            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = ErrorHandler.extractErrorMessage(e);
            ErrorHandler.ErrorDetails errorDetails = ErrorHandler.extractErrorDetails(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to create variable: " + errorMessage,
                errorDetails
            );
        }
    }
}

