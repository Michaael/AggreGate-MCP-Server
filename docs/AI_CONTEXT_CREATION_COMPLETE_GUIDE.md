# Полное руководство для ИИ по созданию контекстов AggreGate

## Введение

Это руководство содержит все необходимые знания для ИИ о том, как правильно создавать контексты всех типов в AggreGate, работать с переменными, функциями, событиями, привязками и выражениями. Все правила основаны на анализе реального сервера AggreGate и проверенных практиках.

## 1. Типы контекстов и их создание

### 1.1. Модели (Models)

Модели создаются в контексте `users.{username}.models` и используются для обработки данных от устройств или других источников.

#### Типы моделей

**1. Относительная модель (Relative Model, type=0)**
- Создается по одному экземпляру на каждый объект (например, устройство)
- Использует относительные ссылки в привязках: `{.:variableName}`
- Требует настройки `containerType` и `objectType`

**2. Абсолютная модель (Absolute Model, type=1)**
- Один экземпляр для всех объектов
- Использует абсолютные пути в привязках: `{users.admin.devices.device1:variable}`
- По умолчанию для моделей, созданных без указания типа

**3. Экземплярная модель (Instance Model, type=2)**
- Создается по требованию
- Может объединять данные от нескольких объектов

#### Создание модели

```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "myModel",
    "description": "Описание модели",
    "modelType": 0,  // 0=relative, 1=absolute (default), 2=instance
    "containerType": "devices",  // Требуется для relative (type=0)
    "objectType": "device"  // Требуется для relative (type=0)
  }
}
```

**⚠️ КРИТИЧЕСКИ ВАЖНО:**
- Для относительных моделей (type=0) ОБЯЗАТЕЛЬНО указать `containerType` и `objectType`
- Тип модели устанавливается автоматически при создании
- После создания модели нужно создать переменные, события и привязки

### 1.2. Устройства (Devices)

Устройства создаются через специальные функции драйверов. Используйте `aggregate_create_device`:

```json
{
  "tool": "aggregate_create_device",
  "parameters": {
    "username": "admin",
    "deviceName": "myDevice",
    "description": "Описание устройства",
    "driverId": "com.tibbo.linkserver.plugin.device.virtual"
  }
}
```

### 1.3. Виджеты (Widgets)

Виджеты создаются в контексте `users.{username}.widgets`:

```json
{
  "tool": "aggregate_create_widget",
  "parameters": {
    "parentPath": "users.admin.widgets",
    "name": "myWidget",
    "description": "Описание виджета",
    "template": "<xml>...</xml>",  // Опционально
    "defaultContext": "users.admin.models.myModel"  // Опционально
  }
}
```

### 1.4. Дашборды (Dashboards)

Дашборды создаются в контексте `users.{username}.dashboards`:

```json
{
  "tool": "aggregate_create_dashboard",
  "parameters": {
    "parentPath": "users.admin.dashboards",
    "name": "myDashboard",
    "description": "Описание дашборда",
    "layout": "dockable"  // dockable, scrollable, grid, absolute
  }
}
```

### 1.5. Тревоги (Alerts)

Тревоги создаются в контексте `users.{username}.alerts`. Они автоматически создаются при создании моделей с определенными настройками, но могут быть созданы и отдельно.

## 2. Работа с переменными

### 2.1. Формат переменных (TableFormat)

Переменные в AggreGate используют специальный формат TableFormat:

**Базовый синтаксис:**
- `<name><S>` - Строка (String)
- `<name><I>` - Целое число (Integer)
- `<name><L>` - Длинное целое (Long)
- `<name><E>` - Число с плавающей точкой (Extended/Double) - **рекомендуется для чисел**
- `<name><D>` - Число с плавающей точкой (Double)
- `<name><B>` - Булево значение (Boolean)
- `<name><T>` - Таблица данных (DataTable)

**Для простых переменных (одно значение):**
```
<temperature><E>
<status><S>
<counter><I>
```

**Для табличных переменных (множественные записи):**
```
<name><S><value><E><timestamp><D>
```

### 2.2. Создание переменных в моделях

**⚠️ КРИТИЧЕСКИ ВАЖНО:** Переменные в моделях НЕ создаются автоматически! Их нужно создавать явно через `aggregate_create_variable`.

