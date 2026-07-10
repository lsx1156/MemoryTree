@echo off
chcp 65001 >nul
title MemoryTree Build Script

echo ================================================
echo          MemoryTree 构建脚本
echo ================================================
echo.

set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot
set MAVEN_OPTS=-Xmx2g

echo [1/4] 检查环境...
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo 错误: 未找到 JDK 21
    echo 请修改脚本中的 JAVA_HOME 路径
    pause
    exit /b 1
)

echo JDK路径: %JAVA_HOME%
"%JAVA_HOME%\bin\java.exe" -version
echo.

echo [2/4] 编译项目...
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo 错误: 编译失败
    pause
    exit /b 1
)
echo 编译成功
echo.

echo [3/4] 打包JAR...
call mvn package -DskipTests -q
if %errorlevel% neq 0 (
    echo 错误: 打包失败
    pause
    exit /b 1
)
echo JAR打包成功
echo.

echo [4/4] 生成EXE安装包...
echo 正在使用jpackage创建EXE...
"%JAVA_HOME%\bin\jpackage.exe" ^
    --type exe ^
    --name MemoryTree ^
    --input target ^
    --main-jar memorytree-3.1.0.jar ^
    --main-class com.memorytree.MemoryTreeApplication ^
    --dest target\jpackage ^
    --java-options "--add-opens java.base/java.lang=ALL-UNNAMED" ^
    --java-options "--add-opens java.base/java.util=ALL-UNNAMED" ^
    --win-console

if %errorlevel% neq 0 (
    echo 警告: EXE打包失败，将使用JAR方式运行
    goto :run_jar
)

echo EXE打包成功!
echo 安装包位置: target\jpackage\MemoryTree.exe
echo.
echo 是否立即启动应用? (Y/N)
set /p choice=
if /i "%choice%"=="Y" goto :run_exe
goto :end

:run_exe
echo 启动EXE应用...
start target\jpackage\MemoryTree.exe
goto :end

:run_jar
echo 启动JAR应用...
"%JAVA_HOME%\bin\java.exe" -jar target\memorytree-2.1.0.jar
goto :end

:end
echo.
echo 构建完成
pause
