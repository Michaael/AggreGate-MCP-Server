# План миграции и улучшения проекта AggreGate

## Стандарт именования ресурсов

### Принципы именования

1. **camelCase** для всех имен контекстов, переменных, функций
2. **Понятные имена** - избегать сокращений без расшифровки
3. **Группировка по функциональным областям**
4. **Единообразие** - одинаковые суффиксы для однотипных объектов

### Преобразование имен

#### Модели оборудования:
- `pump` → `pump` ✅ (уже хорошо)
- `pumpStation` → `pumpStation` ✅
- `pumpUnit` → `pumpUnit` ✅
- `TRV1`, `TRV2` → `thermostaticValve1`, `thermostaticValve2`
- `SCUD` → `accessControlSystem` (SCUD = Система Контроля и Управления Доступом)

#### Счетчики:
- `coldWaterMeters1` → `coldWaterMeter`
- `hotWaterMeters` → `hotWaterMeter`
- `electrMeters` → `electricMeter`
- `gazMeters` → `gasMeter`

#### Имитаторы:
- `dataColdImitator` → `coldWaterSimulator`
- `dataHotImitator` → `hotWaterSimulator`
- `dataElectrImitator` → `electricMeterSimulator`
- `dataGazImitator` → `gasMeterSimulator`
- `imDev*` → `simulatorDevice*`
- `imLog*` → `simulatorLog*`

#### Системы:
- `engineering_systems` → `engineeringSystem`
- `ventilationSystem` → `ventilationSystem` ✅
- `fireAlarm` → `fireAlarmSystem`

#### Интеграции:
- `bff_Lims` → `limsBackend`
- `bffLimsN` → `limsBackendNew`
- `worker_Lims` → `limsWorker`
- `restAPI` → `restApi`
- `restArena` → `restArenaApi`

#### Логирование:
- Все логи → `log*` (например, `logAccess`, `logEnergy`)

## Новая структура моделей

### Группировка по категориям:

```
models/
  core/                    # Основные модели
    objects
    meters
    devices
    personnel
  
  engineering/             # Инженерные системы
    pumps/
      pump
      pumpStation
      pumpUnit
    ventilation/
      ventilation
      ventilationSystem
      vent
    compressors/
      compressorUnit
      compressUnit
      frozenUnit
    thermal/
      thermalPoint
      termal
      termo
      thermostatValve1
      thermostatValve2
  
  safety/                  # Системы безопасности
    fireAlarmSystem
    fire
    smoke
    siren
    accessControlSystem
  
  monitoring/              # Мониторинг
    sensors/
      sensor
      temperatureSensor
      pressureGauge
      pressureSwitch
  
  it/                      # IT инфраструктура
    itInfrastructure
    servers
    networkDevice
    itServices
  
  visualization/          # Визуализация
    graphics
    graphApplication
    graphEngineer
    graphItInfrastructure
  
  integration/            # Интеграции
    lims/
      limsBackend
      limsWorker
    rest/
      restApi
      restArenaApi
    telegram/
      telegramConnector
    stomp/
      stomp
  
  simulation/             # Имитаторы
    devices/
      simulatorDeviceAccess
      simulatorDeviceElectricMeter
      simulatorDeviceFire
      simulatorDevicePump
      simulatorDeviceVent
      simulatorDeviceWaterMeter
      simulatorDeviceWarmMeter
    logs/
      logAccess
      logEnergy
      logFireControl
      logIt
      logLifts
      logPumpControl
      logRefrigerators
      logSewerage
      logTermal
      logVent
      logVideo
    meters/
      coldWaterSimulator
      hotWaterSimulator
      electricMeterSimulator
      gasMeterSimulator
  
  utilities/              # Вспомогательные
    geocoding
    fileManager
    barCodeGenerator
    barCodePresenter
    frontendConfig
    system
```

## План миграции

1. Создать улучшенную структуру на целевом сервере
2. Перенести и переименовать модели
3. Обновить все ссылки
4. Улучшить описания и документацию
5. Применить лучшие практики

