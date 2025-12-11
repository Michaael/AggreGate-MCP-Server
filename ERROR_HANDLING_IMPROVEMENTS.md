# Улучшения обработки ошибок в MCP сервере

## Обзор изменений

Улучшена обработка ошибок во всех инструментах MCP сервера для возврата понятных и детальных сообщений об ошибках от сервера AggreGate.

## Что было сделано

### 1. Создан утилитный класс ErrorHandler

**Файл:** `mcp-server/src/main/java/com/tibbo/aggregate/mcp/util/ErrorHandler.java`

Класс предоставляет методы для извлечения детальной информации об ошибках из исключений AggreGate:

- `extractErrorMessage(Throwable)` - извлекает полное сообщение об ошибке, включая все вложенные причины
- `extractErrorDetails(Throwable)` - извлекает детальную информацию об ошибке для включения в ответ

**Особенности:**
- Извлекает сообщения из всех вложенных исключений (до 10 уровней глубины)
- Специальная обработка для `ContextException`, `AggreGateException`, `AggreGateRuntimeException`
- Предотвращает бесконечные циклы при циклических ссылках в исключениях
- Включает ограниченный stack trace для отладки (первые 5 фреймов из пакета AggreGate)

### 2. Обновлены инструменты

Все инструменты теперь используют `ErrorHandler` для извлечения детальных сообщений об ошибках:

#### Создание элементов:
- ✅ `CreateVariableTool` - создание переменных
- ✅ `CreateEventTool` - создание событий
- ✅ `CreateFunctionTool` - создание функций
- ✅ `CreateDeviceTool` - создание устройств
- ✅ `CreateWidgetTool` - создание виджетов
- ✅ `CreateDashboardTool` - создание дашбордов

#### Другие операции:
- ✅ `ExecuteActionTool` - выполнение действий
- ✅ `SetWidgetTemplateTool` - установка шаблона виджета
- ✅ `AddDashboardElementTool` - добавление элементов в дашборд

#### Протокол:
- ✅ `McpProtocolHandler` - обработчик протокола MCP

### 3. Формат ответа об ошибке

Теперь все ошибки возвращаются в следующем формате:

```json
{
  "jsonrpc": "2.0",
  "id": <request_id>,
  "error": {
    "code": <error_code>,
    "message": "Детальное сообщение об ошибке с полной цепочкой причин",
    "data": {
      "message": "Полное сообщение об ошибке",
      "exceptionType": "com.tibbo.aggregate.common.context.ContextException",
      "stackTrace": [
        "com.tibbo.aggregate.mcp.tools.variable.CreateVariableTool.execute(...)",
        "..."
      ]
    }
  }
}
```

## Примеры улучшений

### До изменений:

```json
{
  "error": {
    "code": -32001,
    "message": "Failed to create variable: null"
  }
}
```

### После изменений:

```json
{
  "error": {
    "code": -32001,
    "message": "Failed to create variable: Удаленная ошибка: Variable already exists: device1Sine -> ContextException: Variable definition already exists",
    "data": {
      "message": "Удаленная ошибка: Variable already exists: device1Sine -> ContextException: Variable definition already exists",
      "exceptionType": "java.lang.RuntimeException",
      "stackTrace": [
        "com.tibbo.aggregate.mcp.tools.variable.CreateVariableTool.execute(...)"
      ]
    }
  }
}
```

## Преимущества

1. **Понятные сообщения** - пользователь видит полную цепочку причин ошибки
2. **Детальная информация** - включена информация о типе исключения и stack trace для отладки
3. **Единообразие** - все инструменты используют одинаковый подход к обработке ошибок
4. **Отладка** - stack trace помогает разработчикам быстро найти источник проблемы

## Использование

Обработка ошибок теперь автоматическая. Все исключения от сервера AggreGate будут обрабатываться и возвращать понятные сообщения.

Пример обработки в коде инструмента:

```java
} catch (Exception e) {
    String errorMessage = ErrorHandler.extractErrorMessage(e);
    ErrorHandler.ErrorDetails errorDetails = ErrorHandler.extractErrorDetails(e);
    throw new McpException(
        McpError.CONTEXT_ERROR,
        "Failed to create variable: " + errorMessage,
        errorDetails
    );
}
```

## Тестирование

Для тестирования можно использовать любую операцию, которая должна вызвать ошибку:

1. Попытка создать переменную с неверным форматом
2. Попытка создать элемент, который уже существует
3. Попытка выполнить действие, которого не существует
4. Попытка обратиться к несуществующему контексту

Во всех случаях теперь будет возвращаться понятное сообщение об ошибке с деталями.

