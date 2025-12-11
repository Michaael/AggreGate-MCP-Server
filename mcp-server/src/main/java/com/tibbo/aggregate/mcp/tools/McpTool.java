package com.tibbo.aggregate.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.protocol.McpException;

/**
 * Interface for MCP tools
 */
public interface McpTool {
    /**
     * Get the tool name
     */
    String getName();
    
    /**
     * Get the tool description
     */
    String getDescription();
    
    /**
     * Get the input schema for the tool
     */
    JsonNode getInputSchema();
    
    /**
     * Execute the tool
     * @param params Parameters
     * @param connectionManager Connection manager
     * @return Result
     * @throws McpException if there's an error
     */
    JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException;
}

