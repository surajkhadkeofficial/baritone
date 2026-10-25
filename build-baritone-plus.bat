@echo off
REM ============================================================
REM  Baritone+ Build Script
REM  Builds all JAR variants: fabric, forge, neoforge, tweaker
REM ============================================================
echo.
echo  ====================================================
echo   BARITONE+ v1.13.1  -  MC 26.2 / Java 26
echo  ====================================================
echo.

cd /d "%~dp0"

echo [1/4] Cleaning previous build...
call gradlew.bat clean --no-daemon -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: Clean failed.
    pause
    exit /b 1
)

echo [2/4] Compiling sources...
call gradlew.bat compileJava --no-daemon -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: Compilation failed. Check output above.
    pause
    exit /b 1
)

echo [3/4] Building JARs...
call gradlew.bat build --no-daemon
if %ERRORLEVEL% neq 0 (
    echo ERROR: Build failed. Check output above.
    pause
    exit /b 1
)

echo.
echo  ====================================================
echo   Build SUCCESS!
echo   JARs are in: build\libs\
echo  ====================================================
echo.
dir /b build\libs\*.jar 2>nul || echo   (no JARs found)
echo.
pause
