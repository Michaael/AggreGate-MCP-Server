# Обновление AggreGate SDK до версии 1.3.6

## Выполненные изменения

### 1. Обновлены зависимости в `modules/aggregate-api/build.gradle.kts`

Обновлены версии библиотек до актуальных из последнего SDK 1.3.6:

- **Apache Commons**:
  - `commons-net`: 3.3
  - `commons-lang3`: 3.14.0
  - `commons-io`: 2.11.0
  - `commons-math3`: 3.6.1
  - `commons-beanutils`: 1.9.4
  - `commons-logging`: 1.2

- **Log4j** (критическое обновление безопасности):
  - `log4j-api`: 2.23.1
  - `log4j-core`: 2.23.1
  - `log4j-1.2-api`: 2.23.1
  - `log4j-slf4j-impl`: 2.23.1
  - `slf4j-api`: 2.0.9

- **Guava**: 32.1.3-jre (последняя версия, поддерживающая Java 8)

- **Jackson**: 2.14.1 (версия из SDK 1.3.6)

### 2. Обновлена конфигурация `mcp-server/build.gradle.kts`

- Обновлена версия Jackson до 2.14.1 (из последнего SDK)
- Добавлены комментарии о версиях из SDK 1.3.6

### 3. Конфигурация исходников

Модуль `modules/aggregate-api` настроен для использования исходников из `sdk-temp/aggregate-api/src/main/java`, что обеспечивает использование последней версии API из репозитория GitHub.

## Как обновить JAR файлы

Для полного обновления до последней версии SDK выполните:

```powershell
.\update-sdk.ps1
```

Или вручную:

1. Соберите SDK из репозитория `https://github.com/Michaael/AggreGate-SDK-CE`
2. Скопируйте JAR файлы в папку `libs/`:
   - `aggregate-api.jar`
   - `aggregate-api-libs.jar` (если есть)

## Версии из последнего SDK 1.3.6

Все версии зависимостей соответствуют последней версии SDK 1.3.6 из репозитория:
- **GitHub**: https://github.com/Michaael/AggreGate-SDK-CE
- **Версия SDK**: 1.3.6
- **Дата обновления**: 2025-12-12

## Примечания

- Все зависимости совместимы с Java 8
- Log4j обновлен до версии 2.23.1 с исправлениями безопасности
- Guava обновлен до 32.1.3-jre (последняя версия для Java 8)

