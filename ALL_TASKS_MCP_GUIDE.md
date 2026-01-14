# Руководство по выполнению всех задач через MCP инструменты

**Дата:** 2025-12-15  
**Версия:** 1.0

## Подготовка

Перед выполнением задач необходимо подключиться к серверу:

```python
# 1. Подключение к серверу
connect_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_connect",
    arguments={
        "host": "localhost",
        "port": 6460,
        "username": "admin",
        "password": "admin"
    }
)

# 2. Вход в систему
login_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_login",
    arguments={}
)
```

---

## Задача 1: Создать двух пользователей и дать первому доступ ко всем устройствам другого

### Шаг 1: Создание первого пользователя

```python
user1_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_user",
    arguments={
        "username": "user1",
        "password": "password1",
        "email": "user1@example.com"
    }
)
```

### Шаг 2: Создание второго пользователя

```python
user2_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_user",
    arguments={
        "username": "user2",
        "password": "password2",
        "email": "user2@example.com"
    }
)
```

### Шаг 3: Создание виртуального устройства для user2

```python
device_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_device",
    arguments={
        "username": "user2",
        "deviceName": "virtualDevice1",
        "description": "Виртуальное устройство user2",
        "driverId": "com.tibbo.linkserver.plugin.device.virtual"
    }
)
```

### Шаг 4: Настройка доступа

**⚠️ ВАЖНО:** Настройка доступа к устройствам другого пользователя требует использования Actions через веб-интерфейс AggreGate или через более сложные операции, которые не доступны через базовые MCP инструменты.

**Рекомендация:** Используйте веб-интерфейс AggreGate:
1. Войдите как admin
2. Перейдите в контекст `users.user1`
3. Используйте действие для настройки доступа к устройствам `users.user2.devices.*`

---

## Задача 2: Тревога Event 1 -> Event 2 (Int > 20)

### Шаг 1: Создание модели для тревоги

```python
model_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_context",
    arguments={
        "parentPath": "users.admin.models",
        "name": "alarm_event_model",
        "description": "Модель тревоги Event 1 -> Event 2"
    }
)
```

### Шаг 2: Создание переменных для отслеживания состояния

```python
# Переменная для статуса тревоги
status_var = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_variable",
    arguments={
        "path": "users.admin.models.alarm_event_model",
        "variableName": "alarmStatus",
        "format": "<status><S>",
        "description": "Статус тревоги",
        "writable": True
    }
)

# Переменная для данных Event 2
event2_data = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_variable",
    arguments={
        "path": "users.admin.models.alarm_event_model",
        "variableName": "event2Data",
        "format": "<Int><I>",
        "description": "Данные Event 2",
        "writable": True
    }
)
```

### Шаг 3: Создание событий

```python
# Событие активации
activate_event = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_event",
    arguments={
        "path": "users.admin.models.alarm_event_model",
        "eventName": "alarmActivated",
        "description": "Тревога активирована"
    }
)

# Событие деактивации
deactivate_event = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_event",
    arguments={
        "path": "users.admin.models.alarm_event_model",
        "eventName": "alarmDeactivated",
        "format": "<Int><I>",
        "description": "Тревога деактивирована"
    }
)
```

### Шаг 4: Создание функции для проверки условия

```python
check_function = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_function",
    arguments={
        "path": "users.admin.models.alarm_event_model",
        "functionName": "checkDeactivation",
        "functionType": 1,  # Expression
        "inputFormat": "<Int><I>",
        "outputFormat": "<result><B>",
        "expression": "table(\"<<result><B>>\", {Int} > 20)",
        "description": "Проверка условия деактивации (Int > 20)"
    }
)
```

**⚠️ ВАЖНО:** Для полной настройки тревоги требуется:
1. Настройка привязок (bindings) для связи событий устройства с переменными модели
2. Настройка логики активации/деактивации через Actions или Rules
3. Это требует дополнительной настройки через веб-интерфейс

---

