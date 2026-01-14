#!/usr/bin/env python3
"""
Скрипт для автоматического переноса всех контекстов CoffeeGate
с сервера 62.109.25.124 на localhost
"""

import json
import sys
from typing import Dict, List, Any, Optional

# Список всех моделей CoffeeGate для переноса
COFFEEGATE_MODELS = [
    # Справочники и настройки
    "cmMain",  # Уже перенесена
    "cmSettings",
    "cmMachineTypes",
    "cmSites",
    "cmStructure",
    
    # Управление кофемашинами
    "cmMachinesManager",
    "cmMachinesPresenter",
    "cmCoffeeMachinesItems",
    "cmDrCoffee",
    "cmWMF1500",
    "cmF2Plus",
    "WMFFunctions",
    
    # Управление объектами
    "cmObjectsManager",
    "cmObjectsPresenter",
    "cmObjectsItems",
    
    # Отчеты
    "cmReportsManager",
    "cmReportsPresenter",
    "cmMainReports",
    "cmReportCleans",
    "cmReportCleansDetailed",
    "cmReportSells",
    "cmReportSellsDetailed",
    "cmReportMonitoring",
    "cmReportServiceCounters",
    "cmIdleReport",
    "cmReportSettings",
    
    # Инциденты и пользователи
    "cmIncidents",
    "cmIncidentsManager",
    "cmUsersManager",
    "cmUsersProperties",
    
    # Интеграции и коннекторы
    "cmConnectors",
    "cmConnectors_agents",
    "cmMailIntegrationManager",
    
    # Дополнительные модели
    "cmCommonFunctions",
    "cmCommonPresenter",
    "cmDictionariesPresenter",
    "cmEngineerPresenter",
    "cmAgentLtManager",
    "cmAgentLtAgents",
    "cmCaching",
    "cmContextCreator",
    "cmCustomer1Manager",
    "cmCustomer1Reports",
    "cmDbCollection",
    "cmFileLoader",
    "cmFileOperations",
    "cmLogAnalyze",
    "cmPrepareClean",
    "cmService",
    "cmSystemAnalysis",
    "cmSystemLog",
    "cmUtils",
    "cm_reestr_query",
]

# Модели, которые нужно пропустить (дубликаты, тестовые)
SKIP_MODELS = [
    "cmCoffeeMachinesItems_rep",  # Копия
    "cmLogAnalyze_copy",  # Копия
    "cmGarbageFunctions",  # Мусор
    "deleteMe",  # Для удаления
]

def print_status(message: str, status: str = "INFO"):
    """Выводит статусное сообщение"""
    symbols = {
        "INFO": "ℹ",
        "SUCCESS": "✓",
        "ERROR": "✗",
        "WARNING": "⚠"
    }
    print(f"[{symbols.get(status, '•')}] {message}")

def main():
    print("=" * 70)
    print("МИГРАЦИЯ ВСЕХ КОНТЕКСТОВ COFFEEGATE")
    print("=" * 70)
    print()
    print("Этот скрипт создан для автоматизации переноса контекстов.")
    print("Для выполнения миграции используйте MCP инструменты напрямую.")
    print()
    print(f"Всего моделей для переноса: {len(COFFEEGATE_MODELS)}")
    print()
    print("Порядок переноса:")
    print("1. Справочники и настройки")
    print("2. Управление кофемашинами")
    print("3. Управление объектами")
    print("4. Отчеты")
    print("5. Инциденты и пользователи")
    print("6. Интеграции и коннекторы")
    print("7. Дополнительные модели")
    print()
    print("Для каждой модели необходимо:")
    print("- Создать контекст (определить тип модели)")
    print("- Создать все переменные")
    print("- Создать все функции")
    print("- Создать все события")
    print("- Настроить привязки (bindings)")
    print("- Импортировать данные")
    print()
    print("=" * 70)

if __name__ == "__main__":
    main()
