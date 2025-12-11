# Инструкции по настройке системы кластера виртуальных устройств

## Что было создано

### 1. Устройства
- ✅ `users.admin.devices.device1` - Виртуальное устройство 1
- ✅ `users.admin.devices.device2` - Виртуальное устройство 2  
- ✅ `users.admin.devices.device3` - Виртуальное устройство 3

### 2. Модель кластера
- ✅ `users.admin.models.cluster` - Модель контекста кластера

**Переменные модели:**
- `device1Sine` - Значение переменной Sine Wave устройства 1
- `device2Sine` - Значение переменной Sine Wave устройства 2
- `device3Sine` - Значение переменной Sine Wave устройства 3
- `clusterStatus` - Состояние кластера: "ОК" или "ошибка"

**События модели:**
- `clusterAlarm` - Тревога кластера при переходе в состояние ошибки

**Привязки:**
- Привязка `.:device1Sine` ← `{users.admin.devices.device1:sine}`
- Привязка `.:device2Sine` ← `{users.admin.devices.device2:sine}`
- Привязка `.:device3Sine` ← `{users.admin.devices.device3:sine}`

**Примечание:** Формат ссылок в AggreGate:
- `.:имя_переменной` - ссылка на переменную текущего контекста
- `{контекст:переменная}` - ссылка на переменную другого контекста
- Подробнее см. [Формат ссылок в AggreGate](docs/AGGREGATE_REFERENCES_FORMAT.md)

**Правила:**
- Правило `checkClusterStatus` - автоматически проверяет состояние кластера и устанавливает `clusterStatus` в "ошибка", если все три значения sine > 0, иначе "ОК"

### 3. Виджеты
- ✅ `users.admin.widgets.clusterStatusWidget` - Виджет состояния кластера
- ✅ `users.admin.widgets.deviceMetricsWidget` - Виджет графиков метрик устройства

### 4. Дашборд
- ✅ `users.admin.dashboards.clusterDashboard` - Инструментальная панель состояния кластера

## Требуется настройка через веб-интерфейс

### 1. Настройка шаблона виджета состояния кластера

Откройте виджет `users.admin.widgets.clusterStatusWidget` в веб-интерфейсе AggreGate и настройте его шаблон:

```xml
<panel>
  <border-layout/>
  <panel>
    <title>Состояние кластера</title>
    <flow-layout/>
    <label>
      <text>Состояние кластера: ${clusterStatus}</text>
      <foreground>${clusterStatus == "ошибка" ? "red" : "green"}</foreground>
    </label>
    <table>
      <model-context path="users.admin.models.cluster"/>
      <column name="device1Sine" title="Устройство 1 (Sine)"/>
      <column name="device2Sine" title="Устройство 2 (Sine)"/>
      <column name="device3Sine" title="Устройство 3 (Sine)"/>
      <column name="clusterStatus" title="Состояние"/>
    </table>
    <panel>
      <title>Устройства кластера</title>
      <flow-layout/>
      <button onclick="openDeviceWidget('device1')" text="Устройство 1"/>
      <button onclick="openDeviceWidget('device2')" text="Устройство 2"/>
      <button onclick="openDeviceWidget('device3')" text="Устройство 3"/>
    </panel>
  </panel>
</panel>
```

Добавьте JavaScript функцию для открытия виджета метрик устройства:

```javascript
function openDeviceWidget(deviceName) {
  var devicePath = "users.admin.devices." + deviceName;
  var widgetPath = "users.admin.widgets.deviceMetricsWidget";
  launchWidget(widgetPath, devicePath);
}
```

**Важно:** Установите default context для виджета `clusterStatusWidget`:
- Перейдите в свойства виджета
- Установите Default Context: `users.admin.models.cluster`

### 2. Настройка шаблона виджета метрик устройства

Откройте виджет `users.admin.widgets.deviceMetricsWidget` и настройте его шаблон:

```xml
<panel>
  <border-layout/>
  <panel>
    <title>Метрики устройства: ${context.name}</title>
    <flow-layout/>
    <chart>
      <title>Sine Wave</title>
      <data-source>
        <context>${context}</context>
        <variable>sine</variable>
      </data-source>
      <history>true</history>
    </chart>
    <chart>
      <title>Sawtooth Wave</title>
      <data-source>
        <context>${context}</context>
        <variable>sawtooth</variable>
      </data-source>
      <history>true</history>
    </chart>
    <chart>
      <title>Triangle Wave</title>
      <data-source>
        <context>${context}</context>
        <variable>triangle</variable>
      </data-source>
      <history>true</history>
    </chart>
  </panel>
</panel>
```

### 3. Настройка автозапуска дашборда при логине

Для автоматического открытия дашборда при логине оператора:

1. Откройте настройки пользователя `admin` (или нужного оператора)
2. Перейдите в раздел "Dashboards" или "Preferences"
3. Найдите настройку "Default Dashboard" или "Startup Dashboard"
4. Установите значение: `users.admin.dashboards.clusterDashboard`

Альтернативный способ (если доступно):
1. Откройте дашборд `users.admin.dashboards.clusterDashboard`
2. В свойствах дашборда найдите опцию "Auto-open on login"
3. Включите эту опцию

### 4. Настройка правила для автоматической проверки состояния

Правило уже создано, но убедитесь, что оно активно:

1. Откройте модель `users.admin.models.cluster`
2. Перейдите в раздел "Rule Sets"
3. Найдите правило `checkClusterStatus`
4. Убедитесь, что оно включено (enabled)
5. Настройте триггер: правило должно срабатывать при изменении переменных `device1Sine`, `device2Sine`, `device3Sine`

Если правило не работает автоматически, можно создать функцию типа Expression, которая будет периодически вызываться:

1. В модели `users.admin.models.cluster` создайте функцию типа Expression
2. Название: `checkClusterStatus`
3. Тип: Expression
4. Выражение:
```javascript
if (device1Sine != null && device2Sine != null && device3Sine != null) {
  var oldStatus = clusterStatus;
  if (device1Sine > 0 && device2Sine > 0 && device3Sine > 0) {
    clusterStatus = "ошибка";
    if (oldStatus != "ошибка") {
      fireEvent("clusterAlarm", "Кластер перешел в состояние ошибки: все устройства имеют sine > 0");
    }
  } else {
    clusterStatus = "ОК";
  }
}
```

5. Настройте периодический вызов этой функции (например, каждые 1-5 секунд)

## Проверка работы системы

1. Убедитесь, что все три устройства подключены и работают
2. Проверьте, что переменные `device1Sine`, `device2Sine`, `device3Sine` обновляются в модели кластера
3. Проверьте работу правила или функции проверки состояния
4. Убедитесь, что при одновременном значении всех sine > 0:
   - `clusterStatus` меняется на "ошибка"
   - Генерируется событие `clusterAlarm`
5. Проверьте работу виджетов и дашборда

## Логика работы

- **Состояние "ОК"**: Когда хотя бы одно из значений sine <= 0
- **Состояние "ошибка"**: Когда все три значения sine > 0 одновременно
- **Тревога**: Генерируется событие `clusterAlarm` при переходе кластера в состояние "ошибка"

## Дополнительные настройки

При необходимости можно добавить:
- Дополнительные визуальные индикаторы состояния устройств
- История состояний кластера
- Уведомления (email, SMS) при тревоге
- Расширенную аналитику и отчеты

