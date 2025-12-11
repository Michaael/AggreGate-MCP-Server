package com.tibbo.aggregate.mcp.connection;

import com.tibbo.aggregate.common.agent.Agent;
import com.tibbo.aggregate.common.agent.AgentContext;
import com.tibbo.aggregate.common.device.DisconnectionException;
import com.tibbo.aggregate.common.protocol.RemoteServer;
import com.tibbo.aggregate.common.util.SyntaxErrorException;

import java.io.IOException;

/**
 * Wrapper for Agent with connection state management
 */
public class AgentConnection {
    private final RemoteServer remoteServer;
    private final String agentName;
    private final boolean eventConfirmation;
    private Agent agent;
    private boolean connected = false;
    
    public AgentConnection(String host, int port, String username, String password, String agentName, boolean eventConfirmation) {
        this.remoteServer = new RemoteServer(host, port, username, password);
        this.agentName = agentName;
        this.eventConfirmation = eventConfirmation;
    }
    
    public AgentConnection(RemoteServer remoteServer, String agentName, boolean eventConfirmation) {
        this.remoteServer = remoteServer;
        this.agentName = agentName;
        this.eventConfirmation = eventConfirmation;
    }
    
    public synchronized void connect() throws Exception {
        if (connected) {
            return;
        }
        
        AgentContext agentContext = new AgentContext(remoteServer, agentName, eventConfirmation);
        agent = new Agent(agentContext, false, false, 0);
        agent.connect();
        connected = true;
    }
    
    public synchronized void disconnect() {
        if (agent != null && connected) {
            try {
                agent.disconnect();
            } catch (Exception e) {
                // Ignore disconnect errors
            }
        }
        connected = false;
    }
    
    public synchronized boolean isConnected() {
        return connected && agent != null;
    }
    
    public synchronized void run() throws DisconnectionException, SyntaxErrorException, IOException {
        if (!isConnected()) {
            throw new IllegalStateException("Agent not connected");
        }
        agent.run();
    }
    
    public Agent getAgent() {
        return agent;
    }
    
    public AgentContext getContext() {
        if (agent == null) {
            return null;
        }
        return agent.getContext();
    }
    
    public RemoteServer getRemoteServer() {
        return remoteServer;
    }
    
    public String getAgentName() {
        return agentName;
    }
}

