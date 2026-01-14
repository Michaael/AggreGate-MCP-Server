# Тест call_mcp_tool

## Проверка доступности инструмента

Попробую вызвать `call_mcp_tool` напрямую для проверки подключения к серверу AggreGate.

### Шаг 1: Подключение к серверу

```python
# Вызов через call_mcp_tool
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_connect",
    arguments={
        "host": "localhost",
        "port": 6460,
        "username": "admin",
        "password": "admin"
    }
)
```

### Шаг 2: Вход в систему

```python
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_login",
    arguments={}
)
```

### Шаг 3: Получение списка контекстов

```python
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_list_contexts",
    arguments={
        "mask": "*"
    }
)
```

---

**Примечание:** Если `call_mcp_tool` не доступен, возможно требуется перезапуск Cursor или проверка конфигурации MCP сервера.

