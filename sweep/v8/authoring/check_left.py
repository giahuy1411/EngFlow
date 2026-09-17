import io, sys, subprocess
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

def sql(q):
    r = subprocess.run(["docker", "exec", "engflow-sqlserver", "/opt/mssql-tools18/bin/sqlcmd",
                        "-S", "localhost", "-U", "sa", "-P", "YourPassword123",
                        "-d", "english_learning", "-C", "-W", "-s", "|", "-Q", q],
                       capture_output=True, text=True)
    out = (r.stdout or "").strip()
    if r.returncode:
        out += "\nERR " + (r.stderr or "").strip()[:300]
    return out

queries = [
    "SELECT zz_lessons = COUNT(*) FROM lessons WHERE title LIKE 'ZZ v8%' OR title LIKE 'ZZTEST%'",
    "SELECT zz_vocab = COUNT(*) FROM vocabulary WHERE word LIKE 'zzprobe%'",
    "SELECT zz_prompts = COUNT(*) FROM speaking_prompts WHERE title LIKE 'ZZ v8%' OR title LIKE 'ZZTEST%'",
    "SELECT zz_decks = COUNT(*) FROM decks WHERE name LIKE 'ZZ v8%'",
    "SELECT orphan_ex = COUNT(*) FROM exercises e LEFT JOIN lessons l ON l.lesson_id = e.lesson_id WHERE l.lesson_id IS NULL",
    "SELECT orphan_snap = COUNT(*) FROM lesson_snapshots s LEFT JOIN lessons l ON l.lesson_id = s.lesson_id WHERE l.lesson_id IS NULL",
    "SELECT zz_users = COUNT(*) FROM users WHERE email LIKE 'zzprobe%@example.com'",
]
for q in queries:
    print(sql(q))
