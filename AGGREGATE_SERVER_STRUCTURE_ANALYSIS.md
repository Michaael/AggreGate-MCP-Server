# Анализ структуры сервера AggreGate

**Дата анализа:** 2025-01-27  
**Адрес сервера:** 62.109.25.124:6460  
**Пользователь:** admin

## 1. Информация о сервере

### Подключение
- **Статус:** Подключено и авторизовано
- **Хост:** 62.109.25.124
- **Порт:** 6460
- **Пользователь:** admin

### Возможности сервера
Сервер поддерживает следующие функции:
- ✅ Модели (Models)
- ✅ Устройства (Devices)
- ✅ Агенты (Agents)
- ✅ Виджеты (Widgets)
- ✅ Дашборды (Dashboards)
- ✅ События (Events)
- ✅ Функции (Functions)
- ✅ Переменные (Variables)

## 2. Пользователи

На сервере зарегистрировано **3 пользователя**:

| Пользователь | Путь | Описание | Дата создания |
|--------------|------|----------|---------------|
| admin | users.admin | admin (Administrator) | 2025-01-13 |
| user1 | users.user1 | user1 | 2025-01-16 |
| user2 | users.user2 | user2 | 2025-01-16 |

## 3. Структура пользователя admin

### 3.1. Устройства (Devices)

**Количество устройств:** 1

#### virtualDevice
- **Путь:** `users.admin.devices.virtualDevice`
- **Описание:** Виртуальное устройство для тестирования
- **Драйвер:** `com.tibbo.linkserver.plugin.device.virtual` (Virtual Device)
- **Статус:** Активно

**Основные переменные устройства:**
- `sine` - Sine Wave (только чтение)
- `sawtooth` - Sawtooth Wave (только чтение)
- `square` - Square Wave (только чтение)
- `triangle` - Triangle Wave (только чтение)
- `random` - Random Value (только чтение)
- `sumWaves` - Сумма значений Sine Wave и Sawtooth Wave
- `table` - Tabular Setting
- `int`, `float`, `string`, `boolean` - Настройки различных типов
- `date` - Date/Time Setting
- `position`, `track` - Location данные
- `errorGenerator`, `shouldGenerateError` - Error Generators

**Функции устройства:**
- `calculate()` - Calculate (группа: remote)
- `generateEvent()` - Generate Event (группа: remote)
- `synchronize()` - Synchronize (группа: system)
- `reset()` - Reset Device Driver (группа: system)
- И другие системные функции

**События устройства:**
- `event1` - Virtual Device Event #1 (группа: remote, уровень: INFO)
- `event2` - Virtual Device Event #2 (группа: remote, уровень: INFO)
- `info` - Information (группа: default, уровень: WARNING)
- Системные события

### 3.2. Модели (Models)

**Количество моделей:** 5

1. **alarmDelayedSum**
   - Путь: `users.admin.models.alarmDelayedSum`
   - Описание: Тревога через 10 секунд после того, как сумма Int+Float попадает в диапазон 50-100
   - Тип: Модель

2. **alarmEvent1Event2**
   - Путь: `users.admin.models.alarmEvent1Event2`
   - Описание: Тревога, срабатывающая на Event 1 и деактивирующаяся при Event 2 (Int > 20)
   - Тип: Модель

3. **alarmTableSum**
   - Путь: `users.admin.models.alarmTableSum`
   - Описание: Тревога на сумму Int в Table > 100 с корректирующим действием
   - Тип: Модель

4. **calculatorModel**
   - Путь: `users.admin.models.calculatorModel`
   - Описание: Модель калькулятора, вызывающая calculate() виртуального устройства
   - Тип: Модель

5. **sumWaveModel**
   - Путь: `users.admin.models.sumWaveModel`
   - Описание: Относительная модель, хранящая сумму Sine Wave и Sawtooth Wave
   - Тип: Модель

### 3.3. Тревоги (Alerts)

**Количество тревог:** 3

