package com.tibbo.aggregate.mcp.tools.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tibbo.aggregate.mcp.connection.ConnectionManager;
import com.tibbo.aggregate.mcp.protocol.McpException;
import com.tibbo.aggregate.mcp.tools.McpTool;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * Tool for explaining an error message and providing hints for AI.
 * This is a pure utility tool, it does not call the server.
 */
public class ExplainErrorTool implements McpTool {
    @Override
    public String getName() {
        return "aggregate_explain_error";
    }

    @Override
    public String getDescription() {
        return "Analyze an error message and return a normalized explanation with possible fixes.";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = instance.objectNode();

        ObjectNode message = instance.objectNode();
        message.put("type", "string");
        message.put("description", "Raw error message text");
        properties.set("message", message);

        ObjectNode toolName = instance.objectNode();
        toolName.put("type", "string");
        toolName.put("description", "Name of the tool that produced the error (optional)");
        properties.set("toolName", toolName);

        schema.set("required", instance.arrayNode().add("message"));
        schema.set("properties", properties);
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode params, ConnectionManager connectionManager) throws McpException {
        if (!params.has("message")) {
            throw new McpException(
                com.tibbo.aggregate.mcp.protocol.McpError.INVALID_PARAMS,
                "Message parameter is required"
            );
        }

        String message = params.get("message").asText();
        String toolName = params.has("toolName") ? params.get("toolName").asText() : null;

        ObjectNode result = instance.objectNode();
        result.put("rawMessage", message);
        if (toolName != null) {
            result.put("toolName", toolName);
        }

        // Simple pattern-based explanations for common errors
        String lower = message.toLowerCase();

        if (lower.contains("maximum number of records is reached") || lower.contains("невозможно добавить запись")) {
            result.put("category", "variable_max_records");
            result.put("explanation",
                "Попытка добавить слишком много записей в переменную с ограничением maxRecords=1 " +
                "или использовать aggregate_set_variable для однозаписной переменной.");
            result.put("recommendation",
                "Используйте aggregate_set_variable_field или aggregate_set_variable_smart. " +
                "Сначала вызовите aggregate_describe_variable, чтобы узнать maxRecords и формат.");
        } else if (lower.contains("invalid inputformat") || lower.contains("invalid outputformat") ||
                   lower.contains("invalid format") || lower.contains("format error") ||
                   message.contains("<<") || message.contains(">>")) {
            result.put("category", "function_format");
            result.put("explanation",
                "Неверный формат входных/выходных данных функции. Для Expression-функций " +
                "inputFormat/outputFormat задаются БЕЗ <<>>, а <<>> используются только в expression внутри table().");
            
            // Check if it's specifically about <<>> brackets
            if (message.contains("<<") || message.contains(">>")) {
                result.put("detailedExplanation",
                    "Обнаружены <<>> в inputFormat или outputFormat - это ОШИБКА! " +
                    "ПРАВИЛЬНО: inputFormat='<value1><E><value2><E>', outputFormat='<result><E>', " +
                    "expression='table(\"<<result><E>>\", {value1} + {value2})'. " +
                    "НЕПРАВИЛЬНО: inputFormat='<<value1><E>>' или outputFormat='<<result><E>>'.");
                result.put("recommendation",
                    "1. Используйте aggregate_build_expression для построения правильных форматов. " +
                    "2. Или используйте aggregate_validate_expression для проверки перед созданием. " +
                    "3. Убедитесь: inputFormat/outputFormat БЕЗ <<>>, expression С <<>> внутри table().");
            } else {
                result.put("recommendation",
                    "1. Используйте aggregate_build_expression для построения правильных форматов. " +
                    "2. Проверьте синтаксис через aggregate_validate_expression. " +
                    "3. Для диагностики вызовите aggregate_get_function и aggregate_test_function.");
            }
        } else if (lower.contains("context not found")) {
            result.put("category", "context_not_found");
            result.put("explanation", "Указанный контекст не существует или путь указан неверно.");
            result.put("recommendation",
                "Проверьте путь или создайте контекст с помощью aggregate_get_or_create_context " +
                "или aggregate_create_context.");
        } else if (lower.contains("not connected") || lower.contains("not logged in")) {
            result.put("category", "connection");
            result.put("explanation", "Нет активного соединения с сервером или не выполнен вход.");
            result.put("recommendation",
                "Сначала вызовите aggregate_connect, затем aggregate_login, затем повторите вызов инструмента.");
        } else {
            result.put("category", "unknown");
            result.put("explanation",
                "Неизвестная или редко встречающаяся ошибка. Используйте текст сообщения для дальнейшего анализа.");
        }

        return result;
    }
}