## Задача 3: Тревога через 10 секунд (сумма Int + Float в диапазоне 50-100)

### Шаг 1: Создание модели

```python
model_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_context",
    arguments={
        "parentPath": "users.admin.models",
        "name": "alarm_delayed_model",
        "description": "Модель тревоги с задержкой"
    }
)
```

### Шаг 2: Создание переменных

```python
# Переменные для значений
int_var = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_variable",
    arguments={
        "path": "users.admin.models.alarm_delayed_model",
        "variableName": "intValue",
        "format": "<value><I>",
        "writable": True
    }
)

float_var = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_variable",
    arguments={
        "path": "users.admin.models.alarm_delayed_model",
        "variableName": "floatValue",
        "format": "<value><E>",
        "writable": True
    }
)

# Переменная для суммы
sum_var = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_variable",
    arguments={
        "path": "users.admin.models.alarm_delayed_model",
        "variableName": "sum",
        "format": "<value><E>",
        "writable": True
    }
)
```

### Шаг 3: Создание функции для проверки диапазона

```python
range_check = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_function",
    arguments={
        "path": "users.admin.models.alarm_delayed_model",
        "functionName": "checkRange",
        "functionType": 1,  # Expression
        "inputFormat": "<sum><E>",
        "outputFormat": "<inRange><B>",
        "expression": "table(\"<<inRange><B>>\", {sum} >= 50 && {sum} <= 100)",
        "description": "Проверка диапазона 50-100"
    }
)
```

**⚠️ ВАЖНО:** Задержка в 10 секунд требует использования Java функции или настройки через Actions/Rules в веб-интерфейсе.

---

## Задача 4: Тревога на сумму в таблице > 100 с корректирующим действием

### Шаг 1: Создание модели

```python
model_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_context",
    arguments={
        "parentPath": "users.admin.models",
        "name": "alarm_table_model",
        "description": "Модель тревоги на таблицу"
    }
)
```

### Шаг 2: Создание табличной переменной

```python
table_var = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_variable",
    arguments={
        "path": "users.admin.models.alarm_table_model",
        "variableName": "dataTable",
        "format": "<Int><I><String><S>",
        "description": "Табличная переменная",
        "writable": True
    }
)
```

### Шаг 3: Создание функции для вычисления суммы

```python
sum_function = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_function",
    arguments={
        "path": "users.admin.models.alarm_table_model",
        "functionName": "calculateSum",
        "functionType": 1,  # Expression
        "inputFormat": "<table><T>",
        "outputFormat": "<sum><I>",
        "expression": "table(\"<<sum><I>>\", sum({table}, \"Int\"))",
        "description": "Вычисление суммы колонки Int"
    }
)
```

**⚠️ ВАЖНО:** Корректирующее действие (показ отчета оператору) требует создания Action через веб-интерфейс AggreGate.

---

## Задача 5: Фильтр событий

**⚠️ ВАЖНО:** Фильтры событий настраиваются через виджеты или Actions в веб-интерфейсе AggreGate. MCP инструменты могут создать события, но не могут напрямую настроить фильтры.

### Создание событий для фильтрации:

```python
# Создание события Event 2 в устройстве
event2 = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_event",
    arguments={
        "path": "users.user2.devices.virtualDevice1",
        "eventName": "Event2",
        "format": "<Integer><I><String><S>",
        "description": "Событие для фильтрации"
    }
)
```

**Условия фильтра:**
- `Integer > 10`
- `String содержит "abc"`

**Настройка:** Через виджет Event Log в веб-интерфейсе с настройкой фильтров.

---

## Задача 6: Форма в виджете (Grid Layout)

### Шаг 1: Создание виджета

```python
widget_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_widget",
    arguments={
        "parentPath": "users.admin.widgets",
        "name": "formWidget",
        "description": "Виджет с формой Grid Layout"
    }
)
```

### Шаг 2: Установка шаблона с Grid Layout

