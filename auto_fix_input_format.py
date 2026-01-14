#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Автоматическое исправление inputFormat для функций с множественными полями
"""

def fix_input_format_for_multiple_fields(path, function_name, input_fields, output_fields, expression):
    """
    Автоматически исправляет inputFormat для функций с множественными полями
    
    Args:
        path: Путь к контексту
        function_name: Имя функции
        input_fields: Список входных полей [{"name": "field1", "type": "E"}, ...]
        output_fields: Список выходных полей [{"name": "result", "type": "E"}, ...]
        expression: Выражение функции
    
    Returns:
        dict с результатом исправления
    """
    from mcp_aggregate_aggregate import (
        aggregate_get_function,
        aggregate_create_function
    )
    
    # Проверить текущий формат
    try:
        current = aggregate_get_function(path, function_name)
        current_field_count = len(current.get('inputFields', []))
        expected_field_count = len(input_fields)
        
        if current_field_count >= expected_field_count:
            return {
                'success': True,
                'message': 'Формат уже правильный',
                'fixed': False
            }
    except:
        pass
    
    # Определить правильный формат
    if len(input_fields) == 1:
        # Одно поле - без <<>>
        inputFormat = f"<{input_fields[0]['name']}><{input_fields[0]['type']}>"
        outputFormat = f"<{output_fields[0]['name']}><{output_fields[0]['type']}>"
    else:
        # Несколько полей - С <<>>
        input_fields_str = "".join([f"<{f['name']}><{f['type']}>" for f in input_fields])
        output_fields_str = "".join([f"<{f['name']}><{f['type']}>" for f in output_fields])
        inputFormat = f"<<{input_fields_str}>>"
        outputFormat = f"<<{output_fields_str}>>"
    
    # Пересоздать функцию с правильным форматом
    try:
        result = aggregate_create_function(
            path=path,
            functionName=function_name,
            functionType=1,
            inputFormat=inputFormat,
            outputFormat=outputFormat,
            expression=expression
        )
        
        # Проверить результат
        check = aggregate_get_function(path, function_name)
        check_field_count = len(check.get('inputFields', []))
        
        if check_field_count == len(input_fields):
            return {
                'success': True,
                'message': f'Формат исправлен: {inputFormat}',
                'fixed': True,
                'inputFormat': inputFormat,
                'outputFormat': outputFormat
            }
        else:
            return {
                'success': False,
                'error': f'Формат все еще неправильный: ожидалось {len(input_fields)} полей, получено {check_field_count}',
                'fixed': False
            }
    except Exception as e:
        return {
            'success': False,
            'error': f'Ошибка при исправлении формата: {e}',
            'fixed': False
        }


def auto_fix_function_format(path, function_name):
    """
    Автоматически определяет и исправляет проблему с форматом функции
    
    Args:
        path: Путь к контексту
        function_name: Имя функции
    
    Returns:
        dict с результатом
    """
    from mcp_aggregate_aggregate import (
        aggregate_get_function,
        aggregate_explain_error
    )
    
    try:
        func_details = aggregate_get_function(path, function_name)
        
        # Проверить expression для определения ожидаемых полей
        expression = func_details.get('expression', '')
        
        # Простой парсинг expression для поиска полей
        import re
        fields_in_expression = re.findall(r'\{(\w+)\}', expression)
        unique_fields = list(set(fields_in_expression))
        
        current_field_count = len(func_details.get('inputFields', []))
        
        if current_field_count < len(unique_fields):
            # Проблема: поля потеряны
            # Восстановить поля из expression
            input_fields = [{"name": f, "type": "E"} for f in unique_fields]
            output_fields = func_details.get('outputFields', [{"name": "result", "type": "E"}])
            
            return fix_input_format_for_multiple_fields(
                path=path,
                function_name=function_name,
                input_fields=input_fields,
                output_fields=output_fields,
                expression=expression
            )
        else:
            return {
                'success': True,
                'message': 'Формат правильный, исправление не требуется',
                'fixed': False
            }
            
    except Exception as e:
        return {
            'success': False,
            'error': f'Ошибка при автоматическом исправлении: {e}',
            'fixed': False
        }
