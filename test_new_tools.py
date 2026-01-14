#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Комплексное тестирование всех новых MCP инструментов
"""
import json
import subprocess
import sys
import os
import time

# Настройка кодировки для Windows
if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

class McpTestClient:
    def __init__(self):
        self.process = None
        self.request_id = 0
        
    def start(self):
        """Запуск MCP сервера"""
        print("[INIT] Запуск MCP сервера...")
        jar_path = os.path.join("mcp-server", "build", "libs", "aggregate-mcp-server-1.0.0.jar")
        if not os.path.exists(jar_path):
            print(f"[ERROR] JAR файл не найден: {jar_path}")
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
        time.sleep(2)  # Дать серверу время на инициализацию
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
        print(f"\n[REQUEST] {method}")
        print(f"  Params: {json.dumps(params, indent=2, ensure_ascii=False)}")
        
        try:
            self.process.stdin.write(request_str)
            self.process.stdin.flush()
            
            # Чтение ответа
            response_line = self.process.stdout.readline()
            if not response_line:
                return None
                
            response = json.loads(response_line.strip())
            print(f"[RESPONSE] Status: {'OK' if 'result' in response else 'ERROR'}")
            
            # MCP формат: result.content[0].text содержит JSON строку с результатом
            if 'result' in response:
                result = response['result']
                if isinstance(result, dict) and 'content' in result:
                    content = result['content']
                    if isinstance(content, list) and len(content) > 0:
                        # Ищем text content
                        for item in content:
                            if item.get('type') == 'text':
                                try:
                                    parsed = json.loads(item.get('text', ''))
                                    if isinstance(parsed, dict):
                                        print(f"  Parsed content keys: {list(parsed.keys())}")
                                    return {'result': parsed}
                                except json.JSONDecodeError:
                                    # Если не JSON, возвращаем как есть
                                    return {'result': {'text': item.get('text', '')}}
                                except:
                                    pass
            elif 'error' in response:
                error = response.get('error', {})
                print(f"  Error: {error.get('message', 'Unknown error')}")
                
            return response
        except Exception as e:
            print(f"[ERROR] Exception: {e}")
            import traceback
            traceback.print_exc()
            return None
    
    def call_tool(self, tool_name, params=None):
        """Вызов MCP инструмента"""
        if params is None:
            params = {}
            
        response = self.send_request("tools/call", {
            "name": tool_name,
            "arguments": params
        })
        
        if not response:
            return None
            
        # Извлекаем результат из MCP формата
        if "result" in response:
            result = response["result"]
            if isinstance(result, dict) and "content" in result:
                for item in result.get("content", []):
                    if item.get("type") == "text":
                        try:
                            parsed = json.loads(item.get("text", ""))
                            return parsed
                        except:
                            # Если не JSON, возвращаем как есть
                            return {"text": item.get("text", "")}
            # Если result уже является нужным объектом
            return result
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
            print("[STOP] MCP сервер остановлен")

def test_build_expression(client):
    """Тест aggregate_build_expression"""
    print("\n" + "="*60)
    print("TEST: aggregate_build_expression")
    print("="*60)
    
    response = client.call_tool("aggregate_build_expression", {
        "inputFields": [
            {"name": "value1", "type": "E", "description": "Первое значение"},
            {"name": "value2", "type": "E", "description": "Второе значение"}
        ],
        "outputFields": [
            {"name": "result", "type": "E", "description": "Результат"}
        ],
        "formula": "({value1} + {value2}) / 2"
    })
    
    if response and isinstance(response, dict):
        input_format = response.get('inputFormat', 'N/A')
        output_format = response.get('outputFormat', 'N/A')
        expression = response.get('expression', 'N/A')
        
        if input_format != 'N/A' and output_format != 'N/A' and expression != 'N/A':
            print(f"✓ inputFormat: {input_format}")
            print(f"✓ outputFormat: {output_format}")
            print(f"✓ expression: {expression[:80]}...")
            # Проверяем, что форматы БЕЗ <<>>
            if '<<' not in input_format and '>>' not in input_format:
                print("✓ inputFormat правильный (без <<>>)")
            if '<<' not in output_format and '>>' not in output_format:
                print("✓ outputFormat правильный (без <<>>)")
            # Проверяем, что expression С <<>>
            if '<<' in expression and '>>' in expression:
                print("✓ expression правильный (с <<>> внутри table())")
            return True
    print("✗ Тест не прошёл")
    return False

def test_validate_expression(client):
    """Тест aggregate_validate_expression"""
    print("\n" + "="*60)
    print("TEST: aggregate_validate_expression")
    print("="*60)
    
    # Тест правильного выражения
    print("\n[TEST 1] Правильное выражение:")
    response = client.call_tool("aggregate_validate_expression", {
        "inputFormat": "<value1><E><value2><E>",
        "outputFormat": "<result><E>",
        "expression": "table(\"<<result><E>>\", ({value1} + {value2}) / 2)"
    })
    
    success = False
    if response and isinstance(response, dict):
        valid = response.get('valid', False)
        errors = response.get('errors', [])
        warnings = response.get('warnings', [])
        print(f"✓ Valid: {valid}")
        print(f"  Errors: {len(errors)}")
        print(f"  Warnings: {len(warnings)}")
        if valid:
            success = True
    
    # Тест неправильного выражения (с <<>> в форматах)
    print("\n[TEST 2] Неправильное выражение (<<>> в форматах):")
    response = client.call_tool("aggregate_validate_expression", {
        "inputFormat": "<<value1><E><value2><E>>",
        "outputFormat": "<<result><E>>",
        "expression": "table(\"<<result><E>>\", ({value1} + {value2}) / 2)"
    })
    
    if response and isinstance(response, dict):
        valid = response.get('valid', False)
        errors = response.get('errors', [])
        print(f"✓ Valid: {valid} (должно быть False)")
        print(f"  Errors: {len(errors)}")
        for error in errors[:2]:
            print(f"    - {error}")
        if not valid and len(errors) > 0:
            return True
    
    return success

def test_list_context_tree(client):
    """Тест aggregate_list_context_tree"""
    print("\n" + "="*60)
    print("TEST: aggregate_list_context_tree")
    print("="*60)
    
    # Сначала подключимся
    print("\n[STEP 1] Подключение к серверу...")
    client.call_tool("aggregate_connect", {
        "host": "localhost",
        "port": 6460,
        "username": "admin",
        "password": "admin"
    })
    time.sleep(1)
    
    client.call_tool("aggregate_login")
    time.sleep(1)
    
    print("\n[STEP 2] Получение дерева контекстов...")
    response = client.call_tool("aggregate_list_context_tree", {
        "rootPath": "users.admin",
        "maxDepth": 2
    })
    
    if response and isinstance(response, dict):
        path = response.get('path', 'N/A')
        name = response.get('name', 'N/A')
        children = response.get('children', [])
        print(f"✓ Path: {path}")
        print(f"✓ Name: {name}")
        print(f"✓ Children: {len(children)}")
        for child in children[:3]:
            if isinstance(child, dict):
                print(f"  - {child.get('path', 'N/A')}")
        return path != 'N/A' and path != 'users.admin'
    
    return False

def test_bulk_set_variables(client):
    """Тест aggregate_bulk_set_variables"""
    print("\n" + "="*60)
    print("TEST: aggregate_bulk_set_variables")
    print("="*60)
    
    # Создадим тестовую модель
    print("\n[STEP 1] Создание тестовой модели...")
    client.call_tool("aggregate_get_or_create_context", {
        "path": "users.admin.models.test_bulk"
    })
    time.sleep(1)
    
    # Создадим переменные (проверяем, что они не существуют)
    print("\n[STEP 2] Создание переменных...")
    var1_result = client.call_tool("aggregate_create_variable", {
        "path": "users.admin.models.test_bulk",
        "variableName": "var1",
        "format": "<value><E>",
        "writable": True
    })
    if var1_result and var1_result.get('error'):
        print(f"  Note: var1 уже существует или ошибка: {var1_result.get('error')}")
    
    var2_result = client.call_tool("aggregate_create_variable", {
        "path": "users.admin.models.test_bulk",
        "variableName": "var2",
        "format": "<value><E>",
        "writable": True
    })
    if var2_result and var2_result.get('error'):
        print(f"  Note: var2 уже существует или ошибка: {var2_result.get('error')}")
    time.sleep(1)
    
    # Тест массовой установки
    print("\n[STEP 3] Массовая установка значений...")
    response = client.call_tool("aggregate_bulk_set_variables", {
        "path": "users.admin.models.test_bulk",
        "items": [
            {"variableName": "var1", "value": 10.5},
            {"variableName": "var2", "value": 20.3}
        ]
    })
    
    if response and isinstance(response, dict):
        results = response.get('results', [])
        print(f"✓ Установлено переменных: {len(results)}")
        success_count = 0
        for item in results:
            if isinstance(item, dict):
                var_name = item.get('variableName', 'N/A')
                success = item.get('success', False)
                print(f"  - {var_name}: success={success}")
                if success:
                    success_count += 1
        return success_count > 0
    
    return False

def test_explain_error(client):
    """Тест улучшенного aggregate_explain_error"""
    print("\n" + "="*60)
    print("TEST: aggregate_explain_error (улучшенный)")
    print("="*60)
    
    # Тест ошибки Expression функции
    print("\n[TEST 1] Ошибка Expression функции:")
    response = client.call_tool("aggregate_explain_error", {
        "message": "Invalid inputFormat: <<value1><E>>",
        "toolName": "aggregate_create_function"
    })
    
    if response and isinstance(response, dict):
        category = response.get('category', 'N/A')
        explanation = response.get('explanation', 'N/A')
        print(f"✓ Category: {category}")
        print(f"✓ Explanation: {explanation[:100]}...")
        if 'detailedExplanation' in response:
            print(f"✓ Detailed: {response.get('detailedExplanation', 'N/A')[:100]}...")
        recommendation = response.get('recommendation', '')
        if 'aggregate_build_expression' in recommendation or 'build_expression' in recommendation:
            print("✓ Рекомендация содержит aggregate_build_expression")
            return True
        # Также проверяем, что это ошибка Expression функции
        if category == 'function_format':
            return True
    
    return False

def test_list_tools(client):
    """Тест aggregate_list_tools - проверка наличия новых инструментов"""
    print("\n" + "="*60)
    print("TEST: aggregate_list_tools (проверка новых инструментов)")
    print("="*60)
    
    # Используем tools/list для получения списка инструментов
    response = client.send_request("tools/list")
    
    # response должен быть в формате {"result": {"tools": [...]}}
    tool_names = []
    if response and 'result' in response:
        result = response['result']
        if 'tools' in result:
            tools = result['tools']
            if isinstance(tools, list):
                tool_names = [tool.get('name', '') for tool in tools if isinstance(tool, dict)]
    
    new_tools = [
        "aggregate_build_expression",
        "aggregate_validate_expression",
        "aggregate_list_context_tree",
        "aggregate_bulk_set_variables",
        "aggregate_explain_error"
    ]
    
    print(f"✓ Всего инструментов найдено: {len(tool_names)}")
    if tool_names:
        print(f"  Примеры: {tool_names[:5]}")
    print("\nПроверка новых инструментов:")
    found_count = 0
    for tool in new_tools:
        if tool in tool_names:
            print(f"  ✓ {tool}")
            found_count += 1
        else:
            print(f"  ✗ {tool} - НЕ НАЙДЕН!")
    
    return found_count == len(new_tools)

def main():
    """Главная функция тестирования"""
    print("="*60)
    print("КОМПЛЕКСНОЕ ТЕСТИРОВАНИЕ НОВЫХ MCP ИНСТРУМЕНТОВ")
    print("="*60)
    
    client = McpTestClient()
    
    try:
        if not client.start():
            print("[ERROR] Не удалось запустить MCP сервер")
            return 1
        
        results = {}
        
        # Тест 1: Проверка наличия инструментов
        results['list_tools'] = test_list_tools(client)
        time.sleep(1)
        
        # Тест 2: Build Expression
        results['build_expression'] = test_build_expression(client)
        time.sleep(1)
        
        # Тест 3: Validate Expression
        results['validate_expression'] = test_validate_expression(client)
        time.sleep(1)
        
        # Тест 4: List Context Tree (требует подключения)
        results['list_context_tree'] = test_list_context_tree(client)
        time.sleep(1)
        
        # Тест 5: Bulk Set Variables (требует подключения)
        results['bulk_set_variables'] = test_bulk_set_variables(client)
        time.sleep(1)
        
        # Тест 6: Explain Error
        results['explain_error'] = test_explain_error(client)
        
        # Итоговый отчёт
        print("\n" + "="*60)
        print("ИТОГОВЫЙ ОТЧЁТ")
        print("="*60)
        
        total = len(results)
        passed = sum(1 for v in results.values() if v)
        
        for test_name, result in results.items():
            status = "✓ PASSED" if result else "✗ FAILED"
            print(f"{test_name:30} {status}")
        
        print(f"\nВсего тестов: {total}")
        print(f"Пройдено: {passed}")
        print(f"Провалено: {total - passed}")
        
        if passed == total:
            print("\n🎉 ВСЕ ТЕСТЫ ПРОЙДЕНЫ УСПЕШНО!")
            return 0
        else:
            print(f"\n⚠️  Некоторые тесты провалены ({total - passed})")
            return 1
            
    except KeyboardInterrupt:
        print("\n[INTERRUPT] Тестирование прервано пользователем")
        return 1
    except Exception as e:
        print(f"\n[ERROR] Критическая ошибка: {e}")
        import traceback
        traceback.print_exc()
        return 1
    finally:
        client.stop()

if __name__ == "__main__":
    sys.exit(main())
