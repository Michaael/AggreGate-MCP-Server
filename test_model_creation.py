#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Тестовый скрипт для проверки создания моделей и связанных сущностей через MCP AggreGate
Тестирует:
- Создание моделей (contexts)
- Создание переменных в моделях
- Создание функций в моделях
- Создание событий в моделях
- Создание привязок (bindings)
- Другие сущности
"""

import json
import subprocess
import sys
import time
import os
from typing import Dict, Any, Optional, List

class ModelCreationTester:
    def __init__(self, jar_path: str):
        self.jar_path = jar_path
        self.process = None
        self.request_id = 1
        self.model_path = None
        
    def start_server(self):
        """Запуск MCP сервера"""
        print("Запуск MCP сервера...")
        self.process = subprocess.Popen(
            ["java", "-jar", self.jar_path],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            encoding='utf-8',
            errors='replace',  # Заменяем некорректные символы вместо ошибки
            bufsize=1
        )
        time.sleep(1)  # Даем серверу время на запуск
        print("[OK] MCP сервер запущен")
        
    def stop_server(self):
        """Остановка MCP сервера"""
        if self.process:
            self.process.terminate()
            self.process.wait()
            print("[OK] MCP сервер остановлен")
    
    def send_request(self, method: str, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Отправка JSON-RPC запроса"""
        request = {
            "jsonrpc": "2.0",
            "id": self.request_id,
            "method": method
        }
        if params:
            request["params"] = params
        
        self.request_id += 1
        
        request_json = json.dumps(request, ensure_ascii=False)
        print(f"[->] {method}")
        
        self.process.stdin.write(request_json + "\n")
        self.process.stdin.flush()
        
        # Читаем ответ
        response_line = None
        try:
            response_line = self.process.stdout.readline()
            if not response_line:
                raise Exception("Нет ответа от сервера")
            
            response_line = response_line.strip()
            
            # Фильтруем логи - ищем JSON объект в строке
            import re
            # Ищем JSON объект (может быть многострочным)
            json_match = re.search(r'\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}', response_line, re.DOTALL)
            if json_match:
                response_line = json_match.group()
            else:
                # Если не нашли, пробуем найти начало JSON
                json_start = response_line.find('{')
                if json_start > 0:
                    response_line = response_line[json_start:]
                else:
                    # Если вообще нет JSON, пробуем прочитать еще строку
                    response_line2 = self.process.stdout.readline()
                    if response_line2:
                        response_line = response_line2.strip()
                        json_start = response_line.find('{')
                        if json_start > 0:
                            response_line = response_line[json_start:]
            
            # Очищаем от некорректных символов перед парсингом
            try:
                response = json.loads(response_line)
            except json.JSONDecodeError as e:
                # Пробуем найти JSON более агрессивно - ищем последний JSON объект
                json_matches = list(re.finditer(r'\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}', response_line, re.DOTALL))
                if json_matches:
                    # Берем последний найденный JSON (скорее всего это ответ)
                    response = json.loads(json_matches[-1].group())
                else:
                    print(f"  [DEBUG] Не удалось найти JSON в ответе. Первые 500 символов: {response_line[:500]}")
                    raise
        except json.JSONDecodeError as e:
            print(f"  [ERROR] Ошибка парсинга JSON: {e}")
            if response_line:
                # Показываем только ASCII символы для отладки
                safe_line = ''.join(c if ord(c) < 128 else '?' for c in response_line[:200])
                print(f"  [DEBUG] Ответ (первые 200 символов): {safe_line}")
            raise
        except Exception as e:
            print(f"  [ERROR] Ошибка чтения ответа: {e}")
            if response_line:
                safe_line = ''.join(c if ord(c) < 128 else '?' for c in str(response_line)[:200])
                print(f"  [DEBUG] Ответ (первые 200 символов): {safe_line}")
            raise
        
        if "error" in response:
            error = response["error"]
            print(f"  [ERROR] {error.get('message', 'Unknown error')}")
            return {"error": error}
        else:
            result = response.get("result", {})
            if result:
                print(f"  [OK] Успешно")
            return result
    
    def call_tool(self, tool_name: str, arguments: Dict[str, Any]) -> Dict[str, Any]:
        """Вызов инструмента MCP"""
        return self.send_request("tools/call", {
            "name": tool_name,
            "arguments": arguments
        })
    
    def initialize(self) -> bool:
        """Инициализация MCP"""
        print("\n=== Инициализация MCP ===")
        try:
            result = self.send_request("initialize", {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {
                    "name": "model-test-client",
                    "version": "1.0.0"
                }
            })
            return "error" not in result
        except Exception as e:
            print(f"  [ERROR] {e}")
            return False
    
    def check_models_context(self) -> bool:
        """Проверка существования контекста models"""
        print("\n  Проверка контекста models...")
        try:
            # Проверяем существование users.admin.models
            result = self.call_tool("aggregate_get_context", {
                "path": "users.admin.models"
            })
            
            if "error" in result:
                print(f"    [WARN] Контекст users.admin.models не найден, попробуем создать...")
                # Пытаемся создать контекст models
                result = self.call_tool("aggregate_create_context", {
                    "parentPath": "users.admin",
                    "name": "models",
                    "description": "Context for models"
                })
                if "error" in result:
                    print(f"    [FAIL] Не удалось создать контекст models: {result.get('error', {}).get('message', 'Unknown error')}")
                    return False
                print(f"    [OK] Контекст models создан")
            else:
                print(f"    [OK] Контекст models существует")
            return True
        except Exception as e:
            print(f"    [ERROR] {e}")
            return False
    
    def connect(self) -> bool:
        """Подключение к AggreGate серверу"""
        print("\n=== Подключение к AggreGate ===")
        try:
            result = self.call_tool("aggregate_connect", {
                "host": "localhost",
                "port": 6460,
                "username": "admin",
                "password": "admin"
            })
            if "error" in result:
                return False
            print(f"  [OK] Подключено к localhost:6460")
            return True
        except Exception as e:
            print(f"  [ERROR] {e}")
            return False
    
    def login(self) -> bool:
        """Вход в систему"""
        print("\n=== Вход в систему ===")
        try:
            result = self.call_tool("aggregate_login", {})
            if "error" in result:
                return False
            print(f"  [OK] Вход выполнен")
            return True
        except Exception as e:
            print(f"  [ERROR] {e}")
            return False
    
    def check_models_context(self) -> bool:
        """Проверка существования контекста models"""
        print("\n  Проверка контекста models...")
        try:
            # Проверяем существование users.admin.models
            result = self.call_tool("aggregate_get_context", {
                "path": "users.admin.models"
            })
            
            if "error" in result:
                print(f"    [WARN] Контекст users.admin.models не найден, попробуем создать...")
                # Пытаемся создать контекст models
                result = self.call_tool("aggregate_create_context", {
                    "parentPath": "users.admin",
                    "name": "models",
                    "description": "Контекст для моделей"
                })
                if "error" in result:
                    error_msg = result.get('error', {})
                    if isinstance(error_msg, dict):
                        error_msg = error_msg.get('message', str(error_msg))
                    print(f"    [FAIL] Не удалось создать контекст models: {error_msg}")
                    return False
                print(f"    [OK] Контекст models создан")
            else:
                print(f"    [OK] Контекст models существует")
            return True
        except Exception as e:
            print(f"    [ERROR] {e}")
            return False
    
    def test_create_model(self) -> bool:
        """Тест создания модели контекста"""
        print("\n" + "="*60)
        print("ТЕСТ 1: Создание модели контекста")
        print("="*60)
        
        # Сначала проверяем/создаем контекст models
        if not self.check_models_context():
            print("  [FAIL] Не удалось проверить/создать контекст models")
            return False
        
        timestamp = str(int(time.time()))
        model_name = f"test_model_{timestamp}"
        self.model_path = f"users.admin.models.{model_name}"
        
        try:
            print(f"\n  Создание модели: {model_name}")
            result = self.call_tool("aggregate_create_context", {
                "parentPath": "users.admin.models",
                "name": model_name,
                "description": f"Test model for entity creation testing (created {time.strftime('%Y-%m-%d %H:%M:%S')})"
            })
            
            if "error" in result:
                error_msg = result.get('error', {})
                if isinstance(error_msg, dict):
                    error_msg = error_msg.get('message', str(error_msg))
                print(f"  [WARN] Ошибка при создании модели: {error_msg}")
                
                # Пробуем подождать и проверить, может модель все-таки создалась
                print(f"  [INFO] Ожидание инициализации модели...")
                for i in range(5):
                    time.sleep(0.5)
                    check_result = self.call_tool("aggregate_get_context", {
                        "path": self.model_path
                    })
                    if "error" not in check_result:
                        print(f"  [OK] Модель создана (проверка после задержки {i+1})")
                        return True
                
                # Если не получилось, пробуем создать в другом месте
                print(f"  [INFO] Пробуем создать модель в users.admin...")
                self.model_path = f"users.admin.{model_name}"
                result2 = self.call_tool("aggregate_create_context", {
                    "parentPath": "users.admin",
                    "name": model_name,
                    "description": f"Test model for entity creation testing (created {time.strftime('%Y-%m-%d %H:%M:%S')})"
                })
                
                if "error" not in result2:
                    print(f"  [OK] Модель создана в альтернативном месте: {self.model_path}")
                    return True
                else:
                    print(f"  [FAIL] Не удалось создать модель ни в одном месте")
                    return False
            
            print(f"  [OK] Модель создана: {self.model_path}")
            if "path" in result:
                self.model_path = result["path"]  # Используем путь из ответа
            print(f"  [INFO] Описание: {result.get('description', 'N/A')}")
            
            # Даем время на инициализацию модели
            time.sleep(1)
            return True
        except Exception as e:
            print(f"  [ERROR] Исключение: {e}")
            import traceback
            traceback.print_exc()
            return False
    
    def test_create_variables(self) -> bool:
        """Тест создания переменных в модели"""
        print("\n" + "="*60)
        print("ТЕСТ 2: Создание переменных в модели")
        print("="*60)
        
        if not self.model_path:
            print("  [SKIP] Модель не создана")
            return False
        
        variables = [
            {
                "name": "status",
                "format": "<status><S>",
                "description": "Статус модели",
                "group": "Основные",
                "writable": True
            },
            {
                "name": "temperature",
                "format": "<temperature><E>",
                "description": "Температура",
                "group": "Датчики",
                "writable": False
            },
            {
                "name": "pressure",
                "format": "<pressure><E>",
                "description": "Давление",
                "group": "Датчики",
                "writable": False
            },
            {
                "name": "counter",
                "format": "<counter><I>",
                "description": "Счетчик",
                "group": "Основные",
                "writable": True
            }
        ]
        
        success_count = 0
        for var in variables:
            try:
                print(f"\n  Создание переменной: {var['name']}")
                result = self.call_tool("aggregate_create_variable", {
                    "path": self.model_path,
                    "variableName": var["name"],
                    "format": var["format"],
                    "description": var["description"],
                    "group": var["group"],
                    "writable": var["writable"],
                    "readPermissions": "observer",
                    "writePermissions": "manager",
                    "storageMode": 0  # database
                })
                
                if "error" in result:
                    print(f"    [FAIL] {result['error']}")
                else:
                    print(f"    [OK] Переменная создана")
                    success_count += 1
            except Exception as e:
                print(f"    [ERROR] {e}")
        
        print(f"\n  [SUMMARY] Создано переменных: {success_count}/{len(variables)}")
        return success_count == len(variables)
    
    def test_create_functions(self) -> bool:
        """Тест создания функций в модели"""
        print("\n" + "="*60)
        print("ТЕСТ 3: Создание функций в модели")
        print("="*60)
        
        if not self.model_path:
            print("  [SKIP] Модель не создана")
            return False
        
        functions = [
            {
                "name": "calculateSum",
                "description": "Вычисление суммы двух чисел",
                "functionType": 0,  # Java
                "inputFormat": "<arg1><E><D=Argument 1><arg2><E><D=Argument 2>",
                "outputFormat": "<result><E><D=Result>"
            },
            {
                "name": "checkStatus",
                "description": "Проверка статуса",
                "functionType": 1,  # Expression
                "expression": "if (.:status != null) { return .:status; } else { return 'unknown'; }",
                "inputFormat": "",
                "outputFormat": "<result><S><D=Result>"
            },
            {
                "name": "getTemperature",
                "description": "Получение температуры",
                "functionType": 1,  # Expression
                "expression": ".:temperature",
                "inputFormat": "",
                "outputFormat": "<temperature><E><D=Temperature>"
            }
        ]
        
        success_count = 0
        for func in functions:
            try:
                print(f"\n  Создание функции: {func['name']} (тип: {func['functionType']})")
                
                params = {
                    "path": self.model_path,
                    "functionName": func["name"],
                    "description": func["description"],
                    "functionType": func["functionType"]
                }
                
                if func.get("inputFormat"):
                    params["inputFormat"] = func["inputFormat"]
                if func.get("outputFormat"):
                    params["outputFormat"] = func["outputFormat"]
                if func.get("expression"):
                    params["expression"] = func["expression"]
                
                result = self.call_tool("aggregate_create_function", params)
                
                if "error" in result:
                    print(f"    [FAIL] {result['error']}")
                else:
                    print(f"    [OK] Функция создана")
                    success_count += 1
            except Exception as e:
                print(f"    [ERROR] {e}")
        
        print(f"\n  [SUMMARY] Создано функций: {success_count}/{len(functions)}")
        return success_count > 0
    
    def test_create_events(self) -> bool:
        """Тест создания событий в модели"""
        print("\n" + "="*60)
        print("ТЕСТ 4: Создание событий в модели")
        print("="*60)
        
        if not self.model_path:
            print("  [SKIP] Модель не создана")
            return False
        
        events = [
            {
                "name": "statusChanged",
                "description": "Изменение статуса",
                "format": "<oldStatus><S><D=Old Status><newStatus><S><D=New Status>",
                "level": 0,  # INFO
                "group": "События"
            },
            {
                "name": "temperatureAlarm",
                "description": "Тревога по температуре",
                "format": "<temperature><E><D=Temperature><threshold><E><D=Threshold>",
                "level": 1,  # WARNING
                "group": "Тревоги"
            },
            {
                "name": "criticalError",
                "description": "Критическая ошибка",
                "format": "<errorMessage><S><D=Error Message>",
                "level": 2,  # ERROR
                "group": "Ошибки"
            }
        ]
        
        success_count = 0
        for event in events:
            try:
                print(f"\n  Создание события: {event['name']} (уровень: {event['level']})")
                result = self.call_tool("aggregate_create_event", {
                    "path": self.model_path,
                    "eventName": event["name"],
                    "description": event["description"],
                    "format": event["format"],
                    "group": event["group"],
                    "level": event["level"],
                    "permissions": "observer",
                    "firePermissions": "admin",
                    "historyStorageTime": 0
                })
                
                if "error" in result:
                    print(f"    [FAIL] {result['error']}")
                else:
                    print(f"    [OK] Событие создано")
                    success_count += 1
            except Exception as e:
                print(f"    [ERROR] {e}")
        
        print(f"\n  [SUMMARY] Создано событий: {success_count}/{len(events)}")
        return success_count > 0
    
    def test_create_bindings(self) -> bool:
        """Тест создания привязок (bindings)"""
        print("\n" + "="*60)
        print("ТЕСТ 5: Создание привязок (bindings)")
        print("="*60)
        
        if not self.model_path:
            print("  [SKIP] Модель не создана")
            return False
        
        try:
            # Создаем привязки через aggregate_set_variable
            # Формат: target = ".:variableName", expression = "{context:variable}"
            # Для теста используем ссылки на корневой контекст
            
            print("\n  Создание привязок через aggregate_set_variable")
            
            # Формат bindings: target (S), expression (S), onevent (B)
            bindings_data = {
                "recordCount": 2,
                "format": {
                    "minRecords": 0,
                    "maxRecords": 2147483647,
                    "fields": [
                        {"name": "target", "type": "S"},
                        {"name": "expression", "type": "S"},
                        {"name": "onevent", "type": "B"}
                    ]
                },
                "records": [
                    {
                        "target": ".:temperature",
                        "expression": "{users.admin:version}",
                        "onevent": True
                    },
                    {
                        "target": ".:status",
                        "expression": "'active'",
                        "onevent": False
                    }
                ]
            }
            
            result = self.call_tool("aggregate_set_variable", {
                "path": self.model_path,
                "name": "bindings",
                "value": bindings_data
            })
            
            if "error" in result:
                error_msg = result.get('error', {})
                if isinstance(error_msg, dict):
                    error_msg = error_msg.get('message', str(error_msg))
                print(f"    [WARN] {error_msg}")
                print(f"    [INFO] Привязки могут быть настроены через веб-интерфейс или требуют существующих переменных")
                # Это не критично для теста, так как привязки требуют существующих переменных
                return False
            else:
                print(f"    [OK] Привязки созданы")
                return True
        except Exception as e:
            print(f"    [ERROR] {e}")
            import traceback
            traceback.print_exc()
            return False
    
    def test_create_rules(self) -> bool:
        """Тест создания правил через функции Expression"""
        print("\n" + "="*60)
        print("ТЕСТ 6: Создание правил (через функции Expression)")
        print("="*60)
        
        if not self.model_path:
            print("  [SKIP] Модель не создана")
            return False
        
        rules = [
            {
                "name": "checkStatusRule",
                "description": "Правило проверки статуса",
                "functionType": 1,  # Expression
                "expression": "if (.:status != null) { if (.:status == 'active') { .:counter = .:counter + 1; } }",
                "inputFormat": "",
                "outputFormat": ""
            },
            {
                "name": "temperatureCheck",
                "description": "Проверка температуры",
                "functionType": 1,  # Expression
                "expression": "if (.:temperature != null && .:temperature > 100) { .:status = 'warning'; }",
                "inputFormat": "",
                "outputFormat": ""
            }
        ]
        
        success_count = 0
        for rule in rules:
            try:
                print(f"\n  Создание правила: {rule['name']}")
                
                params = {
                    "path": self.model_path,
                    "functionName": rule["name"],
                    "description": rule["description"],
                    "functionType": rule["functionType"],
                    "expression": rule["expression"]
                }
                
                if rule.get("inputFormat"):
                    params["inputFormat"] = rule["inputFormat"]
                if rule.get("outputFormat"):
                    params["outputFormat"] = rule["outputFormat"]
                
                result = self.call_tool("aggregate_create_function", params)
                
                if "error" in result:
                    error_msg = result.get('error', {})
                    if isinstance(error_msg, dict):
                        error_msg = error_msg.get('message', str(error_msg))
                    print(f"    [WARN] {error_msg}")
                else:
                    print(f"    [OK] Правило создано")
                    success_count += 1
            except Exception as e:
                print(f"    [ERROR] {e}")
        
        print(f"\n  [SUMMARY] Создано правил: {success_count}/{len(rules)}")
        return success_count > 0
    
    def test_list_entities(self) -> bool:
        """Тест получения списка созданных сущностей"""
        print("\n" + "="*60)
        print("ТЕСТ 7: Проверка созданных сущностей")
        print("="*60)
        
        if not self.model_path:
            print("  [SKIP] Модель не создана")
            return False
        
        try:
            # Список переменных
            print("\n  Получение списка переменных...")
            result = self.call_tool("aggregate_list_variables", {
                "path": self.model_path
            })
            if "error" not in result:
                variables = result.get("variables", [])
                print(f"    [OK] Найдено переменных: {len(variables)}")
                for var in variables[:5]:  # Показываем первые 5
                    print(f"      - {var.get('name', 'N/A')}")
            else:
                print(f"    [WARN] {result['error']}")
            
            # Список функций
            print("\n  Получение списка функций...")
            result = self.call_tool("aggregate_list_functions", {
                "path": self.model_path
            })
            if "error" not in result:
                functions = result.get("functions", [])
                print(f"    [OK] Найдено функций: {len(functions)}")
                for func in functions[:5]:  # Показываем первые 5
                    print(f"      - {func.get('name', 'N/A')}")
            else:
                print(f"    [WARN] {result['error']}")
            
            return True
        except Exception as e:
            print(f"    [ERROR] {e}")
            return False
    
    def test_call_function(self) -> bool:
        """Тест вызова созданной функции"""
        print("\n" + "="*60)
        print("ТЕСТ 8: Вызов созданной функции")
        print("="*60)
        
        if not self.model_path:
            print("  [SKIP] Модель не создана")
            return False
        
        try:
            # Пытаемся вызвать функцию calculateSum
            print("\n  Вызов функции calculateSum...")
            result = self.call_tool("aggregate_call_function", {
                "path": self.model_path,
                "functionName": "calculateSum",
                "input": {
                    "arg1": 10.5,
                    "arg2": 20.3
                }
            })
            
            if "error" in result:
                print(f"    [WARN] {result['error']}")
                print(f"    [INFO] Это может быть нормально, если функция требует другого формата входных данных")
                return False
            else:
                print(f"    [OK] Функция вызвана успешно")
                print(f"    [INFO] Результат: {result}")
                return True
        except Exception as e:
            print(f"    [ERROR] {e}")
            return False
    
    def test_fire_event(self) -> bool:
        """Тест генерации события"""
        print("\n" + "="*60)
        print("ТЕСТ 9: Генерация события")
        print("="*60)
        
        if not self.model_path:
            print("  [SKIP] Модель не создана")
            return False
        
        try:
            # Пытаемся сгенерировать событие statusChanged
            print("\n  Генерация события statusChanged...")
            result = self.call_tool("aggregate_fire_event", {
                "agentName": "testAgent",  # Может потребоваться агент
                "eventName": "statusChanged",
                "level": "INFO",
                "data": {
                    "oldStatus": "initial",
                    "newStatus": "active"
                }
            })
            
            if "error" in result:
                print(f"    [WARN] {result['error']}")
                print(f"    [INFO] Это может быть нормально, если требуется агент или другой формат")
                return False
            else:
                print(f"    [OK] Событие сгенерировано")
                return True
        except Exception as e:
            print(f"    [ERROR] {e}")
            return False
    
    def run_all_tests(self):
        """Запуск всех тестов"""
        results = {
            "passed": 0,
            "failed": 0,
            "skipped": 0,
            "tests": []
        }
        
        try:
            self.start_server()
            
            # Инициализация
            if not self.initialize():
                print("[ERROR] Инициализация не удалась")
                return results
            
            # Подключение
            if not self.connect():
                print("[ERROR] Подключение не удалось")
                return results
            
            if not self.login():
                print("[ERROR] Вход не удался")
                return results
            
            # Тесты
            tests = [
                ("Создание модели", self.test_create_model),
                ("Создание переменных", self.test_create_variables),
                ("Создание функций", self.test_create_functions),
                ("Создание событий", self.test_create_events),
                ("Создание привязок", self.test_create_bindings),
                ("Создание правил", self.test_create_rules),
                ("Проверка сущностей", self.test_list_entities),
                ("Вызов функции", self.test_call_function),
                ("Генерация события", self.test_fire_event),
            ]
            
            for test_name, test_func in tests:
                try:
                    success = test_func()
                    results["tests"].append({
                        "name": test_name,
                        "status": "passed" if success else "failed"
                    })
                    if success:
                        results["passed"] += 1
                    else:
                        results["failed"] += 1
                except Exception as e:
                    print(f"\n[ERROR] Ошибка в тесте '{test_name}': {e}")
                    results["tests"].append({
                        "name": test_name,
                        "status": "failed"
                    })
                    results["failed"] += 1
            
        except Exception as e:
            print(f"\n[ERROR] Критическая ошибка: {e}")
            import traceback
            traceback.print_exc()
        finally:
            self.stop_server()
        
        return results
    
    def print_summary(self, results: Dict[str, Any]):
        """Вывод итогового отчета"""
        print("\n" + "="*60)
        print("ИТОГОВЫЙ ОТЧЕТ")
        print("="*60)
        print(f"Успешно: {results['passed']}")
        print(f"Провалено: {results['failed']}")
        print(f"Всего: {results['passed'] + results['failed']}")
        print("\nДетали:")
        for test in results["tests"]:
            status_icon = "[OK]" if test["status"] == "passed" else "[FAIL]"
            print(f"  {status_icon} {test['name']}: {test['status']}")
        
        if self.model_path:
            print(f"\n[INFO] Созданная модель: {self.model_path}")
            print(f"[INFO] Модель можно проверить в AggreGate Server")

def main():
    # Путь к JAR файлу
    jar_path = os.path.join("mcp-server", "build", "libs", "aggregate-mcp-server-1.0.0.jar")
    
    if not os.path.exists(jar_path):
        print(f"[ERROR] JAR файл не найден: {jar_path}")
        print("Сначала соберите проект: gradlew build")
        sys.exit(1)
    
    tester = ModelCreationTester(jar_path)
    results = tester.run_all_tests()
    tester.print_summary(results)
    
    # Возвращаем код выхода
    if results["failed"] > 0:
        sys.exit(1)
    else:
        sys.exit(0)

if __name__ == "__main__":
    main()

