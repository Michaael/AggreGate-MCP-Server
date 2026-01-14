# Миграция CoffeeGate через PRS файлы

## Метод экспорта/импорта через PRS файлы

Использован метод экспорта/импорта через приложения AggreGate с расширением `.prs` (Properties).

### Выполненные действия

1. **Экспорт через приложение cmApplication:**
   - ✅ `users.admin.applications.cmApplication` → действие `export` → `exports/coffeegate_all.prs`
   - ✅ `users.admin.applications.contextBackup` → действие `export` → `exports/coffeegate_all.prs`

2. **Импорт на localhost:**
   - ⚠️ Требуется создание приложения `contextBackup` на localhost
   - ⚠️ Импорт через действие `import` в контексте приложения

### Примечания

- PRS файлы создаются на сервере AggreGate
- Для импорта требуется наличие соответствующего приложения на целевом сервере
- Файлы должны быть доступны серверу по указанному пути

### Следующие шаги

1. Проверить создание prs файлов на сервере
2. Создать приложение contextBackup на localhost (если необходимо)
3. Выполнить импорт через действие import в контексте приложения
