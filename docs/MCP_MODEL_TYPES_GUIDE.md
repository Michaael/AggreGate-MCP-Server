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
- Требует настройки `validityExpression` для определения условий создания экземпляров
- После установки `validityExpression` становится доступен контейнер `objects` для экземпляров

**Пример создания:**
```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "instanceModel",
    "description": "Экземплярная модель",
    "modelType": 2,
    "containerType": "objects",
    "objectType": "object",
    "validityExpression": "{.:}==''"
  }
}
```

**Важно:** 
- Параметры `containerType` и `objectType` устанавливаются автоматически при создании модели (по умолчанию "objects" и "object")
- Параметр `validityExpression` устанавливается автоматически при создании модели через `aggregate_create_context` с `modelType=2` и `validityExpression`
- **Все необходимые поля в `childInfo` настраиваются автоматически:**
  - `containerType` - тип контейнера (по умолчанию "objects")
  - `containerTypeDescription` - описание типа контейнера (по умолчанию "Objects")
  - `containerName` - имя контейнера (по умолчанию "objects")
  - `objectType` - тип объекта (по умолчанию "object")
  - `objectTypeDescription` - описание типа объекта (по умолчанию "Object")
  - `suitability` - контекст, от которого начинается дерево данных экземпляров

**Пример привязки для экземплярной модели:**
```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.instanceModel",
    "name": "bindings",
    "value": {
      "format": {
        "fields": [
          {"name": "target", "type": "S"},
          {"name": "expression", "type": "S"},
          {"name": "onevent", "type": "B"}
        ]
      },
      "records": [{
        "target": ".:aggregatedData",
        "expression": "{.:}",
        "onevent": true
      }]
    }
  }
}
```

**Работа с экземплярами экземплярной модели:**

После установки `validityExpression` становится доступен контейнер `objects` для создания экземпляров модели.

**Важно понимать:**
- `objects` - это контейнер экземпляров
- `objects.test_instance` - это **сам экземпляр модели**, а не контейнер для экземпляра
- Переменные модели доступны напрямую в экземпляре по пути `objects.test_instance`

**Пример создания экземпляра:**
```json
// 1. Создание экземпляра в контейнере objects
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "objects",
    "name": "test_instance",
    "description": "Экземпляр модели instanceModel"
  }
}
```

**Пример доступа к переменной в экземпляре:**
```json
{
  "tool": "aggregate_get_variable",
  "parameters": {
    "path": "objects.test_instance",
    "name": "aggregatedData"
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
   - Или укажите `validityExpression` при создании модели через `aggregate_create_context`
   - Выражение определяет, для каких объектов должна создаваться модель
   - Примеры выражений:
     - `"{.:sine} != null && {.:sawtooth} != null"` - модель создается только для устройств, у которых есть обе переменные
     - `"{.:}==\"users.admin.devices.test_device\""` - модель создается только для конкретного устройства (путь устройства)
     - `"{.:status} == 'active'"` - модель создается только для активных устройств

5. **Относительные ссылки:**
   - `{.:variableName}` - ссылается на переменную текущего объекта (устройства), к которому привязан экземпляр модели
   - Это позволяет модели работать одинаково для всех устройств

### Для абсолютных моделей:

1. **В привязках используйте абсолютные пути:**
   - ✅ ПРАВИЛЬНО: `{users.admin.devices.device1:sine}`
   - ❌ НЕПРАВИЛЬНО: `{.:sine}` (не будет работать)

### Для экземплярных моделей:

1. **ОБЯЗАТЕЛЬНО укажите параметры при создании модели:**
   - `modelType=2` - указывает, что это экземплярная модель
   - `containerType="objects"` - тип контейнера (по умолчанию "objects", можно указать "devices" и т.д.)
   - `objectType="object"` - тип объекта (по умолчанию "object", можно указать "device" и т.д.)
   - `validityExpression` - выражение пригодности (рекомендуется)
   - **Все поля в `childInfo` настраиваются автоматически при создании модели:**
     - `containerType`, `containerTypeDescription`, `containerName`
     - `objectType`, `objectTypeDescription`
     - `suitability` (контекст, от которого начинается дерево данных экземпляров)
     - `validityExpression` (если указано)

2. **ОБЯЗАТЕЛЬНО установите выражение пригодности (validityExpression):**
   - Укажите `validityExpression` при создании модели через `aggregate_create_context`
   - Или используйте `aggregate_set_variable_field` для установки поля `validityExpression` в переменной `childInfo`
   - Выражение определяет, для каких контекстов должны создаваться экземпляры модели
   - Примеры выражений:
     - `"{.:}==''"` - привязка к контексту сервера (пустой путь), экземпляры создаются в `objects`
     - `"{.:}==\"users.admin.devices\""` - привязка к контексту устройств

3. **ОБЯЗАТЕЛЬНО создайте переменную для хранения данных:**
   - Используйте `aggregate_create_variable` для создания переменной в модели
   - Переменная должна быть `writable: true` для записи данных через привязки

4. **ОБЯЗАТЕЛЬНО создайте привязку (bindings):**
   - Используйте `aggregate_set_variable` для установки переменной `bindings`
   - В привязках можно использовать относительные ссылки `{.:variableName}` или абсолютные пути

5. **Работа с экземплярами:**
   - После установки `validityExpression` становится доступен контейнер `objects`
   - Экземпляры создаются в `objects` как `objects.{instance_name}`
   - **ВАЖНО:** `objects.test_instance` - это **сам экземпляр модели**, а не контейнер
   - Переменные модели доступны напрямую в экземпляре по пути `objects.test_instance`

## Примеры использования

### Пример 1: Относительная модель для суммы волн

**Задача:** Создать модель, которая для каждого виртуального устройства хранит сумму значений Sine Wave и Sawtooth Wave.

**Решение:**
```json
// 1. Создание относительной модели с validityExpression (параметры устанавливаются автоматически)
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "relativeWavesSum",
    "description": "Относительная модель суммы Sine+Sawtooth",
    "modelType": 0,
    "containerType": "devices",
    "objectType": "device",
    "validityExpression": "{.:sine} != null && {.:sawtooth} != null"
  }
}

