/**
 * audit-v10 Phase 4.6b — MATCHING: hai dinh dang dap an co lam hong UI khong?
 *
 * DO DUOC:
 *   - correct_answer dang CHU : "word1=def1,word2=def2,..."
 *   - correct_answer dang CHI SO: "A=D,B=A,C=B,D=C"
 *
 * Frontend MatchingExercise.vue dong 176-185 lam `parseInt` tren moi ve cua
 * moi cap, va BO QUA cap nao khong parse duoc. Voi dinh dang chu, TAT CA deu
 * NaN -> correctPairs rong -> roi vao fallback "tuan tu" (dong 187-192).
 *
 * Cau hoi: dieu do co lam nguoi hoc tra loi dung van bi cham sai khong?
 * Tra loi bang cach goi /grade that voi cac cau tra loi khac nhau.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const API = "http://localhost:8080";
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `dm-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/dm.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "~" -i /tmp/dm.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 250));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}
async function post(p, body, token) {
  const h = { "Content-Type": "application/json" };
  if (token) h.Authorization = `Bearer ${token}`;
  const r = await fetch(API + p, { method: "POST", headers: h, body: JSON.stringify(body) });
  const t = await r.text();
  let d = null; try { d = JSON.parse(t); } catch { d = t.slice(0, 300); }
  return { status: r.status, data: d };
}
async function login(e, p) {
  const r = await post("/api/auth/login", { email: e, password: p });
  return r.data?.token || r.data?.data?.token;
}

(async () => {
  console.log("=== MATCHING: 2 dinh dang dap an ===");
  console.log("");

  // Phan bo 2 dinh dang
  const wordForm = rows(`SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises
    WHERE exercise_type='MATCHING' AND correct_answer LIKE '%word%=%' ;`)[0];
  const letterForm = rows(`SELECT CAST(COUNT(*) AS varchar(10)) FROM exercises
    WHERE exercise_type='MATCHING' AND correct_answer NOT LIKE '%word%=%'
      AND correct_answer LIKE '%=%';`)[0];
  console.log(`dang CHU  ("word1=def1,...") : ${wordForm} row`);
  console.log(`dang CHI SO/CHU CAI ("A=D,.."): ${letterForm} row`);
  console.log("");

  // Lay 1 bai MATCHING dang CHU va 1 dang CHU CAI
  const wordRow = rows(`SELECT TOP 1 CAST(exercise_id AS varchar(20)) + '~' + CAST(lesson_id AS varchar(20)) + '~'
    + CAST(correct_answer AS varchar(500)) FROM exercises
    WHERE exercise_type='MATCHING' AND correct_answer LIKE '%word%=%';`)[0].split("~");
  const letterRow = rows(`SELECT TOP 1 CAST(exercise_id AS varchar(20)) + '~' + CAST(lesson_id AS varchar(20)) + '~'
    + CAST(correct_answer AS varchar(500)) FROM exercises
    WHERE exercise_type='MATCHING' AND correct_answer NOT LIKE '%word%=%' AND correct_answer LIKE '%=%';`)[0].split("~");

  const userTok = await login("user@gmail.com", "123456");
  console.log("--- A. Dang CHU ---");
  console.log(`exercise_id=${wordRow[0]} lesson_id=${wordRow[1]}`);
  console.log(`correct_answer = ${wordRow[2]}`);
  const ansWord = wordRow[2].replace(/,\s*/g, ",");
  for (const [label, a] of [
    ["gui DUNG chuoi dap an", ansWord],
    ["gui chuoi da chuan hoa (lowercase, bo space)", ansWord.toLowerCase().replace(/\s+/g, " ")],
    ["gui chuoi RONG", ""],
  ]) {
    const r = await post(`/api/lessons/${wordRow[1]}/exercises/grade`, {
      lessonId: parseInt(wordRow[1], 10),
      answers: [{ exerciseId: parseInt(wordRow[0], 10), userAnswer: a }],
    }, userTok);
    const item = r.data?.results?.[0];
    console.log(`  ${label.padEnd(46)} status=${r.status} correct=${item?.correct} ungradeable=${item?.ungradeable}`);
  }

  console.log("");
  console.log("--- B. Dang CHU CAI ---");
  console.log(`exercise_id=${letterRow[0]} lesson_id=${letterRow[1]}`);
  console.log(`correct_answer = ${letterRow[2]}`);
  for (const [label, a] of [
    ["gui DUNG chuoi dap an", letterRow[2]],
    ["gui chuoi RONG", ""],
  ]) {
    const r = await post(`/api/lessons/${letterRow[1]}/exercises/grade`, {
      lessonId: parseInt(letterRow[1], 10),
      answers: [{ exerciseId: parseInt(letterRow[0], 10), userAnswer: a }],
    }, userTok);
    const item = r.data?.results?.[0];
    console.log(`  ${label.padEnd(46)} status=${r.status} correct=${item?.correct} ungradeable=${item?.ungradeable}`);
  }

  console.log("");
  console.log("--- ket luan ve kha nang cham ---");
  console.log("Server cham bang SO KHOP CHUOI (ExerciseService dong 187-188):");
  console.log("  normalize(userAnswer) == normalize(correctAnswer)");
  console.log("Nen CHI CAN client gui lai dung chuoi la duoc diem, du dinh dang nao.");
  console.log("Rui ro nam o CLIENT: parseInt bo qua moi ve khong phai so, nen");
  console.log("correctPairs rong -> fallback 'tuan tu' -> nguoi hoc co the noi SAI thu tu");
  console.log("ma van duoc diem, hoac noi dung ma bi coi la sai.");
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
