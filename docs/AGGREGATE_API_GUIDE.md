# Руководство по работе с API AggreGate через MCP сервер

## Введение

Это руководство описывает правильный порядок работы с API AggreGate через MCP сервер. Оно поможет избежать типичных ошибок при создании моделей контекстов, переменных, событий и привязок.

## Важные принципы работы с AggreGate

### 1. Порядок создания элементов модели контекста

При создании модели контекста (model context) в AggreGate **критически важно** соблюдать правильный порядок операций:

1. **Создание модели контекста** (`aggregate_create_context`)
2. **Создание переменных модели** (`aggregate_create_variable`) - **ОБЯЗАТЕЛЬНО**
3. **Создание событий модели** (`aggregate_create_event`) - **ОБЯЗАТЕЛЬНО**
4. **Создание привязок** (bindings) - через `aggregate_execute_action`
5. Создание устройств, виджетов, дашбордов и других элементов

⚠️ **ВАЖНО**: Переменные и события модели **НЕ создаются автоматически** при создании модели контекста. Их нужно создавать **явно** с помощью соответствующих функций MCP сервера.

### 2. Разница между обычным контекстом и моделью контекста

- **Обычный контекст**: переменные и события создаются напрямую через `addVariableDefinition()` и `addEventDefinition()`
- **Модель контекста**: переменные и события хранятся в специальных переменных `V_MODEL_VARIABLES` и `V_MODEL_EVENTS`, которые определяют структуру всех экземпляров модели

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

После подключения выполните вход:

```json
{
  "tool": "aggregate_login"
}
```

### Шаг 2: Создание модели контекста

Создайте модель контекста в нужном месте (например, в `users.admin.models`):

```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "cluster",
    "description": "Модель кластера устройств"
  }
}
```

✅ **Результат**: Модель контекста `users.admin.models.cluster` создана.

### Шаг 3: Создание переменных модели ⚠️ ОБЯЗАТЕЛЬНО

**КРИТИЧЕСКИ ВАЖНО**: После создания модели контекста необходимо создать переменные модели. Без этого модель не будет работать корректно.

Пример создания переменной состояния кластера:

```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.cluster",
    "variableName": "status",
    "format": "<status><S>",
    "description": "Статус кластера",
    "group": "Основные",
    "writable": true,
    "readPermissions": "observer",
    "writePermissions": "manager",
    "storageMode": 0
  }
}
```

Пример создания переменной с числовым значением:

```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.cluster",
    "variableName": "deviceCount",
    "format": "<count><I>",
    "description": "Количество устройств в кластере",
    "group": "Основные",
    "writable": true,
    "storageMode": 0
  }
}
```

Пример создания переменной типа Double (число с плавающей точкой):

```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.cluster",
    "variableName": "device1Sine",
    "format": "<value><D>",
    "description": "Значение переменной Sine Wave устройства 1",
    "group": "Метрики устройств",
    "writable": false,
    "storageMode": 0
  }
}
```

**Параметры переменной:**
- `path` - путь к модели контекста (обязательно)
- `variableName` - имя переменной (обязательно)
- `format` - формат переменной в виде строки TableFormat (обязательно)
  - `<name><S>` - строка
  - `<name><I>` - целое число
  - `<name><L>` - длинное целое
  - `<name><D>` - число с плавающей точкой
  - `<name><B>` - булево значение
- `description` - описание (опционально)
- `group` - группа переменных (опционально)
- `writable` - можно ли записывать (по умолчанию `false`)
- `readPermissions` - права на чтение (по умолчанию `"observer"`)
- `writePermissions` - права на запись (по умолчанию `"manager"`)
- `storageMode` - режим хранения: `0` = база данных, `1` = память (по умолчанию `0`)

### Шаг 4: Создание событий модели ⚠️ ОБЯЗАТЕЛЬНО

**КРИТИЧЕСКИ ВАЖНО**: После создания переменных необходимо создать события модели. События позволяют уведомлять о важных изменениях состояния.

Пример создания события тревоги:

```json
{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.cluster",
    "eventName": "alarm",
    "format": "<message><S><severity><I>",
    "description": "Событие тревоги кластера",
    "group": "События",
    "level": 2,
    "permissions": "observer",
    "firePermissions": "manager",
    "historyStorageTime": 86400000
  }
}
```