```python
template = '''<?xml version="1.0" encoding="UTF-8"?>
<widget>
    <layout type="grid" columns="2">
        <label text="Имя:" gridx="0" gridy="0"/>
        <textfield name="name" gridx="1" gridy="0"/>
        
        <label text="Email:" gridx="0" gridy="1"/>
        <textfield name="email" gridx="1" gridy="1"/>
        
        <label text="Телефон:" gridx="0" gridy="2"/>
        <textfield name="phone" gridx="1" gridy="2"/>
        
        <button name="submit" text="Отправить" gridx="0" gridy="3" gridwidth="2"/>
    </layout>
</widget>'''

template_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_set_widget_template",
    arguments={
        "path": "users.admin.widgets.formWidget",
        "template": template
    }
)
```

---

## Задача 7: Функция калькулятора в модели

### Шаг 1: Создание модели

```python
model_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_context",
    arguments={
        "parentPath": "users.admin.models",
        "name": "calculator_model",
        "description": "Модель калькулятора"
    }
)
```

### Шаг 2: Создание функции калькулятора

```python
calc_function = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_function",
    arguments={
        "path": "users.admin.models.calculator_model",
        "functionName": "calculator",
        "functionType": 1,  # Expression
        "inputFormat": "<num1><E><num2><E>",
        "outputFormat": "<result><E>",
        "expression": "table(\"<<result><E>>\", callFunction(\"calculate\", {num1}, {num2}))",
        "description": "Калькулятор, вызывающий calculate() устройства"
    }
)
```

**⚠️ ПРИМЕЧАНИЕ:** Синтаксис вызова функции устройства может отличаться в зависимости от версии AggreGate.

---

## Задача 8: Относительная модель (сумма Sine Wave + Sawtooth Wave)

### Шаг 1: Создание относительной модели

```python
model_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_context",
    arguments={
        "parentPath": "users.admin.models",
        "name": "relative_waves_model",
        "description": "Относительная модель для суммы волн"
    }
)
```

### Шаг 2: Создание переменной для суммы

```python
sum_var = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_variable",
    arguments={
        "path": "users.admin.models.relative_waves_model",
        "variableName": "wavesSum",
        "format": "<sum><E>",
        "description": "Сумма Sine Wave и Sawtooth Wave",
        "writable": True
    }
)
```

### Шаг 3: Создание функции для вычисления суммы

```python
sum_function = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_function",
    arguments={
        "path": "users.admin.models.relative_waves_model",
        "functionName": "calculateWavesSum",
        "functionType": 1,  # Expression
        "inputFormat": "<sineWave><E><sawtoothWave><E>",
        "outputFormat": "<sum><E>",
        "expression": "table(\"<<sum><E>>\", {sineWave} + {sawtoothWave})",
        "description": "Вычисление суммы волн"
    }
)
```

**⚠️ ВАЖНО:** Для работы относительной модели требуется настройка привязок (bindings) для каждого устройства через веб-интерфейс.

---

## Задача 9: Калькулятор в виде виджета (Grid Layout)

### Шаг 1: Создание виджета

```python
widget_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_widget",
    arguments={
        "parentPath": "users.admin.widgets",
        "name": "calculatorWidget",
        "description": "Калькулятор в виде виджета"
    }
)
```

### Шаг 2: Установка шаблона калькулятора

