@echo off
cd /d "e:\AI\MemoryTree"

echo Compiling MemoryTreeLauncher.cs...
"C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe" /target:exe /out:"target\MemoryTree.exe" "MemoryTreeLauncher.cs" /r:System.dll

if %errorlevel% equ 0 (
    echo.
    echo SUCCESS: MemoryTree.exe created in target\
) else (
    echo.
    echo FAILED: Compilation error
)

pause
