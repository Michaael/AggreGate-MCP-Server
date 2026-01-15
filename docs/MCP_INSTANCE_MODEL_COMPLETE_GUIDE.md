# Полное руководство по созданию экземплярных моделей

## ⚠️ КРИТИЧЕСКИ ВАЖНО для ИИ

При создании экземплярной модели необходимо выполнить **ВСЕ** следующие шаги:

1. ✅ Создание модели с правильными параметрами (`modelType=2` и `validityExpression`)
2. ✅ Создание переменной для хранения данных
3. ✅ Создание привязки (bindings) для записи данных
4. ✅ Создание экземпляров модели в контейнере `objects`

**Без выполнения всех шагов модель не будет работать!**

---

## Полный пример создания экземплярной модели

### Задача
Создать экземплярную модель, которая привязывается к контексту сервера (пустой путь `""`) и создает экземпляры в контейнере `objects`.

### Шаг 1: Создание модели

```json
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
```

**Важно:** 
- Параметр `modelType=2` указывает, что это экземплярная модель
- Параметр `containerType="objects"` определяет тип контейнера для экземпляров (по умолчанию "objects")
- Параметр `objectType="object"` определяет тип объектов экземпляров (по умолчанию "object")
- Параметр `validityExpression="{.:}==''"` привязывает модель к контексту сервера (пустой путь `""`)
- После установки `validityExpression` становится доступен контейнер `objects` для создания экземпляров
- **Все эти параметры настраиваются автоматически при создании модели через `aggregate_create_context`**

**Автоматически настраиваемые поля в `childInfo` (настраиваются при создании модели):**
- `containerType` - тип контейнера (по умолчанию "objects", можно указать "devices" и т.д.)
- `containerTypeDescription` - описание типа контейнера (автоматически: "Objects" для "objects", "Devices" для "devices")
- `containerName` - имя контейнера (по умолчанию "objects")
- `objectType` - тип объекта (по умолчанию "object", можно указать "device" и т.д.)
- `objectTypeDescription` - описание типа объекта (автоматически: "Object" для "object", "Device" для "device")
- `suitability` - контекст, от которого начинается дерево данных экземпляров (по умолчанию родительский контекст)
- `validityExpression` - выражение пригодности (если указано)

**Опциональные поля (можно настроить вручную через `aggregate_set_variable_field`):**
- `defaultContext` - контекст по умолчанию, используемый во всех внутренних выражениях (может быть NULL)
- `objectNamingExpression` - выражение для именования объектов экземплярной модели

### Шаг 2: Создание переменной (ОБЯЗАТЕЛЬНО!)

```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.test_instance_model",
    "variableName": "aggregatedData",
    "format": "<data><E>",
    "description": "Агрегированные данные экземпляра",
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

**Важно:**
- `target` - переменная в модели, куда записывается результат (формат: `.:variableName`)
- `expression` - выражение для вычисления значения
- `onevent` - обновлять значение при изменении исходных переменных (обычно `true`)

### Шаг 4: Создание экземпляра модели (ОБЯЗАТЕЛЬНО!)

После установки `validityExpression={.:}==''` становится доступен контейнер `objects` для создания экземпляров.

**ВАЖНО ПОНИМАТЬ СТРУКТУРУ:**
- `objects` - это **контейнер экземпляров**
- `objects.test_instance` - это **сам экземпляр модели**, а не контейнер для экземпляра
- Переменные модели доступны напрямую в экземпляре по пути `objects.test_instance`

```json
// Создание экземпляра модели в контейнере objects
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "objects",
    "name": "test_instance",
    "description": "Экземпляр модели test_instance_model"
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

---

## Чек-лист для ИИ

При создании экземплярной модели всегда проверяйте:

- [ ] Модель создана с `modelType=2`, `containerType`, `objectType` и `validityExpression`
- [ ] Все поля в `childInfo` настроены автоматически (containerType, containerTypeDescription, containerName, objectType, objectTypeDescription, suitability)
- [ ] Создана переменная для хранения данных (`writable: true`)
- [ ] Создана привязка (bindings)
- [ ] После установки `validityExpression` доступен контейнер `objects`
- [ ] Экземпляр создан в `objects` как `objects.{instance_name}`
- [ ] Понимание: `objects.test_instance` - это **сам экземпляр модели**, а не контейнер