```python
calc_template = '''<?xml version="1.0" encoding="UTF-8"?>
<widget>
    <layout type="grid" columns="4">
        <textfield name="display" gridx="0" gridy="0" gridwidth="4" readonly="true"/>
        
        <button name="btn7" text="7" gridx="0" gridy="1"/>
        <button name="btn8" text="8" gridx="1" gridy="1"/>
        <button name="btn9" text="9" gridx="2" gridy="1"/>
        <button name="btnDiv" text="/" gridx="3" gridy="1"/>
        
        <button name="btn4" text="4" gridx="0" gridy="2"/>
        <button name="btn5" text="5" gridx="1" gridy="2"/>
        <button name="btn6" text="6" gridx="2" gridy="2"/>
        <button name="btnMul" text="*" gridx="3" gridy="2"/>
        
        <button name="btn1" text="1" gridx="0" gridy="3"/>
        <button name="btn2" text="2" gridx="1" gridy="3"/>
        <button name="btn3" text="3" gridx="2" gridy="3"/>
        <button name="btnSub" text="-" gridx="3" gridy="3"/>
        
        <button name="btn0" text="0" gridx="0" gridy="4" gridwidth="2"/>
        <button name="btnDot" text="." gridx="2" gridy="4"/>
        <button name="btnAdd" text="+" gridx="3" gridy="4"/>
        
        <button name="btnEquals" text="=" gridx="0" gridy="5" gridwidth="4"/>
    </layout>
</widget>'''

template_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_set_widget_template",
    arguments={
        "path": "users.admin.widgets.calculatorWidget",
        "template": calc_template
    }
)
```

---

## Задача 10: Запрос для отображения и изменения переменных устройства

**⚠️ ВАЖНО:** Запросы (queries) создаются через веб-интерфейс AggreGate или через более сложные операции, которые не доступны через базовые MCP инструменты.

**Рекомендация:** Используйте веб-интерфейс:
1. Перейдите в контекст `queries`
2. Создайте новый запрос
3. Настройте отображение переменных устройства
4. Настройте возможность редактирования

---

## Задача 11: Относительный отчет по табличной переменной

**⚠️ ВАЖНО:** Отчеты создаются через веб-интерфейс AggreGate.

**Рекомендация:** Используйте веб-интерфейс:
1. Перейдите в контекст `reports`
2. Создайте новый относительный отчет
3. Настройте отображение табличной переменной всех устройств

---

## Задача 12: Посекундный график изменения переменных

**⚠️ ВАЖНО:** Графики создаются через виджеты в веб-интерфейсе AggreGate.

**Рекомендация:** Используйте виджет Chart:
1. Создайте виджет с типом Chart
2. Настройте отображение переменных Sine Wave и Sawtooth Wave
3. Настройте интервал обновления (1 секунда)

---

## Задача 13: Pie Chart по табличной переменной

**⚠️ ВАЖНО:** Pie Chart создается через виджеты в веб-интерфейсе.

**Рекомендация:** Используйте виджет Chart с типом Pie:
1. Создайте виджет Chart
2. Настройте тип графика: Pie
3. Привяжите к табличной переменной устройства

---

## Задача 14: Виджет с формой генерации событий и логом

### Шаг 1: Создание виджета

```python
widget_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_widget",
    arguments={
        "parentPath": "users.admin.widgets",
        "name": "eventGeneratorWidget",
        "description": "Виджет генерации событий с логом"
    }
)
```

### Шаг 2: Установка шаблона

```python
event_template = '''<?xml version="1.0" encoding="UTF-8"?>
<widget>
    <layout type="border">
        <!-- Верхняя часть: форма генерации событий -->
        <panel name="formPanel" position="north">
            <layout type="grid" columns="2">
                <label text="Тип события:" gridx="0" gridy="0"/>
                <combobox name="eventType" gridx="1" gridy="0">
                    <item value="Event1"/>
                    <item value="Event2"/>
                </combobox>
                
                <label text="Данные:" gridx="0" gridy="1"/>
                <textfield name="eventData" gridx="1" gridy="1"/>
                
                <button name="fireButton" text="Генерировать событие" gridx="0" gridy="2" gridwidth="2"/>
            </layout>
        </panel>
        
        <!-- Нижняя часть: лог событий -->
        <eventlog name="eventLog" position="center" showAllTypes="true"/>
    </layout>
</widget>'''

template_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_set_widget_template",
    arguments={
        "path": "users.admin.widgets.eventGeneratorWidget",
        "template": event_template
    }
)
```

---

## Задача 15: Виджет с кнопкой, открывающей другой виджет

