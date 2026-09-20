/**
 * F127 — phan loai CHINH XAC ca 331 row MATCHING published.
 *
 * Cau hoi: bao nhieu row THUC SU cham duoc, bao nhieu row la du lieu placeholder
 * chua bao gio duoc dien?
 *
 * Khong sua gi. Chi do.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `cls-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/cls.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "\t" -i /tmp/cls.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 250));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}

// KHONG dung '~' lam dau phan cach: du lieu that co chua '~' trong options
// (da gap TypeError vi optRaw undefined). Dung tab — va chuan hoa tab trong
// du lieu truoc khi ghep, de so cot luon = 3.
const data = rows(`SELECT TOP 400
    CAST(e.exercise_id AS varchar(20)) + CHAR(9) +
    REPLACE(REPLACE(CAST(e.correct_answer AS varchar(400)), CHAR(9), ' '), CHAR(10), ' ') + CHAR(9) +
    REPLACE(REPLACE(ISNULL(CAST(e.options AS varchar(2000)), ''), CHAR(9), ' '), CHAR(10), ' ')
  FROM exercises e JOIN lessons l ON l.lesson_id = e.lesson_id
  WHERE e.exercise_type='MATCHING' AND l.is_published=1
  ORDER BY e.exercise_id;`);

console.log("tong row published MATCHING: " + data.length);
console.log("");

const cat = {
  PLACEHOLDER_CA: 0,        // correct_answer chua wordN=defN (chua dien)
  OPTIONS_GARBAGE: 0,       // options khong parse duoc JSON hoac khong co '|'
  CA_LETTERS_OK: 0,         // CA dang A=B... va options khop
  CA_WORDS_OK: 0,           // CA dang word1=... va options khop
  MISMATCH: 0,              // ca hai doc duoc nhung khong khop
};
const examples = {};

for (const line of data) {
  const parts = line.split("\t");
  const [exId, ca, optRaw] = parts;
  if (parts.length < 3) {
    // Khong nen xay ra sau khi chuan hoa tab. Neu xay ra thi bao, khong im lang.
    console.log("  CANH BAO: dong khong du 3 cot (" + parts.length + "): " + line.slice(0, 120));
    continue;
  }

  const isPlaceholder = /word\d+=def\d+/i.test(ca || "");
  let opts = null;
  try { opts = JSON.parse(optRaw); } catch (e) { opts = null; }
  const optsUsable = Array.isArray(opts) && opts.length > 0
    && opts.every((o) => typeof o === "string" && o.includes("|"));

  if (isPlaceholder) {
    cat.PLACEHOLDER_CA++;
    if (!examples.PLACEHOLDER_CA) examples.PLACEHOLDER_CA = { exId, ca: String(ca).slice(0, 80), optRaw: optRaw.slice(0, 120) };
    continue;
  }
  if (!optsUsable) {
    cat.OPTIONS_GARBAGE++;
    if (!examples.OPTIONS_GARBAGE) examples.OPTIONS_GARBAGE = { exId, ca: String(ca).slice(0, 80), optRaw: optRaw.slice(0, 120) };
    continue;
  }

  // So sanh cap tu options vs cap tu correct_answer
  const optPairs = opts.map((o) => {
    const i = o.indexOf("|");
    return o.slice(0, i).trim().toLowerCase() + "=" + o.slice(i + 1).trim().toLowerCase();
  }).sort().join("|");

  const caPairs = String(ca || "").split(",").map((p) => {
    const i = p.indexOf("=");
    if (i <= 0) return null;
    return p.slice(0, i).trim().toLowerCase() + "=" + p.slice(i + 1).trim().toLowerCase();
  }).filter(Boolean).sort().join("|");

  if (optPairs === caPairs) {
    if (/^[a-z]=[a-z](,[a-z]=[a-z])*$/i.test(String(ca).trim())) cat.CA_LETTERS_OK++;
    else cat.CA_WORDS_OK++;
  } else {
    cat.MISMATCH++;
    if (!examples.MISMATCH) examples.MISMATCH = { exId, ca: String(ca).slice(0, 80), optRaw: optRaw.slice(0, 120) };
  }
}

console.log("=== PHAN LOAI ===");
console.log(`  PLACEHOLDER_CA (dap an chua duoc dien: "wordN=defN")  : ${cat.PLACEHOLDER_CA}`);
console.log(`  OPTIONS_GARBAGE (options khong co cap "left|right")   : ${cat.OPTIONS_GARBAGE}`);
console.log(`  CA_LETTERS_OK  (CA dang chu cai, khop options)        : ${cat.CA_LETTERS_OK}`);
console.log(`  CA_WORDS_OK    (CA dang chu, khop options)            : ${cat.CA_WORDS_OK}`);
console.log(`  MISMATCH       (ca hai doc duoc nhung khong khop)     : ${cat.MISMATCH}`);
console.log("");
const usable = cat.CA_LETTERS_OK + cat.CA_WORDS_OK;
console.log(`  => CHAM DUOC (neu client gui dung dinh dang)          : ${usable}`);
console.log(`  => KHONG THE CHAM                                     : ${cat.PLACEHOLDER_CA + cat.OPTIONS_GARBAGE + cat.MISMATCH}`);
console.log("");
console.log("=== vi du moi loai ===");
for (const [k, v] of Object.entries(examples)) {
  console.log(`  ${k}: exercise_id=${v.exId}`);
  console.log(`    correct_answer: ${v.ca}`);
  console.log(`    options       : ${v.optRaw}`);
}
