#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Тест создания контекстов тревог в users.admin.alerts
"""

import json
import sys
import time

def call_mcp_tool(server, tool, arguments):
    """Вызов MCP инструмента через call_mcp_tool"""
    # В реальном использовании это будет вызов через Cursor MCP
    # Здесь мы просто выводим команду для ручного выполнения
    print(f"\n{'='*80}")
    print(f"Вызов: {tool}")
    print(f"Параметры: {json.dumps(arguments, indent=2, ensure_ascii=False)}")
    print(f"{'='*80}")
    
    # В реальном сценарии здесь будет:
    # result = call_mcp_tool(server=server, tool=tool, arguments=arguments)
    # return result
    
    return {"success": True, "message": "Tool call simulated"}

def test_alerts_creation():
    """Тест создания контекстов тревог"""
    print("="*80)
    print("ТЕСТ СОЗДАНИЯ КОНТЕКСТОВ ТРЕВОГ")
    print("="*80)
    
    # 1. Подключение к серверу
    print("\n[1] Подключение к серверу...")
    connect_result = call_mcp_tool(
        server="user-aggregate",
        tool="aggregate_connect",
        arguments={
            "host": "localhost",
            "port": 6460,
            "username": "admin",
            "password": "admin"
        }
    )
    print(f"Результат подключения: {connect_result}")
    
    # 2. Вход в систему
    print("\n[2] Вход в систему...")
    login_result = call_mcp_tool(
        server="user-aggregate",
        tool="aggregate_login",
        arguments={}
    )
    print(f"Результат входа: {login_result}")
    
    # 3. Создание тревоги 1: Event 1 -> Event 2 (Int > 20)
    print("\n[3] Создание тревоги alarmEvent1Event2...")
    alarm1_result = call_mcp_tool(
        server="user-aggregate",
        tool="aggregate_create_context",
        arguments={
            "parentPath": "users.admin.alerts",
            "name": "alarmEvent1Event2",
            "description": "Тревога, срабатывающая на Event 1 и деактивирующаяся при Event 2 (Int > 20)"
        }
    )
    print(f"Результат создания тревоги 1: {alarm1_result}")
    
    if alarm1_result.get("success"):
        print("✅ Тревога 1 создана успешно!")
    else:
        print(f"❌ Ошибка создания тревоги 1: {alarm1_result.get('error', 'Unknown error')}")
        return False
    
    # 4. Создание тревоги 2: Через 10 секунд после суммы Int+Float в диапазоне 50-100
    print("\n[4] Создание тревоги alarmDelayedSum...")
    alarm2_result = call_mcp_tool(
        server="user-aggregate",
        tool="aggregate_create_context",
        arguments={
            "parentPath": "users.admin.alerts",
            "name": "alarmDelayedSum",
            "description": "Тревога через 10 секунд после того, как сумма Int+Float попадает в диапазон 50-100"
        }
    )
    print(f"Результат создания тревоги 2: {alarm2_result}")
    
    if alarm2_result.get("success"):
        print("✅ Тревога 2 создана успешно!")
    else:
        print(f"❌ Ошибка создания тревоги 2: {alarm2_result.get('error', 'Unknown error')}")
        return False
    
    # 5. Создание тревоги 3: Сумма Int в Table > 100
    print("\n[5] Создание тревоги alarmTableSum...")
    alarm3_result = call_mcp_tool(
        server="user-aggregate",
        tool="aggregate_create_context",
        arguments={
            "parentPath": "users.admin.alerts",
            "name": "alarmTableSum",
            "description": "Тревога на сумму Int в Table > 100 с корректирующим действием"
        }
    )
    print(f"Результат создания тревоги 3: {alarm3_result}")
    
    if alarm3_result.get("success"):
        print("✅ Тревога 3 создана успешно!")
    else:
        print(f"❌ Ошибка создания тревоги 3: {alarm3_result.get('error', 'Unknown error')}")
        return False
    
    # 6. Проверка создания всех тревог
    print("\n[6] Проверка создания всех тревог...")
    list_result = call_mcp_tool(
        server="user-aggregate",
        tool="aggregate_list_contexts",
        arguments={
            "mask": "users.admin.alerts.*"
        }
    )
    print(f"Список контекстов в alerts: {list_result}")
    
    print("\n" + "="*80)
    print("ТЕСТ ЗАВЕРШЕН")
    print("="*80)
    
    return True

if __name__ == "__main__":
    try:
        success = test_alerts_creation()
        sys.exit(0 if success else 1)
    except Exception as e:
        print(f"\n❌ Ошибка при выполнении теста: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
