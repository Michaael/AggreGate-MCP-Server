# Скрипт для настройки Git Remote для сервера
# Использование: .\setup_git_remote.ps1

$SERVER_HOST = "155.212.171.244"
$SERVER_USER = "root"
$SERVER_REPO_PATH = "/root/aggregate_mcp.git"
$REMOTE_NAME = "server"

Write-Host "=== Настройка Git Remote ===" -ForegroundColor Green

# Проверка наличия git
try {
    $gitVersion = git --version
    Write-Host "Git найден: $gitVersion" -ForegroundColor Green
} catch {
    Write-Host "ОШИБКА: Git не найден. Установите Git и добавьте его в PATH." -ForegroundColor Red
    exit 1
}

# Переход в директорию проекта
$projectPath = "C:\Users\micha\YandexDisk\aggregate_mcp"
if (Test-Path $projectPath) {
    Set-Location $projectPath
    Write-Host "Переход в директорию проекта: $projectPath" -ForegroundColor Green
} else {
    Write-Host "ОШИБКА: Директория проекта не найдена: $projectPath" -ForegroundColor Red
    exit 1
}

# Проверка инициализации git репозитория
if (-not (Test-Path ".git")) {
    Write-Host "Инициализация Git репозитория..." -ForegroundColor Yellow
    git init
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ОШИБКА: Не удалось инициализировать репозиторий" -ForegroundColor Red
        exit 1
    }
    Write-Host "Репозиторий инициализирован" -ForegroundColor Green
} else {
    Write-Host "Git репозиторий уже инициализирован" -ForegroundColor Green
}

# Проверка существующих remote
Write-Host "`nПроверка существующих remote..." -ForegroundColor Yellow
$existingRemotes = git remote -v
if ($existingRemotes) {
    Write-Host "Существующие remote:" -ForegroundColor Cyan
    Write-Host $existingRemotes
}

# Проверка наличия remote с таким именем
$remoteExists = git remote | Select-String -Pattern "^$REMOTE_NAME$"
if ($remoteExists) {
    Write-Host "`nRemote '$REMOTE_NAME' уже существует." -ForegroundColor Yellow
    $response = Read-Host "Хотите изменить URL? (y/n)"
    if ($response -eq "y" -or $response -eq "Y") {
        git remote set-url $REMOTE_NAME "$SERVER_USER@${SERVER_HOST}:$SERVER_REPO_PATH"
        Write-Host "URL remote обновлен" -ForegroundColor Green
    }
} else {
    # Добавление remote
    Write-Host "`nДобавление remote '$REMOTE_NAME'..." -ForegroundColor Yellow
    git remote add $REMOTE_NAME "$SERVER_USER@${SERVER_HOST}:$SERVER_REPO_PATH"
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Remote успешно добавлен!" -ForegroundColor Green
    } else {
        Write-Host "ОШИБКА: Не удалось добавить remote" -ForegroundColor Red
        exit 1
    }
}

# Проверка настроенных remote
Write-Host "`nТекущие remote:" -ForegroundColor Cyan
git remote -v

# Проверка SSH подключения
Write-Host "`nПроверка SSH подключения к серверу..." -ForegroundColor Yellow
$sshTest = ssh -o ConnectTimeout=5 -o BatchMode=yes "$SERVER_USER@${SERVER_HOST}" "echo 'Connection successful'" 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "SSH подключение успешно!" -ForegroundColor Green
} else {
    Write-Host "Предупреждение: SSH подключение не удалось автоматически" -ForegroundColor Yellow
    Write-Host "Возможно, потребуется ввести пароль при первом подключении" -ForegroundColor Yellow
    Write-Host "Пароль: tN7qV1uT9qqP" -ForegroundColor Yellow
}

Write-Host "`n=== Настройка завершена ===" -ForegroundColor Green
Write-Host "`nСледующие шаги:" -ForegroundColor Cyan
Write-Host "1. Настройте пользователя Git: git config user.name 'Your Name'" -ForegroundColor White
Write-Host "2. Настройте email: git config user.email 'your.email@example.com'" -ForegroundColor White
Write-Host "3. Добавьте файлы: git add ." -ForegroundColor White
Write-Host "4. Создайте коммит: git commit -m 'Initial commit'" -ForegroundColor White
Write-Host "5. Отправьте на сервер: git push -u $REMOTE_NAME main" -ForegroundColor White

