package com.tibbo.aggregate.mcp.tools.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.tools.context.GetOrCreateContextTool;
import com.tibbo.aggregate.mcp.tools.variable.CreateVariableTool;
import com.tibbo.aggregate.mcp.tools.event.CreateEventTool;
import com.tibbo.aggregate.mcp.tools.function.CreateFunctionTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * High-level tool for declaratively creating a complete model structure
 * with variables, events, and functions in a single call
 */
public class EnsureModelStructureTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_ensure_model_structure";
    }
    
    @Override
    public String getDescription() {
        return "Declaratively create or update a model context with variables, events, and functions. " +
               "This is a high-level tool that handles the entire model setup in one call.";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Full context path (e.g., 'users.admin.models.my_model')");
        properties.set("path", path);
        
        ObjectNode description = instance.objectNode();
        description.put("type", "string");
        description.put("description", "Model description (optional)");
        properties.set("description", description);
        
        ObjectNode variables = instance.objectNode();
        variables.put("type", "array");
        variables.put("description", "Array of variable definitions. Each variable: {name, format, description?, writable?, group?}");
        ObjectNode variableItem = instance.objectNode();
        variableItem.put("type", "object");
        ObjectNode variableProps = instance.objectNode();
        variableProps.set("name", instance.objectNode().put("type", "string"));
        variableProps.set("format", instance.objectNode().put("type", "string"));
        variableProps.set("description", instance.objectNode().put("type", "string"));
        variableProps.set("writable", instance.objectNode().put("type", "boolean"));
        variableProps.set("group", instance.objectNode().put("type", "string"));
        variableItem.set("properties", variableProps);
        variableItem.set("required", instance.arrayNode().add("name").add("format"));
        variables.set("items", variableItem);
        properties.set("variables", variables);
        
        ObjectNode events = instance.objectNode();
        events.put("type", "array");
        events.put("description", "Array of event definitions. Each event: {name, format?, description?, level?, group?}");
        ObjectNode eventItem = instance.objectNode();
        eventItem.put("type", "object");
        ObjectNode eventProps = instance.objectNode();
        eventProps.set("name", instance.objectNode().put("type", "string"));
        eventProps.set("format", instance.objectNode().put("type", "string"));
        eventProps.set("description", instance.objectNode().put("type", "string"));
        eventProps.set("level", instance.objectNode().put("type", "integer"));
        eventProps.set("group", instance.objectNode().put("type", "string"));
        eventItem.set("properties", eventProps);
        eventItem.set("required", instance.arrayNode().add("name"));
        events.set("items", eventItem);
        properties.set("events", events);
        
        ObjectNode functions = instance.objectNode();
        functions.put("type", "array");
        functions.put("description", "Array of function definitions. Each function: {name, functionType?, inputFormat?, outputFormat?, expression?, query?, description?, group?}");
        ObjectNode functionItem = instance.objectNode();
        functionItem.put("type", "object");
        ObjectNode functionProps = instance.objectNode();
        functionProps.set("name", instance.objectNode().put("type", "string"));
        functionProps.set("functionType", instance.objectNode().put("type", "integer"));
        functionProps.set("inputFormat", instance.objectNode().put("type", "string"));
        functionProps.set("outputFormat", instance.objectNode().put("type", "string"));
        functionProps.set("expression", instance.objectNode().put("type", "string"));
        functionProps.set("query", instance.objectNode().put("type", "string"));
        functionProps.set("description", instance.objectNode().put("type", "string"));
        functionProps.set("group", instance.objectNode().put("type", "string"));
        functionItem.set("properties", functionProps);
        functionItem.set("required", instance.arrayNode().add("name"));
        functions.set("items", functionItem);
        properties.set("functions", functions);
        
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
        
        String path = params.get("path").asText();
        String description = params.has("description") ? params.get("description").asText() : null;
        
        // Step 1: Ensure context exists
        GetOrCreateContextTool getOrCreateContext = new GetOrCreateContextTool();
        ObjectNode contextParams = instance.objectNode();
        contextParams.put("path", path);
        if (description != null) {
            contextParams.put("description", description);
        }
        if (params.has("connectionKey")) {
            contextParams.put("connectionKey", params.get("connectionKey").asText());
        }
        
        JsonNode contextResult = getOrCreateContext.execute(contextParams, connectionManager);
        boolean contextCreated = contextResult.has("created") && contextResult.get("created").asBoolean();
        
        ObjectNode result = instance.objectNode();
        result.put("success", true);
        result.put("path", path);
        result.put("contextCreated", contextCreated);
        
        ArrayNode createdVariables = instance.arrayNode();
        ArrayNode createdEvents = instance.arrayNode();
        ArrayNode createdFunctions = instance.arrayNode();
        
        // Step 2: Create variables
        if (params.has("variables") && params.get("variables").isArray()) {
            CreateVariableTool createVariable = new CreateVariableTool();
            ArrayNode variables = (ArrayNode) params.get("variables");
            
            for (JsonNode varDef : variables) {
                try {
                    ObjectNode varParams = instance.objectNode();
                    varParams.put("path", path);
                    varParams.put("variableName", varDef.get("name").asText());
                    varParams.put("format", varDef.get("format").asText());
                    if (varDef.has("writable")) {
                        varParams.put("writable", varDef.get("writable").asBoolean());
                    } else {
                        varParams.put("writable", true);
                    }
                    if (varDef.has("description")) {
                        varParams.put("description", varDef.get("description").asText());
                    }
                    if (varDef.has("group")) {
                        varParams.put("group", varDef.get("group").asText());
                    }
                    if (params.has("connectionKey")) {
                        varParams.put("connectionKey", params.get("connectionKey").asText());
                    }
                    
                    createVariable.execute(varParams, connectionManager);
                    ObjectNode varInfo = instance.objectNode();
                    varInfo.put("name", varDef.get("name").asText());
                    varInfo.put("success", true);
                    createdVariables.add(varInfo);
                } catch (Exception e) {
                    ObjectNode varInfo = instance.objectNode();
                    varInfo.put("name", varDef.get("name").asText());
                    varInfo.put("success", false);
                    varInfo.put("error", e.getMessage());
                    createdVariables.add(varInfo);
                }
            }
        }
        
        // Step 3: Create events
        if (params.has("events") && params.get("events").isArray()) {
            CreateEventTool createEvent = new CreateEventTool();
            ArrayNode events = (ArrayNode) params.get("events");
            
            for (JsonNode eventDef : events) {
                try {
                    ObjectNode eventParams = instance.objectNode();
                    eventParams.put("path", path);
                    eventParams.put("eventName", eventDef.get("name").asText());
                    if (eventDef.has("format")) {
                        eventParams.put("format", eventDef.get("format").asText());
                    }
                    if (eventDef.has("description")) {
                        eventParams.put("description", eventDef.get("description").asText());
                    }
                    if (eventDef.has("level")) {
                        eventParams.put("level", eventDef.get("level").asInt());
                    } else {
                        eventParams.put("level", 0); // INFO
                    }
                    if (eventDef.has("group")) {
                        eventParams.put("group", eventDef.get("group").asText());
                    }
                    if (params.has("connectionKey")) {
                        eventParams.put("connectionKey", params.get("connectionKey").asText());
                    }
                    
                    createEvent.execute(eventParams, connectionManager);
                    ObjectNode eventInfo = instance.objectNode();
                    eventInfo.put("name", eventDef.get("name").asText());
                    eventInfo.put("success", true);
                    createdEvents.add(eventInfo);
                } catch (Exception e) {
                    ObjectNode eventInfo = instance.objectNode();
                    eventInfo.put("name", eventDef.get("name").asText());
                    eventInfo.put("success", false);
                    eventInfo.put("error", e.getMessage());
                    createdEvents.add(eventInfo);
                }
            }
        }
        
        // Step 4: Create functions
        if (params.has("functions") && params.get("functions").isArray()) {
            CreateFunctionTool createFunction = new CreateFunctionTool();
            ArrayNode functions = (ArrayNode) params.get("functions");
            
            for (JsonNode funcDef : functions) {
                try {
                    ObjectNode funcParams = instance.objectNode();
                    funcParams.put("path", path);
                    funcParams.put("functionName", funcDef.get("name").asText());
                    if (funcDef.has("functionType")) {
                        funcParams.put("functionType", funcDef.get("functionType").asInt());
                    } else {
                        funcParams.put("functionType", 1); // Expression by default
                    }
                    if (funcDef.has("inputFormat")) {
                        funcParams.put("inputFormat", funcDef.get("inputFormat").asText());
                    }
                    if (funcDef.has("outputFormat")) {
                        funcParams.put("outputFormat", funcDef.get("outputFormat").asText());
                    }
                    if (funcDef.has("expression")) {
                        funcParams.put("expression", funcDef.get("expression").asText());
                    }
                    if (funcDef.has("query")) {
                        funcParams.put("query", funcDef.get("query").asText());
                    }
                    if (funcDef.has("description")) {
                        funcParams.put("description", funcDef.get("description").asText());
                    }
                    if (funcDef.has("group")) {
                        funcParams.put("group", funcDef.get("group").asText());
                    }
                    if (params.has("connectionKey")) {
                        funcParams.put("connectionKey", params.get("connectionKey").asText());
                    }
                    
                    createFunction.execute(funcParams, connectionManager);
                    ObjectNode funcInfo = instance.objectNode();
                    funcInfo.put("name", funcDef.get("name").asText());
                    funcInfo.put("success", true);
                    createdFunctions.add(funcInfo);
                } catch (Exception e) {
                    ObjectNode funcInfo = instance.objectNode();
                    funcInfo.put("name", funcDef.get("name").asText());
                    funcInfo.put("success", false);
                    funcInfo.put("error", e.getMessage());
                    createdFunctions.add(funcInfo);
                }
            }
        }
        
        result.set("variables", createdVariables);
        result.set("events", createdEvents);
        result.set("functions", createdFunctions);
        
        result.put("summary", String.format("Created context: %s, Variables: %d/%d, Events: %d/%d, Functions: %d/%d",
            contextCreated ? "yes" : "already existed",
            createdVariables.size() - (int)createdVariables.findValues("success").stream().filter(v -> !v.asBoolean()).count(),
            createdVariables.size(),
            createdEvents.size() - (int)createdEvents.findValues("success").stream().filter(v -> !v.asBoolean()).count(),
            createdEvents.size(),
            createdFunctions.size() - (int)createdFunctions.findValues("success").stream().filter(v -> !v.asBoolean()).count(),
            createdFunctions.size()));
        
        return result;
    }
}
