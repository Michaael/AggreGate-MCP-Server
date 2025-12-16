# Сценарии использования MCP сервера AggreGate

## Содержание
1. [Базовые сценарии](#базовые-сценарии)
2. [Работа с переменными](#работа-с-переменными)
3. [Работа с функциями](#работа-с-функциями)
4. [Работа с событиями](#работа-с-событиями)
5. [Работа с агентами](#работа-с-агентами)
6. [Полные примеры](#полные-примеры)

---

## Базовые сценарии

### Сценарий 1: Создание простой модели с переменными

**Цель:** Создать модель с простыми переменными и установить их значения.

**Шаги:**
1. Подключение и вход
2. Создание модели
3. Создание переменных
4. Установка значений

**Код:**
```json
// 1. Подключение
{"tool": "aggregate_connect", "parameters": {"host": "localhost", "port": 6460, "username": "admin", "password": "admin"}}

// 2. Вход
{"tool": "aggregate_login"}

// 3. Создание модели
{"tool": "aggregate_create_context", "parameters": {"parentPath": "users.admin.models", "name": "sensor", "description": "Датчик"}}

// 4. Создание переменных (ОБЯЗАТЕЛЬНО!)
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.sensor", "variableName": "temperature", "format": "<temperature><E>", "writable": true}}
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.sensor", "variableName": "status", "format": "<status><S>", "writable": true}}

// 5. Установка значений (ИСПОЛЬЗУЕМ aggregate_set_variable_field для простых переменных!)
{"tool": "aggregate_set_variable_field", "parameters": {"path": "users.admin.models.sensor", "variableName": "temperature", "fieldName": "temperature", "value": 25.5}}
{"tool": "aggregate_set_variable_field", "parameters": {"path": "users.admin.models.sensor", "variableName": "status", "fieldName": "status", "value": "active"}}
```

**⚠️ Важно:** Для простых переменных (maxRecords=1) ВСЕГДА используйте `aggregate_set_variable_field`.

---

## Работа с переменными

### Сценарий 2: Простая переменная (maxRecords=1)

**Когда использовать:** Переменная с одним полем, например `<temperature><E>`, `<status><S>`.

**Инструмент:** `aggregate_set_variable_field`

**Пример:**
```json
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.sensor",
    "variableName": "temperature",
    "fieldName": "temperature",
    "value": 25.5
  }
}
```

**Правила:**
- ✅ Используйте `variableName` (не `name`)
- ✅ `fieldName` обычно совпадает с `variableName`
- ✅ Это единственный правильный способ для простых переменных

### Сценарий 3: Переменная с несколькими полями

**Когда использовать:** Переменная с несколькими полями, например `<temperature><E><humidity><E><timestamp><L>`.

**⚠️ ВАЖНО: В моделях AggreGate переменные обычно имеют maxRecords=1, даже с несколькими полями.**

**Инструмент:** `aggregate_set_variable_field` (для каждого поля отдельно)

**Пример (правильный способ для моделей):**
```json
// Устанавливаем каждое поле отдельно
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.sensor",
    "variableName": "sensor_data",
    "fieldName": "temperature",
    "value": 25.5
  }
}

{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.sensor",
    "variableName": "sensor_data",
    "fieldName": "humidity",
    "value": 60.0
  }
}

{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.sensor",
    "variableName": "sensor_data",
    "fieldName": "timestamp",
    "value": 1234567890
  }
}
```

**Правила:**
- ✅ **В моделях используйте `aggregate_set_variable_field` для каждого поля отдельно**
- ✅ Используйте `aggregate_set_variable` только если переменная имеет maxRecords > 1 (проверьте через `aggregate_get_variable`)
- ⚠️ В моделях переменные создаются с maxRecords=1 по умолчанию

---

## Работа с функциями

### Сценарий 4: Создание Expression функции

**Цель:** Создать функцию для вычислений.

**Шаги:**
1. Создание функции
2. Проверка создания (через list_functions)
3. Вызов функции

**Код:**
```json
// 1. Создание Expression функции
{
  "tool": "aggregate_create_function",
  "parameters": {
    "path": "users.admin.models.calculator",
    "functionName": "calculate_average",
    "functionType": 1,
    "description": "Вычисление среднего",
    "inputFormat": "<value1><E><value2><E>",
    "outputFormat": "<result><E>",
    "expression": "table(\"<<result><E>>\", ({value1} + {value2}) / 2)"
  }
}

// 2. Проверка создания (может быть ошибка верификации, но функция создана)
{"tool": "aggregate_list_functions", "parameters": {"path": "users.admin.models.calculator"}}

// 3. Вызов функции
{
  "tool": "aggregate_call_function",
  "parameters": {
    "path": "users.admin.models.calculator",
    "functionName": "calculate_average",
    "parameters": {
      "records": [
        {"value1": 10.5, "value2": 20.3}
      ]
    }
  }
}
```

**⚠️ Важно:**
- `inputFormat` и `outputFormat` - БЕЗ `<<>>` (например, `<value1><E><value2><E>`)
- Двойные скобки `<<>>` только внутри `expression`
- Проверяйте создание через `aggregate_list_functions` даже при ошибке верификации

---

## Работа с событиями

### Сценарий 5: Создание события в модели

**Цель:** Создать событие для модели.

**Код:**
```json
// Простое событие
{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.sensor",
    "eventName": "status_changed",
    "description": "Изменение статуса",
    "level": 0
  }
}

// Событие с параметрами
{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.sensor",
    "eventName": "temperature_alert",
    "format": "<temperature><E><threshold><E>",
    "description": "Предупреждение о температуре",
    "level": 1
  }
}
```

**Правила:**
- ✅ Создавайте события после создания переменных
- ✅ Проверяйте создание через list функций, если была ошибка верификации

---

## Работа с агентами

### Сценарий 6: Создание и использование агента

**Цель:** Создать агента и отправить событие.

**Код:**
```json
// 1. Создание агента
{
  "tool": "aggregate_create_agent",
  "parameters": {
    "agentName": "my_agent",
    "host": "localhost",
    "port": 6460,
    "username": "admin",
    "password": "admin"
  }
}

// 2. ОЖИДАНИЕ синхронизации (минимум 2-3 секунды)
// В коде: time.sleep(3)

// 3. Отправка события
{
  "tool": "aggregate_fire_event",
  "parameters": {
    "agentName": "my_agent",
    "eventName": "test_event",
    "level": "INFO",
    "data": {
      "records": [
        {"message": "Test event"}
      ]
    }
  }
}
```

**⚠️ Важно:**
- ✅ Добавляйте задержку 2-3 секунды после создания агента
- ✅ Инструмент автоматически ждет синхронизации до 5 секунд
- ✅ Если агент не синхронизировался, повторите попытку через 2 секунды

---

## Полные примеры

### Пример 1: Полная модель "Умный дом"

```json
// 1. Подключение
{"tool": "aggregate_connect", "parameters": {"host": "localhost", "port": 6460, "username": "admin", "password": "admin"}}
{"tool": "aggregate_login"}

// 2. Создание модели
{"tool": "aggregate_create_context", "parameters": {"parentPath": "users.admin.models", "name": "smart_home", "description": "Умный дом"}}

// 3. Создание переменных
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.smart_home", "variableName": "temperature", "format": "<temperature><E>", "writable": true}}
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.smart_home", "variableName": "humidity", "format": "<humidity><E>", "writable": true}}
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.smart_home", "variableName": "sensor_data", "format": "<temperature><E><humidity><E><timestamp><L>", "writable": true}}

// 4. Создание событий
{"tool": "aggregate_create_event", "parameters": {"path": "users.admin.models.smart_home", "eventName": "temperature_alert", "format": "<temperature><E><threshold><E>", "level": 1}}

// 5. Создание функции
{
  "tool": "aggregate_create_function",
  "parameters": {
    "path": "users.admin.models.smart_home",
    "functionName": "calculate_average",
    "functionType": 1,
    "inputFormat": "<value1><E><value2><E>",
    "outputFormat": "<result><E>",
    "expression": "table(\"<<result><E>>\", ({value1} + {value2}) / 2)"
  }
}

// 6. Установка значений (ПРАВИЛЬНЫЙ способ)
{"tool": "aggregate_set_variable_field", "parameters": {"path": "users.admin.models.smart_home", "variableName": "temperature", "fieldName": "temperature", "value": 22.5}}
{"tool": "aggregate_set_variable_field", "parameters": {"path": "users.admin.models.smart_home", "variableName": "humidity", "fieldName": "humidity", "value": 45.0}}

// Для переменной с несколькими полями используем aggregate_set_variable
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.smart_home",
    "name": "sensor_data",
    "value": {
      "records": [
        {"temperature": 22.5, "humidity": 45.0, "timestamp": 1234567890}
      ]
    }
  }
}
```

---

## Чек-лист выбора инструментов

### Установка значений переменных

| Тип переменной | Инструмент | Пример | Примечание |
|----------------|------------|--------|------------|
| Простая (maxRecords=1) | `aggregate_set_variable_field` | `<temperature><E>` | ВСЕГДА для простых переменных |
| Несколько полей в модели | `aggregate_set_variable_field` (для каждого поля) | `<temp><E><humidity><E>` | В моделях переменные имеют maxRecords=1 |
| Несколько записей (maxRecords > 1) | `aggregate_set_variable` | Проверьте через get_variable | Только если maxRecords > 1 |

### Создание функций

| Тип функции | functionType | Когда использовать |
|-------------|--------------|-------------------|
| Expression | 1 | Простые вычисления |
| Java | 0 | Сложная логика |
| Query | 2 | SQL запросы |

### Работа с агентами

| Операция | Задержка | Примечание |
|----------|----------|------------|
| Создание агента | 2-3 секунды | После создания |
| Отправка события | Автоматически | Инструмент ждет до 5 секунд |

---

**Версия:** 1.2  
**Дата:** 2025-12-15

