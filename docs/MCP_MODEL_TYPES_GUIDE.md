# Руководство по типам моделей в AggreGate

## Введение

В AggreGate существует три типа моделей, каждый из которых имеет свои особенности и применяется в различных сценариях:

1. **Абсолютная модель** (Absolute Model)
2. **Относительная модель** (Relative Model)
3. **Экземплярная модель** (Instance Model)

## Типы моделей

### 1. Абсолютная модель (Absolute Model)

**Описание:** Действует самостоятельно и не привязана к конкретным объектам или устройствам.

**Применение:**
- Моделирование процессов или сервисов, которые не зависят от внешних данных
- Глобальные вычисления и агрегации
- Централизованная обработка данных

**Характеристики:**
- `type = 1` (абсолютная модель)
- `containerType = "objects"` (по умолчанию)
- `objectType = "object"` (по умолчанию)
- Привязки используют абсолютные пути к контекстам: `{users.admin.devices.device1:sine}`

**Пример создания:**
```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "absoluteModel",
    "description": "Абсолютная модель"
  }
}
```

**Пример привязки:**
```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.absoluteModel",
    "name": "bindings",
    "value": {
      "records": [{
        "target": ".:totalSum",
        "expression": "{users.admin.devices.device1:sine} + {users.admin.devices.device2:sine}",
        "onevent": true
      }]
    }
  }
}
```

### 2. Относительная модель (Relative Model)

**Описание:** Привязывается к объектам более низкого уровня (например, устройствам). Создаются многочисленные экземпляры модели, каждый для своего объекта.

**Применение:**
- Унификация обработки данных от различных устройств
- Обеспечение единообразия в представлении и анализе информации
- Модели, которые должны работать одинаково для каждого устройства

**Характеристики:**
- `type = 0` (относительная модель)
- `containerType = "devices"` (для привязки к устройствам)
- `objectType = "device"` (тип объекта)
- Привязки используют относительные ссылки на текущий объект: `{.:sine}`, `{.:sawtooth}`

**Пример создания:**
```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "relativeModel",
    "description": "Относительная модель"
  }
}
```

**Настройка типа модели:**
```json
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.relativeModel",
    "variableName": "childInfo",
    "fieldName": "containerType",
    "value": "devices"
  }
}
```

```json
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.relativeModel",
    "variableName": "childInfo",
    "fieldName": "objectType",
    "value": "device"
  }
}
```

**Пример привязки (ВАЖНО: используйте относительные ссылки!):**
```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.relativeModel",
    "name": "bindings",
    "value": {
      "records": [{
        "target": ".:wavesSum",
        "expression": "{.:sine} + {.:sawtooth}",
        "onevent": true
      }]
    }
  }
}
```

**Ключевое отличие:** В выражении привязки используется `{.:variableName}`, где `.:` ссылается на текущий объект (устройство), к которому привязан экземпляр модели.

### 3. Экземплярная модель (Instance Model)

**Описание:** Создается по требованию и может включать информацию о нескольких устройствах или процессах.

**Применение:**
- Комплексный анализ и управление сложными объектами или системами
- Модели, объединяющие данные от нескольких устройств
- Пример: модель "нефтевышка" содержит данные обо всех устройствах и контроллерах на вышке

**Характеристики:**
- `type = 2` (экземплярная модель)
- Может быть привязана к нескольким объектам
- Создается динамически по требованию

**Пример создания:**
```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "instanceModel",
    "description": "Экземплярная модель"
  }
}
```

## Сравнительная таблица

| Характеристика | Абсолютная | Относительная | Экземплярная |
|----------------|------------|---------------|--------------|
| **type** | 1 | 0 | 2 |
| **containerType** | "objects" | "devices" | зависит от задачи |
| **objectType** | "object" | "device" | зависит от задачи |
| **Привязки** | Абсолютные пути | Относительные ссылки `{.:var}` | Могут быть комбинированными |
| **Экземпляры** | Один | Много (по одному на объект) | Создаются по требованию |
| **Применение** | Глобальные вычисления | Унификация обработки | Комплексный анализ |

## Важные правила

### Для относительных моделей:

1. **ОБЯЗАТЕЛЬНО** установите:
   - `containerType = "devices"` (или другой тип контейнера)
   - `objectType = "device"` (или другой тип объекта)
   - **Эти параметры устанавливаются автоматически при создании модели через `aggregate_create_context` с `modelType=0`, `containerType` и `objectType`**

2. **ОБЯЗАТЕЛЬНО создайте переменную для хранения данных:**
   - Используйте `aggregate_create_variable` для создания переменной в модели
   - Переменная должна быть `writable: true` для записи данных через привязки

3. **ОБЯЗАТЕЛЬНО создайте привязку (bindings):**
   - Используйте `aggregate_set_variable` для установки переменной `bindings`
   - В привязках используйте относительные ссылки: `{.:variableName}`
   - ✅ ПРАВИЛЬНО: `{.:sine}`, `{.:sawtooth}`
   - ❌ НЕПРАВИЛЬНО: `{users.admin.devices.virtualDevice:sine}`
   - Формат привязки: `{"target": ".:variableName", "expression": "{.:sourceVar1} + {.:sourceVar2}", "onevent": true}`

4. **ОБЯЗАТЕЛЬНО установите выражение пригодности (validityExpression):**
   - Используйте `aggregate_set_variable_field` для установки поля `validityExpression` в переменной `childInfo`
   - Выражение определяет, для каких объектов должна создаваться модель
   - Пример: `"{.:sine} != null && {.:sawtooth} != null"` - модель создается только для устройств, у которых есть обе переменные

