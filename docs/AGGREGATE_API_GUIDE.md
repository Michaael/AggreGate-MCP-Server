# Руководство по работе с API AggreGate через MCP сервер

## Введение

Это руководство описывает правильный порядок работы с API AggreGate через MCP сервер. Оно поможет избежать типичных ошибок при создании моделей контекстов, переменных, событий и привязок.

## Важные принципы работы с AggreGate

### 1. Порядок создания элементов модели контекста

При создании модели контекста (model context) в AggreGate **критически важно** соблюдать правильный порядок операций:

1. **Создание модели контекста** (`aggregate_create_context`)
2. **Создание переменных модели** (`aggregate_create_variable`) - **ОБЯЗАТЕЛЬНО**
3. **Создание событий модели** (`aggregate_create_event`) - **ОБЯЗАТЕЛЬНО**
4. **Создание привязок** (bindings) - через `aggregate_execute_action`
5. Создание устройств, виджетов, дашбордов и других элементов

⚠️ **ВАЖНО**: Переменные и события модели **НЕ создаются автоматически** при создании модели контекста. Их нужно создавать **явно** с помощью соответствующих функций MCP сервера.

### 2. Разница между обычным контекстом и моделью контекста

- **Обычный контекст**: переменные и события создаются напрямую через `addVariableDefinition()` и `addEventDefinition()`
- **Модель контекста**: переменные и события хранятся в специальных переменных `V_MODEL_VARIABLES` и `V_MODEL_EVENTS`, которые определяют структуру всех экземпляров модели

## Пошаговое руководство

### Шаг 1: Подключение к серверу

```json
{
  "tool": "aggregate_connect",
  "parameters": {
    "host": "localhost",
    "port": 6460,
    "username": "admin",
    "password": "admin"
  }
}
```

После подключения выполните вход:

```json
{
  "tool": "aggregate_login"
}
```

### Шаг 2: Создание модели контекста

Создайте модель контекста в нужном месте (например, в `users.admin.models`):

```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "cluster",
    "description": "Модель кластера устройств"
  }
}
```

✅ **Результат**: Модель контекста `users.admin.models.cluster` создана.

### Шаг 3: Создание переменных модели ⚠️ ОБЯЗАТЕЛЬНО

**КРИТИЧЕСКИ ВАЖНО**: После создания модели контекста необходимо создать переменные модели. Без этого модель не будет работать корректно.

Пример создания переменной состояния кластера:

```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.cluster",
    "variableName": "status",
    "format": "<status><S>",
    "description": "Статус кластера",
    "group": "Основные",
    "writable": true,
    "readPermissions": "observer",
    "writePermissions": "manager",
    "storageMode": 0
  }
}
```

Пример создания переменной с числовым значением:

```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.cluster",
    "variableName": "deviceCount",
    "format": "<count><I>",
    "description": "Количество устройств в кластере",
    "group": "Основные",
    "writable": true,
    "storageMode": 0
  }
}
```

**Параметры переменной:**
- `path` - путь к модели контекста (обязательно)
- `variableName` - имя переменной (обязательно)
- `format` - формат переменной в виде строки TableFormat (обязательно)
  - `<name><S>` - строка
  - `<name><I>` - целое число
  - `<name><L>` - длинное целое
  - `<name><D>` - число с плавающей точкой
  - `<name><B>` - булево значение
- `description` - описание (опционально)
- `group` - группа переменных (опционально)
- `writable` - можно ли записывать (по умолчанию `false`)
- `readPermissions` - права на чтение (по умолчанию `"observer"`)
- `writePermissions` - права на запись (по умолчанию `"manager"`)
- `storageMode` - режим хранения: `0` = база данных, `1` = память (по умолчанию `0`)

### Шаг 4: Создание событий модели ⚠️ ОБЯЗАТЕЛЬНО

**КРИТИЧЕСКИ ВАЖНО**: После создания переменных необходимо создать события модели. События позволяют уведомлять о важных изменениях состояния.

Пример создания события тревоги:

```json
{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.cluster",
    "eventName": "alarm",
    "format": "<message><S><severity><I>",
    "description": "Событие тревоги кластера",
    "group": "События",
    "level": 2,
    "permissions": "observer",
    "firePermissions": "manager",
    "historyStorageTime": 86400000
  }
}
```

