# Отчёт об улучшении обработки ошибок для ИИ

## Дата: 2024-12-XX

## Цель
Сделать так, чтобы ИИ понимал ошибки типа "Field 'value2' not found in data record: value1" и мог самостоятельно исправлять проблемы с созданием функций и передачей параметров.

## Реализованные улучшения

### 1. ✅ Улучшен `aggregate_explain_error`

**Файл:** `mcp-server/src/main/java/com/tibbo/aggregate/mcp/tools/server/ExplainErrorTool.java`

**Добавлено:**
- Распознавание ошибок типа "Field 'fieldName' not found in data record"
- Категория ошибки: `function_parameter_mismatch`
- Извлечение имени отсутствующего поля из сообщения об ошибке
- Извлечение имени найденного поля (если есть)
- Детальные объяснения и рекомендации для ИИ

**Пример использования:**
```json
{
  "category": "function_parameter_mismatch",
  "missingField": "value2",
  "foundField": "value1",
  "explanation": "Функция ожидает поле 'value2', но оно отсутствует в параметрах...",
  "recommendation": "1. Вызовите aggregate_get_function для получения inputFormat..."
}
```

### 2. ✅ Улучшен `aggregate_test_function`

**Файл:** `mcp-server/src/main/java/com/tibbo/aggregate/mcp/tools/function/TestFunctionTool.java`

**Добавлено:**
- Автоматическое заполнение всех обязательных полей из inputFormat
- Предупреждения о недостающих полях (`parameterWarning`, `missingFields`)
- Автоматическая установка значений по умолчанию для отсутствующих полей
- Информация о предоставленных полях (`providedFields`)

**Новое поведение:**
- Если переданы не все поля из inputFormat, инструмент автоматически добавляет недостающие с значениями по умолчанию
- Возвращает предупреждение с информацией о том, какие поля были добавлены

### 3. ✅ Создан новый инструмент `aggregate_fix_function_parameters`

**Файл:** `mcp-server/src/main/java/com/tibbo/aggregate/mcp/tools/function/FixFunctionParametersTool.java`

**Функциональность:**
- Анализирует ошибки параметров функций
- Извлекает информацию о функции через `aggregate_get_function`
- Определяет все обязательные поля из inputFormat (использует `inputFields` или `inputFormat.fields`)
- Создаёт исправленные параметры, включая все обязательные поля
- Добавляет значения по умолчанию для отсутствующих полей

**Пример использования:**
```python
# ИИ получает ошибку при тестировании функции
error = "Field 'value2' not found in data record: value1"

# ИИ вызывает aggregate_fix_function_parameters
fix_result = aggregate_fix_function_parameters(
    path="users.admin.models.temperature_monitor",
    functionName="calculate_average",
    errorMessage=error,
    providedParameters={"value1": 10.0}
)

# Получает исправленные параметры
corrected_params = fix_result["correctedParameters"]
# {"value1": 10.0, "value2": 0.0}

# Использует исправленные параметры для повторного теста
test_result = aggregate_test_function(
    path="users.admin.models.temperature_monitor",
    functionName="calculate_average",
    parameters=corrected_params
)
```

### 4. ✅ Улучшена обработка ошибок Expression функций

**Добавлено в `aggregate_explain_error`:**
- Распознавание ошибок типа "Error resolving reference" и "Error evaluating expression"
- Категория: `expression_execution_error`
- Извлечение имени отсутствующего поля из ошибки Expression
- Рекомендации по исправлению

## Рабочий процесс для ИИ

### Сценарий 1: Ошибка при тестировании функции

1. **ИИ вызывает `aggregate_test_function`** с неполными параметрами
2. **Получает ошибку:** "Field 'value2' not found in data record: value1"
3. **ИИ вызывает `aggregate_explain_error`** для анализа:
   ```python
   explain = aggregate_explain_error(
       message=error,
       toolName="aggregate_test_function"
   )
   # Получает: category="function_parameter_mismatch", missingField="value2"
   ```
4. **ИИ вызывает `aggregate_fix_function_parameters`** для автоматического исправления:
   ```python
   fix = aggregate_fix_function_parameters(
       path=path,
       functionName=functionName,
       errorMessage=error,
       providedParameters=original_params
   )
   corrected_params = fix["correctedParameters"]
   ```
5. **ИИ повторяет тест** с исправленными параметрами:
   ```python
   test_result = aggregate_test_function(
       path=path,
       functionName=functionName,
       parameters=corrected_params
   )
   ```

### Сценарий 2: Автоматическое исправление в `aggregate_test_function`

1. **ИИ вызывает `aggregate_test_function`** с неполными параметрами
2. **Инструмент автоматически:**
   - Определяет все обязательные поля из inputFormat
   - Добавляет недостающие поля со значениями по умолчанию
   - Возвращает предупреждение о добавленных полях
3. **Тест проходит успешно** (если нет других ошибок)

## Технические детали

### Извлечение полей из inputFormat

`aggregate_fix_function_parameters` использует два источника:
1. **`inputFields`** (предпочтительно) - массив полей из `aggregate_get_function`
2. **`inputFormat.fields`** (fallback) - поля из структуры inputFormat

### Значения по умолчанию

Для отсутствующих полей устанавливаются значения по умолчанию:
- `S` (String): `""`
- `I` (Integer): `0`
- `L` (Long): `0L`
- `E` (Double/Float): `0.0`
- `B` (Boolean): `false`

### Регистрация инструментов

Все новые инструменты зарегистрированы в `ToolRegistry.java`:
- `aggregate_explain_error` (улучшен)
- `aggregate_test_function` (улучшен)
- `aggregate_fix_function_parameters` (новый)

## Результаты

### ✅ Достигнуто

1. **ИИ может анализировать ошибки параметров** через `aggregate_explain_error`
2. **ИИ может автоматически исправлять параметры** через `aggregate_fix_function_parameters`
3. **`aggregate_test_function` автоматически заполняет недостающие поля**
4. **Детальные объяснения и рекомендации** для каждой категории ошибок

### ⚠️ Известные ограничения

1. **Проблема с созданием функций:** Иногда функции создаются с неправильным inputFormat (только одно поле вместо двух). Это требует дополнительного исследования в `CreateFunctionTool`.

2. **Значения по умолчанию:** Могут быть не подходящими для всех случаев. ИИ должен проверять результаты и корректировать значения.

## Рекомендации для дальнейшего развития

1. **Улучшить `CreateFunctionTool`** для гарантированно правильного создания inputFormat
2. **Добавить валидацию параметров** перед вызовом функции
3. **Улучшить значения по умолчанию** на основе контекста функции
4. **Добавить автоматическое исправление** в `aggregate_create_function` для проверки соответствия inputFormat и expression

## Заключение

Система теперь предоставляет ИИ инструменты для:
- ✅ Понимания ошибок параметров функций
- ✅ Автоматического исправления параметров
- ✅ Получения детальных объяснений и рекомендаций
- ✅ Автоматического заполнения недостающих полей при тестировании

ИИ может самостоятельно исправлять большинство ошибок, связанных с параметрами функций, без необходимости ручного вмешательства.

---

**Статус:** ✅ **РЕАЛИЗОВАНО И ГОТОВО К ИСПОЛЬЗОВАНИЮ**
