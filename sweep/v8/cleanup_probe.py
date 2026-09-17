import io, sys, subprocess
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
Q = ["docker", "exec", "engflow-sqlserver", "/opt/mssql-tools18/bin/sqlcmd",
     "-S", "localhost", "-U", "sa", "-P", "YourPassword123", "-d", "english_learning", "-C", "-W"]

def run(sql):
    r = subprocess.run(Q + ["-Q", sql], capture_output=True, text=True)
    print((r.stdout or r.stderr).strip())

# only rows this audit created (strict prefixes), never user content
run("SET NOCOUNT ON; DELETE FROM vocabulary WHERE word LIKE 'zzprobe%';")
run("SET NOCOUNT ON; DELETE FROM exercises WHERE lesson_id IN (SELECT lesson_id FROM lessons WHERE title LIKE 'ZZ v8%');")
run("SET NOCOUNT ON; DELETE FROM lesson_snapshots WHERE lesson_id IN (SELECT lesson_id FROM lessons WHERE title LIKE 'ZZ v8%');")
run("SET NOCOUNT ON; DELETE FROM lesson_blocks WHERE section_id IN (SELECT s.section_id FROM lesson_sections s JOIN lessons l ON l.lesson_id=s.lesson_id WHERE l.title LIKE 'ZZ v8%');")
run("SET NOCOUNT ON; DELETE FROM lesson_sections WHERE lesson_id IN (SELECT lesson_id FROM lessons WHERE title LIKE 'ZZ v8%');")
run("SET NOCOUNT ON; DELETE FROM lessons WHERE title LIKE 'ZZ v8%';")
run("SET NOCOUNT ON; DELETE FROM speaking_prompts WHERE title LIKE 'ZZ v8%' OR title LIKE 'ZZTEST%';")
run("SET NOCOUNT ON; DELETE FROM deck_vocabulary WHERE deck_id IN (SELECT deck_id FROM decks WHERE name LIKE 'ZZ v8%' OR name LIKE 'ZZTEST%');")
run("SET NOCOUNT ON; DELETE FROM decks WHERE name LIKE 'ZZ v8%' OR name LIKE 'ZZTEST%';")
run("SET NOCOUNT ON; SELECT left_vocab=COUNT(*) FROM vocabulary WHERE word LIKE 'zzprobe%'; SELECT left_lesson=COUNT(*) FROM lessons WHERE title LIKE 'ZZ v8%' OR title LIKE 'ZZTEST%'; SELECT left_prompt=COUNT(*) FROM speaking_prompts WHERE title LIKE 'ZZ v8%' OR title LIKE 'ZZTEST%'; SELECT left_deck=COUNT(*) FROM decks WHERE name LIKE 'ZZ v8%' OR name LIKE 'ZZTEST%'; SELECT orphan=COUNT(*) FROM exercises e LEFT JOIN lessons l ON l.lesson_id=e.lesson_id WHERE l.lesson_id IS NULL;")
