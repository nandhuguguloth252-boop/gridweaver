@echo off
setlocal EnableExtensions
set "MAVEN_VERSION=3.9.16"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%\bin\mvn.cmd"

if exist "%MAVEN_BIN%" goto RUN_MAVEN

echo.
echo [GridWeaver] Maven %MAVEN_VERSION% was not found. Downloading it...
if not exist "%MAVEN_HOME%" mkdir "%MAVEN_HOME%"
set "MAVEN_ZIP=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip"

powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://dlcdn.apache.org/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%MAVEN_ZIP%'"
if errorlevel 1 (
  echo.
  echo [GridWeaver] Maven download failed. Check your internet connection.
  exit /b 1
)

if exist "%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%" rmdir /s /q "%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%MAVEN_ZIP%' '%MAVEN_HOME%'"
if errorlevel 1 (
  echo [GridWeaver] Maven extraction failed.
  exit /b 1
)

del /q "%MAVEN_ZIP%" >nul 2>&1

:RUN_MAVEN
call "%MAVEN_BIN%" %*
exit /b %ERRORLEVEL%
