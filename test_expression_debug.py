#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Отладка проблемы с тройными скобками в expression
"""
import json
import subprocess
import sys
import os
import time

if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

def test_expression():
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
    
    # Инициализация
    send_request("initialize", {
        "protocolVersion": "2024-11-05",
        "capabilities": {},
        "clientInfo": {"name": "debug-client", "version": "1.0.0"}
    }, 1)
    read_response(1)
    
    # Вызов build_expression
    print("Вызов aggregate_build_expression...")
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
    }, 2)
    
    response = read_response(2)
    if response and 'result' in response:
        result = response['result']
        print(f"\nСтруктура ответа:")
        print(f"  result type: {type(result)}")
        if isinstance(result, dict):
            print(f"  result keys: {list(result.keys())}")
            if 'content' in result:
                content = result['content']
                print(f"  content type: {type(content)}")
                if isinstance(content, list) and len(content) > 0:
                    item = content[0]
                    print(f"  content[0] type: {type(item)}")
                    print(f"  content[0] keys: {list(item.keys()) if isinstance(item, dict) else 'N/A'}")
                    if isinstance(item, dict) and 'text' in item:
                        text = item['text']
                        print(f"\n  Raw text (первые 200 символов):")
                        print(f"    {text[:200]}")
                        
                        # Парсим JSON
                        try:
                            parsed = json.loads(text)
                            print(f"\n  Parsed JSON:")
                            print(f"    type: {type(parsed)}")
                            if isinstance(parsed, dict):
                                print(f"    keys: {list(parsed.keys())}")
                                if 'expression' in parsed:
                                    expr = parsed['expression']
                                    print(f"\n  Expression:")
                                    print(f"    value: {expr}")
                                    print(f"    length: {len(expr)}")
                                    print(f"    contains '<<<': {'<<<' in expr}")
                                    print(f"    contains '<<': {'<<' in expr}")
                                    print(f"    contains '>>>': {'>>>' in expr}")
                                    print(f"    contains '>>': {'>>' in expr}")
                                    
                                    # Проверяем, сколько раз встречается <<
                                    count_triple = expr.count('<<<')
                                    count_double = expr.count('<<') - count_triple * 3
                                    print(f"\n  Статистика скобок:")
                                    print(f"    '<<<' встречается: {count_triple} раз")
                                    print(f"    '<<' (не тройные) встречается: {count_double} раз")
                        except Exception as e:
                            print(f"  Ошибка парсинга JSON: {e}")
    
    process.terminate()
    process.wait()

if __name__ == "__main__":
    test_expression()
