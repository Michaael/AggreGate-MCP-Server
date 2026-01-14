#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Пример выполнения задач через MCP инструменты
Этот файл показывает, как вызывать MCP инструменты через call_mcp_tool в Cursor
"""

# ВАЖНО: Этот скрипт показывает формат вызовов call_mcp_tool
# Для реального выполнения используйте call_mcp_tool напрямую в Cursor

def task1_create_users_and_access():
    """
    Задача 1: Создать двух пользователей и дать первому доступ ко всем устройствам другого
    """
    print("="*80)
    print("ЗАДАЧА 1: Создание пользователей и настройка доступа")
    print("="*80)
    
    # Создание первого пользователя
    print("\n[1.1] Создание пользователя user1...")
    # В Cursor используйте:
    # user1_result = call_mcp_tool(
    #     server="user-aggregate",
    #     tool="aggregate_create_user",
    #     arguments={
    #         "username": "user1",
    #         "password": "password1",
    #         "email": "user1@example.com"
    #     }
    # )
    
    # Создание второго пользователя
    print("[1.2] Создание пользователя user2...")
    # user2_result = call_mcp_tool(
    #     server="user-aggregate",
    #     tool="aggregate_create_user",
    #     arguments={
    #         "username": "user2",
    #         "password": "password2",
    #         "email": "user2@example.com"
    #     }
    # )
    
    # Создание устройства для user2
    print("[1.3] Создание виртуального устройства для user2...")
    # device_result = call_mcp_tool(
    #     server="user-aggregate",
    #     tool="aggregate_create_device",
    #     arguments={
    #         "username": "user2",
    #         "deviceName": "virtualDevice1",
    #         "description": "Виртуальное устройство user2",
    #         "driverId": "com.tibbo.linkserver.plugin.device.virtual"
    #     }
    # )
    
    print("\n[INFO] Настройка доступа требует использования Actions через веб-интерфейс")
    print("="*80)

def task2_alarm_event1_event2():
    """
    Задача 2: Тревога Event 1 -> Event 2 (Int > 20)
    """
    print("\n" + "="*80)
    print("ЗАДАЧА 2: Тревога Event 1 -> Event 2")
    print("="*80)
    
    # Создание модели
    print("\n[2.1] Создание модели тревоги...")
    # model_result = call_mcp_tool(
    #     server="user-aggregate",
    #     tool="aggregate_create_context",
    #     arguments={
    #         "parentPath": "users.admin.models",
    #         "name": "alarm_event_model",
    #         "description": "Модель тревоги Event 1 -> Event 2"
    #     }
    # )
    
    # Создание переменных
    print("[2.2] Создание переменных...")
    # status_var = call_mcp_tool(
    #     server="user-aggregate",
    #     tool="aggregate_create_variable",
    #     arguments={
    #         "path": "users.admin.models.alarm_event_model",
    #         "variableName": "alarmStatus",
    #         "format": "<status><S>",
    #         "writable": True
    #     }
    # )
    
    # event2_data = call_mcp_tool(
    #     server="user-aggregate",
    #     tool="aggregate_create_variable",
    #     arguments={
    #         "path": "users.admin.models.alarm_event_model",
    #         "variableName": "event2Data",
    #         "format": "<Int><I>",
    #         "writable": True
    #     }
    # )
    
    # Создание событий
    print("[2.3] Создание событий...")
    # activate_event = call_mcp_tool(
    #     server="user-aggregate",
    #     tool="aggregate_create_event",
    #     arguments={
    #         "path": "users.admin.models.alarm_event_model",
    #         "eventName": "alarmActivated",
    #         "description": "Тревога активирована"
    #     }
    # )
    
    # Создание функции проверки
    print("[2.4] Создание функции проверки условия...")
    # check_function = call_mcp_tool(
    #     server="user-aggregate",
    #     tool="aggregate_create_function",
    #     arguments={
    #         "path": "users.admin.models.alarm_event_model",
    #         "functionName": "checkDeactivation",
    #         "functionType": 1,
    #         "inputFormat": "<Int><I>",
    #         "outputFormat": "<result><B>",
    #         "expression": "table(\"<<result><B>>\", {Int} > 20)",
    #         "description": "Проверка условия деактивации (Int > 20)"
    #     }
    # )
    
    print("\n[INFO] Для полной настройки тревоги требуется настройка привязок через веб-интерфейс")
    print("="*80)

def task6_widget_form():
    """
    Задача 6: Форма в виджете (Grid Layout)
    """
    print("\n" + "="*80)
    print("ЗАДАЧА 6: Форма в виджете (Grid Layout)")
    print("="*80)
    
    # Создание виджета
    print("\n[6.1] Создание виджета...")
    # widget_result = call_mcp_tool(
    #     server="user-aggregate",
    #     tool="aggregate_create_widget",
    #     arguments={
    #         "parentPath": "users.admin.widgets",
    #         "name": "formWidget",
    #         "description": "Виджет с формой Grid Layout"
    #     }
    # )
    
    # Установка шаблона
    print("[6.2] Установка шаблона Grid Layout...")
    template = '''<?xml version="1.0" encoding="UTF-8"?>
<widget>
    <layout type="grid" columns="2">
        <label text="Имя:" gridx="0" gridy="0"/>
        <textfield name="name" gridx="1" gridy="0"/>
        
        <label text="Email:" gridx="0" gridy="1"/>
        <textfield name="email" gridx="1" gridy="1"/>
        
        <label text="Телефон:" gridx="0" gridy="2"/>
        <textfield name="phone" gridx="1" gridy="2"/>
        
        <button name="submit" text="Отправить" gridx="0" gridy="3" gridwidth="2"/>
    </layout>
</widget>'''
    
    # template_result = call_mcp_tool(
    #     server="user-aggregate",
    #     tool="aggregate_set_widget_template",
    #     arguments={
    #         "path": "users.admin.widgets.formWidget",
    #         "template": template
    #     }
    # )
    
    print("[PASS] Виджет с формой создан")
    print("="*80)

def task9_calculator_widget():
    """
    Задача 9: Калькулятор в виде виджета
    """
    print("\n" + "="*80)
    print("ЗАДАЧА 9: Калькулятор в виде виджета")
    print("="*80)
    
    # Создание виджета
    print("\n[9.1] Создание виджета калькулятора...")
    # widget_result = call_mcp_tool(
    #     server="user-aggregate",
    #     tool="aggregate_create_widget",
    #     arguments={
    #         "parentPath": "users.admin.widgets",
    #         "name": "calculatorWidget",
    #         "description": "Калькулятор в виде виджета"
    #     }
    # )
    
    # Установка шаблона
    print("[9.2] Установка шаблона калькулятора...")
    calc_template = '''<?xml version="1.0" encoding="UTF-8"?>
<widget>
    <layout type="grid" columns="4">
        <textfield name="display" gridx="0" gridy="0" gridwidth="4" readonly="true"/>
        
        <button name="btn7" text="7" gridx="0" gridy="1"/>
        <button name="btn8" text="8" gridx="1" gridy="1"/>
        <button name="btn9" text="9" gridx="2" gridy="1"/>
        <button name="btnDiv" text="/" gridx="3" gridy="1"/>
        
        <button name="btn4" text="4" gridx="0" gridy="2"/>
        <button name="btn5" text="5" gridx="1" gridy="2"/>
        <button name="btn6" text="6" gridx="2" gridy="2"/>
        <button name="btnMul" text="*" gridx="3" gridy="2"/>
        
        <button name="btn1" text="1" gridx="0" gridy="3"/>
        <button name="btn2" text="2" gridx="1" gridy="3"/>
        <button name="btn3" text="3" gridx="2" gridy="3"/>
        <button name="btnSub" text="-" gridx="3" gridy="3"/>
        
        <button name="btn0" text="0" gridx="0" gridy="4" gridwidth="2"/>
        <button name="btnDot" text="." gridx="2" gridy="4"/>
        <button name="btnAdd" text="+" gridx="3" gridy="4"/>
        
        <button name="btnEquals" text="=" gridx="0" gridy="5" gridwidth="4"/>
    </layout>
</widget>'''
    
    # template_result = call_mcp_tool(
    #     server="user-aggregate",
    #     tool="aggregate_set_widget_template",
    #     arguments={
    #         "path": "users.admin.widgets.calculatorWidget",
    #         "template": calc_template
    #     }
    # )
    
    print("[PASS] Виджет калькулятора создан")
    print("="*80)

def task18_dashboard():
    """
    Задача 18: Дашборд для виртуальных устройств
    """
    print("\n" + "="*80)
    print("ЗАДАЧА 18: Дашборд для виртуальных устройств")
    print("="*80)
    
    # Создание дашборда
    print("\n[18.1] Создание дашборда...")
    # dashboard_result = call_mcp_tool(
    #     server="user-aggregate",
    #     tool="aggregate_create_dashboard",
    #     arguments={
    #         "parentPath": "users.admin.dashboards",
    #         "name": "virtualDevicesDashboard",
    #         "description": "Дашборд для виртуальных устройств",
    #         "layout": "dockable"
    #     }
    # )
    
    # Добавление элементов
    print("[18.2] Добавление элементов дашборда...")
    # graph_element = call_mcp_tool(
    #     server="user-aggregate",
    #     tool="aggregate_add_dashboard_element",
    #     arguments={
    #         "path": "users.admin.dashboards.virtualDevicesDashboard",
    #         "name": "sineGraph",
    #         "type": "launchWidget",
    #         "parameters": {
    #             "format": "<widget><S>",
    #             "records": [{"widget": "users.admin.widgets.sineChartWidget"}]
    #         }
    #     }
    # )
    
    print("[PASS] Дашборд создан")
    print("="*80)

if __name__ == "__main__":
    print("\n" + "="*80)
    print("ПРИМЕРЫ ВЫЗОВОВ MCP ИНСТРУМЕНТОВ")
    print("="*80)
    print("\nЭтот файл показывает формат вызовов call_mcp_tool.")
    print("Для реального выполнения используйте call_mcp_tool напрямую в Cursor.")
    print("\nПодробное руководство: ALL_TASKS_MCP_GUIDE.md")
    print("="*80)
    
    # Примеры вызовов (закомментированы, так как требуют call_mcp_tool)
    # task1_create_users_and_access()
    # task2_alarm_event1_event2()
    # task6_widget_form()
    # task9_calculator_widget()
    # task18_dashboard()

