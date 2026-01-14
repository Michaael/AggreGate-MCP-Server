# Руководство по автоматическому тестированию и валидации для ИИ

## Проблема
При создании элементов через MCP ИИ не проверяет их работоспособность, что приводит к неработающим заданиям.

## Решение: Автоматическое тестирование после каждого создания

### 1. Чек-лист проверки после создания функции

**ОБЯЗАТЕЛЬНО после создания функции:**

```python
# Шаг 1: Проверить, что функция создана
functions = aggregate_list_functions(path)
if functionName not in functions:
    raise Error("Функция не создана")

# Шаг 2: Получить детали функции
function_details = aggregate_get_function(path, functionName)

# Шаг 3: Проверить формат (для Expression функций)
if functionType == 1:  # Expression
    # Проверить inputFormat и outputFormat
    validate_expression_format(function_details)
    
    # КРИТИЧНО: Проверить, что все поля присутствуют
    expected_field_count = len(inputFields)  # из aggregate_build_expression
    actual_field_count = len(function_details.inputFields)
    if actual_field_count < expected_field_count:
        # Проблема: формат потерял поля - нужно использовать <<>> для множественных полей
        raise Error(f"inputFormat потерял поля: ожидалось {expected_field_count}, получено {actual_field_count}. Используйте формат с <<>> для множественных полей!")

# Шаг 4: ПРОТЕСТИРОВАТЬ функцию с тестовыми данными
test_result = aggregate_test_function(path, functionName, test_parameters)

if not test_result.success:
    # Исправить ошибки
    fix_function_errors(test_result.error)
    # Повторить тест
    test_result = aggregate_test_function(path, functionName, test_parameters)

# Шаг 5: Если тест не прошел, использовать aggregate_build_expression
if not test_result.success:
    correct_expression = aggregate_build_expression(
        inputFields=function_details.inputFields,
        outputFields=function_details.outputFields,
        formula="правильная формула"
    )
    # Пересоздать функцию с правильным выражением
```

### 2. Чек-лист проверки после создания модели

**ОБЯЗАТЕЛЬНО после создания модели:**

```python
# Шаг 1: Проверить создание
context = aggregate_get_context(path)
if not context:
    raise Error("Модель не создана")

# Шаг 2: Проверить наличие переменных
variables = aggregate_list_variables(path)
required_variables = ["variable1", "variable2"]
for var in required_variables:
    if var not in variables:
        aggregate_create_variable(path, var, format, ...)

# Шаг 3: Проверить наличие событий
events = aggregate_list_events(path)
required_events = ["event1", "event2"]
for event in required_events:
    if event not in events:
        aggregate_create_event(path, event, ...)

# Шаг 4: Проверить наличие функций
functions = aggregate_list_functions(path)
required_functions = ["function1", "function2"]
for func in required_functions:
    if func not in functions:
        aggregate_create_function(path, func, ...)
        # ОБЯЗАТЕЛЬНО протестировать
        test_function(path, func)
```

### 3. Чек-лист проверки типа модели и привязок (bindings)

**КРИТИЧНО: Тип модели и привязки часто забываются!**

