/**
 * audit-v10 Phase 3.8 — xem 9 row LISTENING thieu audio_url.
 * READ-ONLY. Quyet dinh backfill dua tren noi dung that, khong doan.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `l9-${Date.now()}-${Math.random().toString(36).slice(2)}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/l9.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "~" -i /tmp/l9.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}

const out = sql(`
SELECT exercise_id, lesson_id,
       ISNULL(LEFT(REPLACE(REPLACE(question, CHAR(13), ' '), CHAR(10), ' '), 90), '(NULL)') AS question,
       ISNULL(LEFT(REPLACE(REPLACE(correct_answer, CHAR(13), ' '), CHAR(10), ' '), 60), '(NULL)') AS answer,
       ISNULL(audio_url, '(NULL)') AS audio
FROM exercises
WHERE exercise_type = 'LISTENING' AND (audio_url IS NULL OR LTRIM(RTRIM(audio_url)) = '')
ORDER BY exercise_id;
`);

if (/Msg \d+/.test(out)) {
  console.log("SQL LOI: " + out.trim().slice(0, 300));
  process.exit(1);
}

const lines = out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
console.log("=== " + lines.length + " row LISTENING thieu audio ===");
console.log("");
for (const l of lines) {
  const [id, lesson, q, a, audio] = l.split("~");
  console.log(`exercise_id=${id}  lesson_id=${lesson}`);
  console.log(`  question: ${q}`);
  console.log(`  answer  : ${a}`);
  console.log(`  audio   : ${audio}`);
  console.log("");
}

// Cung lesson do co bao nhieu exercise khac dang co audio? (de biet ngu canh)
console.log("=== cac row nay thuoc lesson nao, lesson do co gi khac ===");
const lessonIds = [...new Set(lines.map((l) => l.split("~")[1]).filter(Boolean))];
for (const lid of lessonIds) {
  const r = sql(`SELECT
    CAST(COUNT(*) AS varchar(10)) + '~' +
    CAST(SUM(CASE WHEN exercise_type='LISTENING' THEN 1 ELSE 0 END) AS varchar(10)) + '~' +
    CAST(SUM(CASE WHEN exercise_type='LISTENING' AND audio_url IS NOT NULL AND LTRIM(RTRIM(audio_url))<>'' THEN 1 ELSE 0 END) AS varchar(10))
    FROM exercises WHERE lesson_id = ${lid};`);
  const [total, lis, lisWithAudio] = r.trim().split("~");
  console.log(`  lesson ${lid}: tong ${total} exercise, ${lis} LISTENING, ${lisWithAudio} trong so do CO audio`);
}
