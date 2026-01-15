# Рекомендуемые дополнительные инструменты для MCP сервера AggreGate

## Обзор

Этот документ описывает дополнительные инструменты, которые рекомендуется добавить в MCP сервер для полноценной разработки решений на AggreGate через AI-ассистентов.

## Категории инструментов

### 1. Работа с историей данных (History)

#### 1.1. `aggregate_get_variable_history`
**Назначение:** Получение истории значений переменной за указанный период.

**Параметры:**
- `path` (string, required) - путь к контексту
- `variableName` (string, required) - имя переменной
- `startTime` (long, optional) - начальное время (timestamp в миллисекундах)
- `endTime` (long, optional) - конечное время (timestamp в миллисекундах)
- `maxRecords` (integer, optional) - максимальное количество записей (по умолчанию 1000)
- `aggregation` (string, optional) - тип агрегации: "avg", "min", "max", "sum", "count"

**Пример использования:**
```json
{
  "tool": "aggregate_get_variable_history",
  "parameters": {
    "path": "users.admin.models.sensor",
    "variableName": "temperature",
    "startTime": 1704067200000,
    "endTime": 1704153600000,
    "maxRecords": 100,
    "aggregation": "avg"
  }
}
```

**Приоритет:** Высокий - часто требуется для анализа данных и создания отчетов.

---

#### 1.2. `aggregate_get_event_history`
**Назначение:** Получение истории событий за указанный период.

**Параметры:**
- `path` (string, required) - путь к контексту
- `eventName` (string, optional) - имя события (если не указано, возвращаются все события)
- `startTime` (long, optional) - начальное время
- `endTime` (long, optional) - конечное время
- `maxRecords` (integer, optional) - максимальное количество записей
- `level` (integer, optional) - фильтр по уровню события (0=INFO, 1=WARNING, 2=ERROR, 3=FATAL, 4=NOTICE)

**Пример использования:**
```json
{
  "tool": "aggregate_get_event_history",
  "parameters": {
    "path": "users.admin.models.cluster",
    "eventName": "alarm",
    "startTime": 1704067200000,
    "endTime": 1704153600000,
    "level": 2
  }
}
```

**Приоритет:** Высокий - необходимо для анализа событий и диагностики проблем.

---

### 2. Работа с правилами (Rules)

#### 2.1. `aggregate_create_rule`
**Назначение:** Создание правила (rule) в контексте для автоматической обработки данных.

**Параметры:**
- `path` (string, required) - путь к контексту
- `ruleName` (string, required) - имя правила
- `trigger` (string, required) - триггер правила (выражение или ссылка на переменную/событие)
- `expression` (string, required) - выражение правила (JavaScript/Expression код)
- `enabled` (boolean, optional) - включено ли правило (по умолчанию true)
- `description` (string, optional) - описание правила
- `group` (string, optional) - группа правил

**Пример использования:**
```json
{
  "tool": "aggregate_create_rule",
  "parameters": {
    "path": "users.admin.models.cluster",
    "ruleName": "checkClusterStatus",
    "trigger": ".:device1Sine || .:device2Sine || .:device3Sine",
    "expression": "if (.:device1Sine > 0 && .:device2Sine > 0 && .:device3Sine > 0) { .:clusterStatus = \"ошибка\"; .:fireEvent(\"clusterAlarm@\", \"Кластер перешел в состояние ошибки\"); } else { .:clusterStatus = \"ОК\"; }",
    "enabled": true,
    "description": "Проверка состояния кластера"
  }
}
```

**Приоритет:** Высокий - правила критически важны для автоматизации в AggreGate.

---

#### 2.2. `aggregate_list_rules`
**Назначение:** Получение списка всех правил в контексте.

**Параметры:**
- `path` (string, required) - путь к контексту

**Пример использования:**
```json
{
  "tool": "aggregate_list_rules",
  "parameters": {
    "path": "users.admin.models.cluster"
  }
}
```

**Приоритет:** Средний - полезно для проверки и отладки.

---

#### 2.3. `aggregate_enable_rule` / `aggregate_disable_rule`
**Назначение:** Включение/выключение правила.

**Параметры:**
- `path` (string, required) - путь к контексту
- `ruleName` (string, required) - имя правила

