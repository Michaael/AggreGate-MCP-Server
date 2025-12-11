# Git Setup Instructions

## Current Status

Git is not currently installed or not in PATH on your system.

## Step 1: Install Git

Download and install Git for Windows from:
https://git-scm.com/download/win

After installation, restart your terminal/PowerShell.

## Step 2: Run Setup Script

After Git is installed, run the setup script:

```powershell
cd C:\Users\micha\YandexDisk\aggregate_mcp
.\setup_git_simple.ps1
```

## Step 3: Manual Setup (Alternative)

If you prefer to set up manually:

```bash
# 1. Initialize repository
git init

# 2. Configure user (required for commits)
git config user.name "Your Name"
git config user.email "your.email@example.com"

# 3. Add remote server
git remote add server root@155.212.171.244:/root/aggregate_mcp.git

# 4. Verify remote
git remote -v
```

## Step 4: Prepare Server Repository

Before pushing, ensure the repository exists on the server:

```bash
# Connect to server
ssh root@155.212.171.244
# Password: tN7qV1uT9qqP

# Create bare repository on server
mkdir -p /root/aggregate_mcp.git
cd /root/aggregate_mcp.git
git init --bare
exit
```

## Step 5: First Push

```bash
# Add all files
git add .

# Create initial commit
git commit -m "Initial commit"

# Push to server
git push -u server main
```

## Server Information

- **Host**: 155.212.171.244
- **User**: root
- **Password**: tN7qV1uT9qqP
- **Repository Path**: /root/aggregate_mcp.git

## Files Created

1. **`.gitignore`** - Git ignore rules
2. **`.git/config`** - Git configuration (will be created when you run `git init`)
3. **`setup_git_simple.ps1`** - Automated setup script
4. **`GIT_REMOTE_SETUP.md`** - Detailed documentation

## Troubleshooting

### Git not found
- Install Git from https://git-scm.com/download/win
- Add Git to PATH during installation
- Restart terminal after installation

### SSH connection issues
- First connection will require password: `tN7qV1uT9qqP`
- Consider setting up SSH keys for passwordless access

### Permission denied
- Ensure repository path on server exists
- Check file permissions on server
- Verify SSH access works: `ssh root@155.212.171.244`

