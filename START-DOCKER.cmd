@echo off
setlocal
cd /d "%~dp0"

echo ===================================================
echo     GRIDWEAVER FULL DOCKER COMPOSE STACK
echo ===================================================
echo.
echo Checking Docker...
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not installed or Docker Desktop is not running.
    echo Please start Docker Desktop or use START-LOCAL.cmd instead.
    pause
    exit /b 1
)

echo Starting PostgreSQL, Redis, Kafka, Backend, Frontend, Prometheus, Grafana...
docker compose -f devops\docker-compose.yml up --build

pause
