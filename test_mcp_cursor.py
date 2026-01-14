#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Тест работы MCP сервера через Cursor
Проверяет доступность инструментов через call_mcp_tool
"""
import json
import sys

# Настройка кодировки для Windows
if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

def test_mcp_tools():
    """Тест вызова MCP инструментов"""
    print("="*80)
    print("ТЕСТ РАБОТЫ MCP СЕРВЕРА ЧЕРЕЗ CURSOR")
    print("="*80)
    
    # Информация о том, как использовать call_mcp_tool
    print("\n[INFO] Для использования call_mcp_tool в Cursor:")
    print("  1. Убедитесь, что Cursor перезапущен после добавления конфигурации")
    print("  2. Используйте следующий формат вызова:")
    print()
    print("  call_mcp_tool(")
    print("      server='user-aggregate',")
    print("      tool='aggregate_connect',")
    print("      arguments={")
    print("          'host': 'localhost',")
    print("          'port': 6460,")
    print("          'username': 'admin',")
    print("          'password': 'admin'")
    print("      }")
    print("  )")
    print()
    
    # Примеры вызовов
    examples = [
        {
            "name": "Подключение к серверу",
            "server": "user-aggregate",
            "tool": "aggregate_connect",
            "arguments": {
                "host": "localhost",
                "port": 6460,
                "username": "admin",
                "password": "admin"
            }
        },
        {
            "name": "Вход в систему",
            "server": "user-aggregate",
            "tool": "aggregate_login",
            "arguments": {}
        },
        {
            "name": "Список контекстов",
            "server": "user-aggregate",
            "tool": "aggregate_list_contexts",
            "arguments": {
                "mask": "*"
            }
        }
    ]
    
    print("\n[EXAMPLES] Примеры вызовов инструментов:")
    print("-" * 80)
    for i, example in enumerate(examples, 1):
        print(f"\n{i}. {example['name']}:")
        print(f"   call_mcp_tool(")
        print(f"       server='{example['server']}',")
        print(f"       tool='{example['tool']}',")
        print(f"       arguments={json.dumps(example['arguments'], indent=8, ensure_ascii=False)}")
        print(f"   )")
    
    print("\n" + "="*80)
    print("[NOTE] Этот скрипт показывает формат вызовов.")
    print("       Для реального тестирования используйте call_mcp_tool в Cursor.")
    print("="*80)

if __name__ == "__main__":
    test_mcp_tools()

