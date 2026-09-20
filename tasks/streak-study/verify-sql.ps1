$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$evidence = Join-Path $root ('target/study-sql-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $evidence | Out-Null

function Invoke-StudySql([string]$sql) {
    $output = $sql | docker exec -i engflow-sqlserver sh -c '/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d master -b'
    if ($LASTEXITCODE -ne 0 -or ($output -match '^Msg \d+')) { throw 'Study test SQL failed.' }
    return $output
}

$before = Invoke-StudySql @'
SET NOCOUNT ON;
SELECT 'DB_EXISTS=' + CAST(COUNT(*) AS varchar(10)) FROM sys.databases WHERE name=N'engflow_study_test';
SELECT 'LOGIN_EXISTS=' + CAST(COUNT(*) AS varchar(10)) FROM sys.server_principals WHERE name=N'engflow_study_test';
'@
$before | Set-Content -LiteralPath (Join-Path $evidence 'before.log')
if (($before -join ' ') -notmatch 'DB_EXISTS=0' -or ($before -join ' ') -notmatch 'LOGIN_EXISTS=0') {
    throw 'Refusing to reuse an existing database/login.'
}
$databaseCreated = $false
$loginCreated = $false
$previous = $env:ENGFLOW_STUDY_TEST_PASSWORD
$password = 'Study!' + [Guid]::NewGuid().ToString('N') + '9z'
$testExit = 1
Push-Location $root
try {
    Invoke-StudySql 'CREATE DATABASE [engflow_study_test];' | Out-Null
    $databaseCreated = $true
    Invoke-StudySql "CREATE LOGIN [engflow_study_test] WITH PASSWORD=N'$password', DEFAULT_DATABASE=[engflow_study_test], CHECK_POLICY=ON;" | Out-Null
    $loginCreated = $true
    Invoke-StudySql @'
USE [engflow_study_test];
CREATE USER [engflow_study_test] FOR LOGIN [engflow_study_test];
ALTER ROLE db_owner ADD MEMBER [engflow_study_test];
'@ | Out-Null
    $env:ENGFLOW_STUDY_TEST_PASSWORD = $password
    & .\mvnw.cmd test '-Dtest=StudySqlIntegrationTest' *> (Join-Path $evidence 'tests.log')
    $testExit = $LASTEXITCODE
} finally {
    $env:ENGFLOW_STUDY_TEST_PASSWORD = $previous
    if ($databaseCreated) {
        Invoke-StudySql 'ALTER DATABASE [engflow_study_test] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE [engflow_study_test];' | Out-Null
    }
    if ($loginCreated) { Invoke-StudySql 'DROP LOGIN [engflow_study_test];' | Out-Null }
    Pop-Location
}
$after = Invoke-StudySql @'
SET NOCOUNT ON;
SELECT 'DB_EXISTS=' + CAST(COUNT(*) AS varchar(10)) FROM sys.databases WHERE name=N'engflow_study_test';
SELECT 'LOGIN_EXISTS=' + CAST(COUNT(*) AS varchar(10)) FROM sys.server_principals WHERE name=N'engflow_study_test';
'@
$after | Set-Content -LiteralPath (Join-Path $evidence 'cleanup.log')
if (($after -join ' ') -notmatch 'DB_EXISTS=0' -or ($after -join ' ') -notmatch 'LOGIN_EXISTS=0') {
    throw 'Study test database/login residue remains.'
}
$log = Get-Content -LiteralPath (Join-Path $evidence 'tests.log')
$log | Select-String 'Tests run:|BUILD SUCCESS|BUILD FAILURE|^\[ERROR\]'
$after
Write-Output "EVIDENCE_DIRECTORY=$evidence"
# audit-v10 F122 added a seventh SQL test (beforeCutoverRecordsNoStudyDay...),
# so the expected count moves with the suite. Keep this an exact match rather
# than a ">= 6": a silently skipped test must still fail the gate.
if ($testExit -eq 0 -and ($log -join ' ') -notmatch 'Tests run: 7, Failures: 0, Errors: 0, Skipped: 0') {
    throw 'Expected seven executed SQL tests.'
}
exit $testExit