Пример создания информационного события:

```json
{
  "tool": "aggregate_create_event",
  "parameters": {
    "path": "users.admin.models.cluster",
    "eventName": "statusChanged",
    "format": "<oldStatus><S><newStatus><S>",
    "description": "Изменение статуса кластера",
    "group": "События",
    "level": 0,
    "historyStorageTime": 0
  }
}
```

**Параметры события:**
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

### Шаг 5: Создание привязок (Bindings) ⚠️ ОБЯЗАТЕЛЬНО

**КРИТИЧЕСКИ ВАЖНО**: Привязки (bindings) связывают переменные и события модели с переменными и событиями устройств или других контекстов. Без привязок модель не будет получать данные от устройств.

Привязки создаются через переменную `bindings` модели контекста. Используйте правильный формат ссылок AggreGate:

**Формат ссылок:**
- `.:имя_переменной` - ссылка на переменную текущего контекста (используется в `target`)
- `{контекст:переменная}` - ссылка на переменную другого контекста (используется в `expression`)

**Пример привязки переменной через `aggregate_set_variable`:**

```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.cluster",
    "name": "bindings",
    "value": {
      "recordCount": 1,
      "format": {
        "minRecords": 0,
        "maxRecords": 2147483647,
        "fields": [
          {"name": "target", "type": "S"},
          {"name": "expression", "type": "S"},
          {"name": "onevent", "type": "B"}
        ]
      },
      "records": [{
        "target": ".:device1Sine",
        "expression": "{users.admin.devices.device1:sine}",
        "onevent": true
      }]
    }
  }
}
```

**Пример привязки события:**

```json
{
  "records": [{
    "target": ".:alarm",
    "expression": "{users.admin.devices.device1:alarm}",
    "onevent": true
  }]
}
```

**Пример создания нескольких привязок одновременно:**

```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.cluster",
    "name": "bindings",
    "value": {
      "recordCount": 3,
      "format": {
        "minRecords": 0,
        "maxRecords": 2147483647,
        "fields": [
          {"name": "target", "type": "S"},
          {"name": "expression", "type": "S"},
          {"name": "onevent", "type": "B"}
        ]
      },
      "records": [
        {
          "target": ".:device1Sine",
          "expression": "{users.admin.devices.device1:sine}",
          "onevent": true
        },
        {
          "target": ".:device2Sine",
          "expression": "{users.admin.devices.device2:sine}",
          "onevent": true
        },
        {
          "target": ".:device3Sine",
          "expression": "{users.admin.devices.device3:sine}",
          "onevent": true
        }
      ]
    }
  }
}
```

**Важные детали о привязках:**
- `target`: `.:имя_переменной` - ссылка на переменную текущего контекста (обязательно с префиксом `.:`)
- `expression`: `{контекст:переменная}` - ссылка на переменную другого контекста
- `onevent`: `true` - обновление при изменении значения (рекомендуется для реального времени)

**Важно**: 
- В поле `target` всегда указывайте `.:имя_переменной` (с префиксом `.:`)
- В поле `expression` используйте формат `{контекст:переменная}` для ссылки на переменную другого контекста
- Подробнее о формате ссылок см. [Формат ссылок в AggreGate](AGGREGATE_REFERENCES_FORMAT.md)

### Шаг 6: Создание устройств

После настройки модели можно создавать устройства:

```json
{
  "tool": "aggregate_create_device",
  "parameters": {
    "username": "admin",
    "deviceName": "device1",
    "description": "Первое устройство кластера",
    "driverId": "com.tibbo.linkserver.plugin.device.virtual"
  }
}
```

### Шаг 7: Создание правил и функций Expression

После создания привязок можно создать правила (rules) или функции Expression для автоматической обработки данных и вычисления производных значений.

**Создание функции типа Expression через MCP API:**

```json
{
  "tool": "aggregate_create_function",
  "parameters": {
    "path": "users.admin.models.cluster",
    "functionName": "checkClusterStatus",
    "functionType": 1,
    "description": "Проверка состояния кластера",
    "expression": "if (device1Sine != null && device2Sine != null && device3Sine != null) { var oldStatus = clusterStatus; if (device1Sine > 0 && device2Sine > 0 && device3Sine > 0) { clusterStatus = \"ошибка\"; if (oldStatus != \"ошибка\") { fireEvent(\"clusterAlarm\", \"Кластер перешел в состояние ошибки: все устройства имеют sine > 0\"); } } else { clusterStatus = \"ОК\"; } }"
  }
}
```

