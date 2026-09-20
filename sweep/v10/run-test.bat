@echo off
REM audit-v10 test runner. Usage: run-test.bat <TestClass> <outfile>
setlocal
cd /d "%~dp0.."
set OUT=%~2
if "%OUT%"=="" set OUT=.specify\specs\audit-v10-full\evidence\test-output.log
call mvnw.cmd -o -q test -Dtest=%1 > "%OUT%" 2>&1
echo EXITCODE=%ERRORLEVEL%