```python
# После создания модели, которая должна работать с устройствами:

# Шаг 1: Проверить тип модели
child_info = aggregate_get_variable(path, "childInfo")
model_type = child_info.records[0].get("type", 0)  # 0=абсолютная, 1=относительная, 2=экземплярная
container_type = child_info.records[0].get("containerType", "objects")
object_type = child_info.records[0].get("objectType", "object")

# Шаг 2: Если модель должна быть относительной, проверить настройки
if model_type == 1:  # Относительная модель
    if container_type != "devices" or object_type != "device":
        # Исправить настройки
        aggregate_set_variable_field(path, "childInfo", "containerType", "devices")
        aggregate_set_variable_field(path, "childInfo", "objectType", "device")

# Шаг 3: Проверить наличие переменной bindings
bindings = aggregate_get_variable(path, "bindings")

# Шаг 4: Если привязок нет, создать их
if not bindings or bindings.recordCount == 0:
    # Создать привязки в зависимости от типа модели
    create_bindings(path, device_path, variables_to_bind, model_type)

def create_bindings(model_path, device_path, variables, model_type=0):
    bindings_records = []
    for var in variables:
        if model_type == 1:  # Относительная модель
            # Использовать относительные ссылки {.:variableName}
            expression = f"{{.:{var}}}"
        else:  # Абсолютная или экземплярная модель
            # Использовать абсолютные пути {context:variable}
            expression = f"{{{device_path}:{var}}}"
        
        bindings_records.append({
            "target": f".:{var}",  # Переменная в модели
            "expression": expression,
            "onevent": True
        })
    
    aggregate_set_variable(
        path=model_path,
        name="bindings",
        value={
            "records": bindings_records,
            "format": {
                "fields": [
                    {"name": "target", "type": "S"},
                    {"name": "expression", "type": "S"},
                    {"name": "onevent", "type": "B"}
                ]
            }
        }
    )
```

### 4. Автоматическое исправление ошибок функций

**Когда aggregate_test_function возвращает ошибку:**

```python
def fix_function_errors(error_message, function_path, function_name):
    # Анализ ошибки
    error_info = aggregate_explain_error(error_message)
    
    if error_info.category == "function_format":
        # Проблема с форматом - использовать aggregate_build_expression
        correct_formats = aggregate_build_expression(
            inputFields=...,
            outputFields=...,
            formula=...
        )
        # Пересоздать функцию с правильными форматами
        aggregate_create_function(
            path=function_path,
            functionName=function_name,
            inputFormat=correct_formats.inputFormat,
            outputFormat=correct_formats.outputFormat,
            expression=correct_formats.expression
        )
    
    elif "Field not found" in error_message:
        # Использовать aggregate_fix_function_parameters
        fixed_params = aggregate_fix_function_parameters(
            path=function_path,
            functionName=function_name,
            errorMessage=error_message,
            providedParameters=...
        )
        # Повторить вызов с исправленными параметрами
```

### 5. Валидация виджетов

**После создания виджета:**

```python
# Шаг 1: Проверить создание
widget = aggregate_get_widget_template(path)
if not widget:
    raise Error("Виджет не создан")

# Шаг 2: Попытаться установить шаблон (может не работать через MCP)
try:
    aggregate_set_widget_template(path, template)
except Error as e:
    if "read-only" in str(e):
        # Шаблон можно установить только через веб-интерфейс
        log_warning("XML шаблон требует настройки через веб-интерфейс")
    else:
        raise
```

### 6. Валидация дашбордов

**После создания дашборда:**

```python
# Шаг 1: Проверить создание
dashboard = aggregate_list_dashboards(parentPath)
if dashboard_name not in dashboard:
    raise Error("Дашборд не создан")

# Шаг 2: Проверить элементы
# (элементы могут требовать специального формата параметров)
```

### 7. Полный процесс валидации задания

**Для каждого задания:**

```python
def validate_task(task_number, task_description):
    print(f"Валидация задания {task_number}: {task_description}")
    
    # 1. Создать элементы
    create_task_elements(task_number)
    
    # 2. Проверить создание
    validate_created_elements(task_number)
    
    # 3. Протестировать функции
    test_all_functions(task_number)
    
    # 4. Проверить привязки (если нужны)
    if needs_bindings(task_number):
        validate_bindings(task_number)
    
    # 5. Проверить работоспособность
    test_functionality(task_number)
    
    # 6. Задокументировать ограничения
    document_limitations(task_number)
    
    print(f"✅ Задание {task_number} валидировано")
```

## Автоматические правила для ИИ

### Правило 1: Всегда тестировать функции
```
ПОСЛЕ создания функции:
1. aggregate_test_function() - ОБЯЗАТЕЛЬНО
2. Если ошибка - aggregate_explain_error()
3. Если проблема с форматом - aggregate_build_expression()
4. Если проблема с параметрами - aggregate_fix_function_parameters()
5. Пересоздать и повторить тест
```

