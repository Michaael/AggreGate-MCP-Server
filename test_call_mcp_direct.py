#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Прямой тест call_mcp_tool после перезапуска сервера
"""
import sys

# Настройка кодировки для Windows
if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

print("="*80)
print("ТЕСТ CALL_MCP_TOOL ПОСЛЕ ПЕРЕЗАПУСКА СЕРВЕРА")
print("="*80)
print("\n[INFO] Этот скрипт показывает формат вызовов.")
print("       Для реального тестирования используйте call_mcp_tool в Cursor.")
print("\n" + "="*80)

# Примеры вызовов (закомментированы, так как требуют call_mcp_tool в Cursor)
examples = """
# 1. Подключение к серверу
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

# 2. Вход в систему
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_login",
    arguments={}
)

# 3. Получение списка контекстов
result = call_mcp_tool(
    server="user-aggregate",
    tool="aggregate_list_contexts",
    arguments={
        "mask": "*"
    }
)
"""

print(examples)
print("="*80)

