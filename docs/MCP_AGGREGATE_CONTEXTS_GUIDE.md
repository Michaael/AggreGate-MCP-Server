# Руководство по основным контекстам AggreGate для ИИ

## ⚠️ КРИТИЧЕСКИ ВАЖНО ДЛЯ ИИ

Этот документ описывает все основные контейнеры (контексты) в AggreGate и их назначение. **ВСЕГДА используйте правильный контекст для решения задачи!** Не создавайте модели там, где нужны тревоги, не создавайте тревоги в моделях и т.д.

---

## Структура контекстов пользователя

Все контексты пользователя находятся в `users.{username}.*`. Основные контейнеры:

- `users.{username}.devices` - устройства
- `users.{username}.models` - модели для обработки данных
- `users.{username}.alerts` - **тревоги (alarms)** ⚠️ ИСПОЛЬЗУЙТЕ ДЛЯ ТРЕВОГ!
- `users.{username}.widgets` - виджеты (UI компоненты)
- `users.{username}.dashboards` - дашборды (панели мониторинга)
- `users.{username}.queries` - запросы к данным
- `users.{username}.reports` - отчеты
- `users.{username}.filters` - фильтры событий
- `users.{username}.classes` - классы объектов
- `users.{username}.workflows` - рабочие процессы
- `users.{username}.scripts` - скрипты
- `users.{username}.jobs` - запланированные задачи
- `users.{username}.applications` - приложения
- `users.{username}.processControl` - программы управления процессами
- `users.{username}.ipsla` - IP SLA тесты
- `users.{username}.correlators` - корреляторы событий
- `users.{username}.machineLearning` - модели машинного обучения
- `users.{username}.common` - общие таблицы данных

Для каждого типа контекста есть соответствующая группа: `{context}_groups` (например, `widgets_groups`, `alerts_groups`).

---

## 1. `users.{username}.devices` - Устройства

**Назначение:** Контейнер для физических и виртуальных устройств, подключенных к AggreGate.

**Когда использовать:**
- Создание нового устройства через драйвер
- Работа с данными конкретного устройства
- Настройка параметров устройства
- Получение переменных, функций, событий устройства

**Как создавать:**
```json
{
  "tool": "aggregate_create_device",
  "parameters": {
    "username": "admin",
    "deviceName": "myDevice",
    "description": "Описание устройства",
    "driverId": "com.tibbo.linkserver.plugin.device.virtual"
  }
}
```

**Пример пути:** `users.admin.devices.virtual_device`

**⚠️ НЕ используйте для:**
- Создания тревог (используйте `alerts`)
- Создания моделей обработки данных (используйте `models`)
- Создания виджетов (используйте `widgets`)

---

## 2. `users.{username}.models` - Модели

**Назначение:** Контекст для создания моделей обработки данных. Модели используются для:
- Агрегации данных от нескольких устройств
- Вычислений и преобразований данных
- Хранения обработанных данных
- Создания бизнес-логики на основе данных устройств

**Типы моделей:**
1. **Абсолютная модель** (type=1) - один экземпляр, абсолютные пути в привязках
2. **Относительная модель** (type=0) - по одному экземпляру на объект, относительные ссылки
3. **Экземплярная модель** (type=2) - создается по требованию

**Когда использовать:**
- Нужно обработать данные от устройств (суммы, средние, фильтрация)
- Нужно хранить вычисленные значения
- Нужно создать бизнес-логику на основе данных устройств
- Нужно агрегировать данные от нескольких устройств

**Как создавать:**
```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "myModel",
    "description": "Описание модели",
    "modelType": 1  // 0=relative, 1=absolute, 2=instance
  }
}
```

**Пример пути:** `users.admin.models.myModel`

**⚠️ НЕ используйте для:**
- Создания тревог (используйте `alerts`) ⚠️ **КРИТИЧЕСКАЯ ОШИБКА!**
- Создания устройств (используйте `devices`)
- Создания виджетов (используйте `widgets`)
- Создания отчетов (используйте `reports`)

**После создания модели ОБЯЗАТЕЛЬНО:**
1. Создайте переменные через `aggregate_create_variable`
2. Создайте привязки через `aggregate_set_variable` на переменную `bindings`
3. Для relative моделей установите `validityExpression` в `childInfo`

---

## 3. `users.{username}.alerts` - Тревоги (Alarms) ⚠️ ВАЖНО!

**Назначение:** **СПЕЦИАЛЬНЫЙ КОНТЕКСТ ДЛЯ СОЗДАНИЯ ТРЕВОГ!** Все тревоги должны создаваться здесь, а не в моделях!

**Когда использовать:**
- Нужно создать тревогу, которая срабатывает при определенных условиях
- Нужно мониторить события устройств
- Нужно отслеживать значения переменных
- Нужно настроить автоматические действия при срабатывании тревоги

