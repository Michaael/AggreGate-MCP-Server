#!/usr/bin/env python3
"""
Тестовый скрипт для проверки export/import функциональности
Требуется запущенный MCP сервер и подключение к AggreGate серверам
"""

import json
import os
import sys

# Примеры тестовых запросов для export/import

def test_export_models():
    """Тест экспорта контекста models"""
    return {
        "path": "users.admin.models",
        "actionName": "export",
        "filePath": "C:/migration/export/models_test.prs",
        "connectionKey": "source"
    }

def test_export_objects():
    """Тест экспорта модели objects"""
    return {
        "path": "users.admin.models.objects",
        "actionName": "export",
        "filePath": "C:/migration/export/objects.prs",
        "connectionKey": "source"
    }

def test_import_objects():
    """Тест импорта модели objects"""
    return {
        "path": "users.admin.models",
        "actionName": "import",
        "filePath": "C:/migration/export/objects.prs",
        "connectionKey": "target"
    }

def print_test_request(test_name, request):
    """Вывести тестовый запрос в формате JSON"""
    print(f"\n{'='*60}")
    print(f"Тест: {test_name}")
    print(f"{'='*60}")
    print("Запрос к aggregate_execute_action:")
    print(json.dumps(request, indent=2, ensure_ascii=False))
    print(f"{'='*60}\n")

def main():
    """Основная функция"""
    print("Тестовые запросы для export/import функциональности")
    print("=" * 60)
    print("\nДля выполнения тестов:")
    print("1. Убедитесь, что MCP сервер запущен")
    print("2. Подключитесь к исходному серверу (connectionKey: 'source')")
    print("3. Подключитесь к целевому серверу (connectionKey: 'target')")
    print("4. Создайте директорию: C:/migration/export/")
    print("5. Выполните тесты через Cursor IDE или MCP клиент\n")
    
    # Тест 1: Экспорт models
    print_test_request("Экспорт контекста models", test_export_models())
    
    # Тест 2: Экспорт objects
    print_test_request("Экспорт модели objects", test_export_objects())
    
    # Тест 3: Импорт objects
    print_test_request("Импорт модели objects", test_import_objects())
    
    print("\nИнструкции:")
    print("1. Скопируйте JSON запрос")
    print("2. Используйте инструмент aggregate_execute_action в Cursor IDE")
    print("3. Проверьте логи MCP сервера для отладки")
    print("4. Проверьте создание файлов .prs в указанной директории")
    print("\nОжидаемые результаты:")
    print("- Действие выполнилось успешно (success: true)")
    print("- Файл .prs создан в указанной директории")
    print("- В логах видно установку пути к файлу")

if __name__ == "__main__":
    main()

