@echo off
chcp 65001 >nul
title KHOI DONG HE THONG SERVER VA ADMIN (GIAM THI)

echo ========================================================
echo   HE THONG SERVER VA GIAO DIEN GIAM SAT (GIAM THI)
echo ========================================================
echo.

cd /d "%~dp0"

echo [1/2] Dang khoi dong Server Hub (lan-server: Port 9999)...
if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    start "LAN Server Hub" "C:\Program Files\Java\jdk-21\bin\java.exe" -jar lan-server\target\lan-server-0.0.3-SNAPSHOT.jar
) else (
    start "LAN Server Hub" java -jar lan-server\target\lan-server-0.0.3-SNAPSHOT.jar
)

echo Dang cho Server khoi dong...
timeout /t 5 /nobreak >nul

echo [2/2] Dang khoi dong Giao dien Quan tri (lan-admin)...
if exist "lan-admin\target\lan-admin-1.0.1-jar-with-dependencies.jar" (
    start "LAN Admin Dashboard" java -jar lan-admin\target\lan-admin-1.0.1-jar-with-dependencies.jar
) else (
    start "LAN Admin Dashboard" java -jar lan-admin\target\lan-admin-1.0.0-jar-with-dependencies.jar
)

echo.
echo Da khoi dong ca Server va Admin thanh cong!
echo (Thong tin dang nhap mac dinh: admin / 123456)
exit