// 2. Создание переменной (ОБЯЗАТЕЛЬНО!)
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

// 3. Создание привязки (ОБЯЗАТЕЛЬНО! ВАЖНО: относительные ссылки!)
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
```

**Альтернативный способ установки validityExpression после создания модели:**
```json
// Если validityExpression не был указан при создании, можно установить его позже
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

### Пример 1.1: Относительная модель с привязкой к конкретному устройству

**Задача:** Создать относительную модель, которая привязывается только к конкретному устройству `test_device`.

**Решение:**
```json
// 1. Создание относительной модели с validityExpression для конкретного устройства
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "test_relative_model",
    "description": "Относительная модель для тестового устройства",
    "modelType": 0,
    "containerType": "devices",
    "objectType": "device",
    "validityExpression": "{.:}==\"users.admin.devices.test_device\""
  }
}

// 2. Создание переменной
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.test_relative_model",
    "variableName": "sumValue",
    "format": "<sumValue><E>",
    "writable": true
  }
}

// 3. Создание привязки с относительными ссылками
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.test_relative_model",
    "name": "bindings",
    "value": {
      "format": {
        "fields": [
          {"name": "target", "type": "S"},
          {"name": "expression", "type": "S"},
          {"name": "onevent", "type": "B"}
        ]
      },
      "records": [{
        "target": ".:sumValue",
        "expression": "{.:}",
        "onevent": true
      }]
    }
  }
}
```

**Проверка экземпляра модели на устройстве:**
```json
// Путь к экземпляру модели на устройстве: users.admin.devices.test_device.test_relative_model
{
  "tool": "aggregate_get_context",
  "parameters": {
    "path": "users.admin.devices.test_device.test_relative_model"
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

### Пример 3: Экземплярная модель с привязкой к контексту сервера

**Задача:** Создать экземплярную модель, которая привязывается к контексту сервера (пустой путь `""`) и создает экземпляры в контейнере `objects`.

**Решение:**
```json
// 1. Создание экземплярной модели с validityExpression и настройкой полей
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "test_instance_model",
    "description": "Экземплярная модель для тестирования",
    "modelType": 2,
    "containerType": "objects",
    "objectType": "object",
    "validityExpression": "{.:}==''"
  }
}

// 2. Создание переменной (ОБЯЗАТЕЛЬНО!)
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.test_instance_model",
    "variableName": "aggregatedData",
    "format": "<data><E>",
    "writable": true
  }
}

// 3. Создание привязки для экземплярной модели
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.test_instance_model",
    "name": "bindings",
    "value": {
      "format": {
        "fields": [
          {"name": "target", "type": "S"},
          {"name": "expression", "type": "S"},
          {"name": "onevent", "type": "B"}
        ]
      },
      "records": [{
        "target": ".:aggregatedData",
        "expression": "{.:}",
        "onevent": true
      }]
    }
  }
}
```

**Создание экземпляра модели:**
```json
// После установки validityExpression={.:}=='' становится доступен контейнер objects
// Создаем экземпляр модели в контейнере objects
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "objects",
    "name": "test_instance",
    "description": "Экземпляр модели test_instance_model"
  }
}
```

**Важно понимать структуру:**
- `objects` - контейнер экземпляров
- `objects.test_instance` - **сам экземпляр модели**, а не контейнер для экземпляра
- Переменные модели доступны напрямую в экземпляре

**Доступ к переменной в экземпляре:**
```json
{
  "tool": "aggregate_get_variable",
  "parameters": {
    "path": "objects.test_instance",
    "name": "aggregatedData"
  }
}
```

**Проверка существования экземпляра:**
```json
{
  "tool": "aggregate_get_context",
  "parameters": {
    "path": "objects.test_instance"
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
- **Относительная модель**: много экземпляров (по одному на объект), относительные ссылки `{.:var}` в привязках, требует настройки `containerType`, `objectType` и `validityExpression`
- **Экземплярная модель**: создается по требованию, может объединять несколько объектов, требует настройки `validityExpression` для определения условий создания экземпляров

## Ключевые моменты для работы с моделями

### Относительные модели (type=0):
1. ✅ Укажите `modelType=0`, `containerType`, `objectType` при создании
2. ✅ Установите `validityExpression` для определения, для каких объектов создавать модель
3. ✅ Используйте относительные ссылки `{.:variableName}` в привязках
4. ✅ Экземпляры создаются автоматически на устройствах, соответствующих `validityExpression`

### Экземплярные модели (type=2):
1. ✅ Укажите `modelType=2` и `validityExpression` при создании
2. ✅ После установки `validityExpression={.:}==''` становится доступен контейнер `objects`
3. ✅ Экземпляры создаются в `objects` как `objects.{instance_name}`
4. ✅ **ВАЖНО:** `objects.test_instance` - это **сам экземпляр модели**, а не контейнер
5. ✅ Переменные модели доступны напрямую в экземпляре по пути `objects.test_instance`
