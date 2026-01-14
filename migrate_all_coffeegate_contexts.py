#!/usr/bin/env python3
"""
Автоматический перенос всех контекстов CoffeeGate
Использует MCP инструменты для переноса моделей, устройств, дашбордов и других контекстов
"""

print("=" * 70)
print("МИГРАЦИЯ ВСЕХ КОНТЕКСТОВ COFFEEGATE")
print("=" * 70)
print()
print("Этот скрипт предназначен для использования через MCP инструменты.")
print("Для выполнения миграции используйте MCP инструменты напрямую.")
print()
print("Порядок выполнения:")
print("1. Подключиться к исходному серверу (62.109.25.124)")
print("2. Подключиться к целевому серверу (localhost)")
print("3. Для каждой модели:")
print("   - Получить информацию о модели (тип, описание)")
print("   - Создать модель на localhost")
print("   - Получить список переменных, функций, событий")
print("   - Создать все переменные с правильными форматами")
print("   - Импортировать данные переменных")
print("   - Создать все функции")
print("   - Создать все события")
print("   - Настроить привязки (bindings)")
print()
print("Список моделей для переноса:")
models = [
    "cmSettings",  # Частично перенесена
    "cmMachineTypes",
    "cmSites",
    "cmStructure",
    "cmMachinesManager",
    "cmMachinesPresenter",
    "cmCoffeeMachinesItems",
    "cmDrCoffee",
    "cmWMF1500",
    "cmF2Plus",
    "WMFFunctions",
    "cmObjectsManager",
    "cmObjectsPresenter",
    "cmObjectsItems",
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
    "cmIncidents",
    "cmIncidentsManager",
    "cmUsersManager",
    "cmUsersProperties",
    "cmConnectors",
    "cmConnectors_agents",
    "cmMailIntegrationManager",
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
print(f"Всего моделей: {len(models)}")
print()
for i, model in enumerate(models, 1):
    print(f"{i:2d}. {model}")
print()
print("=" * 70)
print("Для выполнения миграции используйте MCP инструменты.")
print("Рекомендуется переносить модели группами по функциональности.")
