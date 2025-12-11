package com.tibbo.aggregate.mcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Handles JSON-RPC protocol communication via stdio
 */
public class McpProtocolHandler {
    // Configure ObjectMapper to use UTF-8 encoding
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final McpRequestHandler requestHandler;
    private boolean running = false;
    
    public McpProtocolHandler(McpRequestHandler requestHandler) {
        this.requestHandler = requestHandler;
        // Use UTF-8 encoding explicitly to avoid encoding issues with Russian text
        this.reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        this.writer = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
    }
    
    public void start() throws IOException {
        running = true;
        
        String line;
        while (running && (line = reader.readLine()) != null) {
            try {
                processMessage(line);
            } catch (Exception e) {
                sendError(null, McpError.INTERNAL_ERROR, "Internal error: " + e.getMessage(), null);
            }
        }
    }
    
    public void stop() {
        running = false;
    }
    
    private void processMessage(String line) throws IOException {
        if (line == null || line.trim().isEmpty()) {
            return;
        }
        
        try {
            JsonNode jsonNode = objectMapper.readTree(line);
            McpMessage message = parseMessage(jsonNode);
            
            if (message.isRequest()) {
                handleRequest(message);
            } else if (message.isNotification()) {
                handleNotification(message);
            } else {
                // Response - we don't handle responses in server mode
                // This could be used for bidirectional communication if needed
            }
        } catch (Exception e) {
            sendError(null, McpError.PARSE_ERROR, "Parse error: " + e.getMessage(), null);
        }
    }
    
    private McpMessage parseMessage(JsonNode jsonNode) {
        McpMessage message = new McpMessage();
        
        if (jsonNode.has("jsonrpc")) {
            message.setJsonrpc(jsonNode.get("jsonrpc").asText());
        }
        
        if (jsonNode.has("method")) {
            message.setMethod(jsonNode.get("method").asText());
        }
        
        if (jsonNode.has("params")) {
            message.setParams(jsonNode.get("params"));
        }
        
        if (jsonNode.has("id")) {
            message.setId(jsonNode.get("id"));
        }
        
        if (jsonNode.has("result")) {
            message.setResult(jsonNode.get("result"));
        }
        
        if (jsonNode.has("error")) {
            JsonNode errorNode = jsonNode.get("error");
            McpError error = new McpError();
            if (errorNode.has("code")) {
                error.setCode(errorNode.get("code").asInt());
            }
            if (errorNode.has("message")) {
                error.setMessage(errorNode.get("message").asText());
            }
            if (errorNode.has("data")) {
                error.setData(errorNode.get("data"));
            }
            message.setError(error);
        }
        
        return message;
    }
    
    private void handleRequest(McpMessage request) throws IOException {
        try {
            JsonNode result = requestHandler.handleRequest(request.getMethod(), request.getParams());
            sendResponse(request.getId(), result);
        } catch (McpException e) {
            sendError(request.getId(), e.getCode(), e.getMessage(), e.getData());
        } catch (Exception e) {
            sendError(request.getId(), McpError.INTERNAL_ERROR, "Internal error: " + e.getMessage(), null);
        }
    }
    
    private void handleNotification(McpMessage notification) {
        // Notifications don't require a response
        try {
            requestHandler.handleNotification(notification.getMethod(), notification.getParams());
        } catch (Exception e) {
            // Log but don't send error for notifications
        }
    }
    
    private void sendResponse(Object id, JsonNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", objectMapper.valueToTree(id));
        response.set("result", result);
        
        writer.println(objectMapper.valueToTree(response));
        writer.flush();
    }
    
    private void sendError(Object id, int code, String message, Object data) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) {
            response.put("id", objectMapper.valueToTree(id));
        } else {
            response.putNull("id");
        }
        
        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        if (data != null) {
            error.set("data", objectMapper.valueToTree(data));
        }
        response.set("error", error);
        
        writer.println(objectMapper.valueToTree(response));
        writer.flush();
    }
}

