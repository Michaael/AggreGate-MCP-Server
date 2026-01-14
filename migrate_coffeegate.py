#!/usr/bin/env python3
"""
Скрипт для переноса приложения CoffeeGate с сервера 62.109.25.124 на localhost
Использует MCP инструменты для экспорта и импорта
"""

import json
from typing import Dict, List, Set, Any, Optional

# Список всех контекстов CoffeeGate, найденных на сервере
COFFEEGATE_MODELS = [
    "cmMain", "cmSettings", "cmService", "cmMachinesManager", "cmMachinesPresenter",
    "cmCoffeeMachinesItems", "cmObjectsManager", "cmObjectsPresenter", "cmObjectsItems",
    "cmReportsManager", "cmReportsPresenter", "cmMainReports", "cmReportFlushing",
    "cmReportSells", "cmReportSellsDetailed", "cmReportCleans", "cmReportCleansDetailed",
    "cmReportMonitoring", "cmReportSettings", "cmReportServiceCounters", "cmIdleReport",
    "cmIncidents", "cmIncidentsManager", "cmUsersManager", "cmUsersProperties",
    "cmCommonFunctions", "cmCommonPresenter", "cmEngineerPresenter", "cmDictionariesPresenter",
    "cmConnectors", "cmConnectors_agents", "cmAgentLtManager", "cmAgentLtAgents",
    "cmCustomer1Manager", "cmCustomer1Reports", "cmDrCoffee", "cmWMF1500", "cmF2Plus",
    "cmMachineTypes", "cmFileLoader", "cmFileOperations", "cmDbCollection",
    "cmMailIntegrationManager", "cmSystemLog", "cmSystemAnalysis", "cmCaching",
    "cmContextCreator", "cmPrepareClean", "cmUtils", "cmGarbageFunctions",
    "cmLogAnalyze", "cmSites", "cmStructure", "cm_reestr_query"
]

COFFEEGATE_DEVICES = [
    "WMF_K031920_17807", "drCoffeeNative_220625007", "drCoffeeTest_220625007"
]

COFFEEGATE_DASHBOARDS = [
    "application", "application_v2", "webCMSummary", "webCMSummary_v2",
    "webCMNanagement", "webCMLog", "webCMAdvertising", "webCMpasswordList",
    "webCmCard", "webMonitoring", "webMonitoring_v2", "webObjectCard",
    "webObjectCard_v2", "reportFlushing", "reportFlushing_v2", "reportOnDrinks",
    "reportOnDdrinks", "reportOnDdrinks_v2", "reportDowntime", "reportIdles_v2",
    "reportOnCounters", "reportOnCounters_v2", "reportMonitioring_v2",
    "reportOnDispetcherLog", "listIncident", "serviceIngineer",
    "web_request_modal_createIncident", "web_request_modal_editIncident",
    "web_request_modal_closedIncident", "web_request_modal_createComment",
    "web_request_modal_editObjectInfo", "web_request_modal_modalInService",
    "web_request_modal_modalInExtService", "web_monitoring_detailInfo_vncScreen"
]

COFFEEGATE_APPLICATIONS = [
    "CG_transfer_backend", "CG_transfer_frontend", "CG_transfer_tirazh"
]

COFFEEGATE_ALERTS = [
    "cmIncidentCreatedAlert", "cmIncidentsOutOfTime"
]

COFFEEGATE_REPORTS = [
    "cmReportFlushing", "cmReportOnDrinks", "cmDispatcherLogReport",
    "cmDowntimeReport_incidents", "cmDowntimeReport_report", "cmServiceCountersReport"
]

COFFEEGATE_WIDGETS = [
    "cmEngineer", "AppendCm", "asr", "buildsWindow", "searchWidget"
]

COFFEEGATE_DEVGROUPS = [
    "Coffeemachines"
]

def print_migration_plan():
    """Выводит план миграции"""
    print("=" * 80)
    print("ПЛАН МИГРАЦИИ COFFEEGATE")
    print("=" * 80)
    print()
    print(f"Модели: {len(COFFEEGATE_MODELS)}")
    print(f"Устройства: {len(COFFEEGATE_DEVICES)}")
    print(f"Дашборды: {len(COFFEEGATE_DASHBOARDS)}")
    print(f"Приложения: {len(COFFEEGATE_APPLICATIONS)}")
    print(f"Тревоги: {len(COFFEEGATE_ALERTS)}")
    print(f"Отчеты: {len(COFFEEGATE_REPORTS)}")
    print(f"Виджеты: {len(COFFEEGATE_WIDGETS)}")
    print(f"Группы устройств: {len(COFFEEGATE_DEVGROUPS)}")
    print()
    print("ОПТИМИЗАЦИЯ:")
    print("- Удаление дубликатов (контексты с суффиксами _copy, _old, _v2_old)")
    print("- Объединение похожих моделей")
    print("- Упрощение структуры привязок")
    print("- Очистка неиспользуемых контекстов")
    print()
    print("=" * 80)

if __name__ == "__main__":
    print_migration_plan()
    print()
    print("Для выполнения миграции используйте MCP инструменты:")
    print("1. aggregate_get_context - получить структуру контекста")
    print("2. aggregate_list_variables - получить переменные")
    print("3. aggregate_list_functions - получить функции")
    print("4. aggregate_list_events - получить события")
    print("5. aggregate_get_variable - получить данные переменных")
    print("6. aggregate_create_context - создать контекст на localhost")
    print("7. aggregate_create_variable - создать переменные")
    print("8. aggregate_create_function - создать функции")
    print("9. aggregate_create_event - создать события")
    print("10. aggregate_set_variable - установить привязки и данные")