**Пример выражения правила с использованием формата ссылок AggreGate:**

```javascript
if (.:device1Sine != null && .:device2Sine != null && .:device3Sine != null) {
  if (.:device1Sine > 0 && .:device2Sine > 0 && .:device3Sine > 0) {
    var oldStatus = .:clusterStatus;
    .:clusterStatus = "ошибка";
    if (oldStatus != "ошибка") {
      .:fireEvent("clusterAlarm@", "Кластер перешел в состояние ошибки: все устройства имеют sine > 0");
    }
  } else {
    .:clusterStatus = "ОК";
  }
}
```

**Примечание:** Правила можно создавать через веб-интерфейс AggreGate в разделе "Rule Sets" модели контекста. Настройте триггеры так, чтобы правило срабатывало при изменении связанных переменных.

### Шаг 8: Создание виджетов

Виджеты создаются через MCP API, но их XML шаблоны настраиваются через веб-интерфейс AggreGate.

**Создание виджета:**

```json
{
  "tool": "aggregate_create_widget",
  "parameters": {
    "parentPath": "users.admin.widgets",
    "name": "clusterStatusWidget",
    "description": "Виджет состояния кластера устройств",
    "defaultContext": "users.admin.models.cluster"
  }
}
```

**Пример XML шаблона для виджета состояния кластера:**

После создания виджета откройте его в веб-интерфейсе и настройте шаблон:

```xml
<panel>
  <border-layout/>
  <panel>
    <title>Состояние кластера</title>
    <flow-layout/>
    <label>
      <text>Состояние кластера: ${clusterStatus}</text>
      <foreground>${clusterStatus == "ошибка" ? "red" : "green"}</foreground>
    </label>
    <table>
      <model-context path="users.admin.models.cluster"/>
      <column name="device1Sine" title="Устройство 1 (Sine)"/>
      <column name="device2Sine" title="Устройство 2 (Sine)"/>
      <column name="device3Sine" title="Устройство 3 (Sine)"/>
      <column name="clusterStatus" title="Состояние"/>
    </table>
    <panel>
      <title>Устройства кластера</title>
      <flow-layout/>
      <button onclick="openDeviceWidget('device1')" text="Устройство 1"/>
      <button onclick="openDeviceWidget('device2')" text="Устройство 2"/>
      <button onclick="openDeviceWidget('device3')" text="Устройство 3"/>
    </panel>
  </panel>
</panel>
```

**JavaScript функция для открытия виджета метрик устройства:**

```javascript
function openDeviceWidget(deviceName) {
  var devicePath = "users.admin.devices." + deviceName;
  var widgetPath = "users.admin.widgets.deviceMetricsWidget";
  launchWidget(widgetPath, devicePath);
}
```

**Пример XML шаблона для виджета метрик устройства:**

```xml
<panel>
  <border-layout/>
  <panel>
    <title>Метрики устройства: ${context.name}</title>
    <flow-layout/>
    <chart>
      <title>Sine Wave</title>
      <data-source>
        <context>${context}</context>
        <variable>sine</variable>
      </data-source>
      <history>true</history>
    </chart>
    <chart>
      <title>Sawtooth Wave</title>
      <data-source>
        <context>${context}</context>
        <variable>sawtooth</variable>
      </data-source>
      <history>true</history>
    </chart>
    <chart>
      <title>Triangle Wave</title>
      <data-source>
        <context>${context}</context>
        <variable>triangle</variable>
      </data-source>
      <history>true</history>
    </chart>
  </panel>
</panel>
```

**Важно:** Установите Default Context для виджета в свойствах виджета через веб-интерфейс.

### Шаг 9: Создание дашбордов

**Создание дашборда:**

```json
{
  "tool": "aggregate_create_dashboard",
  "parameters": {
    "parentPath": "users.admin.dashboards",
    "name": "clusterDashboard",
    "description": "Инструментальная панель состояния кластера",
    "layout": "dockable"
  }
}
```

