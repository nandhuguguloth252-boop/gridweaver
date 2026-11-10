@echo off
setlocal
cd /d "%~dp0"
attrib -R "%~dp0*" /S /D >nul 2>&1
echo GridWeaver files are now writable.
echo You can edit the source files normally.
pause