**Пример использования:**
```json
{
  "tool": "aggregate_enable_rule",
  "parameters": {
    "path": "users.admin.models.cluster",
    "ruleName": "checkClusterStatus"
  }
}
```

**Приоритет:** Средний - полезно для управления правилами.

---

#### 2.4. `aggregate_delete_rule`
**Назначение:** Удаление правила.

**Параметры:**
- `path` (string, required) - путь к контексту
- `ruleName` (string, required) - имя правила

**Приоритет:** Средний.

---

### 3. Работа с шаблонами (Templates)

#### 3.1. `aggregate_create_template`
**Назначение:** Создание шаблона контекста для переиспользования.

**Параметры:**
- `path` (string, required) - путь к контексту, который будет использован как шаблон
- `templateName` (string, required) - имя шаблона
- `description` (string, optional) - описание шаблона

**Пример использования:**
```json
{
  "tool": "aggregate_create_template",
  "parameters": {
    "path": "users.admin.models.sensor",
    "templateName": "sensor_template",
    "description": "Шаблон датчика"
  }
}
```

**Приоритет:** Средний - полезно для стандартизации решений.

---

#### 3.2. `aggregate_instantiate_template`
**Назначение:** Создание экземпляра контекста из шаблона.

**Параметры:**
- `templateName` (string, required) - имя шаблона
- `parentPath` (string, required) - путь к родительскому контексту
- `instanceName` (string, required) - имя экземпляра
- `parameters` (object, optional) - параметры для подстановки в шаблон

**Пример использования:**
```json
{
  "tool": "aggregate_instantiate_template",
  "parameters": {
    "templateName": "sensor_template",
    "parentPath": "users.admin.devices",
    "instanceName": "sensor1",
    "parameters": {
      "sensorId": "SENSOR_001",
      "location": "Room 101"
    }
  }
}
```

**Приоритет:** Средний.

---

#### 3.3. `aggregate_list_templates`
**Назначение:** Получение списка доступных шаблонов.

**Параметры:**
- `parentPath` (string, optional) - путь к родительскому контексту (если не указан, возвращаются все шаблоны)

**Приоритет:** Низкий.

---

### 4. Экспорт и импорт (Export/Import)

#### 4.1. `aggregate_export_context`
**Назначение:** Экспорт контекста в файл (XML, JSON или другой формат).

**Параметры:**
- `path` (string, required) - путь к контексту для экспорта
- `filePath` (string, required) - путь к файлу для сохранения
- `format` (string, optional) - формат экспорта: "xml", "json" (по умолчанию "xml")
- `includeHistory` (boolean, optional) - включать ли историю данных (по умолчанию false)
- `includeBindings` (boolean, optional) - включать ли привязки (по умолчанию true)

**Пример использования:**
```json
{
  "tool": "aggregate_export_context",
  "parameters": {
    "path": "users.admin.models.cluster",
    "filePath": "C:\\temp\\cluster_export.xml",
    "format": "xml",
    "includeHistory": false,
    "includeBindings": true
  }
}
```

**Приоритет:** Высокий - критически важно для резервного копирования и миграции.

---

#### 4.2. `aggregate_import_context`
**Назначение:** Импорт контекста из файла.

**Параметры:**
- `filePath` (string, required) - путь к файлу для импорта
- `parentPath` (string, required) - путь к родительскому контексту
- `contextName` (string, optional) - имя контекста (если не указано, берется из файла)
- `overwrite` (boolean, optional) - перезаписывать ли существующий контекст (по умолчанию false)

**Пример использования:**
```json
{
  "tool": "aggregate_import_context",
  "parameters": {
    "filePath": "C:\\temp\\cluster_export.xml",
    "parentPath": "users.admin.models",
    "contextName": "cluster_imported",
    "overwrite": false
  }
}
```

**Приоритет:** Высокий.

---

### 5. Управление правами доступа (Permissions)

#### 5.1. `aggregate_set_variable_permissions`
**Назначение:** Установка прав доступа к переменной.

**Параметры:**
- `path` (string, required) - путь к контексту
- `variableName` (string, required) - имя переменной
- `readPermissions` (string, optional) - права на чтение (например, "observer", "manager", "admin")
- `writePermissions` (string, optional) - права на запись

