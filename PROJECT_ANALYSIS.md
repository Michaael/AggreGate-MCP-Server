# Анализ проекта AggreGate: Структура и назначение

## Общее описание проекта

Этот проект представляет собой **AggreGate SDK** - набор инструментов и примеров для работы с платформой Tibbo AggreGate. Проект включает:

1. **MCP Server** - сервер для интеграции Cursor IDE с AggreGate
2. **Демонстрационные примеры** - примеры использования различных возможностей AggreGate
3. **Демонстрационный проект управления дата-центром** - полноценный пример IoT приложения

---

## Структура проекта


## Что делает проект на сервере

### 1. Демонстрационный проект управления дата-центром

**Модель:** `users.admin.models.dataCenterManagementDemo`

Это полноценный пример IoT приложения для мониторинга дата-центра, который демонстрирует:

#### Переменные модели (26 переменных):

**Климатические параметры:**
- `temperature` - температура (top, middle, bottom)
- `humidity` - влажность (top, middle, bottom)
- `avgTemperature`, `maxTemperature`, `minTemperature` - статистика температуры
- `avgHumidity`, `maxHumidity`, `minHumidity` - статистика влажности

**Энергетические параметры:**
- `PUE` (Power Usage Effectiveness) - эффективность использования энергии

**Системы безопасности:**
- `fireAlarm` - пожарная сигнализация
- `camsOnline` - количество онлайн камер

**Системы резервного питания:**
- `UPS1` - `UPS10` - источники бесперебойного питания (заряд, состояние)
- `generator1` - `generator4` - генераторы (уровень топлива, состояние)

**Счетчики:**
- `alarmCounter` - счетчик тревог (критические, некритические)

#### Привязки (Bindings) - 35 привязок

Все переменные автоматически вычисляются через привязки, которые используют данные от виртуальных устройств:

**Примеры привязок:**

```javascript
// Температура верхнего уровня
temperature$topTemperature = {random$value} + 17

// Влажность верхнего уровня
humidity$topHumidity = round({sawtooth$value} + 35 + {random$value} / 2.8564)

// Средняя температура
avgTemperature$value = ({temperature$topTemperature}+{temperature$middleTemperature}+{temperature$bottomTemperature}) / 3

// Максимальная температура
maxTemperature$value = max(max({temperature$topTemperature},{temperature$middleTemperature}),{temperature$bottomTemperature})

// PUE (ограниченный диапазон 1-5)
PUE$value = {random$value} < 1 ? 1 : {random$value} > 5 ? 5 : {random$value}

// Пожарная сигнализация (вероятность 10%)
fireAlarm$value = {random$value} < 1 ? true : false

// Заряд UPS
UPS1$chargeLevel = 100 - 5 / {random$value}

// Уровень топлива генератора
generator1$fuelLevel = 100 - {random$value}
```

**Характеристики привязок:**
- Все привязки выполняются `onstartup` (при старте)
- Все привязки выполняются `periodically` (периодически)
- Период выполнения: 5000 мс (5 секунд)
- Используются виртуальные устройства: `random`, `sawtooth` для генерации данных

#### Устройства

**Устройство 1:** `users.admin.devices.dataCenterManagementDevice1`
- Использует виртуальный драйвер (`com.tibbo.linkserver.plugin.device.virtual`)
- Генерирует тестовые данные для демонстрации
- Содержит переменные: `sine`, `sawtooth`, `triangle`, `random`, `int`, `table` и другие

**Устройство 2:** `users.admin.devices.dataCenterManagementDevice2`
- Аналогичное виртуальное устройство

---

### 2. Другие модели на сервере

На сервере присутствует **35+ моделей**, демонстрирующих различные аспекты мониторинга:

#### Мониторинг системных ресурсов:
- `cpuLoad` - загрузка CPU
- `memoryUtilization` - использование памяти
- `diskUtilization` - использование диска
- `interfaceTraffic` - трафик сетевых интерфейсов

#### Мониторинг виртуализации:
- `virtualizationMonitoring` - мониторинг виртуализации
- `virtualizationOverview` - обзор виртуализации

