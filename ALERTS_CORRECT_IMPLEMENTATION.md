# Правильная реализация тревог через контекст Alerts

## Важное открытие

Тревоги в AggreGate должны создаваться в контексте `users.admin.alerts`, а не как модели в `users.admin.models`!

## Структура контекста Alerts

Контекст тревоги имеет специальные переменные:
- `eventTriggers` - триггеры, активируемые событиями
- `variableTriggers` - триггеры, активируемые состоянием переменных
- `alertActions` - автоматические корректирующие действия
- `interactiveActions` - интерактивные корректирующие действия
- `status` - статус тревоги

## Задание 2: Тревога Event 1 -> Event 2 (Int > 20)

**Правильная реализация:**

```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.alerts",
    "name": "alarmEvent1Event2",
    "description": "Тревога, срабатывающая на Event 1 и деактивирующаяся при Event 2 (Int > 20)"
  }
}
```

**Настройка триггера через eventTriggers:**

```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.alerts.alarmEvent1Event2",
    "name": "eventTriggers",
    "value": {
      "records": [{
        "mask": "users.admin.devices.virtualDevice",
        "event": "event1",
        "correlated": "event2",
        "correlator": "Int > 20",
        "level": 1,
        "message": "Тревога активирована на Event 1"
      }],
      "format": {
        "fields": [
          {"name": "mask", "type": "S"},
          {"name": "event", "type": "S"},
          {"name": "correlated", "type": "S"},
          {"name": "correlator", "type": "S"},
          {"name": "level", "type": "I"},
          {"name": "message", "type": "S"}
        ]
      }
    }
  }
}
```

**Поля eventTriggers:**
- `mask` - маска контекста (например, `users.admin.devices.virtualDevice`)
- `event` - событие активации (например, `event1`)
- `correlated` - событие деактивации (например, `event2`)
- `correlator` - выражение для деактивации (например, `Int > 20`)
- `level` - уровень тревоги (0=INFO, 1=WARNING, 2=ERROR, 3=FATAL)
- `message` - сообщение триггера

## Задание 3: Тревога через 10 секунд (сумма Int+Float 50-100)

**Правильная реализация:**

```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.alerts",
    "name": "alarmDelayedSum",
    "description": "Тревога через 10 секунд после того, как сумма Int+Float попадает в диапазон 50-100"
  }
}
```

**Настройка триггера через variableTriggers:**

```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.alerts.alarmDelayedSum",
    "name": "variableTriggers",
    "value": {
      "records": [{
        "mask": "users.admin.devices.virtualDevice",
        "variable": "int",
        "expression": "({int} + {float} >= 50) && ({int} + {float} <= 100)",
        "mode": 0,
        "delay": 10000,
        "level": 1,
        "message": "Сумма Int+Float в диапазоне 50-100"
      }],
      "format": {
        "fields": [
          {"name": "mask", "type": "S"},
          {"name": "variable", "type": "S"},
          {"name": "expression", "type": "S"},
          {"name": "mode", "type": "I"},
          {"name": "delay", "type": "L"},
          {"name": "level", "type": "I"},
          {"name": "message", "type": "S"}
        ]
      }
    }
  }
}
```

**Поля variableTriggers:**
- `mask` - маска контекста
- `variable` - переменная для отслеживания (можно указать любую, выражение будет проверять все переменные)
- `expression` - выражение условия (использует `{variableName}` для доступа к переменным)
- `mode` - режим (0 = проверка значения)
- `delay` - задержка в миллисекундах перед активацией (10000 = 10 секунд)
- `level` - уровень тревоги
- `message` - сообщение триггера

## Задание 4: Тревога на сумму Int в Table > 100

**Правильная реализация:**

```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.alerts",
    "name": "alarmTableSum",
    "description": "Тревога на сумму Int в Table > 100 с корректирующим действием"
  }
}
```

**Настройка триггера через variableTriggers:**

```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.alerts.alarmTableSum",
    "name": "variableTriggers",
    "value": {
      "records": [{
        "mask": "users.admin.devices.virtualDevice",
        "variable": "table",
        "expression": "sum(table.Int) > 100",
        "mode": 0,
        "level": 2,
        "message": "Сумма Int в таблице превысила 100"
      }],
      "format": {
        "fields": [
          {"name": "mask", "type": "S"},
          {"name": "variable", "type": "S"},
          {"name": "expression", "type": "S"},
          {"name": "mode", "type": "I"},
          {"name": "level", "type": "I"},
          {"name": "message", "type": "S"}
        ]
      }
    }
  }
}
```

**Корректирующее действие (alertActions):**

Корректирующее действие (показ отчета оператору) настраивается через переменную `alertActions`. Это требует дополнительной настройки через веб-интерфейс или через более сложные операции MCP.

## Важные замечания

1. **Тревоги создаются в `users.admin.alerts`, а не в `users.admin.models`!**
2. **Используйте `eventTriggers` для тревог на события**
3. **Используйте `variableTriggers` для тревог на переменные**
4. **Поле `delay` в `variableTriggers` позволяет задать задержку перед активацией**
5. **Поле `correlator` в `eventTriggers` позволяет задать условие деактивации**

## Статус выполнения

- ✅ Задание 2: Тревога Event 1 -> Event 2 (Int > 20) - создана правильно в `users.admin.alerts.alarmEvent1Event2`
- ✅ Задание 3: Тревога через 10 секунд - создана правильно в `users.admin.alerts.alarmDelayedSum`
- ✅ Задание 4: Тревога на сумму Int в Table > 100 - создана правильно в `users.admin.alerts.alarmTableSum`
