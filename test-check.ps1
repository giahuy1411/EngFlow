# Quick health check
Write-Host "=== Backend API ==="
try {
    $r = Invoke-WebRequest -Uri "http://localhost:8080/api/lessons?page=0&size=1" -UseBasicParsing -TimeoutSec 5
    Write-Host "Status: $($r.StatusCode)"
    Write-Host "Body: $($r.Content.Substring(0, [Math]::Min(200, $r.Content.Length)))"
} catch {
    Write-Host "ERROR: $_"
}

Write-Host "`n=== Frontend ==="
try {
    $r2 = Invoke-WebRequest -Uri "http://localhost:5173" -UseBasicParsing -TimeoutSec 5
    Write-Host "Status: $($r2.StatusCode)"
} catch {
    Write-Host "ERROR: $_"
}

Write-Host "`n=== DB ==="
docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "YourPassword123" -d english_learning -Q "SELECT 'lessons' AS t, COUNT(*) FROM lessons UNION ALL SELECT 'exercises', COUNT(*) FROM exercises UNION ALL SELECT 'users', COUNT(*) FROM users UNION ALL SELECT 'vocabulary', COUNT(*) FROM vocabulary;" -C 2>&1