```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.myModel",
    "variableName": "temperature",
    "format": "<temperature><E>",
    "description": "Температура",
    "group": "Датчики",
    "writable": true,  // ОБЯЗАТЕЛЬНО true для переменных, которые будут обновляться через привязки
    "readPermissions": "observer",
    "writePermissions": "manager",
    "storageMode": 0  // 0=база данных, 1=память
  }
}
```

**Параметры переменной:**
- `path` - путь к контексту (обязательно)
- `variableName` - имя переменной (обязательно)
- `format` - формат в TableFormat (обязательно)
- `description` - описание (опционально)
- `group` - группа переменных (опционально)
- `writable` - можно ли записывать (по умолчанию `false`, но для моделей обычно `true`)
- `readPermissions` - права на чтение (по умолчанию `"observer"`)
- `writePermissions` - права на запись (по умолчанию `"manager"`)
- `storageMode` - режим хранения: `0` = база данных, `1` = память (по умолчанию `0`)

### 2.3. Примеры переменных из реального сервера

**Простая числовая переменная (Sine Wave):**
```json
{
  "name": "sine",
  "format": "<value><F>",
  "description": "Sine Wave",
  "writable": false,
  "readable": true
}
```

**Табличная переменная:**
```json
{
  "name": "table",
  "format": "<string><S><int><I><date><D>",
  "description": "Tabular Setting",
  "writable": true
}
```

**Переменная модели (sumWaves):**
```json
{
  "name": "sumWaves",
  "format": "<sumWaves><E>",
  "description": "Сумма значений Sine Wave и Sawtooth Wave",
  "writable": true,
  "group": "custom"
}
```

### 2.4. Чтение и запись переменных

**Чтение переменной:**
```json
{
  "tool": "aggregate_get_variable",
  "parameters": {
    "path": "users.admin.models.myModel",
    "name": "temperature"
  }
}
```

**Запись простой переменной (одно поле):**
```json
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.myModel",
    "variableName": "temperature",
    "fieldName": "value",
    "value": 25.5
  }
}
```

**Запись табличной переменной:**
```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.myModel",
    "variableName": "table",
    "value": {
      "records": [
        {"string": "test", "int": 10, "date": "2025-01-27T00:00:00Z"}
      ],
      "format": {
        "fields": [
          {"name": "string", "type": "S"},
          {"name": "int", "type": "I"},
          {"name": "date", "type": "D"}
        ]
      }
    }
  }
}
```

**⚠️ ВАЖНО:** Перед записью переменной всегда используйте `aggregate_describe_variable` для понимания структуры переменной!

## 3. Работа с функциями

### 3.1. Типы функций

**1. Java функции (type=0)**
- Реализованы на Java
- Требуют `inputFormat` и `outputFormat`
- Не требуют `expression` или `query`

**2. Expression функции (type=1)**
- Реализованы через выражения AggreGate
- Требуют `inputFormat`, `outputFormat` и `expression`
- **⚠️ КРИТИЧЕСКИ ВАЖНО:** См. раздел 3.2

**3. Query функции (type=2)**
- Реализованы через запросы
- Требуют `query`
- Не требуют `inputFormat` и `outputFormat`

### 3.2. Expression функции - КРИТИЧЕСКИЕ ПРАВИЛА

**⚠️ ВСЕГДА используйте следующий рабочий процесс:**

#### Шаг 1: Построение правильных форматов

```json
{
  "tool": "aggregate_build_expression",
  "parameters": {
    "inputFields": [
      {"name": "value1", "type": "E", "description": "Первое значение"},
      {"name": "value2", "type": "E", "description": "Второе значение"}
    ],
    "outputFields": [
      {"name": "result", "type": "E", "description": "Результат"}
    ],
    "formula": "({value1} + {value2}) / 2"
  }
}
```

**Результат:**
```json
{
  "inputFormat": "<value1><E><value2><E>",  // БЕЗ <<>>!
  "outputFormat": "<result><E>",  // БЕЗ <<>>!
  "expression": "table(\"<<result><E>>\", ({value1} + {value2}) / 2)"  // С <<>> внутри table()!
}
```

#### Шаг 2: Валидация

```json
{
  "tool": "aggregate_validate_expression",
  "parameters": {
    "inputFormat": "<value1><E><value2><E>",
    "outputFormat": "<result><E>",
    "expression": "table(\"<<result><E>>\", ({value1} + {value2}) / 2)"
  }
}
```

#### Шаг 3: Создание функции

