/**
 * F126 buoc 2 — bai nhap 10888 tra mang RONG, nen CHUA chung minh duoc ro ri.
 * Phai tim mot bai NHAP co section THAT roi thu lai.
 *
 * Neu khong bai nhap nao co section -> severity thap han nhieu (chi la 200 vs
 * 404, khong ro noi dung).
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const API = "http://localhost:8080";
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `f126b-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/f126b.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "~" -i /tmp/f126b.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 250));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}
async function get(p, token) {
  const r = await fetch(API + p, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  const t = await r.text();
  let d = null; try { d = JSON.parse(t); } catch { d = t.slice(0, 200); }
  return { status: r.status, data: d };
}
async function login(e, p) {
  const res = await fetch(API + "/api/auth/login", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: e, password: p }),
  });
  const j = await res.json();
  return j.token || j.data?.token;
}

(async () => {
  console.log("=== F126 buoc 2: bai nhap nao CO section? ===");
  console.log("");

  // Bang section ten gi?
  const tables = rows("SELECT name FROM sys.tables WHERE name LIKE '%section%' ORDER BY name;");
  console.log("bang lien quan 'section': " + JSON.stringify(tables));
  if (!tables.length) { console.log(">>> khong co bang section"); process.exit(0); }
  const T = tables[0];

  const cols = rows(`SELECT name FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.${T}') ORDER BY column_id;`);
  console.log(`cot cua ${T}: ` + cols.join(", "));
  console.log("");

  // Phan bo: bai nhap vs published, co section hay khong
  const lessonCol = cols.find((c) => /lesson/i.test(c)) || "lesson_id";
  // `ORDER BY 1, 2` o day loi: SELECT chi tra VE MOT cot (chuoi da noi), nen
  // "position number 2" khong ton tai. Sap xep theo chinh bieu thuc.
  const dist = rows(`SELECT
    CASE WHEN l.is_published = 1 THEN 'published' ELSE 'draft' END + '~' +
    CASE WHEN s.n IS NULL OR s.n = 0 THEN 'khong section' ELSE 'CO section' END + '~' +
    CAST(COUNT(*) AS varchar(10)) AS line
    FROM lessons l
    LEFT JOIN (SELECT ${lessonCol} AS lid, COUNT(*) AS n FROM ${T} GROUP BY ${lessonCol}) s ON s.lid = l.lesson_id
    GROUP BY CASE WHEN l.is_published = 1 THEN 'published' ELSE 'draft' END,
             CASE WHEN s.n IS NULL OR s.n = 0 THEN 'khong section' ELSE 'CO section' END
    ORDER BY line;`);
  console.log("phan bo bai hoc theo (published/draft) x (co/khong section):");
  for (const l of dist) console.log("  " + l.replace("~", " | "));
  console.log("");

  // Tim bai NHAP co section that
  const draftsWithSections = rows(`SELECT TOP 5 CAST(l.lesson_id AS varchar(20)) + '~' + l.title + '~' + CAST(COUNT(s.${lessonCol}) AS varchar(10))
    FROM lessons l JOIN ${T} s ON s.${lessonCol} = l.lesson_id
    WHERE l.is_published = 0
    GROUP BY l.lesson_id, l.title
    HAVING COUNT(s.${lessonCol}) > 0
    ORDER BY l.lesson_id;`);

  if (!draftsWithSections.length) {
    console.log(">>> KHONG co bai NHAP nao co section.");
    console.log("    Nghia la /api/lessons/{id}/structure tra 200 nhung mang RONG cho moi bai nhap");
    console.log("    -> khong ro noi dung that. Day la loi STATUS CODE, khong phai ro ri du lieu.");
    process.exit(0);
  }

  console.log(">>> TIM THAY bai NHAP co section:");
  const userTok = await login("user@gmail.com", "123456");
  for (const l of draftsWithSections) {
    const [id, title, n] = l.split("~");
    console.log(`  lesson ${id} "${title}" — ${n} section trong DB`);
    const r = await get(`/api/lessons/${id}/structure`, userTok);
    const arr = Array.isArray(r.data) ? r.data : [];
    const bodyLen = JSON.stringify(r.data).length;
    console.log(`    STUDENT -> status=${r.status}  so section tra ve=${arr.length}  body_len=${bodyLen}`);
    if (arr.length) {
      console.log(`    >>> RO RI THAT: student doc duoc ${arr.length} section cua bai NHAP`);
      console.log(`    >>> noi dung: ${JSON.stringify(arr[0]).slice(0, 250)}`);
    }
    const rp = await get(`/api/lessons/${id}`, userTok);
    console.log(`    (doi chieu /api/lessons/${id} -> ${rp.status})`);
  }
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
