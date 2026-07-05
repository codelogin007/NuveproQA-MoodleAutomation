@echo off
REM Regenerate the traceability/execution report after a test run.
REM Usage:  report          (after a fast run:  mvnw test)
REM         report slow     (after a slow run:  mvnw test -Pslow)
cd /d "%~dp0"

set PY=D:\Projects\Nuvepro\Moodle\automation\.venv\Scripts\python.exe

if not exist target\cucumber.json (
    echo [report] target\cucumber.json not found - run "mvnw test" first.
    exit /b 1
)

if /i "%1"=="slow" (
    copy /y target\cucumber.json target\cucumber_slow.json >nul
    echo [report] saved slow-run results.
) else (
    copy /y target\cucumber.json target\cucumber_fast.json >nul
    echo [report] saved fast-run results.
)

"%PY%" tools\traceability.py
if errorlevel 1 exit /b 1

echo.
echo [report] Open: %~dp0reports\traceability_execution.html
start "" "%~dp0reports\traceability_execution.html"
