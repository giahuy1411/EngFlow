@echo off
REM ============================================================
REM  audit-v10 — chay toan bo phan kiem chung con lai
REM  Tao boi Claude Code khi harness classifier bi chet (blocker B1).
REM  Chay:  cd C:\Users\ASUS\Documents\LAPTRINH\engflow  &&  sweep\v10\run-all-audit-v10.bat
REM ============================================================
setlocal enabledelayedexpansion
cd /d "%~dp0..\.."

set EV=.specify\specs\audit-v10-full\evidence
if not exist "%EV%" mkdir "%EV%"

echo ============================================================
echo  BUOC 1/6 — Test F122 (StudyActivityService)
echo ============================================================
call mvnw.cmd -o -Dtest=StudyActivityServiceTest test > "%EV%\t1-study-activity.log" 2>&1
findstr /C:"Tests run:" /C:"BUILD SUCCESS" /C:"BUILD FAILURE" "%EV%\t1-study-activity.log"

echo.
echo ============================================================
echo  BUOC 2/6 — Test F122 tang SQL (can DB engflow_study_test)
echo  Script nay tu tao/xoa DB tam, va tu kiem 7 test phai xanh.
echo ============================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "tasks\streak-study\verify-sql.ps1"

echo.
echo ============================================================
echo  BUOC 3/6 — Test F115 (guard bai nhap)
echo ============================================================
call mvnw.cmd -o -Dtest=AuditV10DraftLessonSubmissionGuardTest test > "%EV%\t3-f115-guard.log" 2>&1
findstr /C:"Tests run:" /C:"BUILD SUCCESS" /C:"BUILD FAILURE" "%EV%\t3-f115-guard.log"

echo.
echo ============================================================
echo  BUOC 4/6 — Test F116/F117 (scheduler)
echo ============================================================
call mvnw.cmd -o -Dtest=StreakReminderSchedulerTest test > "%EV%\t4-scheduler.log" 2>&1
findstr /C:"Tests run:" /C:"BUILD SUCCESS" /C:"BUILD FAILURE" "%EV%\t4-scheduler.log"

echo.
echo ============================================================
echo  BUOC 5/6 — TOAN BO suite backend (doc so TU LOG)
echo ============================================================
call mvnw.cmd -o test > "%EV%\t5-backend-full.log" 2>&1
echo --- dong tong ket (doc tu log, KHONG doc XML) ---
findstr /C:"Tests run:" /C:"BUILD SUCCESS" /C:"BUILD FAILURE" "%EV%\t5-backend-full.log"

echo.
echo ============================================================
echo  BUOC 6/6 — Frontend: test + build
echo ============================================================
pushd frontend
call npx vitest run > "..\%EV%\t6-vitest.log" 2>&1
findstr /C:"Test Files" /C:"Tests " "..\%EV%\t6-vitest.log"
call npx vite build > "..\%EV%\t7-vite-build.log" 2>&1
findstr /C:"built in" /C:"dist/" "..\%EV%\t7-vite-build.log"
popd

echo.
echo ============================================================
echo  XONG. Log nam trong %EV%\
echo  DONG QUAN TRONG: neu buoc 1-2 KHONG xanh thi DUNG LAI,
echo  KHONG chay deploy.sql. Xem REPORT.md muc 8.
echo ============================================================
endlocal
