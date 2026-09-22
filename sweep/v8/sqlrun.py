import subprocess, sys, io

sql_path = sys.argv[1]
sql = open(sql_path, encoding="utf-8").read()
cmd = ["docker", "exec", "-i", "engflow-sqlserver", "/opt/mssql-tools18/bin/sqlcmd",
       "-S", "localhost", "-U", "sa", "-P", "YourPassword123",
       "-d", "english_learning", "-C", "-W", "-s", "|"]
r = subprocess.run(cmd, input=sql.encode("utf-8"), capture_output=True)
out = r.stdout.decode("utf-8", "replace")
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding=chr(117)+chr(116)+chr(102)+chr(45)+chr(56), errors=chr(114)+chr(101)+chr(112)+chr(108)+chr(97)+chr(99)+chr(101))
print(out.strip()[:6000])
err = r.stderr.decode("utf-8", "replace").strip()
if err:
    print("STDERR:", err[:600])
