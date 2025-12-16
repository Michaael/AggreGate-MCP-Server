# Быстрая справка MCP AggreGate для AI моделей

## 🚀 Минимальный рабочий пример

```json
// 1. Подключение
{"tool": "aggregate_connect", "parameters": {"host": "localhost", "port": 6460, "username": "admin", "password": "admin"}}

// 2. Вход
{"tool": "aggregate_login"}

// 3. Создание модели
{"tool": "aggregate_create_context", "parameters": {"parentPath": "users.admin.models", "name": "my_model", "description": "Моя модель"}}

// 4. Создание переменной (ОБЯЗАТЕЛЬНО!)
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.my_model", "variableName": "status", "format": "<status><S>", "writable": true}}

// 5. Установка значения (используйте aggregate_set_variable_field!)
{"tool": "aggregate_set_variable_field", "parameters": {"path": "users.admin.models.my_model", "variableName": "status", "fieldName": "status", "value": "active"}}
```

## ⚠️ Критические правила

### 1. Порядок операций (ОБЯЗАТЕЛЬНО!)
```
aggregate_connect → aggregate_login → aggregate_create_context → 
aggregate_create_variable → aggregate_create_event → ...
```

### 2. Установка значений переменных
**❌ НЕПРАВИЛЬНО:**
```json
{"tool": "aggregate_set_variable", "parameters": {"path": "...", "name": "temp", "value": {...}}}
```

**✅ ПРАВИЛЬНО (для простых переменных):**
```json
{"tool": "aggregate_set_variable_field", "parameters": {
  "path": "users.admin.models.my_model",
  "variableName": "temperature",  // ⚠️ variableName, не name!
  "fieldName": "temperature",
  "value": 25.5
}}
```

### 3. Expression функции
**❌ НЕПРАВИЛЬНО:**
```json
{"inputFormat": "<<value1><E>>", "outputFormat": "<<result><E>>"}
```

**✅ ПРАВИЛЬНО:**
```json
{
  "inputFormat": "<value1><E><value2><E>",  // Без <<>>
  "outputFormat": "<result><E>",            // Без <<>>
  "expression": "table(\"<<result><E>>\", ({value1} + {value2}) / 2)"  // <<>> только здесь
}
```

## 📋 Форматы TableFormat

### Простые переменные:
- `<name><S>` - Строка
- `<name><I>` - Целое число
- `<name><E>` - Число (рекомендуется)
- `<name><B>` - Булево значение

### Переменные с несколькими полями:
- `<temp><E><humidity><E><timestamp><L>`

## 🔧 Часто используемые инструменты

### Создание переменной
```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.my_model",
    "variableName": "temperature",
    "format": "<temperature><E>",
    "writable": true
  }
}
```

### Установка значения (ПРАВИЛЬНЫЙ способ)
```json
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.my_model",
    "variableName": "temperature",
    "fieldName": "temperature",
    "value": 25.5
  }
}
```

### Создание Expression функции
```json
{
  "tool": "aggregate_create_function",
  "parameters": {
    "path": "users.admin.models.my_model",
    "functionName": "calculate",
    "functionType": 1,
    "inputFormat": "<a><E><b><E>",
    "outputFormat": "<result><E>",
    "expression": "table(\"<<result><E>>\", ({a} + {b}) / 2)"
  }
}
```

## ❌ Типичные ошибки

1. **"maximum number of records is reached"**
   → Используйте `aggregate_set_variable_field` вместо `aggregate_set_variable`

2. **"Path, variableName, fieldName, and value parameters are required"**
   → Используйте `variableName` (не `name`) в `aggregate_set_variable_field`

3. **"Invalid inputFormat: null"**
   → Укажите `inputFormat` явно для Expression функций

4. **"verification failed"**
   → Проверьте через `aggregate_list_variables` - элемент может быть создан

## 📖 Полная документация

См. `docs/MCP_AI_MODEL_GUIDE.md` для полной документации всех функций.

