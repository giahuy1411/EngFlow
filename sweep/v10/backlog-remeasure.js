/**
 * audit-v10 Phase 3.1 — do lai 4 backlog item, doc ket qua SACH.
 *
 * Ly do co script nay thay vi goi sqlcmd truc tiep: harness nen output cua
 * sqlcmd thanh dang "N matches in N files", lam mat so lieu. Chay tung query
 * rieng va in mot dong mot ket qua thi khong bi nen.
 *
 * READ-ONLY. Khong xoa, khong sua.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `bl-${Date.now()}-${Math.random().toString(36).slice(2)}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/blq.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -i /tmp/blq.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}

function rows(query) {
  const out = sql(query);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 200));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}

function one(query) {
  const r = rows(query);
  return r.length ? r[0] : "(khong co ket qua)";
}

console.log("=== audit-v10 Phase 3.1 — do lai 4 backlog item ===");
console.log("");

console.log("--- BL1: bang exercises_bak_v5* ---");
const baks = rows("SELECT name FROM sys.tables WHERE name LIKE 'exercises_bak_v5%' ORDER BY name;");
console.log("  so bang: " + baks.length);
baks.forEach((b) => console.log("    " + b));
for (const b of baks) {
  const n = one(`SELECT COUNT(*) FROM ${b};`);
  console.log(`    ${b}: ${n} row`);
}

console.log("");
console.log("--- BL2: user kieu zz* ---");
const zz = rows("SELECT CAST(user_id AS varchar(20)) + ' | ' + username + ' | ' + email + ' | ' + CONVERT(varchar(19), created_at, 120) FROM users WHERE email LIKE 'zz%' OR username LIKE 'zz%' ORDER BY user_id;");
console.log("  so user: " + zz.length);
zz.forEach((z) => console.log("    " + z));

console.log("");
console.log("--- BL3: exercise co correct_answer rong ---");
console.log("  tong: " + one("SELECT COUNT(*) FROM exercises WHERE correct_answer IS NULL OR LTRIM(RTRIM(correct_answer)) = '';"));
console.log("  phan bo theo type:");
rows("SELECT exercise_type + ' = ' + CAST(COUNT(*) AS varchar(20)) FROM exercises WHERE correct_answer IS NULL OR LTRIM(RTRIM(correct_answer)) = '' GROUP BY exercise_type ORDER BY COUNT(*) DESC;")
  .forEach((r) => console.log("    " + r));

console.log("");
console.log("--- BL4: LISTENING thieu audio_url ---");
console.log("  thieu audio: " + one("SELECT COUNT(*) FROM exercises WHERE exercise_type = 'LISTENING' AND (audio_url IS NULL OR LTRIM(RTRIM(audio_url)) = '');"));
console.log("  tong LISTENING: " + one("SELECT COUNT(*) FROM exercises WHERE exercise_type = 'LISTENING';"));

console.log("");
console.log("--- BL5: moi bang co ten kieu _bak_ ---");
rows("SELECT t.name + ' = ' + CAST(p.rows AS varchar(20)) + ' row' FROM sys.tables t JOIN sys.partitions p ON t.object_id = p.object_id AND p.index_id IN (0,1) WHERE t.name LIKE '%_bak_%' OR t.name LIKE '%bak%' ORDER BY t.name;")
  .forEach((r) => console.log("    " + r));

console.log("");
console.log("--- BL6: parity hien tai ---");
console.log("  " + one(`SELECT
    (SELECT COUNT(*) FROM lessons) AS a,
    (SELECT COUNT(*) FROM exercises) AS b,
    (SELECT COUNT(*) FROM users) AS c,
    (SELECT COUNT(*) FROM vocabulary) AS d,
    (SELECT COUNT(*) FROM speaking_submissions) AS e,
    (SELECT COUNT(*) FROM video_attempts) AS f,
    (SELECT COUNT(*) FROM lesson_submissions) AS g,
    (SELECT COUNT(*) FROM payment_transactions) AS h,
    (SELECT COUNT(*) FROM decks) AS i,
    (SELECT COUNT(*) FROM lesson_snapshots) AS j;`));