#### Мониторинг сетевых устройств:
- `routerStatusElaboration` - статус роутеров
- `serviceAvailability` - доступность сервисов
- `subnet` - мониторинг подсетей

#### Мониторинг оборудования:
- `hardwareHealth` - здоровье оборудования
- `ipmiSensors` - IPMI датчики
- `sanMonitoring` - мониторинг SAN

#### Специализированные модели:
- `cmdb` - Configuration Management Database
- `deviceInventory` - инвентаризация устройств
- `deviceTypeDetector` - определение типов устройств
- `geofence` - геозоны
- `metric` - метрики

---

### 3. Виджеты и дашборды

**100+ виджетов** для визуализации данных:
- `dataCenterTemperature` - температура дата-центра
- `dataCenterHumidity` - влажность дата-центра
- `dataCenterPUE` - PUE дата-центра
- `cpuLoad` - загрузка CPU
- `memoryUtilization` - использование памяти
- И многие другие...

**100+ дашбордов** для организации интерфейса:
- `dataCenterManagementDemo` - дашборд управления дата-центром
- `virtualizationOverview` - обзор виртуализации
- `networkOverview` - обзор сети
- И другие...

---

## Архитектурные паттерны проекта

### 1. Использование моделей для бизнес-логики

Модели используются для:
- **Агрегации данных** - объединение данных от нескольких устройств
- **Вычислений** - расчет производных метрик (средние, максимумы, минимумы)
- **Бизнес-логики** - правила и условия для обработки данных

**Пример из dataCenterManagementDemo:**
```javascript
// Вычисление средней температуры
avgTemperature$value = ({temperature$topTemperature}+{temperature$middleTemperature}+{temperature$bottomTemperature}) / 3

// Вычисление максимальной температуры
maxTemperature$value = max(max({temperature$topTemperature},{temperature$middleTemperature}),{temperature$bottomTemperature})
```

### 2. Использование привязок для автоматизации

Привязки обеспечивают:
- **Автоматическое обновление** - значения обновляются при изменении зависимостей
- **Реактивность** - система реагирует на изменения в реальном времени
- **Периодические обновления** - регулярное обновление значений

**Характеристики привязок в проекте:**
- Период: 5000 мс (5 секунд)
- Триггеры: `onstartup` + `periodically`
- Использование виртуальных устройств для генерации данных

### 3. Организация переменных по группам

Переменные организованы в группы для лучшей структуризации:
- `UPS` - группа для UPS устройств
- `Generators` - группа для генераторов
- `Data Center Management Demo` - основная группа

### 4. Использование виртуальных устройств

Виртуальные устройства используются для:
- **Демонстрации** - показ возможностей системы без реального оборудования
- **Тестирования** - тестирование логики и визуализации
- **Разработки** - разработка приложений без физических устройств

---

## Лучшие практики, выявленные в проекте

### 1. Структурирование переменных

**Правильно:**
- Использование групп для организации переменных
- Понятные имена переменных (`topTemperature`, `avgTemperature`)
- Описания для всех переменных

**Пример:**
```
temperature (группа: Data Center Management Demo)
  ├── topTemperature
  ├── middleTemperature
  └── bottomTemperature
```

### 2. Использование привязок

**Правильно:**
- Привязки для вычисляемых значений (средние, максимумы, минимумы)
- Периодические обновления для динамических данных
- Использование функций (`max`, `min`, `round`) в выражениях

**Пример:**
```javascript
// Вычисление максимума
maxTemperature$value = max(max({temperature$topTemperature},{temperature$middleTemperature}),{temperature$bottomTemperature})

// Ограничение диапазона
PUE$value = {random$value} < 1 ? 1 : {random$value} > 5 ? 5 : {random$value}
```

### 3. Организация моделей

**Правильно:**
- Одна модель = одна область функциональности
- Модели для агрегации данных от устройств
- Модели для вычислений и бизнес-логики

