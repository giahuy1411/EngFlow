import urllib.request
import json
import subprocess

results = {}

# API check
try:
    r = urllib.request.urlopen('http://localhost:8080/api/lessons?page=0&size=1', timeout=10)
    data = json.loads(r.read())
    results['api_status'] = r.status
    results['total_lessons'] = data.get('totalElements', 'N/A')
except Exception as e:
    results['api_error'] = str(e)

# DB check via docker
try:
    out = subprocess.run(
        ['docker', 'exec', 'engflow-sqlserver', '/opt/mssql-tools18/bin/sqlcmd',
         '-S', 'localhost', '-U', 'sa', '-P', 'YourPassword123',
         '-d', 'english_learning',
         '-Q', "SELECT 'lessons' AS t, COUNT(*) FROM lessons UNION ALL SELECT 'exercises', COUNT(*) FROM exercises UNION ALL SELECT 'users', COUNT(*) FROM users;",
         '-C', '-h', '-1'],
        capture_output=True, text=True, timeout=15
    )
    results['db_output'] = out.stdout.strip()
except Exception as e:
    results['db_error'] = str(e)

print(json.dumps(results, indent=2))
