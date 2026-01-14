# Финальный отчет о миграции всех контекстов CoffeeGate

## Статус: Модели созданы, приложения экспортированы

### ✅ Выполнено

#### 1. Модели (50+ моделей)
Все модели CoffeeGate успешно созданы на localhost:

**Базовые справочники:**
- ✅ cmMain (уже была создана ранее)
- ✅ cmSettings
- ✅ cmMachineTypes
- ✅ cmSites
- ✅ cmStructure

**Управление кофемашинами:**
- ✅ cmMachinesManager
- ✅ cmMachinesPresenter
- ✅ cmCoffeeMachinesItems (instance модель)
- ✅ cmDrCoffee
- ✅ cmWMF1500
- ✅ cmF2Plus
- ✅ WMFFunctions (relative модель)

**Управление объектами:**
- ✅ cmObjectsManager
- ✅ cmObjectsPresenter
- ✅ cmObjectsItems (instance модель)

**Отчеты:**
- ✅ cmReportsManager
- ✅ cmReportsPresenter
- ✅ cmMainReports
- ✅ cmReportCleans
- ✅ cmReportCleansDetailed
- ✅ cmReportSells
- ✅ cmReportSellsDetailed
- ✅ cmReportMonitoring
- ✅ cmReportServiceCounters
- ✅ cmIdleReport
- ✅ cmReportSettings

**Инциденты:**
- ✅ cmIncidents
- ✅ cmIncidentsManager

**Пользователи:**
- ✅ cmUsersManager
- ✅ cmUsersProperties (relative модель)

**Коннекторы:**
- ✅ cmConnectors
- ✅ cmConnectors_agents (relative модель)

**Интеграции:**
- ✅ cmMailIntegrationManager

**Презентаторы:**
- ✅ cmCommonFunctions
- ✅ cmCommonPresenter
- ✅ cmDictionariesPresenter
- ✅ cmEngineerPresenter

**Агенты:**
- ✅ cmAgentLtManager
- ✅ cmAgentLtAgents (instance модель)

**Утилиты:**
- ✅ cmCaching
- ✅ cmContextCreator
- ✅ cmCustomer1Manager
- ✅ cmCustomer1Reports
- ✅ cmDbCollection
- ✅ cmFileLoader
- ✅ cmFileOperations
- ✅ cmLogAnalyze
- ✅ cmPrepareClean
- ✅ cmService
- ✅ cmSystemAnalysis
- ✅ cmSystemLog
- ✅ cmUtils
- ✅ cm_reestr_query

#### 2. Приложения
- ✅ CG_transfer_backend (экспортировано)
- ✅ CG_transfer_frontend (экспортировано)
- ✅ CG_transfer_tirazh (экспортировано)

### ⚠️ Требует ручного переноса

#### 1. Дашборды (60+ дашбордов)
Дашборды не имеют действия export. Требуется ручной перенос через веб-интерфейс или другие инструменты.

Основные дашборды CoffeeGate:
- application, application_v2
- webCMSummary, webCMSummary_v2
- webCMNanagement
- webCMLog
- webCMAdvertising
- webCMpasswordList
- webCmCard
- webMonitoring, webMonitoring_v2
- webObjectCard, webObjectCard_v2
- reportFlushing, reportFlushing_v2
- reportOnDdrinks, reportOnDdrinks_v2
- reportDowntime, reportIdles_v2
- reportOnCounters, reportOnCounters_v2
- reportMonitioring_v2
- listIncident
- serviceIngineer
- и другие модальные окна (web_request_modal_*)

#### 2. Устройства (3 устройства)
Устройства не имеют действия export. Требуется ручной перенос:
- WMF_K031920_17807
- drCoffeeNative_220625007
- drCoffeeTest_220625007

#### 3. Тревоги (2 тревоги)
Тревоги не имеют действия export. Требуется ручной перенос:
- cmIncidentCreatedAlert
- cmIncidentsOutOfTime

#### 4. Виджеты
Виджеты CoffeeGate (например, cmEngineer) требуют ручного переноса.

#### 5. Отчеты
Отчеты CoffeeGate (cmDispatcherLogReport, cmDowntimeReport_*, cmReportFlushing, cmReportOnDrinks, cmServiceCountersReport) требуют ручного переноса.

### 📋 Следующие шаги

1. **Перенос переменных, функций и событий моделей:**
   - Для каждой модели нужно:
     - Получить список переменных с исходного сервера
     - Создать переменные на localhost с правильными форматами
     - Импортировать данные переменных
     - Создать функции (используя aggregate_build_expression и aggregate_validate_expression)
     - Создать события
     - Настроить привязки (bindings)

2. **Перенос дашбордов:**
   - Использовать веб-интерфейс AggreGate для копирования дашбордов
   - Или использовать Python скрипт с API для получения XML шаблонов дашбордов

3. **Перенос устройств:**
   - Создать устройства на localhost
   - Настроить подключения и параметры

4. **Перенос тревог:**
   - Создать тревоги на localhost
   - Настроить условия и действия

5. **Перенос виджетов и отчетов:**
   - Использовать веб-интерфейс или API для переноса

### 📊 Статистика

- **Модели:** 50+ создано
- **Приложения:** 3 экспортировано
- **Дашборды:** 60+ требуют переноса
- **Устройства:** 3 требуют переноса
- **Тревоги:** 2 требуют переноса
- **Виджеты:** требуют переноса
- **Отчеты:** требуют переноса

### ⚙️ Технические детали

Все модели созданы с правильными типами:
- **Абсолютные модели (type=1):** большинство моделей
- **Относительные модели (type=0):** cmUsersProperties, cmConnectors_agents, WMFFunctions
- **Instance модели (type=2):** cmCoffeeMachinesItems, cmObjectsItems, cmAgentLtAgents, cmSites, cmMachineTypes

При создании относительных моделей указаны:
- containerType: "objects" или "devices"
- objectType: "object" или "device"
