/**
 * Do cuoi cung truoc khi de xuat: 9 row nay la OUTLIER nhu the nao?
 *
 * Gia thuyet: lesson co ten Reading/Writing/Vocabulary thi khong nen co
 * LISTENING. Neu dung, 9 row nay la du lieu gan nhan sai, khong phai bai nghe.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `out-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/out.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "\u0001" -i /tmp/out.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 300));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}
const SEP = "\u0001";

console.log("=== 9 row nay nam trong lesson loai gi? ===");
console.log("");
for (const r of rows(`SELECT
    'lesson ' + CAST(l.lesson_id AS varchar(20)) + '  [' + ISNULL(l.title,'') + ']'
    + '  co ' + CAST((SELECT COUNT(*) FROM exercises x WHERE x.lesson_id = l.lesson_id) AS varchar(5)) + ' exercise'
    + '  trong do LISTENING=' + CAST((SELECT COUNT(*) FROM exercises x WHERE x.lesson_id = l.lesson_id AND x.exercise_type='LISTENING') AS varchar(5))
    + '  MC=' + CAST((SELECT COUNT(*) FROM exercises x WHERE x.lesson_id = l.lesson_id AND x.exercise_type='MULTIPLE_CHOICE') AS varchar(5))
  FROM lessons l
  WHERE l.lesson_id IN (SELECT DISTINCT lesson_id FROM exercises
      WHERE exercise_type='LISTENING' AND (audio_url IS NULL OR LTRIM(RTRIM(audio_url))=''))
  ORDER BY l.lesson_id;`)) {
  console.log("  " + r.replace(/\u0001/g, "  "));
}

console.log("");
console.log("=== So sanh: lesson CO LISTENING that (co audio) ten gi? ===");
for (const r of rows(`SELECT TOP 8
    'lesson ' + CAST(l.lesson_id AS varchar(20)) + '  [' + ISNULL(l.title,'') + ']  audio=' + ISNULL(LEFT(e.audio_url,40),'')
  FROM exercises e JOIN lessons l ON l.lesson_id = e.lesson_id
  WHERE e.exercise_type='LISTENING' AND e.audio_url IS NOT NULL AND LTRIM(RTRIM(e.audio_url))<>''
  ORDER BY e.exercise_id;`)) {
  console.log("  " + r.replace(/\u0001/g, "  "));
}

console.log("");
console.log("=== Trong lesson co LISTENING that, options cua no la gi? ===");
for (const r of rows(`SELECT TOP 5
    'id=' + CAST(exercise_id AS varchar(20)) + '  question=[' + ISNULL(LEFT(CAST(question AS varchar(70)),70),'') + ']'
    + '  options=' + ISNULL(LEFT(CAST(options AS varchar(90)),90),'NULL')
  FROM exercises
  WHERE exercise_type='LISTENING' AND audio_url IS NOT NULL AND LTRIM(RTRIM(audio_url))<>''
  ORDER BY exercise_id;`)) {
  console.log("  " + r.replace(/\u0001/g, "  "));
}

console.log("");
console.log("=== Ti le: bao nhieu LISTENING co audio co >=2 lua chon? ===");
const withAudioTotal = rows(`SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises WHERE exercise_type='LISTENING' AND audio_url IS NOT NULL AND LTRIM(RTRIM(audio_url))<>'';`)[0];
const withAudioMC = rows(`SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises WHERE exercise_type='LISTENING' AND audio_url IS NOT NULL AND LTRIM(RTRIM(audio_url))<>'' AND options LIKE '[[]%';`)[0];
console.log(`  tong LISTENING co audio        : ${withAudioTotal}`);
console.log(`  trong do options la mang JSON  : ${withAudioMC}`);
console.log("");
console.log("=== Va 9 row thieu audio? ===");
const noAudioMC = rows(`SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises WHERE exercise_type='LISTENING' AND (audio_url IS NULL OR LTRIM(RTRIM(audio_url))='') AND options LIKE '[[]%';`)[0];
console.log(`  tong LISTENING thieu audio     : 9`);
console.log(`  trong do options la mang JSON  : ${noAudioMC}`);
