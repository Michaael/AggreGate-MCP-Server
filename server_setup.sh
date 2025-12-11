#!/bin/bash
# Server setup script for Git repository
# Run this script on the server: ssh root@155.212.171.244

# Setup SSH key
mkdir -p ~/.ssh
chmod 700 ~/.ssh
echo 'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDBU9IfbxMmExoliV11ZHmIYieZkoj0Wg+c77rA97QGTWjjd/pzfD3v2avnX5yMwMFhzQfsLYR08XlEgshggmhFfHgipSwxrG96pasQu5YLz0qFj0XxXlVLJDRO28WB6DRLz5LfgLOelbJQV3OvXmHsMl8NpOuX1FHCYL7o32P2ITPx29drgVh4wzUOnrqJY8dfxX+ajZ4i99NYTKqTIomZOLPST5OVTShdkvjU76r07Rg2CxEmX77zVvWgblmc9X36LO7LJ7fQnN2h75PROvSNQdnj2ammjaGbpJVpLt8ygdd5PIQPSDqZZcsgmL0b2aYBACQ10Z5+MmsFlGyum5lVYMAdpiXS6C/1uXfzfgbiN0X8SmkDUSXKivIFX9e4dEdPwkqgxe2Ep2YihNuNvnPPpobtVuRxPJTlonvWPOn0OeHluK2AdviUvLlll496zDmPtXzRY+btPVguzjZNAjYPG/1OjRA2fSCjJkVCcgllERrzlCvoYSUu+zUm8tgHJHbKFsPrNmOs7iYoxEwvJrBMrSOOZnqXOeHuwqAZPVwyHH+A33eGpdPyjHWYKxGXP8fYZ/U2b7UqjDmOeLCpGudKOULnT+LZ9RGp226fMBlgqVyLfS3J4eQB5z1Nj/ZM8FMSPZ4nCio/DjB2sfnASFqFqq1dsUXmBS4sjEdgj5XYdQ== micha@Berloga' >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# Create Git repository
mkdir -p /root/aggregate_mcp.git
cd /root/aggregate_mcp.git
git init --bare

echo "Setup completed successfully!"
echo "Repository created at: /root/aggregate_mcp.git"

