@echo off
cd /d "%~dp0"
call gradlew.bat run
if errorlevel 1 pause
