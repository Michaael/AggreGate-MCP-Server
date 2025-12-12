# Скрипт для обновления SDK из нового репозитория
# Использование: .\update-sdk.ps1

$ErrorActionPreference = "Stop"

Write-Host "=== Обновление AggreGate SDK ===" -ForegroundColor Green

# Путь к временной директории SDK
$sdkTempPath = "sdk-temp"
$libsPath = "libs"

# Шаг 1: Клонирование/обновление SDK репозитория
Write-Host "`n[1/4] Клонирование SDK репозитория..." -ForegroundColor Yellow
if (Test-Path $sdkTempPath) {
    Write-Host "Удаление старой версии SDK..." -ForegroundColor Gray
    Remove-Item -Recurse -Force $sdkTempPath
}

Write-Host "Клонирование root@155.212.171.244:/root/aggregate-sdk.git..." -ForegroundColor Gray
git clone root@155.212.171.244:/root/aggregate-sdk.git $sdkTempPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "ОШИБКА: Не удалось клонировать SDK репозиторий" -ForegroundColor Red
    exit 1
}

# Шаг 2: Сборка SDK
Write-Host "`n[2/4] Сборка SDK..." -ForegroundColor Yellow
Set-Location $sdkTempPath

Write-Host "Сборка widget-api и aggregate-api..." -ForegroundColor Gray
.\gradlew.bat :widget-api:jar :aggregate-api:jar -x test --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "ПРЕДУПРЕЖДЕНИЕ: Сборка SDK не удалась." -ForegroundColor Yellow
    Write-Host "Возможные причины:" -ForegroundColor Yellow
    Write-Host "  - Несовместимость версий Java/Gradle" -ForegroundColor Gray
    Write-Host "  - Отсутствие зависимостей в Maven репозитории" -ForegroundColor Gray
    Write-Host "`nАльтернативные варианты:" -ForegroundColor Yellow
    Write-Host "  1. Соберите SDK на сервере с правильной версией Java/Gradle:" -ForegroundColor Gray
    Write-Host "     ssh root@155.212.171.244" -ForegroundColor Gray
    Write-Host "     cd /root/aggregate-sdk" -ForegroundColor Gray
    Write-Host "     ./gradlew :widget-api:jar :aggregate-api:jar -x test" -ForegroundColor Gray
    Write-Host "     Затем скопируйте JAR файлы вручную" -ForegroundColor Gray
    Write-Host "`n  2. Используйте уже собранные JAR файлы из Maven репозитория" -ForegroundColor Gray
    Write-Host "     https://store.aggregate.digital/repository/maven-public" -ForegroundColor Gray
    Write-Host "`n  3. Попробуйте собрать вручную с правильной версией Java:" -ForegroundColor Gray
    Write-Host "     cd $sdkTempPath" -ForegroundColor Gray
    Write-Host "     .\gradlew.bat :widget-api:jar :aggregate-api:jar -x test" -ForegroundColor Gray
    Set-Location ..
    Write-Host "`nПродолжаем с проверкой существующих JAR файлов..." -ForegroundColor Yellow
} else {
    Write-Host "Сборка SDK успешно завершена!" -ForegroundColor Green
}

# Шаг 3: Копирование JAR файлов
Write-Host "`n[3/4] Копирование JAR файлов в libs/..." -ForegroundColor Yellow
Set-Location ..

# Проверка существования собранных JAR файлов
$aggregateApiJar = "$sdkTempPath\aggregate-api\build\libs\aggregate-api.jar"
$widgetApiJar = "$sdkTempPath\widget-api\build\libs\widget-api.jar"

if (-not (Test-Path $aggregateApiJar)) {
    Write-Host "ОШИБКА: aggregate-api.jar не найден в $aggregateApiJar" -ForegroundColor Red
    Write-Host "`nПожалуйста, соберите SDK вручную или используйте уже собранные JAR файлы." -ForegroundColor Yellow
    Write-Host "После сборки JAR файлы должны находиться в:" -ForegroundColor Gray
    Write-Host "  - $aggregateApiJar" -ForegroundColor Gray
    Write-Host "  - $widgetApiJar" -ForegroundColor Gray
    Set-Location ..
    exit 1
}

# Создание резервной копии старых JAR файлов
Write-Host "Создание резервной копии старых JAR файлов..." -ForegroundColor Gray
$backupPath = "libs-backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
New-Item -ItemType Directory -Path $backupPath -Force | Out-Null
if (Test-Path "$libsPath\aggregate-api.jar") {
    Copy-Item "$libsPath\aggregate-api.jar" "$backupPath\aggregate-api.jar.backup"
}
if (Test-Path "$libsPath\aggregate-api-libs.jar") {
    Copy-Item "$libsPath\aggregate-api-libs.jar" "$backupPath\aggregate-api-libs.jar.backup"
}

# Копирование новых JAR файлов
Write-Host "Копирование aggregate-api.jar..." -ForegroundColor Gray
Copy-Item $aggregateApiJar "$libsPath\aggregate-api.jar" -Force

# Для aggregate-api-libs.jar - это может быть тот же файл или отдельный
# Проверяем, есть ли отдельный файл aggregate-api-libs
if (Test-Path "$sdkTempPath\aggregate-api\build\libs\aggregate-api-libs.jar") {
    Copy-Item "$sdkTempPath\aggregate-api\build\libs\aggregate-api-libs.jar" "$libsPath\aggregate-api-libs.jar" -Force
    Write-Host "Копирование aggregate-api-libs.jar..." -ForegroundColor Gray
} else {
    Write-Host "ПРЕДУПРЕЖДЕНИЕ: aggregate-api-libs.jar не найден. Используется aggregate-api.jar" -ForegroundColor Yellow
    # Если aggregate-api-libs.jar - это отдельный артефакт, возможно нужно собрать его отдельно
    # Или это может быть тот же aggregate-api.jar
}

# Шаг 4: Очистка и пересборка проекта
Write-Host "`n[4/4] Пересборка проекта с новым SDK..." -ForegroundColor Yellow
Write-Host "Очистка проекта..." -ForegroundColor Gray
.\gradlew.bat clean --no-daemon

Write-Host "Сборка проекта..." -ForegroundColor Gray
.\gradlew.bat build --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "ПРЕДУПРЕЖДЕНИЕ: Сборка проекта завершилась с ошибками" -ForegroundColor Yellow
    Write-Host "Старые JAR файлы сохранены в: $backupPath" -ForegroundColor Gray
} else {
    Write-Host "Сборка проекта успешно завершена!" -ForegroundColor Green
}

# Очистка временной директории (опционально)
Write-Host "`nВременная директория SDK сохранена в: $sdkTempPath" -ForegroundColor Gray
Write-Host "Для удаления выполните: Remove-Item -Recurse -Force $sdkTempPath" -ForegroundColor Gray

Write-Host "`n=== Обновление SDK завершено ===" -ForegroundColor Green

