# Скрипт для пакетной миграции моделей

## Использование

Для миграции всех моделей нужно выполнить следующий процесс для каждой модели:

### Процесс для одной модели:

1. Получить данные с исходного сервера:
   - childInfo
   - info
   - modelVariables
   - modelFunctions
   - modelEvents
   - bindings
   - ruleSets

2. Обновить на целевом сервере:
   - set_variable_field(path, "childInfo", "type", model_type)
   - set_variable_field(path, "childInfo", "description", description)
   - set_variable_field(path, "childInfo", "enabled", enabled)
   
   Для относительных моделей (type=0):
   - set_variable_field(path, "childInfo", "defaultContext", defaultContext)
   - set_variable_field(path, "childInfo", "validityExpression", validityExpression)
   
   Для экземплярных моделей (type=2):
   - set_variable_field(path, "childInfo", "containerType", containerType)
   - set_variable_field(path, "childInfo", "containerName", containerName)
   - set_variable_field(path, "childInfo", "objectType", objectType)

3. Установить компоненты (если есть):
   - set_variable(path, "modelVariables", modelVariables)
   - set_variable(path, "modelFunctions", modelFunctions)
   - set_variable(path, "modelEvents", modelEvents)
   - set_variable(path, "bindings", bindings)
   - set_variable(path, "ruleSets", ruleSets)

4. Установить группу (если не default):
   - set_variable_field(path, "info", "group", group)

## Статус миграции

- ✅ objects - мигрирована (4 переменные, 1 функция)
- ✅ abonent - обновлена (type=1)
- ✅ absl - обновлена (type=1)
- ✅ absoluteMeter - обновлена (type=1)

## Осталось мигрировать: 101 модель

