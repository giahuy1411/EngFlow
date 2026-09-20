/**
 * TRUOC KHI XOA 2 row exercise (745673, 745808).
 *
 * Phai biet CHINH XAC cai gi dang tro toi chung. Xoa ma khong biet thi hoac la
 * FK chan (tot), hoac la xoa duoc nhung de lai du lieu mo coi (xau).
 *
 * Danh sach bang/cot lay tu sys.foreign_key_columns — KHONG doan ten.
 * READ-ONLY.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";
const IDS = "745673, 745808";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `pre2-${Date.now()}-${Math.random().toString(36).slice(2)}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/pre2.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "\u0001" -i /tmp/pre2.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 300));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean).map((l) => l.replace(/\u0001/g, "  |  "));
}
function one(q) { const r = rows(q); return r.length ? r[0] : "(rong)"; }

console.log("=== TRUOC KHI XOA 2 row exercise ===");
console.log("");

console.log("--- 1. Xac nhan 2 row TON TAI va dung la 2 row do ---");
for (const l of rows(`SELECT CAST(exercise_id AS varchar(20)) + '${"\u0001"}' + CAST(lesson_id AS varchar(20))
  + '${"\u0001"}' + exercise_type + '${"\u0001"}' + ISNULL(LEFT(CAST(question AS varchar(60)),60),'')
  FROM exercises WHERE exercise_id IN (${IDS}) ORDER BY exercise_id;`)) {
  console.log("  " + l);
}

console.log("");
console.log("--- 2. Moi FK tro toi dbo.exercises ---");
const fks = rows(`SELECT OBJECT_NAME(fkc.parent_object_id) + '.' + COL_NAME(fkc.parent_object_id, fkc.parent_column_id)
  FROM sys.foreign_key_columns fkc
  WHERE fkc.referenced_object_id = OBJECT_ID(N'dbo.exercises')
  ORDER BY 1;`);
if (!fks.length) console.log("  (KHONG co FK nao tro toi exercises)");
fks.forEach((f) => console.log("  " + f));

console.log("");
console.log("--- 3. Row nao trong cac bang do dang tro toi 2 exercise nay? ---");
if (fks.length) {
  for (const f of fks) {
    const [tbl, col] = f.split(".").map((s) => s.trim());
    const n = one(`SELECT COUNT(*) FROM ${tbl} WHERE ${col} IN (${IDS});`);
    const flag = parseInt(n, 10) > 0 ? "   <-- PHAI XU LY TRUOC" : "";
    console.log(`  ${f.padEnd(45)} ${String(n).padStart(5)} row${flag}`);
  }
} else {
  console.log("  (khong co bang nao -> xoa truc tiep duoc)");
}

console.log("");
console.log("--- 4. Cac bang khac co cot ten chua 'exercise' (khong can FK) ---");
for (const l of rows(`SELECT t.name + '.' + c.name FROM sys.tables t
  JOIN sys.columns c ON c.object_id = t.object_id
  WHERE c.name LIKE '%exercise%' ORDER BY t.name, c.name;`)) {
  console.log("  " + l);
}

console.log("");
console.log("--- 5. exercise_attempts co tro toi 2 row nay khong? ---");
const hasAttempts = rows(`SELECT CAST(COUNT(*) AS varchar(10)) FROM sys.tables WHERE name = 'exercise_attempts';`);
if (hasAttempts.length && hasAttempts[0].trim() !== "0") {
  for (const col of ["exercise_id", "exerciseId"]) {
    const exists = rows(`SELECT CAST(COUNT(*) AS varchar(10)) FROM sys.columns
      WHERE object_id = OBJECT_ID(N'dbo.exercise_attempts') AND name = '${col}';`);
    if (exists.length && exists[0].trim() !== "0") {
      console.log(`  cot ${col}: ${one(`SELECT COUNT(*) FROM exercise_attempts WHERE ${col} IN (${IDS});`)} row`);
    }
  }
} else {
  console.log("  (khong co bang exercise_attempts)");
}

console.log("");
console.log("--- 6. PARITY TRUOC KHI XOA ---");
console.log("  " + one(`SELECT
  (SELECT COUNT(*) FROM lessons), (SELECT COUNT(*) FROM exercises),
  (SELECT COUNT(*) FROM users), (SELECT COUNT(*) FROM vocabulary),
  (SELECT COUNT(*) FROM speaking_submissions), (SELECT COUNT(*) FROM video_attempts),
  (SELECT COUNT(*) FROM lesson_submissions), (SELECT COUNT(*) FROM payment_transactions),
  (SELECT COUNT(*) FROM decks), (SELECT COUNT(*) FROM lesson_snapshots);`));

console.log("");
console.log("--- 7. exercises theo type, TRUOC ---");
for (const l of rows(`SELECT exercise_type + ' = ' + CAST(COUNT(*) AS varchar(10)) FROM exercises GROUP BY exercise_type ORDER BY exercise_type;`)) {
  console.log("  " + l);
}
