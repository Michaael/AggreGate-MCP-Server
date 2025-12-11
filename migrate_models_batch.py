#!/usr/bin/env python3
"""
Скрипт для пакетной миграции моделей
Использует MCP инструменты для миграции всех моделей
"""

# Список всех моделей для миграции (исключая тестовые)
MODELS_TO_MIGRATE = [
    "accessControlSystem", "applicationServ", "barQrCodeCreater", "barQrCodePresentator",
    "bffLims", "bffLimsN", "blind", "coldWaterMeter", "coldWaterMeters1", "combo",
    "compressUnit", "compressorUnit", "condensator", "control", "controlAccess",
    "dataClient", "dataColdImitator", "dataElectrImitator", "dataGazImitator",
    "dataHotImitator", "datalift", "detailsForHost", "devElectr", "deviceImages",
    "devicesSCUD", "electrMeters", "electricMeter", "employeeSCUD", "engineeringSystems",
    "fileManagerAgent", "fire", "fireAlarmSystem", "frontendConfig", "frozenUnit",
    "gasMeter", "gazMeters", "geocoding", "gpTestImitator", "graphApplication",
    "graphEngineer", "graphITinfrastructure", "graphics", "guideMeter", "hotWaterMeter",
    "hotWaterMeters", "hydroaccumulator", "imDevAccess", "imDevElectroCounter",
    "imDevFire", "imDevIT1", "imDevPump", "imDevVent", "imDevWarmCounter",
    "imDevWaterCounter", "imLogAccess", "imLogCommon", "imLogEnergy", "imLogFireControl",
    "imLogIT1", "imLogIT2", "imLogIT3", "imLogIT4", "imLogLifts", "imLogPumpControl",
    "imLogRefrigerators", "imLogSewerage", "imLogTermal", "imLogVent", "imLogVideo",
    "importMeters", "itInfrastructure", "itServices", "kpiInfo", "limsBackend",
    "massAndVolumeOfPetroleumProducts", "massAndVolumeOfPetroleumProductsLinux",
    "networkDevice", "personnel", "pressureGauge", "pressureSwitch", "pumpStation",
    "pumpUnit", "restApi", "restArena", "servers", "serversAbsolute", "serviceModel",
    "servises", "siren", "smoke", "stomp", "system", "telegramConnector", "temp",
    "temperatureSensor", "termal", "termo", "test", "testEncoding", "testImage",
    "thermalPoint", "thermostaticValve1", "thermostaticValve2", "userSCUD",
    "ventilation", "ventilationSystem", "video", "workerLims", "workerLimsN",
    "abslModelStrijRawData"
]

# Уже мигрированные модели
MIGRATED = [
    "objects", "abonent", "absl", "absoluteMeter", "pump", "sensor", "meters", "devices"
]

def get_model_path(model_name):
    """Возвращает полный путь к модели"""
    return f"users.admin.models.{model_name}"

def migrate_model_batch(model_names, source_conn="source", target_conn="target"):
    """
    Мигрирует пакет моделей
    
    Args:
        model_names: список имен моделей
        source_conn: ключ подключения к исходному серверу
        target_conn: ключ подключения к целевому серверу
    """
    results = []
    
    for model_name in model_names:
        model_path = get_model_path(model_name)
        print(f"\n{'='*60}")
        print(f"Миграция: {model_name}")
        print(f"{'='*60}")
        
        try:
            # Получить childInfo
            childInfo = mcp_aggregate_aggregate_get_variable(
                path=model_path,
                name="childInfo",
                connectionKey=source_conn
            )
            
            model_type = childInfo['records'][0]['type']
            print(f"Тип: {model_type}")
            
            # Обновить на целевом сервере
            mcp_aggregate_aggregate_set_variable_field(
                path=model_path,
                variableName="childInfo",
                fieldName="type",
                value=model_type,
                connectionKey=target_conn
            )
            
            # Получить и установить компоненты (если есть)
            for component in ["modelVariables", "modelFunctions", "modelEvents", "bindings", "ruleSets"]:
                try:
                    data = mcp_aggregate_aggregate_get_variable(
                        path=model_path,
                        name=component,
                        connectionKey=source_conn
                    )
                    if data['recordCount'] > 0:
                        mcp_aggregate_aggregate_set_variable(
                            path=model_path,
                            name=component,
                            value=data,
                            connectionKey=target_conn
                        )
                        print(f"  ✅ {component}: {data['recordCount']}")
                except Exception as e:
                    print(f"  ⚠️ {component}: ошибка - {e}")
            
            print(f"✅ {model_name} мигрирована")
            results.append({"model": model_name, "success": True})
            
        except Exception as e:
            print(f"❌ Ошибка при миграции {model_name}: {e}")
            results.append({"model": model_name, "success": False, "error": str(e)})
    
    return results

# Для использования: вызвать migrate_model_batch(MODELS_TO_MIGRATE)

