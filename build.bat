@echo off
setlocal
cd /d "%~dp0"
for /f "delims=" %%B in ('git branch --show-current') do set "BRANCH=%%B"
if not defined BRANCH set "BRANCH=detached"
set "BRANCH=%BRANCH:/=-%"
set "JAR=build\berryngine-%BRANCH%.jar"
set "CLASSES=build\classes-%BRANCH%"

if not exist "%CLASSES%" mkdir "%CLASSES%"
if errorlevel 1 exit /b 1
dir /s /b "src\*.java" > "build\sources.txt"
javac -d "%CLASSES%" @build\sources.txt
if errorlevel 1 exit /b 1
xcopy "src\berryngine\default_assets" "%CLASSES%\berryngine\default_assets\" /E /I /Y >nul
if errorlevel 1 exit /b 1
if exist "%JAR%" (
    for /L %%R in (1,1,10) do (
        del /f /q "%JAR%" >nul 2>&1
        if not exist "%JAR%" goto jar_ready
        ping -n 2 127.0.0.1 >nul
    )
    echo Could not replace %JAR% because it is in use.
    exit /b 1
)
:jar_ready
jar --create --file "%JAR%" --main-class berryngine.TestScene -C "%CLASSES%" .
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
