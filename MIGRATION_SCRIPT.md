# Скрипт миграции проекта AggreGate

## Статус миграции

Из-за большого объема проекта (100+ моделей, 200+ дашбордов), полная автоматическая миграция требует значительного времени. 

## Подход к миграции

### Этап 1: Основные модели (Выполняется)

1. ✅ `objects` - Управление объектами
2. ⏳ `meters` - Счетчики
3. ⏳ `devices` - Устройства
4. ⏳ `personnel` - Персонал

### Этап 2: Инженерные системы

- `pump` → `pump`
- `pumpStation` → `pumpStation`
- `pumpUnit` → `pumpUnit`
- `ventilation` → `ventilation`
- `ventilationSystem` → `ventilationSystem`
- `compressorUnit` → `compressorUnit`
- `fireAlarm` → `fireAlarmSystem`
- `SCUD` → `accessControlSystem`

### Этап 3: Счетчики

- `coldWaterMeters1` → `coldWaterMeter`
- `hotWaterMeters` → `hotWaterMeter`
- `electrMeters` → `electricMeter`
- `gazMeters` → `gasMeter`

### Этап 4: Имитаторы

- `dataColdImitator` → `coldWaterSimulator`
- `dataHotImitator` → `hotWaterSimulator`
- `dataElectrImitator` → `electricMeterSimulator`
- `dataGazImitator` → `gasMeterSimulator`
- `imDev*` → `simulatorDevice*`
- `imLog*` → `simulatorLog*`

### Этап 5: Интеграции

- `bff_Lims` → `limsBackend`
- `bffLimsN` → `limsBackendNew`
- `worker_Lims` → `limsWorker`
- `restAPI` → `restApi`
- `restArena` → `restArenaApi`

### Этап 6: Устройства

- Все устройства с улучшенными именами

### Этап 7: Виджеты и дашборды

- Реорганизация с применением стандартов именования

## Улучшения, применяемые при миграции

1. **Именование**: camelCase для всех ресурсов
2. **Описания**: Понятные описания для всех объектов
3. **Группировка**: Группировка переменных по функциональным областям
4. **Документация**: Добавление help-текстов
5. **Права доступа**: Оптимизация прав доступа
6. **Производительность**: Оптимизация привязок и периодов обновления

