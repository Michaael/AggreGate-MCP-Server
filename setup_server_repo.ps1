# Script to setup Git repository on server and configure SSH keys
# Usage: .\setup_server_repo.ps1

$SERVER_HOST = "155.212.171.244"
$SERVER_USER = "root"
$SERVER_PASSWORD = "tN7qV1uT9qqP"
$REPO_PATH = "/root/aggregate_mcp.git"
$SSH_KEY_PATH = "$env:USERPROFILE\.ssh\id_rsa.pub"

Write-Host "=== Setting up Git repository on server ===" -ForegroundColor Green

# Step 1: Check if SSH key exists
if (-not (Test-Path $SSH_KEY_PATH)) {
    Write-Host "`nGenerating SSH key..." -ForegroundColor Yellow
    ssh-keygen -t rsa -b 4096 -f "$env:USERPROFILE\.ssh\id_rsa" -N '""' -q
    Write-Host "SSH key generated" -ForegroundColor Green
} else {
    Write-Host "`nSSH key already exists" -ForegroundColor Green
}

# Step 2: Read public key
Write-Host "`nReading public key..." -ForegroundColor Yellow
$publicKey = Get-Content $SSH_KEY_PATH -ErrorAction SilentlyContinue
if ($publicKey) {
    Write-Host "Public key found" -ForegroundColor Green
} else {
    Write-Host "ERROR: Could not read public key" -ForegroundColor Red
    exit 1
}

# Step 3: Copy SSH key to server
Write-Host "`nCopying SSH key to server..." -ForegroundColor Yellow
Write-Host "This will require entering the password: $SERVER_PASSWORD" -ForegroundColor Cyan

# Create a temporary script to add the key
$tempScript = [System.IO.Path]::GetTempFileName()
$scriptContent = @"
mkdir -p ~/.ssh
chmod 700 ~/.ssh
echo '$publicKey' >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
echo 'SSH key added successfully'
"@
$scriptContent | Out-File -FilePath $tempScript -Encoding ASCII

try {
    # Use plink or ssh with password (requires sshpass or similar)
    # For Windows, we'll use a different approach
    Write-Host "Attempting to copy key using SSH..." -ForegroundColor Yellow
    
    # Try using ssh with expect-like behavior
    $sshCommand = "echo '$SERVER_PASSWORD' | ssh -o StrictHostKeyChecking=no $SERVER_USER@${SERVER_HOST} 'bash -s' < $tempScript"
    
    Write-Host "`nManual steps required:" -ForegroundColor Yellow
    Write-Host "1. Connect to server: ssh $SERVER_USER@${SERVER_HOST}" -ForegroundColor Cyan
    Write-Host "2. Password: $SERVER_PASSWORD" -ForegroundColor Cyan
    Write-Host "3. Run these commands:" -ForegroundColor Cyan
    Write-Host "   mkdir -p ~/.ssh" -ForegroundColor White
    Write-Host "   chmod 700 ~/.ssh" -ForegroundColor White
    Write-Host "   echo '$publicKey' >> ~/.ssh/authorized_keys" -ForegroundColor White
    Write-Host "   chmod 600 ~/.ssh/authorized_keys" -ForegroundColor White
    Write-Host "   mkdir -p $REPO_PATH" -ForegroundColor White
    Write-Host "   cd $REPO_PATH" -ForegroundColor White
    Write-Host "   git init --bare" -ForegroundColor White
    Write-Host "   exit" -ForegroundColor White
    
} finally {
    if (Test-Path $tempScript) {
        Remove-Item $tempScript -Force
    }
}

Write-Host "`n=== Setup instructions ===" -ForegroundColor Green
Write-Host "`nYour public SSH key:" -ForegroundColor Cyan
Write-Host $publicKey -ForegroundColor White

