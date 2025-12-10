@echo off
REM Volume Testing Runner with Web UI
REM Usage:
REM   run_volume_test_ui.bat <volume> <users> <spawn_rate>
REM
REM Examples:
REM   run_volume_test_ui.bat 10000 5 1     - Baseline: 10k listings, 5 users, 1 user/sec
REM   run_volume_test_ui.bat 25000 10 2    - Normal: 25k listings, 10 users, 2 users/sec
REM   run_volume_test_ui.bat 50000 20 5    - Maximum: 50k listings, 20 users, 5 users/sec
REM   run_volume_test_ui.bat 75000 30 5    - Stress: 75k listings, 30 users, 5 users/sec

echo ========================================
echo SmartSearch Volume Testing - Web UI Mode
echo ========================================
echo.

REM Check if all required parameters are provided
if "%1"=="" (
    echo ERROR: Missing required parameters
    echo.
    echo Usage: run_volume_test_ui.bat ^<volume^> ^<users^> ^<spawn_rate^>
    echo.
    echo Examples:
    echo   run_volume_test_ui.bat 10000 5 1     - Baseline test
    echo   run_volume_test_ui.bat 25000 10 2    - Normal usage
    echo   run_volume_test_ui.bat 50000 20 5    - Maximum expected
    echo   run_volume_test_ui.bat 75000 30 5    - Stress test
    echo.
    exit /b 1
)

if "%2"=="" (
    echo ERROR: Missing number of users parameter
    echo Usage: run_volume_test_ui.bat ^<volume^> ^<users^> ^<spawn_rate^>
    exit /b 1
)

if "%3"=="" (
    echo ERROR: Missing spawn rate parameter
    echo Usage: run_volume_test_ui.bat ^<volume^> ^<users^> ^<spawn_rate^>
    exit /b 1
)

REM Set parameters
set VOLUME=%1
set USERS=%2
set SPAWN_RATE=%3

echo Configuration:
echo   Volume Level: %VOLUME% listings
echo   Users: %USERS%
echo   Spawn Rate: %SPAWN_RATE% users/sec
echo   Duration: 15 minutes
echo ========================================
echo.

REM Preveri Python
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python is not installed
    exit /b 1
)

REM Preveri backend
echo Checking backend...
curl -s http://localhost:8080/api/management/status >nul 2>&1
if errorlevel 1 (
    echo ERROR: Backend not running
    echo.
    echo Start with:
    echo   cd ..\02_Backend
    echo   docker-compose -f docker-compose.prod.yml up -d
    echo.
    echo IMPORTANT: Make sure MongoDB ports are exposed in docker-compose.prod.yml!
    echo See README.md for details.
    exit /b 1
)
echo Backend is running!
echo.

REM Generate test data
echo ========================================
echo Generating %VOLUME% test listings...
echo ========================================
python VolumeData\generate_test_data.py %VOLUME%
if errorlevel 1 exit /b 1
echo.

REM Run Locust test
echo ========================================
echo Starting Locust test...
echo ========================================
echo.
echo Test will run for 15 minutes
echo Web UI will be accessible at: http://localhost:8089
echo.
echo Press Ctrl+C to stop early
echo.

cd Locust
locust -f locustfile_volume.py --host=http://localhost:8080 --users=%USERS% --spawn-rate=%SPAWN_RATE% --run-time=15m --html=..\Results\volume_test_ui_report_%VOLUME%_%date:~-4,4%%date:~-7,2%%date:~-10,2%_%time:~0,2%%time:~3,2%%time:~6,2%.html --headless

echo.
echo ========================================
echo Test Completed!
echo ========================================
echo Results saved to: Results\
echo.

cd ..