1. **alarmEvent1Event2** - Тревога, срабатывающая на Event 1 и деактивирующаяся при Event 2 (Int > 20)
2. **alarmDelayedSum** - Тревога через 10 секунд после того, как сумма Int+Float попадает в диапазон 50-100
3. **alarmTableSum** - Тревога на сумму Int в Table > 100 с корректирующим действием

### 3.4. Дашборды (Dashboards)

**Количество дашбордов:** 95+

Основные категории дашбордов:
- **IoT Application Development Guide** - Руководство по разработке IoT приложений
- **Network Management** - Управление сетью (Router, Access Point, Network Device, Interface Chart, и др.)
- **Data Center Management** - Управление дата-центром
- **Virtualization** - Виртуализация (VMware, Hypervisors)
- **VoIP** - VoIP мониторинг (Cisco Unified Communications Manager, Asterisk)
- **Database** - Базы данных (Oracle, MySQL, PostgreSQL, MS SQL, и др.)
- **Application Servers** - Серверы приложений (Apache, Tomcat, JBoss, WebLogic, GlassFish, и др.)
- **Microsoft Services** - Сервисы Microsoft (Active Directory, Exchange, IIS, SharePoint, DNS)
- **Monitoring** - Мониторинг (IP SLA, Ping, Jitter, Interface Traffic)
- **Charts** - Графики (Simple Line Chart, Bar Chart, Area Chart, Pie Chart)
- **CMDB** - Configuration Management Database
- **Process Control** - Управление процессами
- **Custom** - Пользовательские дашборды:
  - `virtualDevicesDashboard` - Дашборд для виртуальных устройств

### 3.5. Виджеты (Widgets)

**Количество виджетов:** 200+

Основные категории виджетов:

#### Пользовательские виджеты (для тестирования):
- `test_mcp_widget` - Test widget for MCP
- `test_widget_full` - Test widget for full MCP testing
- `test_widget_mcp` - Test widget for MCP
- Множество тестовых виджетов с временными метками

#### Виджеты для виртуального устройства:
- `sineHistoryWidget` - Виджет истории переменной sine за 5 минут
- `sineSawtoothChart` - Виджет графика Sine Wave и Sawtooth Wave
- `pieChartWidget` - Виджет Pie Chart по табличной переменной
- `tablePieChart` - Pie Chart по табличной переменной

#### Виджеты управления:
- `buttonWidget`, `svgButtonWidget` - Виджеты кнопок с SVG
- `fanWidget`, `svgFanWidget` - Виджеты вентилятора с SVG
- `buttonFanController` - Виджет управления вентилятором через кнопку
- `fanControlWidget` - Виджет управления вентилятором с сабвиджетами
- `buttonOpenWidget`, `widgetOpener` - Виджеты с кнопкой, открывающей другой виджет
- `formWidget`, `gridFormWidget`, `gridLayoutForm` - Виджеты с формами Grid Layout
- `calculatorWidget` - Калькулятор в виде виджета (grid layout)

#### Виджеты событий:
- `eventFilterWidget` - Виджет Event Log с фильтром событий
- `eventGeneratorWidget`, `eventGeneratorWithLog` - Виджеты генерации событий с логом

#### Мониторинг сети:
- `interfaceTraffic`, `interfaceTrafficSingle` - Трафик интерфейсов
- `interfaceUtilization` - Использование пропускной способности интерфейсов
- `interfaceErrors`, `interfaceDiscards` - Ошибки и отброшенные пакеты
- `pingTime`, `pingPacketLoss` - Ping мониторинг
- `trafficTop10` - Top 10 интерфейсов по трафику

#### Мониторинг системы:
- `cpuLoad`, `cpuTop10` - Загрузка CPU
- `memoryUtilization`, `memoryUsageTop10` - Использование памяти
- `diskUtilization` - Использование диска
- `volumesUtilizationTop10`, `volumesFreeSpaceTop10` - Использование томов

