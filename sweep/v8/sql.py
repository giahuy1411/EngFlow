import subprocess, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

CMD = ["docker", "exec", "-i", "engflow-sqlserver", "/opt/mssql-tools18/bin/sqlcmd",
       "-S", "localhost", "-U", "sa", "-P", "YourPassword123",
       "-d", "english_learning", "-C", "-W", "-s", "|"]

def run(sql):
    p = subprocess.run(CMD, input=sql.encode("utf-8"), capture_output=True)
    out = p.stdout.decode("utf-8", "replace")
    err = p.stderr.decode("utf-8", "replace").strip()
    print(out.rstrip())
    if err:
        print("STDERR:", err[:500])

if __name__ == "__main__":
    run(open(sys.argv[1], encoding="utf-8").read())
