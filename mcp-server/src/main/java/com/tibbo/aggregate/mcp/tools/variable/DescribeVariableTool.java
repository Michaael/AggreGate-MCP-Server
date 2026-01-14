package com.tibbo.aggregate.mcp.tools.variable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.VariableDefinition;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.datatable.FieldFormat;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.connection.ServerConnection;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;
import com.tibbo.aggregate.mcp.util.ContextPathParser;
import com.tibbo.aggregate.mcp.util.DataTableConverter;

import java.util.List;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for getting a normalized description of a variable
 */
public class DescribeVariableTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_describe_variable";
    }

    @Override
    public String getDescription() {
        return "Get a normalized description of a context variable (format, records, fields, recommended write tool)";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();

        ObjectNode path = instance.objectNode();
        path.put("type", "string");
        path.put("description", "Context path");
        properties.set("path", path);

        ObjectNode variableName = instance.objectNode();
        variableName.put("type", "string");
        variableName.put("description", "Variable name");
        properties.set("variableName", variableName);

        ObjectNode connectionKey = instance.objectNode();
        connectionKey.put("type", "string");
        connectionKey.put("description", "Optional connection key");
        properties.set("connectionKey", connectionKey);

        schema.set("required", instance.arrayNode().add("path").add("variableName"));
        schema.set("properties", properties);
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("path") || !params.has("variableName")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Path and variableName parameters are required"
            );
        }

        String connectionKey = params.has("connectionKey") ? params.get("connectionKey").asText() : null;
        ServerConnection connection = connectionKey != null
            ? connectionManager.getServerConnection(connectionKey)
            : connectionManager.getDefaultServerConnection();

        if (connection == null || !connection.isLoggedIn()) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONNECTION_ERROR,
                "Not connected or not logged in"
            );
        }

        try {
            String path = ContextPathParser.parsePath(params.get("path").asText());
            String variableName = params.get("variableName").asText();

            Context context = connection.executeWithTimeout(() -> {
                return connection.getContextManager().get(path);
            }, 60000);

            if (context == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Context not found: " + path
                );
            }

            // Try to find variable definition for metadata
            VariableDefinition targetDef = null;
            List<VariableDefinition> variables = connection.executeWithTimeout(() -> {
                return context.getVariableDefinitions();
            }, 60000);
            for (VariableDefinition vd : variables) {
                if (vd.getName().equals(variableName)) {
                    targetDef = vd;
                    break;
                }
            }

            if (targetDef == null) {
                throw new McpException(
                    com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                    "Variable not found: " + variableName
                );
            }

            // Try to get current value to inspect format
            DataTable value = connection.executeWithTimeout(() -> {
                try {
                    com.tibbo.aggregate.common.context.CallerController caller =
                        context.getContextManager().getCallerController();
                    return context.getVariableClone(variableName, caller);
                } catch (ContextException e) {
                    // Variable may exist but have no value yet; ignore
                    return null;
                }
            }, 60000);

            TableFormat format = null;
            if (value != null) {
                format = value.getFormat();
            }

            ObjectNode result = instance.objectNode();
            result.put("path", path);
            result.put("name", variableName);

            if (targetDef.getDescription() != null) {
                result.put("description", targetDef.getDescription());
            }
            if (targetDef.getGroup() != null) {
                result.put("group", targetDef.getGroup());
            }
            result.put("readable", targetDef.isReadable());
            result.put("writable", targetDef.isWritable());

            if (format != null) {
                // Basic format info
                result.set("format", DataTableConverter.formatToJson(format));
                result.put("minRecords", format.getMinRecords());
                result.put("maxRecords", format.getMaxRecords());
                result.put("fieldCount", format.getFieldCount());

                // Fields summary
                ArrayNode fieldsArray = instance.arrayNode();
                for (int i = 0; i < format.getFieldCount(); i++) {
                    FieldFormat ff = format.getField(i);
                    ObjectNode f = instance.objectNode();
                    f.put("name", ff.getName());
                    f.put("type", String.valueOf(ff.getType()));
                    if (ff.getDescription() != null) {
                        f.put("description", ff.getDescription());
                    }
                    fieldsArray.add(f);
                }
                result.set("fields", fieldsArray);

                boolean singleRecord = format.getMaxRecords() == 1 || format.isSingleRecord();
                boolean singleField = format.getFieldCount() == 1;

                result.put("isSingleRecord", singleRecord);
                result.put("isSingleField", singleField);
                result.put("isSimple", singleRecord && singleField);

                // Recommended write tool
                String recommendedWriteTool = singleRecord
                    ? "aggregate_set_variable_field"
                    : "aggregate_set_variable";
                result.put("recommendedWriteTool", recommendedWriteTool);

                // Also recommend smart wrapper
                result.put("recommendedSmartTool", "aggregate_set_variable_smart");
            } else {
                result.put("note", "Variable definition found but format could not be determined (no value yet?)");
            }

            return result;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = com.tibbo.aggregate.mcp.util.ErrorHandler.extractErrorMessage(e);
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.CONTEXT_ERROR,
                "Failed to describe variable: " + errorMessage
            );
        }
    }
}