### Шаг 1: Создание виджета-кнопки

```python
button_widget = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_widget",
    arguments={
        "parentPath": "users.admin.widgets",
        "name": "buttonWidget",
        "description": "Виджет с кнопкой"
    }
)
```

### Шаг 2: Установка шаблона кнопки

```python
button_template = '''<?xml version="1.0" encoding="UTF-8"?>
<widget>
    <layout type="grid" columns="1">
        <button name="openButton" text="Открыть виджет" gridx="0" gridy="0"
                action="launchWidget" targetWidget="users.admin.widgets.targetWidget"/>
    </layout>
</widget>'''

call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_set_widget_template",
    arguments={
        "path": "users.admin.widgets.buttonWidget",
        "template": button_template
    }
)
```

---

## Задача 16: Виджет с историей переменной и средним значением

### Шаг 1: Создание виджета

```python
history_widget = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_widget",
    arguments={
        "parentPath": "users.admin.widgets",
        "name": "historyWidget",
        "description": "Виджет истории переменной"
    }
)
```

### Шаг 2: Установка шаблона

```python
history_template = '''<?xml version="1.0" encoding="UTF-8"?>
<widget>
    <layout type="border">
        <!-- Таблица истории -->
        <table name="historyTable" position="center"
               variable="users.user2.devices.virtualDevice1.sine"
               timeRange="5m"
               showTime="true"/>
        
        <!-- Среднее значение -->
        <label name="averageLabel" position="south"
               text="Среднее значение: {avg(users.user2.devices.virtualDevice1.sine, 5m)}"/>
    </layout>
</widget>'''

call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_set_widget_template",
    arguments={
        "path": "users.admin.widgets.historyWidget",
        "template": history_template
    }
)
```

---

## Задача 17: Виджеты кнопка и вентилятор (SVG)

### Шаг 1: Создание виджета кнопки

```python
svg_button = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_widget",
    arguments={
        "parentPath": "users.admin.widgets",
        "name": "svgButton",
        "description": "Кнопка SVG"
    }
)
```

### Шаг 2: Шаблон кнопки SVG

```python
button_svg_template = '''<?xml version="1.0" encoding="UTF-8"?>
<widget>
    <layout type="grid" columns="1">
        <svg name="buttonSvg" gridx="0" gridy="0" width="100" height="100">
            <rect x="10" y="10" width="80" height="80" fill="#4CAF50" stroke="#2E7D32" stroke-width="2"/>
            <text x="50" y="55" text-anchor="middle" fill="white" font-size="20">ON</text>
        </svg>
    </layout>
</widget>'''

call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_set_widget_template",
    arguments={
        "path": "users.admin.widgets.svgButton",
        "template": button_svg_template
    }
)
```

### Шаг 3: Создание виджета вентилятора

```python
svg_fan = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_widget",
    arguments={
        "parentPath": "users.admin.widgets",
        "name": "svgFan",
        "description": "Вентилятор SVG"
    }
)
```

### Шаг 4: Шаблон вентилятора SVG

```python
fan_svg_template = '''<?xml version="1.0" encoding="UTF-8"?>
<widget>
    <layout type="grid" columns="1">
        <svg name="fanSvg" gridx="0" gridy="0" width="150" height="150">
            <circle cx="75" cy="75" r="70" fill="#E0E0E0" stroke="#9E9E9E" stroke-width="2"/>
            <!-- Лопасти вентилятора -->
            <path d="M 75 75 L 75 15 L 85 15 L 85 75 Z" fill="#2196F3" transform="rotate(0 75 75)"/>
            <path d="M 75 75 L 75 15 L 85 15 L 85 75 Z" fill="#2196F3" transform="rotate(120 75 75)"/>
            <path d="M 75 75 L 75 15 L 85 15 L 85 75 Z" fill="#2196F3" transform="rotate(240 75 75)"/>
            <circle cx="75" cy="75" r="10" fill="#1976D2"/>
        </svg>
    </layout>
</widget>'''

call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_set_widget_template",
    arguments={
        "path": "users.admin.widgets.svgFan",
        "template": fan_svg_template
    }
)
```

