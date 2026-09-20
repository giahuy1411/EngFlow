/**
 * F127 — verify tren LIVE: dang CHU phai duoc cham DUNG.
 *
 * `diag-f127-decisive.js` gui dang CHI SO va gio tra correct=false — do la
 * HANH VI MOI DUNG (chi so khong mang thong tin ve cap that, va client da
 * xao tron cot phai nen no vo nghia). Nhung do KHONG phai phep kiem fix.
 *
 * Phep kiem fix la: gui dang CHU (dinh dang client MOI gui) va phai correct=true.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const API = "http://localhost:8080";
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `v127-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/v127.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "~" -i /tmp/v127.sql'],
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

let pass = 0, fail = 0;
function check(name, cond, detail) {
  if (cond) { pass++; console.log("  PASS  " + name); }
  else { fail++; console.log("  FAIL  " + name + "  -> " + detail); }
}

(async () => {
  console.log("=== F127 verify tren LIVE ===");
  console.log("");
  const tok = await login("user@gmail.com", "123456");

  // Lay 4 bai MATCHING published, kem options de tu dung cap dung
  const samples = rows(`SELECT TOP 4
      CAST(e.exercise_id AS varchar(20)) + '~' + CAST(e.lesson_id AS varchar(20)) + '~' +
      REPLACE(REPLACE(CAST(e.options AS varchar(900)), CHAR(9),' '), CHAR(10),' ')
    FROM exercises e JOIN lessons l ON l.lesson_id = e.lesson_id
    WHERE e.exercise_type='MATCHING' AND l.is_published=1
      AND CAST(e.options AS varchar(4000)) LIKE '%|%'
    ORDER BY e.exercise_id;`);

  console.log("--- gui dang CHU (dinh dang client MOI gui) ---");
  for (const s of samples) {
    const [exId, lessonId, optRaw] = s.split("~");
    let opts = null;
    try { opts = JSON.parse(optRaw); } catch (e) { opts = null; }
    if (!Array.isArray(opts)) { console.log(`  (bo qua ${exId}: options khong parse duoc)`); continue; }

    // Dung chuoi cap CHU tu options — day la thu client gui khi noi dung het
    const textAnswer = opts.filter((o) => typeof o === "string" && o.includes("|"))
      .map((o) => { const i = o.indexOf("|"); return `${o.slice(0, i).trim()}=${o.slice(i + 1).trim()}`; })
      .join(",");
    if (!textAnswer) { console.log(`  (bo qua ${exId}: options khong co cap)`); continue; }

    const r = await post(`/api/lessons/${lessonId}/exercises/grade`, {
      lessonId: parseInt(lessonId, 10),
      answers: [{ exerciseId: parseInt(exId, 10), userAnswer: textAnswer }],
    }, tok);
    const item = r.data?.results?.[0];
    console.log(`  exercise ${exId}: correct=${item?.correct} ungradeable=${item?.ungradeable}`);
    check(`  bai ${exId} gui dang CHU -> correct=true`, item?.correct === true,
          `nhan correct=${item?.correct} ungradeable=${item?.ungradeable}`);
  }

  console.log("");
  console.log("--- thu tu XAO TRON van phai dung ---");
  const s0 = samples[0].split("~");
  let opts0 = null;
  try { opts0 = JSON.parse(s0[2]); } catch (e) {}
  if (Array.isArray(opts0)) {
    const pairs = opts0.filter((o) => typeof o === "string" && o.includes("|"))
      .map((o) => { const i = o.indexOf("|"); return `${o.slice(0, i).trim()}=${o.slice(i + 1).trim()}`; });
    const shuffled = [...pairs].reverse().join(",");
    const r = await post(`/api/lessons/${s0[1]}/exercises/grade`, {
      lessonId: parseInt(s0[1], 10),
      answers: [{ exerciseId: parseInt(s0[0], 10), userAnswer: shuffled }],
    }, tok);
    const item = r.data?.results?.[0];
    console.log(`  bai ${s0[0]} thu tu dao nguoc: correct=${item?.correct}`);
    check("  dao thu tu van correct=true", item?.correct === true, `nhan ${item?.correct}`);
  }

  console.log("");
  console.log("--- noi THIEU cap phai la SAI ---");
  if (Array.isArray(opts0)) {
    const pairs = opts0.filter((o) => typeof o === "string" && o.includes("|"))
      .map((o) => { const i = o.indexOf("|"); return `${o.slice(0, i).trim()}=${o.slice(i + 1).trim()}`; });
    const partial = pairs.slice(0, Math.max(1, pairs.length - 1)).join(",");
    const r = await post(`/api/lessons/${s0[1]}/exercises/grade`, {
      lessonId: parseInt(s0[1], 10),
      answers: [{ exerciseId: parseInt(s0[0], 10), userAnswer: partial }],
    }, tok);
    const item = r.data?.results?.[0];
    console.log(`  bai ${s0[0]} noi ${pairs.length - 1}/${pairs.length} cap: correct=${item?.correct}`);
    check("  noi thieu cap -> correct=false", item?.correct === false, `nhan ${item?.correct}`);
  }

  console.log("");
  console.log(`  PASS=${pass}  FAIL=${fail}`);
  process.exit(fail ? 1 : 0);
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
