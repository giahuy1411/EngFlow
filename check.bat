@echo off
echo === DB CHECK ===
docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "YourPassword123" -d english_learning -Q "SELECT 'lessons' AS t, COUNT(*) FROM lessons UNION ALL SELECT 'exercises', COUNT(*) FROM exercises UNION ALL SELECT 'users', COUNT(*) FROM users;" -C -h -1
echo.
echo === API CHECK ===
curl -s http://localhost:8080/api/lessons?page=0^&size=1
