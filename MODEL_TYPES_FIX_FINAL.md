# Исправление создания моделей - Финальная версия

## Проблема

Все созданные модели имели `type=1` в `childInfo`, независимо от указанного `modelType`. Это происходило потому, что:
1. Модели создавались с `type=1` по умолчанию в AggreGate
2. Попытка установить `type` вместе с другими параметрами не работала
3. AggreGate требует пошаговой настройки: сначала `type`, затем другие параметры

## Решение

Исправлен код `CreateContextTool.java` для пошаговой настройки моделей:

### Алгоритм:

1. **Создать модель** (она создается с `type=1` по умолчанию)
2. **Установить `type` первым** используя `setVariableField("childInfo", "type", modelType)`
3. **Подождать 500ms** - критично! AggreGate нужно время для применения типа
4. **Для относительных моделей**: установить `containerType` и `objectType` по отдельности с задержками

### Код:

```java
// Step 1: Set model type FIRST using setVariableField (CRITICAL!)
connection.executeWithTimeout(() -> {
    newContext.setVariableField("childInfo", "type", modelType, caller);
    return null;
}, 60000);

// Step 2: Wait for type to be applied (CRITICAL - AggreGate needs time to process)
Thread.sleep(500);

// Step 3: For relative models, set containerType and objectType
if (modelType == 1 && containerType != null && objectType != null) {
    // Set containerType
    connection.executeWithTimeout(() -> {
        newContext.setVariableField("childInfo", "containerType", containerType, caller);
        return null;
    }, 60000);
    
    Thread.sleep(200);
    
    // Set objectType
    connection.executeWithTimeout(() -> {
        newContext.setVariableField("childInfo", "objectType", objectType, caller);
        return null;
    }, 60000);
    
    Thread.sleep(300);
}
```

## Ключевые изменения:

1. ✅ Использование `setVariableField` вместо ручной работы с `DataTable`
2. ✅ Установка `type` первым, до всех остальных параметров
3. ✅ Задержки между операциями (500ms после type, 200ms между containerType/objectType)
4. ✅ Установка параметров по отдельности, не все сразу

## Результат:

- ✅ Абсолютная модель: `type=0`
- ✅ Относительная модель: `type=1`, `containerType="devices"`, `objectType="device"`
- ✅ Экземплярная модель: `type=2`

## Важно для ИИ:

При создании моделей через MCP сервер:
- Тип модели устанавливается автоматически
- Для относительных моделей `containerType` и `objectType` настраиваются автоматически
- Все настройки применяются пошагово с правильными задержками
