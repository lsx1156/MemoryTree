@echo off
chcp 65001 >nul
title MemoryTree Test Runner

set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
set MAVEN_OPTS=-Xmx2g

echo ================================================
echo          MemoryTree 单元测试
echo ================================================
echo.
echo JDK Path: %JAVA_HOME%
"%JAVA_HOME%\bin\java.exe" -version
echo.

echo [1/1] 运行测试...
call mvn test
if %errorlevel% neq 0 (
    echo.
    echo ================================================
    echo   测试失败，请查看上方错误信息
    echo ================================================
    pause
    exit /b 1
)

echo.
echo ================================================
echo   测试全部通过！
echo ================================================
echo.
pause
