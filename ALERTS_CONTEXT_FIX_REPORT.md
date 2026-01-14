# Отчет об исправлении создания контекстов тревог

**Дата:** 2025-01-27  
**Статус:** ✅ Исправлено в коде

## Проблема

При попытке создать контексты тревог в `users.admin.alerts` возникала ошибка:
```
For relative models (modelType=0), containerType is required.
```

Проблема была в том, что код `CreateContextTool` требовал `containerType` и `objectType` для всех относительных моделей (modelType=0), но эти параметры нужны **только для контекстов моделей** (`users.admin.models`), а не для других контекстов (alerts, widgets, dashboards и т.д.).

## Правильное решение

### Принцип
- `containerType` и `objectType` нужны **только для моделей** (контексты в `users.admin.models`)
- Для других контекстов (alerts, widgets, dashboards и т.д.) эти параметры не требуются и не используются
- Нужно проверять тип родительского контекста **до** валидации параметров

### Изменения в `CreateContextTool.java`

1. **Получение родительского контекста ПЕРЕД валидацией:**
   ```java
   // Get parent context FIRST to check its type
   Context parentContext = connection.executeWithTimeout(() -> {
       return cm.get(parentPath);
   }, 60000);
   ```

2. **Проверка, является ли родительский контекст контекстом моделей:**
   ```java
   // Check if parent context is a models context
   // Only models contexts require modelType, containerType, objectType
   boolean isModelsContext = parentPath.contains(".models") || parentPath.endsWith("models");
   ```

3. **Валидация параметров только для контекстов моделей:**
   ```java
   if (isModelsContext) {
       // For models contexts, modelType, containerType, objectType are relevant
       modelType = params.has("modelType") ? params.get("modelType").asInt() : 0;
       containerType = params.has("containerType") ? params.get("containerType").asText() : null;
       objectType = params.has("objectType") ? params.get("objectType").asText() : null;
       
       // Validate relative model parameters - only for models contexts
       if (modelType == 0) {
           // Проверка containerType и objectType
       }
   } else {
       // For non-models contexts, these parameters are not used
       modelType = 1; // Won't be used anyway
       containerType = null;
       objectType = null;
   }
   ```

4. **Настройка modelType только для контекстов моделей:**
   ```java
   // Only configure for models contexts
   if (isModelsContext && modelType == 0 && containerType != null && objectType != null) {
       // Установка containerType и objectType
   }
   ```

## Результат

Теперь контексты тревог можно создавать в `users.admin.alerts` без требования `containerType` и `objectType`:

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

А для моделей в `users.admin.models` по-прежнему требуется указание этих параметров:

```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "relativeModel",
    "description": "Относительная модель",
    "modelType": 0,
    "containerType": "devices",
    "objectType": "device"
  }
}
```

## Требуется перезапуск MCP сервера

Для применения изменений необходимо:
1. Остановить MCP сервер
2. Пересобрать проект: `cd mcp-server && gradlew build -x test`
3. Перезапустить MCP сервер

После перезапуска можно будет создавать контексты тревог в `users.admin.alerts` и другие контексты без ошибок.

## Тестирование

После перезапуска сервера выполните:

```json
// Создание тревоги 1 (в alerts - не требует modelType/containerType/objectType)
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.alerts",
    "name": "alarmEvent1Event2",
    "description": "Тревога, срабатывающая на Event 1 и деактивирующаяся при Event 2 (Int > 20)"
  }
}

// Создание тревоги 2
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.alerts",
    "name": "alarmDelayedSum",
    "description": "Тревога через 10 секунд после того, как сумма Int+Float попадает в диапазон 50-100"
  }
}

// Создание тревоги 3
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.alerts",
    "name": "alarmTableSum",
    "description": "Тревога на сумму Int в Table > 100 с корректирующим действием"
  }
}
```

Все три контекста должны создаться успешно без ошибок.

## Важные замечания

1. **Проверка типа контекста происходит ДО валидации** - это правильный подход
2. **Только контексты моделей** требуют `modelType`, `containerType`, `objectType`
3. **Другие контексты** (alerts, widgets, dashboards и т.д.) создаются стандартным способом без этих параметров
4. **Переменные сделаны final** для использования в лямбда-выражениях
