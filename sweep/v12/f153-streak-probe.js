// audit-v12 F153 verification: does reading flashcards still record a study day?
// Before the fix the ONLY writer of study_days for flashcards was SrsService.reviewWord,
// reached via POST /api/flashcards/review. The drill no longer calls that, so if
// POST /api/flashcards/study is missing or non-transactional the streak silently breaks.
//
// Self-provisions a throwaway user (audit-v12 Phase 10 lesson: never depend on ambient
// rows) and removes it, by enumerated id, at the end.
const { execFileSync } = require('child_process');
const BASE = process.env.BASE || "http://localhost:8080";
const STAMP = process.env.STAMP || "1789";

async function req(method, p, { token, body } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  const r = await fetch(BASE + p, { method, headers, body: body ? JSON.stringify(body) : undefined });
  let data = null;
  try { data = await r.json(); } catch {}
  return { status: r.status, data };
}

function sqlScalar(query) {
  const out = execFileSync('docker', [
    'exec', '-i', 'engflow-sqlserver', '/opt/mssql-tools18/bin/sqlcmd',
    '-S', 'localhost', '-U', 'sa', '-P', 'YourPassword123',
    '-d', 'english_learning', '-C', '-h', '-1', '-W'
  ], { input: `SET NOCOUNT ON;\n${query}\n`, encoding: 'utf8' });
  const line = out.split('\n').map(s => s.trim()).filter(s => /^-?\d+$/.test(s))[0];
  return line === undefined ? null : Number(line);
}

let fail = 0;
const check = (n, ok, d) => { console.log(`${ok ? "PASS" : "FAIL"}  ${n}${ok ? "" : "  <- " + d}`); if (!ok) fail++; };

(async () => {
  const email = `zzf153streak${STAMP}@test.local`;

  // 1. Provision a throwaway user (no study day yet).
  const reg = await req("POST", "/api/auth/register", {
    body: { email, username: `zzf153${STAMP}`, password: "123456", fullName: "F153 Probe" }
  });
  check("register throwaway user", reg.status === 200 || reg.status === 201, `got ${reg.status}`);
  const login = await req("POST", "/api/auth/login", { body: { email, password: "123456" } });
  const token = login.data?.data?.token || login.data?.token;
  const userId = login.data?.id;
  check("login throwaway user", !!token && !!userId, `token=${!!token} userId=${userId}`);

  const before = sqlScalar(`SELECT COUNT(*) FROM study_days WHERE user_id = ${userId};`);
  console.log(`study_days BEFORE (user ${userId}):`, before);
  check("new user starts with 0 study days", before === 0, `got ${before}`);

  // 2. The behaviour under test: reading flashcards records the day.
  const study = await req("POST", "/api/flashcards/study", { token });
  check("POST /api/flashcards/study -> 200", study.status === 200, `got ${study.status}`);

  const after = sqlScalar(`SELECT COUNT(*) FROM study_days WHERE user_id = ${userId};`);
  console.log(`study_days AFTER  (user ${userId}):`, after);
  check("flashcard study recorded a study day", after === 1, `0 -> ${after}`);

  // 3. Idempotent per day (unique index on user+date).
  const study2 = await req("POST", "/api/flashcards/study", { token });
  const after2 = sqlScalar(`SELECT COUNT(*) FROM study_days WHERE user_id = ${userId};`);
  check("second call -> 200, does NOT double-count", study2.status === 200 && after2 === 1, `status=${study2.status} ${after} -> ${after2}`);

  // 4. The streak endpoint must reflect it.
  const streak = await req("GET", "/api/streak/current", { token });
  console.log("GET /api/streak/current ->", streak.status, JSON.stringify(streak.data));
  check("streak endpoint sees the day", streak.status === 200, `got ${streak.status}`);

  // 5. Auth still required.
  const anon = await req("POST", "/api/flashcards/study");
  check("anon -> 401", anon.status === 401, `got ${anon.status}`);

  // Cleanup by enumerated id.
  const cleanup = `
SET QUOTED_IDENTIFIER ON;
DELETE FROM study_days WHERE user_id = ${userId};
DELETE FROM user_vocabulary_progress WHERE user_id = ${userId};
DELETE FROM decks WHERE owner_id = ${userId};
DELETE FROM users WHERE user_id = ${userId};
SELECT 'residue=' + CAST((SELECT COUNT(*) FROM users WHERE user_id = ${userId}) AS varchar(10))
     + ' days=' + CAST((SELECT COUNT(*) FROM study_days WHERE user_id = ${userId}) AS varchar(10));`;
  const cout = execFileSync('docker', [
    'exec', '-i', 'engflow-sqlserver', '/opt/mssql-tools18/bin/sqlcmd',
    '-S', 'localhost', '-U', 'sa', '-P', 'YourPassword123',
    '-d', 'english_learning', '-C', '-W'
  ], { input: cleanup, encoding: 'utf8' });
  const msgs = (cout.match(/Msg \d+/g) || []);
  console.log("cleanup:", cout.trim().split('\n').filter(l => l.includes('residue=') || l.startsWith('Msg ')).join(' | ') || cout.trim());
  check("cleanup left no residue and no SQL errors", msgs.length === 0 && /residue=0 days=0/.test(cout), `msgs=${msgs.length}`);

  console.log(fail === 0 ? "\nALL PASS" : `\n${fail} FAILURE(S)`);
  process.exit(fail === 0 ? 0 : 1);
})();
