param()

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$runId = Get-Date -Format 'yyyyMMdd-HHmmss'
$logDirectory = Join-Path $root "target/audit-v9-srs-$runId"
New-Item -ItemType Directory -Path $logDirectory | Out-Null

function Invoke-AuditSql([string]$Sql) {
    $output = $Sql | docker exec -i engflow-sqlserver sh -c '/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d master -b'
    if ($LASTEXITCODE -ne 0 -or ($output -match '^Msg \d+')) {
        throw 'Audit SQL failed; inspect database/login state before retrying.'
    }
    return $output
}

$state = Invoke-AuditSql @'
SET NOCOUNT ON;
SELECT 'AUDIT_DB_EXISTS=' + CAST(COUNT(*) AS varchar(10)) FROM sys.databases WHERE name=N'english_learning_audit_v9_srs';
SELECT 'AUDIT_LOGIN_EXISTS=' + CAST(COUNT(*) AS varchar(10)) FROM sys.server_principals WHERE name=N'audit_v9_srs';
'@
if (($state -join ' ') -notmatch 'AUDIT_DB_EXISTS=0' -or
    ($state -join ' ') -notmatch 'AUDIT_LOGIN_EXISTS=0') {
    throw 'Refusing to reuse or delete an existing audit DB/login.'
}

$createdDatabase = $false
$createdLogin = $false
$testExit = 1
$previousPassword = $env:ENGFLOW_AUDIT_JPA_PASSWORD
$password = 'Audit!' + [Guid]::NewGuid().ToString('N') + '9a'
Push-Location $root
try {
    Invoke-AuditSql 'CREATE DATABASE [english_learning_audit_v9_srs];' | Out-Null
    $createdDatabase = $true
    Invoke-AuditSql "CREATE LOGIN [audit_v9_srs] WITH PASSWORD=N'$password', DEFAULT_DATABASE=[english_learning_audit_v9_srs], CHECK_POLICY=ON;" | Out-Null
    $createdLogin = $true
    Invoke-AuditSql @'
USE [english_learning_audit_v9_srs];
CREATE USER [audit_v9_srs] FOR LOGIN [audit_v9_srs];
ALTER ROLE db_owner ADD MEMBER [audit_v9_srs];
'@ | Out-Null
    $env:ENGFLOW_AUDIT_JPA_PASSWORD = $password
    & .\mvnw.cmd test '-Dtest=AuditV9SrsIntervalOverflowTest' *> (Join-Path $logDirectory 'tests.log')
    $testExit = $LASTEXITCODE
} finally {
    $env:ENGFLOW_AUDIT_JPA_PASSWORD = $previousPassword
    if ($createdDatabase) {
        Invoke-AuditSql @'
ALTER DATABASE [english_learning_audit_v9_srs] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
DROP DATABASE [english_learning_audit_v9_srs];
'@ | Out-Null
    }
    if ($createdLogin) {
        Invoke-AuditSql 'DROP LOGIN [audit_v9_srs];' | Out-Null
    }
    Pop-Location
}

$residue = Invoke-AuditSql @'
SET NOCOUNT ON;
SELECT 'AUDIT_DB_RESIDUE=' + CAST(COUNT(*) AS varchar(10)) FROM sys.databases WHERE name=N'english_learning_audit_v9_srs';
SELECT 'AUDIT_LOGIN_RESIDUE=' + CAST(COUNT(*) AS varchar(10)) FROM sys.server_principals WHERE name=N'audit_v9_srs';
'@
$residue | Set-Content -LiteralPath (Join-Path $logDirectory 'cleanup.log')
if (($residue -join ' ') -notmatch 'AUDIT_DB_RESIDUE=0' -or
    ($residue -join ' ') -notmatch 'AUDIT_LOGIN_RESIDUE=0') {
    throw 'Audit database/login residue remains.'
}
$testLog = Get-Content -LiteralPath (Join-Path $logDirectory 'tests.log')
$testLog | Select-String 'Tests run:|BUILD SUCCESS|BUILD FAILURE'
$residue
if ($testExit -eq 0 -and ($testLog -join ' ') -notmatch 'Tests run: 3, Failures: 0, Errors: 0, Skipped: 0') {
    throw 'Expected three executed integration tests, no skipped tests.'
}
Write-Output "EVIDENCE_DIRECTORY=$logDirectory"
exit $testExit
