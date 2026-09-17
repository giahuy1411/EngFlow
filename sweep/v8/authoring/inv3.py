import io, sys, subprocess, glob, re
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

sql = ("SET NOCOUNT ON; SELECT lessons=COUNT(*) FROM lessons WHERE title LIKE 'ZZ v8%'; "
       "SET NOCOUNT ON; SELECT orphan_ex=COUNT(*) FROM exercises e LEFT JOIN lessons l ON l.lesson_id=e.lesson_id WHERE l.lesson_id IS NULL; "
       "SET NOCOUNT ON; SELECT prompts=COUNT(*) FROM speaking_prompts WHERE title LIKE 'ZZ v8%'; "
       "SET NOCOUNT ON; SELECT decks=COUNT(*) FROM decks WHERE name LIKE 'ZZ v8%'; "
       "SET NOCOUNT ON; SELECT vocab=COUNT(*) FROM vocabulary WHERE word LIKE 'zzprobe%'; "
       "SET NOCOUNT ON; SELECT snap_left=COUNT(*) FROM lesson_snapshots s LEFT JOIN lessons l ON l.lesson_id=s.lesson_id WHERE l.lesson_id IS NULL;")
r = subprocess.run(["docker", "exec", "engflow-sqlserver", "/opt/mssql-tools18/bin/sqlcmd",
                    "-S", "localhost", "-U", "sa", "-P", "YourPassword123",
                    "-d", "english_learning", "-C", "-W", "-s", "|", "-Q", sql],
                   capture_output=True, text=True)
print(r.stdout.strip()[:1500] or r.stderr[:400])

p = glob.glob("src/main/java/**/AiPromptService.java", recursive=True)[0]
s = open(p, encoding="utf-8", errors="replace").read()
i = s.find("generateSpeakingPrompt")
print("=== AiPromptService.generateSpeakingPrompt ===")
print(s[i-200:i+1200].encode("ascii", "replace").decode())

q = glob.glob("src/main/java/**/AiValidateResult.java", recursive=True)[0]
print("=== AiValidateResult ===")
print(open(q, encoding="utf-8", errors="replace").read().encode("ascii", "replace").decode()[:2200])
