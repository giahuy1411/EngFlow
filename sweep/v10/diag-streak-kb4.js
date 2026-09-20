/**
 * Chan doan KB4: tai sao chen HOM QUA lai khong lam currentStreak = 2?
 *
 * Gia thuyet can kiem: `snapshot()` loc visibleDays theo cua so, nhung
 * currentStreak() dung allDays. Neu study_days co ca hom qua + hom nay thi
 * phai la 2. Nghi van: INSERT cua ta co thanh cong khong?
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const API = "http://localhost:8080";
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `diag-${Date.now()}.sql`);
  fs.writeFileSync(tmp, query, "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/d.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -i /tmp/d.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}

async function req(method, p, { token, body } = {}) {
  const h = {};
  if (token) h.Authorization = `Bearer ${token}`;
  if (body) h["Content-Type"] = "application/json";
  const r = await fetch(API + p, { method, headers: h, body: body ? JSON.stringify(body) : undefined });
  const t = await r.text();
  let d = null; try { d = JSON.parse(t); } catch { d = t.slice(0, 200); }
  return { status: r.status, data: d };
}

(async () => {
  const STAMP = Date.now();
  const EMAIL = `zzdiagkb4${STAMP}@example.com`;
  const PASS = "AuditV10#2026";

  console.log("=== diag KB4 ===");
  const reg = await req("POST", "/api/auth/register", {
    body: { username: "zzdiagkb4" + STAMP, email: EMAIL, password: PASS, fullName: "diag" },
  });
  console.log("register:", reg.status);

  const uid = parseInt(sql(`SET NOCOUNT ON; SELECT user_id FROM users WHERE email = '${EMAIL}';`).match(/\d+/)[0], 10);
  console.log("uid:", uid);

  const login = await req("POST", "/api/auth/login", { body: { email: EMAIL, password: PASS } });
  const token = login.data?.token || login.data?.data?.token;

  const todayVN = new Date(Date.now() + 7 * 3600e3).toISOString().slice(0, 10);
  const yesterday = new Date(Date.now() + 7 * 3600e3 - 86400e3).toISOString().slice(0, 10);
  console.log("today:", todayVN, " yesterday:", yesterday);

  // Buoc 1: ghi ngay HOM NAY qua API that
  const vocabId = parseInt(sql("SET NOCOUNT ON; SELECT TOP 1 vocab_id FROM vocabulary ORDER BY vocab_id;").match(/\d+/)[0], 10);
  const rev = await req("POST", "/api/srs/review", { token, body: { vocabId, quality: 5 } });
  console.log("review:", rev.status);

  let rows = sql(`SET NOCOUNT ON; SELECT study_date FROM study_days WHERE user_id = ${uid} ORDER BY study_date;`);
  console.log("study_days sau review:", JSON.stringify(rows.trim()));

  let snap = await req("GET", "/api/streak/snapshot", { token });
  console.log("snapshot: streak=", snap.data?.currentStreak, " studiedDays=", JSON.stringify(snap.data?.studiedDays));

  // Buoc 2: chen HOM QUA bang SQL
  const ins = sql(`SET NOCOUNT ON; SET QUOTED_IDENTIFIER ON;
    INSERT INTO study_days (user_id, study_date) VALUES (${uid}, '${yesterday}');
    SELECT 'inserted';`);
  console.log("insert output:", JSON.stringify(ins.trim()));

  rows = sql(`SET NOCOUNT ON; SELECT study_date FROM study_days WHERE user_id = ${uid} ORDER BY study_date;`);
  console.log("study_days sau insert:", JSON.stringify(rows.trim()));

  snap = await req("GET", "/api/streak/snapshot", { token });
  console.log("snapshot sau insert: streak=", snap.data?.currentStreak,
              " studiedDays=", JSON.stringify(snap.data?.studiedDays),
              " effectiveFrom=", snap.data?.effectiveFrom);

  // Buoc 3: kiem tra truc tiep gia thuyet loc
  const inWindow = sql(`SET NOCOUNT ON;
    SELECT COUNT(*) FROM study_days WHERE user_id = ${uid}
      AND study_date >= '${snap.data?.effectiveFrom}' AND study_date <= '${todayVN}';`);
  console.log("so ngay trong [effectiveFrom, today]:", inWindow.trim());

  // cleanup
  sql(`SET NOCOUNT ON; SET QUOTED_IDENTIFIER ON;
    DELETE FROM user_vocabulary_progress WHERE user_id = ${uid};
    DELETE FROM study_days WHERE user_id = ${uid};
    DELETE FROM users WHERE user_id = ${uid};`);
  const left = sql(`SET NOCOUNT ON; SELECT COUNT(*) FROM users WHERE user_id = ${uid};`);
  console.log("cleanup: con lai =", left.trim());
  const total = sql("SET NOCOUNT ON; SELECT COUNT(*) FROM users;");
  console.log("tong users (baseline 76):", total.trim());
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
