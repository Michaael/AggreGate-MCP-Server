# Automated server setup script
# This script attempts to setup the Git repository on the server

$SERVER_HOST = "155.212.171.244"
$SERVER_USER = "root"
$SERVER_PASSWORD = "tN7qV1uT9qqP"
$REPO_PATH = "/root/aggregate_mcp.git"
$PUBLIC_KEY = Get-Content "ssh_public_key.txt" -Raw

Write-Host "=== Automated Server Setup ===" -ForegroundColor Green

# Read public key
if (-not $PUBLIC_KEY) {
    Write-Host "ERROR: Could not read public key from ssh_public_key.txt" -ForegroundColor Red
    exit 1
}

$PUBLIC_KEY = $PUBLIC_KEY.Trim()

Write-Host "`nAttempting to setup server..." -ForegroundColor Yellow
Write-Host "Server: $SERVER_USER@${SERVER_HOST}" -ForegroundColor Cyan
Write-Host "Repository: $REPO_PATH" -ForegroundColor Cyan

# Create a script to execute on the server
$serverScript = @"
#!/bin/bash
# Setup SSH key
mkdir -p ~/.ssh
chmod 700 ~/.ssh
echo '$PUBLIC_KEY' >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# Create Git repository
mkdir -p $REPO_PATH
cd $REPO_PATH
git init --bare

echo "Setup completed successfully"
"@

# Save script to temp file
$tempScript = [System.IO.Path]::GetTempFileName() + ".sh"
$serverScript | Out-File -FilePath $tempScript -Encoding ASCII -NoNewline

Write-Host "`nServer setup script created: $tempScript" -ForegroundColor Green
Write-Host "`nTo execute manually, run:" -ForegroundColor Yellow
Write-Host "ssh $SERVER_USER@${SERVER_HOST}" -ForegroundColor Cyan
Write-Host "# Password: $SERVER_PASSWORD" -ForegroundColor Gray
Write-Host "bash < $tempScript" -ForegroundColor White

# Try to execute using ssh (may require password input)
Write-Host "`nAttempting automated execution..." -ForegroundColor Yellow

try {
    # Try using ssh with password (requires sshpass or expect)
    # For Windows, we'll use a different approach
    $command = "cat `"$tempScript`" | ssh $SERVER_USER@${SERVER_HOST} 'bash'"
    
    Write-Host "Note: Automated execution requires manual password entry" -ForegroundColor Yellow
    Write-Host "You can copy the script content and paste it into SSH session" -ForegroundColor Yellow
    
    Write-Host "`n=== Script Content ===" -ForegroundColor Green
    Write-Host $serverScript -ForegroundColor White
    
} catch {
    Write-Host "Automated execution not available. Please run manually." -ForegroundColor Yellow
}

Write-Host "`n=== Manual Steps ===" -ForegroundColor Green
Write-Host "1. Connect: ssh $SERVER_USER@${SERVER_HOST}" -ForegroundColor Cyan
Write-Host "2. Password: $SERVER_PASSWORD" -ForegroundColor Gray
Write-Host "3. Run the commands shown above" -ForegroundColor Cyan

