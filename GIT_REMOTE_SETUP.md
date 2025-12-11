# Настройка Git Remote для сервера

## Данные сервера

- **Хост**: 155.212.171.244
- **Пользователь**: root
- **Пароль**: tN7qV1uT9qqP

## Шаги настройки

### 1. Инициализация Git репозитория (если еще не инициализирован)

```bash
cd C:\Users\micha\YandexDisk\aggregate_mcp
git init
```

### 2. Настройка пользователя Git (если еще не настроено)

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

Или локально для этого репозитория:

```bash
git config user.name "Your Name"
git config user.email "your.email@example.com"
```

### 3. Добавление remote для сервера

#### Вариант A: Если на сервере есть git репозиторий

```bash
# Если репозиторий на сервере находится в /root/aggregate_mcp.git
git remote add server ssh://root@155.212.171.244/root/aggregate_mcp.git

# Или если это обычный путь
git remote add server root@155.212.171.244:/root/aggregate_mcp.git
```

#### Вариант B: Если нужно создать репозиторий на сервере

Сначала подключитесь к серверу:
```bash
ssh root@155.212.171.244
```

На сервере создайте bare репозиторий:
```bash
mkdir -p /root/aggregate_mcp.git
cd /root/aggregate_mcp.git
git init --bare
exit
```

Затем на локальной машине добавьте remote:
```bash
git remote add server root@155.212.171.244:/root/aggregate_mcp.git
```

#### Вариант C: Если репозиторий на сервере в другом месте

Уточните путь к репозиторию на сервере и используйте:
```bash
git remote add server root@155.212.171.244:/path/to/repository.git
```

### 4. Проверка настроенных remote

```bash
git remote -v
```

Должно показать:
```
server  root@155.212.171.244:/root/aggregate_mcp.git (fetch)
server  root@155.212.171.244:/root/aggregate_mcp.git (push)
```

### 5. Первый коммит и push (если нужно)

```bash
# Добавить все файлы
git add .

# Создать первый коммит
git commit -m "Initial commit"

# Отправить на сервер
git push -u server main
# или
git push -u server master
```

## Использование SSH ключей (рекомендуется)

Вместо пароля лучше использовать SSH ключи:

### Генерация ключа (на локальной машине)

```bash
ssh-keygen -t rsa -b 4096 -C "your.email@example.com"
```

### Копирование ключа на сервер

```bash
ssh-copy-id root@155.212.171.244
```

Или вручную:
```bash
type $env:USERPROFILE\.ssh\id_rsa.pub | ssh root@155.212.171.244 "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
```

После этого можно использовать git без ввода пароля.

## Проверка подключения

```bash
# Проверить SSH подключение
ssh root@155.212.171.244 "echo 'Connection successful'"

# Проверить доступ к git на сервере
ssh root@155.212.171.244 "git --version"
```

## Полезные команды

```bash
# Получить изменения с сервера
git fetch server

# Отправить изменения на сервер
git push server main

# Получить и объединить изменения
git pull server main

# Просмотр всех remote
git remote -v

# Удаление remote (если нужно)
git remote remove server

# Изменение URL remote
git remote set-url server root@155.212.171.244:/new/path/to/repo.git
```

## ⚠️ Важные замечания

1. **Безопасность**: Пароль хранится в открытом виде. Используйте SSH ключи.
2. **Путь на сервере**: Убедитесь, что путь к репозиторию на сервере правильный.
3. **Права доступа**: Убедитесь, что у пользователя root есть права на запись в директорию репозитория.

