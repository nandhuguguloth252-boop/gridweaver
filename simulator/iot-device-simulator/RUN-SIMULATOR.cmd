@echo off
setlocal
cd /d "%~dp0"

echo ===================================================
echo     GRIDWEAVER IOT TELEMETRY SIMULATOR
echo ===================================================
echo.
echo Select device simulation scale:
echo   1. 100 devices
echo   2. 1,000 devices
echo   3. 10,000 devices
echo   4. 50,000 devices
echo.

set /p choice="Enter choice [1-4] (default 2): "

if "%choice%"=="1" (
    set NODES=100
) else if "%choice%"=="3" (
    set NODES=10000
) else if "%choice%"=="4" (
    set NODES=50000
) else (
    set NODES=1000
)

echo Starting simulator with %NODES% devices...
python simulator.py --devices %NODES% --iterations 10
if errorlevel 1 (
    echo Python not found, trying Node.js...
    node simulator.js --devices %NODES% --iterations 10
)

pause
