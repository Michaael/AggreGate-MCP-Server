# Дерево решений для выбора инструментов MCP AggreGate

## Установка значений переменных

```
Переменная создана?
│
├─ НЕТ → Используйте aggregate_create_variable сначала
│
└─ ДА → Проверьте maxRecords через aggregate_get_variable
    │
    ├─ maxRecords = 1 → ВСЕГДА используйте aggregate_set_variable_field
    │   │
    │   ├─ Одно поле → aggregate_set_variable_field (fieldName = variableName)
    │   │
    │   └─ Несколько полей → aggregate_set_variable_field для каждого поля отдельно
    │
    └─ maxRecords > 1 → Используйте aggregate_set_variable
        │
        └─ Установите все поля в одной записи через aggregate_set_variable
```

## Создание функций

```
Тип функции?
│
├─ Expression (functionType=1) → Простые вычисления
│   │
│   ├─ inputFormat: БЕЗ <<>> (например, <value1><E><value2><E>)
│   ├─ outputFormat: БЕЗ <<>> (например, <result><E>)
│   └─ expression: С <<>> (например, table("<<result><E>>", ...))
│
├─ Java (functionType=0) → Сложная логика
│   └─ Требует реализации на Java
│
└─ Query (functionType=2) → SQL запросы
    └─ Требует SQL запрос
```

## Работа с агентами

```
Создан агент?
│
├─ НЕТ → aggregate_create_agent
│
└─ ДА → Подождите 2-3 секунды
    │
    └─ Отправка события → aggregate_fire_event
        │
        ├─ Агент синхронизирован? → Да → Отправка успешна
        │
        └─ Нет → Инструмент ждет до 5 секунд автоматически
            │
            └─ Если не синхронизировался → Повторите через 2-3 секунды
```

## Проверка создания элементов

```
Операция создания выполнена?
│
├─ Успешно → Продолжайте работу
│
└─ Ошибка верификации → Проверьте через list функции
    │
    ├─ Элемент найден → Игнорируйте ошибку верификации
    │
    └─ Элемент не найден → Повторите создание через 1-2 секунды
```

## Выбор инструмента для установки значений

### Простые переменные (maxRecords=1)

**Формат:** `<name><T>` (одно поле, один тип)

**Примеры:**
- `<temperature><E>`
- `<status><S>`
- `<count><I>`

**Инструмент:** `aggregate_set_variable_field`

**Код:**
```json
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "...",
    "variableName": "temperature",
    "fieldName": "temperature",
    "value": 25.5
  }
}
```

### Переменные с несколькими полями в моделях

**Формат:** `<field1><T1><field2><T2>...` (несколько полей)

**⚠️ ВАЖНО:** В моделях такие переменные тоже имеют maxRecords=1

**Инструмент:** `aggregate_set_variable_field` для каждого поля отдельно

**Код:**
```json
// Поле 1
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "...",
    "variableName": "sensor_data",
    "fieldName": "temperature",
    "value": 25.5
  }
}

// Поле 2
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "...",
    "variableName": "sensor_data",
    "fieldName": "humidity",
    "value": 60.0
  }
}
```

### Переменные с maxRecords > 1

**Проверка:** Используйте `aggregate_get_variable` для проверки maxRecords

**Инструмент:** `aggregate_set_variable`

**Код:**
```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "...",
    "name": "sensor_data",
    "value": {
      "records": [
        {"temperature": 25.5, "humidity": 60.0}
      ]
    }
  }
}
```

---

**Версия:** 1.2  
**Дата:** 2025-12-15

