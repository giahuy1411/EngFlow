# EngFlow — backfill undo tooling (plan P3.2).
# Modes:
#   before  -> export the CURRENTLY-EMPTY candidate rows (exercise_id, empty answer)
#              to a before-CSV + generate rollback_<stamp>.sql (restores '' for exactly
#              those ids). Run BEFORE any live backfill batch.
#   after   -> re-export the same id-set with whatever answer they now have (filled
#              audit trail). Run after each batch / at end of session.
# Rollback usage: apply rollback_<stamp>.sql chunked (see Apply-Rollback mode).
#   rollback <file> -> runs the generated UPDATE chunks through sqlcmd.
param(
    [Parameter(Mandatory = $true)][ValidateSet('before', 'after', 'rollback')][string]$Mode,
    [string]$RollbackFile
)
$ErrorActionPreference = 'Stop'
$outDir = 'C:\Users\ASUS\engflow-backups'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$container = 'engflow-sqlserver'
$sqlcmd = '/opt/mssql-tools18/bin/sqlcmd'

function Invoke-Sql([string]$q) {
    # -W trim, -h -1 no header, -s ',' separator; QUOTENAME('"') doubles embedded quotes.
    docker exec $container $sqlcmd -S localhost -U sa -P 'YourPassword123' -d english_learning -C -W -h -1 -Q "SET NOCOUNT ON; $q"
}

$CANDIDATE_WHERE = "(correct_answer IS NULL OR TRIM(correct_answer) = '') AND question IS NOT NULL AND TRIM(question) <> ''"

if ($Mode -eq 'before') {
    $rows = Invoke-Sql "SELECT CAST(exercise_id AS varchar(20)) + ',' + QUOTENAME(correct_answer, CHAR(34)) FROM dbo.exercises WHERE $CANDIDATE_WHERE ORDER BY exercise_id"
    $csv = Join-Path $outDir "before-$stamp.csv"
    ('exercise_id,correct_answer_before') + "`r`n" + ($rows -join "`r`n") + "`r`n" | Out-File -FilePath $csv -Encoding utf8
    $ids = $rows | ForEach-Object { ($_ -split ',')[0] } | Where-Object { $_ -match '^\d+$' }
    Write-Host "before-CSV: $csv ($($ids.Count) ids)"
    # Generate the rollback script (chunked 1000-id UPDATEs restoring '').
    $rb = Join-Path $outDir "rollback-$stamp.sql"
    $sb = [System.Text.StringBuilder]::new()
    $null = $sb.AppendLine("-- Undo for backfill before-$stamp.csv ($( Get-Date -Format 'yyyy-MM-dd HH:mm:ss' ) naive VN)")
    $null = $sb.AppendLine("-- Restores correct_answer='' for exactly the $($ids.Count) rows that were empty at backup time.")
    for ($i = 0; $i -lt $ids.Count; $i += 1000) {
        $chunk = $ids[$i..([Math]::Min($i + 999, $ids.Count - 1))] -join ','
        $null = $sb.AppendLine("UPDATE dbo.exercises SET correct_answer = '' WHERE exercise_id IN ($chunk);")
    }
    [IO.File]::WriteAllText($rb, $sb.ToString(), (New-Object System.Text.UTF8Encoding($true)))
    Write-Host "rollback script: $rb ($([math]::Ceiling($ids.Count / 1000)) chunks)"
    # Echo a quick integrity check.
    $cnt = Invoke-Sql "SELECT COUNT(*) FROM dbo.exercises WHERE $CANDIDATE_WHERE"
    Write-Host "live empty-answer count now: $($cnt | Select-Object -First 1)"
}

if ($Mode -eq 'after') {
    $before = Get-ChildItem (Join-Path $outDir 'before-*.csv') | Sort-Object LastWriteTime | Select-Object -Last 1
    if (-not $before) { throw 'no before-*.csv in ' + $outDir + ' — run -Mode before first' }
    $ids = (Import-Csv $before.FullName).exercise_id
    Write-Host "using id-set from $($before.Name) ($($ids.Count) ids)"
    # Values may contain commas/quotes, so select via temp table? Not needed: id-list
    # is large; instead export ALL rows whose id is in the before-set via JOIN to a
    # bulk-loaded temp would be heavy — simplest correct approach: filter in SQL by
    # the 3 chunks of ids and pull exercise_id + answer.
    $out = Join-Path $outDir "after-$stamp.csv"
    ('exercise_id,correct_answer_now') + "`r`n" | Out-File -FilePath $out -Encoding utf8
    $stillEmpty = 0; $filled = 0
    for ($i = 0; $i -lt $ids.Count; $i += 2000) {
        $chunk = $ids[$i..([Math]::Min($i + 1999, $ids.Count - 1))] -join ','
        $rows = Invoke-Sql "SELECT CAST(exercise_id AS varchar(20)) + ',' + QUOTENAME(correct_answer, CHAR(34)) FROM dbo.exercises WHERE exercise_id IN ($chunk) ORDER BY exercise_id"
        ($rows | Where-Object { $_ -match '^\d+,' }) | Out-File -FilePath $out -Encoding utf8 -Append
        $stillEmpty += ($rows | Where-Object { $_ -match ',"\s*"$|,""$"' }).Count
    }
    Write-Host "after-export: $out"
    $total = Invoke-Sql "SELECT COUNT(*) FROM dbo.exercises"
    $nonEmpty = Invoke-Sql "SELECT COUNT(*) FROM dbo.exercises WHERE NOT ($CANDIDATE_WHERE)"
    Write-Host "exercises total=$($total | Select-Object -First 1) ; still-empty-candidate-set rows visible via remaining count above; live non-empty=$($nonEmpty | Select-Object -First 1)"
}

if ($Mode -eq 'rollback') {
    if (-not $RollbackFile -or -not (Test-Path $RollbackFile)) { throw "pass -RollbackFile <path to rollback-*.sql>" }
    # Copy the SQL into the container and run it (BOM matters for Vietnamese in strings — our file is ASCII).
    docker cp $RollbackFile "${container}:/tmp/rollback.sql"
    docker exec $container $sqlcmd -S localhost -U sa -P 'YourPassword123' -d english_learning -C -i /tmp/rollback.sql
    docker exec $container rm -f /tmp/rollback.sql
    Write-Host "rollback applied from $RollbackFile"
}
