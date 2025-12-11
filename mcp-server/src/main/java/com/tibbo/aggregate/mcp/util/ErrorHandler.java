package com.tibbo.aggregate.mcp.util;

import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.AggreGateException;
import com.tibbo.aggregate.common.AggreGateRuntimeException;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for extracting detailed error messages from AggreGate exceptions
 */
public class ErrorHandler {
    
    /**
     * Extracts a detailed, user-friendly error message from an exception
     * including all nested causes and context information
     */
    public static String extractErrorMessage(Throwable exception) {
        if (exception == null) {
            return "Unknown error";
        }
        
        List<String> messages = new ArrayList<>();
        Throwable current = exception;
        int depth = 0;
        final int MAX_DEPTH = 10; // Prevent infinite loops
        
        while (current != null && depth < MAX_DEPTH) {
            String message = getExceptionMessage(current);
            if (message != null && !message.trim().isEmpty()) {
                messages.add(message);
            }
            
            // Get the cause
            Throwable cause = current.getCause();
            if (cause == current) {
                break; // Circular reference
            }
            current = cause;
            depth++;
        }
        
        // Remove duplicate consecutive messages
        List<String> uniqueMessages = new ArrayList<>();
        String lastMessage = null;
        for (String msg : messages) {
            if (lastMessage == null || !msg.equals(lastMessage)) {
                uniqueMessages.add(msg);
                lastMessage = msg;
            }
        }
        
        // If we have multiple messages, combine them
        if (uniqueMessages.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < uniqueMessages.size(); i++) {
                if (i > 0) {
                    sb.append(" -> ");
                }
                sb.append(uniqueMessages.get(i));
            }
            return sb.toString();
        } else if (uniqueMessages.size() == 1) {
            return uniqueMessages.get(0);
        } else {
            // Fallback to class name
            return exception.getClass().getSimpleName();
        }
    }
    
    /**
     * Gets a formatted message from an exception
     */
    private static String getExceptionMessage(Throwable exception) {
        if (exception instanceof ContextException) {
            ContextException ce = (ContextException) exception;
            String message = ce.getMessage();
            // ContextException may have additional context information
            if (message == null || message.trim().isEmpty()) {
                message = "ContextException occurred";
            }
            return message;
        } else if (exception instanceof AggreGateException) {
            AggreGateException ae = (AggreGateException) exception;
            String message = ae.getMessage();
            if (message == null || message.trim().isEmpty()) {
                message = "AggreGateException occurred";
            }
            return message;
        } else if (exception instanceof AggreGateRuntimeException) {
            AggreGateRuntimeException are = (AggreGateRuntimeException) exception;
            String message = are.getMessage();
            if (message == null || message.trim().isEmpty()) {
                message = "AggreGateRuntimeException occurred";
            }
            return message;
        } else if (exception instanceof RuntimeException) {
            RuntimeException re = (RuntimeException) exception;
            String message = re.getMessage();
            if (message == null || message.trim().isEmpty()) {
                message = "RuntimeException: " + exception.getClass().getSimpleName();
            }
            return message;
        } else {
            String message = exception.getMessage();
            if (message == null || message.trim().isEmpty()) {
                message = exception.getClass().getSimpleName();
            }
            return message;
        }
    }
    
    /**
     * Extracts error details as an object that can be included in error response data
     */
    public static ErrorDetails extractErrorDetails(Throwable exception) {
        ErrorDetails details = new ErrorDetails();
        details.setMessage(extractErrorMessage(exception));
        details.setExceptionType(exception.getClass().getName());
        
        // Extract stack trace (limited to first 5 frames)
        if (exception.getStackTrace() != null && exception.getStackTrace().length > 0) {
            List<String> stackTrace = new ArrayList<>();
            int maxFrames = Math.min(5, exception.getStackTrace().length);
            for (int i = 0; i < maxFrames; i++) {
                StackTraceElement element = exception.getStackTrace()[i];
                // Only include frames from our package
                if (element.getClassName().contains("com.tibbo.aggregate")) {
                    stackTrace.add(element.toString());
                }
            }
            if (!stackTrace.isEmpty()) {
                details.setStackTrace(stackTrace);
            }
        }
        
        return details;
    }
    
    /**
     * Error details that can be included in error response
     */
    public static class ErrorDetails {
        private String message;
        private String exceptionType;
        private List<String> stackTrace;
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getExceptionType() {
            return exceptionType;
        }
        
        public void setExceptionType(String exceptionType) {
            this.exceptionType = exceptionType;
        }
        
        public List<String> getStackTrace() {
            return stackTrace;
        }
        
        public void setStackTrace(List<String> stackTrace) {
            this.stackTrace = stackTrace;
        }
    }
}

