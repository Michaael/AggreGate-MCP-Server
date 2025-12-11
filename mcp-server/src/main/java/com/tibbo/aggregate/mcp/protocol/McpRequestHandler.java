package com.tibbo.aggregate.mcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for handling MCP requests
 */
public interface McpRequestHandler {
    /**
     * Handle a request
     * @param method Method name
     * @param params Parameters
     * @return Result
     * @throws McpException if there's an error
     */
    JsonNode handleRequest(String method, JsonNode params) throws McpException;
    
    /**
     * Handle a notification (no response required)
     * @param method Method name
     * @param params Parameters
     */
    default void handleNotification(String method, JsonNode params) {
        // Default: ignore notifications
    }
}