**Как создавать:**
```json
{
  "tool": "aggregate_get_or_create_context",
  "parameters": {
    "path": "users.admin.alerts.myAlarm",
    "description": "Описание тревоги"
  }
}
```

Затем настройте триггеры:
- `eventTriggers` - триггеры на события
- `variableTriggers` - триггеры на переменные
- `alertActions` - автоматические корректирующие действия
- `interactiveActions` - интерактивные действия

**Пример пути:** `users.admin.alerts.temperature_alarm`

**Пример настройки тревоги на событие:**
```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.alerts.event1_alarm",
    "name": "eventTriggers",
    "value": {
      "records": [{
        "mask": "users.admin.devices.virtual_device",
        "event": "event1",
        "correlated": "event2",
        "correlator": "{Int} > 20",
        "message": "Тревога активирована"
      }]
    }
  }
}
```

**Пример настройки тревоги на переменную:**
```json
{
  "tool": "aggregate_set_variable",
  "parameters": {
    "path": "users.admin.alerts.temperature_alarm",
    "name": "variableTriggers",
    "value": {
      "records": [{
        "mask": "users.admin.models.temperatureModel",
        "variable": "temperature",
        "expression": "> 30",
        "delay": 0,
        "message": "Температура превысила порог"
      }]
    }
  }
}
```

**⚠️ КРИТИЧЕСКИ ВАЖНО:**
- **ВСЕГДА создавайте тревоги в `alerts`, а не в `models`!**
- Тревоги - это не модели обработки данных
- Тревоги имеют специальные переменные: `eventTriggers`, `variableTriggers`, `alertActions`
- Используйте `aggregate_list_alarms` для получения списка тревог

**НЕ используйте для:**
- Обработки данных (используйте `models`)
- Создания устройств (используйте `devices`)
- Создания виджетов (используйте `widgets`)

---

## 4. `users.{username}.widgets` - Виджеты

**Назначение:** UI компоненты для отображения данных и взаимодействия с пользователем.

**Когда использовать:**
- Нужно создать пользовательский интерфейс
- Нужно отобразить графики, таблицы, формы
- Нужно создать интерактивные элементы (кнопки, поля ввода)
- Нужно визуализировать данные устройств или моделей

**Как создавать:**
```json
{
  "tool": "aggregate_create_widget",
  "parameters": {
    "parentPath": "users.admin.widgets",
    "name": "myWidget",
    "description": "Описание виджета",
    "template": "<xml>...</xml>",  // XML шаблон виджета
    "defaultContext": "users.admin.models.myModel"  // Контекст по умолчанию
  }
}
```

**Пример пути:** `users.admin.widgets.myWidget`

**После создания виджета:**
- Установите XML шаблон через `aggregate_set_widget_template`
- Или используйте действие `configure` для настройки через UI

**⚠️ НЕ используйте для:**
- Создания тревог (используйте `alerts`)
- Создания моделей (используйте `models`)
- Создания устройств (используйте `devices`)

---

## 5. `users.{username}.dashboards` - Дашборды

**Назначение:** Панели мониторинга, объединяющие несколько виджетов и отчетов.

**Когда использовать:**
- Нужно создать панель мониторинга с несколькими виджетами
- Нужно объединить графики, таблицы, отчеты в одном месте
- Нужно создать комплексный интерфейс для оператора

**Как создавать:**
```json
{
  "tool": "aggregate_create_dashboard",
  "parameters": {
    "parentPath": "users.admin.dashboards",
    "name": "myDashboard",
    "description": "Описание дашборда",
    "layout": "dockable"  // dockable, scrollable, grid, absolute
  }
}
```

**Пример пути:** `users.admin.dashboards.myDashboard`

**После создания дашборда:**
- Добавьте элементы через `aggregate_add_dashboard_element`
- Элементы могут быть виджетами, отчетами, логами событий и т.д.

**⚠️ НЕ используйте для:**
- Создания отдельных виджетов (используйте `widgets`)
- Создания тревог (используйте `alerts`)

---

## 6. `users.{username}.queries` - Запросы

**Назначение:** Запросы к данным для получения и фильтрации информации из устройств, моделей и других контекстов.

**Когда использовать:**
- Нужно создать запрос для получения данных из нескольких источников
- Нужно отфильтровать данные по условиям
- Нужно создать динамический отчет на основе запроса
- Нужно получить агрегированные данные

**Как создавать:**
```json
{
  "tool": "aggregate_get_or_create_context",
  "parameters": {
    "path": "users.admin.queries.myQuery",
    "description": "Описание запроса"
  }
}
```

Затем настройте запрос через переменную `query` или используйте действия для настройки.

