/**
 * F127 — do PHAM VI NGUOI DUNG THAT SU GAP.
 *
 * Chi tinh MATCHING trong bai DA PUBLISHED. Bai nhap khong ai lam duoc (F126
 * vua chan), nen khong tinh vao tac dong nguoi dung.
 *
 * Dong thoi kiem gia thuyet: client XAO TRON cot phai (MatchingExercise.vue
 * dong 157-166) nen chi so hien thi KHONG bang chi so goc -> server khong the
 * chm duoc du co doi dinh dang.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `sc-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/sc.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "~" -i /tmp/sc.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 250));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}

console.log("=== F127: pham vi tren bai DA PUBLISHED ===");
console.log("");

console.log("MATCHING trong bai published : " +
  rows("SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises e JOIN lessons l ON l.lesson_id=e.lesson_id WHERE e.exercise_type='MATCHING' AND l.is_published=1;")[0]);
console.log("MATCHING trong bai nhap      : " +
  rows("SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises e JOIN lessons l ON l.lesson_id=e.lesson_id WHERE e.exercise_type='MATCHING' AND l.is_published=0;")[0]);
console.log("");

console.log("--- Trong so published, dap an dang nao? ---");
console.log("dang CHI SO thuan (0=0,1=1,...): " +
  rows(`SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises e JOIN lessons l ON l.lesson_id=e.lesson_id
    WHERE e.exercise_type='MATCHING' AND l.is_published=1
      AND e.correct_answer NOT LIKE '%[^0-9=, ]%' AND e.correct_answer LIKE '%=%';`)[0]);
console.log("dang CO CHU                    : " +
  rows(`SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises e JOIN lessons l ON l.lesson_id=e.lesson_id
    WHERE e.exercise_type='MATCHING' AND l.is_published=1 AND e.correct_answer LIKE '%[^0-9=, ]%';`)[0]);
console.log("");

console.log("--- Vi du 5 row published, kem options ---");
for (const l of rows(`SELECT TOP 5
    'id=' + CAST(e.exercise_id AS varchar(20)) + ' lesson=' + CAST(e.lesson_id AS varchar(20))
    + '  ca=[' + CAST(e.correct_answer AS varchar(90)) + ']'
    + '  options=[' + ISNULL(LEFT(CAST(e.options AS varchar(120)),120),'NULL') + ']'
  FROM exercises e JOIN lessons l ON l.lesson_id=e.lesson_id
  WHERE e.exercise_type='MATCHING' AND l.is_published=1
  ORDER BY e.exercise_id;`)) {
  console.log("  " + l);
}

console.log("");
console.log("--- options co dung dinh dang 'left|right' khong? ---");
console.log("published MATCHING co options chua '|': " +
  rows(`SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises e JOIN lessons l ON l.lesson_id=e.lesson_id
    WHERE e.exercise_type='MATCHING' AND l.is_published=1 AND CAST(e.options AS varchar(4000)) LIKE '%|%';`)[0]);
console.log("published MATCHING KHONG co '|'    : " +
  rows(`SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises e JOIN lessons l ON l.lesson_id=e.lesson_id
    WHERE e.exercise_type='MATCHING' AND l.is_published=1 AND CAST(e.options AS varchar(4000)) NOT LIKE '%|%';`)[0]);
