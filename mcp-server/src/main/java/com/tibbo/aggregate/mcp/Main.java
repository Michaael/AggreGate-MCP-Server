package com.tibbo.aggregate.mcp;

import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.mcp.protocol.McpProtocolHandler;

import java.io.IOException;

/**
 * Main entry point for MCP server
 */
public class Main {
    public static void main(String[] args) {
        try {
            // Initialize AggreGate logging
            Log.start();
            
            // Create MCP server
            McpServer server = new McpServer();
            
            // Create protocol handler
            McpProtocolHandler handler = new McpProtocolHandler(server);
            
            // Start handling requests
            handler.start();
        } catch (IOException e) {
            System.err.println("Failed to start MCP server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

