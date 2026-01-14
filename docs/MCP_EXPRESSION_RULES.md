# Правила работы с Expression функциями AggreGate через MCP

## ⚠️ Критические правила (встроены в инструменты)

### Правило 1: Форматы БЕЗ <<>>
**inputFormat и outputFormat НЕ должны содержать <<>> скобки!**

✅ **ПРАВИЛЬНО:**
```
inputFormat: "<value1><E><value2><E>"
outputFormat: "<result><E>"
```

❌ **НЕПРАВИЛЬНО:**
```
inputFormat: "<<value1><E><value2><E>>"  // ОШИБКА!
outputFormat: "<<result><E>>"            // ОШИБКА!
```

### Правило 2: Expression С <<>> внутри table()
**expression ДОЛЖЕН содержать <<>> скобки ВНУТРИ функции table()!**

✅ **ПРАВИЛЬНО:**
```
expression: "table(\"<<result><E>>\", ({value1} + {value2}) / 2)"
```

❌ **НЕПРАВИЛЬНО:**
```
expression: "table(\"<result><E>\", ({value1} + {value2}) / 2)"  // ОШИБКА - нет <<>>
```

## 🔧 Рекомендуемый рабочий процесс для ИИ

### Шаг 1: Построение правильных форматов
**ВСЕГДА используйте `aggregate_build_expression` перед созданием Expression функции!**

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
  "success": true,
  "inputFormat": "<value1><E><D=Первое значение><value2><E><D=Второе значение>",
  "outputFormat": "<result><E><D=Результат>",
  "expression": "table(\"<<result><E>>\", ({value1} + {value2}) / 2)",
  "usage": {
    "step1": "Use these values in aggregate_create_function:",
    "step2": "Set functionType to 1 (Expression)",
    "step3": "Use inputFormat and outputFormat AS-IS (they are correct, without <<>>)",
    "step4": "Use expression AS-IS (it already has <<>> inside table())",
    "warning": "DO NOT add <<>> to inputFormat or outputFormat - they are already correct!"
  }
}
```

### Шаг 2: Валидация перед созданием
**ВСЕГДА используйте `aggregate_validate_expression` перед `aggregate_create_function`!**

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

**Результат:**
```json
{
  "valid": true,
  "errors": [],
  "warnings": [],
  "suggestions": []
}
```

Если есть ошибки, инструмент покажет:
- Что именно неверно
- Правильный пример
- Конкретные рекомендации

### Шаг 3: Создание функции
**Только после успешной валидации вызывайте `aggregate_create_function`!**

```json
{
  "tool": "aggregate_create_function",
  "parameters": {
    "path": "users.admin.models.calculator",
    "functionName": "calculate_average",
    "functionType": 1,
    "inputFormat": "<value1><E><value2><E>",
    "outputFormat": "<result><E>",
    "expression": "table(\"<<result><E>>\", ({value1} + {value2}) / 2)",
    "description": "Вычисление среднего значения"
  }
}
```

## 🛡️ Автоматическая обработка ошибок

Если при создании функции возникает ошибка:

1. **Вызовите `aggregate_explain_error`:**
```json
{
  "tool": "aggregate_explain_error",
  "parameters": {
    "message": "Invalid inputFormat: <<value1><E>>",
    "toolName": "aggregate_create_function"
  }
}
```

2. **Инструмент вернёт:**
```json
{
  "category": "function_format",
  "explanation": "Неверный формат... inputFormat/outputFormat задаются БЕЗ <<>>",
  "detailedExplanation": "Обнаружены <<>> в inputFormat или outputFormat - это ОШИБКА!",
  "recommendation": "1. Используйте aggregate_build_expression... 2. Используйте aggregate_validate_expression..."
}
```

3. **Исправьте ошибку и повторите с шага 1**

## 📋 Типы полей TableFormat

- `S` - String (строка)
- `I` - Integer (целое число)
- `E` - Number/Double (число с плавающей точкой, рекомендуется)
- `B` - Boolean (логическое значение)
- `L` - Long (длинное целое)
- `T` - DataTable (таблица данных)

## ✅ Чек-лист для ИИ

Перед созданием Expression функции:

- [ ] Использовал `aggregate_build_expression` для генерации форматов
- [ ] Использовал `aggregate_validate_expression` для проверки
- [ ] Убедился, что `inputFormat` и `outputFormat` БЕЗ <<>>
- [ ] Убедился, что `expression` С <<>> внутри table()
- [ ] Все имена полей в expression соответствуют inputFormat
- [ ] Если ошибка - использовал `aggregate_explain_error` для диагностики

## 🎯 Пример полного цикла

```json
// 1. Построение
{"tool": "aggregate_build_expression", "parameters": {...}}

// 2. Валидация
{"tool": "aggregate_validate_expression", "parameters": {...}}

// 3. Создание
{"tool": "aggregate_create_function", "parameters": {...}}

// 4. Если ошибка - объяснение
{"tool": "aggregate_explain_error", "parameters": {...}}

// 5. Тестирование
{"tool": "aggregate_test_function", "parameters": {...}}
```

**Эти правила встроены в сами инструменты через описания и мета-информацию, поэтому ИИ не может их "забыть" - они всегда доступны через `aggregate_list_tools`!**