**Пример использования:**
```json
{
  "tool": "aggregate_set_variable_permissions",
  "parameters": {
    "path": "users.admin.models.cluster",
    "variableName": "status",
    "readPermissions": "observer",
    "writePermissions": "manager"
  }
}
```

**Приоритет:** Средний - полезно для настройки безопасности.

---

#### 5.2. `aggregate_set_event_permissions`
**Назначение:** Установка прав доступа к событию.

**Параметры:**
- `path` (string, required) - путь к контексту
- `eventName` (string, required) - имя события
- `readPermissions` (string, optional) - права на чтение
- `firePermissions` (string, optional) - права на генерацию события

**Приоритет:** Средний.

---

#### 5.3. `aggregate_set_context_permissions`
**Назначение:** Установка прав доступа к контексту.

**Параметры:**
- `path` (string, required) - путь к контексту
- `readPermissions` (string, optional) - права на чтение
- `writePermissions` (string, optional) - права на запись
- `executePermissions` (string, optional) - права на выполнение действий

**Приоритет:** Средний.

---

### 6. Работа с алармами (Alarms)

#### 6.1. `aggregate_create_alarm`
**Назначение:** Создание аларма (тревоги) в контексте.

**Параметры:**
- `path` (string, required) - путь к контексту
- `alarmName` (string, required) - имя аларма
- `condition` (string, required) - условие срабатывания (выражение)
- `eventName` (string, optional) - имя события для генерации при срабатывании
- `severity` (integer, optional) - уровень серьезности (0=INFO, 1=WARNING, 2=ERROR, 3=FATAL)
- `enabled` (boolean, optional) - включен ли аларм (по умолчанию true)
- `description` (string, optional) - описание аларма

**Пример использования:**
```json
{
  "tool": "aggregate_create_alarm",
  "parameters": {
    "path": "users.admin.models.sensor",
    "alarmName": "temperature_high",
    "condition": ".:temperature > 30",
    "eventName": "temperature_alert",
    "severity": 2,
    "enabled": true,
    "description": "Температура превысила порог"
  }
}
```

**Приоритет:** Высокий - алармы критически важны для мониторинга.

---

#### 6.2. `aggregate_list_alarms`
**Назначение:** Получение списка алармов в контексте.

**Параметры:**
- `path` (string, required) - путь к контексту
- `activeOnly` (boolean, optional) - возвращать только активные алармы (по умолчанию false)

**Приоритет:** Средний.

---

#### 6.3. `aggregate_enable_alarm` / `aggregate_disable_alarm`
**Назначение:** Включение/выключение аларма.

**Параметры:**
- `path` (string, required) - путь к контексту
- `alarmName` (string, required) - имя аларма

**Приоритет:** Средний.

---

#### 6.4. `aggregate_acknowledge_alarm`
**Назначение:** Подтверждение (acknowledge) аларма.

**Параметры:**
- `path` (string, required) - путь к контексту
- `alarmName` (string, required) - имя аларма
- `message` (string, optional) - сообщение при подтверждении

**Приоритет:** Средний.

---

### 7. Работа с драйверами устройств (Device Drivers)

#### 7.1. `aggregate_list_drivers`
**Назначение:** Получение списка доступных драйверов устройств.

**Параметры:**
- `category` (string, optional) - категория драйверов (например, "virtual", "modbus", "opc")

**Пример использования:**
```json
{
  "tool": "aggregate_list_drivers",
  "parameters": {
    "category": "virtual"
  }
}
```

**Приоритет:** Средний - полезно для выбора правильного драйвера при создании устройств.

---

#### 7.2. `aggregate_get_driver_info`
**Назначение:** Получение информации о драйвере устройства.

**Параметры:**
- `driverId` (string, required) - идентификатор драйвера

**Пример использования:**
```json
{
  "tool": "aggregate_get_driver_info",
  "parameters": {
    "driverId": "com.tibbo.linkserver.plugin.device.virtual"
  }
}
```

**Приоритет:** Низкий.

---

### 8. Мониторинг и диагностика (Monitoring & Diagnostics)

