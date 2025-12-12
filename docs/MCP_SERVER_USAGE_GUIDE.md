# Руководство по использованию MCP сервера AggreGate

## Введение

Это руководство описывает правильный порядок работы с MCP сервером AggreGate для создания всех сущностей (моделей, переменных, функций, событий) без ошибок.

## ⚠️ Важные особенности работы MCP сервера

### 1. Проблема верификации после создания

**Важно знать**: При создании переменных, функций и событий в модели контекста MCP сервер может вернуть ошибку верификации (`verification failed`), **НО элементы при этом успешно создаются**.

**Что происходит:**
- Элемент создается в базе данных AggreGate
- Верификация выполняется слишком быстро, до полной инициализации контекста
- Возвращается ошибка, но элемент уже существует

**Решение:**
1. **Не паникуйте** - проверьте, что элемент действительно создан через `aggregate_list_variables`, `aggregate_list_functions` или проверку переменной `modelEvents`
2. **Продолжайте работу** - если элемент создан, можно продолжать создавать следующие элементы
3. **Повторная попытка** - если элемент не создан, повторите операцию через несколько секунд

### 2. Порядок создания элементов модели

**КРИТИЧЕСКИ ВАЖНО** соблюдать правильный порядок:

1. ✅ Подключение к серверу (`aggregate_connect`)
2. ✅ Вход в систему (`aggregate_login`)
3. ✅ Создание модели контекста (`aggregate_create_context`)
4. ✅ **Создание переменных модели** (`aggregate_create_variable`) - **ОБЯЗАТЕЛЬНО**
5. ✅ **Создание событий модели** (`aggregate_create_event`) - **ОБЯЗАТЕЛЬНО**
6. ✅ Создание функций (`aggregate_create_function`) - опционально
7. ✅ Создание привязок (bindings) - через `aggregate_set_variable`
8. ✅ Создание устройств, виджетов, дашбордов

## Пошаговое руководство

### Шаг 1: Подключение к серверу

```json
{
  "tool": "aggregate_connect",
  "parameters": {
    "host": "localhost",
    "port": 6460,
    "username": "admin",
    "password": "admin"
  }
}
```

**Результат:** `{"success": true, "message": "Connected to server"}`

После подключения выполните вход:

```json
{
  "tool": "aggregate_login"
}
```

**Результат:** `{"success": true, "message": "Logged in successfully"}`

### Шаг 2: Создание модели контекста

Модели создаются в папке `users.admin.models`:

```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "my_model",
    "description": "Описание модели"
  }
}
```

**Результат:** 
```json
{
  "success": true,
  "message": "Context created successfully",
  "path": "users.admin.models.my_model",
  "name": "my_model"
}
```

**Важно:** После создания модели подождите 1-2 секунды перед созданием переменных.

### Шаг 3: Создание переменных модели

**⚠️ ОБЯЗАТЕЛЬНО:** Переменные модели НЕ создаются автоматически. Их нужно создавать явно.

#### Формат переменных

Используйте правильный синтаксис TableFormat:

- `<name><S>` - строка (String)
- `<name><I>` - целое число (Integer)
- `<name><L>` - длинное целое (Long)
- `<name><D>` - число с плавающей точкой (Double)
- `<name><E>` - число с плавающей точкой (Extended)
- `<name><B>` - булево значение (Boolean)

#### Примеры создания переменных

**Простая строковая переменная:**
```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.my_model",
    "variableName": "status",
    "format": "<status><S>",
    "description": "Статус системы",
    "group": "Основные",
    "writable": true,
    "storageMode": 0
  }
}
```

**Числовая переменная:**
```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.my_model",
    "variableName": "temperature",
    "format": "<temperature><E>",
    "description": "Температура",
    "group": "Датчики",
    "writable": true,
    "storageMode": 0
  }
}
```

**Целочисленная переменная:**
```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.my_model",
    "variableName": "counter",
    "format": "<counter><I>",
    "description": "Счетчик",
    "group": "Основные",
    "writable": true,
    "storageMode": 0
  }
}
```

