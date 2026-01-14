# Прямые вызовы через call_mcp_tool

Попытка использования `call_mcp_tool` напрямую для выполнения задач.

## Подключение к серверу

```python
connect_result = call_mcp_tool(
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

## Вход в систему

```python
login_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_login",
    arguments={}
)
```

## Получение списка контекстов

```python
contexts_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_list_contexts",
    arguments={
        "mask": "*"
    }
)
```

## Создание пользователей

```python
# Создание user1
user1_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_user",
    arguments={
        "username": "user1",
        "password": "password1",
        "email": "user1@example.com"
    }
)

# Создание user2
user2_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_user",
    arguments={
        "username": "user2",
        "password": "password2",
        "email": "user2@example.com"
    }
)
```

