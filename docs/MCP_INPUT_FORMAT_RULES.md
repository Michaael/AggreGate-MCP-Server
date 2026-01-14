# Правила inputFormat для Expression функций

## ⚠️ Критическое правило: Разные форматы для одного и множественных полей

### Проблема

AggreGate по-разному обрабатывает форматы inputFormat и outputFormat:
- Формат БЕЗ `<<>>`: `<value1><E><value2><E>` - **теряет поля после первого** при парсинге
- Формат С `<<>>`: `<<value1><E><value2><E>>` - **правильно парсит все поля**

### Решение

#### Для функций с ОДНИМ полем (БЕЗ <<>>):
```json
{
  "inputFormat": "<value><E>",
  "outputFormat": "<result><E>"
}
```

#### Для функций с НЕСКОЛЬКИМИ полями (С <<>>):
```json
{
  "inputFormat": "<<value1><E><value2><E>>",
  "outputFormat": "<<result><E>>"
}
```

## 🔍 Как определить проблему

### Симптомы:
1. Функция создается успешно
2. При тестировании ошибка: `Field 'value2' not found in data record: value1`
3. `aggregate_get_function` показывает только одно поле в inputFormat вместо нескольких

### Диагностика:
```python
# После создания функции
function_details = aggregate_get_function(path, functionName)
expected_fields = ["value1", "value2"]  # из aggregate_build_expression
actual_fields = [f['name'] for f in function_details.inputFields]

if len(actual_fields) < len(expected_fields):
    # ПРОБЛЕМА: формат потерял поля
    # Решение: пересоздать с форматом <<>>
```

## ✅ Правильный рабочий процесс

### Шаг 1: Определить количество полей
```python
inputFields = [
    {"name": "Int", "type": "E"},
    {"name": "Float", "type": "E"}
]

if len(inputFields) == 1:
    # Одно поле - без <<>>
    inputFormat = f"<{inputFields[0]['name']}><{inputFields[0]['type']}>"
elif len(inputFields) > 1:
    # Несколько полей - С <<>>
    fields_str = "".join([f"<{f['name']}><{f['type']}>" for f in inputFields])
    inputFormat = f"<<{fields_str}>>"
```

### Шаг 2: Создать функцию
```python
aggregate_create_function(
    path=path,
    functionName=functionName,
    functionType=1,
    inputFormat=inputFormat,  # <<Int><E><Float><E>> для множественных
    outputFormat=outputFormat,
    expression=expression
)
```

### Шаг 3: Проверить создание
```python
function_check = aggregate_get_function(path, functionName)
if len(function_check.inputFields) < len(inputFields):
    # Проблема - пересоздать с <<>>
    # ... пересоздание
```

### Шаг 4: Тестировать
```python
# Для функций с множественными полями используйте DataTable формат
if len(inputFields) > 1:
    result = aggregate_call_function(
        path=path,
        functionName=functionName,
        parameters={
            "records": [{"Int": 30, "Float": 30}],
            "format": {
                "fields": [
                    {"name": "Int", "type": "E"},
                    {"name": "Float", "type": "E"}
                ]
            }
        }
    )
else:
    result = aggregate_test_function(
        path=path,
        functionName=functionName,
        parameters={"value": 10}
    )
```

## 📋 Примеры

### Пример 1: Функция с одним полем
```json
{
  "inputFormat": "<value><E>",
  "outputFormat": "<result><E>",
  "expression": "table(\"<<result><E>>\", {value} * 2)"
}
```

### Пример 2: Функция с двумя полями
```json
{
  "inputFormat": "<<a><E><b><E>>",
  "outputFormat": "<<result><E>>",
  "expression": "table(\"<<result><E>>\", ({a} + {b}) / 2)"
}
```

### Пример 3: Функция с тремя полями
```json
{
  "inputFormat": "<<x><E><y><E><z><E>>",
  "outputFormat": "<<sum><E><avg><E>>",
  "expression": "table(\"<<sum><E><avg><E>>\", {x} + {y} + {z}, ({x} + {y} + {z}) / 3)"
}
```

## 🚨 Автоматическое исправление

Если обнаружена проблема с форматом:

```python
def fix_input_format(path, functionName, inputFields, outputFields, expression):
    # Определить правильный формат
    if len(inputFields) > 1:
        inputFormat = f"<<{''.join([f'<{f['name']}><{f['type']}>' for f in inputFields])}>>"
        outputFormat = f"<<{''.join([f'<{f['name']}><{f['type']}>' for f in outputFields])}>>"
    else:
        inputFormat = f"<{inputFields[0]['name']}><{inputFields[0]['type']}>"
        outputFormat = f"<{outputFields[0]['name']}><{outputFields[0]['type']}>"
    
    # Пересоздать функцию
    aggregate_create_function(
        path=path,
        functionName=functionName,
        functionType=1,
        inputFormat=inputFormat,
        outputFormat=outputFormat,
        expression=expression
    )
    
    # Проверить
    check = aggregate_get_function(path, functionName)
    assert len(check.inputFields) == len(inputFields), "Формат все еще неправильный!"
```

## ✅ Чек-лист

Перед созданием функции с множественными полями:

- [ ] Определено количество полей (одно или несколько)
- [ ] Если несколько полей → используется формат С `<<>>`
- [ ] Если одно поле → используется формат БЕЗ `<<>>`
- [ ] После создания проверено через `aggregate_get_function`
- [ ] Все поля присутствуют в inputFormat
- [ ] Функция протестирована (для множественных полей - через DataTable формат)

## 📝 Важные замечания

1. **aggregate_build_expression** возвращает формат БЕЗ `<<>>`, но для множественных полей нужно добавлять `<<>>` вручную
2. **aggregate_test_function** может не работать для функций с множественными полями - используйте `aggregate_call_function` с DataTable форматом
3. **aggregate_get_function** может показывать неправильный inputFormat - всегда проверяйте количество полей
