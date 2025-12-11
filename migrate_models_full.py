#!/usr/bin/env python3
"""
Скрипт для полной миграции всех моделей с исходного сервера на целевой
Использует прямой подход с учетом всех особенностей
"""

import json
import sys

# Данные для подключения
SOURCE_CONN = "source"
TARGET_CONN = "target"

def migrate_model(model_path, source_conn, target_conn):
    """
    Мигрирует одну модель с учетом всех особенностей
    
    Args:
        model_path: путь к модели (например, "users.admin.models.objects")
        source_conn: ключ подключения к исходному серверу
        target_conn: ключ подключения к целевому серверу
    
    Returns:
        dict: результат миграции
    """
    result = {
        "model": model_path,
        "success": False,
        "error": None,
        "components": {}
    }
    
    try:
        print(f"\n{'='*60}")
        print(f"Миграция модели: {model_path}")
        print(f"{'='*60}")
        
        # Шаг 1: Получение данных с исходного сервера
        print("\n1. Получение данных с исходного сервера...")
        
        # Получить childInfo
        childInfo = get_variable(model_path, "childInfo", source_conn)
        model_type = childInfo['records'][0]['type']
        model_name = childInfo['records'][0]['name']
        model_description = childInfo['records'][0]['description']
        
        print(f"   Тип: {model_type} ({'абсолютная' if model_type == 1 else 'относительная' if model_type == 0 else 'экземплярная'})")
        print(f"   Имя: {model_name}")
        print(f"   Описание: {model_description}")
        
        # Получить info (группа)
        info = get_variable(model_path, "info", source_conn)
        group = info['records'][0].get('group', 'default')
        print(f"   Группа: {group}")
        
        # Получить компоненты
        modelVariables = get_variable(model_path, "modelVariables", source_conn)
        modelFunctions = get_variable(model_path, "modelFunctions", source_conn)
        modelEvents = get_variable(model_path, "modelEvents", source_conn)
        bindings = get_variable(model_path, "bindings", source_conn)
        ruleSets = get_variable(model_path, "ruleSets", source_conn)
        
        result['components'] = {
            'variables': modelVariables['recordCount'],
            'functions': modelFunctions['recordCount'],
            'events': modelEvents['recordCount'],
            'bindings': bindings['recordCount'],
            'ruleSets': ruleSets['recordCount']
        }
        
        print(f"   Переменные: {modelVariables['recordCount']}")
        print(f"   Функции: {modelFunctions['recordCount']}")
        print(f"   События: {modelEvents['recordCount']}")
        print(f"   Привязки: {bindings['recordCount']}")
        print(f"   Наборы правил: {ruleSets['recordCount']}")
        
        # Шаг 2: Проверка/создание модели на целевом сервере
        print("\n2. Проверка модели на целевом сервере...")
        
        try:
            existing = get_context(model_path, target_conn)
            print(f"   Модель уже существует")
        except:
            # Создать контекст модели
            parent_path = ".".join(model_path.split(".")[:-1])
            model_name_only = model_path.split(".")[-1]
            create_context(parent_path, model_name_only, model_description, target_conn)
            print(f"   Модель создана")
        
        # Шаг 3: Установка childInfo
        print("\n3. Установка childInfo...")
        
        # Базовые поля
        set_variable_field(model_path, "childInfo", "type", model_type, target_conn)
        set_variable_field(model_path, "childInfo", "description", model_description, target_conn)
        set_variable_field(model_path, "childInfo", "enabled", childInfo['records'][0].get('enabled', True), target_conn)
        
        # Для относительных моделей (type=0)
        if model_type == 0:
            defaultContext = childInfo['records'][0].get('defaultContext')
            if defaultContext:
                set_variable_field(model_path, "childInfo", "defaultContext", defaultContext, target_conn)
                print(f"   defaultContext установлен: {defaultContext}")
            validityExpression = childInfo['records'][0].get('validityExpression', '')
            if validityExpression:
                set_variable_field(model_path, "childInfo", "validityExpression", validityExpression, target_conn)
        
        # Для экземплярных моделей (type=2)
        if model_type == 2:
            containerType = childInfo['records'][0].get('containerType')
            containerName = childInfo['records'][0].get('containerName')
            objectType = childInfo['records'][0].get('objectType')
            if containerType:
                set_variable_field(model_path, "childInfo", "containerType", containerType, target_conn)
            if containerName:
                set_variable_field(model_path, "childInfo", "containerName", containerName, target_conn)
            if objectType:
                set_variable_field(model_path, "childInfo", "objectType", objectType, target_conn)
            print(f"   containerType: {containerType}, containerName: {containerName}, objectType: {objectType}")
        
        # Шаг 4: Установка компонентов
        print("\n4. Установка компонентов...")
        
        if modelVariables['recordCount'] > 0:
            set_variable(model_path, "modelVariables", modelVariables, target_conn)
            print(f"   ✅ modelVariables установлены ({modelVariables['recordCount']} переменных)")
        
        if modelFunctions['recordCount'] > 0:
            set_variable(model_path, "modelFunctions", modelFunctions, target_conn)
            print(f"   ✅ modelFunctions установлены ({modelFunctions['recordCount']} функций)")
        
        if modelEvents['recordCount'] > 0:
            set_variable(model_path, "modelEvents", modelEvents, target_conn)
            print(f"   ✅ modelEvents установлены ({modelEvents['recordCount']} событий)")
        
        if bindings['recordCount'] > 0:
            set_variable(model_path, "bindings", bindings, target_conn)
            print(f"   ✅ bindings установлены ({bindings['recordCount']} привязок)")
        
        if ruleSets['recordCount'] > 0:
            set_variable(model_path, "ruleSets", ruleSets, target_conn)
            print(f"   ✅ ruleSets установлены ({ruleSets['recordCount']} наборов правил)")
        
        # Шаг 5: Установка группы (если нужно)
        if group and group != 'default':
            print(f"\n5. Установка группы: {group}")
            # Группа устанавливается через info переменную
            try:
                target_info = get_variable(model_path, "info", target_conn)
                if target_info['records'][0].get('group') != group:
                    set_variable_field(model_path, "info", "group", group, target_conn)
                    print(f"   ✅ Группа установлена: {group}")
            except Exception as e:
                print(f"   ⚠️ Не удалось установить группу: {e}")
        
        print(f"\n✅ Модель {model_path} успешно мигрирована!")
        result['success'] = True
        
    except Exception as e:
        print(f"\n❌ ОШИБКА при миграции {model_path}: {e}")
        result['error'] = str(e)
    
    return result


