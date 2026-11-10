@echo off
setlocal
cd /d "%~dp0"

echo ===================================================
echo     GRIDWEAVER COMPREHENSIVE TEST RUNNER
echo ===================================================
echo.

echo [1/3] Running Backend JUnit 5 & Integration Tests (22 tests)...
cd /d "%~dp0backend"
call mvn test
if errorlevel 1 (
    echo [FAIL] Backend tests failed!
    pause
    exit /b 1
)

echo.
echo [2/3] Running Frontend Vitest Suite...
cd /d "%~dp0frontend"
call npm run test
if errorlevel 1 (
    echo [FAIL] Frontend tests failed!
    pause
    exit /b 1
)

echo.
echo [3/3] Checking if live backend is running for E2E tests...
cd /d "%~dp0"
python -c "import urllib.request; urllib.request.urlopen('http://localhost:8080/api/grid/summary', timeout=2)" >nul 2>&1
if not errorlevel 1 (
    echo Running Live End-to-End Automated Test Suite...
    python tests\e2e_test.py http://localhost:8080
) else (
    echo [NOTE] Live backend not currently running on port 8080.
    echo Start the backend using START-LOCAL.cmd or START-DOCKER.cmd to run E2E live tests!
)

echo.
echo ===================================================
echo  ALL TEST PHASES COMPLETED SUCCESSFULLY!
echo ===================================================
pause
