# Формат ссылок на контексты, переменные, события и функции в AggreGate

## Введение

В AggreGate используется специальный синтаксис для ссылок на элементы системы (контексты, переменные, события, функции). Правильное использование этого синтаксиса критически важно для создания привязок (bindings), выражений и других конструкций.

## Основные принципы

### 1. Ссылка на текущий контекст

Символ `.` (точка) представляет текущий контекст.

**Формат:** `.:имя_элемента`

**Примеры:**
- `.:status` - переменная `status` в текущем контексте
- `.:device1Sine` - переменная `device1Sine` в текущем контексте
- `.:fireEvent()` - функция `fireEvent` в текущем контексте

### 2. Ссылка на элементы другого контекста

Для ссылки на элементы другого контекста используется синтаксис с фигурными скобками `{}` и двоеточием `:`.

**Формат:** `{путь_к_контексту:имя_элемента}`

**Примеры:**
- `{users.admin.devices.device1:sine}` - переменная `sine` в контексте `users.admin.devices.device1`
- `{users.admin.models.cluster:clusterStatus}` - переменная `clusterStatus` в контексте `users.admin.models.cluster`
- `{users.admin.devices.device2:getValue()}` - функция `getValue` в контексте `users.admin.devices.device2`

### 3. Полный путь к контексту

Полный путь к контексту указывается через точку (`.`).

**Формат:** `users.username.contexts.context`

**Примеры:**
- `users.admin.devices.device1` - контекст устройства device1
- `users.admin.models.cluster` - контекст модели cluster
- `users.admin.widgets.clusterStatusWidget` - контекст виджета

## Применение в привязках (Bindings)

### Структура привязки

Привязка состоит из двух частей:
1. **target** (цель) - куда записывается значение
2. **expression** (выражение) - откуда берется значение или вычисляемое выражение

### Формат привязок переменных

**Целевая переменная (target):**
- Используйте `.:имя_переменной` для переменной в текущем контексте

**Исходное значение (expression):**
- Используйте `{контекст:переменная}` для ссылки на переменную другого контекста
- Можно использовать выражения на основе ссылок

**Примеры правильных привязок:**

```
target: ".:device1Sine"
expression: "{users.admin.devices.device1:sine}"
```

```
target: ".:device2Sine"
expression: "{users.admin.devices.device2:sine}"
```

```
target: ".:device3Sine"
expression: "{users.admin.devices.device3:sine}"
```

### Пример создания привязки через переменную bindings

```json
{
  "recordCount": 1,
  "format": {...},
  "records": [{
    "target": ".:device1Sine",
    "expression": "{users.admin.devices.device1:sine}",
    "onevent": true
  }]
}
```

## Типы элементов, на которые можно ссылаться

### 1. Переменные (Variables)

**Формат:** `{контекст:имя_переменной}` или `.:имя_переменной`

**Примеры:**
- `{users.admin.devices.device1:sine}` - переменная sine устройства device1
- `.:clusterStatus` - переменная clusterStatus текущего контекста
- `{users.admin.models.cluster:device1Sine}` - переменная device1Sine модели cluster

### 2. События (Events)

**Формат:** `{контекст:имя_события}` или `.:имя_события`

**Примеры:**
- `{users.admin.models.cluster:clusterAlarm@}` - событие clusterAlarm модели cluster
- `.:statusChanged@` - событие statusChanged текущего контекста

### 3. Функции (Functions)

**Формат:** `{контекст:имя_функции()}` или `.:имя_функции()`

**Примеры:**
- `{users.admin.models.cluster:checkClusterStatus()}` - функция checkClusterStatus
- `.:fireEvent()` - функция fireEvent текущего контекста

### 4. Контексты (Contexts)

**Формат:** `путь.к.контексту`

**Примеры:**
- `users.admin.devices.device1` - полный путь к контексту устройства
- `users.admin.models.cluster` - полный путь к модели кластера

## Выражения с ссылками

В выражениях можно комбинировать ссылки и операции:

**Примеры:**

```javascript
// Проверка значения переменной другого контекста
{users.admin.devices.device1:sine} > 0

// Комбинация значений из разных контекстов
{users.admin.devices.device1:sine} + {users.admin.devices.device2:sine}

// Условное выражение
{users.admin.devices.device1:sine} > 0 ? "ошибка" : "ОК"

// Вызов функции другого контекста
{users.admin.models.cluster:checkClusterStatus}()

// Ссылка на переменную текущего контекста в выражении
.:device1Sine > 0 && .:device2Sine > 0
```

## Относительные и абсолютные пути

### Абсолютные пути

Абсолютный путь начинается с корня системы (обычно `users`).

**Примеры:**
- `users.admin.devices.device1`
- `users.admin.models.cluster`

### Относительные пути

Относительные пути можно использовать в некоторых контекстах, но для надежности рекомендуется использовать абсолютные пути.

### Специальные символы

- `.` (точка) - текущий контекст (в начале ссылки)
- `:` (двоеточие) - разделитель между контекстом и элементом
- `{}` (фигурные скобки) - обрамляют ссылку на элемент другого контекста

## Частые ошибки и правильные форматы

### ❌ Неправильно

```
target: "device1Sine"
expression: "users.admin.devices.device1.sine"
```

### ✅ Правильно

```
target: ".:device1Sine"
expression: "{users.admin.devices.device1:sine}"
```

### ❌ Неправильно

```
target: "users.admin.models.cluster.device1Sine"
expression: "users.admin.devices.device1:sine"
```

### ✅ Правильно

```
target: ".:device1Sine"
expression: "{users.admin.devices.device1:sine}"
```

## Использование в правилах (Rules) и выражениях (Expressions)

В правилах и выражениях ссылки используются для доступа к значениям переменных и вызова функций:

**Пример выражения в правиле:**

```javascript
if ({users.admin.devices.device1:sine} > 0 && 
    {users.admin.devices.device2:sine} > 0 && 
    {users.admin.devices.device3:sine} > 0) {
  .:clusterStatus = "ошибка";
  .:fireEvent("clusterAlarm", "Кластер перешел в состояние ошибки");
} else {
  .:clusterStatus = "ОК";
}
```

**Пример с использованием переменных текущего контекста:**

```javascript
if (.:device1Sine > 0 && .:device2Sine > 0 && .:device3Sine > 0) {
  .:clusterStatus = "ошибка";
  .:fireEvent("clusterAlarm", "Кластер перешел в состояние ошибки");
} else {
  .:clusterStatus = "ОК";
}
```

## Рекомендации

1. **Всегда используйте абсолютные пути** для ссылок на другие контексты
2. **Используйте `.:` для ссылок на элементы текущего контекста** - это делает код более читаемым и переносимым
3. **Проверяйте синтаксис** - неправильный формат ссылки приведет к ошибке выполнения
4. **В привязках всегда указывайте `target` с префиксом `.:`** - это указывает на переменную текущего контекста
5. **В `expression` используйте `{контекст:элемент}`** для ссылок на элементы других контекстов

## Дополнительные ресурсы

- Официальная документация AggreGate (aggregate_ru_5.70.15.chm)
- [Руководство по работе с API AggreGate через MCP сервер](AGGREGATE_API_GUIDE.md)

