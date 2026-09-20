/**
 * F127 — phep thu QUYET DINH.
 *
 * Lan truoc toi gui CHUOI THO cua correct_answer va no tra correct=true. Nhung
 * dieu do KHONG chung minh UI hoat dong: UI khong bao gio gui chuoi tho.
 *
 * MatchingExercise.vue dong 226:
 *   const pairs = matchedPairs.value.map(p => `${p.left}=${p.right}`).join(',')
 * trong do p.left / p.right la CHI SO VI TRI hien thi (0-based), khong phai chu.
 *
 * => Phep thu dung la: gui dang CHI SO, xem co khop duoc khong.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const API = "http://localhost:8080";
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `f127d-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/f127d.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "~" -i /tmp/f127d.sql'],
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
  console.log("=== F127: gui dang CHI SO (dung nhu UI gui) ===");
  console.log("");
  const tok = await login("user@gmail.com", "123456");

  // Lay 4 row MATCHING co dung 4 cap, thuoc bai DA PUBLISHED
  const samples = rows(`SELECT TOP 4
      CAST(e.exercise_id AS varchar(20)) + '~' + CAST(e.lesson_id AS varchar(20)) + '~' +
      CAST(e.correct_answer AS varchar(200))
    FROM exercises e JOIN lessons l ON l.lesson_id = e.lesson_id
    WHERE e.exercise_type='MATCHING' AND l.is_published = 1
      AND e.correct_answer LIKE '%=%'
    ORDER BY e.exercise_id;`);

  for (const s of samples) {
    const [exId, lessonId, ca] = s.split("~");
    const nPairs = ca.split(",").length;
    // Dang CHI SO tuan tu — day la thu UI gui khi nguoi hoc noi dung
    const indexForm = Array.from({ length: nPairs }, (_, i) => `${i}=${i}`).join(",");
    const r = await post(`/api/lessons/${lessonId}/exercises/grade`, {
      lessonId: parseInt(lessonId, 10),
      answers: [{ exerciseId: parseInt(exId, 10), userAnswer: indexForm }],
    }, tok);
    const item = r.data?.results?.[0];
    console.log(`exercise_id=${exId}  lesson=${lessonId}  so cap=${nPairs}`);
    console.log(`  correct_answer (dang luu) : ${ca}`);
    console.log(`  UI se gui                 : ${indexForm}`);
    console.log(`  -> correct = ${item?.correct}`);
    console.log("");
  }

  console.log("=== KET LUAN ===");
  console.log("Neu tat ca deu correct=false, thi UI khong the cham dung MATCHING:");
  console.log("client gui dang CHI SO, server so khop CHUOI voi dap an dang CHU.");
  console.log("");
  console.log("Doi chieu: gui CHUOI THO dung bang dap an thi correct=true");
  console.log("(da do o deep-matching.js) — chung to loi nam o SU KHONG KHOP DINH DANG");
  console.log("giua client va server, khong phai o logic cham diem.");
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