**Пример пути:** `users.admin.queries.deviceVariables`

**⚠️ НЕ используйте для:**
- Создания отчетов (используйте `reports`)
- Создания моделей обработки данных (используйте `models`)

---

## 7. `users.{username}.reports` - Отчеты

**Назначение:** Отчеты для отображения данных в табличном или графическом виде.

**Когда использовать:**
- Нужно создать отчет для отображения данных
- Нужно показать таблицу значений переменных
- Нужно создать печатный отчет
- Нужно настроить корректирующее действие тревоги (показ отчета)

**Как создавать:**
```json
{
  "tool": "aggregate_get_or_create_context",
  "parameters": {
    "path": "users.admin.reports.myReport",
    "description": "Описание отчета"
  }
}
```

Затем настройте отчет через действия или переменные.

**Пример пути:** `users.admin.reports.tableReport`

**⚠️ НЕ используйте для:**
- Создания запросов (используйте `queries`)
- Создания виджетов (используйте `widgets`)

---

## 8. `users.{username}.filters` - Фильтры событий

**Назначение:** Фильтры для отображения событий по определенным условиям.

**Когда использовать:**
- Нужно отфильтровать события по условиям
- Нужно показать только определенные типы событий
- Нужно создать фильтр для лога событий

**Как создавать:**
```json
{
  "tool": "aggregate_get_or_create_context",
  "parameters": {
    "path": "users.admin.filters.myFilter",
    "description": "Описание фильтра"
  }
}
```

Затем настройте условия фильтрации через переменные или действия.

**Пример пути:** `users.admin.filters.event2Filter`

**Пример использования:**
- Фильтр для Event 2 с Integer > 10
- Фильтр для событий с String содержащим "abc"

---

## 9. `users.{username}.classes` - Классы объектов

**Назначение:** Классы для классификации и структурирования объектов (устройств, активов и т.д.).

**Когда использовать:**
- Нужно создать классификацию объектов
- Нужно структурировать данные по типам
- Нужно создать иерархию объектов

**Как создавать:**
```json
{
  "tool": "aggregate_get_or_create_context",
  "parameters": {
    "path": "users.admin.classes.myClass",
    "description": "Описание класса"
  }
}
```

**Пример пути:** `users.admin.classes.server`

---

## 10. `users.{username}.workflows` - Рабочие процессы

**Назначение:** Автоматизированные рабочие процессы для выполнения последовательности действий.

**Когда использовать:**
- Нужно автоматизировать последовательность действий
- Нужно создать бизнес-процесс
- Нужно выполнить действия при определенных условиях

**Как создавать:**
```json
{
  "tool": "aggregate_get_or_create_context",
  "parameters": {
    "path": "users.admin.workflows.myWorkflow",
    "description": "Описание процесса"
  }
}
```

**Пример пути:** `users.admin.workflows.setAcknowledgementMessage`

---

## 11. `users.{username}.scripts` - Скрипты

**Назначение:** Скрипты для выполнения вычислений и обработки данных.

**Когда использовать:**
- Нужно выполнить сложные вычисления
- Нужно обработать данные скриптом
- Нужно создать функцию для использования в других контекстах

**Как создавать:**
```json
{
  "tool": "aggregate_get_or_create_context",
  "parameters": {
    "path": "users.admin.scripts.myScript",
    "description": "Описание скрипта"
  }
}
```

**Пример пути:** `users.admin.scripts.deviceInfo`

---

## 12. `users.{username}.jobs` - Запланированные задачи

**Назначение:** Задачи, выполняемые по расписанию.

**Когда использовать:**
- Нужно выполнить действие по расписанию
- Нужно создать периодическую задачу
- Нужно автоматизировать регулярные операции

**Как создавать:**
```json
{
  "tool": "aggregate_get_or_create_context",
  "parameters": {
    "path": "users.admin.jobs.myJob",
    "description": "Описание задачи"
  }
}
```

**Пример пути:** `users.admin.jobs.dailyBackup`

---

## 13. `users.{username}.applications` - Приложения

**Назначение:** Приложения AggreGate (SCADA, Network Manager и т.д.).

**Когда использовать:**
- Работа с встроенными приложениями AggreGate
- Настройка приложений

**Примеры:**
- `users.admin.applications.scada` - SCADA/HMI
- `users.admin.applications.networkManager` - Network Manager
- `users.admin.applications.platformAdministrationKit` - Platform Administration Kit

---

## 14. `users.{username}.processControl` - Управление процессами

**Назначение:** Программы управления процессами (Process Control Programs).

**Когда использовать:**
- Нужно создать программу управления процессом
- Нужно автоматизировать управление оборудованием

