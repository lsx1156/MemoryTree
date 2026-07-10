@echo off
chcp 65001 >nul
title MemoryTree EXE Builder

set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot
set MAVEN_OPTS=-Xmx2g

echo ================================================
echo          MemoryTree EXE 构建脚本
echo ================================================
echo.

echo [1/4] 检查环境...
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: JDK 21 not found
    echo Please modify JAVA_HOME in this script
    pause
    exit /b 1
)

echo JDK Path: %JAVA_HOME%
"%JAVA_HOME%\bin\java.exe" -version
echo.

echo [2/4] 编译打包项目...
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo ERROR: Package failed
    pause
    exit /b 1
)
echo Package successful
echo.

echo [3/4] 准备输入目录...
if exist "target\jpackage-input" rmdir /s /q "target\jpackage-input"
mkdir "target\jpackage-input"
copy "target\memorytree-3.0.0.jar" "target\jpackage-input\"
echo Done
echo.

echo [4/4] 创建EXE...
"%JAVA_HOME%\bin\jpackage.exe" ^
    --type app-image ^
    --name MemoryTree ^
    --input target\jpackage-input ^
    --main-jar memorytree-3.0.0.jar ^
    --dest release ^
    --java-options "-Xmx4g" ^
    --java-options "-Xms512m" ^
    --java-options "--add-opens java.base/java.lang=ALL-UNNAMED" ^
    --java-options "--add-opens java.base/java.util=ALL-UNNAMED" ^
    --java-options "--add-opens java.base/java.lang.reflect=ALL-UNNAMED" ^
    --java-options "-Dspring.main.web-application-type=none" ^
    --win-console

if %errorlevel% neq 0 (
    echo ERROR: EXE build failed
    pause
    exit /b 1
)

echo.
echo ================================================
echo EXE Build Successful!
echo Location: release\MemoryTree\MemoryTree.exe
echo ================================================
echo.
echo Press any key to exit...
pause