5. **Относительные ссылки:**
   - `{.:variableName}` - ссылается на переменную текущего объекта (устройства), к которому привязан экземпляр модели
   - Это позволяет модели работать одинаково для всех устройств

### Для абсолютных моделей:

1. **В привязках используйте абсолютные пути:**
   - ✅ ПРАВИЛЬНО: `{users.admin.devices.device1:sine}`
   - ❌ НЕПРАВИЛЬНО: `{.:sine}` (не будет работать)

## Примеры использования

### Пример 1: Относительная модель для суммы волн

**Задача:** Создать модель, которая для каждого виртуального устройства хранит сумму значений Sine Wave и Sawtooth Wave.

**Решение:**
```json
// 1. Создание относительной модели (параметры устанавливаются автоматически)
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "relativeWavesSum",
    "description": "Относительная модель суммы Sine+Sawtooth",
    "modelType": 0,
    "containerType": "devices",
    "objectType": "device"
  }
}

// 3. Создание переменной (ОБЯЗАТЕЛЬНО!)
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.relativeWavesSum",
    "variableName": "wavesSum",
    "format": "<sum><E>",
    "description": "Сумма Sine Wave и Sawtooth Wave",
    "writable": true
  }
}

// 4. Создание привязки (ОБЯЗАТЕЛЬНО! ВАЖНО: относительные ссылки!)
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.relativeWavesSum",
    "name": "bindings",
    "value": {
      "records": [{
        "target": ".:wavesSum",
        "expression": "{.:sine} + {.:sawtooth}",
        "onevent": true
      }],
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

// 5. Установка выражения пригодности (ОБЯЗАТЕЛЬНО для относительных моделей!)
// Выражение определяет, для каких объектов должна создаваться модель
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

### Пример 2: Абсолютная модель для кластера

**Задача:** Создать модель, которая агрегирует данные от нескольких устройств.

**Решение:**
```json
// 1. Создание модели (по умолчанию абсолютная)
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "cluster",
    "description": "Абсолютная модель кластера"
  }
}

// 2. Создание переменных
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.cluster",
    "variableName": "device1Sine",
    "format": "<value><E>",
    "writable": true
  }
}

// 3. Создание привязки (ВАЖНО: абсолютные пути!)
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.cluster",
    "name": "bindings",
    "value": {
      "records": [{
        "target": ".:device1Sine",
        "expression": "{users.admin.devices.device1:sine}",
        "onevent": true
      }]
    }
  }
}
```

## Проверка типа модели

Проверить тип модели можно через переменную `childInfo`:

```json
{
  "tool": "aggregate_get_variable",
  "parameters": {
    "path": "users.admin.models.relativeWavesSum",
    "name": "childInfo"
  }
}
```

**Поля для проверки:**
- `type` - тип модели (0=относительная, 1=абсолютная, 2=экземплярная)
- `containerType` - тип контейнера ("devices", "objects", и т.д.)
- `objectType` - тип объекта ("device", "object", и т.д.)

## Частые ошибки

### ❌ Ошибка 1: Использование абсолютных путей в относительной модели

**Неправильно:**
```json
{
  "expression": "{users.admin.devices.virtualDevice:sine} + {users.admin.devices.virtualDevice:sawtooth}"
}
```

**Правильно:**
```json
{
  "expression": "{.:sine} + {.:sawtooth}"
}
```

### ❌ Ошибка 2: Не установлен containerType и objectType для относительной модели

**Неправильно:** Создана модель без настройки типа

**Правильно:** При создании модели указать:
- `modelType = 0` (относительная модель)
- `containerType = "devices"`
- `objectType = "device"`

Эти параметры устанавливаются автоматически при создании через `aggregate_create_context`.

### ❌ Ошибка 3: Не создана переменная для записи данных

**Неправильно:** Создана модель без переменной для хранения данных

**Правильно:** После создания модели создать переменную:
```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.relativeModel",
    "variableName": "result",
    "format": "<result><E>",
    "writable": true
  }
}
```

### ❌ Ошибка 4: Не создана привязка (bindings)

**Неправильно:** Создана модель без привязки для записи данных

**Правильно:** После создания переменной создать привязку:
```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.relativeModel",
    "name": "bindings",
    "value": {
      "records": [{
        "target": ".:result",
        "expression": "{.:sourceVar1} + {.:sourceVar2}",
        "onevent": true
      }],
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

### ❌ Ошибка 5: Не установлено выражение пригодности (validityExpression)

**Неправильно:** Создана относительная модель без выражения пригодности

**Правильно:** Установить выражение пригодности для определения, для каких объектов создавать модель:
```json
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.relativeModel",
    "variableName": "childInfo",
    "fieldName": "validityExpression",
    "value": "{.:sourceVar1} != null && {.:sourceVar2} != null"
  }
}
```

### ❌ Ошибка 6: Использование относительных ссылок в абсолютной модели

**Неправильно:**
```json
{
  "expression": "{.:sine}"
}
```

**Правильно:**
```json
{
  "expression": "{users.admin.devices.device1:sine}"
}
```

## Резюме

- **Абсолютная модель**: один экземпляр, абсолютные пути в привязках
- **Относительная модель**: много экземпляров (по одному на объект), относительные ссылки `{.:var}` в привязках, требует настройки `containerType` и `objectType`
- **Экземплярная модель**: создается по требованию, может объединять несколько объектов
