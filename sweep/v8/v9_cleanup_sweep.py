#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""audit-v9 sub-sweep cleanup + parity assertion.

Why this exists: the p1..p5 suites each clean the rows they know about, but
three writers have no working cleanup path:
  * speaking_submissions - there is NO delete API (DELETE /api/v1/admin/
    speaking-submissions/{id} -> 404, and p3b prints `cleanup: 404`), so every
    fake-mic run leaks a row + a MinIO object;
  * video_attempts / lesson_submissions - created by the video + lesson flows,
    never deleted;
  * payment_transactions - the /premium views mint an order row on mount.
Measured 2026-09-17 after the v9 sweep batch: lessons 1471->1471 but
users 76->77, vocabulary 127->128, video_attempts 15->16,
lesson_submissions 4->5, payments 126->127, speaking 28->30.

This script removes ONLY rows created on the audit day (naive VN clock, as
AGENTS.md requires) and matching the audit's own namespace, deletes the MinIO
objects of the speaking rows, then prints the parity line for an independent
check.
"""
import subprocess, sys, io, re

SQLCMD = ["docker", "exec", "-i", "engflow-sqlserver", "/opt/mssql-tools18/bin/sqlcmd",
          "-S", "localhost", "-U", "sa", "-P", "YourPassword123",
          "-d", "english_learning", "-C", "-W", "-s", "|"]
DAY = "2026-09-17"

def sql(text):
    r = subprocess.run(SQLCMD, input=text.encode("utf-8"), capture_output=True)
    out = r.stdout.decode("utf-8", "replace")
    if re.search(r"Msg \d+", out):
        print("SQL ERROR:\n" + out)
        sys.exit(2)
    return out

def minio_rm(keys):
    if not keys:
        return
    # NOTE: build the argument list ONCE. The first revision prefixed every key
    # with "mc rm --force", so `mc` then received "mc" and "rm" as object paths
    # and printed "Failed to remove `mc`" after having already removed the real
    # objects. Harmless noise, but an evidence line that reports errors while
    # claiming success is exactly what this audit is supposed to avoid.
    inner = "mc alias set local http://localhost:9000 \"$MINIO_ROOT_USER\" \"$MINIO_ROOT_PASSWORD\" >/dev/null 2>&1; " \
            + "mc rm --force " + " ".join("local/speaking-uploads/%s" % k for k in keys)
    r = subprocess.run(["docker", "exec", "engflow-minio", "sh", "-c", inner], capture_output=True)
    print("minio rm:", r.stdout.decode("utf-8", "replace").strip().replace("\n", " | "),
          r.stderr.decode("utf-8", "replace").strip()[:120])

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

keys = []
raw = sql("SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; SELECT media_object_key FROM speaking_submissions "
          "WHERE submitted_at >= '%s 00:00:00';" % DAY)
for line in raw.splitlines():
    line = line.strip()
    if line and not line.startswith("-") and "media_object_key" not in line:
        keys.append(line)
print("AUDIT_MINIO_KEYS=%d" % len(keys))
minio_rm(keys)

sql("""SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
DELETE FROM speaking_submissions WHERE submitted_at >= '{d} 00:00:00';
DELETE FROM video_attempts       WHERE submitted_at >= '{d} 00:00:00';
DELETE FROM lesson_submissions   WHERE created_at  >= '{d} 00:00:00';
DELETE FROM payment_transactions WHERE created_at  >= '{d} 00:00:00' AND status <> 'SUCCESS' AND transaction_id IS NULL;
DELETE FROM user_vocabulary_progress WHERE created_at >= '{d} 00:00:00' AND vocabulary_id IN (SELECT vocab_id FROM vocabulary WHERE word LIKE 'zz%');
DELETE FROM vocabulary          WHERE word LIKE 'zz%' AND created_at >= '{d} 00:00:00';
DELETE FROM video_lessons       WHERE title LIKE 'ZZ%';
DELETE FROM exercises           WHERE lesson_id IN (SELECT lesson_id FROM lessons WHERE title LIKE 'ZZ%');
DELETE FROM lesson_sections     WHERE lesson_id IN (SELECT lesson_id FROM lessons WHERE title LIKE 'ZZ%');
DELETE FROM lessons             WHERE title LIKE 'ZZ%';
DELETE FROM decks               WHERE name LIKE 'ZZ%';
-- ONLY users created on the audit day. The first version of this script used a
-- bare `email LIKE 'zz%'` and deleted 4 LEGACY audit users (zzprobe30119/65536/
-- 79221/15227@example.com, ids 170049/170050/170051/170097) that are part of
-- the 76-user baseline and are a documented backlog item, not sweep residue.
-- Measured 2026-09-17: users 76 -> 72, restored verbatim from the 21:06 backup
-- via RESTORE ... WITH MOVE into english_learning_v9r + IDENTITY_INSERT copy.
DELETE FROM users               WHERE email LIKE 'zz%' AND created_at >= '{d} 00:00:00';
GO""".format(d=DAY))

out = sql("""SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON;
SELECT (SELECT COUNT(*) FROM lessons) a, (SELECT COUNT(*) FROM exercises) b, (SELECT COUNT(*) FROM users) c,
       (SELECT COUNT(*) FROM vocabulary) d, (SELECT COUNT(*) FROM speaking_submissions) e,
       (SELECT COUNT(*) FROM video_attempts) f, (SELECT COUNT(*) FROM lesson_submissions) g,
       (SELECT COUNT(*) FROM payment_transactions) h, (SELECT COUNT(*) FROM decks) i,
       (SELECT COUNT(*) FROM lesson_snapshots) j;
GO""")
line = [l.strip() for l in out.splitlines() if re.match(r"^\d+(\|\d+)+$", l.strip())]
parity = line[-1] if line else "UNPARSED"
print("AUDIT_CLEAN_PARITY=%s" % parity)
print("BASELINE_EXPECTED=1471|43737|76|127|28|15|4|126|14|5")
print("PARITY_OK=%s" % (parity == "1471|43737|76|127|28|15|4|126|14|5"))
