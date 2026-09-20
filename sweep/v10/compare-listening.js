/**
 * So sanh: row LISTENING CO audio khac row KHONG co audio the nao?
 * Neu row co audio luon chua mot "passage"/"transcript" trong question hoac
 * co cot rieng, thi suy ra duoc rang 9 row kia thieu DU LIEU NGUON, khong chi
 * thieu audio.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `cmp-${Date.now()}-${Math.random().toString(36).slice(2)}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/cmp.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "~" -i /tmp/cmp.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}

console.log("=== 5 row LISTENING CO audio (de doi chieu) ===");
console.log("");
const withAudio = sql(`
SELECT TOP 5 exercise_id,
  ISNULL(LEFT(REPLACE(REPLACE(question, CHAR(13),' '), CHAR(10),' '), 110), '(NULL)') AS q,
  ISNULL(LEFT(audio_url, 70), '(NULL)') AS a
FROM exercises
WHERE exercise_type='LISTENING' AND audio_url IS NOT NULL AND LTRIM(RTRIM(audio_url))<>''
ORDER BY exercise_id;`);
for (const l of withAudio.split(/\r?\n/).map((x) => x.trim()).filter(Boolean)) {
  const [id, q, a] = l.split("~");
  console.log(`id=${id}`);
  console.log(`  question: ${q}`);
  console.log(`  audio   : ${a}`);
  console.log("");
}

console.log("=== Cot nao co the chua transcript/passage? ===");
console.log("");
const cols = sql(`SELECT name FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.exercises') ORDER BY column_id;`);
console.log("  " + cols.split(/\r?\n/).map((x) => x.trim()).filter(Boolean).join(", "));
console.log("");

console.log("=== 358 row CO audio: question co dai hon khong? ===");
console.log("");
const stats = sql(`
SELECT
  CAST(AVG(LEN(ISNULL(question,''))) AS decimal(10,1)) AS avg_q_len_co_audio,
  CAST(MAX(LEN(ISNULL(question,''))) AS varchar(10)) AS max_q_len_co_audio
FROM exercises WHERE exercise_type='LISTENING' AND audio_url IS NOT NULL AND LTRIM(RTRIM(audio_url))<>'';`);
console.log("  CO audio : " + stats.trim().replace("~", "  max="));
const stats2 = sql(`
SELECT
  CAST(AVG(LEN(ISNULL(question,''))) AS decimal(10,1)) AS avg_q_len_khong_audio,
  CAST(MAX(LEN(ISNULL(question,''))) AS varchar(10)) AS max_q_len_khong_audio
FROM exercises WHERE exercise_type='LISTENING' AND (audio_url IS NULL OR LTRIM(RTRIM(audio_url))='');`);
console.log("  KHONG audio: " + stats2.trim().replace("~", "  max="));
console.log("");

console.log("=== 9 row thieu audio: content_original co gi khong? ===");
console.log("");
const orig = sql(`
SELECT exercise_id,
  CAST(LEN(ISNULL(content_original,'')) AS varchar(10)) AS len_orig,
  ISNULL(LEFT(REPLACE(REPLACE(content_original, CHAR(13),' '), CHAR(10),' '), 150), '(NULL)') AS snippet
FROM exercises
WHERE exercise_type='LISTENING' AND (audio_url IS NULL OR LTRIM(RTRIM(audio_url))='')
ORDER BY exercise_id;`);
for (const l of orig.split(/\r?\n/).map((x) => x.trim()).filter(Boolean)) {
  const [id, len, snip] = l.split("~");
  console.log(`id=${id}  len(content_original)=${len}`);
  console.log(`  ${snip}`);
}