#### Параметры переменной

- `path` - путь к модели контекста (обязательно)
- `variableName` - имя переменной (обязательно)
- `format` - формат переменной в виде строки TableFormat (обязательно)
- `description` - описание (опционально)
- `group` - группа переменных (опционально)
- `writable` - можно ли записывать (по умолчанию `false`, рекомендуется `true`)
- `readPermissions` - права на чтение (по умолчанию `"observer"`)
- `writePermissions` - права на запись (по умолчанию `"manager"`)
- `storageMode` - режим хранения: `0` = база данных, `1` = память (по умолчанию `0`)

#### ⚠️ Обработка ошибок верификации

Если получили ошибку:
```json
{
  "error": {
    "code": -32001,
    "message": "Variable was not created in model context - verification failed"
  }
}
```

**Что делать:**
1. Проверьте, что переменная создана:
```json
{
  "tool": "aggregate_list_variables",
  "parameters": {
    "path": "users.admin.models.my_model"
  }
}
```

2. Если переменная есть в списке - продолжайте работу, ошибка была ложной
3. Если переменной нет - подождите 2-3 секунды и повторите создание

### Шаг 4: Создание событий модели

**⚠️ ОБЯЗАТЕЛЬНО:** События модели НЕ создаются автоматически. Их нужно создавать явно.

#### Примеры создания событий

**Простое информационное событие:**
```json
{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.my_model",
    "eventName": "status_changed",
    "description": "Событие изменения статуса",
    "group": "События",
    "level": 0
  }
}
```

**Событие с параметрами:**
```json
{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.my_model",
    "eventName": "temperature_alert",
    "format": "<temperature><E><threshold><E>",
    "description": "Предупреждение о температуре",
    "group": "Алерты",
    "level": 1,
    "historyStorageTime": 86400000
  }
}
```

**Событие ошибки:**
```json
{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.my_model",
    "eventName": "error_occurred",
    "format": "<message><S><code><I>",
    "description": "Событие ошибки",
    "group": "Ошибки",
    "level": 2,
    "permissions": "observer",
    "firePermissions": "manager"
  }
}
```

#### Параметры события

- `path` - путь к модели контекста (обязательно)
- `eventName` - имя события (обязательно)
- `format` - формат события в виде строки TableFormat (опционально, по умолчанию пустой)
- `description` - описание (опционально)
- `group` - группа событий (опционально)
- `level` - уровень события:
  - `0` = INFO (информация)
  - `1` = WARNING (предупреждение)
  - `2` = ERROR (ошибка)
  - `3` = FATAL (критическая ошибка)
  - `4` = NOTICE (уведомление)
- `permissions` - права на чтение (по умолчанию `"observer"`)
- `firePermissions` - права на генерацию события (по умолчанию `"admin"`)
- `historyStorageTime` - время хранения истории в миллисекундах (по умолчанию `0`)

#### ⚠️ Обработка ошибок верификации

Если получили ошибку верификации, проверьте создание события через переменную `modelEvents`:

```json
{
  "tool": "aggregate_get_variable",
  "parameters": {
    "path": "users.admin.models.my_model",
    "name": "modelEvents"
  }
}
```

В ответе будет список всех созданных событий. Если событие есть - продолжайте работу.

### Шаг 5: Создание функций

Функции создаются опционально, но часто необходимы для автоматизации.

#### Типы функций

- `0` = Java (требует реализацию на Java)
- `1` = Expression (выражение на JavaScript-подобном языке)
- `2` = Query (SQL-запрос)

#### Примеры создания функций

**Функция типа Expression (рекомендуется):**
```json
{
  "tool": "aggregate_create_function",
  "parameters": {
    "path": "users.admin.models.my_model",
    "functionName": "calculate_sum",
    "functionType": 1,
    "description": "Вычисление суммы двух чисел",
    "group": "Вычисления",
    "inputFormat": "<arg1><E><arg2><E>",
    "outputFormat": "<result><E>",
    "expression": "arg1 + arg2"
  }
}
```

