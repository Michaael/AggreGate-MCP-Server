package com.tibbo.aggregate.mcp.tools.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for getting server information
 */
public class GetServerInfoTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_get_server_info";
    }
    
    @Override
    public String getDescription() {
        return "Get information about the AggreGate server (version, capabilities, etc.)";
    }
    
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();
        
        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);
        
        schema.set("properties", properties);
        return schema;
    }
    
    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
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
            ObjectNode result = instance.objectNode();
            
            // Get server version
            try {
                com.tibbo.aggregate.common.context.Context root = connection.getContextManager().getRoot();
                com.tibbo.aggregate.common.datatable.DataTable versionInfo = 
                    connection.executeWithTimeout(() -> {
                        try {
                            return root.callFunction("getServerVersion");
                        } catch (Exception e) {
                            // Try alternative: check if function exists first
                            try {
                                java.util.List<String> functions = root.getFunctionNames();
                                if (functions != null && functions.contains("getServerVersion")) {
                                    return root.callFunction("getServerVersion", null);
                                }
                            } catch (Exception e2) {
                                // Ignore
                            }
                            return null;
                        }
                    }, 60000);
                
                if (versionInfo != null && versionInfo.getRecordCount() > 0) {
                    com.tibbo.aggregate.common.datatable.DataRecord rec = versionInfo.getRecord(0);
                    if (rec.hasField("version")) {
                        result.put("version", rec.getString("version"));
                    }
                    if (rec.hasField("build")) {
                        result.put("build", rec.getString("build"));
                    }
                    if (rec.hasField("buildNumber")) {
                        result.put("buildNumber", rec.getString("buildNumber"));
                    }
                }
            } catch (Exception e) {
                // Version info not available - will be omitted from result
                // Note: getServerVersion function may not be available in all server versions
            }
            
            // Get connection info
            result.put("connected", true);
            result.put("loggedIn", connection.isLoggedIn());
            
            // Get root context info
            try {
                com.tibbo.aggregate.common.context.Context root = connection.getContextManager().getRoot();
                result.put("rootPath", root.getPath());
                result.put("rootName", root.getName());
            } catch (Exception e) {
                // Root context info not available
            }
            
            // Add capabilities/features info
            ObjectNode capabilities = instance.objectNode();
            capabilities.put("models", true);
            capabilities.put("devices", true);
            capabilities.put("agents", true);
            capabilities.put("widgets", true);
            capabilities.put("dashboards", true);
            capabilities.put("events", true);
            capabilities.put("functions", true);
            capabilities.put("variables", true);
            result.set("capabilities", capabilities);
            
            return result;
        } catch (Exception e) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to get server info: " + e.getMessage()
            );
        }
    }
}
