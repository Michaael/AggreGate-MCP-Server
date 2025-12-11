# Улучшенная структура проекта AggreGate

## Принципы улучшения

### 1. Единый стандарт именования (camelCase)

**До:**
- `TRV1`, `TRV2` (непонятные сокращения)
- `SCUD` (аббревиатура без расшифровки)
- `engineering_systems` (snake_case)
- `coldWaterMeters1` (непоследовательность)
- `bff_Lims` (смешанный стиль)

**После:**
- `thermostaticValve1`, `thermostaticValve2` (понятные имена)
- `accessControlSystem` (полное название)
- `engineeringSystem` (camelCase)
- `coldWaterMeter` (единообразие)
- `limsBackend` (camelCase)

### 2. Логическая группировка

Модели организованы по функциональным областям для лучшей навигации и понимания.

### 3. Понятные описания

Все объекты имеют описательные названия и описания на русском языке.

## Новая структура моделей

```
users.admin.models/
  ├── objects                    # Управление объектами недвижимости
  ├── meters                     # Счетчики (общая модель)
  ├── devices                    # Устройства (общая модель)
  ├── personnel                  # Персонал
  │
  ├── engineering/              # Инженерные системы
  │   ├── pump                  # Насос
  │   ├── pumpStation           # Насосная станция
  │   ├── pumpUnit              # Насосный агрегат
  │   ├── ventilation           # Вентиляция
  │   ├── ventilationSystem     # Система вентиляции
  │   ├── compressorUnit        # Компрессорная установка
  │   ├── compressUnit          # Компрессорный агрегат
  │   ├── frozenUnit            # Холодильная установка
  │   ├── condensator           # Конденсатор
  │   ├── hydroaccumulator      # Гидроаккумулятор
  │   ├── thermalPoint          # Тепловая точка
  │   ├── thermalSystem         # Тепловая система
  │   ├── thermostatValve1      # Термостатический вентиль 1
  │   └── thermostatValve2     # Термостатический вентиль 2
  │
  ├── safety/                    # Системы безопасности
  │   ├── fireAlarmSystem       # Система пожарной сигнализации
  │   ├── fireSystem            # Пожарная система
  │   ├── smokeDetector         # Дымовой датчик
  │   ├── siren                 # Сирена оповещения
  │   └── accessControlSystem    # Система контроля доступа (SCUD)
  │
  ├── monitoring/                # Мониторинг
  │   ├── sensor                # Датчик
  │   ├── temperatureSensor     # Датчик температуры
  │   ├── pressureGauge        # Манометр
  │   └── pressureSwitch        # Реле давления
  │
  ├── meters/                    # Счетчики
  │   ├── coldWaterMeter        # Счетчик холодной воды
  │   ├── hotWaterMeter         # Счетчик горячей воды
  │   ├── electricMeter         # Электросчетчик
  │   ├── gasMeter              # Газовый счетчик
  │   ├── heatMeter             # Счетчик тепла
  │   └── absoluteMeter         # Абсолютный счетчик
  │
  ├── it/                        # IT инфраструктура
  │   ├── itInfrastructure      # IT инфраструктура
  │   ├── servers               # Серверы
  │   ├── networkDevice         # Сетевое устройство
  │   └── itServices            # IT сервисы
  │
  ├── visualization/             # Визуализация
  │   ├── graphics              # Графика
  │   ├── graphApplication      # График приложений
  │   ├── graphEngineering      # График инженерных систем
  │   └── graphItInfrastructure # График IT инфраструктуры
  │
  ├── integration/               # Интеграции
  │   ├── lims/
  │   │   ├── limsBackend        # Backend для LIMS
  │   │   ├── limsBackendNew    # Новый backend для LIMS
  │   │   └── limsWorker        # Worker для LIMS
  │   ├── rest/
  │   │   ├── restApi           # REST API
  │   │   └── restArenaApi      # REST API Arena
  │   ├── telegram/
  │   │   └── telegramConnector # Коннектор Telegram
  │   └── stomp/
  │       └── stomp              # STOMP протокол
  │
  ├── simulation/                # Имитаторы
  │   ├── devices/
  │   │   ├── simulatorDeviceAccess        # Имитатор устройства доступа
  │   │   ├── simulatorDeviceElectricMeter # Имитатор электросчетчика
  │   │   ├── simulatorDeviceFire          # Имитатор пожарной сигнализации
  │   │   ├── simulatorDeviceIt            # Имитатор IT устройства
  │   │   ├── simulatorDevicePump          # Имитатор насоса
  │   │   ├── simulatorDeviceVent          # Имитатор вентиляции
  │   │   ├── simulatorDeviceWaterMeter    # Имитатор счетчика воды
  │   │   └── simulatorDeviceWarmMeter     # Имитатор счетчика тепла
  │   ├── logs/
  │   │   ├── simulatorLogAccess           # Лог имитатора доступа
  │   │   ├── simulatorLogEnergy           # Лог имитатора энергетики
  │   │   ├── simulatorLogFireControl      # Лог имитатора пожарного контроля
  │   │   ├── simulatorLogIt               # Лог имитатора IT
  │   │   ├── simulatorLogLifts            # Лог имитатора лифтов
  │   │   ├── simulatorLogPumpControl      # Лог имитатора управления насосами
  │   │   ├── simulatorLogRefrigerators    # Лог имитатора холодильников
  │   │   ├── simulatorLogSewerage         # Лог имитатора канализации
  │   │   ├── simulatorLogTermal           # Лог имитатора тепловых систем
  │   │   ├── simulatorLogVent              # Лог имитатора вентиляции
  │   │   └── simulatorLogVideo             # Лог имитатора видеонаблюдения
  │   └── meters/
  │       ├── coldWaterSimulator             # Имитатор холодной воды
  │       ├── hotWaterSimulator             # Имитатор горячей воды
  │       ├── electricMeterSimulator        # Имитатор электросчетчика
  │       └── gasMeterSimulator            # Имитатор газового счетчика
  │
  └── utilities/                  # Вспомогательные
      ├── geocoding              # Геокодирование
      ├── fileManager            # Файловый менеджер
      ├── barCodeGenerator       # Генератор штрих-кодов
      ├── barCodePresenter       # Презентатор штрих-кодов
      ├── frontendConfig         # Конфигурация фронтенда
      └── system                 # Системные настройки
```

