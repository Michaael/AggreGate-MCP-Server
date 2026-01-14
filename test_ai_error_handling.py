#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Тест автоматического исправления ошибок параметров функций ИИ
"""
import json
import subprocess
import sys
import os
import time

if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

class McpTestClient:
    def __init__(self, jar_path):
        self.jar_path = jar_path
        self.process = None
        self.request_id = 0
        
    def start(self):
        print("[TEST] Запуск MCP сервера...")
        self.process = subprocess.Popen(
            ["java", "-jar", self.jar_path],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            encoding='utf-8',
            bufsize=0
        )
        time.sleep(2)
        
        init_request = {
            "jsonrpc": "2.0",
            "id": self._next_id(),
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "test-client", "version": "1.0.0"}
            }
        }
        self._send_request(init_request)
        self._read_response()
        print("[TEST] ✓ MCP сервер инициализирован")
        return True
    
    def _next_id(self):
        self.request_id += 1
        return self.request_id
    
    def _send_request(self, request):
        request_json = json.dumps(request, ensure_ascii=False) + "\n"
        self.process.stdin.write(request_json)
        self.process.stdin.flush()
    
    def _read_response(self, expected_id=None):
        if expected_id is None:
            expected_id = self.request_id
        
        for _ in range(10):
            time.sleep(0.2)
            line = self.process.stdout.readline()
            if line and line.strip():
                try:
                    response = json.loads(line.strip())
                    if response.get('id') == expected_id:
                        return response
                except:
                    continue
        return None
    
    def call_tool(self, tool_name, params=None):
        if params is None:
            params = {}
        
        request = {
            "jsonrpc": "2.0",
            "id": self._next_id(),
            "method": "tools/call",
            "params": {
                "name": tool_name,
                "arguments": params
            }
        }
        
        self._send_request(request)
        response = self._read_response()
        
        if response and 'result' in response:
            result = response['result']
            if isinstance(result, dict) and 'content' in result:
                content = result['content']
                if isinstance(content, list) and len(content) > 0:
                    item = content[0]
                    if item.get('type') == 'text':
                        try:
                            return json.loads(item.get('text', ''))
                        except:
                            return {"text": item.get('text', '')}
            return result
        elif response and 'error' in response:
            error = response['error']
            return {"success": False, "error": error.get('message', 'Unknown error')}
        
        return None
    
    def stop(self):
        if self.process:
            self.process.terminate()
            self.process.wait()

def test_error_handling():
    jar_path = os.path.join("mcp-server", "build", "libs", "aggregate-mcp-server-1.0.0.jar")
    if not os.path.exists(jar_path):
        print(f"[ERROR] JAR не найден: {jar_path}")
        return 1
    
    client = McpTestClient(jar_path)
    
    try:
        if not client.start():
            return 1
        
        # Подключение
        print("\n[1] Подключение к серверу...")
        connect_result = client.call_tool("aggregate_connect", {
            "host": "localhost",
            "port": 6460,
            "username": "admin",
            "password": "admin"
        })
        if not connect_result or not connect_result.get("success"):
            print(f"[ERROR] Не удалось подключиться")
            return 1
        print("[✓] Подключено")
        time.sleep(1)
        
        client.call_tool("aggregate_login")
        time.sleep(1)
        
        model_path = "users.admin.models.temperature_monitor"
        
        # Создаём контекст, если его нет
        print("\n[2] Создание/получение контекста...")
        context_result = client.call_tool("aggregate_get_or_create_context", {
            "path": model_path,
            "description": "Тестовая модель для проверки обработки ошибок"
        })
        time.sleep(1)
        
        # Получаем информацию о функции
        print("\n[3] Получение информации о функции calculate_average...")
        func_info = client.call_tool("aggregate_get_function", {
            "path": model_path,
            "functionName": "calculate_average"
        })
        
        if not func_info or not func_info.get("success"):
            print("[!] Функция не найдена, создаём её...")
            # Создаём функцию
            build_result = client.call_tool("aggregate_build_expression", {
                "inputFields": [
                    {"name": "value1", "type": "E"},
                    {"name": "value2", "type": "E"}
                ],
                "outputFields": [
                    {"name": "result", "type": "E"}
                ],
                "formula": "({value1} + {value2}) / 2"
            })
            
            if build_result and build_result.get("success"):
                expression = build_result.get("expression", "")
                # Исправляем тройные скобки, если есть
                if '<<<' in expression:
                    expression = expression.replace('<<<', '<<').replace('>>>', '>>')
                
                create_result = client.call_tool("aggregate_create_function", {
                    "path": model_path,
                    "functionName": "calculate_average",
                    "functionType": 1,
                    "inputFormat": build_result.get("inputFormat"),
                    "outputFormat": build_result.get("outputFormat"),
                    "expression": expression,
                    "description": "Вычисление среднего значения"
                })
                
                if create_result and create_result.get("success"):
                    print("[✓] Функция создана")
                else:
                    error = create_result.get("error", "Unknown") if create_result else "No response"
                    print(f"[!] Ошибка создания функции: {error}")
                    # Продолжаем, возможно функция уже существует
                
                time.sleep(2)
                
                # Получаем информацию о созданной функции
                func_info = client.call_tool("aggregate_get_function", {
                    "path": model_path,
                    "functionName": "calculate_average"
                })
                
                if func_info:
                    print(f"[DEBUG] func_info keys: {list(func_info.keys())}")
                    print(f"[DEBUG] func_info success: {func_info.get('success')}")
        
        if not func_info:
            print("[ERROR] Не удалось получить информацию о функции (нет ответа)")
            return 1
        
        # Проверяем, есть ли информация о функции (может быть без поля success)
        if func_info.get("name") != "calculate_average" and not func_info.get("success"):
            print(f"[ERROR] Не удалось получить информацию о функции. Ответ: {json.dumps(func_info, indent=2)[:200]}...")
            return 1
        
        print("[✓] Функция найдена")
        if func_info.get("inputFormat"):
            print(f"  inputFormat: {func_info.get('inputFormat')}")
        
        # Тестируем функцию с неправильными параметрами (только value1)
        print("\n[4] Тестирование функции с неправильными параметрами (только value1)...")
        test_result = client.call_tool("aggregate_test_function", {
            "path": model_path,
            "functionName": "calculate_average",
            "parameters": {
                "value1": 10.0
            }
        })
        
        if test_result and not test_result.get("success"):
            error_msg = test_result.get("error", "")
            print(f"[✗] Ошибка (ожидаемо): {error_msg[:100]}...")
            
            # Анализируем ошибку
            print("\n[5] Анализ ошибки через aggregate_explain_error...")
            explain_result = client.call_tool("aggregate_explain_error", {
                "message": error_msg,
                "toolName": "aggregate_test_function"
            })
            
            if explain_result:
                print(f"[✓] Категория: {explain_result.get('category')}")
                print(f"  Объяснение: {explain_result.get('explanation', '')[:100]}...")
                if explain_result.get("missingField"):
                    print(f"  Отсутствующее поле: {explain_result.get('missingField')}")
                if explain_result.get("recommendation"):
                    print(f"  Рекомендация: {explain_result.get('recommendation')[:150]}...")
            
            # Исправляем параметры автоматически
            print("\n[6] Автоматическое исправление параметров через aggregate_fix_function_parameters...")
            fix_result = client.call_tool("aggregate_fix_function_parameters", {
                "path": model_path,
                "functionName": "calculate_average",
                "errorMessage": error_msg,
                "providedParameters": {
                    "value1": 10.0
                }
            })
            
            if fix_result:
                print("[✓] Параметры исправлены")
                if fix_result.get("missingField"):
                    print(f"  Отсутствовало поле: {fix_result.get('missingField')}")
                if fix_result.get("correctedParameters"):
                    corrected = fix_result.get("correctedParameters")
                    print(f"  Исправленные параметры: {json.dumps(corrected, indent=2)}")
                
                # Тестируем с исправленными параметрами
                print("\n[7] Тестирование функции с исправленными параметрами...")
                test_result2 = client.call_tool("aggregate_test_function", {
                    "path": model_path,
                    "functionName": "calculate_average",
                    "parameters": corrected
                })
                
                if test_result2 and test_result2.get("success"):
                    print("[✓] Тест успешен!")
                    if test_result2.get("result"):
                        result = test_result2.get("result")
                        print(f"  Результат: {json.dumps(result, indent=2)}")
                    return 0
                else:
                    error2 = test_result2.get("error", "Unknown") if test_result2 else "No response"
                    print(f"[✗] Тест всё ещё провален: {error2[:100]}...")
                    return 1
            else:
                print("[✗] Не удалось исправить параметры")
                return 1
        else:
            print("[✗] Ожидалась ошибка, но тест прошёл успешно")
            return 1
        
    except Exception as e:
        print(f"\n[ERROR] Критическая ошибка: {e}")
        import traceback
        traceback.print_exc()
        return 1
    finally:
        client.stop()

if __name__ == "__main__":
    sys.exit(test_error_handling())
