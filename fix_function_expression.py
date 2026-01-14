#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Исправление expression функции - удаление и пересоздание с правильным expression
"""
import json
import subprocess
import sys
import os
import time

if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

def fix_function():
    jar_path = os.path.join("mcp-server", "build", "libs", "aggregate-mcp-server-1.0.0.jar")
    if not os.path.exists(jar_path):
        print(f"JAR не найден: {jar_path}")
        return 1
    
    process = subprocess.Popen(
        ["java", "-jar", jar_path],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding='utf-8'
    )
    time.sleep(2)
    
    def send_request(method, params=None, req_id=1):
        if params is None:
            params = {}
        request = {
            "jsonrpc": "2.0",
            "id": req_id,
            "method": method,
            "params": params
        }
        process.stdin.write(json.dumps(request) + "\n")
        process.stdin.flush()
    
    def read_response(expected_id):
        for _ in range(10):
            time.sleep(0.2)
            line = process.stdout.readline()
            if line and line.strip():
                try:
                    response = json.loads(line.strip())
                    if response.get('id') == expected_id:
                        return response
                except:
                    continue
        return None
    
    # Инициализация
    send_request("initialize", {
        "protocolVersion": "2024-11-05",
        "capabilities": {},
        "clientInfo": {"name": "fix-client", "version": "1.0.0"}
    }, 1)
    read_response(1)
    
    # Подключение
    send_request("tools/call", {
        "name": "aggregate_connect",
        "arguments": {
            "host": "localhost",
            "port": 6460,
            "username": "admin",
            "password": "admin"
        }
    }, 2)
    read_response(2)
    time.sleep(1)
    
    send_request("tools/call", {
        "name": "aggregate_login",
        "arguments": {}
    }, 3)
    read_response(3)
    time.sleep(1)
    
    model_path = "users.admin.models.temperature_monitor"
    
    # Удаление функции (если существует)
    print("Удаление функции calculate_average...")
    # В AggreGate функции удаляются через удаление контекста или через специальный API
    # Для простоты просто пересоздадим функцию
    
    # Построение правильного expression
    print("Построение правильного expression...")
    send_request("tools/call", {
        "name": "aggregate_build_expression",
        "arguments": {
            "inputFields": [
                {"name": "value1", "type": "E"},
                {"name": "value2", "type": "E"}
            ],
            "outputFields": [
                {"name": "result", "type": "E"}
            ],
            "formula": "({value1} + {value2}) / 2"
        }
    }, 4)
    
    response = read_response(4)
    if response and 'result' in response:
        result = response['result']
        if isinstance(result, dict) and 'content' in result:
            content = result['content']
            if isinstance(content, list) and len(content) > 0:
                item = content[0]
                if item.get('type') == 'text':
                    build_result = json.loads(item.get('text', ''))
                    expression = build_result.get('expression', '')
                    print(f"Получен expression: {expression}")
                    
                    # Проверяем, есть ли тройные скобки
                    if '<<<' in expression:
                        print("⚠️ Обнаружены тройные скобки в expression!")
                        # Исправляем вручную
                        expression_fixed = expression.replace('<<<', '<<').replace('>>>', '>>')
                        print(f"Исправленный expression: {expression_fixed}")
                        
                        # Создаём функцию с исправленным expression
                        print("Создание функции с исправленным expression...")
                        send_request("tools/call", {
                            "name": "aggregate_create_function",
                            "arguments": {
                                "path": model_path,
                                "functionName": "calculate_average",
                                "functionType": 1,
                                "inputFormat": build_result.get('inputFormat'),
                                "outputFormat": build_result.get('outputFormat'),
                                "expression": expression_fixed,
                                "description": "Вычисление среднего значения двух температур"
                            }
                        }, 5)
                        
                        create_response = read_response(5)
                        if create_response:
                            print("Функция создана/обновлена")
                        else:
                            print("Ошибка при создании функции")
                    else:
                        print("✓ Expression правильный (без тройных скобок)")
    
    process.terminate()
    process.wait()
    return 0

if __name__ == "__main__":
    sys.exit(fix_function())