#### Виртуализация:
- `vmWareInfo` - Информация о VMware
- `vmsCounter` - Счетчик виртуальных машин
- `hypervisorMainCounters` - Основные счетчики гипервизора
- Виджеты графиков для CPU, Memory, Disk, Network использования

#### VoIP:
- `voipCallManagerJitter`, `voipCallManagerLatency` - Jitter и Latency
- `voipCallManagerMos` - MOS (Mean Opinion Score)
- `voipCallManagerPacketLoss` - Потеря пакетов
- `voipCallManagerStatistics` - Статистика телефонов

#### IP SLA:
- `testsOverview` - Обзор тестов
- `widgetMinMaxAvgRtt` - Round Trip Time
- Виджеты для различных типов тестов (Jitter, ICMP Jitter, Path Echo, HTTP)

#### Data Center:
- `dataCenter3Levels` - 3-уровневый дата-центр
- `dataCenterGeneralPlan` - Общий план дата-центра
- `dataCenterNetworkMap` - Сетевая карта дата-центра
- Виджеты температуры и влажности на разных уровнях
- `dataCenterPUE` - Power Usage Effectiveness

#### NetFlow:
- `top10Applications`, `top10Protocols` - Top 10 приложений и протоколов
- `top10Conversations`, `top10Endpoints` - Top 10 разговоров и конечных точек
- `top10Countries` - Top 10 стран
- `totalBytesTransferred`, `totalPacketsTransferred` - Общий трафик

#### IoT Guide виджеты:
- `iotGuideAlerts`, `iotGuideDashboards`, `iotGuideWidgets` - Управление тревогами, дашбордами, виджетами
- `iotGuideEventLogs`, `iotGuideReports` - Управление логами событий и отчетами
- `iotGuideUsers`, `iotGuideClasses` - Управление пользователями и классами
- И другие виджеты управления платформой

### 3.6. Запросы (Queries)

**Количество запросов:** 80+

Основные категории:
- **IP SLA** - Запросы для IP SLA тестов
- **Availability** - Доступность устройств
- **CPU** - Использование CPU
- **Memory** - Использование памяти
- **Traffic** - Трафик и пропускная способность
- **Processes** - Процессы/Сервисы
- **Space** - Использование дискового пространства
- **VMware** - Виртуализация VMware
- **VoIP** - VoIP мониторинг

Примеры запросов:
- `bandwidthOver50`, `bandwidthOver90` - Использование пропускной способности > 50%/90%
- `cpuOver50`, `cpuOver90` - Загрузка CPU > 50%/90%
- `memoryOver75`, `memoryOver90` - Использование памяти > 75%/90%
- `pingPacketLoss`, `pingTime` - Потеря пакетов и время отклика Ping
- `trafficTop10`, `trafficTop50` - Top 10/50 интерфейсов по трафику
- `vmWareSummary`, `vmWareMemoryUtil` - Сводка и использование памяти VMware

### 3.7. Отчеты (Reports)

**Количество отчетов:** 6

1. `interfaceBandwidth` - Interface Bandwidth Summary
2. `interfaceTraffic` - Interface Traffic Summary
3. `pingAvailabilityNot100` - Availability Not 100%
4. `vmWareMemoryUtil` - VMware Memory Usage
5. `vmWareSummary` - VMware Summary
6. `tableReport` - Отчет по табличной переменной виртуального устройства

### 3.8. Workflows (Рабочие процессы)

**Количество workflows:** 3

1. `collatzConjecture` - The Collatz Conjecture
2. `mlDemo` - Machine Learning Demo
3. `setAcknowledgementMessage` - Set Acknowledgement Message

### 3.9. Machine Learning

**Количество trainable units:** 1

- `trainableUnit` - Machine Learning and Workflow Demo

### 3.10. Скрипты (Scripts)

**Количество скриптов:** 2

1. `deviceInfo` - Device Information
2. `slaBreakdown` - SLA Breakdown Date Calculator

### 3.11. Приложения (Applications)

**Количество приложений:** 5

