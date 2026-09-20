/**
 * audit-v10 Phase 3.5/3.6 — VERIFY sau khi xoa. Doc lap, khong tin output cua
 * chinh script DML.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `ver-${Date.now()}-${Math.random().toString(36).slice(2)}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/vq.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -i /tmp/vq.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 300));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}
function one(q) { const r = rows(q); return r.length ? r[0] : "(rong)"; }

let fail = 0;
function check(name, cond, detail) {
  if (cond) console.log("  PASS  " + name);
  else { fail++; console.log("  FAIL  " + name + "  -> " + detail); }
}

console.log("=== VERIFY sau khi xoa ===");
console.log("");

console.log("--- 1. 4 user phai BIEN MAT ---");
const gone = rows("SELECT CAST(user_id AS varchar(20)) FROM users WHERE user_id IN (170049,170050,170051,170097);");
check("0 trong 4 user con lai", gone.length === 0, "con: " + JSON.stringify(gone));

console.log("");
console.log("--- 2. Khong con user nao khop mau ten rac ---");
const anyZz = rows("SELECT CAST(user_id AS varchar(20)) + ' ' + username FROM users WHERE username LIKE 'zzprobe%' OR email LIKE 'zzprobe%';");
check("khong con user zzprobe*", anyZz.length === 0, "con: " + JSON.stringify(anyZz));

console.log("");
console.log("--- 3. 4 bang backup phai BIEN MAT ---");
const bak = rows("SELECT name FROM sys.tables WHERE name LIKE 'exercises_bak_v5%';");
check("0 bang exercises_bak_v5* con lai", bak.length === 0, "con: " + JSON.stringify(bak));

console.log("");
console.log("--- 4. Users baseline 76 -> 72 ---");
const users = parseInt(one("SELECT COUNT(*) FROM users;"), 10);
check("users = 72", users === 72, "nhan " + users);

console.log("");
console.log("--- 5. Khong co row mo coi (orphan) o cac bang co FK toi users ---");
//
// PHAI co `IS NOT NULL`. Ban dau toi viet thieu dieu kien do va nhan ve 39
// "orphan" — tat ca deu la NULL. `NOT EXISTS (SELECT 1 FROM users u WHERE
// u.user_id = x.graded_by)` tra ve TRUE khi `graded_by IS NULL`, vi NULL
// khong bang bat cu thu gi ke ca NULL. Do la ba cot CHO PHEP NULL
// (is_nullable=1, da kiem chung) va NULL o day co nghia "chua cham diem" /
// "deck he thong khong co chu" — hoan toan hop le, khong phai du lieu hong.
// Do lai bang dung query: ORPHAN THAT = 0 tren ca ba.
const TABLES = [
  ["study_days", "user_id"], ["user_vocabulary_progress", "user_id"],
  ["exercise_attempts", "user_id"], ["lesson_submissions", "user_id"],
  ["speaking_submissions", "user_id"], ["speaking_submissions", "graded_by"],
  ["video_attempts", "user_id"], ["video_attempts", "graded_by"],
  ["payment_transactions", "user_id"], ["decks", "owner_id"],
  ["user_progress", "user_id"], ["user_streaks", "user_id"],
];
let orphans = 0, nullableSkipped = 0;
for (const [t, c] of TABLES) {
  const nullable = one(`SELECT is_nullable FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.${t}') AND name = '${c}';`) === "1";
  const nullCond = nullable ? `x.${c} IS NOT NULL AND ` : "";
  if (nullable) {
    const nulls = parseInt(one(`SELECT COUNT(*) FROM ${t} WHERE ${c} IS NULL;`), 10);
    nullableSkipped += nulls;
  }
  const n = parseInt(one(`SELECT COUNT(*) FROM ${t} x WHERE ${nullCond}NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = x.${c});`), 10);
  if (n) { orphans += n; console.log(`    ${t}.${c}: ${n} ORPHAN THAT`); }
}
console.log(`    (bo qua ${nullableSkipped} gia tri NULL hop le tren cac cot cho phep NULL)`);
check("0 ORPHAN THAT tren ca 12 FK", orphans === 0, orphans + " orphan");

console.log("");
console.log("--- 6. PARITY moi ---");
const parity = one(`SELECT
  (SELECT COUNT(*) FROM lessons) AS a, (SELECT COUNT(*) FROM exercises) AS b,
  (SELECT COUNT(*) FROM users) AS c, (SELECT COUNT(*) FROM vocabulary) AS d,
  (SELECT COUNT(*) FROM speaking_submissions) AS e, (SELECT COUNT(*) FROM video_attempts) AS f,
  (SELECT COUNT(*) FROM lesson_submissions) AS g, (SELECT COUNT(*) FROM payment_transactions) AS h,
  (SELECT COUNT(*) FROM decks) AS i, (SELECT COUNT(*) FROM lesson_snapshots) AS j;`).replace(/\s+/g, "|");
console.log("  " + parity);
const expected = "1471|43737|72|127|28|15|4|126|14|5";
check("parity = " + expected + " (chi users doi 76->72)", parity === expected, "nhan " + parity);

console.log("");
console.log("--- 7. Cac bang NGHIEP VU khac khong doi ---");
check("lessons van 1471", parity.split("|")[0] === "1471", parity.split("|")[0]);
check("exercises van 43737", parity.split("|")[1] === "43737", parity.split("|")[1]);
check("payment_transactions van 126", parity.split("|")[7] === "126", parity.split("|")[7]);

console.log("");
console.log(fail ? `KET LUAN: ${fail} FAIL` : "KET LUAN: TAT CA PASS");
process.exit(fail ? 1 : 0);