### Шаг 5: Создание виджета-контейнера

```python
container_widget = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_widget",
    arguments={
        "parentPath": "users.admin.widgets",
        "name": "fanControlWidget",
        "description": "Виджет управления вентилятором"
    }
)
```

### Шаг 6: Шаблон контейнера с сабвиджетами

```python
container_template = '''<?xml version="1.0" encoding="UTF-8"?>
<widget>
    <layout type="grid" columns="2">
        <subwidget name="buttonSub" widget="users.admin.widgets.svgButton" gridx="0" gridy="0"/>
        <subwidget name="fanSub" widget="users.admin.widgets.svgFan" gridx="1" gridy="0"/>
    </layout>
</widget>'''

call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_set_widget_template",
    arguments={
        "path": "users.admin.widgets.fanControlWidget",
        "template": container_template
    }
)
```

**⚠️ ПРИМЕЧАНИЕ:** Для запуска вентилятора при нажатии кнопки требуется настройка действий через веб-интерфейс.

---

## Задача 18: Дашборд для виртуальных устройств

### Шаг 1: Создание дашборда

```python
dashboard_result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_dashboard",
    arguments={
        "parentPath": "users.admin.dashboards",
        "name": "virtualDevicesDashboard",
        "description": "Дашборд для виртуальных устройств",
        "layout": "dockable"
    }
)
```

### Шаг 2: Добавление элемента - график переменной Sine

```python
graph_element = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_add_dashboard_element",
    arguments={
        "path": "users.admin.dashboards.virtualDevicesDashboard",
        "name": "sineGraph",
        "type": "launchWidget",
        "parameters": {
            "format": "<widget><S>",
            "records": [{"widget": "users.admin.widgets.sineChartWidget"}]
        }
    }
)
```

### Шаг 3: Добавление элемента - свойства устройства

```python
properties_element = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_add_dashboard_element",
    arguments={
        "path": "users.admin.dashboards.virtualDevicesDashboard",
        "name": "deviceProperties",
        "type": "showContextProperties",
        "parameters": {
            "format": "<context><S>",
            "records": [{"context": "users.user2.devices.virtualDevice1"}]
        }
    }
)
```

### Шаг 4: Добавление элемента - лог событий

```python
eventlog_element = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_add_dashboard_element",
    arguments={
        "path": "users.admin.dashboards.virtualDevicesDashboard",
        "name": "deviceEventLog",
        "type": "showEventLog",
        "parameters": {
            "format": "<context><S>",
            "records": [{"context": "users.user2.devices.virtualDevice1"}]
        }
    }
)
```

### Шаг 5: Добавление элемента - отчет по таблице

```python
report_element = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_add_dashboard_element",
    arguments={
        "path": "users.admin.dashboards.virtualDevicesDashboard",
        "name": "tableReport",
        "type": "launchWidget",
        "parameters": {
            "format": "<widget><S>",
            "records": [{"widget": "users.admin.widgets.tableReportWidget"}]
        }
    }
)
```

---

## Итоговые рекомендации

### ✅ Что можно сделать через MCP:

1. Создание пользователей и устройств
2. Создание моделей, переменных, функций, событий
3. Создание виджетов с шаблонами
4. Создание дашбордов и добавление элементов

### ⚠️ Что требует дополнительной настройки через веб-интерфейс:

1. Настройка доступа к устройствам других пользователей
2. Настройка тревог (alarms) с полной логикой
3. Настройка фильтров событий
4. Настройка корректирующих действий
5. Создание запросов (queries)
6. Создание отчетов (reports)
7. Настройка действий (actions) для виджетов
8. Настройка привязок (bindings) для относительных моделей

---

**Дата создания:** 2025-12-15  
**Версия:** 1.0