**Добавление элемента в дашборд:**

```json
{
  "tool": "aggregate_add_dashboard_element",
  "parameters": {
    "path": "users.admin.dashboards.clusterDashboard",
    "name": "clusterStatusElement",
    "type": "launchWidget",
    "parameters": {
      "widgetPath": "users.admin.widgets.clusterStatusWidget",
      "contextPath": "users.admin.models.cluster"
    }
  }
}
```

**Настройка автозапуска дашборда:**

Для автоматического открытия дашборда при логине оператора:

1. Откройте настройки пользователя (например, `admin`)
2. Перейдите в раздел "Dashboards" или "Preferences"
3. Найдите настройку "Default Dashboard" или "Startup Dashboard"
4. Установите значение: `users.admin.dashboards.clusterDashboard`

Альтернативный способ (если доступно):
1. Откройте дашборд в веб-интерфейсе
2. В свойствах дашборда найдите опцию "Auto-open on login"
3. Включите эту опцию

## Диагностика и обработка ошибок

Все функции MCP сервера возвращают детальные сообщения об ошибках от сервера AggreGate. Правильное понимание формата ошибок поможет быстро находить и исправлять проблемы.

### Формат ответов об ошибках

Все ошибки возвращаются в стандартном формате JSON-RPC:

```json
{
  "jsonrpc": "2.0",
  "id": <request_id>,
  "error": {
    "code": <error_code>,
    "message": "Детальное сообщение об ошибке с полной цепочкой причин"
  }
}
```

### Коды ошибок

MCP сервер использует стандартные JSON-RPC коды ошибок:

- **`-32602` (INVALID_PARAMS)** - неверные параметры запроса
  - Примеры: неверный формат переменной, отсутствующие обязательные параметры, неверный тип данных
- **`-32001` (CONTEXT_ERROR)** - ошибки работы с контекстами и элементами AggreGate
  - Примеры: контекст не найден, элемент уже существует, переменная недоступна, ошибка выполнения действия
- **`-32000` (CONNECTION_ERROR)** - ошибки соединения с сервером
  - Примеры: сервер недоступен, ошибка аутентификации, разрыв соединения

### Как правильно читать сообщения об ошибках

**1. Проверяйте поле `message`** - там содержится детальная информация об ошибке:

```json
{
  "error": {
    "code": -32001,
    "message": "Failed to create variable: Context not found: users.admin.models.nonexistent"
  }
}
```

✅ **Что видно:** Проблемный путь контекста четко указан в сообщении

**2. Обращайте внимание на цепочку причин** - сообщения могут содержать полную цепочку ошибок от сервера AggreGate:

```json
{
  "error": {
    "code": -32001,
    "message": "Failed to execute action: Failed to init action: Удаленная ошибка: Action 'NON_EXISTENT_ACTION' not available in context 'Модель кластера виртуальных устройств (users.admin.models.cluster)' (error code: 'E') -> ..."
  }
}
```

✅ **Что видно:** 
- Действие не найдено: `NON_EXISTENT_ACTION`
- Контекст, в котором выполнялось действие: `users.admin.models.cluster`
- Код ошибки от сервера AggreGate: `'E'`

**3. Используйте код ошибки для определения типа проблемы:**

- `-32602` → Проверьте параметры запроса (формат, типы, обязательные поля)
- `-32001` → Проверьте существование контекстов, элементов, правильность путей
- `-32000` → Проверьте соединение с сервером, учетные данные

### Примеры различных типов ошибок

#### Пример 1: Неверный формат переменной

**Запрос:**
```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "users.admin.models.cluster",
    "variableName": "testVar",
    "format": "INVALID_FORMAT_STRING!!!"
  }
}
```

**Ответ:**
```json
{
  "error": {
    "code": -32602,
    "message": "Invalid format: Index: 0, Size: 0, format was 'INVALID_FORMAT_STRING!!!'"
  }
}
```

✅ **Диагностика:** Проблема в формате переменной. Используйте правильный синтаксис TableFormat: `<name><T>`, где `T` - тип (`S`, `I`, `L`, `D`, `B`)

#### Пример 2: Несуществующий контекст

