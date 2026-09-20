/**
 * F127 — kiem gia thuyet: `options` CO SAN cap dung, `correct_answer` chi lap lai.
 *
 * Voi options = ["A|B", "B|D", "C|C", "D|A"], cap dung la (A,B),(B,D),(C,C),(D,A)
 * — dung bang correct_answer "A=B,B=D,C=C,D=A".
 *
 * Neu dieu nay dung cho MOI row thi `options` la nguon su that, va khong can
 * doc correct_answer cho MATCHING nua.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `opt-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/opt.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "~" -i /tmp/opt.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 250));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}

(async () => {
  console.log("=== F127: options co phai nguon su that? ===");
  console.log("");

  // Lay 400 row MATCHING published kem options + correct_answer
  // `lesson_id` co o CA HAI bang -> phai dinh nghia bang nao (Msg 209).
  const data = rows(`SELECT TOP 400
      CAST(e.exercise_id AS varchar(20)) + '~' +
      CAST(e.lesson_id AS varchar(20)) + '~' +
      CAST(e.correct_answer AS varchar(300)) + '~' +
      ISNULL(CAST(e.options AS varchar(1200)), '')
    FROM exercises e JOIN lessons l ON l.lesson_id = e.lesson_id
    WHERE e.exercise_type='MATCHING' AND l.is_published=1
    ORDER BY e.exercise_id;`);

  console.log("so row lay duoc: " + data.length);
  console.log("");

  let optionsParseable = 0, optionsUnparseable = 0;
  let pairsFromOptionsMatchCA = 0, pairsFromOptionsDiffer = 0;
  const differExamples = [];
  let optionCountMismatch = 0;

  for (const line of data) {
    const parts = line.split("~");
    const [exId, lessonId, ca, optRaw] = parts;
    let opts = null;
    try { opts = JSON.parse(optRaw); } catch (e) { opts = null; }

    if (!Array.isArray(opts)) { optionsUnparseable++; continue; }
    optionsParseable++;

    // Cap tu options: moi phan tu "left|right"
    const optPairs = [];
    for (const o of opts) {
      if (typeof o !== "string") continue;
      const i = o.indexOf("|");
      if (i <= 0) continue;
      optPairs.push(o.slice(0, i).trim().toLowerCase() + "=" + o.slice(i + 1).trim().toLowerCase());
    }

    // Cap tu correct_answer
    const caPairs = [];
    for (const p of String(ca || "").split(",")) {
      const i = p.indexOf("=");
      if (i <= 0) continue;
      caPairs.push(p.slice(0, i).trim().toLowerCase() + "=" + p.slice(i + 1).trim().toLowerCase());
    }

    const a = [...optPairs].sort().join("|");
    const b = [...caPairs].sort().join("|");

    if (optPairs.length !== caPairs.length) {
      optionCountMismatch++;
      if (differExamples.length < 6) {
        differExamples.push({ exId, lessonId, ca, optRaw: optRaw.slice(0, 100),
          optPairs, caPairs, reason: "so cap khac nhau" });
      }
      continue;
    }
    if (a === b) pairsFromOptionsMatchCA++;
    else {
      pairsFromOptionsDiffer++;
      if (differExamples.length < 6) {
        differExamples.push({ exId, lessonId, ca, optRaw: optRaw.slice(0, 100),
          optPairs, caPairs, reason: "cung so cap nhung khac noi dung" });
      }
    }
  }

  console.log("options JSON parse duoc : " + optionsParseable);
  console.log("options KHONG parse duoc: " + optionsUnparseable);
  console.log("");
  console.log("cap tu options TRUNG correct_answer : " + pairsFromOptionsMatchCA);
  console.log("cap tu options KHAC  correct_answer : " + pairsFromOptionsDiffer);
  console.log("so cap KHAC nhau                    : " + optionCountMismatch);
  console.log("");

  if (differExamples.length) {
    console.log("--- vi du KHAC nhau ---");
    for (const e of differExamples) {
      console.log(`exercise_id=${e.exId} lesson=${e.lessonId}  (${e.reason})`);
      console.log(`  correct_answer : ${e.ca}`);
      console.log(`  options        : ${e.optRaw}`);
      console.log(`  cap tu options : ${JSON.stringify(e.optPairs)}`);
      console.log(`  cap tu CA      : ${JSON.stringify(e.caPairs)}`);
      console.log("");
    }
  }

  console.log("=== KET LUAN ===");
  if (pairsFromOptionsDiffer === 0 && optionCountMismatch === 0 && optionsParseable > 0) {
    console.log("options la NGUON SU THAT: cap dung lay truc tiep tu options[i].split('|').");
    console.log("correct_answer chi lap lai thong tin do (o nhieu dinh dang khac nhau).");
  } else {
    console.log("options KHONG hoan toan khop correct_answer — can xem tung truong hop.");
  }
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
