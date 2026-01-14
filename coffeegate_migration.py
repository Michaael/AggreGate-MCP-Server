#!/usr/bin/env python3
"""
Скрипт для переноса приложения CoffeeGate с сервера 62.109.25.124 на localhost
"""

import json
import sys
from typing import Dict, List, Set, Any

# Импорт MCP клиента (предполагается, что есть способ вызова MCP инструментов)
# Для этого примера используем прямые вызовы через MCP

class CoffeeGateMigrator:
    def __init__(self):
        self.source_host = "62.109.25.124"
        self.target_host = "localhost"
        self.exported_data = {
            "models": [],
            "devices": [],
            "dashboards": [],
            "widgets": [],
            "reports": [],
            "alerts": [],
            "applications": [],
            "devgroups": []
        }
        
        # Список префиксов для идентификации CoffeeGate контекстов
        self.cm_prefixes = [
            "cm",  # coffee machine
            "CG_",  # CoffeeGate
            "webCM",  # web Coffee Machine
            "reportFlushing",  # отчеты по промывкам
            "reportOnDrinks",  # отчеты по напиткам
            "reportDowntime",  # отчеты по простоям
            "reportOnCounters",  # отчеты по счетчикам
            "cmReport",  # отчеты КМ
            "cmDispatcherLog",  # журнал диспетчера
            "cmDowntime",  # простои
            "cmServiceCounters",  # сервисные счетчики
            "application",  # заявки
            "listIncident",  # инциденты
            "serviceIngineer",  # сервисный инженер
            "Coffeemachines"  # группа устройств
        ]
        
    def is_coffeegate_context(self, name: str, path: str) -> bool:
        """Проверяет, относится ли контекст к CoffeeGate"""
        name_lower = name.lower()
        path_lower = path.lower()
        
        # Проверка по префиксам
        for prefix in self.cm_prefixes:
            if name.startswith(prefix) or path.startswith(prefix):
                return True
        
        # Проверка на ключевые слова
        keywords = ["coffee", "coffeemachine", "coffeemachines", "drink", "flushing", 
                   "incident", "dispatcher", "engineer", "заявк", "кофемашин"]
        for keyword in keywords:
            if keyword in name_lower or keyword in path_lower:
                return True
        
        return False
    
    def export_context_structure(self, path: str, context_type: str) -> Dict[str, Any]:
        """Экспортирует структуру контекста"""
        # Здесь будет вызов MCP инструментов для получения данных
        # Пока возвращаем структуру
        return {
            "path": path,
            "type": context_type,
            "exported": False
        }
    
    def get_all_coffeegate_contexts(self) -> Dict[str, List[str]]:
        """Получает список всех контекстов CoffeeGate"""
        contexts = {
            "models": [],
            "devices": [],
            "dashboards": [],
            "widgets": [],
            "reports": [],
            "alerts": [],
            "applications": [],
            "devgroups": []
        }
        
        # Список будет заполнен через MCP вызовы
        return contexts
    
    def export_model(self, model_path: str) -> Dict[str, Any]:
        """Экспортирует модель со всеми переменными, функциями, событиями и привязками"""
        model_data = {
            "path": model_path,
            "variables": [],
            "functions": [],
            "events": [],
            "bindings": None,
            "childInfo": None
        }
        
        # Здесь будут вызовы MCP для получения данных
        return model_data
    
    def export_device(self, device_path: str) -> Dict[str, Any]:
        """Экспортирует устройство"""
        device_data = {
            "path": device_path,
            "variables": [],
            "functions": [],
            "events": []
        }
        
        return device_data
    
    def export_dashboard(self, dashboard_path: str) -> Dict[str, Any]:
        """Экспортирует дашборд"""
        dashboard_data = {
            "path": dashboard_path,
            "template": None,
            "elements": []
        }
        
        return dashboard_data
    
    def optimize_structure(self, exported_data: Dict[str, Any]) -> Dict[str, Any]:
        """Оптимизирует структуру приложения"""
        optimized = {
            "models": [],
            "devices": [],
            "dashboards": [],
            "widgets": [],
            "reports": [],
            "alerts": [],
            "applications": [],
            "devgroups": []
        }
        
        # Удаление дубликатов (контексты с суффиксами _copy, _copy1, _old, _v2_old)
        seen_names = set()
        
        for context_type in ["models", "dashboards", "widgets"]:
            for item in exported_data.get(context_type, []):
                name = item.get("name", "")
                # Пропускаем дубликаты
                base_name = name.replace("_copy", "").replace("_copy1", "").replace("_old", "").replace("_v2_old", "").replace("_copy_copy", "")
                
                if base_name not in seen_names or not any(suffix in name for suffix in ["_copy", "_old", "_v2_old"]):
                    if base_name not in seen_names:
                        seen_names.add(base_name)
                        optimized[context_type].append(item)
        
        return optimized
    
    def import_to_localhost(self, optimized_data: Dict[str, Any]):
        """Импортирует оптимизированные данные на localhost"""
        # Здесь будут вызовы MCP для создания контекстов на localhost
        pass

if __name__ == "__main__":
    print("CoffeeGate Migration Script")
    print("=" * 50)
    print("Этот скрипт будет экспортировать и переносить приложение CoffeeGate")
    print("с сервера 62.109.25.124 на localhost")
    print()
    print("Для выполнения миграции используйте MCP инструменты напрямую")