**Простая функция типа Java:**
```json
{
  "tool": "aggregate_create_function",
  "parameters": {
    "path": "users.admin.models.my_model",
    "functionName": "get_status",
    "description": "Получить статус системы",
    "group": "Информация",
    "functionType": 0
  }
}
```

**Функция с пустым форматом (для простых функций):**
```json
{
  "tool": "aggregate_create_function",
  "parameters": {
    "path": "users.admin.models.my_model",
    "functionName": "reset_counter",
    "description": "Сбросить счетчик",
    "group": "Управление",
    "functionType": 0
  }
}
```

#### Параметры функции

- `path` - путь к модели контекста (обязательно)
- `functionName` - имя функции (обязательно)
- `functionType` - тип функции: `0`=Java, `1`=Expression, `2`=Query (по умолчанию `0`)
- `description` - описание (опционально)
- `group` - группа функций (опционально)
- `inputFormat` - формат входных параметров (опционально)
- `outputFormat` - формат результата (опционально)
- `expression` - выражение для типа Expression (обязательно для `functionType=1`)
- `query` - SQL-запрос для типа Query (обязательно для `functionType=2`)

#### ⚠️ Обработка ошибок

Если получили ошибку:
- `"Function already exists"` - функция уже создана, можно продолжать
- `"verification failed"` - проверьте через `aggregate_list_functions`, функция может быть создана

### Шаг 6: Проверка созданных элементов

После создания всех элементов проверьте их наличие:

**Проверка переменных:**
```json
{
  "tool": "aggregate_list_variables",
  "parameters": {
    "path": "users.admin.models.my_model"
  }
}
```

**Проверка функций:**
```json
{
  "tool": "aggregate_list_functions",
  "parameters": {
    "path": "users.admin.models.my_model"
  }
}
```

**Проверка событий (через переменную):**
```json
{
  "tool": "aggregate_get_variable",
  "parameters": {
    "path": "users.admin.models.my_model",
    "name": "modelEvents"
  }
}
```

## Полный пример создания модели

Вот полный пример создания модели с переменными, функциями и событиями:

```json
// 1. Подключение
{"tool": "aggregate_connect", "parameters": {"host": "localhost", "port": 6460, "username": "admin", "password": "admin"}}
{"tool": "aggregate_login"}

// 2. Создание модели
{"tool": "aggregate_create_context", "parameters": {"parentPath": "users.admin.models", "name": "test_model", "description": "Тестовая модель"}}

// 3. Создание переменных (ОБЯЗАТЕЛЬНО!)
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.test_model", "variableName": "status", "format": "<status><S>", "description": "Статус", "writable": true}}
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.test_model", "variableName": "temperature", "format": "<temperature><E>", "description": "Температура", "writable": true}}
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.test_model", "variableName": "counter", "format": "<counter><I>", "description": "Счетчик", "writable": true}}

// 4. Создание событий (ОБЯЗАТЕЛЬНО!)
{"tool": "aggregate_create_event", "parameters": {"path": "users.admin.models.test_model", "eventName": "status_changed", "description": "Изменение статуса", "level": 0}}
{"tool": "aggregate_create_event", "parameters": {"path": "users.admin.models.test_model", "eventName": "temperature_alert", "format": "<temperature><E>", "description": "Предупреждение о температуре", "level": 1}}
{"tool": "aggregate_create_event", "parameters": {"path": "users.admin.models.test_model", "eventName": "counter_reset", "description": "Сброс счетчика", "level": 0}}

// 5. Создание функций (опционально)
{"tool": "aggregate_create_function", "parameters": {"path": "users.admin.models.test_model", "functionName": "get_status", "description": "Получить статус", "functionType": 0}}
{"tool": "aggregate_create_function", "parameters": {"path": "users.admin.models.test_model", "functionName": "reset_counter", "description": "Сбросить счетчик", "functionType": 0}}

// 6. Проверка созданных элементов
{"tool": "aggregate_list_variables", "parameters": {"path": "users.admin.models.test_model"}}
{"tool": "aggregate_list_functions", "parameters": {"path": "users.admin.models.test_model"}}
```

