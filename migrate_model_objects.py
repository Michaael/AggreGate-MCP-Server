#!/usr/bin/env python3
"""
Скрипт для переноса модели objects прямым методом
Данные получены с исходного сервера, теперь нужно создать на целевом
"""

# Данные модели objects с исходного сервера
model_data = {
    "name": "objects",
    "description": "объекты",
    "type": 1,  # TYPE_ABSOLUTE
    "group": "default",
    "childInfo": {
        "name": "objects",
        "description": "объекты",
        "type": 1,
        "validityExpression": "",
        "validityListeners": {"recordCount": 0, "records": []},
        "containerType": "objects",
        "containerTypeDescription": "Objects",
        "containerName": "objects",
        "defaultContext": None,
        "objectType": "object",
        "objectTypeDescription": "Object",
        "objectNamingExpression": "",
        "enabled": True,
        "generateAttachedEvents": False,
        "ruleSetCallStackDepthThreshold": 100,
        "normalConcurrentBindings": 3,
        "maximumConcurrentBindings": 30,
        "maximumBindingQueueLength": 100,
        "logBindingsExecution": False,
        "protected": False
    },
    "modelVariables": 4,  # 4 переменные
    "modelFunctions": 1,  # 1 функция
    "modelEvents": 0,
    "bindings": 0,
    "ruleSets": 0
}

print("Данные модели objects:")
print(f"  Тип: {model_data['type']} (абсолютная)")
print(f"  Группа: {model_data['group']}")
print(f"  Переменные: {model_data['modelVariables']}")
print(f"  Функции: {model_data['modelFunctions']}")
print(f"  События: {model_data['modelEvents']}")
print(f"  Привязки: {model_data['bindings']}")
print(f"  Наборы правил: {model_data['ruleSets']}")

print("\nШаги для переноса:")
print("1. Проверить существование модели на целевом сервере")
print("2. Если существует - удалить или обновить")
print("3. Создать контекст модели")
print("4. Установить childInfo с типом")
print("5. Установить info с группой")
print("6. Установить modelVariables")
print("7. Установить modelFunctions")
print("8. Установить modelEvents (если есть)")
print("9. Установить bindings (если есть)")
print("10. Установить ruleSets (если есть)")