Пример создания информационного события:

```json
{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.cluster",
    "eventName": "statusChanged",
    "format": "<oldStatus><S><newStatus><S>",
    "description": "Изменение статуса кластера",
    "group": "События",
    "level": 0,
    "historyStorageTime": 0
  }
}
```

**Параметры события:**
- `path` - путь к модели контекста (обязательно)
- `eventName` - имя события (обязательно)
- `format` - формат события в виде строки TableFormat (опционально, по умолчанию пустой)
- `description` - описание (опционально)
- `group` - группа событий (опционально)
- `level` - уровень события:
  - `0` = INFO (информация)
  - `1` = WARNING (предупреждение)
  - `2` = ERROR (ошибка)
  - `3` = FATAL (критическая ошибка)
  - `4` = NOTICE (уведомление)
- `permissions` - права на чтение (по умолчанию `"observer"`)
- `firePermissions` - права на генерацию события (по умолчанию `"admin"`)
- `historyStorageTime` - время хранения истории в миллисекундах (по умолчанию `0`)

### Шаг 5: Создание привязок (Bindings) ⚠️ ОБЯЗАТЕЛЬНО

**КРИТИЧЕСКИ ВАЖНО**: Привязки (bindings) связывают переменные и события модели с переменными и событиями устройств или других контекстов. Без привязок модель не будет получать данные от устройств.

Привязки создаются через переменную `bindings` модели контекста. Используйте правильный формат ссылок AggreGate:

**Формат ссылок:**
- `.:имя_переменной` - ссылка на переменную текущего контекста (используется в `target`)
- `{контекст:переменная}` - ссылка на переменную другого контекста (используется в `expression`)

**Пример привязки переменной через `aggregate_set_variable`:**

```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.cluster",
    "name": "bindings",
    "value": {
      "recordCount": 1,
      "format": {
        "minRecords": 0,
        "maxRecords": 2147483647,
        "fields": [
          {"name": "target", "type": "S"},
          {"name": "expression", "type": "S"},
          {"name": "onevent", "type": "B"}
        ]
      },
      "records": [{
        "target": ".:device1Sine",
        "expression": "{users.admin.devices.device1:sine}",
        "onevent": true
      }]
    }
  }
}
```

**Пример привязки события:**

```json
{
  "records": [{
    "target": ".:alarm",
    "expression": "{users.admin.devices.device1:alarm}",
    "onevent": true
  }]
}
```

**Важно**: 
- В поле `target` всегда указывайте `.:имя_переменной` (с префиксом `.:`)
- В поле `expression` используйте формат `{контекст:переменная}` для ссылки на переменную другого контекста
- Подробнее о формате ссылок см. [Формат ссылок в AggreGate](AGGREGATE_REFERENCES_FORMAT.md)

### Шаг 6: Создание устройств

После настройки модели можно создавать устройства:

```json
{
  "tool": "aggregate_create_device",
  "parameters": {
    "username": "admin",
    "deviceName": "device1",
    "description": "Первое устройство кластера",
    "driverId": "com.tibbo.linkserver.plugin.device.virtual"
  }
}
```

### Шаг 7: Создание виджетов и дашбордов

После создания устройств можно создавать виджеты и дашборды для визуализации данных.

## Типичные ошибки и их решения

### ❌ Ошибка: "Переменные модели не созданы"

**Проблема**: Агент создал модель контекста, но забыл создать переменные модели.

**Решение**: Всегда после создания модели контекста вызывайте `aggregate_create_variable` для каждой необходимой переменной.

### ❌ Ошибка: "События модели не созданы"

**Проблема**: Агент создал модель контекста и переменные, но забыл создать события модели.

**Решение**: После создания переменных обязательно создайте события модели с помощью `aggregate_create_event`.

### ❌ Ошибка: "Привязки не созданы"

**Проблема**: Агент создал модель, переменные и события, но забыл создать привязки между устройствами и моделью.

**Решение**: Используйте `aggregate_execute_action` с действием `addBinding` для создания привязок. Если это не работает, создайте привязки вручную через веб-интерфейс AggreGate.

### ❌ Ошибка: "Неверный формат переменной"

**Проблема**: Формат переменной указан неправильно.

