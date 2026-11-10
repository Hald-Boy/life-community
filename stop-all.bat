@echo off
echo ================================================
echo   Stop all java / nacos / redis processes
echo   WARNING: this kills ALL java.exe (incl. IDEA)
echo ================================================

echo Stopping services (java.exe) ...
taskkill /F /IM java.exe >nul 2>&1

echo Stopping Nacos ...
taskkill /F /FI "WINDOWTITLE eq nacos*" >nul 2>&1

echo Stopping Redis ...
taskkill /F /IM redis-server.exe >nul 2>&1

echo.
echo Done. Note: IDEA will also be killed if it was running.
pause