#### 8.1. `aggregate_get_context_statistics`
**Назначение:** Получение статистики по контексту (количество переменных, событий, функций, правил и т.д.).

**Параметры:**
- `path` (string, required) - путь к контексту

**Пример использования:**
```json
{
  "tool": "aggregate_get_context_statistics",
  "parameters": {
    "path": "users.admin.models.cluster"
  }
}
```

**Приоритет:** Низкий - полезно для диагностики и анализа.

---

#### 8.2. `aggregate_get_server_statistics`
**Назначение:** Получение общей статистики сервера (количество контекстов, устройств, пользователей, нагрузка и т.д.).

**Параметры:**
- Нет параметров

**Приоритет:** Низкий.

---

#### 8.3. `aggregate_get_connection_status`
**Назначение:** Получение статуса подключения устройства или агента.

**Параметры:**
- `path` (string, required) - путь к устройству или агенту

**Пример использования:**
```json
{
  "tool": "aggregate_get_connection_status",
  "parameters": {
    "path": "users.admin.devices.device1"
  }
}
```

**Приоритет:** Средний - полезно для диагностики проблем с устройствами.

---

### 9. Работа с плагинами (Plugins)

#### 9.1. `aggregate_list_plugins`
**Назначение:** Получение списка установленных плагинов.

**Параметры:**
- Нет параметров

**Приоритет:** Низкий.

---

#### 9.2. `aggregate_get_plugin_info`
**Назначение:** Получение информации о плагине.

**Параметры:**
- `pluginId` (string, required) - идентификатор плагина

**Приоритет:** Низкий.

---

### 10. Управление дашбордами (Dashboard Management)

#### 10.1. `aggregate_get_dashboard`
**Назначение:** Получение информации о дашборде.

**Параметры:**
- `path` (string, required) - путь к дашборду

**Приоритет:** Средний.

---

#### 10.2. `aggregate_update_dashboard_element`
**Назначение:** Обновление элемента дашборда.

**Параметры:**
- `path` (string, required) - путь к дашборду
- `elementName` (string, required) - имя элемента
- `parameters` (object, required) - новые параметры элемента

**Приоритет:** Средний.

---

#### 10.3. `aggregate_delete_dashboard_element`
**Назначение:** Удаление элемента из дашборда.

**Параметры:**
- `path` (string, required) - путь к дашборду
- `elementName` (string, required) - имя элемента

**Приоритет:** Средний.

---

#### 10.4. `aggregate_set_default_dashboard`
**Назначение:** Установка дашборда по умолчанию для пользователя.

**Параметры:**
- `username` (string, required) - имя пользователя
- `dashboardPath` (string, required) - путь к дашборду

**Пример использования:**
```json
{
  "tool": "aggregate_set_default_dashboard",
  "parameters": {
    "username": "admin",
    "dashboardPath": "users.admin.dashboards.clusterDashboard"
  }
}
```

**Приоритет:** Средний - упоминается в документации как важная функция.

---

### 11. Работа с действиями (Actions) - расширение

#### 11.1. `aggregate_list_actions`
**Назначение:** Получение списка доступных действий в контексте.

**Параметры:**
- `path` (string, required) - путь к контексту

**Пример использования:**
```json
{
  "tool": "aggregate_list_actions",
  "parameters": {
    "path": "users.admin.models.cluster"
  }
}
```

**Приоритет:** Высокий - полезно для понимания доступных действий перед их выполнением.

---

#### 11.2. `aggregate_get_action_info`
**Назначение:** Получение информации о действии (параметры, описание).

**Параметры:**
- `path` (string, required) - путь к контексту
- `actionName` (string, required) - имя действия

**Приоритет:** Высокий - критически важно для правильного вызова действий.

---

### 12. Управление переменными - расширение

#### 12.1. `aggregate_update_variable`
**Назначение:** Обновление свойств переменной (описание, группа, права и т.д.).

**Параметры:**
- `path` (string, required) - путь к контексту
- `variableName` (string, required) - имя переменной
- `description` (string, optional) - новое описание
- `group` (string, optional) - новая группа
- `readPermissions` (string, optional) - новые права на чтение
- `writePermissions` (string, optional) - новые права на запись

**Приоритет:** Средний.

