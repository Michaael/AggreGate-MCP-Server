# Решение проблемы с inputFormat для множественных полей

## Проблема

При создании Expression функций с несколькими входными полями, inputFormat сохраняется неправильно - только с одним полем вместо нескольких.

## Причина

AggreGate по-разному обрабатывает форматы:
- Формат БЕЗ `<<>>`: `<Int><E><Float><E>` - парсится неправильно, теряет поля
- Формат С `<<>>`: `<<Int><E><Float><E>>` - парсится правильно, но требует DataTable формат для вызова

## Решение

### Правильный способ создания функции с множественными полями:

1. **Использовать формат С `<<>>` скобками:**
```json
{
  "inputFormat": "<<Int><E><Float><E>>",
  "outputFormat": "<<inRange><B><sum><E>>",
  "expression": "table(\"<<inRange><B><sum><E>>\", ({Int} + {Float} >= 50) && ({Int} + {Float} <= 100), {Int} + {Float})"
}
```

2. **Вызывать функцию через DataTable формат:**
```json
{
  "tool": "aggregate_call_function",
  "parameters": {
    "path": "users.admin.models.alarm_delayed_sum_v2",
    "functionName": "checkRangeCorrect",
    "parameters": {
      "records": [{"Int": 30, "Float": 30}],
      "format": {
        "fields": [
          {"name": "Int", "type": "E"},
          {"name": "Float", "type": "E"}
        ]
      }
    }
  }
}
```

### НЕПРАВИЛЬНО (формат без <<>>):

```json
{
  "inputFormat": "<Int><E><Float><E>",  // ❌ Теряет второе поле
  "outputFormat": "<inRange><B><sum><E>"
}
```

### ПРАВИЛЬНО (формат с <<>>):

```json
{
  "inputFormat": "<<Int><E><Float><E>>",  // ✅ Сохраняет все поля
  "outputFormat": "<<inRange><B><sum><E>>"
}
```

## Важное замечание

**Документация говорит использовать формат БЕЗ `<<>>`, но для множественных полей это НЕ работает!**

Для функций с одним полем - формат БЕЗ `<<>>` работает.
Для функций с несколькими полями - нужно использовать формат С `<<>>`.

## Обновление правил

Нужно обновить документацию:
- Для одного поля: `<value><E>` (без <<>>)
- Для нескольких полей: `<<value1><E><value2><E>>` (с <<>>)

## Проверка работы

Функция `checkRangeCorrect` работает правильно:
- Int=30, Float=30 (сумма=60) → inRange=true ✅
- Int=10, Float=20 (сумма=30) → inRange=false ✅  
- Int=60, Float=50 (сумма=110) → inRange=false ✅
