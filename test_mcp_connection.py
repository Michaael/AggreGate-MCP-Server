#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Тест подключения к AggreGate через MCP сервер
"""
import json
import subprocess
import sys
import os

# Настройка кодировки для Windows
if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

class McpClient:
    def __init__(self):
        self.process = None
        self.request_id = 0
        
    def start(self):
        """Запуск MCP сервера"""
        print("[INIT] Запуск MCP сервера...")
        jar_path = os.path.join("mcp-server", "build", "libs", "aggregate-mcp-server-1.0.0.jar")
        if not os.path.exists(jar_path):
            print(f"[ERROR] JAR файл не найден: {jar_path}")
            print("[INFO] Выполните сборку: gradlew :mcp-server:build")
            return False
            
        self.process = subprocess.Popen(
            ["java", "-jar", jar_path],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            bufsize=0,
            text=True,
            encoding='utf-8'
        )
        print("[OK] MCP сервер запущен")
        return True
        
    def send_request(self, method, params=None):
        """Отправка JSON-RPC запроса"""
        if params is None:
            params = {}
            
        self.request_id += 1
        request = {
            "jsonrpc": "2.0",
            "id": self.request_id,
            "method": method,
            "params": params
        }
        
        request_str = json.dumps(request, ensure_ascii=False) + "\n"
        self.process.stdin.write(request_str)
        self.process.stdin.flush()
        
        # Чтение ответа
        response_line = self.process.stdout.readline()
        if response_line:
            try:
                response = json.loads(response_line.strip())
                # MCP сервер возвращает ответы в формате с полем "content"
                if "content" in response and isinstance(response["content"], list):
                    for item in response["content"]:
                        if item.get("type") == "text":
                            text_content = item.get("text", "")
                            try:
                                return json.loads(text_content)
                            except:
                                return {"text": text_content}
                return response
            except json.JSONDecodeError as e:
                print(f"[ERROR] Ошибка парсинга ответа: {e}")
                print(f"[ERROR] Ответ: {response_line}")
                return None
        return None
    
    def call_tool(self, tool_name, parameters=None):
        """Вызов MCP инструмента"""
        if parameters is None:
            parameters = {}
            
        response = self.send_request("tools/call", {
            "name": tool_name,
            "arguments": parameters
        })
        
        if not response:
            print(f"[ERROR] Нет ответа от MCP сервера для {tool_name}")
            return None
            
        # MCP сервер может возвращать ответы в разных форматах
        if "result" in response:
            result = response["result"]
            if isinstance(result, dict) and "content" in result:
                for item in result.get("content", []):
                    if item.get("type") == "text":
                        try:
                            return json.loads(item.get("text", ""))
                        except:
                            pass
            return result
        elif "content" in response:
            for item in response.get("content", []):
                if item.get("type") == "text":
                    try:
                        return json.loads(item.get("text", ""))
                    except:
                        return {"text": item.get("text", "")}
        elif "error" in response:
            error = response["error"]
            error_msg = error.get('message', 'Unknown error')
            print(f"[ERROR] Ошибка вызова {tool_name}: {error_msg}")
            return {"success": False, "error": error_msg}
        return None
    
    def stop(self):
        """Остановка MCP сервера"""
        if self.process:
            self.process.terminate()
            self.process.wait()
            print("[OK] MCP сервер остановлен")

def main():
    client = McpClient()
    
    try:
        if not client.start():
            return
        
        # Инициализация
        print("\n[INIT] Инициализация...")
        init_response = client.send_request("initialize", {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {
                "name": "test-client",
                "version": "1.0.0"
            }
        })
        if "error" in init_response:
            print(f"[ERROR] Ошибка инициализации: {init_response['error']}")
            return
        print("[OK] Инициализация успешна")
        
        # Подключение
        print("\n[CONNECT] Подключение к серверу AggreGate...")
        connect_result = client.call_tool("aggregate_connect", {
            "host": "localhost",
            "port": 6460,
            "username": "admin",
            "password": "admin"
        })
        if not connect_result:
            print("[ERROR] Не удалось получить ответ от MCP сервера")
            return
        if not connect_result.get("success"):
            error_msg = connect_result.get("error", connect_result.get("message", "Unknown error"))
            print(f"[ERROR] Не удалось подключиться: {error_msg}")
            return
        print("[OK] Подключено")
        connection_key = connect_result.get("connectionKey", "default")
        
        # Вход
        print("\n[LOGIN] Вход в систему...")
        login_result = client.call_tool("aggregate_login", {
            "connectionKey": connection_key
        })
        if not login_result or not login_result.get("success"):
            error_msg = login_result.get("error", "Unknown error") if login_result else "No response"
            print(f"[ERROR] Не удалось войти: {error_msg}")
            return
        print("[OK] Вход выполнен")
        
        # Получение списка контекстов
        print("\n[CONTEXTS] Получение списка всех контекстов...")
        contexts_result = client.call_tool("aggregate_list_contexts", {
            "mask": "*",
            "connectionKey": connection_key
        })
        if not contexts_result:
            print("[ERROR] Не удалось получить список контекстов")
            return
        
        # Проверяем формат ответа - может быть список или объект
        if isinstance(contexts_result, list):
            contexts = contexts_result
        elif isinstance(contexts_result, dict):
            if not contexts_result.get("success"):
                error_msg = contexts_result.get("error", "Unknown error")
                print(f"[ERROR] Ошибка получения контекстов: {error_msg}")
                return
            contexts = contexts_result.get("contexts", [])
        else:
            print(f"[ERROR] Неожиданный формат ответа: {type(contexts_result)}")
            return
        print(f"\n[OK] Найдено контекстов: {len(contexts)}")
        print("\n" + "="*80)
        print("СПИСОК КОНТЕКСТОВ:")
        print("="*80)
        
        for ctx in contexts:
            path = ctx.get("path", "unknown")
            ctx_type = ctx.get("type", "unknown")
            print(f"  {path:<50} [{ctx_type}]")
        
        print("="*80)
        
        # Отключение
        print("\n[DISCONNECT] Отключение от сервера...")
        disconnect_result = client.call_tool("aggregate_disconnect", {
            "connectionKey": connection_key
        })
        if disconnect_result and disconnect_result.get("success"):
            print("[OK] Отключено")
        
    except Exception as e:
        print(f"[ERROR] Критическая ошибка: {e}")
        import traceback
        traceback.print_exc()
    finally:
        client.stop()

if __name__ == "__main__":
    main()

