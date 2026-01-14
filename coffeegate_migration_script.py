#!/usr/bin/env python3
"""
Автоматический скрипт для миграции CoffeeGate с сервера 62.109.25.124 на localhost
Использует MCP инструменты для экспорта и импорта
"""

import json
import sys
from typing import Dict, List, Any, Optional

# Список всех моделей CoffeeGate для миграции
COFFEEGATE_MODELS = [
    # Базовые справочники
    "cmMain",
    "cmSettings", 
    "cmMachineTypes",
    "cmSites",
    "cmStructure",
    
    # Управление кофемашинами
    "cmMachinesManager",
    "cmMachinesPresenter", 
    "cmCoffeeMachinesItems",
    
    # Управление объектами
    "cmObjectsManager",
    "cmObjectsPresenter",
    "cmObjectsItems",
    
    # Отчеты
    "cmReportsManager",
    "cmReportsPresenter",
    "cmMainReports",
    "cmReportFlushing",
    "cmReportSellsDetailed",
    "cmReportCleansDetailed",
    "cmReportMonitoring",
    "cmReportSettings",
    "cmReportServiceCounters",
    "cmIdleReport",
    
    # Инциденты
    "cmIncidents",
    "cmIncidentsManager",
    
    # Пользователи
    "cmUsersManager",
    "cmUsersProperties",
    
    # Интерфейсы кофемашин
    "cmDrCoffee",
    "cmWMF1500",
    "cmF2Plus",
    "WMFFunctions",
    
    # Функциональность
    "cmCommonFunctions",
    "cmCommonPresenter",
    "cmEngineerPresenter",
    "cmDictionariesPresenter",
    "cmService",
    "cmUtils",
    
    # Интеграции
    "cmMailIntegrationManager",
    "cmDbCollection",
    "cmFileLoader",
    "cmFileOperations",
    
    # Коннекторы и агенты
    "cmConnectors",
    "cmConnectors_agents",
    "cmAgentLtManager",
    "cmAgentLtAgents",
    
    # Специфичные
    "cmCustomer1Manager",
    "cmCustomer1Reports",
    
    # Аналитика
    "cmSystemLog",
    "cmSystemAnalysis",
    "cmCaching",
    
    # Вспомогательные
    "cmContextCreator",
    "cmPrepareClean",
    "cm_reestr_query"
]

# Модели для удаления (дубликаты, мусор)
MODELS_TO_SKIP = [
    "cmCoffeeMachinesItems_rep",  # Дубликат
    "cmLogAnalyze_copy",  # Дубликат
    "cmGarbageFunctions",  # Мусор
    "cmReportSells",  # Старая версия (есть Detailed)
    "cmReportCleans",  # Старая версия (есть Detailed)
]

def export_model_structure(model_name: str, source_connection: str = None) -> Dict[str, Any]:
    """
    Экспортирует структуру модели с сервера-источника
    Возвращает словарь с данными модели
    """
    # Здесь будут вызовы MCP инструментов для экспорта
    # Пока возвращаем структуру
    return {
        "name": model_name,
        "path": f"users.admin.models.{model_name}",
        "variables": [],
        "functions": [],
        "events": [],
        "bindings": None,
        "childInfo": None
    }

def import_model_to_localhost(model_data: Dict[str, Any], target_connection: str = "localhost"):
    """
    Импортирует модель на localhost
    """
    # Здесь будут вызовы MCP инструментов для импорта
    pass

def optimize_model_list(models: List[str]) -> List[str]:
    """
    Оптимизирует список моделей, удаляя дубликаты и мусор
    """
    optimized = []
    seen_base_names = set()
    
    for model in models:
        if model in MODELS_TO_SKIP:
            continue
        
        # Удаляем суффиксы для проверки дубликатов
        base_name = model.replace("_rep", "").replace("_copy", "").replace("_old", "")
        
        if base_name not in seen_base_names:
            seen_base_names.add(base_name)
            optimized.append(model)
    
    return optimized

def main():
    """
    Главная функция миграции
    """
    print("=" * 80)
    print("МИГРАЦИЯ COFFEEGATE")
    print("=" * 80)
    print()
    
    # Оптимизация списка моделей
    optimized_models = optimize_model_list(COFFEEGATE_MODELS)
    
    print(f"Всего моделей для миграции: {len(optimized_models)}")
    print(f"Моделей пропущено (дубликаты/мусор): {len(MODELS_TO_SKIP)}")
    print()
    
    print("Список моделей для миграции:")
    for i, model in enumerate(optimized_models, 1):
        print(f"  {i}. {model}")
    
    print()
    print("=" * 80)
    print()
    print("Для выполнения миграции используйте MCP инструменты:")
    print("1. aggregate_get_context - получить структуру")
    print("2. aggregate_list_variables - получить переменные")
    print("3. aggregate_list_functions - получить функции")
    print("4. aggregate_list_events - получить события")
    print("5. aggregate_get_variable - получить данные переменных")
    print("6. aggregate_create_context - создать на localhost")
    print("7. aggregate_create_variable - создать переменные")
    print("8. aggregate_create_function - создать функции")
    print("9. aggregate_create_event - создать события")
    print("10. aggregate_set_variable - установить данные и привязки")
    print()
    print("Рекомендуется выполнять миграцию поэтапно:")
    print("1. Базовые справочники (cmMain, cmSettings)")
    print("2. Управление (Machines, Objects)")
    print("3. Отчеты")
    print("4. Остальные компоненты")

if __name__ == "__main__":
    main()