**Решение**: Используйте правильный синтаксис TableFormat:
- Для простых переменных: `<name><T>`, где `T` - тип (`S`=строка, `I`=целое, `L`=длинное, `D`=дробное, `B`=булево)
- Для сложных структур используйте полный синтаксис TableFormat

## Чек-лист создания модели контекста

При создании модели контекста убедитесь, что выполнены все следующие шаги:

- [ ] ✅ Подключение к серверу (`aggregate_connect`)
- [ ] ✅ Вход в систему (`aggregate_login`)
- [ ] ✅ Создание модели контекста (`aggregate_create_context`)
- [ ] ✅ **Создание переменных модели** (`aggregate_create_variable`) - **ОБЯЗАТЕЛЬНО**
- [ ] ✅ **Создание событий модели** (`aggregate_create_event`) - **ОБЯЗАТЕЛЬНО**
- [ ] ✅ **Создание привязок** (`aggregate_execute_action` с `addBinding`) - **ОБЯЗАТЕЛЬНО**
- [ ] ✅ Создание устройств (`aggregate_create_device`)
- [ ] ✅ Создание виджетов (`aggregate_create_widget`)
- [ ] ✅ Создание дашбордов (`aggregate_create_dashboard`)

## Пример полного сценария

Вот пример полного сценария создания модели кластера с тремя устройствами:

```json
// 1. Подключение
{"tool": "aggregate_connect", "parameters": {"host": "localhost", "port": 6460, "username": "admin", "password": "admin"}}
{"tool": "aggregate_login"}

// 2. Создание модели
{"tool": "aggregate_create_context", "parameters": {"parentPath": "users.admin.models", "name": "cluster", "description": "Модель кластера"}}

// 3. Создание переменных модели (ОБЯЗАТЕЛЬНО!)
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.cluster", "variableName": "status", "format": "<status><S>", "description": "Статус кластера"}}
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.cluster", "variableName": "deviceCount", "format": "<count><I>", "description": "Количество устройств"}}

// 4. Создание событий модели (ОБЯЗАТЕЛЬНО!)
{"tool": "aggregate_create_event", "parameters": {"path": "users.admin.models.cluster", "eventName": "alarm", "format": "<message><S>", "description": "Тревога", "level": 2}}

// 5. Создание привязок (ОБЯЗАТЕЛЬНО!)
// (выполняется после создания устройств)

// 6. Создание устройств
{"tool": "aggregate_create_device", "parameters": {"username": "admin", "deviceName": "device1", "description": "Устройство 1", "driverId": "com.tibbo.linkserver.plugin.device.virtual"}}
{"tool": "aggregate_create_device", "parameters": {"username": "admin", "deviceName": "device2", "description": "Устройство 2", "driverId": "com.tibbo.linkserver.plugin.device.virtual"}}
{"tool": "aggregate_create_device", "parameters": {"username": "admin", "deviceName": "device3", "description": "Устройство 3", "driverId": "com.tibbo.linkserver.plugin.device.virtual"}}

// 7. Создание привязок после устройств
// Используйте правильный формат: target: ".:имя_переменной", expression: "{контекст:переменная}"
{"tool": "aggregate_set_variable", "parameters": {"path": "users.admin.models.cluster", "name": "bindings", "value": {"records": [{"target": ".:device1Sine", "expression": "{users.admin.devices.device1:sine}", "onevent": true}]}}}

// 8. Создание виджетов и дашбордов
{"tool": "aggregate_create_widget", "parameters": {...}}
{"tool": "aggregate_create_dashboard", "parameters": {...}}
```

## Дополнительные ресурсы

- [Формат ссылок на контексты, переменные, события и функции](AGGREGATE_REFERENCES_FORMAT.md) - **ВАЖНО**: Правильный синтаксис ссылок в AggreGate
- [Полное руководство по MCP серверу](mcp-server/COMPLETE_GUIDE.md)
- [Лучшие практики AggreGate](manual/AGGREGATE_BEST_PRACTICES.md)
- [Описание контекстов AggreGate](manual/AGGREGATE_CONTEXTS_DESCRIPTION.md)

## Заключение

Помните: **переменные модели, события модели и привязки НЕ создаются автоматически**. Их нужно создавать явно после создания модели контекста. Соблюдение правильного порядка операций гарантирует корректную работу модели контекста в AggreGate.

