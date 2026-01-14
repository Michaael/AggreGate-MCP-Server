#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Прямой тест создания контекстов тревог через MCP инструменты
После перезапуска MCP сервера этот скрипт можно использовать для тестирования
"""
import sys

# Настройка кодировки для Windows
if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

print("="*80)
print("ТЕСТ СОЗДАНИЯ КОНТЕКСТОВ ТРЕВОГ")
print("="*80)
print("\n[INFO] Используйте call_mcp_tool в Cursor для выполнения этих команд.")
print("       После перезапуска MCP сервера выполните команды по порядку.\n")

print("\n" + "="*80)
print("ШАГ 1: Подключение к серверу")
print("="*80)
print("""
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_connect",
    arguments={
        "host": "localhost",
        "port": 6460,
        "username": "admin",
        "password": "admin"
    }
)
print(result)
""")

print("\n" + "="*80)
print("ШАГ 2: Вход в систему")
print("="*80)
print("""
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_login",
    arguments={}
)
print(result)
""")

print("\n" + "="*80)
print("ШАГ 3: Создание тревоги 1 - Event 1 -> Event 2 (Int > 20)")
print("="*80)
print("""
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_context",
    arguments={
        "parentPath": "users.admin.alerts",
        "name": "alarmEvent1Event2",
        "description": "Тревога, срабатывающая на Event 1 и деактивирующаяся при Event 2 (Int > 20)"
    }
)
print(result)
if result.get("success"):
    print("✅ Тревога 1 создана успешно!")
else:
    print(f"❌ Ошибка: {result.get('error', 'Unknown error')}")
""")

print("\n" + "="*80)
print("ШАГ 4: Создание тревоги 2 - Через 10 секунд после суммы Int+Float в диапазоне 50-100")
print("="*80)
print("""
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_context",
    arguments={
        "parentPath": "users.admin.alerts",
        "name": "alarmDelayedSum",
        "description": "Тревога через 10 секунд после того, как сумма Int+Float попадает в диапазон 50-100"
    }
)
print(result)
if result.get("success"):
    print("✅ Тревога 2 создана успешно!")
else:
    print(f"❌ Ошибка: {result.get('error', 'Unknown error')}")
""")

print("\n" + "="*80)
print("ШАГ 5: Создание тревоги 3 - Сумма Int в Table > 100")
print("="*80)
print("""
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_create_context",
    arguments={
        "parentPath": "users.admin.alerts",
        "name": "alarmTableSum",
        "description": "Тревога на сумму Int в Table > 100 с корректирующим действием"
    }
)
print(result)
if result.get("success"):
    print("✅ Тревога 3 создана успешно!")
else:
    print(f"❌ Ошибка: {result.get('error', 'Unknown error')}")
""")

print("\n" + "="*80)
print("ШАГ 6: Проверка создания всех тревог")
print("="*80)
print("""
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_list_contexts",
    arguments={
        "mask": "users.admin.alerts.*"
    }
)
print(result)
print(f"\\nНайдено контекстов: {len(result.get('contexts', []))}")
for ctx in result.get('contexts', []):
    print(f"  - {ctx.get('path', 'N/A')}: {ctx.get('name', 'N/A')}")
""")

print("\n" + "="*80)
print("ОЖИДАЕМЫЙ РЕЗУЛЬТАТ")
print("="*80)
print("""
Все три тревоги должны быть созданы успешно без ошибок:
- users.admin.alerts.alarmEvent1Event2
- users.admin.alerts.alarmDelayedSum
- users.admin.alerts.alarmTableSum

Если возникает ошибка о необходимости containerType/objectType - значит сервер не перезапущен
или изменения не применены.
""")
