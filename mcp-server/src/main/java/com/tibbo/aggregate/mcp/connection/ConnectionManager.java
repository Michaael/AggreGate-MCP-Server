package com.tibbo.aggregate.mcp.connection;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe connection manager for AggreGate server connections
 */
public class ConnectionManager {
    private final ConcurrentMap<String, ServerConnection> serverConnections = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AgentConnection> agentConnections = new ConcurrentHashMap<>();
    
    private static final String DEFAULT_SERVER_KEY = "default";
    
    /**
     * Get or create a server connection
     */
    public ServerConnection getServerConnection(String host, int port, String username, String password) {
        return getServerConnection(DEFAULT_SERVER_KEY, host, port, username, password);
    }
    
    /**
     * Get or create a server connection with a specific key
     */
    public ServerConnection getServerConnection(String key, String host, int port, String username, String password) {
        return serverConnections.computeIfAbsent(key, k -> 
            new ServerConnection(host, port, username, password)
        );
    }
    
    /**
     * Get existing server connection
     */
    public ServerConnection getServerConnection(String key) {
        if (key == null) {
            key = DEFAULT_SERVER_KEY;
        }
        return serverConnections.get(key);
    }
    
    /**
     * Get default server connection
     */
    public ServerConnection getDefaultServerConnection() {
        return serverConnections.get(DEFAULT_SERVER_KEY);
    }
    
    /**
     * Create or get an agent connection
     */
    public AgentConnection getAgentConnection(String host, int port, String username, String password, 
                                               String agentName, boolean eventConfirmation) {
        String key = agentName != null ? agentName : "default_agent";
        return agentConnections.computeIfAbsent(key, k ->
            new AgentConnection(host, port, username, password, agentName, eventConfirmation)
        );
    }
    
    /**
     * Get existing agent connection
     */
    public AgentConnection getAgentConnection(String agentName) {
        String key = agentName != null ? agentName : "default_agent";
        return agentConnections.get(key);
    }
    
    /**
     * Remove server connection
     */
    public void removeServerConnection(String key) {
        if (key == null) {
            key = DEFAULT_SERVER_KEY;
        }
        ServerConnection conn = serverConnections.remove(key);
        if (conn != null) {
            conn.disconnect();
        }
    }
    
    /**
     * Remove agent connection
     */
    public void removeAgentConnection(String agentName) {
        String key = agentName != null ? agentName : "default_agent";
        AgentConnection conn = agentConnections.remove(key);
        if (conn != null) {
            conn.disconnect();
        }
    }
    
    /**
     * Disconnect all connections
     */
    public void disconnectAll() {
        serverConnections.values().forEach(ServerConnection::disconnect);
        serverConnections.clear();
        agentConnections.values().forEach(AgentConnection::disconnect);
        agentConnections.clear();
    }
}

