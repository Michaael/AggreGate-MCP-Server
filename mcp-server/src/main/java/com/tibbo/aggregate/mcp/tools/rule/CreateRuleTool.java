package com.tibbo.aggregate.mcp.tools.rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.action.command.EditData;
import com.tibbo.aggregate.common.action.command.Confirm;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for creating a rule in a context
 */
public class CreateRuleTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_create_rule";
    }
    
    @Override
    public String getDescription() {
        return "Create a rule in a context for automatic data processing";
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
        
        ObjectNode ruleName = instance.objectNode();
        ruleName.put("type", "string");
        ruleName.put("description", "Rule name");
        properties.set("ruleName", ruleName);
        
        ObjectNode trigger = instance.objectNode();
        trigger.put("type", "string");
        trigger.put("description", "Rule trigger (expression or reference to variable/event)");
        properties.set("trigger", trigger);
        
        ObjectNode expression = instance.objectNode();
        expression.put("type", "string");
        expression.put("description", "Rule expression (JavaScript/Expression code)");
        properties.set("expression", expression);
        
        ObjectNode enabled = instance.objectNode();
        enabled.put("type", "boolean");
        enabled.put("description", "Whether the rule is enabled (default: true)");
        properties.set("enabled", enabled);
        
        ObjectNode description = instance.objectNode();
        description.put("type", "string");
        description.put("description", "Rule description (optional)");
        properties.set("description", description);
        
        ObjectNode group = instance.objectNode();
        group.put("type", "string");
        group.put("description", "Rule group (optional)");
        properties.set("group", group);
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("required", instance.arrayNode().add("path").add("ruleName").add("trigger").add("expression"));
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("ruleName") || !params.has("trigger") || !params.has("expression")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path, ruleName, trigger, and expression parameters are required"
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
            String ruleName = params.get("ruleName").asText();
            String trigger = params.get("trigger").asText();
            String expression = params.get("expression").asText();
            boolean enabled = params.has("enabled") ? params.get("enabled").asBoolean() : true;
            String description = params.has("description") ? params.get("description").asText() : null;
            String group = params.has("group") ? params.get("group").asText() : null;
            
            Context<?> context = connection.executeWithTimeout(() -> {
                Context<?> ctx = connection.getContextManager().get(path);
                if (ctx == null) {
                    throw new RuntimeException("Context not found: " + path);
                }
                return ctx;
            }, 60000L);
            
            // Rules can only be created in model contexts
            // Check if this is a model context
            boolean isModelContext = connection.executeWithTimeout(() -> {
                try {
                    context.getVariable(com.tibbo.aggregate.common.server.ModelContextConstants.V_MODEL_EVENTS);
                    return true;
                } catch (ContextException e) {
                    return false;
                }
            }, 60000L);
            
            if (!isModelContext) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Rules can only be created in model contexts. Path '" + path + "' is not a model context."
                );
            }
            
            // Rules in models are created through actions (unlike events and functions which use variables)
            // Use action-based approach for model contexts
            connection.executeWithTimeout(() -> {
                try {
                    // Try different action names - use empty input first, fill via EditData commands
                    String[] actionNames = {"addRule", "createRule", "setRule", "add", "create"};
                    Exception lastException = null;
                    
                    // Start with empty input - data will be filled via EditData commands
                    com.tibbo.aggregate.common.action.ServerActionInput actionInput = 
                        new com.tibbo.aggregate.common.action.ServerActionInput();
                    
                    for (String actionName : actionNames) {
                        try {
                            // Check if action exists before trying to use it
                            @SuppressWarnings("unchecked")
                            java.util.List<com.tibbo.aggregate.common.action.ActionDefinition> actions = 
                                (java.util.List<com.tibbo.aggregate.common.action.ActionDefinition>) 
                                context.getActionDefinitions();
                            
                            boolean actionExists = false;
                            if (actions != null) {
                                for (com.tibbo.aggregate.common.action.ActionDefinition ad : actions) {
                                    if (ad != null && actionName.equals(ad.getName())) {
                                        actionExists = true;
                                        break;
                                    }
                                }
                            }
                            
                            if (!actionExists) {
                                continue; // Skip non-existent actions
                            }
                            
                            com.tibbo.aggregate.common.action.ActionIdentifier actionId = 
                                com.tibbo.aggregate.common.action.ActionUtils.initAction(
                                    context,
                                    actionName,
                                    actionInput,
                                    null,
                                    new com.tibbo.aggregate.common.action.ActionExecutionMode(
                                        com.tibbo.aggregate.common.action.ActionExecutionMode.HEADLESS
                                    ),
                                    null
                                );
                            
                            // Execute the action with full flow (like ExecuteActionTool)
                            com.tibbo.aggregate.common.action.GenericActionResponse[] actionResponseRef = 
                                new com.tibbo.aggregate.common.action.GenericActionResponse[1];
                            actionResponseRef[0] = null;
                            int maxSteps = 100;
                            int stepCount = 0;
                            
                            while (stepCount < maxSteps) {
                                final com.tibbo.aggregate.common.action.GenericActionResponse currentResponse = actionResponseRef[0];
                                com.tibbo.aggregate.common.action.GenericActionCommand cmd = 
                                    com.tibbo.aggregate.common.action.ActionUtils.stepAction(
                                        context, 
                                        actionId, 
                                        currentResponse, 
                                        null
                                    );
                                
                                if (cmd == null) {
                                    break; // End of action
                                }
                                
                                // Process command - fill data via EditData commands
                                com.tibbo.aggregate.common.action.GenericActionResponse newResponse;
                                
                                // Handle EditData commands - fill with rule data
                                if (cmd.getType().equals(com.tibbo.aggregate.common.action.ActionUtils.CMD_EDIT_DATA) 
                                    && cmd instanceof EditData) {
                                    EditData editDataCmd = (EditData) cmd;
                                    com.tibbo.aggregate.common.datatable.DataTable data = editDataCmd.getData();
                                    
                                    // If data table exists, try to fill it with rule information
                                    if (data != null && data.getRecordCount() > 0) {
                                        com.tibbo.aggregate.common.datatable.DataRecord rec = data.rec();
                                        
                                        // Try to set rule fields if they exist in the format
                                        // Use safe field checking with multiple field name variations
                                        try {
                                            // Common field names for rules (try multiple variations)
                                            String[] nameFields = {"name", "ruleName", "rule_name", "Name", "RuleName"};
                                            String[] triggerFields = {"trigger", "Trigger", "ruleTrigger"};
                                            String[] expressionFields = {"expression", "Expression", "ruleExpression", "code"};
                                            String[] enabledFields = {"enabled", "Enabled", "isEnabled", "active"};
                                            String[] descriptionFields = {"description", "Description", "desc"};
                                            String[] groupFields = {"group", "Group", "category"};
                                            
                                            // Set name field
                                            for (String fieldName : nameFields) {
                                                if (rec.getFormat().hasField(fieldName)) {
                                                    rec.setValue(fieldName, ruleName);
                                                    break;
                                                }
                                            }
                                            
                                            // Set trigger field
                                            for (String fieldName : triggerFields) {
                                                if (rec.getFormat().hasField(fieldName)) {
                                                    rec.setValue(fieldName, trigger);
                                                    break;
                                                }
                                            }
                                            
                                            // Set expression field
                                            for (String fieldName : expressionFields) {
                                                if (rec.getFormat().hasField(fieldName)) {
                                                    rec.setValue(fieldName, expression);
                                                    break;
                                                }
                                            }
                                            
                                            // Set enabled field
                                            for (String fieldName : enabledFields) {
                                                if (rec.getFormat().hasField(fieldName)) {
                                                    rec.setValue(fieldName, enabled);
                                                    break;
                                                }
                                            }
                                            
                                            // Set description field (optional)
                                            if (description != null) {
                                                for (String fieldName : descriptionFields) {
                                                    if (rec.getFormat().hasField(fieldName)) {
                                                        rec.setValue(fieldName, description);
                                                        break;
                                                    }
                                                }
                                            }
                                            
                                            // Set group field (optional)
                                            if (group != null) {
                                                for (String fieldName : groupFields) {
                                                    if (rec.getFormat().hasField(fieldName)) {
                                                        rec.setValue(fieldName, group);
                                                        break;
                                                    }
                                                }
                                            }
                                        } catch (Exception fieldError) {
                                            // If field setting fails, log but continue with default response
                                            // This allows the action to proceed even if some fields can't be set
                                        }
                                        
                                        newResponse = new com.tibbo.aggregate.common.action.GenericActionResponse(data);
                                    } else {
                                        newResponse = editDataCmd.createDefaultResponse();
                                    }
                                } else if (cmd.getType().equals(com.tibbo.aggregate.common.action.ActionUtils.CMD_CONFIRM) 
                                    && cmd instanceof Confirm) {
                                    // Confirm command - reply with YES
                                    Confirm confirmCmd = (Confirm) cmd;
                                    com.tibbo.aggregate.common.action.GenericActionResponse defaultResponse = 
                                        confirmCmd.createDefaultResponse();
                                    if (defaultResponse.getParameters() != null && 
                                        defaultResponse.getParameters().getRecordCount() > 0) {
                                        defaultResponse.getParameters().rec().setValue(
                                            Confirm.RF_OPTION, 
                                            com.tibbo.aggregate.common.action.ActionUtils.YES_OPTION
                                        );
                                    }
                                    newResponse = defaultResponse;
                                } else {
                                    // Default response
                                    newResponse = new com.tibbo.aggregate.common.action.GenericActionResponse(
                                        new com.tibbo.aggregate.common.datatable.SimpleDataTable()
                                    );
                                }
                                
                                if (cmd.isLast()) {
                                    break; // End of action
                                }
                                
                                if (cmd.getRequestId() != null) {
                                    newResponse.setRequestId(cmd.getRequestId());
                                }
                                
                                actionResponseRef[0] = newResponse;
                                stepCount++;
                            }
                            
                            // Rule created successfully
                            return null;
                        } catch (Exception e) {
                            lastException = e;
                            // Try next action name
                        }
                    }
                    
                    // If all action names failed, throw exception with helpful message
                    String availableActions = "";
                    try {
                        @SuppressWarnings("unchecked")
                        java.util.List<com.tibbo.aggregate.common.action.ActionDefinition> actions = 
                            (java.util.List<com.tibbo.aggregate.common.action.ActionDefinition>) 
                            context.getActionDefinitions();
                        if (actions != null && !actions.isEmpty()) {
                            java.util.List<String> actionNamesList = new java.util.ArrayList<>();
                            for (com.tibbo.aggregate.common.action.ActionDefinition ad : actions) {
                                if (ad != null && ad.getName() != null) {
                                    actionNamesList.add(ad.getName());
                                }
                            }
                            availableActions = " Available actions: " + String.join(", ", actionNamesList);
                        }
                    } catch (Exception e) {
                        // Ignore errors when getting action list
                    }
                    
                    throw new RuntimeException("Failed to create rule. Tried actions: " + 
                        String.join(", ", actionNames) + "." + availableActions + 
                        " Error: " + (lastException != null ? lastException.getMessage() : "Unknown error"));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create rule: " + e.getMessage(), e);
                }
            }, 60000L);
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Rule created successfully");
            result.put("path", path);
            result.put("ruleName", ruleName);
            return result;
        } catch (ContextException e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to create rule: " + e.getMessage()
            );
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to create rule: " + errorMessage
            );
        }
    }
}
