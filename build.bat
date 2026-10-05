@echo off
setlocal
cd /d "%~dp0"
for /f "delims=" %%B in ('git branch --show-current') do set "BRANCH=%%B"
if not defined BRANCH set "BRANCH=detached"
set "BRANCH=%BRANCH:/=-%"
set "JAR=build\berryngine-%BRANCH%.jar"

if not exist "build\classes" mkdir "build\classes"
if errorlevel 1 exit /b 1
dir /s /b "src\*.java" > "build\sources.txt"
javac -d "build\classes" @build\sources.txt
if errorlevel 1 exit /b 1
xcopy "src\berryngine\default_assets" "build\classes\berryngine\default_assets\" /E /I /Y >nul
if errorlevel 1 exit /b 1
jar --create --file "%JAR%" --main-class berryngine.TestScene -C "build\classes" .
if errorlevel 1 exit /b 1
echo Built %JAR%

if /I "%~1"=="test" (
    java -jar "%JAR%"
    exit /b
)
if /I "%~1"=="benchmark" (
    java -jar "%JAR%" --benchmark %~2 %~3
    exit /b
)
if not "%~1"=="" (
    echo Usage: build.bat [test ^| benchmark [frames] [threads]]
    exit /b 2
)
exit /b 0