**Для функций с одним полем:**
```json
{
  "tool": "aggregate_create_function",
  "parameters": {
    "path": "users.admin.models.myModel",
    "functionName": "calculate_single",
    "functionType": 1,
    "inputFormat": "<value><E>",  // БЕЗ <<>>
    "outputFormat": "<result><E>",  // БЕЗ <<>>
    "expression": "table(\"<<result><E>>\", {value} * 2)",  // С <<>> внутри table()
    "description": "Удвоение значения"
  }
}
```

**Для функций с несколькими полями (ИСПОЛЬЗУЙТЕ <<>> в inputFormat/outputFormat):**
```json
{
  "tool": "aggregate_create_function",
  "parameters": {
    "path": "users.admin.models.myModel",
    "functionName": "calculate_average",
    "functionType": 1,
    "inputFormat": "<<value1><E><value2><E>>",  // С <<>> для множественных полей!
    "outputFormat": "<<result><E>>",  // С <<>> для множественных полей!
    "expression": "table(\"<<result><E>>\", ({value1} + {value2}) / 2)",  // С <<>> внутри table()
    "description": "Вычисление среднего значения"
  }
}
```

**КРИТИЧЕСКИЕ ПРАВИЛА:**
1. **inputFormat/outputFormat БЕЗ <<>>** для одного поля: `<value><E>`
2. **inputFormat/outputFormat С <<>>** для нескольких полей: `<<value1><E><value2><E>>`
3. **expression ВСЕГДА С <<>>** внутри table(): `table(\"<<result><E>>\", ...)`
4. **ВСЕГДА используйте `aggregate_build_expression`** перед созданием
5. **ВСЕГДА используйте `aggregate_validate_expression`** перед созданием

### 3.3. Пример функции из реального сервера

**Функция calculate (Java функция):**
```json
{
  "name": "calculate",
  "description": "Calculate",
  "group": "remote",
  "inputFormat": {
    "fields": [
      {"name": "leftOperand", "type": "F"},
      {"name": "rightOperand", "type": "F"},
      {"name": "operation", "type": "S"}
    ]
  },
  "outputFormat": {
    "fields": [
      {"name": "result", "type": "F"}
    ]
  },
  "functionType": 0
}
```

### 3.4. Вызов функций

```json
{
  "tool": "aggregate_call_function",
  "parameters": {
    "path": "users.admin.devices.virtualDevice",
    "functionName": "calculate",
    "parameters": {
      "leftOperand": 10.0,
      "rightOperand": 5.0,
      "operation": "+"
    }
  }
}
```

## 4. Работа с событиями

### 4.1. Создание событий в моделях

**⚠️ КРИТИЧЕСКИ ВАЖНО:** События в моделях НЕ создаются автоматически! Их нужно создавать явно через `aggregate_create_event`.

```json
{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.myModel",
    "eventName": "alarm",
    "format": "<message><S><severity><I>",
    "description": "Событие тревоги",
    "group": "События",
    "level": 2,  // 0=INFO, 1=WARNING, 2=ERROR, 3=FATAL, 4=NOTICE
    "permissions": "observer",
    "firePermissions": "manager",
    "historyStorageTime": 86400000  // Время хранения истории в миллисекундах
  }
}
```

**Параметры события:**
- `path` - путь к контексту (обязательно)
- `eventName` - имя события (обязательно)
- `format` - формат события в TableFormat (опционально, по умолчанию пустой)
- `description` - описание (опционально)
- `group` - группа событий (опционально)
- `level` - уровень события: `0`=INFO, `1`=WARNING, `2`=ERROR, `3`=FATAL, `4`=NOTICE
- `permissions` - права на чтение (по умолчанию `"observer"`)
- `firePermissions` - права на генерацию события (по умолчанию `"admin"`)
- `historyStorageTime` - время хранения истории в миллисекундах (по умолчанию `0`)

### 4.2. Примеры событий из реального сервера

**Простое событие:**
```json
{
  "name": "event1",
  "description": "Virtual Device Event #1",
  "group": "remote",
  "level": 0  // INFO
}
```

**Событие с параметрами:**
```json
{
  "name": "alarm",
  "format": "<message><S><severity><I>",
  "description": "Событие тревоги",
  "level": 2  // ERROR
}
```

### 4.3. Генерация событий