1. `agricultureManagement` - Smart Agriculture
2. `diagnostics` - diagnostics
3. `networkManager` - Network Manager
4. `platformAdministrationKit` - Platform Administration Kit
5. `scada` - SCADA/HMI

### 3.12. Классы (Classes)

**Количество классов:** 12

- `access_point` - Access Point
- `cluster` - Cluster
- `db` - Database
- `department` - Department
- `ipaddress` - IP Address
- `ipphone` - VoIP
- `person` - Person
- `purchase` - Purchase
- `rack` - Rack
- `router` - Router
- `server` - Server
- `storage` - Storage
- `uninterruptible_power_supply` - UPS
- `workstation` - Workstation

### 3.13. Фильтры событий (Event Filters)

**Количество фильтров:** 3

1. `ciLog` - Configuration Items Log
2. `sanOverheat` - Overheat
3. `virtualizationEvents` - Virtualization Events

### 3.14. Process Control

**Количество программ:** 7

1. `boiler` - Boiler
2. `combinationLock` - Combination Lock
3. `execute` - Execute
4. `mainPump` - Pump Demo
5. `pump1` - Pump 1
6. `pump2` - Pump 2
7. `trafficLight` - Traffic Light

### 3.15. IP SLA

**Количество групп тестов:** 13

- `dhcpGroup` - DHCP
- `dnsGroup` - DNS
- `echoGroup` - Echo
- `ftpGroup` - FTP
- `httpGroup` - HTTP
- `icmpJitterGroup` - ICMP Jitter
- `jitterGroup` - Jitter
- `pathEchoGroup` - Path Echo
- `rtpGroup` - RTP
- `tcpConnectGroup` - TCP Connect
- `udpEchoGroup` - UDP Echo
- `voipGroup` - VoIP Call
- `dlswGroup` - DLSw

### 3.16. Common Data

**Общие таблицы данных:**
- `applicationTypes` - Application Type
- `assetEvents` - Asset Events
- `assetInformation` - Asset Information
- `assetLocation` - Asset Location
- `deviceTypes` - Network Device Types
- `interfaceTypes` - Network Interface Types

## 4. Структура других пользователей

### 4.1. user1

**Устройства:** Нет устройств

**Структура:** Стандартная структура контекстов без пользовательских данных

### 4.2. user2

**Устройства:** 1 устройство
- `virtualDevice1` - Виртуальное устройство user2
  - Путь: `users.user2.devices.virtualDevice1`

**Структура:** Стандартная структура контекстов с одним виртуальным устройством

## 5. Статистика

### Общая статистика по пользователю admin:

- **Устройства:** 1
- **Модели:** 5
- **Тревоги:** 3
- **Дашборды:** 95+
- **Виджеты:** 200+
- **Запросы:** 80+
- **Отчеты:** 6
- **Workflows:** 3
- **Machine Learning units:** 1
- **Скрипты:** 2
- **Приложения:** 5
- **Классы:** 12
- **Фильтры событий:** 3
- **Process Control программы:** 7
- **IP SLA группы:** 13

## 6. Выводы

1. **Сервер активно используется** для разработки и тестирования IoT приложений
2. **Основной пользователь** - admin, имеет обширную конфигурацию с множеством компонентов
3. **Виртуальное устройство** используется для тестирования различных функций платформы
4. **Множество моделей и тревог** демонстрируют работу системы мониторинга и оповещений
5. **Богатая библиотека виджетов и дашбордов** показывает развитую экосистему визуализации
6. **Поддержка различных технологий:** Network Management, Virtualization, VoIP, Database, Application Servers
7. **Наличие тестовых виджетов** указывает на активную разработку через MCP интерфейс

## 7. Рекомендации

1. Рассмотреть очистку тестовых виджетов с временными метками
2. Документировать пользовательские модели и их назначение
3. Проверить активность других пользователей (user1, user2)
4. Регулярно проверять статус устройств и их подключение
5. Мониторить использование ресурсов сервера при большом количестве компонентов
