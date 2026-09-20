/**
 * Chan doan 39 "orphan": la du lieu sai, hay la query cua toi dem nham NULL?
 *
 * Gia thuyet: `NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = x.graded_by)`
 * tra ve TRUE khi `x.graded_by IS NULL`, vi NULL khong bang bat cu thu gi. Vay
 * moi row CHUA duoc cham diem bi dem la "orphan" — sai.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `dg-${Date.now()}-${Math.random().toString(36).slice(2)}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/dg.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -i /tmp/dg.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 300));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}

const CASES = [
  ["speaking_submissions", "graded_by"],
  ["video_attempts", "graded_by"],
  ["decks", "owner_id"],
];

console.log("=== chan doan orphan: NULL hay du lieu sai? ===");
console.log("");

for (const [t, c] of CASES) {
  console.log(`--- ${t}.${c} ---`);
  const nulls = rows(`SELECT COUNT(*) FROM ${t} WHERE ${c} IS NULL;`)[0];
  const notNull = rows(`SELECT COUNT(*) FROM ${t} WHERE ${c} IS NOT NULL;`)[0];
  const total = rows(`SELECT COUNT(*) FROM ${t};`)[0];
  console.log(`  tong row        : ${total}`);
  console.log(`  ${c} IS NULL     : ${nulls}   <- KHONG phai orphan, chi la chua gan`);
  console.log(`  ${c} IS NOT NULL : ${notNull}`);

  // Query DUNG: chi kiem nhung row co gia tri that
  const realOrphan = rows(`SELECT COUNT(*) FROM ${t} x WHERE x.${c} IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = x.${c});`)[0];
  console.log(`  ORPHAN THAT     : ${realOrphan}   <- chi tinh khi ${c} khac NULL`);

  if (parseInt(realOrphan, 10) > 0) {
    const which = rows(`SELECT TOP 5 CAST(x.${c} AS varchar(20)) FROM ${t} x WHERE x.${c} IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = x.${c});`);
    console.log(`    id mo coi: ${JSON.stringify(which)}`);
  }
  console.log("");
}

console.log("=== cot co cho phep NULL khong? ===");
for (const [t, c] of CASES) {
  const n = rows(`SELECT is_nullable FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.${t}') AND name = '${c}';`)[0];
  console.log(`  ${t}.${c} is_nullable = ${n}`);
}
