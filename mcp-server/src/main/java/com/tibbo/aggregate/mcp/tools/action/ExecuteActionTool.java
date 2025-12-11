package com.tibbo.aggregate.mcp.tools.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.action.ActionExecutionMode;
import com.tibbo.aggregate.common.action.ActionIdentifier;
import com.tibbo.aggregate.common.action.ActionUtils;
import com.tibbo.aggregate.common.action.ServerActionInput;
import com.tibbo.aggregate.common.action.GenericActionCommand;
import com.tibbo.aggregate.common.action.GenericActionResponse;
import com.tibbo.aggregate.common.action.command.EditData;
import com.tibbo.aggregate.common.action.command.Confirm;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.DataTableConverter;
import com.tibbo.aggregate.mcp.util.ErrorHandler;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for executing a server action
 * Note: This is a simplified version. Full action execution may require handling multiple steps.
 */
public class ExecuteActionTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_execute_action";
    }
    
    @Override
    public String getDescription() {
        return "Execute a server action (simplified - may not work for all actions)";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Context path where action should be executed");
        properties.set("path", path);
        
        ObjectNode actionName = instance.objectNode();
        actionName.put("type", "string");
        actionName.put("description", "Action name");
        properties.set("actionName", actionName);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        ObjectNode input = instance.objectNode();
        input.put("type", "object");
        input.put("description", "Action input parameters as DataTable JSON (optional)");
        properties.set("input", input);
        
        ObjectNode filePath = instance.objectNode();
        filePath.put("type", "string");
        filePath.put("description", "Optional file path for export/import actions");
        properties.set("filePath", filePath);
        
        schema.set("required", instance.arrayNode().add("path").add("actionName"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("actionName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and actionName parameters are required"
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
            String path = params.get("path").asText();
            String actionName = params.get("actionName").asText();
            String filePath = params.has("filePath") ? params.get("filePath").asText() : null;
            
            Context context = connection.getContextManager().get(path);
            
            // Create ServerActionInput with optional parameters
            ServerActionInput actionInput;
            if (params.has("input") && params.get("input").isObject()) {
                com.tibbo.aggregate.common.datatable.DataTable inputData = 
                    DataTableConverter.fromJson(params.get("input"));
                actionInput = ActionUtils.createActionInput(inputData);
            } else {
                actionInput = new ServerActionInput();
            }
            
            // Initialize action (like in ExecuteAction example)
            ActionIdentifier actionId = connection.executeWithTimeout(() -> {
                try {
                    return ActionUtils.initAction(
                        context, 
                        actionName, 
                        actionInput, 
                        null, 
                        new ActionExecutionMode(ActionExecutionMode.HEADLESS), 
                        null
                    );
                } catch (ContextException e) {
                    throw new RuntimeException("Failed to init action: " + e.getMessage(), e);
                }
            }, 60000);
            
            // Execute action steps in a loop (like in ExecuteAction example)
            // Initial response sent at first step should be NULL
            GenericActionResponse[] actionResponseRef = new GenericActionResponse[1];
            actionResponseRef[0] = null;
            int maxSteps = 100; // Prevent infinite loops
            int stepCount = 0;
            
            while (stepCount < maxSteps) {
                // Performing action step and getting next UI procedure to execute
                final GenericActionResponse currentResponse = actionResponseRef[0];
                GenericActionCommand cmd = connection.executeWithTimeout(() -> {
                    try {
                        return ActionUtils.stepAction(context, actionId, currentResponse, null);
                    } catch (ContextException e) {
                        throw new RuntimeException("Failed to step action: " + e.getMessage(), e);
                    }
                }, 60000);
                
                if (cmd == null) {
                    break; // End of action
                }
                
                // Log command type for debugging
                System.err.println("Action command received: " + cmd.getType() + " (title: " + cmd.getTitle() + ")");
                if (cmd.getParameters() != null) {
                    System.err.println("Command parameters: " + cmd.getParameters().encode());
                }
                
                // Process command (like in ExecuteAction.processCommand)
                GenericActionResponse newResponse = processCommand(cmd, actionName, filePath);
                
                if (cmd.isLast()) {
                    break; // End of action
                }
                
                // Replicating request ID to the reply
                if (cmd.getRequestId() != null) {
                    newResponse.setRequestId(cmd.getRequestId());
                }
                
                actionResponseRef[0] = newResponse;
                stepCount++;
            }
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Action executed");
            result.put("actionId", actionId.getId());
            result.put("stepsExecuted", stepCount);
            
            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = ErrorHandler.extractErrorMessage(e);
            ErrorHandler.ErrorDetails errorDetails = ErrorHandler.extractErrorDetails(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to execute action: " + errorMessage,
                errorDetails
            );
        }
    }
    
    /**
     * Process action command (like in ExecuteAction.processCommand example)
     * This method emulates execution of UI procedures by processing action commands
     */
    private GenericActionResponse processCommand(GenericActionCommand cmd, String actionName, String filePath) {
        // Extract parameters
        com.tibbo.aggregate.common.datatable.DataTable parameters = cmd.getParameters();
        
        // Edit Data command received - process it
        if (cmd.getType().equals(ActionUtils.CMD_EDIT_DATA) && cmd instanceof EditData) {
            EditData editDataCmd = (EditData) cmd;
            // The data to be edited
            com.tibbo.aggregate.common.datatable.DataTable data = null;
            if (parameters != null && parameters.getRecordCount() > 0) {
                com.tibbo.aggregate.common.datatable.DataRecord paramsRecord = parameters.rec();
                if (paramsRecord.getFormat().hasField(EditData.CF_DATA)) {
                    data = paramsRecord.getDataTable(EditData.CF_DATA);
                }
            }
            
            // If no data from parameters, try to get it from command directly
            if (data == null) {
                data = editDataCmd.getData();
            }
            
            // For export/import actions, if filePath is provided and data table has a file field, set it
            if (filePath != null && (actionName.equals("export") || actionName.equals("import"))) {
                System.err.println("Processing export/import with filePath: " + filePath);
                System.err.println("Data table: " + (data != null ? "exists" : "null"));
                if (data != null) {
                    System.err.println("Data table record count: " + data.getRecordCount());
                    System.err.println("Data table format fields: " + (data.getFormat() != null ? data.getFormat().getFieldCount() : 0));
                }
                
                if (data != null && data.getRecordCount() > 0) {
                    com.tibbo.aggregate.common.datatable.DataRecord record = data.rec();
                    System.err.println("Record format fields: " + record.getFormat().getFieldCount());
                    
                    // First, try to find a field that might be for file path (common names: "file", "path", "fileName")
                    boolean filePathSet = false;
                    for (int i = 0; i < record.getFormat().getFieldCount(); i++) {
                        com.tibbo.aggregate.common.datatable.FieldFormat field = record.getFormat().getField(i);
                        String fieldName = field.getName().toLowerCase();
                        System.err.println("Checking field: " + field.getName() + " (type: " + field.getType() + ")");
                        
                        // Check if this looks like a file path field
                        if (fieldName.contains("file") || fieldName.contains("path") || fieldName.equals("name")) {
                            try {
                                if (field.getType() == com.tibbo.aggregate.common.datatable.FieldFormat.STRING_FIELD) {
                                    record.setValue(field.getName(), filePath);
                                    System.err.println("✓ Set file path field '" + field.getName() + "' to: " + filePath);
                                    filePathSet = true;
                                    break;
                                }
                            } catch (Exception e) {
                                System.err.println("✗ Failed to set file path in field '" + field.getName() + "': " + e.getMessage());
                            }
                        }
                    }
                    
                    // If no specific file field found, try to set the first string field
                    if (!filePathSet) {
                        System.err.println("No specific file field found, trying first string field...");
                        for (int i = 0; i < record.getFormat().getFieldCount(); i++) {
                            com.tibbo.aggregate.common.datatable.FieldFormat field = record.getFormat().getField(i);
                            if (field.getType() == com.tibbo.aggregate.common.datatable.FieldFormat.STRING_FIELD) {
                                try {
                                    record.setValue(field.getName(), filePath);
                                    System.err.println("✓ Set first string field '" + field.getName() + "' to: " + filePath);
                                    filePathSet = true;
                                    break;
                                } catch (Exception e) {
                                    System.err.println("✗ Failed to set file path in field '" + field.getName() + "': " + e.getMessage());
                                }
                            }
                        }
                    }
                } else {
                    // If no data table, create one with file path
                    System.err.println("No data table found, creating new one with file path...");
                    com.tibbo.aggregate.common.datatable.SimpleDataTable newData = 
                        new com.tibbo.aggregate.common.datatable.SimpleDataTable();
                    com.tibbo.aggregate.common.datatable.TableFormat format = 
                        new com.tibbo.aggregate.common.datatable.TableFormat(1, 1);
                    format.addField("<file><S>");
                    newData.setFormat(format);
                    newData.addRecord().addString(filePath);
                    data = newData;
                    System.err.println("✓ Created new data table with file path: " + filePath);
                }
            }
            
            // Use createDefaultResponse which creates response with the data
            if (data != null) {
                return new GenericActionResponse(data);
            } else {
                return editDataCmd.createDefaultResponse();
            }
        }
        
        // Confirm command received
        if (cmd.getType().equals(ActionUtils.CMD_CONFIRM) && cmd instanceof Confirm) {
            Confirm confirmCmd = (Confirm) cmd;
            // Reply with "Yes" by default (can be made configurable)
            // Use createDefaultResponse and then set YES_OPTION
            GenericActionResponse defaultResponse = confirmCmd.createDefaultResponse();
            if (defaultResponse.getParameters() != null && defaultResponse.getParameters().getRecordCount() > 0) {
                defaultResponse.getParameters().rec().setValue(Confirm.RF_OPTION, ActionUtils.YES_OPTION);
            }
            return defaultResponse;
        }
        
        // For other command types, use default response
        if (cmd instanceof EditData) {
            return ((EditData) cmd).createDefaultResponse();
        }
        
        // Fallback: return empty response
        return new GenericActionResponse(new com.tibbo.aggregate.common.datatable.SimpleDataTable());
    }
}

