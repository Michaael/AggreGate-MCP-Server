package com.tibbo.aggregate.mcp.tools.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for deleting a context
 */
public class DeleteContextTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_delete_context";
    }
    
    @Override
    public String getDescription() {
        return "Delete a context using removeChild or destroy methods";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Context path to delete");
        properties.set("path", path);
        
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
            Context context = connection.executeWithTimeout(() -> {
                return connection.getContextManager().get(path);
            }, 60000);
            
            if (context == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Context not found: " + path
                );
            }
            
            Context parent = connection.executeWithTimeout(() -> {
                return context.getParent();
            }, 60000);
            
            if (parent == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Cannot delete root context"
                );
            }
            
            // Delete context using multiple methods (try in order)
            final String contextName = context.getName();
            connection.executeWithTimeout(() -> {
                try {
                    // Method 1: removeChild by name
                    parent.removeChild(contextName);
                } catch (Exception e1) {
                    try {
                        // Method 2: removeChild by context object
                        parent.removeChild(context);
                    } catch (Exception e2) {
                        try {
                            // Method 3: destroy
                            context.destroy(false);
                        } catch (Exception e3) {
                            throw new RuntimeException("All deletion methods failed: " + 
                                e1.getMessage() + ", " + e2.getMessage() + ", " + e3.getMessage());
                        }
                    }
                }
                return null;
            }, 60000);
            
            // Verify that context was actually deleted
            Context verifyContext = connection.executeWithTimeout(() -> {
                return connection.getContextManager().get(path);
            }, 60000);
            
            if (verifyContext != null) {
                // Context still exists, try one more time with destroy(true) to force deletion
                try {
                    connection.executeWithTimeout(() -> {
                        Context ctx = connection.getContextManager().get(path);
                        if (ctx != null) {
                            ctx.destroy(true); // Force deletion
                        }
                        return null;
                    }, 60000);
                    
                    // Verify again
                    verifyContext = connection.executeWithTimeout(() -> {
                        return connection.getContextManager().get(path);
                    }, 60000);
                    
                    if (verifyContext != null) {
                        throw new McpException(
                            com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                            "Context deletion reported success but context still exists: " + path
                        );
                    }
                } catch (Exception e) {
                    throw new McpException(
                        com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                        "Context deletion reported success but verification failed: " + path + " - " + e.getMessage()
                    );
                }
            }
            
            ObjectNode result = instance.objectNode();
            result.put("success", true);
            result.put("message", "Context deleted successfully");
            result.put("path", path);
            
            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            com.tibbo.aggregate.mcp.util.ErrorHandler.ErrorDetails errorDetails = 
                com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorDetails(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to delete context: " + errorMessage,
                errorDetails
            );
        }
    }
}

