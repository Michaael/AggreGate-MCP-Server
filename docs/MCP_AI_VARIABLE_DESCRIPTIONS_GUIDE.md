# Руководство для ИИ: Использование описаний полей переменных

## Введение

При работе с переменными в AggreGate **критически важно** понимать формат переменной и описания всех полей перед их заполнением. Это позволяет ИИ правильно интерпретировать назначение каждого поля и заполнять его корректными значениями.

## Критическое правило

**ВСЕГДА вызывайте `aggregate_describe_variable` перед установкой значения переменной!**

## Процесс работы с переменными

### Шаг 1: Получить описание переменной

```json
{
  "tool": "aggregate_describe_variable",
  "parameters": {
    "path": "users.admin.models.myModel",
    "variableName": "childInfo"
  }
}
```

### Шаг 2: Изучить ответ

Ответ содержит:
- **`fields`** - массив полей с описаниями
- **`aiGuidance`** - рекомендации для ИИ
- **`requiredFields`** - обязательные поля
- **`optionalFields`** - опциональные поля

**Пример ответа:**
```json
{
  "name": "childInfo",
  "description": "Properties",
  "fields": [
    {
      "name": "type",
      "type": "I",
      "typeName": "Integer",
      "description": "Type",
      "nullable": false,
      "hasDefault": false,
      "formatString": "<type><I>"
    },
    {
      "name": "containerType",
      "type": "S",
      "typeName": "String",
      "description": "Container Type",
      "nullable": true,
      "hasDefault": false,
      "formatString": "<containerType><S>"
    }
  ],
  "aiGuidance": {
    "note": "Before setting this variable, review all field descriptions to understand their purpose and requirements.",
    "requiredFields": ["type"],
    "optionalFields": ["containerType", "objectType"]
  }
}
```

### Шаг 3: Использовать описания полей для правильного заполнения

**Ключевая информация из описаний:**
- **`description`** - описание назначения поля (КРИТИЧНО для понимания!)
- **`typeName`** - тип данных (String, Integer, Boolean, и т.д.)
- **`nullable`** - может ли поле быть null
- **`hasDefault`** - есть ли значение по умолчанию
- **`defaultValue`** - значение по умолчанию (если есть)

### Шаг 4: Установить значение с учетом описаний

**Для простых переменных (одно поле):**
```json
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.myModel",
    "variableName": "status",
    "fieldName": "status",
    "value": "active"  // Используйте описание поля для понимания допустимых значений
  }
}
```

**Для переменных с несколькими полями:**
```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.myModel",
    "name": "childInfo",
    "value": {
      "records": [{
        "type": 0,  // Из описания: "Type" - Integer, required
        "containerType": "devices",  // Из описания: "Container Type" - String, optional
        "objectType": "device"  // Из описания: "Object Type" - String, optional
      }]
    }
  }
}
```

## Примеры использования описаний

### Пример 1: Установка типа модели

**Шаг 1: Получить описание**
```json
{
  "tool": "aggregate_describe_variable",
  "parameters": {
    "path": "users.admin.models.relativeModel",
    "variableName": "childInfo"
  }
}
```

**Шаг 2: Изучить поля**
- `type` - "Type" (Integer, required) - тип модели
- `containerType` - "Container Type" (String, optional) - тип контейнера
- `objectType` - "Object Type" (String, optional) - тип объекта

**Шаг 3: Установить значения**
```json
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.relativeModel",
    "variableName": "childInfo",
    "fieldName": "type",
    "value": 0  // Из описания понимаем, что это тип модели (0=relative)
  }
}
```

### Пример 2: Установка привязок (bindings)

**Шаг 1: Получить описание**
```json
{
  "tool": "aggregate_describe_variable",
  "parameters": {
    "path": "users.admin.models.myModel",
    "variableName": "bindings"
  }
}
```

**Шаг 2: Изучить формат**
- Формат: массив записей
- Поля: `target`, `expression`, `onevent`
- Описания полей помогут понять:
  - `target` - куда привязывается (обычно `.:variableName`)
  - `expression` - выражение для вычисления значения
  - `onevent` - обновлять ли при событии

**Шаг 3: Установить привязки**
```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.myModel",
    "name": "bindings",
    "value": {
      "records": [{
        "target": ".:wavesSum",  // Из описания понимаем формат
        "expression": "{.:sine} + {.:sawtooth}",  // Из описания понимаем формат
        "onevent": true  // Из описания понимаем назначение
      }]
    }
  }
}
```

## Ключевые правила для ИИ

### ✅ ВСЕГДА делать:

1. **Вызывать `aggregate_describe_variable` перед установкой переменной**
2. **Изучать описания всех полей (`fields[].description`)**
3. **Проверять обязательные поля (`aiGuidance.requiredFields`)**
4. **Использовать описания для понимания назначения полей**
5. **Учитывать типы данных (`typeName`)**
6. **Проверять, может ли поле быть null (`nullable`)**

### ❌ НИКОГДА не делать:

1. Устанавливать переменную без предварительного изучения формата
2. Игнорировать описания полей
3. Заполнять поля значениями, не соответствующими их назначению
4. Пропускать обязательные поля
5. Использовать неправильные типы данных

## Автоматизация

Инструменты `aggregate_set_variable_field`, `aggregate_set_variable`, и `aggregate_set_variable_smart` теперь содержат в описании напоминание о необходимости вызывать `aggregate_describe_variable` перед использованием.

## Преимущества

1. **Правильное понимание назначения полей** - описания помогают понять, что означает каждое поле
2. **Корректное заполнение** - знание типов и требований предотвращает ошибки
3. **Автоматическая валидация** - информация о required/optional полях помогает избежать ошибок
4. **Лучшее качество данных** - правильное понимание полей приводит к корректным значениям

## Пример полного процесса

```json
// 1. Получить описание
{
  "tool": "aggregate_describe_variable",
  "parameters": {
    "path": "users.admin.models.myModel",
    "variableName": "settings"
  }
}

// 2. Изучить ответ:
// - fields[0].name = "enabled", description = "Enable model", typeName = "Boolean"
// - fields[1].name = "timeout", description = "Timeout in milliseconds", typeName = "Integer"
// - requiredFields = ["enabled"]
// - optionalFields = ["timeout"]

// 3. Установить значение с учетом описаний
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.myModel",
    "name": "settings",
    "value": {
      "records": [{
        "enabled": true,  // Из описания: "Enable model" - Boolean, required
        "timeout": 5000  // Из описания: "Timeout in milliseconds" - Integer, optional
      }]
    }
  }
}
```

## Заключение

Использование `aggregate_describe_variable` перед установкой переменных - это **критически важная практика**, которая позволяет ИИ правильно понимать и заполнять переменные в AggreGate. Описания полей содержат ценную информацию о назначении каждого поля, что помогает избежать ошибок и создавать корректные конфигурации.
