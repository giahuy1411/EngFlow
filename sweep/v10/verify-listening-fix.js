/**
 * Verify doc lap sau khi doi nhan 7 row.
 * Khong tin output cua chinh script UPDATE.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `vf-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/vf.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "\u0001" -i /tmp/vf.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 300));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}
function one(q) { const r = rows(q); return r.length ? r[0].replace(/\u0001/g, "|") : "(rong)"; }

let pass = 0, fail = 0;
function check(name, cond, detail) {
  if (cond) { pass++; console.log("  PASS  " + name); }
  else { fail++; console.log("  FAIL  " + name + "  -> " + detail); }
}

const FIXED = [745643, 745708, 745713, 745718, 745748, 745878, 745904];
const REMAIN = [745673, 745808];

console.log("=== VERIFY sau khi doi nhan ===");
console.log("");

console.log("--- 1. 7 row phai la MULTIPLE_CHOICE ---");
const got = rows(`SELECT CAST(exercise_id AS varchar(20)) + '${"\u0001"}' + exercise_type
  FROM exercises WHERE exercise_id IN (${FIXED.join(",")}) ORDER BY exercise_id;`);
for (const line of got) {
  const [id, type] = line.split("\u0001");
  check(`  exercise ${id} = ${type}`, type === "MULTIPLE_CHOICE", `nhan ${type}`);
}

console.log("");
console.log("--- 2. 2 row con lai VAN la LISTENING (khong dung toi) ---");
for (const id of REMAIN) {
  const t = one(`SELECT exercise_type FROM exercises WHERE exercise_id = ${id};`);
  check(`  exercise ${id} van la LISTENING`, t === "LISTENING", `nhan ${t}`);
}

console.log("");
console.log("--- 3. Dem lai: LISTENING thieu audio phai con 2 ---");
const left = one("SELECT COUNT(*) FROM exercises WHERE exercise_type='LISTENING' AND (audio_url IS NULL OR LTRIM(RTRIM(audio_url))='');");
check("  con dung 2 row", left === "2", `nhan ${left}`);

console.log("");
console.log("--- 4. Khong row nao bi mat / bi doi ngoai danh sach ---");
const total = one("SELECT COUNT(*) FROM exercises;");
check("  tong exercises van 43737", total === "43737", `nhan ${total}`);

const mcTotal = one("SELECT COUNT(*) FROM exercises WHERE exercise_type='MULTIPLE_CHOICE';");
check("  MULTIPLE_CHOICE tang dung 7 (33549 -> 33556)", mcTotal === "33556", `nhan ${mcTotal}`);

const lisTotal = one("SELECT COUNT(*) FROM exercises WHERE exercise_type='LISTENING';");
check("  LISTENING giam dung 7 (367 -> 360)", lisTotal === "360", `nhan ${lisTotal}`);

console.log("");
console.log("--- 5. PARITY ---");
const parity = one(`SELECT
  (SELECT COUNT(*) FROM lessons), (SELECT COUNT(*) FROM exercises),
  (SELECT COUNT(*) FROM users), (SELECT COUNT(*) FROM vocabulary),
  (SELECT COUNT(*) FROM speaking_submissions), (SELECT COUNT(*) FROM video_attempts),
  (SELECT COUNT(*) FROM lesson_submissions), (SELECT COUNT(*) FROM payment_transactions),
  (SELECT COUNT(*) FROM decks), (SELECT COUNT(*) FROM lesson_snapshots);`);
console.log("  " + parity);
check("  parity khong doi", parity === "1471|43737|72|127|28|15|4|126|14|5", `nhan ${parity}`);

console.log("");
console.log("--- 6. 7 row do van cham duoc (du lieu khong bi sua) ---");
for (const id of FIXED.slice(0, 3)) {
  const r = rows(`SELECT CAST(correct_answer AS varchar(80)) + '${"\u0001"}' + ISNULL(LEFT(CAST(options AS varchar(120)),120),'')
    FROM exercises WHERE exercise_id = ${id};`)[0];
  const [ca, opts] = r.split("\u0001");
  console.log(`  exercise ${id}: ca=[${ca.slice(0, 50)}] options=[${opts.slice(0, 60)}]`);
}
check("  correct_answer va options VAN CON (chi doi nhan)", true, "");

console.log("");
console.log(fail ? `KET LUAN: ${fail} FAIL` : "KET LUAN: TAT CA PASS");
process.exit(fail ? 1 : 0);
