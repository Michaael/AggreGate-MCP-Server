package com.tibbo.aggregate.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.agent.CreateAgentTool;
import com.tibbo.aggregate.mcp.tools.agent.GetAgentStatusTool;
import com.tibbo.aggregate.mcp.tools.agent.WaitAgentReadyTool;
import com.tibbo.aggregate.mcp.tools.agent.SendAgentEventSimpleTool;
import com.tibbo.aggregate.mcp.tools.connection.ConnectTool;
import com.tibbo.aggregate.mcp.tools.connection.DisconnectTool;
import com.tibbo.aggregate.mcp.tools.connection.LoginTool;
import com.tibbo.aggregate.mcp.tools.context.CreateContextTool;
import com.tibbo.aggregate.mcp.tools.context.DeleteContextTool;
import com.tibbo.aggregate.mcp.tools.context.GetContextTool;
import com.tibbo.aggregate.mcp.tools.context.ListContextsTool;
import com.tibbo.aggregate.mcp.tools.context.ListContextTreeTool;
import com.tibbo.aggregate.mcp.tools.device.CreateDeviceTool;
import com.tibbo.aggregate.mcp.tools.device.DeleteDeviceTool;
import com.tibbo.aggregate.mcp.tools.device.GetDeviceStatusTool;
import com.tibbo.aggregate.mcp.tools.device.ListDevicesTool;
import com.tibbo.aggregate.mcp.tools.device.GetDeviceTool;
import com.tibbo.aggregate.mcp.tools.event.CreateEventTool;
import com.tibbo.aggregate.mcp.tools.event.FireEventTool;
import com.tibbo.aggregate.mcp.tools.event.ListEventsTool;
import com.tibbo.aggregate.mcp.tools.function.CallFunctionTool;
import com.tibbo.aggregate.mcp.tools.function.CreateFunctionTool;
import com.tibbo.aggregate.mcp.tools.function.ListFunctionsTool;
import com.tibbo.aggregate.mcp.tools.function.GetFunctionTool;
import com.tibbo.aggregate.mcp.tools.function.TestFunctionTool;
import com.tibbo.aggregate.mcp.tools.function.BuildExpressionTool;
import com.tibbo.aggregate.mcp.tools.function.ValidateExpressionTool;
import com.tibbo.aggregate.mcp.tools.function.FixFunctionParametersTool;
import com.tibbo.aggregate.mcp.tools.user.CreateUserTool;
import com.tibbo.aggregate.mcp.tools.user.DeleteUserTool;
import com.tibbo.aggregate.mcp.tools.user.ListUsersTool;
import com.tibbo.aggregate.mcp.tools.user.UpdateUserTool;
import com.tibbo.aggregate.mcp.tools.user.GetUserTool;
import com.tibbo.aggregate.mcp.tools.user.UpsertUserTool;
import com.tibbo.aggregate.mcp.tools.variable.CreateVariableTool;
import com.tibbo.aggregate.mcp.tools.variable.GetVariableTool;
import com.tibbo.aggregate.mcp.tools.variable.ListVariablesTool;
import com.tibbo.aggregate.mcp.tools.variable.SetVariableFieldTool;
import com.tibbo.aggregate.mcp.tools.variable.SetVariableTool;
import com.tibbo.aggregate.mcp.tools.variable.DescribeVariableTool;
import com.tibbo.aggregate.mcp.tools.variable.SetVariableSmartTool;
import com.tibbo.aggregate.mcp.tools.variable.BulkSetVariablesTool;
import com.tibbo.aggregate.mcp.tools.variable.GetOrCreateVariableTool;
import com.tibbo.aggregate.mcp.tools.action.ExecuteActionTool;
import com.tibbo.aggregate.mcp.tools.action.ListActionsTool;
import com.tibbo.aggregate.mcp.tools.action.GetActionInfoTool;
import com.tibbo.aggregate.mcp.tools.history.GetVariableHistoryTool;
import com.tibbo.aggregate.mcp.tools.history.GetEventHistoryTool;
import com.tibbo.aggregate.mcp.tools.rule.CreateRuleTool;
import com.tibbo.aggregate.mcp.tools.rule.ListRulesTool;
import com.tibbo.aggregate.mcp.tools.rule.EnableRuleTool;
import com.tibbo.aggregate.mcp.tools.rule.DisableRuleTool;
import com.tibbo.aggregate.mcp.tools.context.ExportContextTool;
import com.tibbo.aggregate.mcp.tools.context.ImportContextTool;
import com.tibbo.aggregate.mcp.tools.alarm.CreateAlarmTool;
import com.tibbo.aggregate.mcp.tools.alarm.ListAlarmsTool;
import com.tibbo.aggregate.mcp.tools.alarm.EnableAlarmTool;
import com.tibbo.aggregate.mcp.tools.alarm.DisableAlarmTool;
import com.tibbo.aggregate.mcp.tools.alarm.AcknowledgeAlarmTool;
import com.tibbo.aggregate.mcp.tools.template.CreateTemplateTool;
import com.tibbo.aggregate.mcp.tools.template.InstantiateTemplateTool;
import com.tibbo.aggregate.mcp.tools.template.ListTemplatesTool;
import com.tibbo.aggregate.mcp.tools.permission.SetVariablePermissionsTool;
import com.tibbo.aggregate.mcp.tools.permission.SetEventPermissionsTool;
import com.tibbo.aggregate.mcp.tools.permission.SetContextPermissionsTool;
import com.tibbo.aggregate.mcp.tools.variable.UpdateVariableTool;
import com.tibbo.aggregate.mcp.tools.variable.DeleteVariableTool;
import com.tibbo.aggregate.mcp.tools.event.UpdateEventTool;
import com.tibbo.aggregate.mcp.tools.event.DeleteEventTool;
import com.tibbo.aggregate.mcp.tools.function.DeleteFunctionTool;
import com.tibbo.aggregate.mcp.tools.driver.ListDriversTool;
import com.tibbo.aggregate.mcp.tools.driver.GetDriverInfoTool;
import com.tibbo.aggregate.mcp.tools.dashboard.GetDashboardTool;
import com.tibbo.aggregate.mcp.tools.dashboard.UpdateDashboardElementTool;
import com.tibbo.aggregate.mcp.tools.dashboard.DeleteDashboardElementTool;
import com.tibbo.aggregate.mcp.tools.dashboard.SetDefaultDashboardTool;
import com.tibbo.aggregate.mcp.tools.widget.CreateWidgetTool;
import com.tibbo.aggregate.mcp.tools.widget.SetWidgetTemplateTool;
import com.tibbo.aggregate.mcp.tools.widget.GetWidgetTemplateTool;
import com.tibbo.aggregate.mcp.tools.widget.ListWidgetsTool;
import com.tibbo.aggregate.mcp.tools.dashboard.CreateDashboardTool;
import com.tibbo.aggregate.mcp.tools.dashboard.AddDashboardElementTool;
import com.tibbo.aggregate.mcp.tools.dashboard.ListDashboardsTool;
import com.tibbo.aggregate.mcp.tools.context.GetOrCreateContextTool;
import com.tibbo.aggregate.mcp.tools.model.EnsureModelStructureTool;
import com.tibbo.aggregate.mcp.tools.server.GetServerInfoTool;
import com.tibbo.aggregate.mcp.tools.server.ExplainErrorTool;

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
        register(new GetOrCreateContextTool());
        register(new ListContextTreeTool());
        register(new ExportContextTool());
        register(new ImportContextTool());
        
        // Template tools
        register(new CreateTemplateTool());
        register(new InstantiateTemplateTool());
        register(new ListTemplatesTool());
        
        // Variable tools
        register(new GetVariableTool());
        register(new SetVariableTool());
        register(new SetVariableFieldTool());
        register(new ListVariablesTool());
        register(new CreateVariableTool());
        register(new DescribeVariableTool());
        register(new SetVariableSmartTool());
        register(new BulkSetVariablesTool());
        register(new GetOrCreateVariableTool());
        register(new UpdateVariableTool());
        register(new DeleteVariableTool());
        
        // Function tools
        register(new CallFunctionTool());
        register(new ListFunctionsTool());
        register(new CreateFunctionTool());
        register(new GetFunctionTool());
        register(new TestFunctionTool());
        register(new BuildExpressionTool());
        register(new ValidateExpressionTool());
        register(new FixFunctionParametersTool());
        register(new DeleteFunctionTool());
        
        // Device tools
        register(new CreateDeviceTool());
        register(new ListDevicesTool());
        register(new DeleteDeviceTool());
        register(new GetDeviceStatusTool());
        register(new GetDeviceTool());
        
        // Driver tools
        register(new ListDriversTool());
        register(new GetDriverInfoTool());
        
        // User tools
        register(new CreateUserTool());
        register(new ListUsersTool());
        register(new DeleteUserTool());
        register(new UpdateUserTool());
        register(new GetUserTool());
        register(new UpsertUserTool());
        
        // Event tools
        register(new FireEventTool());
        register(new CreateEventTool());
        register(new ListEventsTool());
        register(new UpdateEventTool());
        register(new DeleteEventTool());
        
        // Action tools
        register(new ExecuteActionTool());
        register(new ListActionsTool());
        register(new GetActionInfoTool());
        
        // History tools
        register(new GetVariableHistoryTool());
        register(new GetEventHistoryTool());
        
        // Rule tools
        register(new CreateRuleTool());
        register(new ListRulesTool());
        register(new EnableRuleTool());
        register(new DisableRuleTool());
        
        // Alarm tools
        register(new CreateAlarmTool());
        register(new ListAlarmsTool());
        register(new EnableAlarmTool());
        register(new DisableAlarmTool());
        register(new AcknowledgeAlarmTool());
        
        // Agent tools
        register(new CreateAgentTool());
        register(new GetAgentStatusTool());
        register(new WaitAgentReadyTool());
        register(new SendAgentEventSimpleTool());
        
        // Widget tools
        register(new CreateWidgetTool());
        register(new SetWidgetTemplateTool());
        register(new GetWidgetTemplateTool());
        register(new ListWidgetsTool());
        
        // Note: SetWidgetTemplateTool is registered but may need to be called via aggregate_set_widget_template
        
        // Dashboard tools
        register(new CreateDashboardTool());
        register(new AddDashboardElementTool());
        register(new ListDashboardsTool());
        register(new GetDashboardTool());
        register(new UpdateDashboardElementTool());
        register(new DeleteDashboardElementTool());
        register(new SetDefaultDashboardTool());
        
        // Permission tools
        register(new SetVariablePermissionsTool());
        register(new SetEventPermissionsTool());
        register(new SetContextPermissionsTool());
        
        // Model tools (high-level)
        register(new EnsureModelStructureTool());
        
        // Server tools
        register(new GetServerInfoTool());
        register(new ExplainErrorTool());

        // MCP tools meta
        register(new McpTool() {
            @Override
            public String getName() {
                return "aggregate_list_tools";
            }

            @Override
            public String getDescription() {
                return "List all available MCP tools with their input schemas";
            }

            @Override
            public JsonNode getInputSchema() {
                ObjectNode schema = instance.objectNode();
                schema.put("type", "object");
                // No required parameters; kept for MCP protocol consistency
                schema.set("properties", instance.objectNode());
                return schema;
            }

            @Override
            public JsonNode execute(JsonNode params, ConnectionManager connectionManager) {
                // Simply delegate to registry method
                return listTools();
            }
        });
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

