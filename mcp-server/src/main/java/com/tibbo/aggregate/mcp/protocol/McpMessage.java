package com.tibbo.aggregate.mcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents an MCP message (request or response)
 */
public class McpMessage {
    private String jsonrpc = "2.0";
    private String method;
    private JsonNode params;
    private Object id;
    private JsonNode result;
    private McpError error;
    
    public McpMessage() {
    }
    
    public String getJsonrpc() {
        return jsonrpc;
    }
    
    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public JsonNode getParams() {
        return params;
    }
    
    public void setParams(JsonNode params) {
        this.params = params;
    }
    
    public Object getId() {
        return id;
    }
    
    public void setId(Object id) {
        this.id = id;
    }
    
    public JsonNode getResult() {
        return result;
    }
    
    public void setResult(JsonNode result) {
        this.result = result;
    }
    
    public McpError getError() {
        return error;
    }
    
    public void setError(McpError error) {
        this.error = error;
    }
    
    public boolean isRequest() {
        return method != null && id != null;
    }
    
    public boolean isResponse() {
        return (result != null || error != null) && id != null;
    }
    
    public boolean isNotification() {
        return method != null && id == null;
    }
}