**Запрос:**
```json
{
  "tool": "aggregate_create_variable",
  "parameters": {
    "path": "NON_EXISTENT_CONTEXT_PATH",
    "variableName": "testVar",
    "format": "<value><S>"
  }
}
```

**Ответ:**
```json
{
  "error": {
    "code": -32001,
    "message": "Failed to create variable: Context not found: NON_EXISTENT_CONTEXT_PATH"
  }
}
```

✅ **Диагностика:** Контекст не существует. Проверьте правильность пути или создайте контекст сначала.

#### Пример 3: Дублирование элемента

**Запрос:**
```json
{
  "tool": "aggregate_create_device",
  "parameters": {
    "username": "admin",
    "deviceName": "device1",
    "description": "Устройство",
    "driverId": "com.tibbo.linkserver.plugin.device.virtual"
  }
}
```

**Ответ (если устройство уже существует):**
```json
{
  "error": {
    "code": -32001,
    "message": "Failed to create device: Failed to call add function: Удаленная ошибка: Device already exists:device1 (error code: 'E') -> ..."
  }
}
```

✅ **Диагностика:** Устройство `device1` уже существует. Используйте другое имя или удалите существующее устройство.

#### Пример 4: Несуществующая переменная

**Запрос:**
```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.models.cluster",
    "name": "NON_EXISTENT_VARIABLE",
    "value": "test"
  }
}
```

**Ответ:**
```json
{
  "error": {
    "code": -32001,
    "message": "Failed to set variable: Переменная 'NON_EXISTENT_VARIABLE' недоступна в контексте 'users.admin.models.cluster'"
  }
}
```

✅ **Диагностика:** 
- Переменная `NON_EXISTENT_VARIABLE` не существует в контексте `users.admin.models.cluster`
- Создайте переменную сначала с помощью `aggregate_create_variable`

#### Пример 5: Неверное выражение функции

**Запрос:**
```json
{
  "tool": "aggregate_create_function",
  "parameters": {
    "path": "users.admin.models.cluster",
    "functionName": "testFunction",
    "functionType": 1,
    "expression": "invalid javascript syntax !!!"
  }
}
```

**Ответ:**
```json
{
  "error": {
    "code": -32001,
    "message": "Failed to create function: ... (детали ошибки парсинга выражения)"
  }
}
```

✅ **Диагностика:** Проблема в синтаксисе выражения. Проверьте правильность JavaScript/Expression синтаксиса.

### Рекомендации по работе с ошибками

1. **Всегда проверяйте поле `message`** - там содержится детальная информация о проблеме, включая:
   - Пути к контекстам и элементам
   - Имена переменных, событий, функций
   - Полную цепочку причин от сервера AggreGate
   - Сообщения на языке сервера (русский/английский)

2. **Используйте код ошибки для быстрой диагностики:**
   - `-32602` → Проблема в параметрах запроса
   - `-32001` → Проблема в контексте или элементах AggreGate
   - `-32000` → Проблема с соединением

3. **Обращайте внимание на цепочку ошибок** - сообщения могут содержать несколько уровней детализации:
   ```
   Failed to create variable: 
   → Удаленная ошибка: Variable already exists: device1Sine 
   → ContextException: Variable definition already exists
   ```

4. **Для отладки используйте поле `data`** (если оно включено в ответ):
   - Тип исключения
   - Stack trace для разработчиков
   - Дополнительные детали ошибки

5. **Проверяйте существование элементов перед операциями:**
   - Убедитесь, что контекст существует перед созданием переменных
   - Проверьте, что переменные созданы перед установкой значений
   - Убедитесь, что устройства подключены перед созданием привязок

6. **Используйте правильный формат данных:**
   - Формат переменных: `<name><T>` (TableFormat)
   - Формат ссылок: `.:имя` для текущего контекста, `{контекст:элемент}` для других
   - Правильный синтаксис выражений JavaScript/Expression

## Типичные ошибки и их решения

### ❌ Ошибка: "Переменные модели не созданы"

**Проблема**: Агент создал модель контекста, но забыл создать переменные модели.

**Решение**: Всегда после создания модели контекста вызывайте `aggregate_create_variable` для каждой необходимой переменной.

### ❌ Ошибка: "События модели не созданы"

**Проблема**: Агент создал модель контекста и переменные, но забыл создать события модели.

