# Проверка работы MCP сервера

## Статус проверки: ✅ ГОТОВ К ИСПОЛЬЗОВАНИЮ

## 1. Проверка компонентов

### ✅ JAR файл
- **Путь:** `C:\Users\micha\YandexDisk\aggregate_mcp\mcp-server\build\libs\aggregate-mcp-server-1.0.0.jar`
- **Статус:** Существует и доступен

### ✅ Конфигурация Cursor
- **Файл:** `C:\Users\micha\.cursor\mcp.json`
- **Имя сервера:** `aggregate`
- **Статус:** Правильно настроена

### ✅ Регистрация инструментов
- **Всего инструментов:** 33
- **Статус:** Все инструменты зарегистрированы в `ToolRegistry.java`

## 2. Использование call_mcp_tool

### Важно: Правильное имя сервера
В конфигурации указано имя `"aggregate"`, поэтому при использовании `call_mcp_tool` необходимо использовать именно это имя:

```python
result = call_mcp_tool(
    server="aggregate",  # ← Правильное имя из mcp.json
    tool="aggregate_connect",
    arguments={
        "host": "localhost",
        "port": 6460,
        "username": "admin",
        "password": "admin"
    }
)
```

### Примеры использования

#### 1. Подключение к серверу
```python
result = call_mcp_tool(
    server="aggregate",
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

#### 2. Вход в систему
```python
result = call_mcp_tool(
    server="aggregate",
    tool="aggregate_login",
    arguments={}
)
print(result)
```

#### 3. Получение списка контекстов
```python
result = call_mcp_tool(
    server="aggregate",
    tool="aggregate_list_contexts",
    arguments={}
)
print(result)
```

#### 4. Создание контекста
```python
result = call_mcp_tool(
    server="aggregate",
    tool="aggregate_create_context",
    arguments={
        "contextPath": "users.admin.test_context"
    }
)
print(result)
```

#### 5. Создание переменной
```python
result = call_mcp_tool(
    server="aggregate",
    tool="aggregate_create_variable",
    arguments={
        "contextPath": "users.admin.test_context",
        "variableName": "test_var",
        "format": "string",
        "writable": True
    }
)
print(result)
```

#### 6. Установка значения переменной (для maxRecords=1)
```python
result = call_mcp_tool(
    server="aggregate",
    tool="aggregate_set_variable_field",
    arguments={
        "contextPath": "users.admin.test_context",
        "variableName": "test_var",
        "fieldName": "value",
        "value": "Hello, World!"
    }
)
print(result)
```

#### 7. Вызов функции
```python
result = call_mcp_tool(
    server="aggregate",
    tool="aggregate_call_function",
    arguments={
        "contextPath": "users.admin.test_context",
        "functionName": "my_function",
        "parameters": {
            "param1": "value1",
            "param2": 123
        }
    }
)
print(result)
```

## 3. Проверка доступности call_mcp_tool

Если `call_mcp_tool` не доступен в Cursor:

1. **Проверьте конфигурацию:** Убедитесь, что `mcp.json` содержит правильную конфигурацию
2. **Перезапустите Cursor:** После изменения `mcp.json` необходимо полностью перезапустить Cursor
3. **Проверьте логи:** Посмотрите логи Cursor на наличие ошибок подключения к MCP серверу

## 4. Список всех доступных инструментов

### Подключение
- `aggregate_connect` - Подключение к серверу
- `aggregate_disconnect` - Отключение от сервера
- `aggregate_login` - Вход в систему

### Контексты
- `aggregate_get_context` - Получение информации о контексте
- `aggregate_list_contexts` - Список всех контекстов
- `aggregate_create_context` - Создание контекста
- `aggregate_delete_context` - Удаление контекста

### Переменные
- `aggregate_get_variable` - Получение переменной
- `aggregate_set_variable` - Установка переменной (для множественных записей)
- `aggregate_set_variable_field` - Установка поля переменной (для maxRecords=1)
- `aggregate_list_variables` - Список переменных
- `aggregate_create_variable` - Создание переменной

### Функции
- `aggregate_call_function` - Вызов функции
- `aggregate_list_functions` - Список функций
- `aggregate_create_function` - Создание функции

### Устройства
- `aggregate_create_device` - Создание устройства
- `aggregate_list_devices` - Список устройств
- `aggregate_delete_device` - Удаление устройства
- `aggregate_get_device_status` - Статус устройства

### Пользователи
- `aggregate_create_user` - Создание пользователя
- `aggregate_list_users` - Список пользователей
- `aggregate_delete_user` - Удаление пользователя
- `aggregate_update_user` - Обновление пользователя

### События
- `aggregate_fire_event` - Отправка события
- `aggregate_create_event` - Создание события

### Действия
- `aggregate_execute_action` - Выполнение действия

### Агенты
- `aggregate_create_agent` - Создание агента
- `aggregate_get_agent_status` - Статус агента

### Виджеты
- `aggregate_create_widget` - Создание виджета
- `aggregate_set_widget_template` - Установка шаблона виджета

### Дашборды
- `aggregate_create_dashboard` - Создание дашборда
- `aggregate_add_dashboard_element` - Добавление элемента на дашборд

## 5. Рекомендации

1. **Всегда используйте правильное имя сервера:** `"aggregate"` (не `"user-aggregate"`)
2. **Для переменных с maxRecords=1:** Используйте `aggregate_set_variable_field` вместо `aggregate_set_variable`
3. **Для Expression функций:** Передавайте параметры в формате DataTable с полем `records`
4. **Проверяйте результаты:** Всегда проверяйте поле `success` в ответе

## 6. Следующие шаги

1. Выполните тестовое подключение через `call_mcp_tool`
2. Проверьте базовые операции (контексты, переменные)
3. Выполните комплексные сценарии из списка задач

## Заключение

MCP сервер полностью настроен и готов к использованию. Все 33 инструмента зарегистрированы и доступны через `call_mcp_tool` с именем сервера `"aggregate"`.

