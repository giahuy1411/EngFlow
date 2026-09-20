const http = require('http');
const { execSync } = require('child_process');
const fs = require('fs');

const results = {};

// 1. Backend API
try {
    const data = execSync('curl -s http://localhost:8080/api/lessons?page=0&size=1', { timeout: 10000 }).toString();
    const json = JSON.parse(data);
    results.api = { status: 'OK', totalLessons: json.totalElements };
} catch(e) {
    results.api = { status: 'ERROR', message: e.message.substring(0, 200) };
}

// 2. DB
try {
    const dbOut = execSync('docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "YourPassword123" -d english_learning -Q "SELECT \'lessons\' AS t, COUNT(*) FROM lessons UNION ALL SELECT \'exercises\', COUNT(*) FROM exercises UNION ALL SELECT \'users\', COUNT(*) FROM users;" -C -h -1', { timeout: 15000 }).toString();
    results.db = { status: 'OK', rows: dbOut.trim() };
} catch(e) {
    results.db = { status: 'ERROR', message: e.message.substring(0, 200) };
}

// 3. Frontend
try {
    execSync('curl -s -o /dev/null -w "%{http_code}" http://localhost:5173', { timeout: 10000 });
    results.frontend = { status: 'OK' };
} catch(e) {
    results.frontend = { status: 'ERROR', message: e.message.substring(0, 200) };
}

fs.writeFileSync('check-results.json', JSON.stringify(results, null, 2));
console.log(JSON.stringify(results, null, 2));
