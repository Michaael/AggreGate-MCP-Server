# Исправление относительной модели

## Проблема

Относительная модель была создана неверно - использовались абсолютные пути в привязках вместо относительных ссылок.

## Решение

### 1. Настройка типа модели

Для относительной модели необходимо установить:
- `containerType = "devices"` - указывает, что модель привязывается к устройствам
- `objectType = "device"` - указывает тип объекта

```json
{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.relativeWavesSum",
    "variableName": "childInfo",
    "fieldName": "containerType",
    "value": "devices"
  }
}

{
  "tool": "aggregate_set_variable_field",
  "parameters": {
    "path": "users.admin.models.relativeWavesSum",
    "variableName": "childInfo",
    "fieldName": "objectType",
    "value": "device"
  }
}
```

### 2. Исправление привязок

**НЕПРАВИЛЬНО (абсолютные пути):**
```json
{
  "expression": "{users.admin.devices.virtualDevice:sine} + {users.admin.devices.virtualDevice:sawtooth}"
}
```

**ПРАВИЛЬНО (относительные ссылки):**
```json
{
  "expression": "{.:sine} + {.:sawtooth}"
}
```

В относительной модели `{.:variableName}` ссылается на переменную текущего объекта (устройства), к которому привязан экземпляр модели.

### 3. Итоговая привязка

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
      }]
    }
  }
}
```

## Разница между типами моделей

### Относительная модель (type=0)
- Много экземпляров (по одному на объект)
- Привязки используют относительные ссылки: `{.:sine}`
- Требует настройки `containerType` и `objectType`
- Используется для унификации обработки данных от разных объектов

### Абсолютная модель (type=1)
- Один экземпляр модели
- Привязки используют абсолютные пути: `{users.admin.devices.device1:sine}`
- Используется для глобальных вычислений

### Экземплярная модель (type=2)
- Создается по требованию
- Может объединять несколько объектов
- Используется для комплексного анализа

## Обновление документации

Создан файл `docs/MCP_MODEL_TYPES_GUIDE.md` с подробным описанием всех трех типов моделей и правил их использования.
