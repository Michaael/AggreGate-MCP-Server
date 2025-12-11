package com.tibbo.aggregate.mcp.util;

import com.tibbo.aggregate.common.context.ContextUtils;

/**
 * Utility class for parsing and validating context paths
 */
public class ContextPathParser {
    
    /**
     * Parse and validate a context path
     * @param path Context path string (e.g., "users.admin", "users.admin.devices.device1")
     * @return Validated context path
     * @throws IllegalArgumentException if path is invalid
     */
    public static String parsePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Context path cannot be null or empty");
        }
        
        String trimmed = path.trim();
        
        // Basic validation - paths should not contain invalid characters
        if (trimmed.contains("..") || trimmed.startsWith(".")) {
            throw new IllegalArgumentException("Invalid context path: " + path);
        }
        
        return trimmed;
    }
    
    /**
     * Build user context path
     */
    public static String userContextPath(String username) {
        return ContextUtils.userContextPath(username);
    }
    
    /**
     * Build device context path
     */
    public static String deviceContextPath(String username, String deviceName) {
        return ContextUtils.deviceContextPath(username, deviceName);
    }
    
    /**
     * Build devices container context path
     */
    public static String devicesContextPath(String username) {
        return ContextUtils.devicesContextPath(username);
    }
    
    /**
     * Expand mask to context paths (for listing)
     */
    public static String expandMask(String mask) {
        if (mask == null || mask.trim().isEmpty()) {
            return ContextUtils.CONTEXT_GROUP_MASK;
        }
        return mask;
    }
}

