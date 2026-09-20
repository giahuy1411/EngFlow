/**
 * F126 — xac nhan /api/lessons/{id}/structure lo noi dung bai NHAP cho student.
 *
 * Phai chung minh: (a) endpoint tra 200, (b) body CHUA noi dung that cua bai
 * nhap (khong phai mang rong), (c) bai do THAT SU la nhap (is_published=0).
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const API = "http://localhost:8080";
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `f126-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/f126.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "~" -i /tmp/f126.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
async function req(method, p, { token } = {}) {
  const h = {}; if (token) h.Authorization = `Bearer ${token}`;
  const r = await fetch(API + p, { method, headers: h });
  const t = await r.text();
  let d = null; try { d = JSON.parse(t); } catch { d = t.slice(0, 300); }
  return { status: r.status, data: d, raw: t };
}
async function login(e, p) {
  const r = await req("POST", "/api/auth/login");
  const res = await fetch(API + "/api/auth/login", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: e, password: p }),
  });
  const j = await res.json();
  return j.token || j.data?.token;
}

(async () => {
  console.log("=== F126: structure endpoint vs bai nhap ===");
  console.log("");

  const draftId = sql("SELECT TOP 1 CAST(lesson_id AS varchar(20)) FROM lessons WHERE is_published = 0 ORDER BY lesson_id;").trim();
  console.log("bai nhap: " + draftId);
  const meta = sql(`SELECT title + '~' + CAST(is_published AS varchar(2)) FROM lessons WHERE lesson_id = ${draftId};`).trim();
  const [title, pub] = meta.split("~");
  console.log(`  title="${title}"  is_published=${pub}`);
  console.log("");

  const userTok = await login("user@gmail.com", "123456");
  const adminTok = await login("admin@gmail.com", "123456");
  const anon = await req("GET", `/api/lessons/${draftId}/structure`);

  console.log("--- goi /api/lessons/" + draftId + "/structure ---");
  for (const [label, tok] of [["ANON (khong token)", null], ["STUDENT", userTok], ["ADMIN", adminTok]]) {
    const r = tok === null ? anon : await req("GET", `/api/lessons/${draftId}/structure`, { token: tok });
    const len = typeof r.data === "string" ? r.data.length : JSON.stringify(r.data).length;
    const arr = Array.isArray(r.data) ? r.data : (r.data?.data || []);
    console.log(`  ${label.padEnd(20)} status=${r.status}  so section=${Array.isArray(arr) ? arr.length : "?"}  body_len=${len}`);
    if (Array.isArray(arr) && arr.length) {
      const s = arr[0];
      console.log(`     section[0] keys: ${Object.keys(s).join(", ")}`);
      const txt = JSON.stringify(s).slice(0, 220);
      console.log(`     noi dung: ${txt}`);
    }
  }

  console.log("");
  console.log("--- so sanh: bai PUBLISHED tra ve gi? ---");
  const pubId = sql("SELECT TOP 1 CAST(lesson_id AS varchar(20)) FROM lessons WHERE is_published = 1 ORDER BY lesson_id;").trim();
  const rp = await req("GET", `/api/lessons/${pubId}/structure`, { token: userTok });
  const arrP = Array.isArray(rp.data) ? rp.data : [];
  console.log(`  bai published ${pubId}: status=${rp.status} so section=${arrP.length}`);

  console.log("");
  console.log("--- ket luan ---");
  const rStudent = await req("GET", `/api/lessons/${draftId}/structure`, { token: userTok });
  const arrS = Array.isArray(rStudent.data) ? rStudent.data : [];
  const leaks = rStudent.status === 200 && arrS.length > 0;
  console.log(leaks
    ? `>>> LO HONG: student doc duoc ${arrS.length} section cua bai NHAP (is_published=0) qua /api/lessons/{id}/structure`
    : `>>> Khong lo: status=${rStudent.status}, section=${arrS.length}`);
  console.log("");
  console.log("So sanh voi cac duong DA duoc chan (de thay day la mot lo hong that):");
  for (const p of [`/api/lessons/${draftId}`, `/api/lessons/${draftId}/exercises`, `/api/lessons/${draftId}/snapshot`]) {
    const r = await req("GET", p, { token: userTok });
    console.log(`  ${r.status}  ${p}`);
  }
  console.log(`  200  /api/lessons/${draftId}/structure   <-- KHAC BIET`);
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
