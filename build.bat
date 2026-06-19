@echo off
cd /d "%~dp0"
call gradlew assembleDebug --no-daemon
pause
