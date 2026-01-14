#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Очистка тестовых контекстов перед запуском тестов
"""
import json
import subprocess
import sys
import os
import time

if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

class McpCleanupClient:
    def __init__(self, jar_path):
        self.jar_path = jar_path
        self.process = None
        self.request_id = 0
        
    def start(self):
        """Запуск MCP сервера"""
        print("[CLEANUP] Запуск MCP сервера...")
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
        
        # Инициализация
        init_request = {
            "jsonrpc": "2.0",
            "id": self._next_id(),
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "cleanup-client", "version": "1.0.0"}
            }
        }
        self._send_request(init_request)
        self._read_response()
        print("[CLEANUP] ✓ MCP сервер инициализирован")
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
        """Вызов MCP инструмента"""
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
        """Остановка сервера"""
        if self.process:
            self.process.terminate()
            self.process.wait()

def cleanup_test_contexts():
    """Очистка тестовых контекстов"""
    jar_path = os.path.join("mcp-server", "build", "libs", "aggregate-mcp-server-1.0.0.jar")
    if not os.path.exists(jar_path):
        print(f"[ERROR] JAR не найден: {jar_path}")
        return 1
    
    client = McpCleanupClient(jar_path)
    
    try:
        if not client.start():
            return 1
        
        # Подключение к серверу
        print("\n[CLEANUP] Подключение к AggreGate серверу...")
        connect_result = client.call_tool("aggregate_connect", {
            "host": "localhost",
            "port": 6460,
            "username": "admin",
            "password": "admin"
        })
        
        if not connect_result or not connect_result.get("success"):
            print(f"[ERROR] Не удалось подключиться: {connect_result.get('error', 'Unknown') if connect_result else 'No response'}")
            return 1
        
        print("[CLEANUP] ✓ Подключено")
        time.sleep(1)
        
        client.call_tool("aggregate_login")
        time.sleep(1)
        
        # Контекст для очистки
        test_context = "users.admin.models.temperature_monitor"
        
        print(f"\n[CLEANUP] Очистка контекста: {test_context}")
        
        # Шаг 1: Удаление функций
        print("  [1] Удаление функций...")
        try:
            # Попробуем удалить функцию calculate_average
            # (В AggreGate функции удаляются через удаление контекста или через API)
            # Для простоты просто удалим весь контекст
            print("    (функции будут удалены вместе с контекстом)")
        except Exception as e:
            print(f"    ⚠ Ошибка: {e}")
        
        # Шаг 2: Удаление переменных
        print("  [2] Удаление переменных...")
        variables_to_delete = ["current_temp", "min_temp", "max_temp"]
        deleted_vars = 0
        for var_name in variables_to_delete:
            try:
                # В AggreGate переменные удаляются через удаление контекста
                # или через специальный API, но для теста проще удалить контекст
                print(f"    (переменная {var_name} будет удалена вместе с контекстом)")
            except Exception as e:
                print(f"    ⚠ Ошибка при удалении {var_name}: {e}")
        
        # Шаг 3: Удаление контекста (это удалит всё внутри)
        print("  [3] Удаление контекста...")
        try:
            delete_result = client.call_tool("aggregate_delete_context", {
                "path": test_context
            })
            
            if delete_result and delete_result.get("success"):
                print(f"    ✓ Контекст {test_context} удалён")
            else:
                error = delete_result.get("error", "Неизвестная ошибка") if delete_result else "Нет ответа"
                if "not found" in error.lower() or "does not exist" in error.lower():
                    print(f"    ✓ Контекст {test_context} не существует (уже удалён)")
                else:
                    print(f"    ⚠ Не удалось удалить контекст: {error}")
        except Exception as e:
            print(f"    ⚠ Ошибка при удалении контекста: {e}")
        
        print("\n[CLEANUP] ✓ Очистка завершена")
        return 0
        
    except Exception as e:
        print(f"\n[ERROR] Критическая ошибка: {e}")
        import traceback
        traceback.print_exc()
        return 1
    finally:
        client.stop()

if __name__ == "__main__":
    sys.exit(cleanup_test_contexts())
