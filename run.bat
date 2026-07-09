@echo off
chcp 65001 >nul
title MemoryTree - 记忆树AI框架

echo ================================================
echo          MemoryTree 启动脚本
echo ================================================
echo.

set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot

echo [1/3] 检查环境...
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo 错误: 未找到 JDK 21
    echo 请修改脚本中的 JAVA_HOME 路径
    pause
    exit /b 1
)

echo JDK路径: %JAVA_HOME%
echo.

echo [2/3] 检查Ollama服务...
curl -s http://localhost:11434/api/tags >nul 2>&1
if %errorlevel% neq 0 (
    echo 警告: 未检测到Ollama服务
    echo 请先启动: ollama serve
    echo.
)

echo [3/3] 启动MemoryTree...
echo.
echo 启动参数:
echo   - 模型: qwen2.5:7b
echo   - 端口: 8080
echo   - Ollama: http://localhost:11434
echo.
echo 按 Ctrl+C 退出
echo ================================================
echo.

"%JAVA_HOME%\bin\java.exe" ^
    --add-opens java.base/java.lang=ALL-UNNAMED ^
    --add-opens java.base/java.util=ALL-UNNAMED ^
    -jar target\memorytree-2.1.0.jar

pause
