/**
 * F127 — do PHAM VI: bao nhieu bai MATCHING co dinh dang dap an KHONG THE cham dung?
 *
 * Server cham bang so khop chuoi:
 *   normalize(userAnswer) == normalize(correctAnswer)
 *
 * Client (MatchingExercise.vue) gui len `"<left>=<right>,..."` trong do
 * left/right la CHI SO hoac VI TRI, KHONG BAO GIO la chu.
 *
 * => Neu correct_answer chua chu (khong phai so), client khong the tao ra chuoi
 *    khop, va bai do KHONG BAO GIO duoc diem du nguoi hoc noi dung het.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `f127-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/f127.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "~" -i /tmp/f127.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 250));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}

console.log("=== F127: pham vi anh huong ===");
console.log("");
console.log("Tieu chi: correct_answer la chuoi cac cap 'X=Y' ma X hoac Y KHONG phai so.");
console.log("Client chi gui duoc dang CHI SO (0=0,1=1,...), nen nhung row nay");
console.log("khong the khop du nguoi hoc lam dung.");
console.log("");

const total = rows("SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises WHERE exercise_type='MATCHING';")[0];
console.log("tong MATCHING                        : " + total);

// Dang CHI SO: moi ve deu la so -> client co the khop
const numeric = rows(`SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises
  WHERE exercise_type='MATCHING'
    AND correct_answer NOT LIKE '%[^0-9=, ]%' AND correct_answer LIKE '%=%';`)[0];
console.log("dang CHI SO (client khop duoc)       : " + numeric);

// Dang co chu: co it nhat mot ky tu khong phai so/=/,/space
const textual = rows(`SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises
  WHERE exercise_type='MATCHING'
    AND correct_answer LIKE '%[^0-9=, ]%';`)[0];
console.log("dang CO CHU (client KHONG khop duoc) : " + textual + "   <-- F127");

console.log("");
console.log("--- vi du 3 row dang CO CHU ---");
for (const l of rows(`SELECT TOP 3 CAST(exercise_id AS varchar(20)) + ' | ' + CAST(correct_answer AS varchar(160))
  FROM exercises WHERE exercise_type='MATCHING' AND correct_answer LIKE '%[^0-9=, ]%';`)) {
  console.log("  " + l);
}

console.log("");
console.log("--- 3 row dang CHI SO (de doi chieu) ---");
for (const l of rows(`SELECT TOP 3 CAST(exercise_id AS varchar(20)) + ' | ' + CAST(correct_answer AS varchar(160))
  FROM exercises WHERE exercise_type='MATCHING'
    AND correct_answer NOT LIKE '%[^0-9=, ]%' AND correct_answer LIKE '%=%';`)) {
  console.log("  " + l);
}

console.log("");
console.log("--- phan bo theo lesson (co chu) ---");
for (const l of rows(`SELECT TOP 10 'lesson ' + CAST(lesson_id AS varchar(20)) + ': ' + CAST(COUNT(*) AS varchar(10)) + ' row'
  FROM exercises WHERE exercise_type='MATCHING' AND correct_answer LIKE '%[^0-9=, ]%'
  GROUP BY lesson_id ORDER BY COUNT(*) DESC;`)) {
  console.log("  " + l);
}