**Из агента:**
```json
{
  "tool": "aggregate_fire_event",
  "parameters": {
    "agentName": "myAgent",
    "eventName": "alarm",
    "level": "ERROR",
    "data": {
      "records": [
        {"message": "Критическая ошибка", "severity": 2}
      ]
    }
  }
}
```

## 5. Работа с привязками (Bindings)

### 5.1. Формат ссылок AggreGate

**Ссылка на текущий контекст:**
- `.:variableName` - переменная в текущем контексте
- `.:functionName()` - функция в текущем контексте
- `.:eventName@` - событие в текущем контексте

**Ссылка на другой контекст:**
- `{contextPath:variableName}` - переменная в другом контексте
- `{contextPath:functionName()}` - функция в другом контексте
- `{contextPath:eventName@}` - событие в другом контексте

**Примеры:**
- `.:temperature` - переменная temperature в текущем контексте
- `{users.admin.devices.device1:sine}` - переменная sine в контексте device1
- `{users.admin.models.cluster:checkStatus()}` - функция checkStatus в модели cluster

### 5.2. Структура привязки

Привязка состоит из:
- `target` - куда записывается значение (всегда `.:variableName`)
- `expression` - откуда берется значение или вычисляемое выражение
- `onevent` - обновление при изменении значения (рекомендуется `true`)
- `onstartup` - обновление при старте (опционально)
- `periodically` - периодическое обновление (опционально)
- `period` - период обновления в миллисекундах (если `periodically=true`)
- `condition` - условие выполнения (опционально)
- `activator` - активатор (опционально)

### 5.3. Создание привязок

**⚠️ КРИТИЧЕСКИ ВАЖНО:** Привязки создаются через переменную `bindings` модели. Без привязок модель не будет получать данные от устройств!

```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.myModel",
    "name": "bindings",
    "value": {
      "records": [
        {
          "target": ".:temperature",
          "expression": "{users.admin.devices.device1:temperature}",
          "onevent": true
        }
      ],
      "format": {
        "fields": [
          {"name": "target", "type": "S"},
          {"name": "expression", "type": "S"},
          {"name": "onevent", "type": "B"}
        ]
      }
    }
  }
}
```

### 5.4. Привязки для относительных моделей

**⚠️ КРИТИЧЕСКИ ВАЖНО:** Для относительных моделей используйте относительные ссылки `{.:variableName}`!

```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.relativeModel",
    "name": "bindings",
    "value": {
      "records": [
        {
          "target": ".:sumWaves",
          "expression": "{.:sine} + {.:sawtooth}",  // Относительные ссылки!
          "onevent": true
        }
      ]
    }
  }
}
```

**✅ ПРАВИЛЬНО для относительных моделей:**
- `{.:sine}` - ссылается на переменную sine текущего объекта (устройства)
- `{.:sawtooth}` - ссылается на переменную sawtooth текущего объекта

**❌ НЕПРАВИЛЬНО для относительных моделей:**
- `{users.admin.devices.device1:sine}` - абсолютный путь не работает в относительных моделях

### 5.5. Привязки для абсолютных моделей

**⚠️ КРИТИЧЕСКИ ВАЖНО:** Для абсолютных моделей используйте абсолютные пути!

```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.absoluteModel",
    "name": "bindings",
    "value": {
      "records": [
        {
          "target": ".:device1Sine",
          "expression": "{users.admin.devices.device1:sine}",  // Абсолютный путь!
          "onevent": true
        },
        {
          "target": ".:device2Sine",
          "expression": "{users.admin.devices.device2:sine}",  // Абсолютный путь!
          "onevent": true
        }
      ]
    }
  }
}
```

**✅ ПРАВИЛЬНО для абсолютных моделей:**
- `{users.admin.devices.device1:sine}` - абсолютный путь к переменной

**❌ НЕПРАВИЛЬНО для абсолютных моделей:**
- `{.:sine}` - относительная ссылка не работает в абсолютных моделях

### 5.6. Пример привязки из реального сервера

**Привязка из модели sumWaveModel:**
```json
{
  "target": ".:sumWaves",
  "expression": "{.:sine} + {.:sawtooth}",
  "onevent": true,
  "periodically": false,
  "period": 60000
}
```

Это относительная модель, поэтому используются относительные ссылки `{.:sine}` и `{.:sawtooth}`.

### 5.7. Выражения в привязках

