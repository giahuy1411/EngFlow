/**
 * Kiem chung gia thuyet: 9 row "LISTENING" thuc chat la MULTIPLE_CHOICE.
 *
 * Neu dung thi chi can doi exercise_type, khong can sinh audio, khong can xoa.
 *
 * Kiem 4 dieu:
 *  1. Lesson chua chung co PUBLISHED khong (neu nhap thi khong ai cham toi)
 *  2. correct_answer co TRUNG KHIT mot phan tu options khong (dieu kien de
 *     MULTIPLE_CHOICE cham duoc, vi client gui CHU cua lua chon)
 *  3. options co parse duoc thanh MANG JSON khong
 *  4. Trong cung lesson, cac exercise KHAC loai gi (de doi chieu)
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `hyp-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/hyp.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "\u0001" -i /tmp/hyp.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 300));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}
const SEP = "\u0001";

const data = rows(`SELECT
    CAST(e.exercise_id AS varchar(20)) + '${SEP}' +
    CAST(e.lesson_id AS varchar(20)) + '${SEP}' +
    CAST(l.is_published AS varchar(2)) + '${SEP}' +
    ISNULL(l.title, '') + '${SEP}' +
    REPLACE(REPLACE(REPLACE(ISNULL(CAST(e.options AS varchar(2000)),''), CHAR(13),' '), CHAR(10),' '), '${SEP}',' ') + '${SEP}' +
    REPLACE(REPLACE(REPLACE(ISNULL(CAST(e.correct_answer AS varchar(400)),''), CHAR(13),' '), CHAR(10),' '), '${SEP}',' ')
  FROM exercises e JOIN lessons l ON l.lesson_id = e.lesson_id
  WHERE e.exercise_type = 'LISTENING'
    AND (e.audio_url IS NULL OR LTRIM(RTRIM(e.audio_url)) = '')
  ORDER BY e.exercise_id;`);

function norm(s) {
  return String(s == null ? "" : s).trim().replace(/^['"]|['"]$/g, "").trim().toLowerCase();
}

console.log("=== 9 row: co phai MULTIPLE_CHOICE khong? ===");
console.log("");
let mcCount = 0, otherCount = 0;

for (const line of data) {
  const p = line.split(SEP);
  if (p.length < 6) { console.log("(dong loi)"); continue; }
  const [exId, lessonId, pub, title, optsRaw, ca] = p;

  let opts = null;
  try { opts = JSON.parse(optsRaw); } catch (e) { opts = null; }

  const isArray = Array.isArray(opts);
  const n = isArray ? opts.length : 0;

  // correct_answer co trung mot lua chon khong?
  let matchIdx = -1;
  if (isArray) {
    for (let i = 0; i < opts.length; i++) {
      if (norm(opts[i]) === norm(ca)) { matchIdx = i; break; }
    }
  }

  const looksMC = isArray && n >= 2 && matchIdx >= 0;
  if (looksMC) mcCount++; else otherCount++;

  console.log(`exercise_id=${exId}  lesson=${lessonId}  is_published=${pub}`);
  console.log(`  title: ${title.slice(0, 70)}`);
  console.log(`  options: ${isArray ? "MANG JSON, " + n + " phan tu" : "KHONG phai mang JSON -> " + JSON.stringify(optsRaw.slice(0, 80))}`);
  console.log(`  correct_answer khop lua chon so: ${matchIdx >= 0 ? matchIdx + " (TRUNG KHIT)" : "KHONG khop lua chon nao"}`);
  console.log(`  => ${looksMC ? "*** CAU TRUC MULTIPLE_CHOICE ***" : "khong ket luan duoc"}`);
  console.log("");
}

console.log("=== TONG ===");
console.log(`  cau truc MULTIPLE_CHOICE : ${mcCount}`);
console.log(`  con lai                  : ${otherCount}`);
console.log("");

console.log("=== Trong 9 lesson do, cac exercise KHAC loai gi? ===");
const lessons = [...new Set(data.map((l) => l.split(SEP)[1]).filter(Boolean))];
for (const lid of lessons.slice(0, 3)) {
  console.log(`--- lesson ${lid} ---`);
  for (const r of rows(`SELECT CAST(exercise_id AS varchar(20)) + '  ' + exercise_type + '  ' + ISNULL(LEFT(CAST(question AS varchar(70)),70),'')
    FROM exercises WHERE lesson_id = ${lid} ORDER BY order_index;`)) {
    console.log("  " + r);
  }
}

console.log("");
console.log("=== Cac row MULTIPLE_CHOICE KHAC trong DB: correct_answer co dang");
console.log("    'A. noi dung' (khop lua chon) khong? — de biet dinh dang chuan ===");
for (const r of rows(`SELECT TOP 6 CAST(exercise_id AS varchar(20)) + '  ca=[' + ISNULL(LEFT(CAST(correct_answer AS varchar(60)),60),'NULL') + ']'
  FROM exercises WHERE exercise_type='MULTIPLE_CHOICE'
    AND options LIKE '[[]%' AND correct_answer LIKE '%[a-z]%'
  ORDER BY exercise_id;`)) {
  console.log("  " + r);
}
