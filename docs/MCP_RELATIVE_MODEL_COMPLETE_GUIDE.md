# Полное руководство по созданию относительных моделей

## ⚠️ КРИТИЧЕСКИ ВАЖНО для ИИ

При создании относительной модели необходимо выполнить **ВСЕ** следующие шаги:

1. ✅ Создание модели с правильными параметрами
2. ✅ Создание переменной для хранения данных
3. ✅ Создание привязки (bindings) для записи данных
4. ✅ Установка выражения пригодности (validityExpression)

**Без выполнения всех шагов модель не будет работать!**

---

## Полный пример создания относительной модели

### Задача
Создать относительную модель, которая для каждого виртуального устройства хранит сумму значений Sine Wave и Sawtooth Wave.

### Шаг 1: Создание модели

```json
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
```

**Важно:** Параметры `modelType`, `containerType` и `objectType` устанавливаются автоматически при создании модели.

### Шаг 2: Создание переменной (ОБЯЗАТЕЛЬНО!)

```json
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
```

**Важно:** 
- Переменная должна быть `writable: true` для записи данных через привязки
- Формат переменной должен соответствовать типу данных, которые будут записываться

### Шаг 3: Создание привязки (ОБЯЗАТЕЛЬНО!)

```json
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

**Важно:**
- `target` - переменная в модели, куда записывается результат (формат: `.:variableName`)
- `expression` - выражение для вычисления значения (используйте относительные ссылки: `{.:variableName}`)
- `onevent` - обновлять значение при изменении исходных переменных (обычно `true`)
- **В относительных моделях ВСЕГДА используйте относительные ссылки `{.:variableName}`, а не абсолютные пути!**

### Шаг 4: Установка выражения пригодности (ОБЯЗАТЕЛЬНО!)

**Способ 1: Указать при создании модели (рекомендуется)**
```json
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
```

**Способ 2: Установить после создания модели**
```json
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

**Важно:**
- Выражение пригодности определяет, для каких объектов (устройств) должна создаваться модель
- Модель создается только для объектов, для которых выражение возвращает `true`
- Используйте относительные ссылки `{.:variableName}` в выражении
- Типичные выражения:
  - `"{.:variableName} != null"` - модель создается для объектов с определенной переменной
  - `"{.:variableName1} != null && {.:variableName2} != null"` - модель создается для объектов с обеими переменными
  - `"{.:status} == 'active'"` - модель создается только для активных объектов
  - `"{.:}==\"users.admin.devices.test_device\""` - модель создается только для конкретного устройства (путь устройства)

---

## Чек-лист для ИИ

При создании относительной модели всегда проверяйте:

- [ ] Модель создана с `modelType=0`, `containerType` и `objectType`
- [ ] Создана переменная для хранения данных (`writable: true`)
- [ ] Создана привязка (bindings) с относительными ссылками `{.:variableName}`
- [ ] Установлено выражение пригодности (validityExpression) в `childInfo`

---

## Частые ошибки

### ❌ Ошибка: Не создана переменная
**Симптом:** Модель создана, но данные не записываются
**Решение:** Создайте переменную через `aggregate_create_variable` с `writable: true`

### ❌ Ошибка: Не создана привязка
**Симптом:** Переменная создана, но значения не обновляются
**Решение:** Создайте привязку через `aggregate_set_variable` с переменной `bindings`

### ❌ Ошибка: Не установлено выражение пригодности
**Симптом:** Модель не создается для устройств
**Решение:** Установите `validityExpression` через `aggregate_set_variable_field` в `childInfo`

### ❌ Ошибка: Использование абсолютных путей в привязках
**Симптом:** Привязка не работает или работает только для одного устройства
**Решение:** Используйте относительные ссылки `{.:variableName}` вместо абсолютных путей

---

## Дополнительные примеры

### Пример 1: Модель с несколькими переменными

```json
// 1. Создание модели
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "deviceMetrics",
    "modelType": 0,
    "containerType": "devices",
    "objectType": "device"
  }
}

// 2. Создание переменных
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.deviceMetrics",
    "variableName": "total",
    "format": "<total><E>",
    "writable": true
  }
}

{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.deviceMetrics",
    "variableName": "average",
    "format": "<average><E>",
    "writable": true
  }
}

// 3. Создание привязок
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.deviceMetrics",
    "name": "bindings",
    "value": {
      "records": [
        {
          "target": ".:total",
          "expression": "{.:value1} + {.:value2} + {.:value3}",
          "onevent": true
        },
        {
          "target": ".:average",
          "expression": "({.:value1} + {.:value2} + {.:value3}) / 3",
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

// 4. Установка выражения пригодности
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.deviceMetrics",
    "variableName": "childInfo",
    "fieldName": "validityExpression",
    "value": "{.:value1} != null && {.:value2} != null && {.:value3} != null"
  }
}
```

### Пример 2: Модель с условием в выражении пригодности

```json
// Выражение пригодности: модель создается только для активных устройств
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.activeDevices",
    "variableName": "childInfo",
    "fieldName": "validityExpression",
    "value": "{.:status} == 'active' && {.:enabled} == true"
  }
}
```

### Пример 3: Относительная модель с привязкой к конкретному устройству

**Задача:** Создать относительную модель, которая привязывается только к конкретному устройству `test_device`.

```json
// 1. Создание модели с validityExpression для конкретного устройства
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

---

## Резюме

**Для относительной модели ОБЯЗАТЕЛЬНО:**

1. ✅ Создать модель с `modelType=0`, `containerType`, `objectType`
2. ✅ Создать переменную (`writable: true`)
3. ✅ Создать привязку с относительными ссылками `{.:var}`
4. ✅ Установить выражение пригодности в `childInfo.validityExpression`

**Без выполнения всех шагов модель не будет работать!**