### Правило 2: Всегда проверять привязки
```
ПОСЛЕ создания модели, работающей с устройствами:
1. Проверить наличие переменной bindings
2. Если нет - создать привязки
3. Проверить правильность формата ссылок
```

### Правило 3: Всегда валидировать Expression функции
```
ПЕРЕД созданием Expression функции:
1. aggregate_build_expression() - для правильных форматов
2. aggregate_validate_expression() - для проверки синтаксиса
3. ТОЛЬКО ПОТОМ aggregate_create_function()
```

### Правило 4: Проверять ограничения MCP
```
ПОСЛЕ создания элемента:
1. Проверить, можно ли его настроить через MCP
2. Если нет (например, XML шаблоны) - задокументировать
3. Предложить альтернативный способ настройки
```

## Примеры автоматического исправления

### Пример 1: Исправление функции с неправильным форматом

```python
# Ошибка: Field 'b' not found
error = "Field 'b' not found in data record: a"

# Шаг 1: Объяснить ошибку
explanation = aggregate_explain_error(error)
# Результат: проблема с inputFormat

# Шаг 2: Построить правильный формат
correct = aggregate_build_expression(
    inputFields=[
        {"name": "a", "type": "E"},
        {"name": "b", "type": "E"}
    ],
    outputFields=[{"name": "result", "type": "E"}],
    formula="({a} + {b})"
)

# Шаг 3: Пересоздать функцию
aggregate_create_function(
    path=path,
    functionName="calculate",
    inputFormat=correct.inputFormat,  # <a><E><b><E>
    outputFormat=correct.outputFormat,  # <result><E>
    expression=correct.expression  # table("<<result><E>>", ({a} + {b}))
)

# Шаг 4: Протестировать
test_result = aggregate_test_function(path, "calculate", {"a": 10, "b": 20})
assert test_result.success
```

### Пример 2: Создание привязок для модели

```python
# После создания модели alarm_event1_event2
model_path = "users.admin.models.alarm_event1_event2"
device_path = "users.admin.devices.virtualDevice"

# Проверить наличие привязок
bindings = aggregate_get_variable(model_path, "bindings")
if not bindings or bindings.recordCount == 0:
    # Создать привязки для событий устройства
    aggregate_set_variable(
        path=model_path,
        name="bindings",
        value={
            "records": [
                {
                    "target": ".:event2Int",
                    "expression": "{users.admin.devices.virtualDevice:Event2.Int}",
                    "onevent": True
                }
            ],
            "format": {
                "fields": [
                    {"name": "target", "type": "S"},
                    {"name": "expression", "type": "S"},
                    {"name": "onevent", "type": "B"}
                ]
            }
        }
    )
```

## Чек-лист для каждого задания

```
□ Элементы созданы
□ Элементы проверены (list_*)
□ Функции протестированы (test_function)
□ Привязки созданы (если нужны)
□ Ошибки исправлены
□ Ограничения задокументированы
□ Задание работает или есть четкие инструкции по доработке
```

## Рекомендации для улучшения процесса

1. **Создать функцию-обертку для создания функций с автоматическим тестированием:**
   ```python
   def create_and_test_function(path, name, ...):
       # Создать
       result = aggregate_create_function(...)
       # Протестировать
       test = aggregate_test_function(path, name, test_data)
       if not test.success:
           # Исправить
           fix_and_retry(...)
       return result
   ```

2. **Создать функцию для автоматического создания привязок:**
   ```python
   def ensure_bindings(model_path, device_path, variable_mappings):
       # Проверить и создать привязки
   ```

3. **Создать валидатор для каждого типа задания:**
   ```python
   def validate_task_1():  # Пользователи и доступ
   def validate_task_2():  # Тревога Event 1 -> Event 2
   # и т.д.
   ```

4. **Добавить автоматическое логирование проблем:**
   - Что создано
   - Что не работает
   - Что требует доработки через веб-интерфейс