---

## Частые ошибки

### ❌ Ошибка: Не установлено выражение пригодности (validityExpression)
**Симптом:** Контейнер `objects` не доступен, экземпляры не создаются
**Решение:** Установите `validityExpression` при создании модели или через `aggregate_set_variable_field` в `childInfo`

### ❌ Ошибка: Не создана переменная
**Симптом:** Модель создана, но данные не записываются
**Решение:** Создайте переменную через `aggregate_create_variable` с `writable: true`

### ❌ Ошибка: Не создана привязка
**Симптом:** Переменная создана, но значения не обновляются
**Решение:** Создайте привязку через `aggregate_set_variable` с переменной `bindings`

### ❌ Ошибка: Неправильное понимание структуры экземпляров
**Симптом:** Попытка доступа к переменной по пути `objects.test_instance.test_instance_model.aggregatedData`
**Решение:** Правильный путь: `objects.test_instance.aggregatedData` (экземпляр - это сам `objects.test_instance`)

### ❌ Ошибка: Попытка создать экземпляр до установки validityExpression
**Симптом:** Контейнер `objects` не доступен
**Решение:** Сначала установите `validityExpression`, затем создавайте экземпляры

---

## Дополнительные примеры

### Пример 1: Экземплярная модель с привязкой к контексту устройств

```json
// 1. Создание модели с validityExpression для контекста устройств
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "deviceInstanceModel",
    "description": "Экземплярная модель для устройств",
    "modelType": 2,
    "validityExpression": "{.:}==\"users.admin.devices\""
  }
}

// 2. Создание переменной
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.deviceInstanceModel",
    "variableName": "deviceData",
    "format": "<data><E>",
    "writable": true
  }
}

// 3. Создание привязки
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.deviceInstanceModel",
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
        "target": ".:deviceData",
        "expression": "{.:}",
        "onevent": true
      }]
    }
  }
}
```

### Пример 2: Установка validityExpression после создания модели

```json
// Если validityExpression не был указан при создании, можно установить его позже
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.test_instance_model",
    "variableName": "childInfo",
    "fieldName": "validityExpression",
    "value": "{.:}==''"
  }
}
```

### Пример 3: Настройка дополнительных полей для экземплярной модели

```json
// Настройка defaultContext (опционально)
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.test_instance_model",
    "variableName": "childInfo",
    "fieldName": "defaultContext",
    "value": "users.admin.devices"
  }
}

// Настройка objectNamingExpression (опционально)
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.test_instance_model",
    "variableName": "childInfo",
    "fieldName": "objectNamingExpression",
    "value": "{.:name} + '_instance'"
  }
}
```

---

## Резюме

**Для экземплярной модели ОБЯЗАТЕЛЬНО:**

1. ✅ Создать модель с `modelType=2` и `validityExpression`
2. ✅ Создать переменную (`writable: true`)
3. ✅ Создать привязку (bindings)
4. ✅ После установки `validityExpression` доступен контейнер `objects`
5. ✅ Создать экземпляр в `objects` как `objects.{instance_name}`
6. ✅ Понимать: `objects.test_instance` - это **сам экземпляр модели**, а не контейнер

**Без выполнения всех шагов модель не будет работать!**

---

## Ключевые моменты

### Структура экземпляров экземплярной модели:

```
objects                          <- контейнер экземпляров
  └── test_instance              <- сам экземпляр модели (НЕ контейнер!)
      └── aggregatedData         <- переменная модели (доступна напрямую)
```

**Правильные пути:**
- ✅ `objects.test_instance` - экземпляр модели
- ✅ `objects.test_instance.aggregatedData` - переменная в экземпляре

**Неправильные пути:**
- ❌ `objects.test_instance.test_instance_model` - такого пути не существует
- ❌ `objects.test_instance.test_instance_model.aggregatedData` - неправильный путь

### Выражения пригодности (validityExpression):

- `"{.:}==''"` - привязка к контексту сервера (пустой путь), экземпляры в `objects`
- `"{.:}==\"users.admin.devices\""` - привязка к контексту устройств
- `"{.:type} == 'device'"` - привязка к объектам определенного типа
