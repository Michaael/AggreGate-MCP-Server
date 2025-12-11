# Команды для настройки сервера

## Шаг 1: Подключение к серверу

```bash
ssh root@155.212.171.244
# Пароль: tN7qV1uT9qqP
```

## Шаг 2: Настройка SSH ключа

После подключения выполните:

```bash
mkdir -p ~/.ssh
chmod 700 ~/.ssh
```

Затем добавьте ваш публичный ключ (будет показан ниже):

```bash
echo 'ВАШ_ПУБЛИЧНЫЙ_КЛЮЧ' >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

## Шаг 3: Создание Git репозитория

```bash
mkdir -p /root/aggregate_mcp.git
cd /root/aggregate_mcp.git
git init --bare
exit
```

## Ваш публичный SSH ключ

Ключ будет добавлен ниже после генерации.

