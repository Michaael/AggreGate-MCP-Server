# Инструкция по сборке SDK из репозитория GitHub

## Проблема

При сборке SDK из репозитория https://github.com/Michaael/AggreGate-SDK-CE возникает ошибка компиляции из-за отсутствия библиотеки `javacsv`.

## Решение

### Вариант 1: Использовать уже собранные JAR из sdk-temp (рекомендуется)

Если у вас уже есть собранные JAR файлы в `sdk-temp`, используйте их:

```powershell
# Создать резервную копию
$backupDir = "libs-backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
New-Item -ItemType Directory -Path $backupDir -Force

# Скопировать старые JAR
Copy-Item "libs\aggregate-api.jar" -Destination "$backupDir\aggregate-api.jar.backup" -Force
Copy-Item "libs\aggregate-api-libs.jar" -Destination "$backupDir\aggregate-api-libs.jar.backup" -Force

# Обновить из sdk-temp
Copy-Item "sdk-temp\aggregate-api\build\libs\aggregate-api.jar" -Destination "libs\aggregate-api.jar" -Force
```

### Вариант 2: Собрать SDK с исправлением зависимостей

1. **Скачать javacsv**:
   ```powershell
   New-Item -ItemType Directory -Path "aggregate-sdk-ce\libs" -Force
   Invoke-WebRequest -Uri "https://sourceforge.net/projects/javacsv/files/javacsv/2.1/javacsv-2.1.jar/download" -OutFile "aggregate-sdk-ce\libs\javacsv-2.1.jar"
   ```

2. **Убедиться, что build.gradle.kts правильно настроен**:
   Файл `aggregate-sdk-ce/aggregate-api/build.gradle.kts` должен содержать:
   ```kotlin
   // Используем локальные файлы из libs для javacsv и jpf
   val libsDir = file("../libs")
   if (libsDir.exists() && libsDir.isDirectory) {
       libsDir.listFiles()?.filter { it.name.endsWith(".jar") }?.forEach { jarFile ->
           api(files(jarFile))
       }
   }
   ```

3. **Собрать SDK**:
   ```powershell
   cd aggregate-sdk-ce
   .\gradlew.bat :aggregate-api:jar :widget-api:jar -x test --no-daemon
   ```

4. **Скопировать собранные JAR**:
   ```powershell
   Copy-Item "aggregate-api\build\libs\aggregate-api.jar" -Destination "..\libs\aggregate-api.jar" -Force
   ```

### Вариант 3: Использовать скрипт update-sdk.ps1

Запустите скрипт обновления SDK:
```powershell
.\update-sdk.ps1
```

## Текущий статус

- ✅ Репозиторий клонирован в `aggregate-sdk-ce/`
- ✅ javacsv скачан в `aggregate-sdk-ce/libs/javacsv-2.1.jar`
- ⚠️ Требуется проверка конфигурации build.gradle.kts для правильного подключения javacsv
- ⚠️ Альтернатива: использовать уже собранные JAR из `sdk-temp/` если они есть

## Рекомендация

Если у вас уже есть рабочие JAR файлы в `sdk-temp/aggregate-api/build/libs/`, используйте их вместо сборки из GitHub репозитория, так как они уже собраны с правильными зависимостями.