В выражениях привязок можно использовать:
- Арифметические операции: `+`, `-`, `*`, `/`
- Условные выражения: `>`, `<`, `>=`, `<=`, `==`, `!=`
- Логические операции: `&&`, `||`, `!`
- Функции: `sum()`, `avg()`, `max()`, `min()`, и т.д.

**Примеры:**
```json
{
  "target": ".:total",
  "expression": "{users.admin.devices.device1:value} + {users.admin.devices.device2:value}",
  "onevent": true
}
```

```json
{
  "target": ".:status",
  "expression": "{users.admin.devices.device1:temperature} > 30 ? \"ALARM\" : \"OK\"",
  "onevent": true
}
```

## 6. Выражения AggreGate

### 6.1. Синтаксис выражений

Выражения AggreGate используют JavaScript-подобный синтаксис с поддержкой:
- Арифметических операций
- Условных операторов (`? :`)
- Логических операций
- Функций
- Ссылок на переменные, функции и события

### 6.2. Ссылки в выражениях

**Переменные:**
- `{context:variable}` - переменная в другом контексте
- `.:variable` - переменная в текущем контексте

**Функции:**
- `{context:function()}` - вызов функции в другом контексте
- `.:function()` - вызов функции в текущем контексте

**События:**
- `{context:event@}` - ссылка на событие в другом контексте
- `.:event@` - ссылка на событие в текущем контексте

### 6.3. Примеры выражений

**Простое вычисление:**
```
{users.admin.devices.device1:sine} + {users.admin.devices.device2:sine}
```

**Условное выражение:**
```
{users.admin.devices.device1:temperature} > 30 ? "ALARM" : "OK"
```

**Логическое выражение:**
```
{users.admin.devices.device1:status} == "OK" && {users.admin.devices.device2:status} == "OK"
```

**Вызов функции:**
```
{users.admin.models.calculator:calculate()}(10, 5, "+")
```

**Выражение с несколькими операциями:**
```
({users.admin.devices.device1:value} + {users.admin.devices.device2:value}) / 2
```

### 6.4. Функция table() для Expression функций

**⚠️ КРИТИЧЕСКИ ВАЖНО:** В Expression функциях результат должен быть обернут в функцию `table()` с форматом С <<>>!

**Правильный формат:**
```
table("<<result><E>>", ({value1} + {value2}) / 2)
```

**Неправильный формат:**
```
table("<result><E>", ({value1} + {value2}) / 2)  // ОШИБКА - нет <<>>
```

## 7. Полный рабочий процесс создания модели

### 7.1. Относительная модель

```json
// Шаг 1: Создание модели
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "relativeWavesSum",
    "description": "Относительная модель суммы волн",
    "modelType": 0,
    "containerType": "devices",
    "objectType": "device"
  }
}

// Шаг 2: Создание переменной
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.relativeWavesSum",
    "variableName": "sumWaves",
    "format": "<sumWaves><E>",
    "description": "Сумма Sine Wave и Sawtooth Wave",
    "writable": true
  }
}

// Шаг 3: Создание события (опционально)
{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.relativeWavesSum",
    "eventName": "sumChanged",
    "description": "Изменение суммы",
    "level": 0
  }
}

// Шаг 4: Создание привязки (ОБЯЗАТЕЛЬНО!)
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.relativeWavesSum",
    "name": "bindings",
    "value": {
      "records": [
        {
          "target": ".:sumWaves",
          "expression": "{.:sine} + {.:sawtooth}",
          "onevent": true
        }
      ],
      "format": {
        "fields": [
          {"name": "target", "type": "S"},
          {"name": "expression", "type": "S"},
          {"name": "onevent", "type": "B"}
        ]
      }
    }
  }
}

// Шаг 5: Установка выражения пригодности (ОБЯЗАТЕЛЬНО для относительных моделей!)
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.relativeWavesSum",
    "variableName": "childInfo",
    "fieldName": "validityExpression",
    "value": "{.:sine} != null && {.:sawtooth} != null"
  }
}
```

### 7.2. Абсолютная модель

```json
// Шаг 1: Создание модели
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "cluster",
    "description": "Абсолютная модель кластера"
  }
}

// Шаг 2: Создание переменных
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.cluster",
    "variableName": "device1Sine",
    "format": "<value><E>",
    "writable": true
  }
}

// Шаг 3: Создание привязки (ОБЯЗАТЕЛЬНО!)
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.cluster",
    "name": "bindings",
    "value": {
      "records": [
        {
          "target": ".:device1Sine",
          "expression": "{users.admin.devices.device1:sine}",
          "onevent": true
        }
      ]
    }
  }
}
```

