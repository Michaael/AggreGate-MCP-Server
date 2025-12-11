# Simple Git Remote Setup Script
# Run this after installing Git

$SERVER_HOST = "155.212.171.244"
$SERVER_USER = "root"
$SERVER_REPO_PATH = "/root/aggregate_mcp.git"
$REMOTE_NAME = "server"

Write-Host "=== Git Remote Setup ===" -ForegroundColor Green

# Check if git is available
try {
    $null = git --version 2>&1
    Write-Host "Git found" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Git not found. Please install Git first." -ForegroundColor Red
    Write-Host "Download from: https://git-scm.com/download/win" -ForegroundColor Yellow
    exit 1
}

# Change to project directory
$projectPath = "C:\Users\micha\YandexDisk\aggregate_mcp"
if (Test-Path $projectPath) {
    Set-Location $projectPath
    Write-Host "Changed to project directory" -ForegroundColor Green
} else {
    Write-Host "ERROR: Project directory not found" -ForegroundColor Red
    exit 1
}

# Initialize git if needed
if (-not (Test-Path ".git")) {
    Write-Host "Initializing Git repository..." -ForegroundColor Yellow
    git init
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Failed to initialize repository" -ForegroundColor Red
        exit 1
    }
    Write-Host "Repository initialized" -ForegroundColor Green
}

# Check existing remotes
Write-Host "`nChecking existing remotes..." -ForegroundColor Yellow
$existingRemotes = git remote -v
if ($existingRemotes) {
    Write-Host "Existing remotes:" -ForegroundColor Cyan
    Write-Host $existingRemotes
}

# Check if remote exists
$remoteExists = git remote | Select-String -Pattern "^$REMOTE_NAME$"
if ($remoteExists) {
    Write-Host "`nRemote '$REMOTE_NAME' already exists." -ForegroundColor Yellow
    $response = Read-Host "Change URL? (y/n)"
    if ($response -eq "y" -or $response -eq "Y") {
        git remote set-url $REMOTE_NAME "$SERVER_USER@${SERVER_HOST}:$SERVER_REPO_PATH"
        Write-Host "Remote URL updated" -ForegroundColor Green
    }
} else {
    # Add remote
    Write-Host "`nAdding remote '$REMOTE_NAME'..." -ForegroundColor Yellow
    git remote add $REMOTE_NAME "$SERVER_USER@${SERVER_HOST}:$SERVER_REPO_PATH"
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Remote added successfully!" -ForegroundColor Green
    } else {
        Write-Host "ERROR: Failed to add remote" -ForegroundColor Red
        exit 1
    }
}

# Show configured remotes
Write-Host "`nCurrent remotes:" -ForegroundColor Cyan
git remote -v

Write-Host "`n=== Setup Complete ===" -ForegroundColor Green
Write-Host "`nNext steps:" -ForegroundColor Cyan
Write-Host "1. Configure user: git config user.name 'Your Name'" -ForegroundColor White
Write-Host "2. Configure email: git config user.email 'your.email@example.com'" -ForegroundColor White
Write-Host "3. Add files: git add ." -ForegroundColor White
Write-Host "4. Create commit: git commit -m 'Initial commit'" -ForegroundColor White
Write-Host "5. Push to server: git push -u $REMOTE_NAME main" -ForegroundColor White

