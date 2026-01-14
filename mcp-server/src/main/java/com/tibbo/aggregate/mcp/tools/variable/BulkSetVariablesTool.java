package com.tibbo.aggregate.mcp.tools.variable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for setting multiple variables in a context in a single call.
 * Internally delegates to aggregate_set_variable_smart.
 */
public class BulkSetVariablesTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_bulk_set_variables";
    }

    @Override
    public String getDescription() {
        return "Set multiple variables in a context in a single call (uses aggregate_set_variable_smart internally).";
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

        ObjectNode items = instance.objectNode();
        items.put("type", "array");
        items.put("description", "Array of items: {variableName, value}");
        ObjectNode itemSchema = instance.objectNode();
        itemSchema.put("type", "object");
        ObjectNode itemProps = instance.objectNode();
        itemProps.set("variableName", instance.objectNode().put("type", "string"));
        itemProps.set("value", instance.objectNode().put("description", "Value to set (scalar or object)"));
        itemSchema.set("properties", itemProps);
        itemSchema.set("required", instance.arrayNode().add("variableName").add("value"));
        items.set("items", itemSchema);
        properties.set("items", items);

        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);

        schema.set("required", instance.arrayNode().add("path").add("items"));
        schema.set("properties", properties);
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("items") || !params.get("items").isArray()) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and items array are required"
            );
        }

        String path = params.get("path").asText();
        String connectionKey = params.has("connectionKey") ? params.get("connectionKey").asText() : null;

        ArrayNode items = (ArrayNode) params.get("items");
        ArrayNode results = instance.arrayNode();

        SetVariableSmartTool smartTool = new SetVariableSmartTool();

        for (JsonNode item : items) {
            ObjectNode itemResult = instance.objectNode();
            if (!item.has("variableName") || !item.has("value")) {
                itemResult.put("success", false);
                itemResult.put("error", "variableName and value are required for each item");
                results.add(itemResult);
                continue;
            }

            String variableName = item.get("variableName").asText();
            itemResult.put("variableName", variableName);

            try {
                ObjectNode smartParams = instance.objectNode();
                smartParams.put("path", path);
                smartParams.put("variableName", variableName);
                smartParams.set("value", item.get("value"));
                if (connectionKey != null) {
                    smartParams.put("connectionKey", connectionKey);
                }

                JsonNode smartResult = smartTool.execute(smartParams, connectionManager);
                itemResult.put("success", true);
                if (smartResult != null && smartResult.has("message")) {
                    itemResult.put("message", smartResult.get("message").asText());
                }
            } catch (Exception e) {
                itemResult.put("success", false);
                itemResult.put("error", e.getMessage());
            }

            results.add(itemResult);
        }

        ObjectNode result = instance.objectNode();
        result.put("path", path);
        result.set("results", results);  // Fixed: should be "results" not "items"

        return result;
    }
}

