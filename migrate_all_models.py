#!/usr/bin/env python3
"""
Скрипт для переноса всех моделей с исходного сервера на целевой
Использует прямой подход для переноса всех компонентов модели
"""

import json

def migrate_model(source_path, target_path, source_conn, target_conn):
    """
    Переносит модель с исходного сервера на целевой
    
    Args:
        source_path: путь к модели на исходном сервере (например, "users.admin.models.objects")
        target_path: путь к модели на целевом сервере
        source_conn: ключ подключения к исходному серверу
        target_conn: ключ подключения к целевому серверу
    """
    print(f"\n{'='*60}")
    print(f"Перенос модели: {source_path} -> {target_path}")
    print(f"{'='*60}")
    
    # Шаг 1: Получение данных с исходного сервера
    print("\n1. Получение данных с исходного сервера...")
    
    try:
        # Получить childInfo
        childInfo = get_variable(source_path, "childInfo", source_conn)
        model_type = childInfo['records'][0]['type']
        model_name = childInfo['records'][0]['name']
        model_description = childInfo['records'][0]['description']
        
        print(f"   Тип модели: {model_type} ({'абсолютная' if model_type == 1 else 'относительная' if model_type == 0 else 'экземплярная'})")
        print(f"   Имя: {model_name}")
        print(f"   Описание: {model_description}")
        
        # Получить info (группа)
        info = get_variable(source_path, "info", source_conn)
        group = info['records'][0].get('group', 'default')
        print(f"   Группа: {group}")
        
        # Получить компоненты модели
        modelVariables = get_variable(source_path, "modelVariables", source_conn)
        modelFunctions = get_variable(source_path, "modelFunctions", source_conn)
        modelEvents = get_variable(source_path, "modelEvents", source_conn)
        bindings = get_variable(source_path, "bindings", source_conn)
        ruleSets = get_variable(source_path, "ruleSets", source_conn)
        
        print(f"   Переменные: {modelVariables['recordCount']}")
        print(f"   Функции: {modelFunctions['recordCount']}")
        print(f"   События: {modelEvents['recordCount']}")
        print(f"   Привязки: {bindings['recordCount']}")
        print(f"   Наборы правил: {ruleSets['recordCount']}")
        
    except Exception as e:
        print(f"   ОШИБКА при получении данных: {e}")
        return False
    
    # Шаг 2: Создание модели на целевом сервере
    print("\n2. Создание модели на целевом сервере...")
    
    try:
        # Проверить, существует ли модель
        try:
            existing = get_context(target_path, target_conn)
            print(f"   Модель уже существует, пропускаем создание")
        except:
            # Создать контекст модели
            parent_path = ".".join(target_path.split(".")[:-1])
            model_name_only = target_path.split(".")[-1]
            create_context(parent_path, model_name_only, model_description, target_conn)
            print(f"   Модель создана: {target_path}")
        
    except Exception as e:
        print(f"   ОШИБКА при создании модели: {e}")
        return False
    
    # Шаг 3: Установка компонентов
    print("\n3. Установка компонентов модели...")
    
    try:
        # Установить childInfo (если нужно обновить)
        if model_type != 1:  # Для относительных и экземплярных нужно установить defaultContext
            set_variable(target_path, "childInfo", childInfo, target_conn)
            print(f"   childInfo установлен")
        
        # Установить компоненты
        if modelVariables['recordCount'] > 0:
            set_variable(target_path, "modelVariables", modelVariables, target_conn)
            print(f"   modelVariables установлены ({modelVariables['recordCount']} переменных)")
        
        if modelFunctions['recordCount'] > 0:
            set_variable(target_path, "modelFunctions", modelFunctions, target_conn)
            print(f"   modelFunctions установлены ({modelFunctions['recordCount']} функций)")
        
        if modelEvents['recordCount'] > 0:
            set_variable(target_path, "modelEvents", modelEvents, target_conn)
            print(f"   modelEvents установлены ({modelEvents['recordCount']} событий)")
        
        if bindings['recordCount'] > 0:
            set_variable(target_path, "bindings", bindings, target_conn)
            print(f"   bindings установлены ({bindings['recordCount']} привязок)")
        
        if ruleSets['recordCount'] > 0:
            set_variable(target_path, "ruleSets", ruleSets, target_conn)
            print(f"   ruleSets установлены ({ruleSets['recordCount']} наборов правил)")
        
        print(f"\n✅ Модель {target_path} успешно перенесена!")
        return True
        
    except Exception as e:
        print(f"   ОШИБКА при установке компонентов: {e}")
        return False


def migrate_all_models(source_conn="source", target_conn="target"):
    """
    Переносит все модели с исходного сервера на целевой
    """
    print("="*60)
    print("НАЧАЛО МИГРАЦИИ ВСЕХ МОДЕЛЕЙ")
    print("="*60)
    
    # Получить список всех моделей
    models = list_contexts("users.admin.models.*", source_conn)
    
    print(f"\nНайдено моделей: {len(models)}")
    
    success_count = 0
    error_count = 0
    
    for model in models:
        source_path = model['path']
        target_path = source_path  # Используем тот же путь
        
        if migrate_model(source_path, target_path, source_conn, target_conn):
            success_count += 1
        else:
            error_count += 1
    
    print("\n" + "="*60)
    print("ИТОГИ МИГРАЦИИ")
    print("="*60)
    print(f"Успешно: {success_count}")
    print(f"Ошибок: {error_count}")
    print(f"Всего: {len(models)}")


if __name__ == "__main__":
    # Пример использования
    migrate_model(
        "users.admin.models.objects",
        "users.admin.models.objects",
        "source",
        "target"
    )

