#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Тестовый скрипт для проверки всех функций MCP сервера AggreGate
"""

import json
import subprocess
import sys
import time
import os
from typing import Dict, Any, Optional, List

class McpTester:
    def __init__(self, jar_path: str):
        self.jar_path = jar_path
        self.process = None
        self.request_id = 1
        
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
        print(f"[->] Отправка: {method}")
        
        self.process.stdin.write(request_json + "\n")
        self.process.stdin.flush()
        
        # Читаем ответ с обработкой ошибок кодировки
        response_line = None
        try:
            response_line = self.process.stdout.readline()
            if not response_line:
                raise Exception("Нет ответа от сервера")
            
            # Если это bytes, декодируем с обработкой ошибок
            if isinstance(response_line, bytes):
                # Пробуем UTF-8 с заменой невалидных символов
                try:
                    response_line = response_line.decode('utf-8', errors='strict')
                except UnicodeDecodeError:
                    # Если не получается, используем замену
                    response_line = response_line.decode('utf-8', errors='replace')
            
            # Удаляем невалидные символы, которые могут мешать парсингу JSON
            response_line = response_line.strip()
            
            # Пробуем найти начало JSON (может быть мусор перед ним)
            json_start = response_line.find('{')
            if json_start > 0:
                response_line = response_line[json_start:]
            
            response = json.loads(response_line)
        except UnicodeDecodeError as e:
            # Если проблема с кодировкой, пробуем прочитать как bytes и декодировать с заменой
            print(f"  [WARN] Проблема с кодировкой, пробуем исправить...")
            try:
                # Читаем как bytes и декодируем с заменой
                if response_line is None:
                    raise Exception("Не удалось прочитать ответ")
                if isinstance(response_line, bytes):
                    response_line = response_line.decode('utf-8', errors='replace')
                else:
                    response_line = response_line.encode('latin1', errors='replace').decode('utf-8', errors='replace')
                response = json.loads(response_line.strip())
            except Exception as e2:
                raise Exception(f"Ошибка декодирования UTF-8: {e}, попытка исправления: {e2}")
        except json.JSONDecodeError as e:
            print(f"  [WARN] Проблема с парсингом JSON: {e}")
            # Пробуем найти JSON в строке
            try:
                if response_line is None:
                    raise e
                # Ищем первую { и последнюю }
                start = response_line.find('{')
                end = response_line.rfind('}')
                if start >= 0 and end > start:
                    response_line = response_line[start:end+1]
                    response = json.loads(response_line)
                else:
                    raise e
            except Exception as e2:
                print(f"  [WARN] Строка ответа (первые 200 символов): {response_line[:200] if response_line else 'None'}")
                raise Exception(f"Ошибка парсинга JSON: {e}, попытка исправления: {e2}")
        
        if "error" in response:
            error = response["error"]
            print(f"  [ERROR] Ошибка: {error.get('message', 'Unknown error')}")
            return {"error": error}
        else:
            print(f"  [OK] Успешно")
            return response.get("result", {})
    
    def test_initialize(self) -> bool:
        """Тест инициализации"""
        print("\n=== Тест: initialize ===")
        try:
            result = self.send_request("initialize", {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {
                    "name": "test-client",
                    "version": "1.0.0"
                }
            })
            return "error" not in result
        except Exception as e:
            print(f"  [ERROR] Исключение: {e}")
            return False
    
    def test_tools_list(self) -> List[str]:
        """Тест получения списка инструментов"""
        print("\n=== Тест: tools/list ===")
        try:
            result = self.send_request("tools/list")
            if "error" in result:
                return []
            tools = result.get("tools", [])
            tool_names = [tool.get("name", "") for tool in tools]
            print(f"  Найдено инструментов: {len(tool_names)}")
            return tool_names
        except Exception as e:
            print(f"  [ERROR] Исключение: {e}")
            return []
    
    def test_tool_call(self, tool_name: str, arguments: Dict[str, Any]) -> bool:
        """Тест вызова инструмента"""
        print(f"\n=== Тест: {tool_name} ===")
        try:
            result = self.send_request("tools/call", {
                "name": tool_name,
                "arguments": arguments
            })
            if "error" in result:
                error = result["error"]
                print(f"  [ERROR] Ошибка: {error.get('message', 'Unknown error')}")
                return False
            return True
        except Exception as e:
            print(f"  [ERROR] Исключение: {e}")
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
            if not self.test_initialize():
                print("[ERROR] Инициализация не удалась, пропускаем остальные тесты")
                return results
            
            # Получаем список всех инструментов
            tool_names = self.test_tools_list()
            print(f"\nВсего инструментов для тестирования: {len(tool_names)}")
            
            # Тестируем функции подключения
            print("\n" + "="*60)
            print("ТЕСТИРОВАНИЕ ФУНКЦИЙ ПОДКЛЮЧЕНИЯ")
            print("="*60)
            
            # aggregate_connect
            if "aggregate_connect" in tool_names:
                success = self.test_tool_call("aggregate_connect", {
                    "host": "localhost",
                    "port": 6460,
                    "username": "admin",
                    "password": "admin"
                })
                results["tests"].append({"name": "aggregate_connect", "status": "passed" if success else "failed"})
                if success:
                    results["passed"] += 1
                else:
                    results["failed"] += 1
            else:
                results["skipped"] += 1
            
            # aggregate_login
            if "aggregate_login" in tool_names:
                success = self.test_tool_call("aggregate_login", {})
                results["tests"].append({"name": "aggregate_login", "status": "passed" if success else "failed"})
                if success:
                    results["passed"] += 1
                else:
                    results["failed"] += 1
            else:
                results["skipped"] += 1
            
            # Тестируем функции работы с контекстами
            print("\n" + "="*60)
            print("ТЕСТИРОВАНИЕ ФУНКЦИЙ РАБОТЫ С КОНТЕКСТАМИ")
            print("="*60)
            
            context_tools = [
                "aggregate_get_context",
                "aggregate_list_contexts",
                "aggregate_create_context",
                "aggregate_delete_context"
            ]
            
            for tool in context_tools:
                if tool in tool_names:
                    if tool == "aggregate_get_context":
                        success = self.test_tool_call(tool, {"path": "users.admin"})
                    elif tool == "aggregate_list_contexts":
                        success = self.test_tool_call(tool, {"mask": "users.*"})
                    elif tool == "aggregate_create_context":
                        # Создаем тестовый контекст
                        success = self.test_tool_call(tool, {
                            "parentPath": "users.admin",
                            "name": "test_context_" + str(int(time.time()))
                        })
                    else:
                        # delete_context - пропускаем, так как нужен существующий контекст
                        print(f"\n=== Тест: {tool} ===")
                        print("  [SKIP] Пропущен (требует существующий контекст)")
                        success = None
                    
                    if success is not None:
                        results["tests"].append({"name": tool, "status": "passed" if success else "failed"})
                        if success:
                            results["passed"] += 1
                        else:
                            results["failed"] += 1
                    else:
                        results["skipped"] += 1
                else:
                    results["skipped"] += 1
            
            # Тестируем функции работы с переменными
            print("\n" + "="*60)
            print("ТЕСТИРОВАНИЕ ФУНКЦИЙ РАБОТЫ С ПЕРЕМЕННЫМИ")
            print("="*60)
            
            variable_tools = [
                "aggregate_list_variables",
                "aggregate_get_variable",
                "aggregate_create_variable",
                "aggregate_set_variable",
                "aggregate_set_variable_field"
            ]
            
            test_var_name = "test_var_" + str(int(time.time()))
            
            for tool in variable_tools:
                if tool in tool_names:
                    if tool == "aggregate_list_variables":
                        success = self.test_tool_call(tool, {"path": "users.admin"})
                    elif tool == "aggregate_get_variable":
                        success = self.test_tool_call(tool, {
                            "path": "users.admin",
                            "name": "name"
                        })
                    elif tool == "aggregate_create_variable":
                        success = self.test_tool_call(tool, {
                            "path": "users.admin",
                            "variableName": test_var_name,
                            "format": "<value><S>",
                            "description": "Test variable"
                        })
                    elif tool == "aggregate_set_variable":
                        success = self.test_tool_call(tool, {
                            "path": "users.admin",
                            "name": test_var_name,
                            "value": {"value": "test_value"}
                        })
                    elif tool == "aggregate_set_variable_field":
                        success = self.test_tool_call(tool, {
                            "path": "users.admin",
                            "variableName": test_var_name,
                            "fieldName": "value",
                            "value": "updated_value"
                        })
                    else:
                        success = False
                    
                    results["tests"].append({"name": tool, "status": "passed" if success else "failed"})
                    if success:
                        results["passed"] += 1
                    else:
                        results["failed"] += 1
                else:
                    results["skipped"] += 1
            
            # Тестируем функции работы с функциями
            print("\n" + "="*60)
            print("ТЕСТИРОВАНИЕ ФУНКЦИЙ РАБОТЫ С ФУНКЦИЯМИ")
            print("="*60)
            
            function_tools = [
                "aggregate_list_functions",
                "aggregate_call_function",
                "aggregate_create_function"
            ]
            
            for tool in function_tools:
                if tool in tool_names:
                    if tool == "aggregate_list_functions":
                        success = self.test_tool_call(tool, {"path": "users.admin"})
                    elif tool == "aggregate_call_function":
                        # Пытаемся вызвать существующую функцию
                        success = self.test_tool_call(tool, {
                            "path": "users.admin",
                            "functionName": "getContextInfo"
                        })
                    elif tool == "aggregate_create_function":
                        # Создаем тестовую функцию
                        success = self.test_tool_call(tool, {
                            "path": "users.admin",
                            "functionName": "test_function_" + str(int(time.time())),
                            "description": "Test function",
                            "functionType": 0  # Java type
                        })
                    else:
                        success = False
                    
                    results["tests"].append({"name": tool, "status": "passed" if success else "failed"})
                    if success:
                        results["passed"] += 1
                    else:
                        results["failed"] += 1
                else:
                    results["skipped"] += 1
            
            # Тестируем функции работы с пользователями
            print("\n" + "="*60)
            print("ТЕСТИРОВАНИЕ ФУНКЦИЙ РАБОТЫ С ПОЛЬЗОВАТЕЛЯМИ")
            print("="*60)
            
            user_tools = [
                "aggregate_list_users",
                "aggregate_create_user",
                "aggregate_update_user",
                "aggregate_delete_user"
            ]
            
            test_username = "test_user_" + str(int(time.time()))
            
            for tool in user_tools:
                if tool in tool_names:
                    if tool == "aggregate_list_users":
                        success = self.test_tool_call(tool, {})
                    elif tool == "aggregate_create_user":
                        success = self.test_tool_call(tool, {
                            "username": test_username,
                            "password": "test123"
                        })
                    elif tool == "aggregate_update_user":
                        success = self.test_tool_call(tool, {
                            "username": test_username,
                            "email": "test@example.com"
                        })
                    elif tool == "aggregate_delete_user":
                        success = self.test_tool_call(tool, {
                            "username": test_username
                        })
                    else:
                        success = False
                    
                    results["tests"].append({"name": tool, "status": "passed" if success else "failed"})
                    if success:
                        results["passed"] += 1
                    else:
                        results["failed"] += 1
                else:
                    results["skipped"] += 1
            
            # Тестируем остальные функции
            print("\n" + "="*60)
            print("ТЕСТИРОВАНИЕ ОСТАЛЬНЫХ ФУНКЦИЙ")
            print("="*60)
            
            other_tools = [
                "aggregate_list_devices",
                "aggregate_create_device",
                "aggregate_get_device_status",
                "aggregate_create_event",
                "aggregate_fire_event",
                "aggregate_execute_action",
                "aggregate_create_agent",
                "aggregate_get_agent_status",
                "aggregate_create_widget",
                "aggregate_set_widget_template",
                "aggregate_create_dashboard",
                "aggregate_add_dashboard_element"
            ]
            
            for tool in other_tools:
                if tool in tool_names:
                    # Для большинства этих функций нужны специфические параметры
                    # Просто проверяем, что они доступны
                    print(f"\n=== Тест: {tool} ===")
                    print("  [SKIP] Пропущен (требует специфические параметры)")
                    results["skipped"] += 1
                else:
                    results["skipped"] += 1
            
            # aggregate_disconnect
            if "aggregate_disconnect" in tool_names:
                print("\n=== Тест: aggregate_disconnect ===")
                success = self.test_tool_call("aggregate_disconnect", {})
                results["tests"].append({"name": "aggregate_disconnect", "status": "passed" if success else "failed"})
                if success:
                    results["passed"] += 1
                else:
                    results["failed"] += 1
            else:
                results["skipped"] += 1
            
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
        print(f"Пропущено: {results['skipped']}")
        print(f"Всего: {results['passed'] + results['failed'] + results['skipped']}")
        print("\nДетали:")
        for test in results["tests"]:
            status_icon = "[OK]" if test["status"] == "passed" else "[FAIL]"
            print(f"  {status_icon} {test['name']}: {test['status']}")

def main():
    # Путь к JAR файлу
    jar_path = os.path.join("mcp-server", "build", "libs", "aggregate-mcp-server-1.0.0.jar")
    
    if not os.path.exists(jar_path):
        print(f"[ERROR] JAR файл не найден: {jar_path}")
        print("Сначала соберите проект: gradlew build")
        sys.exit(1)
    
    tester = McpTester(jar_path)
    results = tester.run_all_tests()
    tester.print_summary(results)
    
    # Возвращаем код выхода в зависимости от результатов
    if results["failed"] > 0:
        sys.exit(1)
    else:
        sys.exit(0)

if __name__ == "__main__":
    main()

