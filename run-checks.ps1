Write-Host "=== DB CHECK ==="
docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "YourPassword123" -d english_learning -Q "SELECT 'lessons' AS t, COUNT(*) AS cnt FROM lessons UNION ALL SELECT 'exercises', COUNT(*) FROM exercises UNION ALL SELECT 'users', COUNT(*) FROM users;" -C -h -1

Write-Host "`n=== API CHECK ==="
try {
    $r = Invoke-WebRequest -Uri "http://localhost:8080/api/lessons?page=0&size=1" -UseBasicParsing -TimeoutSec 10
    Write-Host "API Status: $($r.StatusCode)"
} catch {
    Write-Host "API Error: $($_.Exception.Message)"
}