def migrate_all_models(source_conn=SOURCE_CONN, target_conn=TARGET_CONN):
    """
    Мигрирует все модели с исходного сервера на целевой
    """
    print("="*60)
    print("НАЧАЛО ПОЛНОЙ МИГРАЦИИ МОДЕЛЕЙ")
    print("="*60)
    
    # Получить список всех моделей
    models = list_contexts("users.admin.models.*", source_conn)
    
    # Исключить тестовые модели
    test_models = ['objects_test', 'testRelative', 'testInstantiable']
    models = [m for m in models if m['name'] not in test_models]
    
    print(f"\nНайдено моделей для миграции: {len(models)}")
    
    results = []
    success_count = 0
    error_count = 0
    
    for i, model in enumerate(models, 1):
        print(f"\n[{i}/{len(models)}] ", end="")
        result = migrate_model(model['path'], source_conn, target_conn)
        results.append(result)
        
        if result['success']:
            success_count += 1
        else:
            error_count += 1
    
    # Итоги
    print("\n" + "="*60)
    print("ИТОГИ МИГРАЦИИ")
    print("="*60)
    print(f"Успешно: {success_count}")
    print(f"Ошибок: {error_count}")
    print(f"Всего: {len(models)}")
    
    # Сохранить результаты
    with open('migration_results.json', 'w', encoding='utf-8') as f:
        json.dump(results, f, ensure_ascii=False, indent=2)
    
    print(f"\nРезультаты сохранены в migration_results.json")
    
    return results


if __name__ == "__main__":
    # Для использования через MCP нужно будет адаптировать функции
    # get_variable, set_variable, create_context, list_contexts, get_context, set_variable_field
    # к соответствующим MCP инструментам
    
    print("Этот скрипт требует адаптации для работы с MCP инструментами")
    print("Используйте его как шаблон для создания миграционного скрипта")

