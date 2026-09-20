/**
 * `exercise_attempts.details` la JSON. No co chua exerciseId khong?
 * Neu co, xoa exercise se lam lich su cu tro toi ID khong con.
 * READ-ONLY.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `ad-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/ad.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "\u0001" -i /tmp/ad.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 300));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean).map((l) => l.replace(/\u0001/g, "  |  "));
}
function one(q) { const r = rows(q); return r.length ? r[0] : "(rong)"; }

console.log("=== exercise_attempts.details co chua exerciseId khong? ===");
console.log("");

console.log("tong attempt:", one("SELECT COUNT(*) FROM exercise_attempts;"));
console.log("attempt co details:", one("SELECT COUNT(*) FROM exercise_attempts WHERE details IS NOT NULL;"));
console.log("");

console.log("--- attempt thuoc 2 lesson bi anh huong (11477, 11539) ---");
console.log("  " + one("SELECT COUNT(*) FROM exercise_attempts WHERE lesson_id IN (11477, 11539);"));
console.log("");

console.log("--- 3 mau details ---");
for (const l of rows(`SELECT TOP 3 CAST(attempt_id AS varchar(20)) + '${"\u0001"}' + CAST(lesson_id AS varchar(20))
  + '${"\u0001"}' + ISNULL(LEFT(CAST(details AS varchar(300)),300),'(NULL)')
  FROM exercise_attempts WHERE details IS NOT NULL ORDER BY attempt_id DESC;`)) {
  console.log("  " + l);
}
console.log("");

console.log("--- details co chua '745673' hoac '745808' o BAT KY dau khong? ---");
console.log("  745673: " + one("SELECT COUNT(*) FROM exercise_attempts WHERE details LIKE '%745673%';"));
console.log("  745808: " + one("SELECT COUNT(*) FROM exercise_attempts WHERE details LIKE '%745808%';"));
console.log("");

console.log("--- details co chua 'exerciseId' khong? ---");
console.log("  " + one("SELECT COUNT(*) FROM exercise_attempts WHERE details LIKE '%exerciseId%';"));
console.log("");

console.log("=== KET LUAN ===");
console.log("Neu ca hai deu 0 thi xoa 2 exercise KHONG lam hong lich su lam bai.");
console.log("exercise_attempts chi luu lesson_id, va details khong nhac toi 2 ID do.");
