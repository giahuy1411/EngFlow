@echo off
REM ============================================================
REM  audit-v10 — BUOC 2 (SAU KHI buoc 1-2 o run-all xanh)
REM  Backup DB co verify + deploy schema study + verify schema.
REM  KHONG chay neu test chua xanh. Doc REPORT.md muc 8 truoc.
REM ============================================================
setlocal
cd /d "%~dp0..\.."

set EV=.specify\specs\audit-v10-full\evidence
set STAMP=%DATE:~-4%%DATE:~3,2%%DATE:~0,2%
set BAK=engflow_%STAMP%_audit-v10.bak

echo ============================================================
echo  BUOC A — Backup DB + RESTORE VERIFYONLY + SHA256
echo ============================================================
docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "%DB_PASSWORD%" -C -b -Q "BACKUP DATABASE english_learning TO DISK='/var/opt/mssql/backup/%BAK%' WITH COMPRESSION, CHECKSUM"
if errorlevel 1 goto :fail
docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "%DB_PASSWORD%" -C -b -Q "RESTORE VERIFYONLY FROM DISK='/var/opt/mssql/backup/%BAK%' WITH CHECKSUM"
if errorlevel 1 goto :fail
docker cp engflow-sqlserver:/var/opt/mssql/backup/%BAK% "C:\Users\ASUS\engflow-backups\%BAK%"
certutil -hashfile "C:\Users\ASUS\engflow-backups\%BAK%" SHA256 > "%EV%\backup-sha256.txt"
echo Backup + verify + SHA256 xong: %BAK%
type "%EV%\backup-sha256.txt"

echo.
echo ============================================================
echo  BUOC B — deploy.sql cutover 2026-09-20
echo ============================================================
docker exec -i engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "%DB_PASSWORD%" -C -d english_learning -b ^
  -v EffectiveFrom="2026-09-20" BackupFile="/var/opt/mssql/backup/%BAK%" ^
  -i /dev/stdin < tasks\streak-study\deploy.sql > "%EV%\deploy-study.log" 2>&1
findstr /C:"Msg " /C:"STUDY_POLICY_EFFECTIVE_FROM" "%EV%\deploy-study.log"
echo (Neu thay "Msg " o tren = CO LOI, dung lai.)

echo.
echo ============================================================
echo  BUOC C — Verify schema doc lap
echo ============================================================
docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "%DB_PASSWORD%" -C -d english_learning -h -1 -W -Q "SELECT 'TABLES='+STRING_AGG(name,',') FROM sys.tables WHERE name IN ('study_days','study_policy'); SELECT 'POLICY='+CONVERT(varchar(10),effective_from,23) FROM dbo.study_policy WHERE id=1; SELECT 'FK='+name FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID('dbo.study_days'); SELECT 'UQ='+name FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.study_days') AND is_unique=1 AND name IS NOT NULL;" > "%EV%\schema-verify.log" 2>&1
type "%EV%\schema-verify.log"

echo.
echo ============================================================
echo  BUOC D — Rebuild backend container
echo ============================================================
docker compose up -d --build backend

echo.
echo XONG buoc 2. Tiep theo: chay browser sweep (xem REPORT.md muc 8 buoc 11).
goto :eof

:fail
echo.
echo *** THAT BAI — DUNG LAI. Khong deploy. Kiem tra lai backup truoc. ***
exit /b 1
