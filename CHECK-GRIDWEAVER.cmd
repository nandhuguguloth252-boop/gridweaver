@echo off
setlocal
cd /d "%~dp0"
echo GridWeaver prerequisite check
echo.

docker --version >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Docker is not installed or Docker Desktop is not running.
  exit /b 1
)

docker compose version >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Docker Compose is not available.
  exit /b 1
)

echo [OK] Docker and Docker Compose are available.
echo.
echo Run START-GRIDWEAVER.cmd to launch the complete stack.
