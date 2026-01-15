# Настройка MCP сервера AggreGate в Cursor

## Статус настройки

✅ **MCP сервер добавлен в конфигурацию Cursor**

Файл конфигурации: `C:\Users\micha\.cursor\mcp.json`

```json
{
  "mcpServers": {
    "aggregate": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\mcp-server\\build\\libs\\aggregate-mcp-server-1.0.0.jar"
      ]
    }
  }
}
```

## Проверка работы

### 1. Перезапуск Cursor

После добавления конфигурации **необходимо перезапустить Cursor**, чтобы изменения вступили в силу.

### 2. Проверка доступности инструментов

После перезапуска Cursor должен автоматически подключиться к MCP серверу и сделать доступными все инструменты через `call_mcp_tool`.

### 3. Доступные инструменты

После настройки будут доступны следующие инструменты:

- `aggregate_connect` - Подключение к серверу AggreGate
- `aggregate_login` - Вход в систему
- `aggregate_disconnect` - Отключение от сервера
- `aggregate_list_contexts` - Список контекстов
- `aggregate_create_context` - Создание контекста
- `aggregate_delete_context` - Удаление контекста
- `aggregate_get_context` - Получение информации о контексте
- `aggregate_list_variables` - Список переменных
- `aggregate_create_variable` - Создание переменной
- `aggregate_set_variable` - Установка значения переменной
- `aggregate_set_variable_field` - Установка поля переменной
- `aggregate_get_variable` - Получение переменной
- `aggregate_list_functions` - Список функций
- `aggregate_create_function` - Создание функции
- `aggregate_call_function` - Вызов функции
- `aggregate_create_event` - Создание события
- `aggregate_fire_event` - Генерация события
- `aggregate_list_devices` - Список устройств
- `aggregate_create_device` - Создание устройства
- `aggregate_delete_device` - Удаление устройства
- `aggregate_get_device_status` - Статус устройства
- `aggregate_list_users` - Список пользователей
- `aggregate_create_user` - Создание пользователя
- `aggregate_update_user` - Обновление пользователя
- `aggregate_delete_user` - Удаление пользователя
- `aggregate_create_agent` - Создание агента
- `aggregate_agent_get_status` - Статус агента
- `aggregate_create_widget` - Создание виджета
- `aggregate_set_widget_template` - Установка шаблона виджета
- `aggregate_create_dashboard` - Создание дашборда
- `aggregate_add_dashboard_element` - Добавление элемента в дашборд
- `aggregate_execute_action` - Выполнение действия

### 4. Пример использования через call_mcp_tool

После перезапуска Cursor можно использовать инструменты напрямую:

```python
# Подключение к серверу
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

# Вход в систему
result = call_mcp_tool(
    server="aggregate",
    tool="aggregate_login",
    arguments={}
)

# Получение списка контекстов
result = call_mcp_tool(
    server="aggregate",
    tool="aggregate_list_contexts",
    arguments={
        "mask": "*"
    }
)
```

### 5. Проверка через командную строку

Для проверки работы сервера без Cursor можно использовать тестовый скрипт:

```bash
python test_mcp_connection.py
```

Этот скрипт:
1. Запускает MCP сервер
2. Подключается к AggreGate
3. Выполняет вход
4. Получает список всех контекстов
5. Отображает результаты

## Устранение проблем

### Сервер не запускается

1. Проверьте, что Java установлена и доступна в PATH:
   ```bash
   java -version
   ```

2. Проверьте, что JAR файл существует:
   ```bash
   Test-Path "C:\Users\micha\YandexDisk\aggregate_mcp\mcp-server\build\libs\aggregate-mcp-server-1.0.0.jar"
   ```

3. Проверьте логи Cursor на наличие ошибок запуска MCP сервера

### Инструменты не доступны

1. Убедитесь, что Cursor перезапущен после изменения `mcp.json`
2. Проверьте, что сервер запущен (должен быть виден в статусе Cursor)
3. Проверьте конфигурацию в `mcp.json` на наличие синтаксических ошибок

### Ошибки подключения к AggreGate

1. Убедитесь, что сервер AggreGate запущен на `localhost:6460`
2. Проверьте учетные данные (по умолчанию `admin/admin`)
3. Проверьте сетевые настройки и файрвол

## Дополнительная информация

- Документация по использованию: `docs/MCP_AI_MODEL_GUIDE.md`
- Быстрая справка: `docs/MCP_AI_QUICK_REFERENCE.md`
- Сценарии использования: `docs/MCP_USAGE_SCENARIOS.md`

---

**Дата настройки:** 2025-12-15  
**Версия MCP сервера:** 1.0.0

