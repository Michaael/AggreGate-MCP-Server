package com.tibbo.aggregate.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.agent.CreateAgentTool;
import com.tibbo.aggregate.mcp.tools.agent.GetAgentStatusTool;
import com.tibbo.aggregate.mcp.tools.connection.ConnectTool;
import com.tibbo.aggregate.mcp.tools.connection.DisconnectTool;
import com.tibbo.aggregate.mcp.tools.connection.LoginTool;
import com.tibbo.aggregate.mcp.tools.context.CreateContextTool;
import com.tibbo.aggregate.mcp.tools.context.DeleteContextTool;
import com.tibbo.aggregate.mcp.tools.context.GetContextTool;
import com.tibbo.aggregate.mcp.tools.context.ListContextsTool;
import com.tibbo.aggregate.mcp.tools.device.CreateDeviceTool;
import com.tibbo.aggregate.mcp.tools.device.DeleteDeviceTool;
import com.tibbo.aggregate.mcp.tools.device.GetDeviceStatusTool;
import com.tibbo.aggregate.mcp.tools.device.ListDevicesTool;
import com.tibbo.aggregate.mcp.tools.event.CreateEventTool;
import com.tibbo.aggregate.mcp.tools.event.FireEventTool;
import com.tibbo.aggregate.mcp.tools.function.CallFunctionTool;
import com.tibbo.aggregate.mcp.tools.function.CreateFunctionTool;
import com.tibbo.aggregate.mcp.tools.function.ListFunctionsTool;
import com.tibbo.aggregate.mcp.tools.user.CreateUserTool;
import com.tibbo.aggregate.mcp.tools.user.DeleteUserTool;
import com.tibbo.aggregate.mcp.tools.user.ListUsersTool;
import com.tibbo.aggregate.mcp.tools.user.UpdateUserTool;
import com.tibbo.aggregate.mcp.tools.variable.CreateVariableTool;
import com.tibbo.aggregate.mcp.tools.variable.GetVariableTool;
import com.tibbo.aggregate.mcp.tools.variable.ListVariablesTool;
import com.tibbo.aggregate.mcp.tools.variable.SetVariableFieldTool;
import com.tibbo.aggregate.mcp.tools.variable.SetVariableTool;
import com.tibbo.aggregate.mcp.tools.action.ExecuteActionTool;
import com.tibbo.aggregate.mcp.tools.widget.CreateWidgetTool;
import com.tibbo.aggregate.mcp.tools.widget.SetWidgetTemplateTool;
import com.tibbo.aggregate.mcp.tools.dashboard.CreateDashboardTool;
import com.tibbo.aggregate.mcp.tools.dashboard.AddDashboardElementTool;

import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Registry for all MCP tools
 */
public class ToolRegistry {
    private final Map<String, McpTool> tools = new HashMap<>();
    private final ConnectionManager connectionManager;
    
    public ToolRegistry(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        registerAllTools();
    }
    
    private void registerAllTools() {
        // Connection tools
        register(new ConnectTool());
        register(new DisconnectTool());
        register(new LoginTool());
        
        // Context tools
        register(new GetContextTool());
        register(new ListContextsTool());
        register(new CreateContextTool());
        register(new DeleteContextTool());
        
        // Variable tools
        register(new GetVariableTool());
        register(new SetVariableTool());
        register(new SetVariableFieldTool());
        register(new ListVariablesTool());
        register(new CreateVariableTool());
        
        // Function tools
        register(new CallFunctionTool());
        register(new ListFunctionsTool());
        register(new CreateFunctionTool());
        
        // Device tools
        register(new CreateDeviceTool());
        register(new ListDevicesTool());
        register(new DeleteDeviceTool());
        register(new GetDeviceStatusTool());
        
        // User tools
        register(new CreateUserTool());
        register(new ListUsersTool());
        register(new DeleteUserTool());
        register(new UpdateUserTool());
        
        // Event tools
        register(new FireEventTool());
        register(new CreateEventTool());
        
        // Action tools
        register(new ExecuteActionTool());
        
        // Agent tools
        register(new CreateAgentTool());
        register(new GetAgentStatusTool());
        
        // Widget tools
        register(new CreateWidgetTool());
        register(new SetWidgetTemplateTool());
        
        // Note: SetWidgetTemplateTool is registered but may need to be called via aggregate_set_widget_template
        
        // Dashboard tools
        register(new CreateDashboardTool());
        register(new AddDashboardElementTool());
    }
    
    private void register(McpTool tool) {
        tools.put(tool.getName(), tool);
    }
    
    public JsonNode listTools() {
        ArrayNode result = instance.arrayNode();
        for (McpTool tool : tools.values()) {
            ObjectNode toolNode = instance.objectNode();
            toolNode.put("name", tool.getName());
            toolNode.put("description", tool.getDescription());
            toolNode.set("inputSchema", tool.getInputSchema());
            result.add(toolNode);
        }
        return result;
    }
    
    public JsonNode callTool(String toolName, JsonNode params) throws McpException {
        McpTool tool = tools.get(toolName);
        if (tool == null) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.METHOD_NOT_FOUND,
                "Tool not found: " + toolName
            );
        }
        
        return tool.execute(params, connectionManager);
    }
    
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }
}