## Улучшенные устройства

```
users.admin.devices/
  ├── dataCenterDevice1          # Устройство управления дата-центром 1
  ├── dataCenterDevice2          # Устройство управления дата-центром 2
  ├── geoService                 # Геосервис
  ├── nameGenerator              # Генератор имен
  ├── nameGeneratorFemale       # Генератор женских имен
  ├── nameGeneratorMale         # Генератор мужских имен
  ├── surnameGenerator           # Генератор фамилий
  ├── limsRandomizer            # Рандомизатор для LIMS
  ├── telegramBot               # Telegram бот
  └── virtualDevice             # Виртуальное устройство
```

## Применяемые лучшие практики

### 1. Именование
- ✅ camelCase для всех имен
- ✅ Понятные имена без сокращений
- ✅ Единообразные суффиксы

### 2. Описания
- ✅ Описательные названия на русском
- ✅ Понятные описания для всех объектов
- ✅ Help-тексты для переменных

### 3. Группировка
- ✅ Группировка переменных по функциональным областям
- ✅ Логическая организация моделей
- ✅ Иерархическая структура

### 4. Права доступа
- ✅ Оптимизация прав доступа
- ✅ Минимальные необходимые права
- ✅ Разделение прав чтения и записи

### 5. Производительность
- ✅ Оптимизация привязок
- ✅ Правильные периоды обновления
- ✅ Использование onevent вместо periodically где возможно

### 6. Документация
- ✅ Описания для всех объектов
- ✅ Комментарии к сложным привязкам
- ✅ Документация структуры проекта

## Маппинг старых имен на новые

| Старое имя | Новое имя | Категория |
|-----------|-----------|-----------|
| `TRV1` | `thermostaticValve1` | engineering |
| `TRV2` | `thermostaticValve2` | engineering |
| `SCUD` | `accessControlSystem` | safety |
| `engineering_systems` | `engineeringSystem` | engineering |
| `coldWaterMeters1` | `coldWaterMeter` | meters |
| `hotWaterMeters` | `hotWaterMeter` | meters |
| `electrMeters` | `electricMeter` | meters |
| `gazMeters` | `gasMeter` | meters |
| `dataColdImitator` | `coldWaterSimulator` | simulation |
| `dataHotImitator` | `hotWaterSimulator` | simulation |
| `dataElectrImitator` | `electricMeterSimulator` | simulation |
| `dataGazImitator` | `gasMeterSimulator` | simulation |
| `imDevAccess` | `simulatorDeviceAccess` | simulation |
| `imLogAccess` | `simulatorLogAccess` | simulation |
| `bff_Lims` | `limsBackend` | integration |
| `bffLimsN` | `limsBackendNew` | integration |
| `worker_Lims` | `limsWorker` | integration |
| `restAPI` | `restApi` | integration |
| `dataCenterManagementDevice1` | `dataCenterDevice1` | devices |
| `nameFemale` | `nameGeneratorFemale` | devices |
| `nameMale` | `nameGeneratorMale` | devices |

## Преимущества новой структуры

1. **Понятность** - все имена описывают назначение объекта
2. **Единообразие** - единый стиль именования по всему проекту
3. **Навигация** - логическая группировка упрощает поиск
4. **Масштабируемость** - легко добавлять новые компоненты
5. **Поддерживаемость** - понятная структура упрощает поддержку

