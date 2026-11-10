@echo off
setlocal
cd /d "%~dp0"

echo ===================================================
echo     GRIDWEAVER LOCAL RUNNER (NO DOCKER NEEDED)
echo ===================================================
echo.

echo [1/3] Terminating any previous processes and clearing cache...
powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 8080, 5173 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }" >nul 2>&1
if exist "frontend\node_modules\.vite" rmdir /s /q "frontend\node_modules\.vite" >nul 2>&1
timeout /t 1 /nobreak >nul

set "PATH=%PATH%;C:\Users\hp\apache-maven-3.9.6\bin"

echo [2/3] Launching Java 21 Virtual Thread Backend (Port 8080)...
start "GridWeaver Backend" cmd /k "cd /d %~dp0backend && set PATH=%PATH%;C:\Users\hp\apache-maven-3.9.6\bin && mvn spring-boot:run"

echo [3/3] Launching React Dashboard (Port 5173)...
start "GridWeaver Frontend" cmd /k "cd /d %~dp0frontend && npx vite --force"

echo.
echo Waiting 5 seconds for services to initialize...
timeout /t 5 /nobreak >nul

echo Opening browser at http://127.0.0.1:5173 ...
start http://127.0.0.1:5173

echo.
echo ===================================================
echo   GridWeaver is now running!
echo     - Dashboard : http://127.0.0.1:5173
echo     - Backend   : http://localhost:8080
echo ===================================================
pause
