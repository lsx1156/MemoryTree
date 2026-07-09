@echo off
chcp 65001 >nul
title MemoryTree AI Framework

echo ================================================
echo          MemoryTree AI Framework v2.1.0
echo ================================================
echo.

setlocal

echo [1] 检查Java环境...
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot"
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

if not exist "%JAVA_EXE%" (
    echo ERROR: Java 21 not found at %JAVA_HOME%
    echo.
    echo 请安装JDK 21并修改此脚本中的JAVA_HOME路径
    echo 下载地址: https://learn.microsoft.com/zh-cn/java/openjdk/download
    pause
    exit /b 1
)

echo Java路径: %JAVA_HOME%
"%JAVA_EXE%" -version
echo.

echo [2] 检查应用程序...
set "APP_DIR=%~dp0"
set "JAR_FILE=%APP_DIR%target\memorytree-2.1.0.jar"

if not exist "%JAR_FILE%" (
    echo ERROR: JAR文件未找到: %JAR_FILE%
    echo.
    echo 请先运行: mvn clean package -DskipTests
    pause
    exit /b 1
)

echo JAR路径: %JAR_FILE%
echo.

echo [3] 启动MemoryTree...
echo 按 Ctrl+C 停止服务
echo.

"%JAVA_EXE%" ^
    --add-opens java.base/java.lang=ALL-UNNAMED ^
    --add-opens java.base/java.util=ALL-UNNAMED ^
    -Xms512m ^
    -Xmx4096m ^
    -jar "%JAR_FILE%"

echo.
echo MemoryTree已停止
pause
