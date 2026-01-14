#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Упрощённый тест новых MCP инструментов с детальным выводом
"""
import json
import subprocess
import sys
import os
import time

if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

def test_tool(jar_path, tool_name, params=None):
    """Тест одного инструмента"""
    print(f"\n{'='*60}")
    print(f"TEST: {tool_name}")
    print('='*60)
    
    process = subprocess.Popen(
        ["java", "-jar", jar_path],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding='utf-8'
    )
    
    time.sleep(2)
    
    # Инициализация
    init_request = {
        "jsonrpc": "2.0",
        "id": 1,
        "method": "initialize",
        "params": {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {"name": "test", "version": "1.0"}
        }
    }
    process.stdin.write(json.dumps(init_request) + "\n")
    process.stdin.flush()
    
    # Читаем ответ на инициализацию (id=1)
    init_response = process.stdout.readline()
    print(f"Init response (id=1): {init_response[:100]}...")
    
    # Вызов инструмента
    if params is None:
        params = {}
    
    tool_request = {
        "jsonrpc": "2.0",
        "id": 2,
        "method": "tools/call",
        "params": {
            "name": tool_name,
            "arguments": params
        }
    }
    
    print(f"\nSending request for {tool_name} (id=2)...")
    process.stdin.write(json.dumps(tool_request) + "\n")
    process.stdin.flush()
    
    # Читаем ответ на вызов инструмента (id=2)
    # Может быть несколько строк (stderr warnings), читаем до получения JSON с id=2
    response_line = None
    for _ in range(10):  # Максимум 10 попыток
        time.sleep(0.2)
        line = process.stdout.readline()
        if line and line.strip():
            try:
                parsed = json.loads(line.strip())
                if parsed.get('id') == 2:
                    response_line = line
                    break
            except:
                # Не JSON, пропускаем (возможно stderr)
                continue
    
    print(f"Raw response length: {len(response_line) if response_line else 0}")
    if response_line:
        print(f"Raw response preview: {response_line[:500]}...")
    else:
        print("WARNING: No response received for tool call!")
    
    try:
        response = json.loads(response_line.strip())
        print(f"\nParsed response structure:")
        print(f"  Keys: {list(response.keys())}")
        
        if 'result' in response:
            result = response['result']
            print(f"  Result type: {type(result)}")
            if isinstance(result, dict):
                print(f"  Result keys: {list(result.keys())}")
                if 'content' in result:
                    content = result['content']
                    print(f"  Content type: {type(content)}")
                    if isinstance(content, list) and len(content) > 0:
                        item = content[0]
                        print(f"  Content[0] keys: {list(item.keys())}")
                        if 'text' in item:
                            text = item['text']
                            print(f"  Text length: {len(text)}")
                            print(f"  Text preview: {text[:200]}...")
                            try:
                                parsed = json.loads(text)
                                print(f"  Parsed JSON keys: {list(parsed.keys()) if isinstance(parsed, dict) else 'N/A'}")
                                return parsed
                            except:
                                print("  Text is not JSON")
        
        return response
    except Exception as e:
        print(f"Error parsing: {e}")
        return None
    finally:
        process.terminate()
        process.wait()

def main():
    jar_path = os.path.join("mcp-server", "build", "libs", "aggregate-mcp-server-1.0.0.jar")
    
    if not os.path.exists(jar_path):
        print(f"JAR not found: {jar_path}")
        return 1
    
    # Тест 1: build_expression
    result1 = test_tool(jar_path, "aggregate_build_expression", {
        "inputFields": [
            {"name": "value1", "type": "E"},
            {"name": "value2", "type": "E"}
        ],
        "outputFields": [
            {"name": "result", "type": "E"}
        ],
        "formula": "({value1} + {value2}) / 2"
    })
    
    if result1 and isinstance(result1, dict):
        print(f"\n✓ SUCCESS: Got result with keys: {list(result1.keys())}")
        if 'inputFormat' in result1:
            print(f"  inputFormat: {result1['inputFormat']}")
        if 'outputFormat' in result1:
            print(f"  outputFormat: {result1['outputFormat']}")
        if 'expression' in result1:
            print(f"  expression: {result1['expression'][:80]}...")
    else:
        print("\n✗ FAILED: No valid result")
    
    # Тест 2: validate_expression
    print("\n\n")
    result2 = test_tool(jar_path, "aggregate_validate_expression", {
        "inputFormat": "<<value1><E><value2><E>>",
        "outputFormat": "<<result><E>>",
        "expression": "table(\"<<result><E>>\", ({value1} + {value2}) / 2)"
    })
    
    if result2 and isinstance(result2, dict):
        print(f"\n✓ SUCCESS: Got result with keys: {list(result2.keys())}")
        if 'valid' in result2:
            print(f"  valid: {result2['valid']}")
        if 'errors' in result2:
            print(f"  errors: {len(result2['errors'])}")
            for err in result2['errors'][:2]:
                print(f"    - {err}")
    else:
        print("\n✗ FAILED: No valid result")
    
    return 0

if __name__ == "__main__":
    sys.exit(main())