**Решение**: После создания переменных обязательно создайте события модели с помощью `aggregate_create_event`.

### ❌ Ошибка: "Привязки не созданы"

**Проблема**: Агент создал модель, переменные и события, но забыл создать привязки между устройствами и моделью.

**Решение**: Используйте `aggregate_execute_action` с действием `addBinding` для создания привязок. Если это не работает, создайте привязки вручную через веб-интерфейс AggreGate.

### ❌ Ошибка: "Неверный формат переменной"

**Проблема**: Формат переменной указан неправильно.

**Решение**: Используйте правильный синтаксис TableFormat:
- Для простых переменных: `<name><T>`, где `T` - тип (`S`=строка, `I`=целое, `L`=длинное, `D`=дробное, `B`=булево)
- Для сложных структур используйте полный синтаксис TableFormat

### ❌ Ошибка: "Правило не срабатывает автоматически"

**Проблема**: Правило или функция Expression создана, но не выполняется автоматически.

**Решение**: 
1. Убедитесь, что правило включено (enabled) в разделе "Rule Sets" модели контекста
2. Настройте триггеры правила так, чтобы оно срабатывало при изменении связанных переменных
3. Альтернативно: создайте функцию типа Expression и настройте её периодический вызов (например, каждые 1-5 секунд)

### ❌ Ошибка: "Виджет не отображает данные"

**Проблема**: Виджет создан, но не показывает данные.

**Решение**:
1. Убедитесь, что установлен Default Context для виджета в свойствах виджета
2. Проверьте, что XML шаблон виджета правильно настроен
3. Убедитесь, что переменные модели имеют значения (проверьте работу привязок)
4. Проверьте права доступа к переменным и контекстам

### ❌ Ошибка: "Привязки не обновляются"

**Проблема**: Привязки созданы, но значения не передаются в модель.

**Решение**:
1. Убедитесь, что в привязке установлено `onevent: true` для обновления при изменении
2. Проверьте правильность формата ссылок: `target: ".:имя_переменной"`, `expression: "{контекст:переменная}"`
3. Убедитесь, что устройства подключены и работают
4. Проверьте, что переменные устройств существуют и имеют значения

## Архитектура системы кластера

Пример архитектуры системы с моделью контекста кластера:

```
users.admin
├── devices
│   ├── device1 (Virtual Device)
│   │   └── sine, sawtooth, triangle (переменные)
│   ├── device2 (Virtual Device)
│   │   └── sine, sawtooth, triangle (переменные)
│   └── device3 (Virtual Device)
│       └── sine, sawtooth, triangle (переменные)
├── models
│   └── cluster (Model Context)
│       ├── device1Sine ← {users.admin.devices.device1:sine} (привязка)
│       ├── device2Sine ← {users.admin.devices.device2:sine} (привязка)
│       ├── device3Sine ← {users.admin.devices.device3:sine} (привязка)
│       ├── clusterStatus (вычисляется правилом)
│       ├── clusterAlarm (событие)
│       └── checkClusterStatus (правило/функция)
├── widgets
│   ├── clusterStatusWidget
│   └── deviceMetricsWidget
└── dashboards
    └── clusterDashboard
```

## Поток данных в системе

1. **Виртуальные устройства** генерируют значения переменных `sine`, `sawtooth`, `triangle`
2. **Привязки** передают значения `sine` в переменные модели:
   - `{users.admin.devices.device1:sine}` → `.:device1Sine`
   - `{users.admin.devices.device2:sine}` → `.:device2Sine`
   - `{users.admin.devices.device3:sine}` → `.:device3Sine`
3. **Правило `checkClusterStatus`** автоматически проверяет состояние:
   - Если все три `sine` > 0 → `clusterStatus = "ошибка"` + генерируется `clusterAlarm`
   - Иначе → `clusterStatus = "ОК"`
4. **Виджет `clusterStatusWidget`** отображает состояние кластера и устройств
5. **При клике на устройство** открывается `deviceMetricsWidget` с графиками метрик

## Формат ссылок в AggreGate

Используется правильный формат согласно документации AggreGate:

- **Ссылка на текущий контекст:** `.:имя_элемента`
- **Ссылка на другой контекст:** `{путь.к.контексту:имя_элемента}`
- **События:** `.:имя_события@` или `{контекст:событие@}`
- **Функции:** `.:имя_функции()` или `{контекст:функция()}`

