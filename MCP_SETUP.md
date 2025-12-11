# Настройка MCP сервера для Cursor

## Статус сборки

✅ Проект успешно собран
✅ JAR файл создан: `mcp-server/build/libs/aggregate-mcp-server-1.0.0.jar`
✅ Конфигурация MCP создана: `%USERPROFILE%\.cursor\mcp.json`

## Подключение к Cursor

### Автоматическая настройка

Конфигурационный файл уже создан в `%USERPROFILE%\.cursor\mcp.json`. 

### Проверка конфигурации

Файл конфигурации находится по пути:
```
C:\Users\micha\.cursor\mcp.json
```

Содержимое:
```json
{
  "mcpServers": {
    "aggregate": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\Users\\micha\\YandexDisk\\aggregate_mcp\\mcp-server\\build\\libs\\aggregate-mcp-server-1.0.0.jar"
      ]
    }
  }
}
```

### Активация в Cursor

1. **Перезапустите Cursor IDE** - это необходимо для загрузки новой конфигурации MCP
2. После перезапуска откройте чат в Cursor
3. MCP сервер должен автоматически подключиться

### Проверка подключения

После перезапуска Cursor попробуйте использовать инструменты AggreGate:
- `aggregate_connect` - для подключения к AggreGate серверу
- `aggregate_list_contexts` - для получения списка контекстов
- И другие инструменты из списка доступных

### Устранение проблем

Если MCP сервер не подключается:

1. **Проверьте Java:**
   ```powershell
   java -version
   ```
   Должна быть версия Java 8 или выше

2. **Проверьте JAR файл:**
   ```powershell
   Test-Path "C:\Users\micha\YandexDisk\aggregate_mcp\mcp-server\build\libs\aggregate-mcp-server-1.0.0.jar"
   ```

3. **Проверьте конфигурацию:**
   ```powershell
   Get-Content "$env:USERPROFILE\.cursor\mcp.json"
   ```

4. **Проверьте логи Cursor:**
   - Откройте Developer Tools (Ctrl+Shift+I)
   - Проверьте консоль на наличие ошибок MCP

### Ручная настройка (если автоматическая не сработала)

Если конфигурация не была создана автоматически:

1. Создайте директорию `.cursor` в вашей домашней папке (если её нет):
   ```powershell
   New-Item -ItemType Directory -Path "$env:USERPROFILE\.cursor" -Force
   ```

2. Создайте файл `mcp.json` в этой директории с содержимым:
   ```json
   {
     "mcpServers": {
       "aggregate": {
         "command": "java",
         "args": [
           "-jar",
           "C:\\Users\\micha\\YandexDisk\\aggregate_mcp\\mcp-server\\build\\libs\\aggregate-mcp-server-1.0.0.jar"
         ]
       }
     }
   }
   ```

3. **Важно:** Замените путь к JAR файлу на актуальный путь на вашей системе

4. Перезапустите Cursor IDE

## Пересборка проекта

Если вы внесли изменения в код MCP сервера:

```powershell
cd "C:\Users\micha\YandexDisk\aggregate_mcp"
$env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_161"
.\gradlew.bat :mcp-server:clean :mcp-server:build --no-daemon
```

JAR файл будет обновлен в `mcp-server/build/libs/aggregate-mcp-server-1.0.0.jar`

## Дополнительная информация

- Полная документация: [docs/mcp-server/COMPLETE_GUIDE.md](docs/mcp-server/COMPLETE_GUIDE.md)
- Быстрый старт: [docs/mcp-server/README.md](docs/mcp-server/README.md)

