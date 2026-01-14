package com.tibbo.aggregate.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.protocol.McpRequestHandler;
import com.tibbo.aggregate.mcp.tools.ToolRegistry;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Main MCP server implementation
 */
public class McpServer implements McpRequestHandler {
    private final ConnectionManager connectionManager;
    private final ToolRegistry toolRegistry;
    
    public McpServer() {
        this.connectionManager = new ConnectionManager();
        this.toolRegistry = new ToolRegistry(connectionManager);
    }
    
    @Override
    public JsonNode handleRequest(String method, JsonNode params) throws McpException {
        switch (method) {
            case "initialize":
                return handleInitialize(params);
            case "tools/list":
                return handleToolsList();
            case "tools/call":
                return handleToolsCall(params);
            case "resources/list":
                return handleResourcesList();
            case "resources/read":
                return handleResourcesRead(params);
            default:
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.METHOD_NOT_FOUND,
                    "Method not found: " + method
                );
        }
    }
    
    private JsonNode handleInitialize(JsonNode params) {
        ObjectNode result = instance.objectNode();
        result.put("protocolVersion", "2024-11-05");
        result.put("serverInfo", instance.objectNode()
            .put("name", "aggregate-mcp-server")
            .put("version", "1.0.0")
        );
        result.set("capabilities", instance.objectNode()
            .set("tools", instance.objectNode()
                .put("listChanged", false)
            )
        );
        return result;
    }
    
    private JsonNode handleToolsList() {
        ObjectNode result = instance.objectNode();
        result.set("tools", toolRegistry.listTools());
        return result;
    }
    
    private JsonNode handleToolsCall(JsonNode params) throws McpException {
        if (!params.has("name")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Tool name is required"
            );
        }
        
        String toolName = params.get("name").asText();
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : instance.objectNode();
        
        JsonNode toolResult = toolRegistry.callTool(toolName, arguments);
        
        // MCP tools/call response format: always return object with "content" field
        // Content is an array of content items, each with "type" and "text" fields
        ObjectNode result = instance.objectNode();
        ArrayNode content = instance.arrayNode();
        ObjectNode contentItem = instance.objectNode();
        contentItem.put("type", "text");
        
        // Serialize the result to JSON string using ObjectMapper for proper JSON encoding
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(toolResult);
            contentItem.put("text", jsonString);
        } catch (Exception e) {
            // Fallback to toString if serialization fails
            if (toolResult.isTextual()) {
                contentItem.put("text", toolResult.asText());
            } else {
                contentItem.put("text", toolResult.toString());
            }
        }
        
        content.add(contentItem);
        result.set("content", content);
        return result;
    }
    
    private JsonNode handleResourcesList() {
        // Return empty list for now - resources can be added later
        ObjectNode result = instance.objectNode();
        result.set("resources", instance.arrayNode());
        return result;
    }
    
    private JsonNode handleResourcesRead(JsonNode params) throws McpException {
        throw new McpException(
            com.tibbo.aggregate.mcp.protocol.McpError.METHOD_NOT_FOUND,
            "Resources not implemented yet"
        );
    }
}

