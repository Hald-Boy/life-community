@echo off
echo ================================================
echo   Community Microservices - One Click Start
echo   Redis + Nacos + user/content/report services
echo ================================================

echo [1/5] Starting Redis ...
start "redis" /min "D:\Mysoft\IT\Redis\Redis-x64-3.2.100\redis-server.exe"

echo [2/5] Starting Nacos (standalone) ...
start "nacos" /min cmd /c "cd /d D:\Mysoft\IT\nacos\bin && startup.cmd -m standalone"

echo [3/5] Waiting Nacos init (~40s) ...
ping -n 45 127.0.0.1 >nul

echo [4/5] Starting 3 services ...
start "user-service"    /min java -jar "D:\Mysoft\IT\javaCode\news-community-cloud\user-service\target\user-service-1.0.0.jar"
start "content-service" /min java -jar "D:\Mysoft\IT\javaCode\news-community-cloud\content-service\target\content-service-1.0.0.jar"
start "report-service"  /min java -jar "D:\Mysoft\IT\javaCode\news-community-cloud\report-service\target\report-service-1.0.0.jar"

echo.
echo [5/5] All start commands sent. Ready in ~60s.
echo.
echo   Nacos console : http://localhost:8848/nacos  (nacos/nacos)
echo   user-service  : http://localhost:8081/swagger-ui.html
echo.
pause
