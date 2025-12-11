package com.tibbo.aggregate.mcp.connection;

import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.protocol.RemoteServer;
import com.tibbo.aggregate.common.protocol.RemoteServerController;

import java.util.concurrent.*;

/**
 * Wrapper for RemoteServerController with connection state management
 */
public class ServerConnection {
    private static final long DEFAULT_CONNECTION_TIMEOUT = 30000; // 30 seconds
    private static final long DEFAULT_OPERATION_TIMEOUT = 60000; // 60 seconds
    
    private final RemoteServer remoteServer;
    private RemoteServerController controller;
    private boolean connected = false;
    private boolean loggedIn = false;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    public ServerConnection(String host, int port, String username, String password) {
        this.remoteServer = new RemoteServer(host, port, username, password);
    }
    
    public ServerConnection(RemoteServer remoteServer) {
        this.remoteServer = remoteServer;
    }
    
    public synchronized void connect() throws Exception {
        if (connected) {
            return;
        }
        
        System.err.println("[MCP] Connecting to AggreGate server: " + remoteServer.getAddress() + ":" + remoteServer.getPort());
        
        controller = new RemoteServerController(remoteServer, true);
        
        // Execute connect with timeout
        Future<?> connectFuture = executorService.submit(() -> {
            try {
                controller.connect();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        try {
            connectFuture.get(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
            connected = true;
            System.err.println("[MCP] Successfully connected to AggreGate server");
        } catch (TimeoutException e) {
            connectFuture.cancel(true);
            connected = false;
            System.err.println("[MCP ERROR] Connection timeout after " + DEFAULT_CONNECTION_TIMEOUT + "ms");
            throw new Exception("Connection timeout: server did not respond within " + DEFAULT_CONNECTION_TIMEOUT + "ms");
        } catch (ExecutionException e) {
            connected = false;
            Throwable cause = e.getCause();
            System.err.println("[MCP ERROR] Connection failed: " + (cause != null ? cause.getMessage() : e.getMessage()));
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new Exception("Connection failed: " + (cause != null ? cause.getMessage() : e.getMessage()));
        }
    }
    
    public synchronized void login() throws ContextException {
        if (!connected) {
            throw new IllegalStateException("Not connected to server");
        }
        
        if (loggedIn) {
            return;
        }
        
        System.err.println("[MCP] Logging in to AggreGate server");
        
        // Execute login with timeout
        Future<Void> loginFuture = executorService.submit(() -> {
            try {
                controller.login();
                return null;
            } catch (ContextException e) {
                throw new RuntimeException(e);
            }
        });
        
        try {
            loginFuture.get(DEFAULT_OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
            loggedIn = true;
            System.err.println("[MCP] Successfully logged in to AggreGate server");
        } catch (TimeoutException e) {
            loginFuture.cancel(true);
            loggedIn = false;
            System.err.println("[MCP ERROR] Login timeout after " + DEFAULT_OPERATION_TIMEOUT + "ms");
            throw new ContextException("Login timeout: server did not respond within " + DEFAULT_OPERATION_TIMEOUT + "ms");
        } catch (ExecutionException e) {
            loggedIn = false;
            Throwable cause = e.getCause();
            System.err.println("[MCP ERROR] Login failed: " + (cause != null ? cause.getMessage() : e.getMessage()));
            if (cause instanceof ContextException) {
                throw (ContextException) cause;
            }
            throw new ContextException("Login failed: " + (cause != null ? cause.getMessage() : e.getMessage()));
        } catch (InterruptedException e) {
            loginFuture.cancel(true);
            loggedIn = false;
            Thread.currentThread().interrupt();
            throw new ContextException("Login interrupted");
        }
    }
    
    public synchronized void disconnect() {
        if (controller != null && connected) {
            try {
                System.err.println("[MCP] Disconnecting from AggreGate server");
                controller.disconnect();
            } catch (Exception e) {
                System.err.println("[MCP ERROR] Error during disconnect: " + e.getMessage());
                // Ignore disconnect errors
            }
        }
        connected = false;
        loggedIn = false;
        executorService.shutdown();
    }
    
    public synchronized boolean isConnected() {
        return connected && controller != null;
    }
    
    public synchronized boolean isLoggedIn() {
        return loggedIn && isConnected();
    }
    
    public synchronized ContextManager getContextManager() {
        if (!isLoggedIn()) {
            throw new IllegalStateException("Not logged in to server");
        }
        if (controller == null) {
            throw new IllegalStateException("Controller is null");
        }
        return controller.getContextManager();
    }
    
    /**
     * Execute an operation with timeout
     */
    public <T> T executeWithTimeout(Callable<T> operation, long timeoutMs) throws Exception {
        Future<T> future = executorService.submit(operation);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new Exception("Operation timeout after " + timeoutMs + "ms");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new Exception("Operation failed: " + (cause != null ? cause.getMessage() : e.getMessage()));
        }
    }
    
    public RemoteServer getRemoteServer() {
        return remoteServer;
    }
    
    public RemoteServerController getController() {
        return controller;
    }
}

