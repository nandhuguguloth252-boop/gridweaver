@echo off
setlocal
cd /d "%~dp0"

echo ===================================================
echo     GRIDWEAVER VIRTUAL THREADS LOAD BENCHMARK
echo ===================================================
echo.
echo Running concurrency benchmark against GridWeaver at http://localhost:8080...
python tests\load\benchmark_load.py --url http://localhost:8080
if errorlevel 1 (
    echo.
    echo Make sure GridWeaver backend is running before executing benchmark!
)
pause