---

#### 12.2. `aggregate_update_event`
**Назначение:** Обновление свойств события.

**Параметры:**
- `path` (string, required) - путь к контексту
- `eventName` (string, required) - имя события
- `description` (string, optional) - новое описание
- `group` (string, optional) - новая группа
- `level` (integer, optional) - новый уровень
- `historyStorageTime` (integer, optional) - новое время хранения истории

**Приоритет:** Средний.

---

#### 12.3. `aggregate_delete_variable`
**Назначение:** Удаление переменной.

**Параметры:**
- `path` (string, required) - путь к контексту
- `variableName` (string, required) - имя переменной

**Приоритет:** Средний.

---

#### 12.4. `aggregate_delete_event`
**Назначение:** Удаление события.

**Параметры:**
- `path` (string, required) - путь к контексту
- `eventName` (string, required) - имя события

**Приоритет:** Средний.

---

#### 12.5. `aggregate_delete_function`
**Назначение:** Удаление функции.

**Параметры:**
- `path` (string, required) - путь к контексту
- `functionName` (string, required) - имя функции

**Приоритет:** Средний.

---

## Приоритеты внедрения

### Высокий приоритет (критически важные)
1. ✅ `aggregate_get_variable_history` - история данных
2. ✅ `aggregate_get_event_history` - история событий
3. ✅ `aggregate_create_rule` - создание правил
4. ✅ `aggregate_export_context` - экспорт контекстов
5. ✅ `aggregate_import_context` - импорт контекстов
6. ✅ `aggregate_create_alarm` - создание алармов
7. ✅ `aggregate_list_actions` - список действий
8. ✅ `aggregate_get_action_info` - информация о действиях

### Средний приоритет (полезные)
1. `aggregate_list_rules` - список правил
2. `aggregate_enable_rule` / `aggregate_disable_rule` - управление правилами
3. `aggregate_create_template` - создание шаблонов
4. `aggregate_instantiate_template` - создание из шаблона
5. `aggregate_set_variable_permissions` - управление правами
6. `aggregate_list_alarms` - список алармов
7. `aggregate_list_drivers` - список драйверов
8. `aggregate_get_connection_status` - статус подключения
9. `aggregate_set_default_dashboard` - дашборд по умолчанию
10. `aggregate_update_variable` / `aggregate_update_event` - обновление элементов
11. `aggregate_delete_variable` / `aggregate_delete_event` / `aggregate_delete_function` - удаление элементов

### Низкий приоритет (опциональные)
1. `aggregate_list_templates` - список шаблонов
2. `aggregate_get_driver_info` - информация о драйвере
3. `aggregate_get_context_statistics` - статистика контекста
4. `aggregate_get_server_statistics` - статистика сервера
5. `aggregate_list_plugins` - список плагинов
6. `aggregate_get_dashboard` - информация о дашборде

## Рекомендации по реализации

### Фаза 1 (Минимальный набор для полноценной разработки)
- История данных и событий
- Правила (создание и управление)
- Экспорт/Импорт
- Алармы (создание и управление)
- Информация о действиях

### Фаза 2 (Расширенный функционал)
- Шаблоны
- Управление правами
- Расширенное управление элементами (обновление, удаление)
- Драйверы устройств
- Дашборды (расширенное управление)

### Фаза 3 (Дополнительные возможности)
- Статистика и мониторинг
- Плагины
- Дополнительные диагностические инструменты

## Примечания

1. **История данных** - критически важна для анализа и отчетности. Без неё сложно создавать полноценные решения.

2. **Правила** - в AggreGate правила часто используются вместо или вместе с Expression функциями для автоматизации. Отсутствие инструментов для работы с правилами ограничивает возможности AI-ассистента.

3. **Экспорт/Импорт** - необходимы для резервного копирования, миграции и шаблонизации решений.

4. **Алармы** - важная часть систем мониторинга. Без инструментов для создания алармов сложно создавать полноценные решения.

5. **Информация о действиях** - `aggregate_execute_action` уже есть, но без информации о доступных действиях и их параметрах AI-ассистент не может правильно их использовать.

---

**Версия:** 1.0  
**Дата:** 2025-01-27  
**Автор:** AI Assistant