Подробнее: [Формат ссылок в AggreGate](AGGREGATE_REFERENCES_FORMAT.md)

## Чек-лист создания модели контекста

При создании модели контекста убедитесь, что выполнены все следующие шаги:

- [ ] ✅ Подключение к серверу (`aggregate_connect`)
- [ ] ✅ Вход в систему (`aggregate_login`)
- [ ] ✅ Создание модели контекста (`aggregate_create_context`)
- [ ] ✅ **Создание переменных модели** (`aggregate_create_variable`) - **ОБЯЗАТЕЛЬНО**
- [ ] ✅ **Создание событий модели** (`aggregate_create_event`) - **ОБЯЗАТЕЛЬНО**
- [ ] ✅ Создание устройств (`aggregate_create_device`)
- [ ] ✅ **Создание привязок** (`aggregate_set_variable` с `bindings`) - **ОБЯЗАТЕЛЬНО**
- [ ] ✅ Создание правил/функций Expression (`aggregate_create_function`) - опционально
- [ ] ✅ Создание виджетов (`aggregate_create_widget`)
- [ ] ✅ Настройка XML шаблонов виджетов через веб-интерфейс
- [ ] ✅ Создание дашбордов (`aggregate_create_dashboard`)
- [ ] ✅ Настройка автозапуска дашборда при логине

## Пример полного сценария: Кластер виртуальных устройств

Вот пример полного сценария создания модели кластера с тремя виртуальными устройствами:

```json
// 1. Подключение к серверу
{"tool": "aggregate_connect", "parameters": {"host": "localhost", "port": 6460, "username": "admin", "password": "admin"}}
{"tool": "aggregate_login"}

// 2. Создание модели контекста
{"tool": "aggregate_create_context", "parameters": {"parentPath": "users.admin.models", "name": "cluster", "description": "Модель кластера виртуальных устройств"}}

// 3. Создание переменных модели (ОБЯЗАТЕЛЬНО!)
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.cluster", "variableName": "device1Sine", "format": "<value><D>", "description": "Значение переменной Sine Wave устройства 1"}}
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.cluster", "variableName": "device2Sine", "format": "<value><D>", "description": "Значение переменной Sine Wave устройства 2"}}
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.cluster", "variableName": "device3Sine", "format": "<value><D>", "description": "Значение переменной Sine Wave устройства 3"}}
{"tool": "aggregate_create_variable", "parameters": {"path": "users.admin.models.cluster", "variableName": "clusterStatus", "format": "<status><S>", "description": "Состояние кластера: ОК или ошибка", "writable": true}}

// 4. Создание событий модели (ОБЯЗАТЕЛЬНО!)
{"tool": "aggregate_create_event", "parameters": {"path": "users.admin.models.cluster", "eventName": "clusterAlarm", "format": "<message><S>", "description": "Тревога кластера при переходе в состояние ошибки", "level": 2, "historyStorageTime": 86400000}}

// 5. Создание устройств
{"tool": "aggregate_create_device", "parameters": {"username": "admin", "deviceName": "device1", "description": "Виртуальное устройство 1 кластера", "driverId": "com.tibbo.linkserver.plugin.device.virtual"}}
{"tool": "aggregate_create_device", "parameters": {"username": "admin", "deviceName": "device2", "description": "Виртуальное устройство 2 кластера", "driverId": "com.tibbo.linkserver.plugin.device.virtual"}}
{"tool": "aggregate_create_device", "parameters": {"username": "admin", "deviceName": "device3", "description": "Виртуальное устройство 3 кластера", "driverId": "com.tibbo.linkserver.plugin.device.virtual"}}

// 6. Создание привязок после устройств (ОБЯЗАТЕЛЬНО!)
// Используйте правильный формат: target: ".:имя_переменной", expression: "{контекст:переменная}"
{"tool": "aggregate_set_variable", "parameters": {"path": "users.admin.models.cluster", "name": "bindings", "value": {"recordCount": 3, "format": {"fields": [{"name": "target", "type": "S"}, {"name": "expression", "type": "S"}, {"name": "onevent", "type": "B"}]}, "records": [{"target": ".:device1Sine", "expression": "{users.admin.devices.device1:sine}", "onevent": true}, {"target": ".:device2Sine", "expression": "{users.admin.devices.device2:sine}", "onevent": true}, {"target": ".:device3Sine", "expression": "{users.admin.devices.device3:sine}", "onevent": true}]}}}

// 7. Создание функции для проверки состояния кластера
{"tool": "aggregate_create_function", "parameters": {"path": "users.admin.models.cluster", "functionName": "checkClusterStatus", "functionType": 1, "description": "Проверка состояния кластера", "expression": "if (device1Sine != null && device2Sine != null && device3Sine != null) { var oldStatus = clusterStatus; if (device1Sine > 0 && device2Sine > 0 && device3Sine > 0) { clusterStatus = \"ошибка\"; if (oldStatus != \"ошибка\") { fireEvent(\"clusterAlarm\", \"Кластер перешел в состояние ошибки: все устройства имеют sine > 0\"); } } else { clusterStatus = \"ОК\"; } }"}}

// 8. Создание виджетов
{"tool": "aggregate_create_widget", "parameters": {"parentPath": "users.admin.widgets", "name": "clusterStatusWidget", "description": "Виджет состояния кластера устройств", "defaultContext": "users.admin.models.cluster"}}
{"tool": "aggregate_create_widget", "parameters": {"parentPath": "users.admin.widgets", "name": "deviceMetricsWidget", "description": "Виджет графиков метрик устройства"}}

// 9. Создание дашборда
{"tool": "aggregate_create_dashboard", "parameters": {"parentPath": "users.admin.dashboards", "name": "clusterDashboard", "description": "Инструментальная панель состояния кластера", "layout": "dockable"}}
{"tool": "aggregate_add_dashboard_element", "parameters": {"path": "users.admin.dashboards.clusterDashboard", "name": "clusterStatusElement", "type": "launchWidget", "parameters": {"widgetPath": "users.admin.widgets.clusterStatusWidget", "contextPath": "users.admin.models.cluster"}}}
```

