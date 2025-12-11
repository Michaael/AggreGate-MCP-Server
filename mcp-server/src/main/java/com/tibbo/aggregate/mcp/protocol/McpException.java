package com.tibbo.aggregate.mcp.protocol;

/**
 * Exception for MCP protocol errors
 */
public class McpException extends Exception {
    private final int code;
    private final Object data;
    
    public McpException(int code, String message) {
        super(message);
        this.code = code;
        this.data = null;
    }
    
    public McpException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }
    
    public int getCode() {
        return code;
    }
    
    public Object getData() {
        return data;
    }
}

