@echo off
chcp 65001 >nul
title KHOI DONG HE THONG THI VA GIAM SAT (SINH VIEN)

echo ========================================================
echo   HE THONG THI TRAC NGHIEM VA GIAM SAT PHONG MAY LAN
echo ========================================================
echo.

cd /d "%~dp0"

echo Dang khoi dong ung dung Lam bai thi (lan-student)...
if exist "lan-student\target\lan-student-1.0.1-jar-with-dependencies.jar" (
    start "LAN Student Exam" java -jar lan-student\target\lan-student-1.0.1-jar-with-dependencies.jar
) else (
    start "LAN Student Exam" java -jar lan-student\target\lan-student-1.0.0-jar-with-dependencies.jar
)

echo.
echo Vui long nhap IP Server tai man hinh dang nhap de he thong tu dong bat giam sat!
exit