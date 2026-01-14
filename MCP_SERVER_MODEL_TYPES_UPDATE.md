# Обновление MCP сервера: Поддержка типов моделей

## Дата обновления
2025-01-20

## Изменения

### Файл: `mcp-server/src/main/java/com/tibbo/aggregate/mcp/tools/context/CreateContextTool.java`

#### Добавлены параметры:

1. **`modelType`** (integer, optional, default=0)
   - `0` = Относительная модель (по умолчанию)
   - `1` = Абсолютная модель
   - `2` = Экземплярная модель

2. **`containerType`** (string, optional, required для modelType=0)
   - Тип контейнера для относительных моделей
   - Общие значения: `"devices"` (для моделей устройств), `"objects"` (по умолчанию)

3. **`objectType`** (string, optional, required для modelType=1)
   - Тип объекта для относительных моделей
   - Общие значения: `"device"` (для моделей устройств), `"object"` (по умолчанию)

#### Автоматическая настройка

При создании относительной модели (`modelType=0`) с указанными `containerType` и `objectType`, сервер автоматически:
1. Создает контекст модели
2. Настраивает переменную `childInfo`:
   - Устанавливает `type = 0`
   - Устанавливает `containerType` (из параметра)
   - Устанавливает `objectType` (из параметра)

#### Валидация

- Если указан `modelType=0`, но не указаны `containerType` или `objectType`, возвращается ошибка с понятным сообщением и ссылкой на документацию.

#### Результат выполнения

Ответ теперь включает информацию о типе модели:
```json
{
  "success": true,
  "message": "Context created successfully",
  "path": "users.admin.models.relativeModel",
  "name": "relativeModel",
  "modelType": 1,
  "modelTypeName": "relative",
  "containerType": "devices",
  "objectType": "device",
  "note": "Relative model configured. Use relative references {.:var} in bindings, not absolute paths."
}
```

## Примеры использования

### Абсолютная модель (по умолчанию)
```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "absoluteModel",
    "description": "Абсолютная модель"
  }
}
```

### Относительная модель
```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "relativeModel",
    "description": "Относительная модель",
    "modelType": 1,
    "containerType": "devices",
    "objectType": "device"
  }
}
```

### Экземплярная модель
```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "instanceModel",
    "description": "Экземплярная модель",
    "modelType": 2
  }
}
```

## Преимущества для ИИ

1. **Автоматическая настройка**: ИИ не нужно вручную настраивать `childInfo` после создания модели
2. **Валидация**: Сервер проверяет корректность параметров и выдает понятные ошибки
3. **Подсказки**: В описании параметров и результатах есть четкие инструкции
4. **Предотвращение ошибок**: ИИ не сможет забыть настроить тип модели для относительных моделей

## Обратная совместимость

- Все существующие вызовы `aggregate_create_context` без параметров `modelType`, `containerType`, `objectType` продолжают работать
- По умолчанию создается абсолютная модель (как раньше)
- Параметры опциональны, кроме случая `modelType=1`

## Связанная документация

- `docs/MCP_MODEL_TYPES_GUIDE.md` - подробное руководство по типам моделей
- `AI_WORKFLOW_RULES.md` - правила для ИИ при работе с моделями
- `AI_AUTO_TESTING_GUIDE.md` - автоматическое тестирование моделей
