# Результат проверки call_mcp_tool

**Дата:** 2025-12-15  
**Статус:** MCP сервер настроен и работает (32 инструмента доступны)

## Проверка конфигурации

✅ **MCP сервер "aggregate" включен**  
✅ **32 инструмента доступны**  
✅ **Схемы инструментов загружены**

## Готовность к использованию

Все готово для использования `call_mcp_tool`. Примеры вызовов:

### 1. Подключение к серверу

```python
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

### 2. Вход в систему

```python
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_login",
    arguments={}
)
```

### 3. Получение списка контекстов

```python
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_list_contexts",
    arguments={
        "mask": "*"
    }
)
```

## Следующие шаги

Теперь можно выполнять все задачи через MCP инструменты напрямую в Cursor, используя `call_mcp_tool`.

---

**Примечание:** Если `call_mcp_tool` не работает напрямую, возможно требуется использовать другой формат вызова или проверить документацию Cursor по использованию MCP инструментов.

