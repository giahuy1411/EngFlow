/**
 * audit-v10 Phase 3.5/3.6 — kiem tra truoc khi xoa, doc ket qua SACH.
 * READ-ONLY.
 *
 * Danh sach bang/cot lay tu sys.foreign_key_columns, khong doan:
 *   sweep/v10/find-user-fks.sql -> 12 FK tro toi dbo.users.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";
const IDS = "170049, 170050, 170051, 170097";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `pre-${Date.now()}-${Math.random().toString(36).slice(2)}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/pq.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -i /tmp/pq.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}

function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 300));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}
function one(q) { const r = rows(q); return r.length ? r[0] : "(rong)"; }

console.log("=== KIEM TRA TRUOC KHI XOA ===");
console.log("");

console.log("--- 1. 4 user co ton tai va dung la rac? ---");
rows(`SELECT CAST(user_id AS varchar(20)) + ' | ' + username + ' | is_admin=' + CAST(is_admin AS varchar(2))
      + ' | is_premium=' + CAST(is_premium AS varchar(2))
      FROM users WHERE user_id IN (${IDS}) ORDER BY user_id;`)
  .forEach((r) => console.log("  " + r));

console.log("");
console.log("--- 2. Row cua 4 user nay o tung bang co FK toi users ---");
const TABLES = [
  ["study_days", "user_id"],
  ["user_vocabulary_progress", "user_id"],
  ["exercise_attempts", "user_id"],
  ["lesson_submissions", "user_id"],
  ["speaking_submissions", "user_id"],
  ["speaking_submissions", "graded_by"],
  ["video_attempts", "user_id"],
  ["video_attempts", "graded_by"],
  ["payment_transactions", "user_id"],
  ["decks", "owner_id"],
  ["user_progress", "user_id"],
  ["user_streaks", "user_id"],
];
let totalRows = 0;
for (const [t, c] of TABLES) {
  const n = parseInt(one(`SELECT COUNT(*) FROM ${t} WHERE ${c} IN (${IDS});`), 10);
  totalRows += n;
  console.log(`  ${(t + "." + c).padEnd(42)} ${String(n).padStart(4)} row` + (n ? "   <-- PHAI XOA TRUOC" : ""));
}
console.log("  " + "-".repeat(50));
console.log("  TONG row phu thuoc: " + totalRows);

console.log("");
console.log("--- 3. 4 bang backup co FK / doi tuong nao tro toi? ---");
rows(`SELECT t.name + ' | fk_tro_toi=' + CAST((SELECT COUNT(*) FROM sys.foreign_keys WHERE referenced_object_id = t.object_id) AS varchar(5))
      + ' | phu_thuoc=' + CAST((SELECT COUNT(*) FROM sys.sql_expression_dependencies WHERE referenced_id = t.object_id) AS varchar(5))
      FROM sys.tables t WHERE t.name LIKE 'exercises_bak_v5%' ORDER BY t.name;`)
  .forEach((r) => console.log("  " + r));

console.log("");
console.log("--- 4. View/procedure nao ten chua exercises_bak? ---");
// `name` (Latin1_General_CI_AS_KS_WS) va `type_desc` (SQL_Latin1_General_CP1_CI_AS)
// khac collation -> phep `+` bao Msg 451. Ep COLLATE ve mot phia. Tra ve hai COT
// rieng thay vi noi chuoi cung tranh duoc van de nay.
const deps = rows(`SELECT name COLLATE SQL_Latin1_General_CP1_CI_AS AS obj_name, type_desc
                   FROM sys.objects
                   WHERE name LIKE '%exercises_bak%' AND type IN ('V','P','FN','IF','TF','TR');`);
console.log(deps.length ? deps.map((r) => "  " + r).join("\n") : "  (khong co — an toan de drop)");

console.log("");
console.log("--- 5. Tong users TRUOC khi xoa (baseline 76) ---");
console.log("  " + one("SELECT COUNT(*) FROM users;") + " users");

console.log("");
console.log("--- 6. Parity TRUOC khi xoa ---");
console.log("  " + one(`SELECT
  (SELECT COUNT(*) FROM lessons) AS a, (SELECT COUNT(*) FROM exercises) AS b,
  (SELECT COUNT(*) FROM users) AS c, (SELECT COUNT(*) FROM vocabulary) AS d,
  (SELECT COUNT(*) FROM speaking_submissions) AS e, (SELECT COUNT(*) FROM video_attempts) AS f,
  (SELECT COUNT(*) FROM lesson_submissions) AS g, (SELECT COUNT(*) FROM payment_transactions) AS h,
  (SELECT COUNT(*) FROM decks) AS i, (SELECT COUNT(*) FROM lesson_snapshots) AS j;`)
  .replace(/\s+/g, "|"));