## Типичные проблемы и решения

### Проблема 1: Ошибка верификации при создании

**Симптомы:**
```json
{
  "error": {
    "code": -32001,
    "message": "Variable was not created in model context - verification failed"
  }
}
```

**Решение:**
1. Проверьте список элементов через `aggregate_list_variables` или `aggregate_get_variable` (для событий)
2. Если элемент создан - игнорируйте ошибку и продолжайте
3. Если элемент не создан - подождите 2-3 секунды и повторите

### Проблема 2: Переменная не создается

**Причины:**
- Неверный формат переменной
- Контекст не существует
- Недостаточно прав

**Решение:**
1. Проверьте правильность формата: `<name><T>`, где `T` - тип (`S`, `I`, `L`, `D`, `E`, `B`)
2. Убедитесь, что модель создана: `aggregate_get_context`
3. Проверьте права доступа

### Проблема 3: Событие не создается

**Причины:**
- Неверный формат события
- Контекст не инициализирован

**Решение:**
1. Проверьте формат события (опционально, можно оставить пустым)
2. Убедитесь, что модель создана и переменные созданы
3. Подождите 1-2 секунды после создания переменных

### Проблема 4: Функция не создается

**Причины:**
- Неверный формат входных/выходных параметров
- Неверное выражение для Expression типа
- Функция уже существует

**Решение:**
1. Для простых функций можно не указывать форматы
2. Для Expression типа проверьте синтаксис выражения
3. Проверьте, не существует ли функция: `aggregate_list_functions`

## Рекомендации

### 1. Порядок операций

Всегда соблюдайте порядок:
1. Подключение → Вход
2. Создание модели
3. **Создание переменных** (обязательно!)
4. **Создание событий** (обязательно!)
5. Создание функций (опционально)
6. Проверка созданных элементов

### 2. Обработка ошибок

- **Не паникуйте** при ошибках верификации - проверьте, что элемент создан
- **Проверяйте** созданные элементы через списки перед продолжением
- **Повторяйте** создание только если элемент действительно не создан

### 3. Форматы данных

- Используйте правильный синтаксис TableFormat: `<name><T>`
- Для простых переменных достаточно одного поля
- Для сложных структур используйте полный синтаксис TableFormat

### 4. Время ожидания

- После создания модели подождите 1-2 секунды
- После создания переменных подождите 0.5-1 секунду перед созданием событий
- При ошибках верификации подождите 2-3 секунды перед повторной попыткой

## Чек-лист создания модели

- [ ] ✅ Подключение к серверу (`aggregate_connect`)
- [ ] ✅ Вход в систему (`aggregate_login`)
- [ ] ✅ Создание модели контекста (`aggregate_create_context`)
- [ ] ✅ **Создание переменных модели** (`aggregate_create_variable`) - **ОБЯЗАТЕЛЬНО**
- [ ] ✅ **Создание событий модели** (`aggregate_create_event`) - **ОБЯЗАТЕЛЬНО**
- [ ] ✅ Создание функций (`aggregate_create_function`) - опционально
- [ ] ✅ Проверка созданных элементов через списки
- [ ] ✅ Создание привязок (bindings) - если нужно
- [ ] ✅ Создание устройств, виджетов, дашбордов - если нужно

## Заключение

Помните:
- **Переменные и события модели НЕ создаются автоматически** - их нужно создавать явно
- **Ошибки верификации часто ложные** - проверяйте создание элементов через списки
- **Соблюдайте порядок операций** - это гарантирует корректную работу
- **Проверяйте созданные элементы** перед продолжением работы

Следуя этому руководству, вы сможете успешно создавать все сущности в AggreGate через MCP сервер без ошибок.

