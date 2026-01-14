# Как использовать call_mcp_tool в Cursor

## Способ 1: Прямой вызов в чате Cursor

В чате Cursor вы можете напрямую использовать `call_mcp_tool` для вызова MCP инструментов:

```python
# Подключение к серверу AggreGate
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

# Вывод результата
print(result)
```

## Способ 2: Использование через Composer (AI ассистент)

Просто напишите в чате Cursor:

```
Подключись к серверу AggreGate через call_mcp_tool
```

Или более конкретно:

```
Используй call_mcp_tool для подключения к серверу AggreGate:
- server: "user-aggregate"
- tool: "aggregate_connect"
- arguments: host="localhost", port=6460, username="admin", password="admin"
```

## Способ 3: Выполнение последовательности команд

Вы можете попросить AI выполнить последовательность команд:

```
Выполни следующие команды через call_mcp_tool:
1. Подключись к серверу (aggregate_connect)
2. Выполни вход (aggregate_login)
3. Получи список всех контекстов (aggregate_list_contexts с mask="*")
```

## Формат вызова

Стандартный формат вызова `call_mcp_tool`:

```python
result = call_mcp_tool(
    server="user-aggregate",      # Имя MCP сервера
    tool="aggregate_connect",     # Имя инструмента
    arguments={                   # Аргументы инструмента
        "host": "localhost",
        "port": 6460,
        "username": "admin",
        "password": "admin"
    }
)
```

## Примеры использования

### Пример 1: Подключение и вход

```python
# 1. Подключение
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

# 2. Вход
login_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_login",
    arguments={}
)
```

### Пример 2: Создание пользователя

```python
user_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_user",
    arguments={
        "username": "user1",
        "password": "password1",
        "email": "user1@example.com"
    }
)
```

### Пример 3: Получение списка контекстов

```python
contexts_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_list_contexts",
    arguments={
        "mask": "*"
    }
)
```

## Проверка доступности

Если `call_mcp_tool` не работает, проверьте:

1. **MCP сервер включен** - в настройках Cursor должен быть виден сервер "aggregate" со статусом "enabled"
2. **Сервер запущен** - должен быть зеленый индикатор статуса
3. **Инструменты загружены** - должно быть видно "32 tools enabled"

## Устранение проблем

### Проблема: "call_mcp_tool is not defined"

**Решение:** Убедитесь, что:
- Cursor перезапущен после добавления конфигурации MCP
- MCP сервер "aggregate" включен в настройках
- Инструменты загружены (должно быть видно "32 tools enabled")

### Проблема: "Tool not found"

**Решение:** Проверьте:
- Правильность имени инструмента (например, `aggregate_connect`, а не `connect`)
- Правильность имени сервера (`user-aggregate` или `aggregate`)

### Проблема: "Connection error"

**Решение:** Проверьте:
- Сервер AggreGate запущен на localhost:6460
- Правильность учетных данных (admin/admin по умолчанию)
- Сетевые настройки и файрвол

---

**Примечание:** `call_mcp_tool` - это внутренний инструмент Cursor для вызова MCP инструментов. Он должен быть доступен автоматически, когда MCP сервер настроен и включен.


