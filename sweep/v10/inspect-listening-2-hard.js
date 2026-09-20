/**
 * 2 row KHONG theo cau truc MULTIPLE_CHOICE: 745713 va 745808.
 * Xem ky de biet co cuu duoc khong.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `h2-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/h2.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "\u0001" -i /tmp/h2.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 300));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}
const SEP = "\u0001";

console.log("=== 2 row kho: 745713 va 745808 ===");
console.log("");

for (const exId of ["745713", "745808"]) {
  const r = rows(`SELECT
      CAST(e.exercise_id AS varchar(20)) + '${SEP}' +
      CAST(e.lesson_id AS varchar(20)) + '${SEP}' +
      CAST(l.is_published AS varchar(2)) + '${SEP}' +
      ISNULL(l.title,'') + '${SEP}' +
      REPLACE(REPLACE(CAST(e.question AS varchar(500)), CHAR(13),' '), CHAR(10),' ') + '${SEP}' +
      REPLACE(REPLACE(ISNULL(CAST(e.correct_answer AS varchar(300)),''), CHAR(13),' '), CHAR(10),' ') + '${SEP}' +
      REPLACE(REPLACE(REPLACE(ISNULL(CAST(e.options AS varchar(1200)),''), CHAR(13),' '), CHAR(10),' '), '${SEP}',' ') + '${SEP}' +
      REPLACE(REPLACE(ISNULL(CAST(e.explanation AS varchar(600)),''), CHAR(13),' '), CHAR(10),' ')
    FROM exercises e JOIN lessons l ON l.lesson_id = e.lesson_id
    WHERE e.exercise_id = ${exId};`)[0].split(SEP);

  const [id, lessonId, pub, title, q, ca, opts, expl] = r;
  console.log(`--- exercise_id=${id}  lesson=${lessonId}  is_published=${pub} ---`);
  console.log(`  lesson title : ${title}`);
  console.log(`  question     : ${q}`);
  console.log(`  correct_answer: ${ca}`);
  console.log(`  explanation  : ${expl}`);
  console.log(`  options RAW  : ${opts}`);
  console.log("");

  // Thu parse theo cac kieu khac nhau
  console.log("  Thu cac kieu parse:");
  try { const j = JSON.parse(opts); console.log("    JSON.parse -> " + (Array.isArray(j) ? "MANG " + j.length : typeof j)); }
  catch (e) { console.log("    JSON.parse -> LOI: " + e.message.slice(0, 60)); }

  // Kieu "A) x.B) y." — tach bang regex
  const byLetter = opts.match(/[A-D]\)\s*[^A-D]*?(?=[A-D]\)|$)/g);
  console.log("    tach theo 'X)' -> " + (byLetter ? byLetter.length + " phan tu" : "khong tach duoc"));
  if (byLetter) byLetter.forEach((x) => console.log("       " + x.trim().slice(0, 90)));

  console.log("");
  console.log("  Cac exercise khac trong lesson nay:");
  for (const x of rows(`SELECT '    ' + CAST(exercise_id AS varchar(20)) + '  ' + exercise_type + '  ' + ISNULL(LEFT(CAST(question AS varchar(60)),60),'')
    FROM exercises WHERE lesson_id = ${lessonId} ORDER BY order_index;`)) console.log(x);
  console.log("");
}

console.log("=== Bao nhieu row MULTIPLE_CHOICE khac co options dang 'A) ...B) ...'? ===");
console.log("  " + rows(`SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises
  WHERE exercise_type='MULTIPLE_CHOICE' AND options LIKE '%A)%' AND options NOT LIKE '[[]%';`)[0] + " row");
console.log("");
console.log("=== Bao nhieu row LISTENING CO audio co options dang 'A) ...'? ===");
console.log("  " + rows(`SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises
  WHERE exercise_type='LISTENING' AND audio_url IS NOT NULL AND options LIKE '%A)%' AND options NOT LIKE '[[]%';`)[0] + " row");