**Примеры:**
- `dataCenterManagementDemo` - управление дата-центром
- `cpuLoad` - загрузка CPU
- `virtualizationMonitoring` - мониторинг виртуализации

### 4. Использование виртуальных устройств

**Правильно:**
- Виртуальные устройства для демонстрации и тестирования
- Использование различных типов волн (`sine`, `sawtooth`, `random`)
- Комбинирование данных от разных источников

---

## Как правильно работать с AggreGate на основе этого проекта

### 1. Создание модели для мониторинга

**Шаг 1:** Создать модель
```json
{
  "name": "aggregate_create_context",
  "arguments": {
    "parentPath": "users.admin.models",
    "name": "myMonitoring",
    "description": "My Monitoring Model"
  }
}
```

**Шаг 2:** Создать переменные модели
```json
{
  "name": "aggregate_create_variable",
  "arguments": {
    "path": "users.admin.models.myMonitoring",
    "variableName": "temperature",
    "format": "<value><E>",
    "description": "Temperature",
    "writable": false
  }
}
```

**Шаг 3:** Создать привязки для автоматического вычисления
```json
{
  "name": "aggregate_set_variable",
  "arguments": {
    "path": "users.admin.models.myMonitoring",
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
        "target": "temperature@",
        "expression": "{users.admin.devices.device1:sine} * 30 + 20",
        "onstartup": true,
        "onevent": false,
        "periodically": true,
        "period": 5000
      }]
    }
  }
}
```

### 2. Работа с устройствами

**Создание виртуального устройства:**
```json
{
  "name": "aggregate_create_device",
  "arguments": {
    "username": "admin",
    "deviceName": "myDevice",
    "description": "My Virtual Device",
    "driverId": "com.tibbo.linkserver.plugin.device.virtual"
  }
}
```

**Использование данных устройства в привязках:**
```javascript
// Использование синусоидальной волны
{users.admin.devices.myDevice:sine} * 100

// Использование случайных значений
{users.admin.devices.myDevice:random} * 50

// Комбинирование данных
{users.admin.devices.myDevice:sine} + {users.admin.devices.myDevice:random} / 2
```

### 3. Создание вычисляемых метрик

**Вычисление средних значений:**
```javascript
avgValue$value = ({value1} + {value2} + {value3}) / 3
```

**Вычисление максимумов:**
```javascript
maxValue$value = max(max({value1}, {value2}), {value3})
```

**Вычисление минимумов:**
```javascript
minValue$value = min(min({value1}, {value2}), {value3})
```

**Ограничение диапазона:**
```javascript
limitedValue$value = {rawValue} < 0 ? 0 : {rawValue} > 100 ? 100 : {rawValue}
```

### 4. Организация переменных

**Использование групп:**
- Создавайте группы для логической организации переменных
- Используйте вложенные группы для сложных структур

**Пример:**
```
Data Center Management Demo
├── Temperature
│   ├── topTemperature
│   ├── middleTemperature
│   └── bottomTemperature
├── UPS
│   ├── UPS1
│   ├── UPS2
│   └── ...
└── Generators
    ├── generator1
    └── ...
```

### 5. Периодические обновления

**Настройка периодических привязок:**
- Используйте `periodically: true` для регулярных обновлений
- Устанавливайте разумный период (минимум 1000 мс)
- Используйте `onevent: true` для реактивных обновлений

**Пример:**
```javascript
{
  "target": "temperature@",
  "expression": "{device1:sine} * 30 + 20",
  "onstartup": true,
  "onevent": false,
  "periodically": true,
  "period": 5000  // 5 секунд
}
```

---

## Выводы

Этот проект демонстрирует:

1. **Правильную архитектуру** - использование моделей для бизнес-логики, устройств для данных
2. **Автоматизацию** - использование привязок для автоматических вычислений
3. **Структурирование** - организация переменных по группам
4. **Масштабируемость** - поддержка множества моделей и устройств
5. **Визуализацию** - использование виджетов и дашбордов для отображения данных

Проект служит отличным примером для изучения и создания собственных IoT приложений на платформе AggreGate.

