# Руководство по использованию AggreGate: Лучшие практики и архитектура

## Содержание

1. [Введение](#введение)
2. [Архитектура AggreGate](#архитектура-aggregate)
3. [Типы контекстов в AggreGate](#типы-контекстов-в-aggregate)
4. [Модели (Models)](#модели-models)
5. [Переменные (Variables)](#переменные-variables)
6. [Функции (Functions)](#функции-functions)
7. [События (Events)](#события-events)
8. [Привязки (Bindings)](#привязки-bindings)
9. [Наборы правил (Rule Sets)](#наборы-правил-rule-sets)
10. [Устройства (Devices)](#устройства-devices)
11. [Виджеты (Widgets)](#виджеты-widgets)
12. [Дашборды (Dashboards)](#дашборды-dashboards)
13. [Алерты (Alerts)](#алерты-alerts)
14. [Запросы (Queries)](#запросы-queries)
15. [Задания (Jobs)](#задания-jobs)
16. [Рабочие процессы (Workflows)](#рабочие-процессы-workflows)
17. [Скрипты (Scripts)](#скрипты-scripts)
18. [Отчеты (Reports)](#отчеты-reports)
19. [Язык выражений AggreGate](#язык-выражений-aggregate)
20. [Лучшие практики разработки](#лучшие-практики-разработки)

---

## Введение

AggreGate - это платформа для управления устройствами и данными IoT, которая использует иерархическую структуру контекстов для организации логики приложения. Каждый контекст может содержать переменные, функции, события и действия.

### О проекте

Этот проект представляет собой **AggreGate SDK** с демонстрационным проектом управления дата-центром. На сервере развернут полноценный пример IoT приложения, демонстрирующий:

- **35+ моделей** для различных аспектов мониторинга (CPU, память, диск, сеть, виртуализация и т.д.)
- **Демонстрационный проект управления дата-центром** (`dataCenterManagementDemo`) с:
  - Мониторингом климатических параметров (температура, влажность)
  - Мониторингом систем резервного питания (UPS, генераторы)
  - Системами безопасности (пожарная сигнализация, камеры)
  - Вычислением метрик эффективности (PUE)
- **100+ виджетов** для визуализации данных
- **100+ дашбордов** для организации интерфейса
- **Виртуальные устройства** для генерации тестовых данных

Подробный анализ проекта см. в [PROJECT_ANALYSIS.md](PROJECT_ANALYSIS.md).

### Основные концепции

- **Контекст** - контейнер для переменных, функций, событий и действий
- **Модель** - специальный тип контекста для бизнес-логики и вычислений
- **Привязка** - автоматическое вычисление значений переменных на основе выражений
- **Набор правил** - последовательность правил для условной логики
- **Событие** - уведомление о состоянии или изменении в системе

---

## Архитектура AggreGate

### Иерархия контекстов

Все контексты в AggreGate организованы в иерархическую структуру:

```
users/
  {username}/
    models/          # Модели (бизнес-логика)
    devices/         # Устройства
    widgets/         # Виджеты (UI компоненты)
    dashboards/      # Дашборды (панели управления)
    alerts/          # Алерты (уведомления)
    queries/         # Запросы (SQL запросы)
    jobs/            # Задания (планировщик)
    workflows/       # Рабочие процессы
    scripts/         # Скрипты
    reports/         # Отчеты
```

### Пути контекстов

Путь к контексту имеет формат: `users.{username}.{contextType}.{contextName}`

Примеры:
- `users.admin.models.cpuLoad` - модель загрузки CPU
- `users.admin.devices.device1` - устройство device1
- `users.admin.widgets.cpuLoad` - виджет для отображения загрузки CPU

---

## Типы контекстов в AggreGate

### 1. Модели (Models) - `users.{username}.models.{modelName}`

**Назначение:** Виртуальные контексты для создания бизнес-логики, вычислений и обработки данных.

**Особенности:**
- Поддерживают переменные, функции, события
- Имеют привязки (bindings) для автоматических вычислений
- Поддерживают наборы правил (rule sets) для условной логики
- Могут быть относительными (relative), абсолютными (absolute) или инстанцируемыми (instantiable)

**Основные переменные модели:**
- `modelVariables` - переменные модели
- `modelFunctions` - функции модели
- `modelEvents` - события модели
- `bindings` - привязки (связи между переменными)
- `ruleSets` - наборы правил
- `threadPoolStatus` - статус пула потоков

### 2. Устройства (Devices) - `users.{username}.devices.{deviceName}`

**Назначение:** Представляют физические или виртуальные устройства, подключенные к системе.

**Особенности:**
- Имеют драйверы для связи с устройствами
- Поддерживают синхронизацию переменных и функций
- Имеют статус подключения и синхронизации

**Основные переменные устройства:**
- `status` - статус устройства (DataTable)
- `connectionProperties` - свойства подключения
- `genericProperties` - общие свойства
- `variablesCache` - кэш переменных
- `functionsCache` - кэш функций
- `eventsCache` - кэш событий

### 3. Виджеты (Widgets) - `users.{username}.widgets.{widgetName}`

**Назначение:** UI компоненты для отображения данных в дашбордах.

**Особенности:**
- Имеют XML шаблоны для определения интерфейса
- Могут иметь контекст по умолчанию
- Используются в дашбордах

**Основные свойства:**
- `template` - XML шаблон виджета
- `defaultContext` - контекст по умолчанию

### 4. Дашборды (Dashboards) - `users.{username}.dashboards.{dashboardName}`

**Назначение:** Панели управления, объединяющие виджеты и другие элементы.

**Типы раскладки:**
- `dockable` - док-панель (по умолчанию)
- `scrollable` - прокручиваемая
- `grid` - сетка
- `absolute` - абсолютная

**Типы элементов:**
- `launchWidget` - запуск виджета
- `showEventLog` - журнал событий
- `showSystemTree` - системное дерево
- `editData` - редактирование данных
- `editProperties` - редактирование свойств
- `showReport` - показ отчета
- `activateDashboard` - активация дашборда
- `showHtmlSnippet` - HTML-фрагмент

### 5. Алерты (Alerts) - `users.{username}.alerts.{alertName}`

**Назначение:** Уведомления о событиях и состояниях системы.

**Основные переменные:**
- `eventTriggers` - триггеры событий
- `notifications` - уведомления
- `status` - статус алерта
- `alertActions` - действия алерта
- `activeInstances` - активные экземпляры

**Статусы алерта:**
- `STATUS_ENABLED` (0) - включена
- `STATUS_DISABLED` (1) - отключена
- `STATUS_ACTIVE` (2) - активна
- `STATUS_ESCALATED` (3) - эскалирована

### 6. Запросы (Queries) - `users.{username}.queries.{queryName}`

**Назначение:** SQL запросы для работы с данными.

**Использование:**
- Выполнение SQL запросов к базе данных
- Использование в функциях типа Query
- Создание представлений данных

### 7. Задания (Jobs) - `users.{username}.jobs.{jobName}`

**Назначение:** Планировщик заданий для выполнения задач по расписанию.

**Использование:**
- Выполнение функций по расписанию
- Автоматизация повторяющихся задач
- Интеграция с системным планировщиком

### 8. Рабочие процессы (Workflows) - `users.{username}.workflows.{workflowName}`

**Назначение:** Автоматизация бизнес-процессов.

**Использование:**
- Определение последовательности действий
- Условная логика выполнения
- Интеграция различных компонентов системы

### 9. Скрипты (Scripts) - `users.{username}.scripts.{scriptName}`

**Назначение:** Хранение и выполнение скриптов.

**Использование:**
- Хранение кода для повторного использования
- Выполнение скриптов через функции
- Автоматизация задач

### 10. Отчеты (Reports) - `users.{username}.reports.{reportName}`

**Назначение:** Генерация и отображение отчетов.

**Использование:**
- Создание отчетов на основе данных
- Экспорт данных
- Визуализация информации

---

## Модели (Models)

Модели - это основной инструмент для создания бизнес-логики в AggreGate.

### Создание модели

```json
{
  "name": "aggregate_create_context",
  "arguments": {
    "parentPath": "users.admin.models",
    "name": "myModel",
    "description": "My Model Description"
  }
}
```

### Типы моделей

1. **Относительная модель (Relative)** - привязывается к родительскому контексту
2. **Абсолютная модель (Absolute)** - независимая модель
3. **Инстанцируемая модель (Instantiable)** - может создавать экземпляры

### Структура модели

Модель содержит следующие основные компоненты:

1. **Переменные модели** (`modelVariables`)
2. **Функции модели** (`modelFunctions`)
3. **События модели** (`modelEvents`)
4. **Привязки** (`bindings`)
5. **Наборы правил** (`ruleSets`)

---

## Переменные (Variables)

Переменные хранят данные в контекстах. Они могут быть простыми (одно значение) или табличными (множество записей).

### Типы полей переменных

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

### Формат переменных

#### Простая переменная (одно значение)

Для переменных, которые хранят одно значение:
```
<value><E>
```

Или с указанием min/max:
```
<M=1><X=1><value><E>
```

#### Табличная переменная (множество записей)

Для переменных, которые хранят таблицу:
```
<M=0><X=2147483647><name><S><value><E>
```

Где:
- `M=0` - минимальное количество записей
- `X=2147483647` - максимальное количество записей
- `<name><S>` - поле name типа String
- `<value><E>` - поле value типа Double

### Создание переменной в модели

```json
{
  "name": "aggregate_create_variable",
  "arguments": {
    "path": "users.admin.models.myModel",
    "variableName": "myVariable",
    "format": "<value><E>",
    "description": "My variable description",
    "writable": false,
    "readPermissions": "observer",
    "writePermissions": "manager",
    "storageMode": 0
  }
}
```

**Параметры:**
- `format` - формат переменной (TableFormat строка)
- `writable` - можно ли записывать (по умолчанию true)
- `readPermissions` - права на чтение (по умолчанию "observer")
- `writePermissions` - права на запись (по умолчанию "manager")
- `storageMode` - режим хранения: 0=database, 1=memory (по умолчанию 0)

### Получение и установка значений переменных

```json
// Получение переменной
{
  "name": "aggregate_get_variable",
  "arguments": {
    "path": "users.admin.models.myModel",
    "name": "myVariable"
  }
}

// Установка переменной
{
  "name": "aggregate_set_variable",
  "arguments": {
    "path": "users.admin.models.myModel",
    "name": "myVariable",
    "value": {
      "format": {
        "fields": [{"name": "value", "type": "E"}]
      },
      "records": [{"value": 123.45}]
    }
  }
}
```

---

## Функции (Functions)

Функции выполняют вычисления и операции в контекстах.

### Типы функций

1. **Java Code (0)** - Java-функция
2. **Expression (1)** - функция-выражение
3. **Query (2)** - функция-запрос

### Создание функции в модели

```json
{
  "name": "aggregate_create_function",
  "arguments": {
    "path": "users.admin.models.myModel",
    "functionName": "calculate",
    "inputFormat": "<arg1><E><arg2><E>",
    "outputFormat": "<result><E>",
    "functionType": 1,
    "expression": "{arg1} + {arg2}",
    "description": "Calculate sum",
    "permissions": "operator"
  }
}
```

**Параметры:**
- `functionType` - тип функции: 0=Java, 1=Expression, 2=Query
- `expression` - выражение для типа Expression
- `query` - SQL запрос для типа Query
- `implementation` - Java код для типа Java Code
- `permissions` - права на выполнение (по умолчанию "operator")
- `concurrent` - может ли выполняться параллельно (по умолчанию true)

### Вызов функции

```json
{
  "name": "aggregate_call_function",
  "arguments": {
    "path": "users.admin.models.myModel",
    "functionName": "calculate",
    "input": {
      "format": {
        "fields": [
          {"name": "arg1", "type": "E"},
          {"name": "arg2", "type": "E"}
        ]
      },
      "records": [{
        "arg1": 10,
        "arg2": 20
      }]
    }
  }
}
```

### Пример функции-выражения

```javascript
// Простое сложение
{arg1} + {arg2}

// Условное выражение
{arg1} > 0 ? {arg1} * {arg2} : 0

// Вызов другой функции
{users.admin.models.calculator:calculate({arg1}, {arg2}, 'add')}
```

### Пример Java-функции

```java
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;

public class %ScriptClassNamePattern% implements FunctionImplementation
{
  public DataTable execute(Context con, FunctionDefinition def, CallerController caller, RequestController request, DataTable parameters) throws ContextException
  {
    // Получение параметров
    double arg1 = parameters.getRecord(0).getDouble("arg1");
    double arg2 = parameters.getRecord(0).getDouble("arg2");
    
    // Вычисление
    double result = arg1 + arg2;
    
    // Создание результата
    DataTable resultTable = new SimpleDataTable(def.getOutputFormat());
    DataRecord record = resultTable.addRecord();
    record.setValue("result", result);
    
    return resultTable;
  }
}
```

---

## События (Events)

События используются для уведомления о состояниях и изменениях в системе.

### Создание события в модели

```json
{
  "name": "aggregate_create_event",
  "arguments": {
    "path": "users.admin.models.myModel",
    "eventName": "errorEvent",
    "description": "Error event",
    "level": 2,
    "format": "<message><S>",
    "permissions": "observer",
    "firePermissions": "manager"
  }
}
```

**Параметры:**
- `level` - уровень события: 0=INFO, 1=WARNING, 2=ERROR, 3=FATAL, 4=NOTICE
- `format` - формат данных события
- `permissions` - права на чтение события
- `firePermissions` - права на отправку события

### Отправка события

```json
{
  "name": "aggregate_fire_event",
  "arguments": {
    "agentName": "myAgent",
    "eventName": "errorEvent",
    "level": "ERROR",
    "data": {
      "format": {
        "fields": [{"name": "message", "type": "S"}]
      },
      "records": [{"message": "Error occurred"}]
    }
  }
}
```

### Отправка события через выражение

В привязке или функции можно использовать `fireEvent()`:

```javascript
fireEvent('users.admin.models.myModel', 'errorEvent', 2, table('<<message><S>>', 'Error message'))
```

Или с использованием ссылки на контекст:

```javascript
fireEvent({.:}, 'errorEvent', 2, table('<<message><S>>', 'Error message'))
```

---

## Привязки (Bindings)

Привязки автоматически вычисляют значения переменных на основе выражений.

### Назначение привязок

- Автоматическое обновление переменных при изменении зависимостей
- Создание вычисляемых полей
- Реакция на события
- Периодическое обновление значений

### Формат привязки

Привязка имеет следующие поля:
- `target` - целевая переменная (пустая строка для генерации событий)
- `expression` - выражение для вычисления
- `onstartup` - выполнять при старте (Boolean)
- `onevent` - выполнять при изменении зависимостей (Boolean)
- `periodically` - выполнять периодически (Boolean)
- `period` - период выполнения в миллисекундах (Long)

### Создание привязки

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
      "records": [{
        "target": "result@",
        "expression": "{users.admin.devices.device1:sine} * 100",
        "onstartup": true,
        "onevent": true,
        "periodically": false,
        "period": 0
      }]
    }
  }
}
```

### Примеры привязок

#### Простая привязка переменной

```javascript
// Target: result@
// Expression:
{users.admin.devices.device1:sine} * 100
```

#### Привязка с генерацией события

```javascript
// Target: (пустая строка)
// Expression:
{.:isError} && !{.:previousIsError} ? fireEvent('users.admin.models.cluster', 'clusterError', 2, table('<<message><S>>', 'Cluster error: all devices sine > 0')) : null
```

#### Периодическая привязка

```javascript
// Target: currentTime@
// Expression:
now()
// periodically: true
// period: 1000 (1 секунда)
```

#### Реальные примеры из проекта dataCenterManagementDemo

**Вычисление температуры:**
```javascript
// Температура верхнего уровня
temperature$topTemperature = {random$value} + 17

// Средняя температура
avgTemperature$value = ({temperature$topTemperature}+{temperature$middleTemperature}+{temperature$bottomTemperature}) / 3

// Максимальная температура
maxTemperature$value = max(max({temperature$topTemperature},{temperature$middleTemperature}),{temperature$bottomTemperature})
```

**Вычисление влажности:**
```javascript
// Влажность верхнего уровня (с округлением)
humidity$topHumidity = round({sawtooth$value} + 35 + {random$value} / 2.8564)

// Средняя влажность
avgHumidity$value = ({humidity$topHumidity}+{humidity$middleHumidity}+{humidity$bottomHumidity}) / 3
```

**Ограничение диапазона:**
```javascript
// PUE ограничен диапазоном 1-5
PUE$value = {random$value} < 1 ? 1 : {random$value} > 5 ? 5 : {random$value}
```

**Условная логика:**
```javascript
// Пожарная сигнализация (вероятность 10%)
fireAlarm$value = {random$value} < 1 ? true : false
```

**Вычисление заряда UPS:**
```javascript
// Заряд UPS с защитой от деления на ноль
UPS1$chargeLevel = 100 - 5 / {random$value}
```

### Синтаксис target

- `variableName@` - переменная в текущем контексте
- `{context:variable}@` - переменная в другом контексте
- Пустая строка - генерация события (используется fireEvent в expression)

---

## Наборы правил (Rule Sets)

Наборы правил используются для условной логики в моделях.

### Типы наборов правил

1. **Sequential (0)** - последовательное выполнение правил
2. **Dependent (1)** - зависимое выполнение (правила зависят друг от друга)

### Структура правила

Правило содержит:
- `target` - целевая переменная
- `expression` - выражение для вычисления
- `condition` - условие выполнения правила
- `comment` - комментарий

### Создание набора правил

```json
{
  "name": "aggregate_set_variable",
  "arguments": {
    "path": "users.admin.models.myModel",
    "name": "ruleSets",
    "value": {
      "format": {
        "fields": [
          {"name": "name", "type": "S"},
          {"name": "description", "type": "S"},
          {"name": "type", "type": "I"},
          {"name": "rules", "type": "T"}
        ]
      },
      "records": [{
        "name": "myRuleSet",
        "description": "My rule set",
        "type": 0,
        "rules": {
          "format": {
            "fields": [
              {"name": "target", "type": "S"},
              {"name": "expression", "type": "S"},
              {"name": "condition", "type": "S"},
              {"name": "comment", "type": "S"}
            ]
          },
          "records": [
            {
              "target": "result@",
              "expression": "{value} * 2",
              "condition": "{value} > 0",
              "comment": "Double positive values"
            }
          ]
        }
      }]
    }
  }
}
```

### Использование наборов правил

Наборы правил выполняются автоматически при изменении зависимостей или могут быть вызваны явно через функцию `processRuleSet`.

---

## Устройства (Devices)

Устройства представляют физические или виртуальные устройства.

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

### Драйверы устройств

#### Виртуальное устройство

**Идентификатор:** `com.tibbo.linkserver.plugin.device.virtual`

**Переменные:**
- `sine` (Double) - синусоидальная волна (-1 до 1)
- `sawtooth` (Double) - пилообразная волна
- `triangle` (Double) - треугольная волна
- `int` (Integer) - целочисленная переменная
- `table` (DataTable) - табличная переменная

**Функции:**
- `generateEvent(eventName, level, message, data)` - генерация события

### Статусы устройств

#### Статусы подключения
- `CONNECTION_STATUS_OFFLINE` (0) - устройство офлайн
- `CONNECTION_STATUS_ONLINE` (1) - устройство онлайн
- `CONNECTION_STATUS_SUSPENDED` (2) - приостановлено
- `CONNECTION_STATUS_UNKNOWN` (3) - неизвестен
- `CONNECTION_STATUS_MAINTENANCE` (4) - обслуживание

#### Статусы синхронизации
- `SYNC_STATUS_OK` (20) - синхронизация успешна
- `SYNC_STATUS_WAITING` (30) - ожидание
- `SYNC_STATUS_ERROR` (40) - ошибка
- `SYNC_STATUS_UNDEFINED` (50) - не определен
- `SYNC_STATUS_CONNECTING` (70) - подключение
- `SYNC_STATUS_READING_METADATA` (80) - чтение метаданных
- `SYNC_STATUS_SYNCHRONIZING_SETTINGS` (90) - синхронизация настроек

**Общий статус** = статус подключения + статус синхронизации
- `21` = ONLINE (1) + OK (20) - устройство онлайн и синхронизировано
- `71` = ONLINE (1) + CONNECTING (70) - устройство онлайн и подключается

---

## Виджеты (Widgets)

Виджеты - это UI компоненты для отображения данных.

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

### Установка XML шаблона

```json
{
  "name": "aggregate_set_widget_template",
  "arguments": {
    "path": "users.admin.widgets.myWidget",
    "template": "<?xml version=\"1.0\"?><widget><panel name=\"mainPanel\" layout=\"grid\"><label name=\"title\" text=\"My Widget\"/></panel></widget>"
  }
}
```

### Пример XML шаблона

```xml
<?xml version="1.0"?>
<widget>
  <panel name="mainPanel" layout="grid" maxColumns="2" maxRows="4">
    <label name="title" text="Cluster Status" gridx="0" gridy="0" gridWidth="2" gridHeight="1"/>
    <label name="statusLabel" text="Status: " gridx="0" gridy="1" gridWidth="1" gridHeight="1"/>
    <label name="status" text="{clusterStatus}" gridx="1" gridy="1" gridWidth="1" gridHeight="1"/>
  </panel>
</widget>
```

В шаблоне можно использовать ссылки на переменные: `{variableName}` или `{context:variable}`.

---

## Дашборды (Dashboards)

Дашборды объединяют виджеты и другие элементы в панели управления.

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
    "name": "myWidget",
    "type": "launchWidget",
    "parameters": {
      "format": {
        "fields": [
          {"name": "widgetContext", "type": "S"},
          {"name": "defaultContext", "type": "S"}
        ]
      },
      "records": [{
        "widgetContext": "users.admin.widgets.myWidget",
        "defaultContext": "users.admin.models.myModel"
      }]
    }
  }
}
```

### Открытие дашборда

```json
{
  "name": "aggregate_execute_action",
  "arguments": {
    "path": "users.admin.dashboards.myDashboard",
    "actionName": "open",
    "input": {
      "format": {
        "fields": [
          {"name": "name", "type": "S"},
          {"name": "location", "type": "T"}
        ]
      },
      "records": [{
        "name": "myDashboard",
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

## Алерты (Alerts)

Алерты создают уведомления на основе событий.

### Создание алерта

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
        "context": "users.admin.models.myModel",
        "event": "errorEvent",
        "description": "Error Alert"
      }]
    }
  }
}
```

### Настройка триггеров событий

```json
{
  "name": "aggregate_set_variable",
  "arguments": {
    "path": "users.admin.alerts.errorAlert",
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
        "mask": "users.admin.models.myModel",
        "event": "errorEvent",
        "level": 2,
        "count": 1,
        "period": 0,
        "message": "Error occurred"
      }]
    }
  }
}
```

---

## Запросы (Queries)

Запросы используются для выполнения SQL запросов.

### Использование в функциях

Функции типа Query используют SQL запросы для получения данных:

```json
{
  "name": "aggregate_create_function",
  "arguments": {
    "path": "users.admin.models.myModel",
    "functionName": "getData",
    "inputFormat": "<filter><S>",
    "outputFormat": "<id><I><name><S><value><E>",
    "functionType": 2,
    "query": "SELECT id, name, value FROM data WHERE name LIKE {filter}"
  }
}
```

---

## Задания (Jobs)

Задания выполняют функции по расписанию.

### Использование

Задания создаются через планировщик и могут выполнять функции контекстов по расписанию.

---

## Рабочие процессы (Workflows)

Рабочие процессы автоматизируют бизнес-процессы.

### Использование

Workflows определяют последовательность действий и условную логику для автоматизации процессов.

---

## Скрипты (Scripts)

Скрипты хранят код для повторного использования.

### Использование

Скрипты могут хранить Java код или другие типы скриптов для выполнения через функции.

---

## Отчеты (Reports)

Отчеты генерируют и отображают отчеты на основе данных.

### Использование

Reports создают отчеты на основе данных из контекстов, устройств и моделей.

---

## Язык выражений AggreGate

Язык выражений используется в привязках, функциях типа Expression и правилах.

### Базовый синтаксис

#### Литералы

```javascript
123              // число
45.67            // число с плавающей точкой
"text"           // строка
true, false      // булевы значения
null             // null значение
```

#### Ссылки на объекты

**Формат:** `{context:entity('param1', 'param2', ...)$field[row]#property`

**Примеры:**

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

#### Операторы

**Арифметические:**
```javascript
{arg1} + {arg2}
{price} * {quantity}
{value} / {count}
{total} - {discount}
```

**Логические:**
```javascript
{status} == "active" && {value} > 100
{enabled} || {force}
!{disabled}
```

**Сравнения:**
```javascript
{value} == 100
{status} != "error"
{count} < 10
{price} > 50
{age} <= 18
{score} >= 80
```

#### Условные выражения

```javascript
// Тернарный оператор
{.:value} > 0 ? {.:value} : 0

// Функция if
if({status} == "active", 1, 0)

// Функция case
case({type}, "A", 1, "B", 2, 0)
```

### Функции для работы с контекстами

#### getVariable(context, variable)

```javascript
getVariable("users.admin.models.calculator", "result")
```

#### setVariable(context, variable, value)

```javascript
setVariable("users.admin.models.calculator", "result", 100)
```

#### callFunction(context, function, param1, param2, ...)

```javascript
callFunction("users.admin.models.calculator", "calculate", 10, 20, "add")
```

#### fireEvent(context, event, level, data)

```javascript
fireEvent("users.admin.models.cluster", "clusterError", 2, table("<<message><S>>", "Cluster error"))
```

**Важно:**
- Контекст должен быть полным путем строкой: `'users.admin.models.cluster'` или ссылкой: `{.:}` или `{users.admin.models.cluster}`
- ❌ Не используйте `'.'` как строку - это не работает!
- Для `table()` формат должен быть в двойных угловых скобках: `'<<message><S>>'`

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

// Работа с таблицами
sum({users.admin.devices.device1:data$value})
avg({users.admin.devices.device1:data$temperature})
count({users.admin.devices.device1:data})
```

---

## Лучшие практики разработки

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

### Организация моделей

1. **Разделяйте логику по моделям** - каждая модель должна отвечать за определенную область функциональности
2. **Используйте относительные модели** - для привязки к родительским контекстам
3. **Группируйте переменные** - используйте группы для организации переменных
4. **Документируйте привязки** - добавляйте комментарии к сложным привязкам

### Работа с привязками

1. **Используйте правильные триггеры:**
   - `onstartup` - для инициализации
   - `onevent` - для реактивных обновлений
   - `periodically` - для периодических обновлений

2. **Оптимизируйте периоды:**
   - Не устанавливайте слишком короткие периоды (минимум 100-1000 мс)
   - Используйте `onevent` вместо `periodically` когда возможно

3. **Избегайте циклических зависимостей:**
   - Не создавайте привязки, которые ссылаются друг на друга циклически

### Работа с событиями

1. **Используйте правильные уровни:**
   - INFO (0) - информационные сообщения
   - WARNING (1) - предупреждения
   - ERROR (2) - ошибки
   - FATAL (3) - критические ошибки
   - NOTICE (4) - уведомления

2. **Структурируйте данные событий:**
   - Используйте формат с полями для структурированных данных
   - Добавляйте описательные сообщения

### Безопасность

1. **Устанавливайте правильные права доступа:**
   - `observer` - только чтение
   - `operator` - базовые операции
   - `manager` - управление
   - `engineer` - инженерные операции
   - `admin` - администратор

2. **Используйте валидацию:**
   - Добавляйте валидаторы к переменным
   - Проверяйте входные данные в функциях

### Производительность

1. **Используйте кэширование:**
   - Настройте режим кэширования для переменных
   - Используйте `storageMode` для оптимизации

2. **Оптимизируйте привязки:**
   - Минимизируйте количество зависимостей
   - Используйте `concurrent` для параллельных вычислений

3. **Мониторинг:**
   - Используйте `threadPoolStatus` для мониторинга производительности
   - Отслеживайте статистику контекста

---

## Заключение

AggreGate предоставляет мощную платформу для создания IoT приложений с использованием иерархической структуры контекстов. Правильное использование моделей, переменных, функций, событий, привязок и наборов правил позволяет создавать эффективные и масштабируемые решения.

Следуя лучшим практикам, описанным в этом руководстве, вы сможете создавать качественные приложения на платформе AggreGate.