**Примечание:** После создания виджетов необходимо настроить их XML шаблоны через веб-интерфейс AggreGate (см. раздел "Шаг 8: Создание виджетов").

## Проверка работы системы

После создания всех компонентов проверьте:

1. ✅ Все устройства подключены и работают
2. ✅ Переменные модели обновляются через привязки
3. ✅ Правило или функция проверки состояния работает корректно
4. ✅ При изменении условий (например, все sine > 0):
   - Переменная состояния (`clusterStatus`) обновляется
   - Генерируется событие тревоги (`clusterAlarm`)
5. ✅ Виджет состояния кластера отображается корректно
6. ✅ При клике на устройство открывается виджет метрик
7. ✅ Дашборд автоматически открывается при логине (если настроено)

## Использованные инструменты MCP API

При создании системы кластера используются следующие функции MCP API:

- `aggregate_connect` - Подключение к серверу
- `aggregate_login` - Вход в систему
- `aggregate_create_context` - Создание модели контекста
- `aggregate_create_variable` - Создание переменных модели
- `aggregate_create_event` - Создание событий модели
- `aggregate_create_device` - Создание устройств
- `aggregate_set_variable` - Настройка привязок
- `aggregate_create_function` - Создание функций Expression
- `aggregate_create_widget` - Создание виджетов
- `aggregate_create_dashboard` - Создание дашборда
- `aggregate_add_dashboard_element` - Добавление элементов в дашборд

## Дополнительные ресурсы

- [Формат ссылок на контексты, переменные, события и функции](AGGREGATE_REFERENCES_FORMAT.md) - **ВАЖНО**: Правильный синтаксис ссылок в AggreGate
- [Полное руководство по MCP серверу](mcp-server/COMPLETE_GUIDE.md)
- [Лучшие практики AggreGate](manual/AGGREGATE_BEST_PRACTICES.md)
- [Описание контекстов AggreGate](manual/AGGREGATE_CONTEXTS_DESCRIPTION.md)

## Заключение

Помните: **переменные модели, события модели и привязки НЕ создаются автоматически**. Их нужно создавать явно после создания модели контекста. Соблюдение правильного порядка операций гарантирует корректную работу модели контекста в AggreGate.

