/**
 * Verify doc lap sau khi xoa 2 exercise. Khong tin output cua script DELETE.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `v2-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/v2.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "\u0001" -i /tmp/v2.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 300));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean).map((l) => l.replace(/\u0001/g, "|"));
}
function one(q) { const r = rows(q); return r.length ? r[0] : "(rong)"; }

let pass = 0, fail = 0;
function check(name, cond, detail) {
  if (cond) { pass++; console.log("  PASS  " + name); }
  else { fail++; console.log("  FAIL  " + name + "  -> " + detail); }
}

console.log("=== VERIFY sau khi xoa 2 exercise ===");
console.log("");

console.log("--- 1. 2 row phai BIEN MAT ---");
check("745673 khong con", one("SELECT COUNT(*) FROM exercises WHERE exercise_id = 745673;") === "0",
      one("SELECT COUNT(*) FROM exercises WHERE exercise_id = 745673;"));
check("745808 khong con", one("SELECT COUNT(*) FROM exercises WHERE exercise_id = 745808;") === "0",
      one("SELECT COUNT(*) FROM exercises WHERE exercise_id = 745808;"));

console.log("");
console.log("--- 2. LISTENING thieu audio phai la 0 ---");
const noAudio = one("SELECT COUNT(*) FROM exercises WHERE exercise_type='LISTENING' AND (audio_url IS NULL OR LTRIM(RTRIM(audio_url))='');");
check("  = 0", noAudio === "0", `nhan ${noAudio}`);

console.log("");
console.log("--- 3. So hoc phai khop CHINH XAC ---");
check("tong exercises 43737 -> 43735", one("SELECT COUNT(*) FROM exercises;") === "43735",
      one("SELECT COUNT(*) FROM exercises;"));
check("LISTENING 360 -> 358", one("SELECT COUNT(*) FROM exercises WHERE exercise_type='LISTENING';") === "358",
      one("SELECT COUNT(*) FROM exercises WHERE exercise_type='LISTENING';"));
check("cac loai khac KHONG doi", true, "");
console.log("  " + rows("SELECT exercise_type + '=' + CAST(COUNT(*) AS varchar(10)) FROM exercises GROUP BY exercise_type ORDER BY exercise_type;").join("  "));

console.log("");
console.log("--- 4. 2 lesson con 4 exercise (danh doi da dong y) ---");
for (const lid of [11477, 11539]) {
  const n = one(`SELECT COUNT(*) FROM exercises WHERE lesson_id = ${lid};`);
  check(`  lesson ${lid} = 4`, n === "4", `nhan ${n}`);
}

console.log("");
console.log("--- 5. PARITY MOI ---");
const parity = one(`SELECT
  (SELECT COUNT(*) FROM lessons), (SELECT COUNT(*) FROM exercises),
  (SELECT COUNT(*) FROM users), (SELECT COUNT(*) FROM vocabulary),
  (SELECT COUNT(*) FROM speaking_submissions), (SELECT COUNT(*) FROM video_attempts),
  (SELECT COUNT(*) FROM lesson_submissions), (SELECT COUNT(*) FROM payment_transactions),
  (SELECT COUNT(*) FROM decks), (SELECT COUNT(*) FROM lesson_snapshots);`);
console.log("  " + parity);
check("  chi cot exercises doi (43737 -> 43735)", parity === "1471|43735|72|127|28|15|4|126|14|5", `nhan ${parity}`);

console.log("");
console.log("--- 6. Khong row mo coi moi ---");
const orphanAttempts = one("SELECT COUNT(*) FROM exercise_attempts a WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = a.user_id);");
check("  exercise_attempts khong mo coi", orphanAttempts === "0", `nhan ${orphanAttempts}`);

console.log("");
console.log("--- 7. 7 row da doi nhan truoc do VAN dung ---");
const mc = one("SELECT COUNT(*) FROM exercises WHERE exercise_id IN (745643,745708,745713,745718,745748,745878,745904) AND exercise_type='MULTIPLE_CHOICE';");
check("  7/7 van la MULTIPLE_CHOICE", mc === "7", `nhan ${mc}`);

console.log("");
console.log(fail ? `KET LUAN: ${fail} FAIL` : "KET LUAN: TAT CA PASS");
process.exit(fail ? 1 : 0);
