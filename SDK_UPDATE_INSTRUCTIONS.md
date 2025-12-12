# Инструкция по обновлению AggreGate SDK

## Обзор

Проект использует локальные JAR файлы из папки `libs/`:
- `aggregate-api.jar` - основной API AggreGate
- `aggregate-api-libs.jar` - зависимости API (может быть тем же файлом или отдельным)

Новый SDK репозиторий: `root@155.212.171.244:/root/aggregate-sdk.git`

## Способ 1: Автоматическое обновление (рекомендуется)

Используйте скрипт `update-sdk.ps1`:

```powershell
.\update-sdk.ps1
```

Скрипт выполнит:
1. Клонирование нового SDK репозитория
2. Сборку JAR файлов
3. Копирование в `libs/`
4. Пересборку проекта

**Примечание:** Если сборка SDK не удалась из-за проблем с версиями Java/Gradle, используйте Способ 2.

## Способ 2: Сборка на сервере (если локальная сборка не работает)

Если локальная сборка не работает из-за несовместимости версий, соберите SDK на сервере:

```bash
# Подключитесь к серверу
ssh root@155.212.171.244

# Перейдите в директорию SDK
cd /root/aggregate-sdk

# Соберите JAR файлы
./gradlew :widget-api:jar :aggregate-api:jar -x test

# JAR файлы будут в:
# - /root/aggregate-sdk/aggregate-api/build/libs/aggregate-api.jar
# - /root/aggregate-sdk/widget-api/build/libs/widget-api.jar
```

Затем скопируйте JAR файлы на локальную машину:

```powershell
# Создайте резервную копию старых JAR
New-Item -ItemType Directory -Path "libs-backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')" -Force
Copy-Item "libs\aggregate-api.jar" "libs-backup-*\aggregate-api.jar.backup"
Copy-Item "libs\aggregate-api-libs.jar" "libs-backup-*\aggregate-api-libs.jar.backup"

# Скопируйте новые JAR файлы (используйте scp или другой способ)
scp root@155.212.171.244:/root/aggregate-sdk/aggregate-api/build/libs/aggregate-api.jar libs\aggregate-api.jar
```

## Способ 3: Ручное обновление

1. **Клонируйте SDK репозиторий:**
   ```powershell
   git clone root@155.212.171.244:/root/aggregate-sdk.git sdk-temp
   ```

2. **Соберите JAR файлы:**
   ```powershell
   cd sdk-temp
   .\gradlew.bat :widget-api:jar :aggregate-api:jar -x test
   ```

3. **Создайте резервную копию старых JAR:**
   ```powershell
   cd ..
   $backup = "libs-backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
   New-Item -ItemType Directory -Path $backup -Force
   Copy-Item "libs\aggregate-api.jar" "$backup\aggregate-api.jar.backup"
   Copy-Item "libs\aggregate-api-libs.jar" "$backup\aggregate-api-libs.jar.backup"
   ```

4. **Скопируйте новые JAR файлы:**
   ```powershell
   Copy-Item "sdk-temp\aggregate-api\build\libs\aggregate-api.jar" "libs\aggregate-api.jar" -Force
   
   # Если есть aggregate-api-libs.jar:
   if (Test-Path "sdk-temp\aggregate-api\build\libs\aggregate-api-libs.jar") {
       Copy-Item "sdk-temp\aggregate-api\build\libs\aggregate-api-libs.jar" "libs\aggregate-api-libs.jar" -Force
   }
   ```

5. **Пересоберите проект:**
   ```powershell
   .\gradlew.bat clean build
   ```

## Способ 4: Использование Maven репозитория

Если JAR файлы доступны в Maven репозитории, можно скачать их напрямую:

```powershell
# Пример (требуется правильная версия и координаты)
# Maven репозиторий: https://store.aggregate.digital/repository/maven-public
# Используйте Maven или Gradle для загрузки зависимостей
```

## Проверка обновления

После обновления проверьте:

1. **Размеры JAR файлов изменились:**
   ```powershell
   Get-Item libs\aggregate-api.jar | Select-Object Name, Length, LastWriteTime
   ```

2. **Проект собирается без ошибок:**
   ```powershell
   .\gradlew.bat clean build
   ```

3. **MCP сервер работает корректно:**
   ```powershell
   # Запустите MCP сервер и проверьте работу
   ```

## Откат изменений

Если что-то пошло не так, можно откатить изменения:

```powershell
# Найдите последнюю резервную копию
$backup = Get-ChildItem -Directory -Filter "libs-backup-*" | Sort-Object LastWriteTime -Descending | Select-Object -First 1

# Восстановите JAR файлы
Copy-Item "$backup\aggregate-api.jar.backup" "libs\aggregate-api.jar" -Force
if (Test-Path "$backup\aggregate-api-libs.jar.backup") {
    Copy-Item "$backup\aggregate-api-libs.jar.backup" "libs\aggregate-api-libs.jar" -Force
}
```

## Устранение проблем

### Проблема: Сборка SDK не удалась

**Причины:**
- Несовместимость версий Java (требуется Java 8+)
- Несовместимость версий Gradle (SDK использует Gradle 8.5)
- Отсутствие зависимостей в Maven репозитории

**Решения:**
1. Используйте Способ 2 (сборка на сервере)
2. Обновите Java до версии 8 или выше
3. Используйте Gradle wrapper из SDK репозитория

### Проблема: JAR файлы не найдены после сборки

**Проверьте:**
- Путь к JAR файлам: `sdk-temp\aggregate-api\build\libs\aggregate-api.jar`
- Сборка завершилась успешно (проверьте логи)
- Файлы не были удалены антивирусом

### Проблема: Проект не собирается после обновления

**Проверьте:**
- Совместимость версий API
- Отсутствие конфликтов зависимостей
- Логи сборки на наличие ошибок

## Дополнительная информация

- SDK репозиторий: `root@155.212.171.244:/root/aggregate-sdk.git`
- Maven репозиторий: `https://store.aggregate.digital/repository/maven-public`
- Версия SDK: см. `sdk-temp/buildSrc/src/main/java/provider/ConstantsProvider.kt`

