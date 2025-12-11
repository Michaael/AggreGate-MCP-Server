# Полное руководство по MCP серверу AggreGate

## Содержание

1. [Введение](#введение)
2. [Установка и настройка](#установка-и-настройка)
3. [Сборка проекта](#сборка-проекта)
4. [Доступные инструменты](#доступные-инструменты)
5. [Работа с контекстами](#работа-с-контекстами)
6. [Работа с переменными](#работа-с-переменными)
7. [Работа с функциями](#работа-с-функциями)
8. [Работа с устройствами](#работа-с-устройствами)
9. [Работа с моделями](#работа-с-моделями)
10. [Работа с виджетами и дашбордами](#работа-с-виджетами-и-дашбордами)
11. [Работа с алертами](#работа-с-алертами)
12. [Язык выражений AggreGate](#язык-выражений-aggregate)
13. [Форматы переменных](#форматы-переменных)
14. [Драйверы устройств](#драйверы-устройств)
15. [Стандарты разработки](#стандарты-разработки)
16. [Тестирование](#тестирование)
17. [Примеры использования](#примеры-использования)

---

## Введение

MCP (Model Context Protocol) сервер для интеграции Cursor IDE с платформой Tibbo AggreGate.

Этот MCP сервер предоставляет набор инструментов для работы с AggreGate сервером через Cursor IDE. Он позволяет:

- Подключаться к AggreGate серверу
- Управлять контекстами, переменными и функциями
- Создавать и управлять устройствами
- Управлять пользователями
- Работать с агентами и событиями
- Выполнять серверные действия

### Требования

- Java 8 или выше
- AggreGate SDK 6.34.06
- Cursor IDE с поддержкой MCP

---

## Установка и настройка

### Автоматическая настройка

Конфигурационный файл уже создан в `.cursor/mcp.json` в корне проекта. Если вы хотите использовать его глобально:

**Windows:**
```powershell
Copy-Item ".cursor\mcp.json" "$env:USERPROFILE\.cursor\mcp.json"
```

**Linux/Mac:**
```bash
cp .cursor/mcp.json ~/.cursor/mcp.json
```

### Ручная настройка

1. Создайте директорию `.cursor` в вашей домашней папке (если её нет):
   - Windows: `%USERPROFILE%\.cursor\`
   - Linux/Mac: `~/.cursor/`

2. Создайте файл `mcp.json` в этой директории:

```json
{
  "mcpServers": {
    "aggregate": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\Users\\Михаил\\Downloads\\aggregate_sdk_6.34.06\\mcp-server\\build\\libs\\aggregate-mcp-server-1.0.0.jar"
      ]
    }
  }
}
```

**Важно:** Замените путь к JAR файлу на актуальный путь на вашей системе.

3. Перезапустите Cursor IDE

### Проверка установки

После перезапуска Cursor:
1. Откройте чат в Cursor
2. Попробуйте использовать инструмент `aggregate_connect` для подключения к AggreGate серверу
3. Если инструменты не видны, проверьте:
   - Что Java установлена и доступна в PATH: `java -version`
   - Что путь к JAR файлу правильный
   - Что файл `mcp.json` находится в правильной директории

---

## Сборка проекта

### Требования для сборки

1. **Java Development Kit (JDK)**
   - Требуется: Java 8 или выше
   - Проверка: `java -version` и `javac -version`
   - Установка переменной `JAVA_HOME` (если требуется):
     ```powershell
     $env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_XXX"
     ```

2. **Библиотеки в `libs/` директории:**
   - `aggregate-api.jar`
   - `aggregate-api-libs.jar`
   - `jackson-core-2.20.1.jar`
   - `jackson-databind-2.20.1.jar`
   - `jackson-annotations-2.20.jar` (совместима с 2.20.1)

### Команды сборки

```powershell
# Установите JAVA_HOME (если нужно)
$env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_XXX"

# Соберите проект
./gradlew :mcp-server:build --no-daemon

# Или с очисткой
./gradlew :mcp-server:clean :mcp-server:build --no-daemon
```

JAR файл будет создан в `mcp-server/build/libs/aggregate-mcp-server-1.0.0.jar`

### Возможные проблемы при сборке

**Проблема: "Could not find tools.jar"**
- Решение: Установите `JAVA_HOME`

**Проблема: "Gradle build failed"**
- Проверьте, что все JAR файлы в `libs/` на месте
- Проверьте версию Java: `java -version`
- Попробуйте очистить и пересобрать: `./gradlew clean :mcp-server:build --no-daemon`

**Проблема: "Jackson version mismatch"**
- Версии Jackson должны быть совместимы. Текущие версии (2.20/2.20.1) совместимы.

---

## Доступные инструменты

### Подключение

- **aggregate_connect** - Подключиться к AggreGate серверу
- **aggregate_disconnect** - Отключиться от сервера
- **aggregate_login** - Войти в систему

### Контексты

- **aggregate_get_context** - Получить контекст по пути
- **aggregate_list_contexts** - Список контекстов по маске
- **aggregate_create_context** - Создать контекст (модель)
- **aggregate_delete_context** - Удалить контекст

### Переменные

- **aggregate_get_variable** - Получить значение переменной
- **aggregate_set_variable** - Установить значение переменной
- **aggregate_set_variable_field** - Установить поле переменной
- **aggregate_list_variables** - Список переменных контекста
- **aggregate_create_variable** - Создать переменную (включая модели)

### Функции

- **aggregate_call_function** - Вызвать функцию контекста
- **aggregate_list_functions** - Список доступных функций
- **aggregate_create_function** - Создать функцию (включая модели)

### Устройства

- **aggregate_create_device** - Создать устройство
- **aggregate_list_devices** - Список устройств пользователя
- **aggregate_delete_device** - Удалить устройство
- **aggregate_get_device_status** - Получить статус устройства

### Пользователи

- **aggregate_create_user** - Создать пользователя
- **aggregate_list_users** - Список пользователей
- **aggregate_delete_user** - Удалить пользователя
- **aggregate_update_user** - Обновить информацию пользователя

### События

- **aggregate_fire_event** - Отправить событие от агента
- **aggregate_create_event** - Создать событие (включая модели)

### Действия

- **aggregate_execute_action** - Выполнить серверное действие

### Агенты

- **aggregate_create_agent** - Создать и подключить агента
- **aggregate_agent_get_status** - Получить статус агента

### Виджеты и дашборды

- **aggregate_create_widget** - Создать виджет
- **aggregate_set_widget_template** - Установить XML шаблон виджета
- **aggregate_create_dashboard** - Создать дашборд
- **aggregate_add_dashboard_element** - Добавить элемент в дашборд

---

## Работа с контекстами

### Типы контекстов в AggreGate

В AggreGate существует множество типов контекстов:

- **devices** - устройства (`users.{username}.devices.{deviceName}`)
- **users** - пользователи (`users.{username}`)
- **models** - модели (`users.{username}.models.{modelName}`)
- **widgets** - виджеты (`users.{username}.widgets.{widgetName}`)
- **dashboards** - дашборды (`users.{username}.dashboards.{dashboardName}`)
- **alerts** - алерты (`users.{username}.alerts.{alertName}`)
- **queries** - запросы (`users.{username}.queries.{queryName}`)
- **jobs** - задания (`users.{username}.jobs.{jobName}`)
- **workflows** - рабочие процессы (`users.{username}.workflows.{workflowName}`)
- **scripts** - скрипты (`users.{username}.scripts.{scriptName}`)
- **reports** - отчеты (`users.{username}.reports.{reportName}`)

### Синтаксис путей контекста

#### В ссылках (References)
- `{.:variableName}` - текущий контекст (точка используется только в ссылках!)
- `{users.admin.models.cluster:variableName}` - явный путь к контексту

#### В строках (String parameters)
- ❌ **Неправильно**: `'.'` - точка не работает как строка
- ✅ **Правильно**: `'users.admin.models.cluster'` - полный путь контекста

#### В ссылках для функций
- ✅ `{.:}` - ссылка на текущий контекст (разрешится в путь)
- ✅ `{users.admin.models.cluster}` - ссылка на явный контекст (разрешится в путь)

### Примеры работы с контекстами

```json
// Получение контекста
{
  "name": "aggregate_get_context",
  "arguments": {
    "path": "users.admin.models"
  }
}

// Список контекстов
{
  "name": "aggregate_list_contexts",
  "arguments": {
    "mask": "users.*.models.*"
  }
}

// Создание контекста (модели)
{
  "name": "aggregate_create_context",
  "arguments": {
    "parentPath": "users.admin.models",
    "name": "myModel",
    "description": "My Model"
  }
}
```

---

## Работа с переменными

### Форматы переменных

AggreGate использует следующие типы полей:

- `I` - Integer (целое число)
- `S` - String (строка)
- `B` - Boolean (булево значение)
- `L` - Long (длинное целое число)
- `F` - Float (число с плавающей точкой)
- `E` - Double (число двойной точности) ⚠️ **НЕ 'D'!**
- `D` - Date (дата/время) ⚠️ **НЕ для double!**
- `T` - DataTable (таблица данных)
- `C` - Color (цвет)
- `A` - Data (блок данных)

### Формат для простых переменных (одно значение)

Для переменных, которые хранят одно значение (не таблицу), используется формат:
- `minRecords = 1`
- `maxRecords = 1`
- Одно поле с нужным типом

#### Правильные форматы

**Double (число с плавающей точкой):**
```
<value><E>
```
**Важно**: Используется `E` (DOUBLE_FIELD), а не `D` (DATE_FIELD)!

**String (строка):**
```
<value><S>
```

**Boolean (булево значение):**
```
<value><B>
```

**Integer (целое число):**
```
<value><I>
```

**Date (дата/время):**
```
<value><D>
```

### Формат для таблиц (множество записей)

Для переменных, которые хранят таблицу данных:
```
<M=0><X=2147483647><field1><S><field2><I>
```

### Примеры работы с переменными

```json
// Получение переменной
{
  "name": "aggregate_get_variable",
  "arguments": {
    "path": "users.admin",
    "name": "status"
  }
}

// Установка переменной
{
  "name": "aggregate_set_variable",
  "arguments": {
    "path": "users.admin.models.myModel",
    "name": "result",
    "value": {
      "format": {
        "fields": [{"name": "value", "type": "E"}]
      },
      "records": [{"value": 123.45}]
    }
  }
}

// Создание переменной в модели
{
  "name": "aggregate_create_variable",
  "arguments": {
    "path": "users.admin.models.myModel",
    "variableName": "myVariable",
    "format": "<value><E>",
    "description": "My variable",
    "writable": false
  }
}
```

---

## Работа с функциями

### Типы функций

- `FUNCTION_TYPE_JAVA` (0) - Java-функция
- `FUNCTION_TYPE_EXPRESSION` (1) - функция-выражение
- `FUNCTION_TYPE_QUERY` (2) - функция-запрос

### Примеры работы с функциями

```json
// Вызов функции
{
  "name": "aggregate_call_function",
  "arguments": {
    "path": "users.admin.models.calculator",
    "functionName": "calculate",
    "input": {
      "format": {
        "fields": [
          {"name": "arg1", "type": "E"},
          {"name": "arg2", "type": "E"},
          {"name": "operator", "type": "S"}
        ]
      },
      "records": [{
        "arg1": 10,
        "arg2": 20,
        "operator": "add"
      }]
    }
  }
}

// Создание функции в модели
{
  "name": "aggregate_create_function",
  "arguments": {
    "path": "users.admin.models.myModel",
    "functionName": "myFunction",
    "inputFormat": "<arg1><E><arg2><E>",
    "outputFormat": "<result><E>",
    "functionType": 1,
    "expression": "{arg1} + {arg2}"
  }
}
```

---

## Работа с устройствами

### Создание устройства

```json
{
  "name": "aggregate_create_device",
  "arguments": {
    "username": "admin",
    "deviceName": "myDevice",
    "description": "My Test Device",
    "driverId": "com.tibbo.linkserver.plugin.device.virtual"
  }
}
```

### Статусы устройств

#### Статусы подключения
- `CONNECTION_STATUS_OFFLINE` (0) - устройство офлайн
- `CONNECTION_STATUS_ONLINE` (1) - устройство онлайн
- `CONNECTION_STATUS_SUSPENDED` (2) - устройство приостановлено
- `CONNECTION_STATUS_UNKNOWN` (3) - статус неизвестен
- `CONNECTION_STATUS_MAINTENANCE` (4) - устройство в режиме обслуживания

#### Статусы синхронизации
- `SYNC_STATUS_OK` (20) - синхронизация успешна
- `SYNC_STATUS_WAITING` (30) - ожидание синхронизации
- `SYNC_STATUS_ERROR` (40) - ошибка синхронизации
- `SYNC_STATUS_UNDEFINED` (50) - статус не определен
- `SYNC_STATUS_CONNECTING` (70) - подключение
- `SYNC_STATUS_READING_METADATA` (80) - чтение метаданных
- `SYNC_STATUS_SYNCHRONIZING_SETTINGS` (90) - синхронизация настроек

Общий статус вычисляется как сумма статуса подключения и статуса синхронизации:
- `21` = ONLINE (1) + OK (20) - устройство онлайн и синхронизировано
- `71` = ONLINE (1) + CONNECTING (70) - устройство онлайн и подключается

### Примеры работы с устройствами

```json
// Получение статуса устройства
{
  "name": "aggregate_get_device_status",
  "arguments": {
    "username": "admin",
    "deviceName": "device1"
  }
}

// Список устройств
{
  "name": "aggregate_list_devices",
  "arguments": {
    "username": "admin"
  }
}
```

Подробнее о драйверах устройств см. раздел [Драйверы устройств](#драйверы-устройств).

---

## Работа с моделями

Модели представляют виртуальные контексты для создания бизнес-логики, вычислений и обработки данных.

### Основные переменные модели

- `modelVariables` - переменные модели
- `modelFunctions` - функции модели
- `modelEvents` - события модели
- `ruleSets` - наборы правил
- `bindings` - привязки (связи между переменными)
- `threadPoolStatus` - статус пула потоков

### Создание привязок (bindings)

Привязки используются для автоматического вычисления значений переменных на основе выражений.

#### ⚠️ Важно: Формат target в привязках

**Правила для поля `target`:**
- ❌ **НЕ используйте** фигурные скобки `{}` и символ `@`
- ✅ Для переменной в **текущем контексте**: `.:variableName`
- ✅ Для переменной в **другом контексте**: `context:variableName` (полный путь)
- ✅ Для **генерации события**: пустая строка `''`

**Правила для поля `expression`:**
- ✅ Используйте относительные ссылки с фигурными скобками:
  - `{.:variable}` - для текущего контекста
  - `{context:variable}` - для других контекстов

**Примеры:**

```json
{
  "name": "aggregate_set_variable",
  "arguments": {
    "path": "users.admin.models.myModel",
    "name": "bindings",
    "value": {
      "format": {
        "fields": [
          {"name": "target", "type": "S"},
          {"name": "expression", "type": "S"},
          {"name": "onstartup", "type": "B"},
          {"name": "onevent", "type": "B"},
          {"name": "periodically", "type": "B"},
          {"name": "period", "type": "L"}
        ]
      },
      "records": [
        {
          "target": ".:myVariable",
          "expression": "{users.admin.devices.device1:sine}",
          "onstartup": true,
          "onevent": true,
          "periodically": true,
          "period": 1000
        },
        {
          "target": ".:result",
          "expression": "{.:myVariable} * 2",
          "onstartup": true,
          "onevent": true,
          "periodically": false,
          "period": 0
        },
        {
          "target": "",
          "expression": "{.:isError} && !{.:previousIsError} ? fireEvent('users.admin.models.myModel', 'errorEvent', 2, table('<<message><S>>', 'Error occurred')) : null",
          "onstartup": false,
          "onevent": true,
          "periodically": true,
          "period": 1000
        }
      ]
    }
  }
}
```

**Неправильно:**
- ❌ `"target": "myVariable@"` - не используйте `@`
- ❌ `"target": "{myVariable}"` - не используйте `{}`
- ❌ `"target": "{.:myVariable}"` - не используйте `{}`

**Правильно:**
- ✅ `"target": ".:myVariable"` - для текущего контекста
- ✅ `"target": "users.admin.models.otherModel:variable"` - для другого контекста
- ✅ `"target": ""` - для генерации события

### Пример создания модели

1. Создать модель: `aggregate_create_context`
2. Создать переменные: `aggregate_create_variable`
3. Создать привязки: `aggregate_set_variable` (bindings)
4. Создать событие: `aggregate_create_event`
5. Создать привязку для тревоги: `aggregate_set_variable` (bindings)

---

## Работа с виджетами и дашбордами

### Создание виджета

```json
{
  "name": "aggregate_create_widget",
  "arguments": {
    "parentPath": "users.admin.widgets",
    "name": "myWidget",
    "description": "My widget",
    "defaultContext": "users.admin.models.myModel"
  }
}
```

### Установка XML шаблона виджета

```json
{
  "name": "aggregate_set_widget_template",
  "arguments": {
    "path": "users.admin.widgets.myWidget",
    "template": "<?xml version=\"1.0\"?><widget>...</widget>"
  }
}
```

### Создание дашборда

```json
{
  "name": "aggregate_create_dashboard",
  "arguments": {
    "parentPath": "users.admin.dashboards",
    "name": "myDashboard",
    "description": "My dashboard",
    "layout": "dockable"
  }
}
```

### Добавление элемента в дашборд

```json
{
  "name": "aggregate_add_dashboard_element",
  "arguments": {
    "path": "users.admin.dashboards.myDashboard",
    "name": "clusterStatus",
    "type": "launchWidget",
    "parameters": {
      "format": {
        "fields": [
          {"name": "widgetContext", "type": "S"},
          {"name": "defaultContext", "type": "S"}
        ]
      },
      "records": [{
        "widgetContext": "users.admin.widgets.clusterStatus",
        "defaultContext": "users.admin.models.cluster"
      }]
    }
  }
}
```

### Типы раскладки дашборда

- `dockable` - док-панель (по умолчанию)
- `scrollable` - прокручиваемая
- `grid` - сетка
- `absolute` - абсолютная

### Типы элементов дашборда

- `launchWidget` - запуск виджета
- `showEventLog` - журнал событий
- `showSystemTree` - системное дерево
- `editData` - редактирование данных
- `editProperties` - редактирование свойств
- `showReport` - показ отчета
- `activateDashboard` - активация дашборда
- `showHtmlSnippet` - HTML-фрагмент

### Автоматическое открытие дашборда

Используйте `aggregate_execute_action` для выполнения действия `open`:

```json
{
  "name": "aggregate_execute_action",
  "arguments": {
    "path": "users.admin.dashboards.clusterDashboard",
    "actionName": "open",
    "input": {
      "format": {
        "fields": [
          {"name": "name", "type": "S"},
          {"name": "location", "type": "T"}
        ]
      },
      "records": [{
        "name": "clusterDashboard",
        "location": {
          "format": {
            "fields": [
              {"name": "state", "type": "S"},
              {"name": "side", "type": "S"}
            ]
          },
          "records": [{
            "state": "docked",
            "side": "top"
          }]
        }
      }]
    }
  }
}
```

---

## Работа с алертами

### Создание алерта для события

Алерт создается через действие `createForEvent` в контексте `users.{username}.alerts`.

```json
{
  "name": "aggregate_execute_action",
  "arguments": {
    "path": "users.admin.alerts",
    "actionName": "createForEvent",
    "input": {
      "format": {
        "fields": [
          {"name": "context", "type": "S"},
          {"name": "event", "type": "S"},
          {"name": "description", "type": "S"}
        ]
      },
      "records": [{
        "context": "users.admin.models.cluster",
        "event": "clusterError",
        "description": "Cluster Error Alert"
      }]
    }
  }
}
```

### Основные переменные алерта

- `eventTriggers` - триггеры событий
- `notifications` - уведомления
- `status` - статус алерта
- `alertActions` - действия алерта
- `activeInstances` - активные экземпляры

### Статусы алерта

- `STATUS_ENABLED` (0) - включена
- `STATUS_DISABLED` (1) - отключена
- `STATUS_ACTIVE` (2) - активна
- `STATUS_ESCALATED` (3) - эскалирована

### Настройка триггеров событий

```json
{
  "name": "aggregate_set_variable",
  "arguments": {
    "path": "users.admin.alerts.clusterError",
    "name": "eventTriggers",
    "value": {
      "format": {
        "fields": [
          {"name": "mask", "type": "S"},
          {"name": "event", "type": "S"},
          {"name": "level", "type": "I"},
          {"name": "count", "type": "I"},
          {"name": "period", "type": "L"},
          {"name": "message", "type": "S"}
        ]
      },
      "records": [{
        "mask": "users.admin.models.cluster",
        "event": "clusterError",
        "level": 2,
        "count": 1,
        "period": 0,
        "message": "Cluster error: all devices sine > 0"
      }]
    }
  }
}
```

---

## Язык выражений AggreGate

### Обзор

Язык выражений AggreGate используется в:
- **Привязках (Bindings)** - для автоматического вычисления значений переменных
- **Функциях типа Expression** - для создания функций на основе выражений
- **Правилах (Rules)** - для условной логики в моделях
- **Виджетах и дашбордах** - для динамического отображения данных

### Базовый синтаксис

Выражения могут содержать:
- **Литералы**: числа (`123`, `45.67`), строки (`"text"`), булевы значения (`true`, `false`), `null`
- **Ссылки на объекты**: `{context:variable}`, `{context:function(param1, param2)}`
- **Операторы**: арифметические (`+`, `-`, `*`, `/`), логические (`&&`, `||`, `!`), сравнения (`==`, `!=`, `<`, `>`, `<=`, `>=`)
- **Функции**: встроенные функции (`if()`, `sum()`, `getVariable()`, и т.д.)

### Ссылки на объекты (References)

**Общий формат**: `schema/server^context:entity('param1', 'param2', ...)$field[row]#property`

Где:
- `schema/` - схема (опционально): `form/`, `web/`, `table/`, `env/`, `parent/`
- `server^` - сервер (опционально)
- `context:` - путь к контексту
- `entity` - имя переменной, функции, события или действия
- `('param1', ...)` - параметры (для функций и действий)
- `$field` - поле в таблице данных
- `[row]` - номер строки (0-based)
- `#property` - свойство объекта

#### Примеры ссылок

```javascript
// Переменная в текущем контексте
{.:myVariable}

// Переменная в другом контексте
{users.admin.models.calculator:result}

// Поле в таблице переменной
{users.admin.devices.device1:status$value}

// Конкретная строка таблицы
{users.admin.devices.device1:data$temperature[0]}

// Вызов функции
{users.admin.models.calculator:calculate({arg1}, {arg2}, {operator})}

// Ссылка на событие
{users.admin.devices.device1:alarm@}

// Вызов действия
{users.admin.devices.device1:restart!}
```

### Функции для работы с контекстами

#### `getVariable(context, variable)`
Получить значение переменной из указанного контекста.

```javascript
getVariable("users.admin.models.calculator", "result")
```

#### `setVariable(context, variable, value)`
Установить значение переменной в указанном контексте.

```javascript
setVariable("users.admin.models.calculator", "result", 100)
```

#### `callFunction(context, function, param1, param2, ...)`
Вызвать функцию в указанном контексте.

```javascript
callFunction("users.admin.models.calculator", "calculate", 10, 20, "add")
```

#### `fireEvent(context, event, level, data)`
Отправить событие из указанного контекста.

```javascript
fireEvent("users.admin.models.cluster", "clusterError", 2, table("<<message><S>>", "Cluster error"))
```

**Важно**: 
- Контекст должен быть полным путем строкой: `'users.admin.models.cluster'` или ссылкой: `{.:}` или `{users.admin.models.cluster}`
- ❌ Не используйте `'.'` как строку - это не работает!
- Для `table()` формат должен быть в двойных угловых скобках: `'<<message><S>>'`

### Условные выражения

```javascript
// Тернарный оператор
{.:value} > 0 ? {.:value} : 0

// Функция if
if({status} == "active", 1, 0)

// Функция case
case({type}, "A", 1, "B", 2, 0)
```

### Примеры выражений

```javascript
// Арифметические операции
{arg1} + {arg2}
{price} * {quantity}

// Логические операции
{status} == "active" && {value} > 100
{enabled} || {force}

// Условные выражения
{.:isError} ? 'ERROR' : 'OK'

// Вызов функции в выражении
{calculate({arg1}, {arg2}, {operator})}
```

---

## Форматы переменных

### Типы полей

- `I` - Integer (целое число)
- `S` - String (строка)
- `B` - Boolean (булево значение)
- `L` - Long (длинное целое число)
- `F` - Float (число с плавающей точкой)
- `E` - Double (число двойной точности) ⚠️ **НЕ 'D'!**
- `D` - Date (дата/время) ⚠️ **НЕ для double!**
- `T` - DataTable (таблица данных)
- `C` - Color (цвет)
- `A` - Data (блок данных)

### Важные замечания

1. **Для простых переменных** (одно значение) используйте `(1, 1)` - одна запись, одно поле
2. **Для double** используйте `E`, а не `D`
3. **Для date** используйте `D`
4. **Формат должен соответствовать типу данных**, который будет записываться в переменную

### Примеры форматов

#### Простая переменная (одно значение)
```
<value><E>
```

#### Переменная с указанием min/max в строке
```
<M=1><X=1><value><E>
```

#### Таблица (множество записей)
```
<M=0><X=2147483647><name><S><value><E>
```

---

## Драйверы устройств

### Виртуальное устройство

**Идентификатор драйвера:** `com.tibbo.linkserver.plugin.device.virtual`

**Описание:** Виртуальное устройство для тестирования и разработки.

#### Специфичные переменные

- `sine` (Double) - синусоидальная волна (значение от -1 до 1)
- `sawtooth` (Double) - пилообразная волна
- `triangle` (Double) - треугольная волна
- `int` (Integer) - целочисленная переменная
- `table` (DataTable) - табличная переменная

#### Специфичные функции

- `generateEvent(eventName, level, message, data)` - генерация тестового события

### Сетевые устройства

**Типы подключения:**
- TCP (MODE_TCP = 0)
- UDP (MODE_UDP = 1)

#### Переменные подключения (connectionProperties)

- `address` (String) - IP-адрес или hostname устройства
- `port` (Integer) - порт подключения
- `mode` (Integer) - режим подключения (0 = TCP, 1 = UDP)
- `timeout` (Long) - таймаут подключения в миллисекундах

### Устройства последовательного порта

**Тип подключения:** Serial (MODE_SERIAL = 2)

#### Переменные подключения

- `portName` (String) - имя COM-порта (например, "COM1", "/dev/ttyUSB0")
- `baudRate` (Integer) - скорость передачи данных
- `databits` (Integer) - количество бит данных
- `stopbits` (Integer) - количество стоп-битов
- `parity` (Integer) - контроль четности
- `timeout` (Long) - таймаут в миллисекундах

### Общие переменные устройств

Все устройства имеют следующие общие переменные:

- `status` (DataTable) - статус устройства
- `connectionProperties` - свойства подключения
- `genericProperties` - общие свойства устройства
- `variablesCache` - кэш переменных
- `functionsCache` - кэш функций
- `eventsCache` - кэш событий

### Общие функции устройств

- `reset()` - сброс устройства
- `synchronize(variable)` - синхронизация конкретной переменной

---

## Стандарты разработки

### Именование объектов

#### Используйте стиль camelCase

- **Имя контекста:** `monitoringMainPage`, `mqttDevice`
- **Имя функции:** `getUserInformation`, `xmlHttpRequest`
- **Имя переменной:** `inputData`, `unitsList`

#### Давайте объектам понятные имена

**Дашборды:**
- `startPage` - стартовая страница
- `monitoringSettings` - настройки мониторинга
- `alertsHistory` - история алертов

**Функции:**
- ❌ **Плохо:** `findUser1`, `findUser2`
- ✅ **Хорошо:** `findUserByName`, `findUserById`

#### Используйте соответствующие глаголы для функций

- `createSensor` - создать датчик
- `getUserInformation` - получить информацию о пользователе
- `sendAlertsListByEmail` - отправить список алертов по email

### Оформление кода выражений

#### Располагайте параметры функции в столбик

**❌ Плохо:**
```javascript
buildTable(sourceTable, filterExpression, sortField, sortDirection, limit, offset)
```

**✅ Хорошо:**
```javascript
buildTable(
  , sourceTable
  , filterExpression
  , sortField
  , sortDirection
  , limit
  , offset
)
```

#### Разбивайте длинные строки

**❌ Плохо:**
```javascript
"SELECT * FROM devices WHERE status = 'online' AND type = 'sensor' AND location = 'building1'"
```

**✅ Хорошо:**
```javascript
"SELECT * FROM devices"
+ " WHERE status = 'online'"
+ " AND type = 'sensor'"
+ " AND location = 'building1'"
```

#### Используйте табуляцию для вложенных функций

**❌ Плохо:**
```javascript
callFunction("users.admin.models.processor", "processData", getVariable("users.admin.devices.device1", "data"), filterExpression, sortField)
```

**✅ Хорошо:**
```javascript
callFunction(
  , "users.admin.models.processor"
  , "processData"
  , getVariable(
      , "users.admin.devices.device1"
      , "data"
    )
  , filterExpression
  , sortField
)
```

### Описания и комментарии

- Заполняйте поле описание для всех объектов
- Комментарии должны отражать суть действия, а не описывать каждую операцию
- Не оставляйте закомментированные фрагменты кода после отладки

### Использование справочников

Используйте справочники и развязочные таблицы вместо прямых ссылок и констант:

```javascript
// Вместо константы
getVariable("users.admin.models.units", "unitsList")
```

---

## Тестирование

### Таймауты

MCP сервер использует таймауты для предотвращения зависаний:

- **Подключение:** 30 секунд
- **Операции** (login, get context, list contexts, etc.): 60 секунд

### Примеры тестов

#### Тест подключения

```json
{
  "method": "tools/call",
  "params": {
    "name": "aggregate_connect",
    "arguments": {
      "host": "localhost",
      "port": 6460,
      "username": "admin",
      "password": "admin"
    }
  }
}
```

#### Тест получения списка контекстов

```json
{
  "method": "tools/call",
  "params": {
    "name": "aggregate_list_contexts",
    "arguments": {
      "mask": "users.*.models.*"
    }
  }
}
```

### Логирование

Все операции логируются в stderr с префиксом `[MCP]`:
- `[MCP]` - информационные сообщения
- `[MCP ERROR]` - сообщения об ошибках

---

## Примеры использования

### Пример 1: Подключение к серверу

```json
{
  "name": "aggregate_connect",
  "arguments": {
    "host": "localhost",
    "port": 6460,
    "username": "admin",
    "password": "admin"
  }
}
```

### Пример 2: Получение переменной

```json
{
  "name": "aggregate_get_variable",
  "arguments": {
    "path": "users.admin",
    "name": "status"
  }
}
```

### Пример 3: Создание устройства

```json
{
  "name": "aggregate_create_device",
  "arguments": {
    "username": "admin",
    "deviceName": "myDevice",
    "description": "My Test Device",
    "driverId": "com.tibbo.linkserver.plugin.device.virtual"
  }
}
```

### Пример 4: Создание модели кластера

1. Создать модель: `aggregate_create_context`
2. Создать переменные: `aggregate_create_variable`
3. Создать привязки: `aggregate_set_variable` (bindings)
4. Создать событие: `aggregate_create_event`
5. Создать привязку для тревоги: `aggregate_set_variable` (bindings)

### Пример 5: Создание виджета состояния кластера

1. Создать виджет: `aggregate_create_widget`
2. Установить шаблон: `aggregate_set_widget_template`
3. Добавить в дашборд: `aggregate_add_dashboard_element`

### Пример 6: Полная привязка с генерацией события

```json
{
  "name": "aggregate_set_variable",
  "arguments": {
    "path": "users.admin.models.cluster",
    "name": "bindings",
    "value": {
      "format": {
        "fields": [
          {"name": "target", "type": "S"},
          {"name": "expression", "type": "S"},
          {"name": "onstartup", "type": "B"},
          {"name": "onevent", "type": "B"},
          {"name": "periodically", "type": "B"},
          {"name": "period", "type": "L"}
        ]
      },
      "records": [{
        "target": "",
        "expression": "{.:isError} && !{.:previousIsError} ? fireEvent('users.admin.models.cluster', 'clusterError', 2, table('<<message><S>>', 'Cluster error: all devices sine > 0')) : null",
        "onstartup": false,
        "onevent": true,
        "periodically": true,
        "period": 1000
      }]
    }
  }
}
```

**Важно:**
- `target` - пустая строка `""` для генерации события
- `expression` - использует относительные ссылки `{.:variable}` для вычислений
- Или с использованием ссылки на текущий контекст в fireEvent:

```javascript
{.:isError} && !{.:previousIsError} ? fireEvent({.:}, 'clusterError', 2, table('<<message><S>>', 'Cluster error: all devices sine > 0')) : null
```

---

## Ограничения

1. **Асинхронные события**: MCP через stdio не поддерживает push-уведомления. События можно получать только через polling.

2. **Множественные соединения**: По умолчанию используется одно соединение. Можно использовать параметр `connectionKey` для работы с несколькими серверами.

3. **Безопасность**: Учетные данные передаются через параметры инструментов. Не храните пароли в конфигурационных файлах.

---

## Разработка

Проект использует Gradle для сборки. Основные модули:

- `mcp-server` - MCP сервер
- `aggregate-api` - AggreGate SDK API

---

## Лицензия

См. лицензию AggreGate SDK.

