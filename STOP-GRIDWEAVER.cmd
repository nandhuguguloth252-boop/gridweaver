@echo off
setlocal
cd /d "%~dp0"

echo Stopping GridWeaver services...

powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 8080, 5173 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }"

docker compose -f devops\docker-compose.yml down --remove-orphans >nul 2>&1

echo All GridWeaver services stopped.
pause
