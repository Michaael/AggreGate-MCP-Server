# Инструкции по завершению настройки сервера

## ✅ Выполнено локально

1. ✅ SSH ключ сгенерирован
2. ✅ Публичный ключ сохранен в `ssh_public_key.txt`
3. ✅ Скрипт для сервера создан: `server_setup.sh`

## 🔧 Шаги для выполнения на сервере

### Вариант 1: Использовать готовый скрипт

1. **Скопируйте содержимое файла `server_setup.sh`**

2. **Подключитесь к серверу:**
   ```bash
   ssh root@155.212.171.244
   # Пароль: tN7qV1uT9qqP
   ```

3. **Вставьте и выполните скрипт:**
   ```bash
   # Скопируйте весь скрипт из server_setup.sh и вставьте в терминал
   ```

### Вариант 2: Выполнить команды вручную

1. **Подключитесь к серверу:**
   ```bash
   ssh root@155.212.171.244
   # Пароль: tN7qV1uT9qqP
   ```

2. **Настройте SSH ключ:**
   ```bash
   mkdir -p ~/.ssh
   chmod 700 ~/.ssh
   echo 'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDBU9IfbxMmExoliV11ZHmIYieZkoj0Wg+c77rA97QGTWjjd/pzfD3v2avnX5yMwMFhzQfsLYR08XlEgshggmhFfHgipSwxrG96pasQu5YLz0qFj0XxXlVLJDRO28WB6DRLz5LfgLOelbJQV3OvXmHsMl8NpOuX1FHCYL7o32P2ITPx29drgVh4wzUOnrqJY8dfxX+ajZ4i99NYTKqTIomZOLPST5OVTShdkvjU76r07Rg2CxEmX77zVvWgblmc9X36LO7LJ7fQnN2h75PROvSNQdnj2ammjaGbpJVpLt8ygdd5PIQPSDqZZcsgmL0b2aYBACQ10Z5+MmsFlGyum5lVYMAdpiXS6C/1uXfzfgbiN0X8SmkDUSXKivIFX9e4dEdPwkqgxe2Ep2YihNuNvnPPpobtVuRxPJTlonvWPOn0OeHluK2AdviUvLlll496zDmPtXzRY+btPVguzjZNAjYPG/1OjRA2fSCjJkVCcgllERrzlCvoYSUu+zUm8tgHJHbKFsPrNmOs7iYoxEwvJrBMrSOOZnqXOeHuwqAZPVwyHH+A33eGpdPyjHWYKxGXP8fYZ/U2b7UqjDmOeLCpGudKOULnT+LZ9RGp226fMBlgqVyLfS3J4eQB5z1Nj/ZM8FMSPZ4nCio/DjB2sfnASFqFqq1dsUXmBS4sjEdgj5XYdQ== micha@Berloga' >> ~/.ssh/authorized_keys
   chmod 600 ~/.ssh/authorized_keys
   ```

3. **Создайте Git репозиторий:**
   ```bash
   mkdir -p /root/aggregate_mcp.git
   cd /root/aggregate_mcp.git
   git init --bare
   exit
   ```

## ✅ После выполнения на сервере

После выполнения команд на сервере, вернитесь на локальную машину и выполните:

```bash
git push -u server master
```

Теперь push должен работать без запроса пароля!

## 📝 Проверка

После настройки проверьте подключение:

```bash
git ls-remote server
```

Если команда выполнится без ошибок, значит все настроено правильно.

