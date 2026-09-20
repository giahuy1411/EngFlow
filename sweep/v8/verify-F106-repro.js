/**
 * verify-F106-repro.js — independent reproducer for F106 that does NOT touch
 * real learner data.
 *
 * It registers a throwaway account (audit namespace), forces its
 * user_vocabulary_progress row into the exact pre-fix state measured on
 * 2026-09-17 (repetitions=15, srs_interval=1537216, ease_factor=2.6), then calls
 * POST /api/srs/review and reports the HTTP status + the DB row afterwards.
 *
 * Usage: node verify-F106-repro.js <seed|review|cleanup>
 *   seed    -> create the account and force the poison interval
 *   review  -> call the endpoint once (no seed) and print status + row
 *   cleanup -> delete the row + the account
 */
const { execFileSync } = require("child_process");

const BASE = "http://localhost:8080";
const EMAIL = "zzverify106@example.com";
const USER = "zzverify106";
const PASS = "Test123456";
const VOCAB = 10017;
const POISON = 1537216;

function sql(q) {
  return execFileSync("docker", ["exec", "engflow-sqlserver", "/opt/mssql-tools18/bin/sqlcmd",
    "-S", "localhost", "-U", "sa", "-P", "YourPassword123", "-d", "english_learning",
    "-C", "-I", "-h", "-1", "-W", "-Q", "SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; " + q],
    { encoding: "utf8" }).trim();
}
async function login() {
  const r = await fetch(BASE + "/api/auth/login", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: EMAIL, password: PASS }) });
  const j = await r.json();
  const d = j.data || j;
  return d.token;
}
const row = () => sql("SELECT COUNT(*) FROM user_vocabulary_progress WHERE user_id=(SELECT user_id FROM users WHERE email='" + EMAIL + "') AND vocabulary_id=" + VOCAB)
  + " | interval=" + sql("SELECT ISNULL(CAST(srs_interval AS varchar(20)),'null') FROM user_vocabulary_progress WHERE user_id=(SELECT user_id FROM users WHERE email='" + EMAIL + "') AND vocabulary_id=" + VOCAB)
  + " | next_review_date=" + sql("SELECT ISNULL(CONVERT(varchar(19),next_review_date,120),'null') FROM user_vocabulary_progress WHERE user_id=(SELECT user_id FROM users WHERE email='" + EMAIL + "') AND vocabulary_id=" + VOCAB);

(async () => {
  const mode = process.argv[2] || "review";
  if (mode === "seed") {
    await fetch(BASE + "/api/auth/register", { method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: USER, email: EMAIL, password: PASS, confirmPassword: PASS }) });
    const uid = sql("SELECT user_id FROM users WHERE email='" + EMAIL + "'");
    sql("DELETE FROM user_vocabulary_progress WHERE user_id=" + uid + " AND vocabulary_id=" + VOCAB + ";"
      + "INSERT INTO user_vocabulary_progress (user_id, vocabulary_id, mastery_level, review_count, ease_factor, srs_interval, repetitions, next_review_date, created_at, updated_at)"
      + " VALUES (" + uid + ", " + VOCAB + ", 2, 42, 2.6, " + POISON + ", 15, DATEADD(day," + POISON + ",GETDATE()), GETDATE(), GETDATE());");
    console.log("SEEDED user_id=" + uid + "  " + row());
    return;
  }
  if (mode === "cleanup") {
    const uid = sql("SELECT ISNULL(CAST(user_id AS varchar(20)),'') FROM users WHERE email='" + EMAIL + "'");
    if (uid) {
      sql("DELETE FROM user_vocabulary_progress WHERE user_id=" + uid + ";"
        + "DELETE FROM user_progress WHERE user_id=" + uid + ";"
        + "DELETE FROM user_streaks WHERE user_id=" + uid + ";"
        + "DELETE FROM users WHERE user_id=" + uid + ";");
    }
    console.log("CLEANED uid=" + (uid || "none") + "  rows_left=" + sql("SELECT COUNT(*) FROM users WHERE email='" + EMAIL + "'"));
    return;
  }
  const token = await login();
  if (!token) { console.log("LOGIN FAILED (user missing?)"); return; }
  const before = row();
  const r = await fetch(BASE + "/api/srs/review", {
    method: "POST", headers: { "Content-Type": "application/json", Authorization: "Bearer " + token },
    body: JSON.stringify({ vocabId: VOCAB, quality: 5 }) });
  const body = (await r.text()).slice(0, 160).replace(/\s+/g, " ");
  console.log("HTTP " + r.status + "  body=" + body);
  console.log("row BEFORE: " + before);
  console.log("row AFTER : " + row());
})().catch((e) => { console.log("ERR", e.message); process.exit(1); });