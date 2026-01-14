package com.tibbo.aggregate.mcp.tools.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.EventDefinition;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.event.EventLevel;
import com.tibbo.aggregate.common.security.Permissions;
import com.tibbo.aggregate.common.security.ServerPermissionChecker;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for creating an event in a context (including model context)
 */
public class CreateEventTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_create_event";
    }
    
    @Override
    public String getDescription() {
        return "Create an event in a context (supports model context). " +
               "⚠️ CRITICAL: Events in models are NOT created automatically - you MUST create them explicitly! " +
               "Events allow notifications about important state changes. " +
               "See docs/AI_CONTEXT_CREATION_COMPLETE_GUIDE.md for complete guide.";
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
        
        ObjectNode format = instance.objectNode();
        format.put("type", "string");
        format.put("description", "Event format as TableFormat string (optional, default: empty)");
        properties.set("format", format);
        
        ObjectNode description = instance.objectNode();
        description.put("type", "string");
        description.put("description", "Event description (optional)");
        properties.set("description", description);
        
        ObjectNode group = instance.objectNode();
        group.put("type", "string");
        group.put("description", "Event group (optional)");
        properties.set("group", group);
        
        ObjectNode level = instance.objectNode();
        level.put("type", "integer");
        level.put("description", "Event level: 0=INFO, 1=WARNING, 2=ERROR, 3=FATAL, 4=NOTICE (default: 0)");
        properties.set("level", level);
        
        ObjectNode permissions = instance.objectNode();
        permissions.put("type", "string");
        permissions.put("description", "Read permissions (optional, default: observer)");
        properties.set("permissions", permissions);
        
        ObjectNode firePermissions = instance.objectNode();
        firePermissions.put("type", "string");
        firePermissions.put("description", "Fire permissions (optional, default: admin)");
        properties.set("firePermissions", firePermissions);
        
        ObjectNode historyStorageTime = instance.objectNode();
        historyStorageTime.put("type", "integer");
        historyStorageTime.put("description", "History storage time in milliseconds (optional, default: 0)");
        properties.set("historyStorageTime", historyStorageTime);
        
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
            String formatStr = params.has("format") ? params.get("format").asText() : null;
            String description = params.has("description") ? params.get("description").asText() : null;
            String group = params.has("group") ? params.get("group").asText() : null;
            int level = params.has("level") ? params.get("level").asInt() : EventLevel.INFO;
            String permissions = params.has("permissions") ? params.get("permissions").asText() : null;
            String firePermissions = params.has("firePermissions") ? params.get("firePermissions").asText() : null;
            long historyStorageTime = params.has("historyStorageTime") ? params.get("historyStorageTime").asLong() : 0L;
            
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
            
            // Parse format if provided
            final TableFormat format;
            if (formatStr != null && !formatStr.isEmpty()) {
                try {
                    // Use constructor with minRecords, maxRecords, formatString
                    format = new TableFormat(0, Integer.MAX_VALUE, formatStr);
                } catch (Exception e) {
                    throw new McpException(
                        com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                        "Invalid format: " + e.getMessage()
                    );
                }
            } else {
                // Default empty format
                format = new TableFormat();
            }
            
            // Check if this is a model context
            boolean isModelContext = connection.executeWithTimeout(() -> {
                try {
                    context.getVariable(com.tibbo.aggregate.common.server.ModelContextConstants.V_MODEL_EVENTS);
                    return true;
                } catch (ContextException e) {
                    return false; // Not a model context
                }
            }, 60000);
            
            if (isModelContext) {
                // For model context: update V_MODEL_EVENTS variable directly
                connection.executeWithTimeout(() -> {
                    try {
                        CallerController caller = context.getContextManager().getCallerController();
                        
                        // Get mutable clone of modelEvents
                        DataTable modelEvents = 
                            context.getVariableClone(com.tibbo.aggregate.common.server.ModelContextConstants.V_MODEL_EVENTS, caller);
                        
                        // Check if event already exists
                        boolean exists = false;
                        for (int i = 0; i < modelEvents.getRecordCount(); i++) {
                            DataRecord rec = modelEvents.getRecord(i);
                            if (eventName.equals(rec.getString(com.tibbo.aggregate.common.context.AbstractContext.FIELD_ED_NAME))) {
                                exists = true;
                                break;
                            }
                        }
                        
                        if (exists) {
                            throw new RuntimeException("Event already exists: " + eventName);
                        }
                        
                        // Add new record
                        DataRecord newRec = modelEvents.addRecord();
                        
                        // Set required fields using correct field names
                        newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_ED_NAME, eventName);
                        
                        if (description != null) {
                            newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_ED_DESCRIPTION, description);
                        }
                        
                        if (group != null) {
                            newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_ED_GROUP, group);
                        }
                        
                        // Set format as DataTable
                        DataTable formatTable = com.tibbo.aggregate.common.datatable.DataTableBuilding.formatToTable(
                            format, 
                            new com.tibbo.aggregate.common.datatable.encoding.ClassicEncodingSettings(true), false);
                        newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_ED_FORMAT, formatTable);
                        
                        // Set level
                        newRec.setValue(com.tibbo.aggregate.common.context.AbstractContext.FIELD_ED_LEVEL, level);
                        
                        // Set permissions
                        if (permissions != null) {
                            newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_ED_PERMISSIONS, permissions);
                        } else {
                            newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_ED_PERMISSIONS, 
                                ServerPermissionChecker.OBSERVER_PERMISSIONS);
                        }
                        
                        if (firePermissions != null) {
                            newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_ED_FIRE_PERMISSIONS, firePermissions);
                        } else {
                            newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_ED_FIRE_PERMISSIONS, 
                                ServerPermissionChecker.ADMIN_PERMISSIONS);
                        }
                        
                        // Set history storage time
                        newRec.setValue(com.tibbo.aggregate.common.server.ModelContextConstants.FIELD_ED_HISTORY_STORAGE_TIME, historyStorageTime);
                        
                        // Update the variable - this will trigger setVmodelEvents and update all instances
                        context.setVariable(com.tibbo.aggregate.common.server.ModelContextConstants.V_MODEL_EVENTS, caller, modelEvents);
                        
                        return null;
                    } catch (ContextException e) {
                        throw new RuntimeException("Failed to add event to model context: " + e.getMessage(), e);
                    }
                }, 60000);
                
                // Verify event was added with retries and delay
                EventDefinition verifyEd = null;
                int maxRetries = 5;
                int retryDelay = 200; // milliseconds
                
                for (int retry = 0; retry < maxRetries; retry++) {
                    if (retry > 0) {
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    
                    verifyEd = connection.executeWithTimeout(() -> {
                        return context.getEventDefinition(eventName);
                    }, 60000);
                    
                    if (verifyEd != null) {
                        break;
                    }
                }
                
                if (verifyEd == null) {
                    // Try to check in modelEvents variable as fallback
                    try {
                        com.tibbo.aggregate.common.context.CallerController caller = 
                            context.getContextManager().getCallerController();
                        com.tibbo.aggregate.common.datatable.DataTable modelEvents = 
                            context.getVariable(com.tibbo.aggregate.common.server.ModelContextConstants.V_MODEL_EVENTS, caller);
                        
                        boolean found = false;
                        for (int i = 0; i < modelEvents.getRecordCount(); i++) {
                            com.tibbo.aggregate.common.datatable.DataRecord rec = modelEvents.getRecord(i);
                            if (eventName.equals(rec.getString(com.tibbo.aggregate.common.context.AbstractContext.FIELD_ED_NAME))) {
                                found = true;
                                break;
                            }
                        }
                        
                        if (found) {
                            // Event exists in modelEvents, verification passed
                            verifyEd = context.getEventDefinition(eventName);
                        }
                    } catch (Exception e) {
                        // Ignore - will throw error below
                    }
                }
                
                if (verifyEd == null) {
                    throw new McpException(
                        com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                        "Event was not created in model context - verification failed"
                    );
                }
            } else {
                // For regular context: use addEventDefinition
                connection.executeWithTimeout(() -> {
                    EventDefinition ed = new EventDefinition(
                        eventName, 
                        format, 
                        description != null ? description : eventName,
                        group
                    );
                    
                    ed.setLevel(level);
                    
                    if (permissions != null) {
                        ed.setPermissions(new Permissions(permissions));
                    }
                    
                    if (firePermissions != null) {
                        ed.setFirePermissions(new Permissions(firePermissions));
                    }
                    
                    if (historyStorageTime > 0) {
                        ed.setExpirationPeriod(historyStorageTime);
                    }
                    
                    context.addEventDefinition(ed);
                    
                    return null;
                }, 60000);
                
                // Verify event exists
                EventDefinition verifyEd = connection.executeWithTimeout(() -> {
                    return context.getEventDefinition(eventName);
                }, 60000);
                
                if (verifyEd == null) {
                    throw new McpException(
                        com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                        "Event was not created - verification failed"
                    );
                }
            }
            
            // Get final event definition for result
            EventDefinition verifyEd = connection.executeWithTimeout(() -> {
                return context.getEventDefinition(eventName);
            }, 60000);
            
            if (verifyEd == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Event was not created - verification failed"
                );
            }
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Event created successfully");
            result.put("path", path);
            result.put("eventName", eventName);
            result.put("description", verifyEd.getDescription());
            result.put("group", verifyEd.getGroup());
            result.put("level", verifyEd.getLevel());
            
            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            com.tibbo.aggregate.mcp.util.ErrorHandler.ErrorDetails errorDetails = 
                com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorDetails(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to create event: " + errorMessage,
                errorDetails
            );
        }
    }
}

