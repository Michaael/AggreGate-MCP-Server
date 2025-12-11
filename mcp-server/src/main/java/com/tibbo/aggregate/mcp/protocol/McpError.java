package com.tibbo.aggregate.mcp.protocol;

/**
 * Represents an MCP error
 */
public class McpError {
    private int code;
    private String message;
    private Object data;
    
    public McpError() {
    }
    
    public McpError(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public McpError(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    // Standard JSON-RPC error codes
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    
    // Custom error codes for AggreGate
    public static final int CONNECTION_ERROR = -32000;
    public static final int CONTEXT_ERROR = -32001;
    public static final int AUTHENTICATION_ERROR = -32002;
}

