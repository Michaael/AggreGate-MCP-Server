#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Упрощенный тест создания модели через MCP AggreGate
"""

import json
import subprocess
import sys
import time
import os
import re
from typing import Dict, Any, Optional

class SimpleModelTester:
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
            errors='replace',
            bufsize=1
        )
        time.sleep(2)
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
        
        # Читаем ответ, пропуская логи
        max_lines = 20
        lines_read = 0
        response_json = None
        
        while lines_read < max_lines:
            line = self.process.stdout.readline()
            if not line:
                break
            line = line.strip()
            if not line:
                continue
            
            # Ищем JSON (объект или массив) в строке
            # Пробуем объект
            json_match = re.search(r'\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}', line, re.DOTALL)
            if json_match:
                try:
                    response_json = json.loads(json_match.group())
                    break
                except:
                    pass
            
            # Пробуем массив
            json_match = re.search(r'\[[^\[\]]*(?:\[[^\[\]]*\][^\[\]]*)*\]', line, re.DOTALL)
            if json_match:
                try:
                    response_json = json.loads(json_match.group())
                    break
                except:
                    pass
            
            lines_read += 1
        
        if response_json is None:
            # Пробуем прочитать еще раз, может быть ответ пришел позже
            time.sleep(0.1)
            line = self.process.stdout.readline()
            if line:
                line = line.strip()
                json_match = re.search(r'\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}|\[[^\[\]]*(?:\[[^\[\]]*\][^\[\]]*)*\]', line, re.DOTALL)
                if json_match:
                    try:
                        response_json = json.loads(json_match.group())
                    except:
                        pass
        
        if response_json is None:
            raise Exception("Не удалось получить JSON ответ после " + str(max_lines) + " попыток")
        
        if "error" in response_json:
            error = response_json["error"]
            print(f"  [ERROR] {error.get('message', 'Unknown error')}")
            return {"error": error}
        else:
            result = response_json.get("result", {})
            if result:
                print(f"  [OK] Успешно")
            return result
    
    def call_tool(self, tool_name: str, arguments: Dict[str, Any]) -> Dict[str, Any]:
        """Вызов инструмента MCP"""
        return self.send_request("tools/call", {
            "name": tool_name,
            "arguments": arguments
        })
    
    def test(self):
        """Основной тест"""
        try:
            self.start_server()
            
            # Инициализация
            print("\n=== Инициализация ===")
            self.send_request("initialize", {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "test", "version": "1.0"}
            })
            
            # Подключение
            print("\n=== Подключение ===")
            self.call_tool("aggregate_connect", {
                "host": "localhost",
                "port": 6460,
                "username": "admin",
                "password": "admin"
            })
            
            # Вход
            print("\n=== Вход ===")
            self.call_tool("aggregate_login", {})
            
            # Проверка корневого контекста
            print("\n=== Проверка корневого контекста ===")
            result = self.call_tool("aggregate_get_context", {
                "path": "users.admin"
            })
            if "error" in result:
                print(f"  [ERROR] Контекст users.admin не найден: {result['error']}")
                return False
            
            # Проверка контекста models
            print("\n=== Проверка контекста models ===")
            result = self.call_tool("aggregate_get_context", {
                "path": "users.admin.models"
            })
            if "error" in result:
                print(f"  [INFO] Контекст models не найден: {result['error']}")
                print("  [INFO] Пробуем создать models...")
                result = self.call_tool("aggregate_create_context", {
                    "parentPath": "users.admin",
                    "name": "models",
                    "description": "Models context"
                })
                if "error" in result:
                    print(f"  [WARN] Не удалось создать models: {result['error']}")
                    print("  [INFO] Пробуем создать модель напрямую в users.admin...")
                    model_parent = "users.admin"
                else:
                    print("  [OK] Контекст models создан")
                    time.sleep(1)  # Даем время на инициализацию
                    model_parent = "users.admin.models"
            else:
                print("  [OK] Контекст models существует")
                model_parent = "users.admin.models"
            
            # Создание модели
            print(f"\n=== Создание модели в {model_parent} ===")
            model_name = f"test_model_{int(time.time())}"
            result = self.call_tool("aggregate_create_context", {
                "parentPath": model_parent,
                "name": model_name,
                "description": "Test model"
            })
            
            model_path = f"{model_parent}.{model_name}"
            
            # Ждем инициализации
            print("\n=== Ожидание инициализации модели ===")
            time.sleep(2)
            
            # Проверка модели
            print(f"\n=== Проверка модели: {model_path} ===")
            result = self.call_tool("aggregate_get_context", {
                "path": model_path
            })
            
            if "error" in result:
                print(f"  [FAIL] Модель не найдена: {result['error']}")
                return False
            
            print(f"  [OK] Модель найдена: {model_path}")
            
            # Создание переменной
            print("\n=== Создание переменной ===")
            result = self.call_tool("aggregate_create_variable", {
                "path": model_path,
                "variableName": "testVar",
                "format": "<testVar><S>",
                "description": "Test variable",
                "writable": True
            })
            
            if "error" not in result:
                print("  [OK] Переменная создана")
            else:
                print(f"  [WARN] {result['error']}")
            
            # Создание функции
            print("\n=== Создание функции ===")
            result = self.call_tool("aggregate_create_function", {
                "path": model_path,
                "functionName": "testFunc",
                "functionType": 1,  # Expression
                "description": "Test function",
                "expression": "return 'test';"
            })
            
            if "error" not in result:
                print("  [OK] Функция создана")
            else:
                print(f"  [WARN] {result['error']}")
            
            # Создание события
            print("\n=== Создание события ===")
            result = self.call_tool("aggregate_create_event", {
                "path": model_path,
                "eventName": "testEvent",
                "description": "Test event",
                "level": 0
            })
            
            if "error" not in result:
                print("  [OK] Событие создано")
            else:
                print(f"  [WARN] {result['error']}")
            
            print(f"\n[INFO] Модель: {model_path}")
            return True
            
        except Exception as e:
            print(f"\n[ERROR] {e}")
            import traceback
            traceback.print_exc()
            return False
        finally:
            self.stop_server()

def main():
    jar_path = os.path.join("mcp-server", "build", "libs", "aggregate-mcp-server-1.0.0.jar")
    
    if not os.path.exists(jar_path):
        print(f"[ERROR] JAR не найден: {jar_path}")
        sys.exit(1)
    
    tester = SimpleModelTester(jar_path)
    success = tester.test()
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()