**Как создавать:**
```json
{
  "tool": "aggregate_get_or_create_context",
  "parameters": {
    "path": "users.admin.processControl.myProgram",
    "description": "Описание программы"
  }
}
```

**Пример пути:** `users.admin.processControl.boiler`

---

## 15. `users.{username}.ipsla` - IP SLA тесты

**Назначение:** IP SLA тесты для мониторинга сети.

**Когда использовать:**
- Нужно создать тест доступности сети
- Нужно мониторить производительность сети

**Как создавать:**
```json
{
  "tool": "aggregate_get_or_create_context",
  "parameters": {
    "path": "users.admin.ipsla.myTest",
    "description": "Описание теста"
  }
}
```

---

## 16. `users.{username}.correlators` - Корреляторы событий

**Назначение:** Корреляторы для анализа и группировки событий.

**Когда использовать:**
- Нужно коррелировать события
- Нужно группировать связанные события

**Как создавать:**
```json
{
  "tool": "aggregate_get_or_create_context",
  "parameters": {
    "path": "users.admin.correlators.myCorrelator",
    "description": "Описание коррелятора"
  }
}
```

---

## 17. `users.{username}.machineLearning` - Машинное обучение

**Назначение:** Модели машинного обучения и обучаемые единицы.

**Когда использовать:**
- Нужно создать модель машинного обучения
- Нужно обучить систему на данных

**Как создавать:**
```json
{
  "tool": "aggregate_get_or_create_context",
  "parameters": {
    "path": "users.admin.machineLearning.myModel",
    "description": "Описание модели ML"
  }
}
```

---

## 18. `users.{username}.common` - Общие данные

**Назначение:** Общие таблицы данных, используемые в нескольких контекстах.

**Когда использовать:**
- Нужно создать общую таблицу данных
- Нужно хранить данные, используемые в нескольких местах

**Примеры:**
- `users.admin.common.assetInformation` - Информация об активах
- `users.admin.common.deviceTypes` - Типы устройств

---

## Группы контекстов

Для каждого типа контекста есть соответствующая группа: `{context}_groups`

**Примеры:**
- `users.admin.widgets_groups` - группы виджетов
- `users.admin.alerts_groups` - группы тревог
- `users.admin.models_groups` - группы моделей
- `users.admin.queries_groups` - группы запросов
- `users.admin.reports_groups` - группы отчетов

Группы используются для организации контекстов по категориям.

---

## Дерево решений для выбора контекста

### Нужно создать тревогу?
→ **Используйте `users.{username}.alerts`** ⚠️ НЕ модели!

### Нужно обработать данные от устройств?
→ **Используйте `users.{username}.models`**

### Нужно создать устройство?
→ **Используйте `aggregate_create_device`** (создается в `users.{username}.devices`)

### Нужно создать UI компонент?
→ **Используйте `users.{username}.widgets`**

### Нужно создать панель мониторинга?
→ **Используйте `users.{username}.dashboards`**

### Нужно создать запрос к данным?
→ **Используйте `users.{username}.queries`**

### Нужно создать отчет?
→ **Используйте `users.{username}.reports`**

### Нужно отфильтровать события?
→ **Используйте `users.{username}.filters`**

---

## Типичные ошибки ИИ

### ❌ Ошибка: Создание тревоги в моделях
**Неправильно:**
```json
{
  "tool": "aggregate_create_context",
  "parameters": {
    "parentPath": "users.admin.models",
    "name": "alarm_model"
  }
}
```

**Правильно:**
```json
{
  "tool": "aggregate_get_or_create_context",
  "parameters": {
    "path": "users.admin.alerts.myAlarm",
    "description": "Описание тревоги"
  }
}
```

### ❌ Ошибка: Создание модели для тревоги
**Неправильно:** Создание модели `alarm_model` для хранения состояния тревоги

**Правильно:** Создание тревоги в `users.admin.alerts.myAlarm` с настройкой триггеров

### ❌ Ошибка: Создание виджета в моделях
**Неправильно:** Создание контекста в `models` для виджета

**Правильно:** Использование `aggregate_create_widget` с `parentPath: "users.admin.widgets"`

---

## Чек-лист для ИИ

Перед созданием контекста всегда проверяйте:

- [ ] Это тревога? → Используйте `alerts`
- [ ] Это обработка данных? → Используйте `models`
- [ ] Это устройство? → Используйте `aggregate_create_device`
- [ ] Это UI компонент? → Используйте `widgets`
- [ ] Это панель мониторинга? → Используйте `dashboards`
- [ ] Это запрос? → Используйте `queries`
- [ ] Это отчет? → Используйте `reports`
- [ ] Это фильтр событий? → Используйте `filters`

---

**Версия:** 1.0  
**Дата:** 2025-01-27  
**Основано на анализе реального сервера AggreGate**
