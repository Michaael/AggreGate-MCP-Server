#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Удаление и пересоздание функции с правильным expression
"""
import json
import subprocess
import sys
import os
import time

if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

def delete_and_recreate():
    jar_path = os.path.join("mcp-server", "build", "libs", "aggregate-mcp-server-1.0.0.jar")
    
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
    
    def call_tool(tool_name, params=None, req_id=1):
        send_request("tools/call", {
            "name": tool_name,
            "arguments": params or {}
        }, req_id)
        response = read_response(req_id)
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
        return None
    
    # Инициализация
    send_request("initialize", {
        "protocolVersion": "2024-11-05",
        "capabilities": {},
        "clientInfo": {"name": "fix-client", "version": "1.0.0"}
    }, 1)
    read_response(1)
    
    # Подключение
    call_tool("aggregate_connect", {
        "host": "localhost",
        "port": 6460,
        "username": "admin",
        "password": "admin"
    }, 2)
    time.sleep(1)
    
    call_tool("aggregate_login", {}, 3)
    time.sleep(1)
    
    model_path = "users.admin.models.temperature_monitor"
    
    # Удаление контекста (это удалит функцию)
    print("Удаление контекста для удаления функции...")
    call_tool("aggregate_delete_context", {"path": model_path}, 4)
    time.sleep(1)
    
    # Создание контекста заново
    print("Создание контекста заново...")
    call_tool("aggregate_get_or_create_context", {
        "path": model_path,
        "description": "Система мониторинга температуры"
    }, 5)
    time.sleep(1)
    
    # Построение правильного expression
    print("Построение правильного expression...")
    build_result = call_tool("aggregate_build_expression", {
        "inputFields": [
            {"name": "value1", "type": "E"},
            {"name": "value2", "type": "E"}
        ],
        "outputFields": [
            {"name": "result", "type": "E"}
        ],
        "formula": "({value1} + {value2}) / 2"
    }, 6)
    
    if build_result and build_result.get("success"):
        expression = build_result.get("expression", "")
        print(f"Получен expression: {expression}")
        
        # Исправляем тройные скобки (если есть)
        if '<<<' in expression:
            expression = expression.replace('<<<', '<<').replace('>>>', '>>')
            print(f"Исправленный expression: {expression}")
        
        # Создаём функцию с исправленным expression
        print("Создание функции с исправленным expression...")
        create_result = call_tool("aggregate_create_function", {
            "path": model_path,
            "functionName": "calculate_average",
            "functionType": 1,
            "inputFormat": build_result.get("inputFormat"),
            "outputFormat": build_result.get("outputFormat"),
            "expression": expression,
            "description": "Вычисление среднего значения двух температур"
        }, 7)
        
        if create_result and create_result.get("success"):
            print("✓ Функция создана успешно")
        else:
            error = create_result.get("error", "Неизвестная ошибка") if create_result else "Нет ответа"
            print(f"✗ Ошибка при создании функции: {error}")
    
    process.terminate()
    process.wait()
    return 0

if __name__ == "__main__":
    sys.exit(delete_and_recreate())
