/**
 * Phan loai 9 row LISTENING thieu audio — de xuat phuong an.
 *
 * Cau hoi quyet dinh: row nao CO THE cuu (doi exercise_type), row nao KHONG
 * THE cuu (thieu han du lieu nguon)?
 *
 * Doc them: options, explanation, do dai, va thu tu trong lesson.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `l9c-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/l9c.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "\u0001" -i /tmp/l9c.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 300));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}

// \u0001 = SOH, khong bao gio xuat hien trong du lieu van ban
const SEP = "\u0001";

const data = rows(`SELECT
    CAST(e.exercise_id AS varchar(20)) + '${SEP}' +
    CAST(e.lesson_id AS varchar(20)) + '${SEP}' +
    REPLACE(REPLACE(REPLACE(CAST(e.question AS varchar(600)), CHAR(13),' '), CHAR(10),' '), '${SEP}', ' ') + '${SEP}' +
    REPLACE(REPLACE(REPLACE(ISNULL(CAST(e.correct_answer AS varchar(400)),''), CHAR(13),' '), CHAR(10),' '), '${SEP}', ' ') + '${SEP}' +
    REPLACE(REPLACE(REPLACE(ISNULL(CAST(e.options AS varchar(900)),''), CHAR(13),' '), CHAR(10),' '), '${SEP}', ' ') + '${SEP}' +
    REPLACE(REPLACE(REPLACE(ISNULL(CAST(e.explanation AS varchar(300)),''), CHAR(13),' '), CHAR(10),' '), '${SEP}', ' ') + '${SEP}' +
    CAST(e.order_index AS varchar(10))
  FROM exercises e
  WHERE e.exercise_type = 'LISTENING'
    AND (e.audio_url IS NULL OR LTRIM(RTRIM(e.audio_url)) = '')
  ORDER BY e.exercise_id;`);

console.log("=== 9 row LISTENING thieu audio — phan tich day du ===");
console.log("");

for (const line of data) {
  const p = line.split(SEP);
  if (p.length < 7) { console.log("  (dong loi: " + line.slice(0, 100) + ")"); continue; }
  const [exId, lessonId, q, ca, opts, expl, order] = p;

  console.log(`--- exercise_id=${exId}  lesson=${lessonId}  order_index=${order} ---`);
  console.log(`  question (${q.length} ky tu): ${q}`);
  console.log(`  correct_answer (${ca.length}): ${ca}`);

  let optArr = null;
  try { optArr = JSON.parse(opts); } catch (e) {}
  if (Array.isArray(optArr) && optArr.length) {
    console.log(`  options (${optArr.length} lua chon):`);
    optArr.forEach((o) => console.log(`     ${String(o).slice(0, 90)}`));
  } else {
    console.log(`  options: ${opts ? opts.slice(0, 120) : "(rong)"}`);
  }
  console.log(`  explanation: ${expl || "(rong)"}`);

  // Phan loai so bo
  const isWrite = /^\s*write\b/i.test(q);
  const isRead = /^\s*(please\s+)?read\b/i.test(q) || /\bthe (article|text)\b/i.test(q) || /^I can understand a text/i.test(q);
  const isListen = /^\s*listen\b/i.test(q);
  const hasChoices = Array.isArray(optArr) && optArr.length >= 2;
  const caLooksLikeLetter = /^[A-D]$/i.test(ca.trim());
  const caLooksLikeOption = /^[A-D]\.\s/.test(ca.trim());

  const flags = [];
  if (isWrite) flags.push("CAU LENH VIET");
  if (isRead) flags.push("CAU LENH DOC");
  if (isListen) flags.push("CAU LENH NGHE");
  if (hasChoices) flags.push(`${optArr.length} LUA CHON`);
  if (caLooksLikeLetter) flags.push("dap an = 1 chu cai");
  if (caLooksLikeOption) flags.push("dap an = noi dung lua chon");

  console.log(`  => ${flags.join(" · ") || "(khong nhan dien duoc)"}`);
  console.log("");
}

console.log("=== co WRITING/READING trong enum khong? ===");
console.log("  ExerciseType chi co: MULTIPLE_CHOICE, FILL_BLANK, LISTENING, MATCHING, TRANSLATION");
console.log("  => KHONG co READING, KHONG co WRITING.");
