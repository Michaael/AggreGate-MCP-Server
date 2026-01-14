# Инструкции по миграции CoffeeGate

## Текущий статус

✅ **Начата миграция приложения CoffeeGate**

### Выполнено:
1. ✅ Подключение к обоим серверам
2. ✅ Анализ структуры приложения
3. ✅ Создание базовой модели `cmMain` на localhost
4. ✅ Создание переменных в `cmMain`:
   - `regions` - Справочник регионов (13 записей)
   - `drinks` - Напитки (61 запись)
   - `models` - Модели машин (6 записей)
   - `managementForms` - Формы управления (4 записи)
   - `log` - Лог сообщений
5. ✅ Импорт данных в переменные

## Структура приложения CoffeeGate

### Модели (50+)
Приложение включает более 50 моделей для управления кофемашинами, объектами, отчетами, инцидентами и интеграциями.

### Устройства (3)
- WMF_K031920_17807
- drCoffeeNative_220625007
- drCoffeeTest_220625007

### Дашборды (30+)
Веб-интерфейс с множеством дашбордов для мониторинга, управления и отчетов.

### Отчеты (6)
Отчеты по промывкам, напиткам, простоям, сервисным счетчикам и журналу диспетчера.

## Процесс миграции

### Для каждой модели:

1. **Экспорт с сервера-источника:**
   ```python
   # Получить структуру
   aggregate_get_context(path="users.admin.models.{modelName}")
   
   # Получить переменные
   aggregate_list_variables(path="users.admin.models.{modelName}")
   
   # Получить функции
   aggregate_list_functions(path="users.admin.models.{modelName}")
   
   # Получить события
   aggregate_list_events(path="users.admin.models.{modelName}")
   
   # Получить привязки
   aggregate_get_variable(path="users.admin.models.{modelName}", name="bindings")
   
   # Получить данные переменных
   aggregate_get_variable(path="users.admin.models.{modelName}", name="{variableName}")
   ```

2. **Создание на localhost:**
   ```python
   # Создать контекст
   aggregate_create_context(
       parentPath="users.admin.models",
       name="{modelName}",
       description="{description}",
       modelType={0|1|2},  # 0=relative, 1=absolute, 2=instance
       containerType="devices",  # для relative моделей
       objectType="device",  # для relative моделей
       connectionKey="localhost"
   )
   
   # Создать переменные
   for variable in variables:
       aggregate_create_variable(
           path="users.admin.models.{modelName}",
           variableName=variable["name"],
           format=variable["format"],
           description=variable["description"],
           group=variable.get("group"),
           writable=variable.get("writable", True),
           connectionKey="localhost"
       )
   
   # Создать функции
   for function in functions:
       aggregate_create_function(
           path="users.admin.models.{modelName}",
           functionName=function["name"],
           functionType=function.get("type", 0),
           inputFormat=function.get("inputFormat"),
           outputFormat=function.get("outputFormat"),
           expression=function.get("expression"),
           description=function.get("description"),
           connectionKey="localhost"
       )
   
   # Создать события
   for event in events:
       aggregate_create_event(
           path="users.admin.models.{modelName}",
           eventName=event["name"],
           format=event.get("format"),
           description=event.get("description"),
           level=event.get("level", 0),
           connectionKey="localhost"
       )
   
   # Установить привязки
   aggregate_set_variable(
       path="users.admin.models.{modelName}",
       name="bindings",
       value=bindings_data,
       connectionKey="localhost"
   )
   
   # Импортировать данные переменных
   for variable in variables_with_data:
       aggregate_set_variable(
           path="users.admin.models.{modelName}",
           name=variable["name"],
           value=variable["data"],
           connectionKey="localhost"
       )
   ```

## Приоритет миграции

### Этап 1: Базовые справочники (ВЫСОКИЙ ПРИОРИТЕТ)
1. ✅ `cmMain` - Главные справочники (ВЫПОЛНЕНО)
2. `cmSettings` - Настройки системы
3. `cmMachineTypes` - Типы кофемашин
4. `cmSites` - Разделы
5. `cmStructure` - Организационная структура

### Этап 2: Управление кофемашинами (ВЫСОКИЙ ПРИОРИТЕТ)
1. `cmMachinesManager` - Менеджер кофемашин
2. `cmMachinesPresenter` - Презентатор кофемашин
3. `cmCoffeeMachinesItems` - Экземпляры кофемашин (относительная модель)

### Этап 3: Интерфейсы кофемашин (ВЫСОКИЙ ПРИОРИТЕТ)
1. `cmDrCoffee` - Интерфейс DrCoffee
2. `cmWMF1500` - Интерфейс WMF 1500, 5000
3. `cmF2Plus` - Интерфейс F2Plus
4. `WMFFunctions` - Функции КМ

### Этап 4: Управление объектами (СРЕДНИЙ ПРИОРИТЕТ)
1. `cmObjectsManager` - Менеджер объектов
2. `cmObjectsPresenter` - Презентатор объектов
3. `cmObjectsItems` - Экземпляры объектов

### Этап 5: Отчеты (СРЕДНИЙ ПРИОРИТЕТ)
1. `cmReportsManager` - Менеджер отчетов
2. `cmReportsPresenter` - Презентатор отчетов
3. Все модели отчетов

### Этап 6: Остальные компоненты (НИЗКИЙ ПРИОРИТЕТ)
1. Инциденты
2. Пользователи
3. Интеграции
4. Коннекторы и агенты
5. Аналитика
6. Дашборды
7. Виджеты
8. Приложения
9. Тревоги

## Оптимизации

### 1. Удаление дубликатов
Следующие контексты будут пропущены при миграции:
- `cmCoffeeMachinesItems_rep` - дубликат
- `cmLogAnalyze_copy` - дубликат
- `frontendConfig_copy` - дубликат
- Все дашборды с суффиксами `_copy`, `_old`, `_v2_old`

### 2. Объединение похожих моделей
- `cmReportSells` → использовать только `cmReportSellsDetailed`
- `cmReportCleans` → использовать только `cmReportCleansDetailed`

### 3. Упрощение структуры
- Рассмотреть объединение презентаторов
- Упростить привязки
- Оптимизировать запросы

### 4. Очистка
- Удалить `cmGarbageFunctions`
- Удалить тестовые контексты
- Удалить неиспользуемые модели

## Рекомендации

1. **Мигрировать поэтапно** - начинать с базовых справочников, затем управление, затем остальное
2. **Проверять после каждого этапа** - убедиться, что все работает корректно
3. **Тестировать функциональность** - проверить функции, привязки, события
4. **Оптимизировать после миграции** - удалить дубликаты и неиспользуемые компоненты

## Автоматизация

Для автоматизации миграции можно использовать скрипт `coffeegate_migration_script.py`, который содержит список всех моделей и функции для оптимизации.

## Следующие шаги

1. Завершить миграцию базовых справочников (cmSettings, cmMachineTypes, cmSites, cmStructure)
2. Мигрировать управление кофемашинами
3. Мигрировать интерфейсы кофемашин
4. Продолжить с остальными компонентами по приоритету

---

**Текущий прогресс:** 1 модель из ~50 (2%)