## 8. Типичные ошибки и их исправление

### 8.1. Ошибка: Переменная не создана

**Проблема:** Модель создана, но переменные не созданы.

**Решение:** Всегда создавайте переменные явно через `aggregate_create_variable` после создания модели.

### 8.2. Ошибка: Привязка не работает

**Проблема:** Привязка создана, но данные не обновляются.

**Возможные причины:**
1. Не указан `onevent: true` в привязке
2. Неправильный формат ссылок (относительные vs абсолютные)
3. Переменная не существует в целевом контексте

**Решение:**
- Проверьте формат ссылок (относительные для relative моделей, абсолютные для absolute моделей)
- Убедитесь, что `onevent: true` установлен
- Проверьте существование переменных через `aggregate_list_variables`

### 8.3. Ошибка: Expression функция не работает

**Проблема:** Функция создана, но при вызове возникает ошибка.

**Возможные причины:**
1. Неправильный формат inputFormat/outputFormat (наличие или отсутствие <<>>)
2. Неправильный формат expression (отсутствие <<>> в table())
3. Несоответствие имен полей

**Решение:**
- ВСЕГДА используйте `aggregate_build_expression` перед созданием
- ВСЕГДА используйте `aggregate_validate_expression` перед созданием
- Проверьте формат через `aggregate_get_function` после создания

### 8.4. Ошибка: Относительная модель не создает экземпляры

**Проблема:** Относительная модель создана, но экземпляры не создаются для устройств.

**Возможные причины:**
1. Не установлены `containerType` и `objectType`
2. Не установлено `validityExpression`
3. Устройства не соответствуют условиям `validityExpression`

**Решение:**
- Убедитесь, что при создании модели указаны `containerType` и `objectType`
- Установите `validityExpression` через `aggregate_set_variable_field`
- Проверьте, что устройства имеют необходимые переменные

### 8.5. Ошибка: Неправильный формат ссылок

**Проблема:** В привязках используются неправильные ссылки.

**Решение:**
- Для относительных моделей: используйте `{.:variableName}`
- Для абсолютных моделей: используйте `{contextPath:variableName}`
- В target всегда используйте `.:variableName`

## 9. Чек-лист для ИИ

### При создании модели:

- [ ] Создан контекст через `aggregate_create_context`
- [ ] Для relative модели указаны `containerType` и `objectType`
- [ ] Созданы все необходимые переменные через `aggregate_create_variable`
- [ ] Переменные имеют `writable: true` (если будут обновляться через привязки)
- [ ] Созданы события через `aggregate_create_event` (если нужны)
- [ ] Созданы привязки через `aggregate_set_variable` на переменную `bindings`
- [ ] В привязках используется правильный формат ссылок (относительные для relative, абсолютные для absolute)
- [ ] Для relative модели установлено `validityExpression`

### При создании Expression функции:

- [ ] Использован `aggregate_build_expression` для генерации форматов
- [ ] Использован `aggregate_validate_expression` для проверки
- [ ] inputFormat/outputFormat БЕЗ <<>> для одного поля, С <<>> для нескольких полей
- [ ] expression С <<>> внутри table()
- [ ] После создания проверен через `aggregate_get_function`

### При работе с переменными:

- [ ] Использован `aggregate_describe_variable` перед записью
- [ ] Использован правильный формат TableFormat
- [ ] Для простых переменных использован `aggregate_set_variable_field`
- [ ] Для табличных переменных использован `aggregate_set_variable`

## 10. Дополнительные ресурсы

- [MCP_EXPRESSION_RULES.md](MCP_EXPRESSION_RULES.md) - Правила работы с Expression функциями
- [AGGREGATE_REFERENCES_FORMAT.md](AGGREGATE_REFERENCES_FORMAT.md) - Формат ссылок AggreGate
- [MCP_MODEL_TYPES_GUIDE.md](MCP_MODEL_TYPES_GUIDE.md) - Руководство по типам моделей
- [AGGREGATE_API_GUIDE.md](AGGREGATE_API_GUIDE.md) - Общее руководство по API

---

**Последнее обновление:** 2025-01-27  
**Основано на анализе реального сервера AggreGate:** 62.109.25.124
