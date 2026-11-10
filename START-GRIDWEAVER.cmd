@echo off
setlocal
cd /d "%~dp0"
call "%~dp0MAKE-EDITABLE.cmd" >nul

echo ============================================
echo        GRIDWEAVER LOCAL STACK
echo ============================================
echo.
echo Main dashboard : http://localhost:8080
echo Kafka UI       : http://localhost:8090
echo Prometheus     : http://localhost:9090
echo Grafana        : http://localhost:3000
echo.
echo Starting Docker Compose...
echo.

docker compose -f devops\docker-compose.yml up --build
