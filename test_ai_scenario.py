#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Комплексный тест: моделирование реального запроса ИИ для работы с MCP сервером
Задание: Создать систему мониторинга температуры с Expression функциями
"""
import json
import subprocess
import sys
import os
import time

if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

class McpAIClient:
    """Клиент для имитации работы ИИ с MCP сервером"""
    
    def __init__(self, jar_path):
        self.jar_path = jar_path
        self.process = None
        self.request_id = 0
        self.connection_key = "default"
        
    def start(self):
        """Запуск MCP сервера"""
        print("[AI] Запуск MCP сервера...")
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
                "clientInfo": {"name": "ai-assistant", "version": "1.0.0"}
            }
        }
        self._send_request(init_request)
        self._read_response()
        print("[AI] ✓ MCP сервер инициализирован")
        return True
    
    def _next_id(self):
        self.request_id += 1
        return self.request_id
    
    def _send_request(self, request):
        """Отправка запроса"""
        request_json = json.dumps(request, ensure_ascii=False) + "\n"
        self.process.stdin.write(request_json)
        self.process.stdin.flush()
    
    def _read_response(self, expected_id=None):
        """Чтение ответа"""
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

def print_step(step_num, description):
    """Вывод шага"""
    print(f"\n{'='*70}")
    print(f"ШАГ {step_num}: {description}")
    print('='*70)

def print_result(success, message, details=None):
    """Вывод результата"""
    status = "✓ УСПЕХ" if success else "✗ ОШИБКА"
    print(f"\n[{status}] {message}")
    if details:
        if isinstance(details, dict):
            for key, value in details.items():
                if isinstance(value, str) and len(value) > 100:
                    print(f"  {key}: {value[:100]}...")
                else:
                    print(f"  {key}: {value}")
        else:
            print(f"  {details}")

def run_ai_scenario():
    """Выполнение комплексного сценария работы ИИ"""
    
    jar_path = os.path.join("mcp-server", "build", "libs", "aggregate-mcp-server-1.0.0.jar")
    if not os.path.exists(jar_path):
        print(f"[ERROR] JAR не найден: {jar_path}")
        return 1
    
    client = McpAIClient(jar_path)
    results = {
        "total_steps": 0,
        "successful_steps": 0,
        "failed_steps": []
    }
    
    try:
        if not client.start():
            return 1
        
        # ============================================================
        # ЗАДАНИЕ ДЛЯ ИИ:
        # Создать систему мониторинга температуры с Expression функциями:
        # 1. Создать модель "temperature_monitor"
        # 2. Создать переменные: current_temp, min_temp, max_temp
        # 3. Создать Expression функцию для вычисления среднего значения
        # 4. Создать Expression функцию для проверки выхода за пределы
        # 5. Протестировать функции
        # 6. Установить начальные значения переменных
        # ============================================================
        
        # ШАГ 1: Подключение к серверу
        print_step(1, "Подключение к AggreGate серверу")
        results["total_steps"] += 1
        
        connect_result = client.call_tool("aggregate_connect", {
            "host": "localhost",
            "port": 6460,
            "username": "admin",
            "password": "admin"
        })
        
        if connect_result and connect_result.get("success"):
            print_result(True, "Подключено к серверу", {
                "host": connect_result.get("host"),
                "port": connect_result.get("port"),
                "username": connect_result.get("username")
            })
            results["successful_steps"] += 1
        else:
            error = connect_result.get("error", "Неизвестная ошибка") if connect_result else "Нет ответа"
            print_result(False, f"Не удалось подключиться: {error}")
            results["failed_steps"].append("Шаг 1: Подключение")
            # Продолжаем тест даже без подключения для демонстрации
        
        time.sleep(1)
        
        # Вход в систему
        login_result = client.call_tool("aggregate_login")
        if login_result and login_result.get("success"):
            print_result(True, "Вход выполнен")
        time.sleep(1)
        
        # ШАГ 2: Исследование структуры контекстов
        print_step(2, "Исследование структуры контекстов (aggregate_list_context_tree)")
        results["total_steps"] += 1
        
        tree_result = client.call_tool("aggregate_list_context_tree", {
            "rootPath": "users.admin",
            "maxDepth": 2
        })
        
        if tree_result and isinstance(tree_result, dict):
            path = tree_result.get("path", "N/A")
            name = tree_result.get("name", "N/A")
            children = tree_result.get("children", [])
            print_result(True, f"Найдено дерево контекстов", {
                "path": path,
                "name": name,
                "children_count": len(children)
            })
            results["successful_steps"] += 1
        else:
            print_result(False, "Не удалось получить дерево контекстов")
            results["failed_steps"].append("Шаг 2: Дерево контекстов")
        
        time.sleep(1)
        
        # ШАГ 3: Создание модели (идемпотентно)
        print_step(3, "Создание модели temperature_monitor (aggregate_get_or_create_context)")
        results["total_steps"] += 1
        
        model_path = "users.admin.models.temperature_monitor"
        create_result = client.call_tool("aggregate_get_or_create_context", {
            "path": model_path,
            "description": "Система мониторинга температуры"
        })
        
        if create_result and isinstance(create_result, dict):
            path = create_result.get("path", "N/A")
            created = create_result.get("created", False)
            print_result(True, f"Модель {'создана' if created else 'уже существует'}", {
                "path": path,
                "created": created
            })
            results["successful_steps"] += 1
        else:
            print_result(False, "Не удалось создать модель")
            results["failed_steps"].append("Шаг 3: Создание модели")
        
        time.sleep(1)
        
        # ШАГ 4: Создание переменных
        print_step(4, "Создание переменных (aggregate_create_variable)")
        results["total_steps"] += 1
        
        variables = [
            {"name": "current_temp", "format": "<value><E>", "description": "Текущая температура"},
            {"name": "min_temp", "format": "<value><E>", "description": "Минимальная температура"},
            {"name": "max_temp", "format": "<value><E>", "description": "Максимальная температура"}
        ]
        
        created_vars = []
        existing_vars = []
        for var in variables:
            # Используем идемпотентную операцию
            var_result = client.call_tool("aggregate_get_or_create_variable", {
                "path": model_path,
                "variableName": var["name"],
                "format": var["format"],
                "description": var["description"],
                "writable": True
            })
            
            if var_result and var_result.get("exists"):
                if var_result.get("created"):
                    created_vars.append(var["name"])
                    print(f"  ✓ Создана переменная: {var['name']}")
                else:
                    existing_vars.append(var["name"])
                    print(f"  ✓ Переменная уже существует: {var['name']}")
            else:
                error = var_result.get("error", "Неизвестная ошибка") if var_result else "Нет ответа"
                print(f"  ⚠ Переменная {var['name']}: {error}")
        
        total_vars = len(created_vars) + len(existing_vars)
        if total_vars > 0:
            print_result(True, f"Переменные готовы: {total_vars}/{len(variables)} (создано: {len(created_vars)}, существовало: {len(existing_vars)})", {
                "created": created_vars,
                "existing": existing_vars
            })
            results["successful_steps"] += 1
        else:
            print_result(False, "Не удалось создать переменные")
            results["failed_steps"].append("Шаг 4: Создание переменных")
        
        time.sleep(1)
        
        # ШАГ 5: Построение Expression функции для среднего значения
        print_step(5, "Построение Expression функции (aggregate_build_expression)")
        results["total_steps"] += 1
        
        build_result = client.call_tool("aggregate_build_expression", {
            "inputFields": [
                {"name": "value1", "type": "E", "description": "Первое значение"},
                {"name": "value2", "type": "E", "description": "Второе значение"}
            ],
            "outputFields": [
                {"name": "result", "type": "E", "description": "Среднее значение"}
            ],
            "formula": "({value1} + {value2}) / 2"
        })
        
        if build_result and build_result.get("success"):
            input_format = build_result.get("inputFormat", "")
            output_format = build_result.get("outputFormat", "")
            expression = build_result.get("expression", "")
            
            print_result(True, "Expression функция построена", {
                "inputFormat": input_format,
                "outputFormat": output_format,
                "expression": expression[:80] + "..." if len(expression) > 80 else expression
            })
            
            # Проверяем правильность форматов
            if '<<' not in input_format and '>>' not in input_format:
                print("  ✓ inputFormat правильный (без <<>>)")
            if '<<' not in output_format and '>>' not in output_format:
                print("  ✓ outputFormat правильный (без <<>>)")
            if '<<' in expression and '>>' in expression:
                print("  ✓ expression правильный (с <<>> внутри table())")
            
            results["successful_steps"] += 1
            avg_function_data = build_result
        else:
            print_result(False, "Не удалось построить Expression функцию")
            results["failed_steps"].append("Шаг 5: Построение Expression")
            avg_function_data = None
        
        time.sleep(1)
        
        # ШАГ 6: Валидация Expression функции
        print_step(6, "Валидация Expression функции (aggregate_validate_expression)")
        results["total_steps"] += 1
        
        if avg_function_data:
            validate_result = client.call_tool("aggregate_validate_expression", {
                "inputFormat": avg_function_data.get("inputFormat"),
                "outputFormat": avg_function_data.get("outputFormat"),
                "expression": avg_function_data.get("expression")
            })
            
            if validate_result:
                valid = validate_result.get("valid", False)
                errors = validate_result.get("errors", [])
                warnings = validate_result.get("warnings", [])
                
                if valid:
                    print_result(True, "Expression функция валидна", {
                        "errors": len(errors),
                        "warnings": len(warnings)
                    })
                    results["successful_steps"] += 1
                else:
                    print_result(False, f"Expression функция невалидна: {len(errors)} ошибок", {
                        "errors": errors[:2] if errors else []
                    })
                    results["failed_steps"].append("Шаг 6: Валидация")
            else:
                print_result(False, "Не удалось валидировать Expression")
                results["failed_steps"].append("Шаг 6: Валидация")
        else:
            print_result(False, "Пропущено (нет данных для валидации)")
            results["failed_steps"].append("Шаг 6: Валидация")
        
        time.sleep(1)
        
        # ШАГ 7: Создание Expression функции
        print_step(7, "Создание Expression функции (aggregate_create_function)")
        results["total_steps"] += 1
        
        if avg_function_data and validate_result and validate_result.get("valid"):
            create_func_result = client.call_tool("aggregate_create_function", {
                "path": model_path,
                "functionName": "calculate_average",
                "functionType": 1,  # Expression
                "inputFormat": avg_function_data.get("inputFormat"),
                "outputFormat": avg_function_data.get("outputFormat"),
                "expression": avg_function_data.get("expression"),
                "description": "Вычисление среднего значения двух температур"
            })
            
            if create_func_result and create_func_result.get("success"):
                print_result(True, "Expression функция создана", {
                    "functionName": "calculate_average",
                    "path": model_path
                })
                results["successful_steps"] += 1
            else:
                error = create_func_result.get("error", "Неизвестная ошибка") if create_func_result else "Нет ответа"
                # Если функция уже существует - это нормально
                if "already exists" in error.lower():
                    print_result(True, "Expression функция уже существует", {
                        "functionName": "calculate_average",
                        "path": model_path,
                        "note": "Функция была создана ранее"
                    })
                    results["successful_steps"] += 1
                else:
                    print_result(False, f"Не удалось создать функцию: {error}")
                    
                    # Используем aggregate_explain_error для диагностики
                    explain_result = client.call_tool("aggregate_explain_error", {
                        "message": error,
                        "toolName": "aggregate_create_function"
                    })
                    
                    if explain_result:
                        print("  [Диагностика ошибки:]")
                        print(f"    Категория: {explain_result.get('category', 'N/A')}")
                        print(f"    Объяснение: {explain_result.get('explanation', 'N/A')[:100]}...")
                        if 'recommendation' in explain_result:
                            print(f"    Рекомендация: {explain_result.get('recommendation', 'N/A')[:100]}...")
                    
                    results["failed_steps"].append("Шаг 7: Создание функции")
        else:
            print_result(False, "Пропущено (функция невалидна)")
            results["failed_steps"].append("Шаг 7: Создание функции")
        
        time.sleep(1)
        
        # ШАГ 8: Тестирование функции
        print_step(8, "Тестирование функции (aggregate_test_function)")
        results["total_steps"] += 1
        
        test_result = client.call_tool("aggregate_test_function", {
            "path": model_path,
            "functionName": "calculate_average",
            "parameters": {
                "value1": 20.5,
                "value2": 25.3
            }
        })
        
        if test_result:
            success = test_result.get("success", False)
            if success:
                result_value = test_result.get("result", {})
                print_result(True, "Функция протестирована", {
                    "input": "value1=20.5, value2=25.3",
                    "result": result_value
                })
                results["successful_steps"] += 1
            else:
                error = test_result.get("error", "Неизвестная ошибка")
                print_result(False, f"Тест провален: {error}")
                results["failed_steps"].append("Шаг 8: Тестирование")
        else:
            print_result(False, "Не удалось протестировать функцию")
            results["failed_steps"].append("Шаг 8: Тестирование")
        
        time.sleep(1)
        
        # ШАГ 9: Массовая установка переменных
        print_step(9, "Массовая установка переменных (aggregate_bulk_set_variables)")
        results["total_steps"] += 1
        
        bulk_result = client.call_tool("aggregate_bulk_set_variables", {
            "path": model_path,
            "items": [
                {"variableName": "current_temp", "value": 22.5},
                {"variableName": "min_temp", "value": 15.0},
                {"variableName": "max_temp", "value": 30.0}
            ]
        })
        
        if bulk_result and isinstance(bulk_result, dict):
            results_list = bulk_result.get("results", [])
            if results_list:
                success_count = sum(1 for r in results_list if r.get("success", False))
                
                if success_count > 0:
                    print_result(True, f"Установлено переменных: {success_count}/{len(results_list)}", {
                        "results": [r.get("variableName") for r in results_list if r.get("success")]
                    })
                    results["successful_steps"] += 1
                else:
                    # Если массовая установка не сработала, пробуем по одной через set_variable_field
                    print("  Попытка установить переменные по одной через set_variable_field...")
                    success_count = 0
                    for item in [{"variableName": "current_temp", "value": 22.5},
                                {"variableName": "min_temp", "value": 15.0},
                                {"variableName": "max_temp", "value": 30.0}]:
                        # Для переменных с maxRecords=1 используем set_variable_field
                        set_result = client.call_tool("aggregate_set_variable_field", {
                            "path": model_path,
                            "variableName": item["variableName"],
                            "fieldName": "value",
                            "value": item["value"]
                        })
                        if set_result and set_result.get("success"):
                            success_count += 1
                            print(f"  ✓ Установлено: {item['variableName']} = {item['value']}")
                    
                    if success_count > 0:
                        print_result(True, f"Установлено переменных: {success_count}/3")
                        results["successful_steps"] += 1
                    else:
                        errors = [r.get("error", "N/A") for r in results_list if not r.get("success")]
                        print_result(False, f"Не удалось установить переменные: {errors[0] if errors else 'Unknown'}")
                        results["failed_steps"].append("Шаг 9: Массовая установка")
            else:
                print_result(False, "Не удалось выполнить массовую установку (пустой результат)")
                results["failed_steps"].append("Шаг 9: Массовая установка")
        else:
            print_result(False, "Не удалось выполнить массовую установку")
            results["failed_steps"].append("Шаг 9: Массовая установка")
        
        time.sleep(1)
        
        # ШАГ 10: Получение информации о функции
        print_step(10, "Получение информации о функции (aggregate_get_function)")
        results["total_steps"] += 1
        
        get_func_result = client.call_tool("aggregate_get_function", {
            "path": model_path,
            "functionName": "calculate_average"
        })
        
        if get_func_result and isinstance(get_func_result, dict):
            func_name = get_func_result.get("name", "N/A")
            func_type = get_func_result.get("functionType", "N/A")
            print_result(True, "Информация о функции получена", {
                "name": func_name,
                "type": func_type,
                "hasInputFormat": "inputFormat" in get_func_result,
                "hasOutputFormat": "outputFormat" in get_func_result
            })
            results["successful_steps"] += 1
        else:
            print_result(False, "Не удалось получить информацию о функции")
            results["failed_steps"].append("Шаг 10: Получение информации")
        
        # ============================================================
        # ИТОГОВЫЙ ОТЧЁТ
        # ============================================================
        print(f"\n\n{'='*70}")
        print("ИТОГОВЫЙ ОТЧЁТ")
        print('='*70)
        print(f"Всего шагов: {results['total_steps']}")
        print(f"Успешных: {results['successful_steps']}")
        print(f"Провалено: {len(results['failed_steps'])}")
        
        if results['failed_steps']:
            print(f"\nПроваленные шаги:")
            for step in results['failed_steps']:
                print(f"  - {step}")
        
        success_rate = (results['successful_steps'] / results['total_steps'] * 100) if results['total_steps'] > 0 else 0
        print(f"\nПроцент успеха: {success_rate:.1f}%")
        
        if success_rate >= 80:
            print("\n🎉 СЦЕНАРИЙ ВЫПОЛНЕН УСПЕШНО!")
            return 0
        else:
            print("\n⚠️  СЦЕНАРИЙ ВЫПОЛНЕН С ОШИБКАМИ")
            return 1
        
    except KeyboardInterrupt:
        print("\n[INTERRUPT] Тест прерван пользователем")
        return 1
    except Exception as e:
        print(f"\n[ERROR] Критическая ошибка: {e}")
        import traceback
        traceback.print_exc()
        return 1
    finally:
        client.stop()

if __name__ == "__main__":
    sys.exit(run_ai_scenario())
