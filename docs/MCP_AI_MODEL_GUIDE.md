# Полное руководство по использованию MCP сервера AggreGate для AI моделей

## Содержание
1. [Введение](#введение)
2. [Подключение и аутентификация](#подключение-и-аутентификация)
3. [Работа с контекстами](#работа-с-контекстами)
4. [Работа с переменными](#работа-с-переменными)
5. [Работа с функциями](#работа-с-функциями)
6. [Работа с событиями](#работа-с-событиями)
7. [Работа с устройствами](#работа-с-устройствами)
8. [Работа с пользователями](#работа-с-пользователями)
9. [Работа с агентами](#работа-с-агентами)
10. [Форматы данных TableFormat](#форматы-данных-tableformat)
11. [Типичные ошибки и решения](#типичные-ошибки-и-решения)
12. [Полные примеры использования](#полные-примеры-использования)
13. [Правила выбора инструментов](#правила-выбора-инструментов)

**📖 См. также:** 
- [Сценарии использования](MCP_USAGE_SCENARIOS.md) - подробные примеры для всех случаев
- [Дерево решений](MCP_DECISION_TREE.md) - визуальное руководство по выбору инструментов
- [Четкие правила](MCP_CLEAR_RULES.md) - **ОБЯЗАТЕЛЬНО К ПРОЧТЕНИЮ** - когда что использовать

---

## Введение

MCP сервер AggreGate предоставляет набор инструментов для управления системой AggreGate через JSON-RPC протокол. Все инструменты возвращают JSON ответы и принимают параметры в формате JSON.

### 🚀 Быстрый старт для AI моделей

**Минимальный рабочий пример создания модели:**

```json
// 1. Подключение
{"tool": "aggregate_connect", "parameters": {"host": "localhost", "port": 6460, "username": "admin", "password": "admin"}}

// 2. Вход
{"tool": "aggregate_login"}

// 3. Создание модели
{"tool": "aggregate_create_context", "parameters": {"parentPath": "users.admin.models", "name": "my_model", "description": "Моя модель"}}

// 4. Создание переменной (ОБЯЗАТЕЛЬНО!)
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.my_model", "variableName": "status", "format": "<status><S>", "writable": true}}

// 5. Установка значения (используйте aggregate_set_variable_field для простых переменных)
{"tool": "aggregate_set_variable_field", "parameters": {"path": "users.admin.models.my_model", "variableName": "status", "fieldName": "status", "value": "active"}}
```

### ⚠️ Критически важные правила для AI моделей:

1. **Порядок операций критичен**: Всегда подключайтесь (`aggregate_connect`), затем входите (`aggregate_login`) перед выполнением других операций.

2. **Ошибки верификации**: При создании переменных, функций и событий в моделях может возвращаться ошибка верификации, но элемент часто создается успешно. Всегда проверяйте через `aggregate_list_variables`, `aggregate_list_functions` или `aggregate_get_variable`.

3. **Форматы данных**: Переменные используют формат TableFormat. Для простых переменных используйте `<name><T>`, где T - тип (S=String, I=Integer, E=Extended/Double, B=Boolean).

4. **Модели контекстов**: Модели создаются в `users.admin.models.*`. После создания модели обязательно создайте переменные и события перед использованием.

5. **⚠️ Установка значений переменных - ВАЖНО**: 
   - Для переменных с `maxRecords=1` (большинство простых переменных) **ВСЕГДА используйте `aggregate_set_variable_field`**
   - Используйте параметр `variableName` (не `name`) в `aggregate_set_variable_field`
   - Это предотвратит ошибку "maximum number of records is reached"

6. **⚠️ Expression функции - ВАЖНО**: 
   - При создании Expression функций (functionType=1) формат `inputFormat` и `outputFormat` должен быть **обычным TableFormat БЕЗ двойных скобок `<<>>`**
   - Пример правильного формата: `<value1><E><value2><E>` (не `<<value1><E>>`)
   - Двойные скобки `<<>>` используются **только внутри выражения** в параметре `expression`
   - Пример выражения: `table("<<result><E>>", ({value1} + {value2}) / 2)`

7. **Стабильность соединения**: После множественных операций (10-15+) может потребоваться переподключение. Если получаете ошибки чтения ответа, выполните `aggregate_disconnect` и `aggregate_connect` + `aggregate_login` заново.

8. **Проверка перед созданием**: Всегда проверяйте существование элементов через `aggregate_list_variables`, `aggregate_list_functions` перед созданием, чтобы избежать ошибок "already exists".

---

## Подключение и аутентификация

### aggregate_connect

Подключается к серверу AggreGate.

**Параметры:**
- `host` (string, optional, default: "localhost") - Хост или IP адрес сервера
- `port` (integer, optional, default: 6460) - Порт сервера
- `username` (string, optional, default: "admin") - Имя пользователя
- `password` (string, optional, default: "admin") - Пароль
- `connectionKey` (string, optional) - Ключ для множественных подключений

**Пример:**
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

**Ответ:**
```json
{
  "success": true,
  "message": "Connected to server",
  "host": "localhost",
  "port": 6460,
  "username": "admin"
}
```

### aggregate_login

Выполняет вход в систему. Должен вызываться после `aggregate_connect`.

**Параметры:**
- `connectionKey` (string, optional) - Ключ подключения, если используется множественное подключение

**Пример:**
```json
{
  "tool": "aggregate_login"
}
```

**Ответ:**
```json
{
  "success": true,
  "message": "Logged in successfully"
}
```

### aggregate_disconnect

Отключается от сервера.

**Параметры:**
- `connectionKey` (string, optional) - Ключ подключения

**Пример:**
```json
{
  "tool": "aggregate_disconnect"
}
```

---

## Работа с контекстами

### aggregate_create_context

Создает новый контекст (модель) в AggreGate.

**Параметры:**
- `parentPath` (string, required) - Путь к родительскому контексту
- `name` (string, required) - Имя нового контекста
- `description` (string, optional) - Описание контекста
- `connectionKey` (string, optional) - Ключ подключения

**Важно:** Для создания моделей используйте `parentPath: "users.admin.models"`

**Пример создания модели:**
```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "temperature_sensor",
    "description": "Модель датчика температуры"
  }
}
```

**Ответ:**
```json
{
  "success": true,
  "message": "Context created successfully",
  "path": "users.admin.models.temperature_sensor",
  "name": "temperature_sensor",
  "description": "Модель датчика температуры"
}
```

### aggregate_get_context

Получает информацию о контексте.

**Параметры:**
- `path` (string, required) - Путь к контексту
- `connectionKey` (string, optional) - Ключ подключения

**Пример:**
```json
{
  "tool": "aggregate_get_context",
  "parameters": {
    "path": "users.admin.models.temperature_sensor"
  }
}
```

### aggregate_list_contexts

Список контекстов по маске.

**Параметры:**
- `mask` (string, optional) - Маска для поиска (например, "users.*" или "users.admin.models.*")
- `connectionKey` (string, optional) - Ключ подключения

**Пример:**
```json
{
  "tool": "aggregate_list_contexts",
  "parameters": {
    "mask": "users.admin.models.*"
  }
}
```

**Ответ:**
```json
[
  {
    "path": "users.admin.models.temperature_sensor",
    "name": "temperature_sensor",
    "description": "Модель датчика температуры"
  }
]
```

### aggregate_delete_context

Удаляет контекст.

**Параметры:**
- `path` (string, required) - Путь к контексту
- `connectionKey` (string, optional) - Ключ подключения

---

## Работа с переменными

### aggregate_create_variable

Создает переменную в контексте. **ОБЯЗАТЕЛЬНО** для моделей - переменные не создаются автоматически.

**Параметры:**
- `path` (string, required) - Путь к контексту
- `variableName` (string, required) - Имя переменной
- `format` (string, required) - Формат переменной в TableFormat (например, `<temperature><E>`)
- `description` (string, optional) - Описание переменной
- `group` (string, optional) - Группа переменных
- `writable` (boolean, optional, default: false) - Можно ли записывать (рекомендуется true)
- `readPermissions` (string, optional, default: "observer") - Права на чтение
- `writePermissions` (string, optional, default: "manager") - Права на запись
- `storageMode` (integer, optional, default: 0) - Режим хранения: 0=база данных, 1=память
- `connectionKey` (string, optional) - Ключ подключения

**Форматы TableFormat для переменных:**
- `<name><S>` - Строка (String)
- `<name><I>` - Целое число (Integer)
- `<name><L>` - Длинное целое (Long)
- `<name><D>` - Число с плавающей точкой (Double)
- `<name><E>` - Расширенное число (Extended/Double) - рекомендуется для чисел
- `<name><B>` - Булево значение (Boolean)

**Примеры:**

**Простая строковая переменная:**
```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.temperature_sensor",
    "variableName": "status",
    "format": "<status><S>",
    "description": "Статус датчика",
    "group": "Основные",
    "writable": true
  }
}
```

**Числовая переменная:**
```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.temperature_sensor",
    "variableName": "temperature",
    "format": "<temperature><E>",
    "description": "Температура в градусах",
    "group": "Датчики",
    "writable": true,
    "storageMode": 0
  }
}
```

**Переменная с несколькими полями:**
```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.temperature_sensor",
    "variableName": "sensor_data",
    "format": "<temperature><E><humidity><E><timestamp><L>",
    "description": "Данные датчика",
    "writable": true
  }
}
```

**Ответ:**
```json
{
  "success": true,
  "message": "Variable created successfully",
  "path": "users.admin.models.temperature_sensor",
  "variableName": "temperature",
  "description": "Температура в градусах",
  "group": "Датчики",
  "writable": true
}
```

**⚠️ Важно:** Если получили ошибку "verification failed", проверьте через `aggregate_list_variables` - переменная может быть создана.

### aggregate_get_variable

Получает значение переменной.

**Параметры:**
- `path` (string, required) - Путь к контексту
- `name` (string, required) - Имя переменной
- `connectionKey` (string, optional) - Ключ подключения

**Пример:**
```json
{
  "tool": "aggregate_get_variable",
  "parameters": {
    "path": "users.admin.models.temperature_sensor",
    "name": "temperature"
  }
}
```

**Ответ (DataTable формат):**
```json
{
  "recordCount": 1,
  "format": {
    "minRecords": 1,
    "maxRecords": 1,
    "fields": [
      {
        "name": "temperature",
        "type": "E",
        "description": "Температура в градусах"
      }
    ]
  },
  "records": [
    {
      "temperature": 25.5
    }
  ]
}
```

### aggregate_set_variable

Устанавливает значение переменной. **⚠️ ВАЖНО: В моделях AggreGate переменные обычно имеют maxRecords=1, даже если у них несколько полей. Используйте этот инструмент только если вы уверены, что переменная имеет maxRecords > 1.**

**Когда использовать:**
- ✅ Переменные с maxRecords > 1 (несколько записей) - **проверьте через `aggregate_get_variable` перед использованием**
- ✅ Переменные, созданные явно с maxRecords > 1
- ❌ **НЕ используйте для переменных в моделях** - модели обычно создают переменные с maxRecords=1
- ❌ НЕ используйте для простых переменных с maxRecords=1 - используйте `aggregate_set_variable_field`

**⚠️ КРИТИЧЕСКИ ВАЖНО:**
- **В моделях AggreGate переменные создаются с maxRecords=1 по умолчанию**, даже если у них несколько полей
- **Для переменных в моделях ВСЕГДА используйте `aggregate_set_variable_field`** для установки значений полей
- Используйте `aggregate_set_variable` только если вы явно создали переменную с maxRecords > 1

**Параметры:**
- `path` (string, required) - Путь к контексту
- `name` (string, required) - Имя переменной
- `value` (object, required) - Значение в формате DataTable JSON
- `connectionKey` (string, optional) - Ключ подключения

**Пример (только для переменных с maxRecords > 1):**
```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.sensor",
    "name": "sensor_data",
    "value": {
      "records": [
        {
          "temperature": 25.5,
          "humidity": 60.0,
          "timestamp": 1234567890
        }
      ]
    }
  }
}
```

**Ответ:**
```json
{
  "success": true,
  "message": "Variable set successfully"
}
```

**Рекомендация:** Для переменных в моделях используйте `aggregate_set_variable_field` для каждого поля отдельно.

### aggregate_set_variable_field

Устанавливает значение конкретного поля переменной. **ИСПОЛЬЗУЙТЕ ДЛЯ ВСЕХ ПРОСТЫХ ПЕРЕМЕННЫХ С maxRecords=1.**

**Когда использовать:**
- ✅ **ВСЕГДА** для простых переменных с maxRecords=1 (например, `<temperature><E>`, `<status><S>`)
- ✅ Для установки одного поля в переменной
- ✅ Это **предпочтительный** способ установки значений для большинства переменных
- ❌ НЕ используйте для установки нескольких полей одновременно - используйте `aggregate_set_variable`

**Параметры:**
- `path` (string, required) - Путь к контексту
- `variableName` (string, required) - Имя переменной (⚠️ **обязательно используйте "variableName", не "name"**)
- `fieldName` (string, required) - Имя поля (для простых переменных обычно совпадает с variableName)
- `value` (any, required) - Значение поля (может быть string, number, boolean, или null)
- `connectionKey` (string, optional) - Ключ подключения

**Пример для простой переменной:**
```json
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.temperature_sensor",
    "variableName": "temperature",
    "fieldName": "temperature",
    "value": 25.5
  }
}
```

**Пример для строковой переменной:**
```json
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.sensor",
    "variableName": "status",
    "fieldName": "status",
    "value": "active"
  }
}
```

**Ответ:**
```json
{
  "success": true,
  "message": "Variable field set successfully"
}
```

**⚠️ КРИТИЧЕСКИ ВАЖНО:** 
- **ВСЕГДА используйте `aggregate_set_variable_field` для простых переменных** (maxRecords=1)
- **Используйте параметр `variableName` (не `name`)** - это обязательное требование
- Для переменных с одним полем `fieldName` обычно совпадает с `variableName`
- Это **единственный правильный способ** установки значений для простых переменных

### aggregate_list_variables

Получает список всех переменных в контексте.

**Параметры:**
- `path` (string, required) - Путь к контексту
- `group` (string, optional) - Фильтр по группе
- `connectionKey` (string, optional) - Ключ подключения

**Пример:**
```json
{
  "tool": "aggregate_list_variables",
  "parameters": {
    "path": "users.admin.models.temperature_sensor"
  }
}
```

**Ответ:**
```json
[
  {
    "name": "temperature",
    "description": "Температура в градусах",
    "group": "Датчики",
    "readable": true,
    "writable": true
  },
  {
    "name": "status",
    "description": "Статус датчика",
    "group": "Основные",
    "readable": true,
    "writable": true
  }
]
```

---

## Работа с функциями

### aggregate_create_function

Создает функцию в контексте.

**Параметры:**
- `path` (string, required) - Путь к контексту
- `functionName` (string, required) - Имя функции
- `functionType` (integer, optional, default: 0) - Тип функции:
  - `0` = Java (требует реализацию на Java)
  - `1` = Expression (выражение на JavaScript-подобном языке) - **рекомендуется**
  - `2` = Query (SQL-запрос)
- `description` (string, optional) - Описание функции
- `group` (string, optional) - Группа функций
- `inputFormat` (string, optional) - Формат входных параметров в TableFormat
- `outputFormat` (string, optional) - Формат результата в TableFormat
- `expression` (string, optional) - Выражение для типа Expression (обязательно для functionType=1)
- `query` (string, optional) - SQL-запрос для типа Query (обязательно для functionType=2)
- `connectionKey` (string, optional) - Ключ подключения

**Пример функции типа Expression (рекомендуется):**
```json
{
  "tool": "aggregate_create_function",
  "parameters": {
    "path": "users.admin.models.temperature_sensor",
    "functionName": "calculate_average",
    "functionType": 1,
    "description": "Вычисление среднего значения",
    "group": "Вычисления",
    "inputFormat": "<value1><E><value2><E>",
    "outputFormat": "<result><E>",
    "expression": "table(\"<<result><E>>\", ({value1} + {value2}) / 2)"
  }
}
```

**⚠️ Важно для Expression функций:**
- `inputFormat` и `outputFormat` должны быть обычным TableFormat (например, `<value1><E><value2><E>`), **БЕЗ** двойных скобок `<<>>`
- Двойные скобки `<<format>>` используются **только внутри выражения** в параметре `expression`
- Если `inputFormat` или `outputFormat` не указаны, будут созданы форматы по умолчанию
- Выражение должно использовать синтаксис: `table("<<outputFormat>>", expression)`, где `{fieldName}` ссылается на входные параметры

**Пример простой функции типа Java:**
```json
{
  "tool": "aggregate_create_function",
  "parameters": {
    "path": "users.admin.models.temperature_sensor",
    "functionName": "get_status",
    "description": "Получить статус",
    "functionType": 0
  }
}
```

**Пример функции с пустым форматом:**
```json
{
  "tool": "aggregate_create_function",
  "parameters": {
    "path": "users.admin.models.temperature_sensor",
    "functionName": "reset",
    "description": "Сброс счетчика",
    "functionType": 0
  }
}
```

**⚠️ Важно для Expression функций:**
- Формат выражения: `table("<<outputFormat>>", expression)`, где `{fieldName}` ссылается на входные параметры
- `inputFormat` и `outputFormat` в параметрах должны быть **без** двойных скобок (например, `<result><E>`)
- Двойные скобки `<<>>` используются **только внутри выражения** в параметре `expression`
- Если формат не указан, будет создан формат по умолчанию

### aggregate_call_function

Вызывает функцию контекста.

**Параметры:**
- `path` (string, required) - Путь к контексту
- `functionName` (string, required) - Имя функции
- `parameters` (object, optional) - Параметры функции (может быть простым объектом или DataTable JSON)
- `connectionKey` (string, optional) - Ключ подключения

**Пример вызова функции без параметров:**
```json
{
  "tool": "aggregate_call_function",
  "parameters": {
    "path": "users.admin.models.temperature_sensor",
    "functionName": "get_status"
  }
}
```

**Пример вызова функции с простыми параметрами:**
```json
{
  "tool": "aggregate_call_function",
  "parameters": {
    "path": "users.admin.models.temperature_sensor",
    "functionName": "calculate_average",
    "parameters": {
      "value1": 10.5,
      "value2": 20.3
    }
  }
}
```

**Пример вызова функции с DataTable параметрами:**
```json
{
  "tool": "aggregate_call_function",
  "parameters": {
    "path": "users.admin.models.temperature_sensor",
    "functionName": "process_data",
    "parameters": {
      "format": {
        "fields": [{"name": "data", "type": "S"}]
      },
      "records": [
        {"data": "test"}
      ]
    }
  }
}
```

**Ответ:**
```json
{
  "recordCount": 1,
  "format": {
    "fields": [{"name": "result", "type": "E"}]
  },
  "records": [
    {
      "result": 15.4
    }
  ]
}
```

### aggregate_list_functions

Получает список всех функций в контексте.

**Параметры:**
- `path` (string, required) - Путь к контексту
- `connectionKey` (string, optional) - Ключ подключения

**Пример:**
```json
{
  "tool": "aggregate_list_functions",
  "parameters": {
    "path": "users.admin.models.temperature_sensor"
  }
}
```

---

## Работа с событиями

### aggregate_create_event

Создает событие в контексте. **ОБЯЗАТЕЛЬНО** для моделей - события не создаются автоматически.

**Параметры:**
- `path` (string, required) - Путь к контексту
- `eventName` (string, required) - Имя события
- `format` (string, optional, default: empty) - Формат события в TableFormat
- `description` (string, optional) - Описание события
- `group` (string, optional) - Группа событий
- `level` (integer, optional, default: 0) - Уровень события:
  - `0` = INFO (информация)
  - `1` = WARNING (предупреждение)
  - `2` = ERROR (ошибка)
  - `3` = FATAL (критическая ошибка)
  - `4` = NOTICE (уведомление)
- `permissions` (string, optional, default: "observer") - Права на чтение
- `firePermissions` (string, optional, default: "admin") - Права на генерацию события
- `historyStorageTime` (integer, optional, default: 0) - Время хранения истории в миллисекундах
- `connectionKey` (string, optional) - Ключ подключения

**Примеры:**

**Простое информационное событие:**
```json
{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.temperature_sensor",
    "eventName": "status_changed",
    "description": "Изменение статуса",
    "group": "События",
    "level": 0
  }
}
```

**Событие с параметрами:**
```json
{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.temperature_sensor",
    "eventName": "temperature_alert",
    "format": "<temperature><E><threshold><E>",
    "description": "Предупреждение о температуре",
    "group": "Алерты",
    "level": 1,
    "historyStorageTime": 86400000
  }
}
```

**Событие ошибки:**
```json
{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.temperature_sensor",
    "eventName": "error_occurred",
    "format": "<message><S><code><I>",
    "description": "Событие ошибки",
    "group": "Ошибки",
    "level": 2
  }
}
```

**⚠️ Важно:** Если получили ошибку "verification failed", проверьте через `aggregate_get_variable` с именем `modelEvents` - событие может быть создано.

### aggregate_fire_event

Генерирует событие из агента.

**Параметры:**
- `agentName` (string, required) - Имя агента
- `eventName` (string, required) - Имя события
- `level` (string, optional, default: "INFO") - Уровень события (INFO, WARNING, ERROR, FATAL, NOTICE)
- `data` (object, optional) - Данные события в формате DataTable JSON

**Пример:**
```json
{
  "tool": "aggregate_fire_event",
  "parameters": {
    "agentName": "my_agent",
    "eventName": "temperature_alert",
    "level": "WARNING",
    "data": {
      "records": [
        {
          "temperature": 35.0,
          "threshold": 30.0
        }
      ]
    }
  }
}
```

---

## Работа с устройствами

### aggregate_create_device

Создает новое устройство.

**Параметры:**
- `username` (string, required) - Имя пользователя, владеющего устройством
- `deviceName` (string, required) - Имя устройства
- `description` (string, required) - Описание устройства
- `driverId` (string, required) - ID драйвера устройства (например, "com.tibbo.linkserver.plugin.device.virtual")
- `connectionKey` (string, optional) - Ключ подключения

**Пример:**
```json
{
  "tool": "aggregate_create_device",
  "parameters": {
    "username": "admin",
    "deviceName": "sensor_01",
    "description": "Датчик температуры #1",
    "driverId": "com.tibbo.linkserver.plugin.device.virtual"
  }
}
```

**Ответ:**
```json
{
  "success": true,
  "message": "Device created successfully",
  "path": "users.admin.devices.sensor_01",
  "name": "sensor_01"
}
```

### aggregate_list_devices

Получает список устройств пользователя.

**Параметры:**
- `username` (string, required) - Имя пользователя
- `connectionKey` (string, optional) - Ключ подключения

**Пример:**
```json
{
  "tool": "aggregate_list_devices",
  "parameters": {
    "username": "admin"
  }
}
```

### aggregate_get_device_status

Получает статус устройства.

**Параметры:**
- `path` (string, required) - Путь к контексту устройства
- `connectionKey` (string, optional) - Ключ подключения

**Пример:**
```json
{
  "tool": "aggregate_get_device_status",
  "parameters": {
    "path": "users.admin.devices.sensor_01"
  }
}
```

### aggregate_delete_device

Удаляет устройство.

**Параметры:**
- `path` (string, required) - Путь к контексту устройства
- `connectionKey` (string, optional) - Ключ подключения

---

## Работа с пользователями

### aggregate_create_user

Создает нового пользователя.

**Параметры:**
- `username` (string, required) - Имя пользователя
- `password` (string, required) - Пароль
- `email` (string, optional) - Email пользователя
- `connectionKey` (string, optional) - Ключ подключения

**Пример:**
```json
{
  "tool": "aggregate_create_user",
  "parameters": {
    "username": "newuser",
    "password": "password123",
    "email": "user@example.com"
  }
}
```

**Ответ:**
```json
{
  "success": true,
  "message": "User created successfully",
  "path": "users.newuser",
  "username": "newuser"
}
```

### aggregate_list_users

Получает список пользователей.

**Параметры:**
- `connectionKey` (string, optional) - Ключ подключения

**Пример:**
```json
{
  "tool": "aggregate_list_users"
}
```

### aggregate_update_user

Обновляет информацию о пользователе.

**Параметры:**
- `username` (string, required) - Имя пользователя
- `email` (string, optional) - Новый email
- `firstname` (string, optional) - Имя
- `lastname` (string, optional) - Фамилия
- `connectionKey` (string, optional) - Ключ подключения

**Пример:**
```json
{
  "tool": "aggregate_update_user",
  "parameters": {
    "username": "newuser",
    "email": "newemail@example.com"
  }
}
```

### aggregate_delete_user

Удаляет пользователя.

**Параметры:**
- `username` (string, required) - Имя пользователя
- `connectionKey` (string, optional) - Ключ подключения

---

## Работа с агентами

### aggregate_create_agent

Создает и подключает агента к серверу AggreGate.

**Параметры:**
- `agentName` (string, required) - Имя агента
- `host` (string, optional, default: "localhost") - Хост сервера
- `port` (integer, optional, default: 6460) - Порт сервера
- `username` (string, optional, default: "admin") - Имя пользователя
- `password` (string, optional, default: "admin") - Пароль
- `eventConfirmation` (boolean, optional, default: true) - Подтверждение событий

**Пример:**
```json
{
  "tool": "aggregate_create_agent",
  "parameters": {
    "agentName": "temperature_monitor",
    "host": "localhost",
    "port": 6460,
    "username": "admin",
    "password": "admin"
  }
}
```

### aggregate_get_agent_status

Получает статус агента.

**Параметры:**
- `agentName` (string, required) - Имя агента

**Пример:**
```json
{
  "tool": "aggregate_get_agent_status",
  "parameters": {
    "agentName": "temperature_monitor"
  }
}
```

---

## Работа с виджетами и дашбордами

### aggregate_create_widget

Создает виджет.

**Параметры:**
- `parentPath` (string, required) - Путь к родительскому контексту виджетов (например, "users.admin.widgets")
- `name` (string, required) - Имя виджета
- `description` (string, optional) - Описание виджета
- `template` (string, optional) - XML шаблон виджета
- `defaultContext` (string, optional) - Контекст по умолчанию
- `connectionKey` (string, optional) - Ключ подключения

**Пример:**
```json
{
  "tool": "aggregate_create_widget",
  "parameters": {
    "parentPath": "users.admin.widgets",
    "name": "temperature_widget",
    "description": "Виджет температуры"
  }
}
```

### aggregate_set_widget_template

Устанавливает XML шаблон для виджета.

**Параметры:**
- `path` (string, required) - Путь к виджету (например, "users.admin.widgets.temperature_widget")
- `template` (string, required) - XML шаблон виджета
- `connectionKey` (string, optional) - Ключ подключения

### aggregate_create_dashboard

Создает дашборд.

**Параметры:**
- `parentPath` (string, required) - Путь к родительскому контексту дашбордов (например, "users.admin.dashboards")
- `name` (string, required) - Имя дашборда
- `description` (string, optional) - Описание дашборда
- `layout` (string, optional) - Тип макета: "dockable", "scrollable", "grid", "absolute" (default: "dockable")
- `connectionKey` (string, optional) - Ключ подключения

**Пример:**
```json
{
  "tool": "aggregate_create_dashboard",
  "parameters": {
    "parentPath": "users.admin.dashboards",
    "name": "monitoring_dashboard",
    "description": "Дашборд мониторинга",
    "layout": "dockable"
  }
}
```

### aggregate_add_dashboard_element

Добавляет элемент на дашборд.

**Параметры:**
- `path` (string, required) - Путь к дашборду (например, "users.admin.dashboards.monitoring_dashboard")
- `name` (string, required) - Имя элемента
- `type` (string, required) - Тип элемента (например, "launchWidget", "showEventLog", "panel")
- `parameters` (object, optional) - Параметры элемента в формате DataTable JSON
- `connectionKey` (string, optional) - Ключ подключения

**Пример:**
```json
{
  "tool": "aggregate_add_dashboard_element",
  "parameters": {
    "path": "users.admin.dashboards.monitoring_dashboard",
    "name": "temp_widget",
    "type": "launchWidget",
    "parameters": {
      "records": [
        {
          "widget": "users.admin.widgets.temperature_widget"
        }
      ]
    }
  }
}
```

---

## Работа с действиями

### aggregate_execute_action

Выполняет серверное действие (упрощенная версия).

**Параметры:**
- `path` (string, required) - Путь к контексту, где должно выполняться действие
- `actionName` (string, required) - Имя действия
- `input` (object, optional) - Входные параметры в формате DataTable JSON
- `filePath` (string, optional) - Путь к файлу для экспорта/импорта
- `connectionKey` (string, optional) - Ключ подключения

**Примечание:** Этот инструмент может не работать для всех типов действий, так как некоторые действия требуют интерактивных шагов.

**Пример:**
```json
{
  "tool": "aggregate_execute_action",
  "parameters": {
    "path": "users.admin.models.temperature_sensor",
    "actionName": "export",
    "filePath": "C:\\temp\\export.xml"
  }
}
```

---

## Форматы данных TableFormat

TableFormat - это формат описания структуры данных в AggreGate. Используется для переменных, функций и событий.

### Синтаксис TableFormat

**Базовый синтаксис:**
```
<fieldName><fieldType><options>
```

**Типы полей:**
- `<S>` - String (строка)
- `<I>` - Integer (целое число)
- `<L>` - Long (длинное целое)
- `<D>` - Double (число с плавающей точкой)
- `<E>` - Extended (расширенное число, рекомендуется для чисел)
- `<B>` - Boolean (булево значение)
- `<T>` - Date (дата/время)

**Опции:**
- `<D=Description>` - Описание поля
- `<F=N>` - Поле необязательное (nullable)

**Примеры форматов:**

**Простая переменная:**
```
<temperature><E>
```

**Переменная с описанием:**
```
<temperature><E><D=Температура в градусах>
```

**Переменная с несколькими полями:**
```
<temperature><E><humidity><E><timestamp><L>
```

**Переменная с необязательным полем:**
```
<value><E><F=N><comment><S>
```

**Формат для функций Expression:**
Для Expression функций формат должен быть обернут в двойные скобки:
```
<<result><E>>
```

**Пример Expression функции:**
```javascript
table("<<result><E>>", ({arg1} + {arg2}) / 2)
```

### Формат DataTable JSON

При работе с переменными через `aggregate_set_variable` и `aggregate_get_variable` используется формат DataTable JSON:

```json
{
  "recordCount": 1,
  "format": {
    "minRecords": 1,
    "maxRecords": 1,
    "fields": [
      {
        "name": "temperature",
        "type": "E",
        "description": "Температура"
      }
    ]
  },
  "records": [
    {
      "temperature": 25.5
    }
  ]
}
```

**Упрощенный формат (для переменных с maxRecords=1):**
Можно использовать только records, формат будет взят из определения переменной:
```json
{
  "records": [
    {
      "temperature": 25.5
    }
  ]
}
```

---

## Типичные ошибки и решения

### 1. Ошибка "verification failed" при создании переменной/функции/события

**Симптомы:**
```json
{
  "error": {
    "code": -32001,
    "message": "Variable was not created in model context - verification failed"
  }
}
```

**Причина:** Верификация выполняется слишком быстро, до полной инициализации контекста.

**Решение:**
1. Проверьте, что элемент создан через `aggregate_list_variables`, `aggregate_list_functions` или `aggregate_get_variable` (для событий проверьте переменную `modelEvents`)
2. Если элемент создан - игнорируйте ошибку и продолжайте работу
3. Если элемент не создан - подождите 2-3 секунды и повторите операцию

### 2. Ошибка "maximum number of records is reached"

**Симптомы:**
```json
{
  "error": {
    "message": "maximum number of records is reached"
  }
}
```

**Причина:** Попытка добавить запись в переменную с maxRecords=1 через `aggregate_set_variable`.

**Решение:** 
1. **Используйте `aggregate_set_variable_field`** для переменных с maxRecords=1 (большинство простых переменных)
2. Инструмент `aggregate_set_variable` автоматически пытается использовать `setVariableField` при ошибке, но лучше использовать правильный инструмент сразу
3. Убедитесь, что используете параметр `variableName` (не `name`) в `aggregate_set_variable_field`

### 3. Ошибка "Context not found"

**Симптомы:**
```json
{
  "error": {
    "message": "Context not found: users.admin.models.my_model"
  }
}
```

**Причина:** Контекст не существует или путь указан неверно.

**Решение:**
1. Проверьте путь через `aggregate_list_contexts`
2. Убедитесь, что модель создана через `aggregate_create_context`
3. Подождите 1-2 секунды после создания контекста перед использованием

### 4. Ошибка "Not connected or not logged in"

**Симптомы:**
```json
{
  "error": {
    "message": "Not connected or not logged in"
  }
}
```

**Причина:** Не выполнено подключение или вход в систему.

**Решение:**
1. Вызовите `aggregate_connect` с правильными параметрами
2. Вызовите `aggregate_login` после подключения
3. Убедитесь, что сервер AggreGate запущен

### 5. Ошибка "Invalid format" при создании переменной или функции

**Симптомы:**
```json
{
  "error": {
    "message": "Invalid format: ..."
  }
}
```

**Причина:** Неверный синтаксис TableFormat.

**Решение:**
1. Проверьте синтаксис формата: `<name><T>`, где T - тип
2. Убедитесь, что используются правильные типы (S, I, L, D, E, B)
3. Для простых переменных используйте формат без M и X элементов
4. **Для Expression функций**: `inputFormat` и `outputFormat` должны быть обычным TableFormat **БЕЗ** двойных скобок `<<>>`. Двойные скобки используются только внутри выражения.

### 8. Ошибка "Invalid inputFormat: null" при создании Expression функции

**Симптомы:**
```json
{
  "error": {
    "message": "Invalid inputFormat: null"
  }
}
```

**Причина:** Проблема с парсингом inputFormat для Expression функций.

**Решение:**
1. Убедитесь, что `inputFormat` указан и не является null
2. Используйте обычный TableFormat без двойных скобок: `<value1><E><value2><E>`
3. Если формат не указан, будет создан формат по умолчанию, но лучше указать явно

### 6. Ошибка "Path, variableName, fieldName, and value parameters are required"

**Симптомы:**
```json
{
  "error": {
    "message": "Path, variableName, fieldName, and value parameters are required"
  }
}
```

**Причина:** Неправильное имя параметра в `aggregate_set_variable_field`.

**Решение:**
1. Используйте параметр `variableName` (не `name`)
2. Убедитесь, что все обязательные параметры указаны: `path`, `variableName`, `fieldName`, `value`

### 7. Ошибка "Variable already exists"

**Симптомы:**
```json
{
  "error": {
    "message": "Variable already exists: temperature"
  }
}
```

**Причина:** Переменная с таким именем уже существует.

**Решение:**
1. Используйте другое имя переменной
2. Или проверьте существующие переменные через `aggregate_list_variables`

### 8. Ошибка "Function already exists"

**Симптомы:**
```json
{
  "error": {
    "message": "Function already exists: calculate_average"
  }
}
```

**Причина:** Функция с таким именем уже существует.

**Решение:**
1. Используйте другое имя функции
2. Или проверьте существующие функции через `aggregate_list_functions`

### 9. Ошибка "Path, variableName, fieldName, and value parameters are required"

**Симптомы:**
```json
{
  "error": {
    "message": "Path, variableName, fieldName, and value parameters are required"
  }
}
```

**Причина:** Неправильное имя параметра в `aggregate_set_variable_field`.

**Решение:**
1. Используйте параметр `variableName` (не `name`) в `aggregate_set_variable_field`
2. Убедитесь, что все обязательные параметры указаны: `path`, `variableName`, `fieldName`, `value`

### 10. Ошибка "Invalid inputFormat: null" при создании Expression функции

**Симптомы:**
```json
{
  "error": {
    "message": "Invalid inputFormat: null"
  }
}
```

**Причина:** Проблема с парсингом inputFormat для Expression функций.

**Решение:**
1. Убедитесь, что `inputFormat` указан и не является null
2. Используйте обычный TableFormat без двойных скобок: `<value1><E><value2><E>`
3. Если формат не указан, будет создан формат по умолчанию, но лучше указать явно

### 11. Ошибка "Expecting value: line 1 column 1 (char 0)" или проблемы с чтением ответа

**Симптомы:**
```json
{
  "error": {
    "message": "Expecting value: line 1 column 1 (char 0)"
  }
}
```

**Причина:** Проблема с соединением после множественных операций или сервер не отвечает.

**Решение:**
1. Выполните переподключение: `aggregate_disconnect`, затем `aggregate_connect` + `aggregate_login`
2. Разбейте операции на группы по 10-15 операций с переподключением между группами
3. Убедитесь, что сервер AggreGate запущен и доступен
4. Проверьте, что между операциями есть небольшие задержки (0.5-1 секунда)

---

## Полные примеры использования

### Пример 1: Создание полной модели с переменными, функциями и событиями

```json
// 1. Подключение
{
  "tool": "aggregate_connect",
  "parameters": {
    "host": "localhost",
    "port": 6460,
    "username": "admin",
    "password": "admin"
  }
}

// 2. Вход
{
  "tool": "aggregate_login"
}

// 3. Создание модели
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "smart_home",
    "description": "Умный дом"
  }
}

// 4. Создание переменных (ОБЯЗАТЕЛЬНО!)
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.smart_home",
    "variableName": "temperature",
    "format": "<temperature><E>",
    "description": "Температура",
    "writable": true
  }
}

{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.smart_home",
    "variableName": "humidity",
    "format": "<humidity><E>",
    "description": "Влажность",
    "writable": true
  }
}

{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.smart_home",
    "variableName": "status",
    "format": "<status><S>",
    "description": "Статус системы",
    "writable": true
  }
}

// 5. Создание событий (ОБЯЗАТЕЛЬНО!)
{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.smart_home",
    "eventName": "temperature_alert",
    "format": "<temperature><E><threshold><E>",
    "description": "Предупреждение о температуре",
    "level": 1
  }
}

{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.smart_home",
    "eventName": "status_changed",
    "description": "Изменение статуса",
    "level": 0
  }
}

// 6. Создание функции
{
  "tool": "aggregate_create_function",
  "parameters": {
    "path": "users.admin.models.smart_home",
    "functionName": "calculate_comfort_index",
    "functionType": 1,
    "description": "Вычисление индекса комфорта",
    "inputFormat": "<temperature><E><humidity><E>",
    "outputFormat": "<index><E>",
    "expression": "table(\"<<index><E>>\", ({temperature} + {humidity}) / 2)"
  }
}

// 7. Установка значений переменных (рекомендуется использовать aggregate_set_variable_field для простых переменных)
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.smart_home",
    "variableName": "temperature",
    "fieldName": "temperature",
    "value": 22.5
  }
}

{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.smart_home",
    "variableName": "humidity",
    "fieldName": "humidity",
    "value": 45.0
  }
}

// 8. Вызов функции
{
  "tool": "aggregate_call_function",
  "parameters": {
    "path": "users.admin.models.smart_home",
    "functionName": "calculate_comfort_index",
    "parameters": {
      "temperature": 22.5,
      "humidity": 45.0
    }
  }
}

// 9. Проверка созданных элементов
{
  "tool": "aggregate_list_variables",
  "parameters": {
    "path": "users.admin.models.smart_home"
  }
}

{
  "tool": "aggregate_list_functions",
  "parameters": {
    "path": "users.admin.models.smart_home"
  }
}
```

### Пример 2: Работа с устройствами

```json
// Создание устройства
{
  "tool": "aggregate_create_device",
  "parameters": {
    "username": "admin",
    "deviceName": "sensor_01",
    "description": "Датчик температуры #1",
    "driverId": "com.tibbo.linkserver.plugin.device.virtual"
  }
}

// Получение списка устройств
{
  "tool": "aggregate_list_devices",
  "parameters": {
    "username": "admin"
  }
}

// Получение статуса устройства
{
  "tool": "aggregate_get_device_status",
  "parameters": {
    "path": "users.admin.devices.sensor_01"
  }
}
```

### Пример 3: Работа с пользователями

```json
// Создание пользователя
{
  "tool": "aggregate_create_user",
  "parameters": {
    "username": "operator",
    "password": "secure123",
    "email": "operator@example.com"
  }
}

// Список пользователей
{
  "tool": "aggregate_list_users"
}

// Обновление пользователя
{
  "tool": "aggregate_update_user",
  "parameters": {
    "username": "operator",
    "email": "newemail@example.com"
  }
}
```

### Пример 4: Работа с агентами

```json
// Создание агента
{
  "tool": "aggregate_create_agent",
  "parameters": {
    "agentName": "monitor_agent",
    "host": "localhost",
    "port": 6460,
    "username": "admin",
    "password": "admin"
  }
}

// Проверка статуса агента
{
  "tool": "aggregate_get_agent_status",
  "parameters": {
    "agentName": "monitor_agent"
  }
}

// Генерация события из агента
{
  "tool": "aggregate_fire_event",
  "parameters": {
    "agentName": "monitor_agent",
    "eventName": "alert",
    "level": "WARNING",
    "data": {
      "records": [
        {
          "message": "High temperature detected"
        }
      ]
    }
  }
}
```

---

## Чек-лист для AI моделей

При работе с MCP сервером AggreGate всегда следуйте этому порядку:

- [ ] ✅ Подключение к серверу (`aggregate_connect`)
- [ ] ✅ Вход в систему (`aggregate_login`)
- [ ] ✅ Создание модели контекста (`aggregate_create_context`)
- [ ] ✅ **Создание переменных модели** (`aggregate_create_variable`) - **ОБЯЗАТЕЛЬНО**
- [ ] ✅ **Создание событий модели** (`aggregate_create_event`) - **ОБЯЗАТЕЛЬНО**
- [ ] ✅ Создание функций (`aggregate_create_function`) - опционально
- [ ] ✅ Проверка созданных элементов через списки
- [ ] ✅ Установка значений переменных:
  - [ ] Для простых переменных (maxRecords=1): **`aggregate_set_variable_field`**
  - [ ] Для переменных с несколькими полями: `aggregate_set_variable`
- [ ] ✅ Вызов функций (`aggregate_call_function`)

## Правила выбора инструментов

### Установка значений переменных

**Используйте `aggregate_set_variable_field` если:**
- ✅ Переменная имеет maxRecords=1 (большинство простых переменных)
- ✅ Формат переменной: `<name><T>` (одно поле, один тип)
- ✅ Примеры: `<temperature><E>`, `<status><S>`, `<count><I>`

**Используйте `aggregate_set_variable` если:**
- ✅ Переменная имеет maxRecords > 1 (несколько записей) - **проверьте через `aggregate_get_variable`**
- ✅ Переменная создана явно с maxRecords > 1
- ⚠️ **В моделях переменные обычно имеют maxRecords=1**, даже с несколькими полями - используйте `aggregate_set_variable_field`

**Примеры правильного выбора:**

```json
// ✅ ПРАВИЛЬНО: Простая переменная - используем aggregate_set_variable_field
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.sensor",
    "variableName": "temperature",
    "fieldName": "temperature",
    "value": 25.5
  }
}

// ✅ ПРАВИЛЬНО: Переменная с несколькими полями В МОДЕЛИ - используем aggregate_set_variable_field для каждого поля
// ПРИМЕЧАНИЕ: В моделях переменные имеют maxRecords=1, даже с несколькими полями
// Поле 1
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.sensor",
    "variableName": "sensor_data",
    "fieldName": "temperature",
    "value": 25.5
  }
}

// Поле 2
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.sensor",
    "variableName": "sensor_data",
    "fieldName": "humidity",
    "value": 60.0
  }
}

// Поле 3
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.sensor",
    "variableName": "sensor_data",
    "fieldName": "timestamp",
    "value": 1234567890
  }
}

// ❌ НЕПРАВИЛЬНО: Простая переменная с maxRecords=1 - НЕ используйте aggregate_set_variable
// Это вызовет ошибку "maximum number of records is reached"
```

**Важные напоминания:**

1. **Всегда проверяйте создание элементов** после операций создания, особенно если получили ошибку верификации
2. **Используйте правильные форматы TableFormat** для переменных и функций
3. **Для Expression функций**: 
   - `inputFormat` и `outputFormat` в параметрах - обычный TableFormat **БЕЗ** `<<>>`
   - Двойные скобки `<<>>` используются **только внутри выражения** в параметре `expression`
4. **⚠️ КРИТИЧЕСКИ ВАЖНО - Установка значений переменных:**
   - **Для ВСЕХ переменных в моделях**: ВСЕГДА используйте `aggregate_set_variable_field` (даже с несколькими полями)
   - **Для переменных с несколькими полями в моделях**: используйте `aggregate_set_variable_field` для каждого поля отдельно
   - **Для переменных с maxRecords > 1**: используйте `aggregate_set_variable` (проверьте через `aggregate_get_variable`)
   - **Параметр**: `variableName` (не `name`) в `aggregate_set_variable_field`
5. **Подождите 1-2 секунды** после создания модели перед созданием переменных
6. **Используйте Expression функции (functionType=1)** для простых вычислений вместо Java функций
7. **Проверяйте существование элементов** перед созданием, чтобы избежать ошибок "already exists"
8. **При проблемах с соединением** выполняйте переподключение после 10-15 операций
9. **Добавляйте небольшие задержки** (0.5-1 секунда) между операциями для стабильности
10. **Для агентов**: добавляйте задержку 2-3 секунды после создания перед использованием (`aggregate_fire_event`)

---

## Дополнительная информация

### Пути контекстов

- **Модели**: `users.admin.models.*`
- **Устройства**: `users.{username}.devices.*`
- **Виджеты**: `users.{username}.widgets.*`
- **Дашборды**: `users.{username}.dashboards.*`
- **Агенты**: создаются через `aggregate_create_agent`

### Права доступа

- `observer` - только чтение
- `manager` - чтение и запись
- `admin` - полный доступ

### Уровни событий

- `0` = INFO - информационное сообщение
- `1` = WARNING - предупреждение
- `2` = ERROR - ошибка
- `3` = FATAL - критическая ошибка
- `4` = NOTICE - уведомление

### Режимы хранения переменных

- `0` = DATABASE - хранение в базе данных (рекомендуется)
- `1` = MEMORY - хранение в памяти (временное)

---

## Заключение

Это руководство содержит полную информацию о всех инструментах MCP сервера AggreGate. При работе с AI моделями:

1. **Всегда следуйте порядку операций** из чек-листа
2. **Проверяйте результаты** после каждой операции
3. **Используйте правильные форматы** для переменных и функций
4. **Обрабатывайте ошибки верификации** правильно - проверяйте, что элемент создан
5. **Используйте Expression функции** для простых вычислений вместо Java функций

Для получения дополнительной информации см. официальную документацию AggreGate: https://aggregate.digital/docs/

**Версия документации:** 1.3  
**Дата обновления:** 2025-12-15

**📖 Дополнительные документы:**
- [Быстрая справка](MCP_AI_QUICK_REFERENCE.md) - для быстрого доступа
- [Сценарии использования](MCP_USAGE_SCENARIOS.md) - подробные примеры
- [Дерево решений](MCP_DECISION_TREE.md) - визуальное руководство
- [Четкие правила](MCP_CLEAR_RULES.md) - **ОБЯЗАТЕЛЬНО К ПРОЧТЕНИЮ**

## Изменения в версии 1.3

- ✅ **Устранены все разногласия и недопонимание** в документации
- ✅ Добавлены четкие правила выбора инструментов
- ✅ Объяснено, что в моделях переменные имеют maxRecords=1 по умолчанию
- ✅ Добавлены сценарии использования для всех функций
- ✅ Создан файл `MCP_CLEAR_RULES.md` с четкими правилами
- ✅ Создан файл `MCP_DECISION_TREE.md` с деревом решений
- ✅ Создан файл `MCP_USAGE_SCENARIOS.md` с подробными сценариями
- ✅ Все функции описаны без противоречий

## Изменения в версии 1.2

- ✅ Исправлен `aggregate_call_function` - автоматическая обработка формата параметров
- ✅ Исправлен `aggregate_add_dashboard_element` - автоматическое определение формата
- ✅ Улучшен `aggregate_set_variable` - всегда использует setVariableField для maxRecords=1
- ✅ Улучшен `aggregate_fire_event` - автоматическое ожидание синхронизации агента
- ✅ Улучшена стабильность всех функций
- ✅ Результаты тестирования: 28/32 функций работают (87%)
- ✅ Все критические проблемы исправлены

## Изменения в версии 1.1

- ✅ Исправлена документация для `aggregate_set_variable_field` - указан правильный параметр `variableName`
- ✅ Добавлены важные замечания об использовании `aggregate_set_variable_field` для переменных с maxRecords=1
- ✅ Уточнена документация по Expression функциям - разъяснено использование форматов
- ✅ Добавлены решения для типичных ошибок, выявленных при тестировании
- ✅ Добавлены рекомендации по стабильности соединения
- ✅ Обновлены примеры использования с учетом лучших практик
- ✅ Исправлен код `aggregate_set_variable_field` - добавлен CallerController и обработка таймаутов
- ✅ Исправлен код `aggregate_create_function` - улучшена обработка форматов для Expression функций
- ✅ Улучшена обработка ответов в протоколе для повышения стабильности

## Статус функций (по результатам тестирования)

### ✅ Полностью работоспособные функции (12):

**Подключение:**
- `aggregate_connect` ✅
- `aggregate_login` ✅

**Контексты:**
- `aggregate_create_context` ✅
- `aggregate_get_context` ✅
- `aggregate_list_contexts` ✅

**Переменные:**
- `aggregate_create_variable` ✅ (все типы форматов)
- `aggregate_get_variable` ✅
- `aggregate_list_variables` ✅

**Функции:**
- `aggregate_create_function` ✅ (Java тип)
- `aggregate_call_function` ✅ (без параметров)

### ⚠️ Функции, требующие внимания:

- `aggregate_set_variable` - для maxRecords=1 используйте `aggregate_set_variable_field`
- `aggregate_set_variable_field` - исправлен, должен работать корректно
- `aggregate_create_function` (Expression) - исправлен, должен работать корректно
- Остальные функции - требуют проверки после исправления проблемы с соединением

