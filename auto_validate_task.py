#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Автоматическая валидация и тестирование созданных элементов
Используется ИИ для проверки работоспособности после создания
"""

def validate_function(path, function_name, test_parameters=None, expected_input_fields=None):
    """
    Валидация функции: создание, тестирование, исправление ошибок
    
    Args:
        path: Путь к контексту
        function_name: Имя функции
        test_parameters: Параметры для тестирования
        expected_input_fields: Ожидаемые входные поля (для проверки формата)
    """
    from mcp_aggregate_aggregate import (
        aggregate_list_functions,
        aggregate_get_function,
        aggregate_test_function,
        aggregate_call_function,
        aggregate_explain_error,
        aggregate_build_expression,
        aggregate_validate_expression,
        aggregate_fix_function_parameters
    )
    
    # Шаг 1: Проверить существование
    functions = aggregate_list_functions(path)
    if function_name not in [f['name'] for f in functions]:
        return {
            'success': False,
            'error': f'Функция {function_name} не найдена в {path}',
            'step': 'existence_check'
        }
    
    # Шаг 2: Получить детали
    try:
        func_details = aggregate_get_function(path, function_name)
    except Exception as e:
        return {
            'success': False,
            'error': f'Ошибка получения деталей функции: {e}',
            'step': 'get_details'
        }
    
    # Шаг 2.5: КРИТИЧНО - Проверить inputFormat для множественных полей
    if expected_input_fields and len(expected_input_fields) > 1:
        actual_field_count = len(func_details.get('inputFields', []))
        expected_field_count = len(expected_input_fields)
        
        if actual_field_count < expected_field_count:
            return {
                'success': False,
                'error': f'inputFormat потерял поля: ожидалось {expected_field_count}, получено {actual_field_count}',
                'step': 'format_check',
                'needs_fix': True,
                'fix_suggestion': 'Используйте формат С <<>> скобками для множественных полей: <<field1><E><field2><E>>'
            }
    
    # Шаг 3: Для Expression функций - валидировать формат
    if func_details.get('functionType') == 1:  # Expression
        if 'inputFormat' in func_details and 'outputFormat' in func_details:
            validation = aggregate_validate_expression(
                inputFormat=func_details['inputFormat'],
                outputFormat=func_details['outputFormat'],
                expression=func_details.get('expression', '')
            )
            if not validation.get('valid'):
                return {
                    'success': False,
                    'error': f'Неверный формат выражения: {validation.get("errors")}',
                    'step': 'format_validation',
                    'suggestions': validation.get('suggestions', [])
                }
    
    # Шаг 4: Тестировать функцию
    if test_parameters is None:
        # Создать тестовые параметры на основе inputFormat
        test_parameters = create_test_parameters(func_details)
    
    # Шаг 4.5: Определить способ тестирования
    # Для функций с множественными полями используем aggregate_call_function с DataTable
    field_count = len(func_details.get('inputFields', []))
    use_datatable = field_count > 1
    
    try:
        if use_datatable:
            # Для множественных полей - используем DataTable формат
            test_result = aggregate_call_function(
                path=path,
                functionName=function_name,
                parameters={
                    "records": [test_parameters],
                    "format": {
                        "fields": [
                            {"name": f['name'], "type": f['type']}
                            for f in func_details.get('inputFields', [])
                        ]
                    }
                }
            )
            # Преобразуем результат в формат test_function для совместимости
            if 'error' in test_result:
                test_result = {'success': False, 'error': test_result['error']}
            else:
                test_result = {'success': True, 'result': test_result}
        else:
            # Для одного поля - используем обычный test_function
            test_result = aggregate_test_function(path, function_name, test_parameters)
        
        if not test_result.get('success'):
            # Шаг 5: Объяснить ошибку
            error_explanation = aggregate_explain_error(
                message=test_result.get('error', ''),
                toolName='aggregate_test_function'
            )
            
            # Шаг 6: Попытаться исправить
            if 'Field not found' in test_result.get('error', ''):
                # Проверить, не проблема ли это с inputFormat для множественных полей
                if expected_input_fields and len(expected_input_fields) > 1:
                    actual_field_count = len(func_details.get('inputFields', []))
                    if actual_field_count < len(expected_input_fields):
                        return {
                            'success': False,
                            'error': test_result.get('error'),
                            'step': 'function_test',
                            'explanation': error_explanation,
                            'needs_fix': True,
                            'fix_type': 'input_format_multiple_fields',
                            'fix_suggestion': f'Используйте формат С <<>> для множественных полей: <<{"".join([f"<{f["name"]}><{f["type"]}>" for f in expected_input_fields])}>>'
                        }
                
                fixed_params = aggregate_fix_function_parameters(
                    path=path,
                    functionName=function_name,
                    errorMessage=test_result.get('error', ''),
                    providedParameters=test_parameters
                )
                return {
                    'success': False,
                    'error': test_result.get('error'),
                    'step': 'function_test',
                    'explanation': error_explanation,
                    'fixed_parameters': fixed_params,
                    'needs_fix': True
                }
            
            return {
                'success': False,
                'error': test_result.get('error'),
                'step': 'function_test',
                'explanation': error_explanation,
                'needs_fix': True
            }
        
        return {
            'success': True,
            'test_result': test_result,
            'function_details': func_details
        }
        
    except Exception as e:
        return {
            'success': False,
            'error': f'Ошибка тестирования функции: {e}',
            'step': 'function_test'
        }


def create_test_parameters(function_details):
    """Создать тестовые параметры на основе inputFormat функции"""
    test_params = {}
    input_fields = function_details.get('inputFields', [])
    
    for field in input_fields:
        field_name = field.get('name')
        field_type = field.get('type')
        
        # Значения по умолчанию для разных типов
        if field_type == 'E' or field_type == 'F' or field_type == 'D':
            test_params[field_name] = 10.5
        elif field_type == 'I' or field_type == 'L':
            test_params[field_name] = 10
        elif field_type == 'S':
            test_params[field_name] = "test"
        elif field_type == 'B':
            test_params[field_name] = True
        else:
            test_params[field_name] = None
    
    return test_params


def validate_model(path, required_variables=None, required_events=None, required_functions=None):
    """
    Валидация модели: проверка всех необходимых элементов
    """
    from mcp_aggregate_aggregate import (
        aggregate_get_context,
        aggregate_list_variables,
        aggregate_list_events,
        aggregate_list_functions
    )
    
    results = {
        'success': True,
        'errors': [],
        'warnings': []
    }
    
    # Проверить существование контекста
    try:
        context = aggregate_get_context(path)
        if not context:
            results['success'] = False
            results['errors'].append(f'Контекст {path} не найден')
            return results
    except Exception as e:
        results['success'] = False
        results['errors'].append(f'Ошибка получения контекста: {e}')
        return results
    
    # Проверить переменные
    if required_variables:
        variables = aggregate_list_variables(path)
        var_names = [v['name'] for v in variables]
        for var in required_variables:
            if var not in var_names:
                results['warnings'].append(f'Переменная {var} не найдена')
    
    # Проверить события
    if required_events:
        events = aggregate_list_events(path)
        event_names = [e['name'] for e in events]
        for event in required_events:
            if event not in event_names:
                results['warnings'].append(f'Событие {event} не найдено')
    
    # Проверить функции
    if required_functions:
        functions = aggregate_list_functions(path)
        func_names = [f['name'] for f in functions]
        for func in required_functions:
            if func not in func_names:
                results['warnings'].append(f'Функция {func} не найдена')
    
    return results


def validate_bindings(model_path, device_path, expected_bindings=None):
    """
    Валидация привязок модели к устройству
    """
    from mcp_aggregate_aggregate import aggregate_get_variable
    
    results = {
        'success': True,
        'bindings_exist': False,
        'bindings_count': 0,
        'errors': []
    }
    
    try:
        bindings = aggregate_get_variable(model_path, 'bindings')
        if bindings and bindings.get('recordCount', 0) > 0:
            results['bindings_exist'] = True
            results['bindings_count'] = bindings.get('recordCount', 0)
        else:
            results['warnings'] = ['Привязки не найдены - модель не будет получать данные от устройства']
            results['success'] = False
    except Exception as e:
        results['errors'].append(f'Ошибка проверки привязок: {e}')
        results['success'] = False
    
    return results


def validate_task_complete(task_number, validation_checks):
    """
    Полная валидация задания
    """
    results = {
        'task_number': task_number,
        'overall_success': True,
        'checks': [],
        'errors': [],
        'warnings': [],
        'needs_manual_setup': []
    }
    
    for check in validation_checks:
        check_result = check()
        results['checks'].append(check_result)
        
        if not check_result.get('success', True):
            results['overall_success'] = False
        
        if 'errors' in check_result:
            results['errors'].extend(check_result['errors'])
        
        if 'warnings' in check_result:
            results['warnings'].extend(check_result['warnings'])
        
        if 'needs_manual_setup' in check_result:
            results['needs_manual_setup'].extend(check_result['needs_manual_setup'])
    
    return results


# Пример использования для задания 7 (калькулятор)
def validate_task_7():
    """Валидация задания 7: Функция калькулятора"""
    model_path = "users.admin.models.calculator"
    function_name = "calculate"
    
    # Проверить модель
    model_check = validate_model(
        model_path,
        required_functions=[function_name]
    )
    
    # Проверить функцию
    func_check = validate_function(
        model_path,
        function_name,
        test_parameters={'a': 10, 'b': 20}
    )
    
    return {
        'model_check': model_check,
        'function_check': func_check,
        'success': model_check['success'] and func_check['success']
    }


if __name__ == "__main__":
    # Пример валидации
    result = validate_task_7()
    print(f"Результат валидации: {result}")
