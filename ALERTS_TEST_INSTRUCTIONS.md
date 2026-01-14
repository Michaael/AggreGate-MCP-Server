# Инструкции по тестированию создания тревог

**Дата:** 2025-01-27  
**Статус:** ✅ Сервер скомпилирован

## Компиляция завершена

Сервер успешно скомпилирован:
```
BUILD SUCCESSFUL in 6s
2 actionable tasks: 2 executed
```

## Следующие шаги

### 1. Перезапуск MCP сервера

Для применения изменений необходимо перезапустить MCP сервер:
1. Остановите текущий MCP сервер (если запущен)
2. Запустите MCP сервер заново

### 2. Тестирование создания тревог

После перезапуска сервера выполните следующие команды через `call_mcp_tool` в Cursor:

#### Шаг 1: Подключение
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
print(result)
```

#### Шаг 2: Вход
```python
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_login",
    arguments={}
)
print(result)
```

#### Шаг 3: Создание тревоги 1
```python
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_context",
    arguments={
        "parentPath": "users.admin.alerts",
        "name": "alarmEvent1Event2",
        "description": "Тревога, срабатывающая на Event 1 и деактивирующаяся при Event 2 (Int > 20)"
    }
)
print(result)
```

**Ожидаемый результат:** `{"success": true, "message": "Context created successfully", ...}`

#### Шаг 4: Создание тревоги 2
```python
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_context",
    arguments={
        "parentPath": "users.admin.alerts",
        "name": "alarmDelayedSum",
        "description": "Тревога через 10 секунд после того, как сумма Int+Float попадает в диапазон 50-100"
    }
)
print(result)
```

**Ожидаемый результат:** `{"success": true, "message": "Context created successfully", ...}`

#### Шаг 5: Создание тревоги 3
```python
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_context",
    arguments={
        "parentPath": "users.admin.alerts",
        "name": "alarmTableSum",
        "description": "Тревога на сумму Int в Table > 100 с корректирующим действием"
    }
)
print(result)
```

**Ожидаемый результат:** `{"success": true, "message": "Context created successfully", ...}`

#### Шаг 6: Проверка создания
```python
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_list_contexts",
    arguments={
        "mask": "users.admin.alerts.*"
    }
)
print(result)
```

**Ожидаемый результат:** Список должен содержать все три созданные тревоги:
- `users.admin.alerts.alarmEvent1Event2`
- `users.admin.alerts.alarmDelayedSum`
- `users.admin.alerts.alarmTableSum`

## Проверка успешности

✅ **Успешно, если:**
- Все три тревоги созданы без ошибок
- В ответе `{"success": true, ...}`
- Нет ошибок о необходимости `containerType` или `objectType`

❌ **Ошибка, если:**
- Появляется ошибка: `"For relative models (modelType=0), containerType is required"`
- Это означает, что сервер не перезапущен или изменения не применены

## Что было исправлено

1. ✅ Код проверяет тип родительского контекста **ДО** валидации параметров
2. ✅ `containerType` и `objectType` требуются **только для контекстов моделей** (`users.admin.models`)
3. ✅ Для контекстов в `users.admin.alerts` эти параметры **не требуются**
4. ✅ Сервер успешно скомпилирован с исправлениями

## Файлы для справки

- `test_alerts_creation_direct.py` - скрипт с примерами команд
- `ALERTS_CONTEXT_FIX_REPORT.md` - подробный отчет об исправлениях
- `mcp-server/src/main/java/com/tibbo/aggregate/mcp/tools/context/CreateContextTool.java` - исправленный код
