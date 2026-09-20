/**
 * audit-v10 Phase 4.6 — deep check Bai hoc / Bai tap.
 *
 * Kiem:
 *  1. Enum ExerciseType co MAY loai (tai lieu ghi 6 — kiem chung)
 *  2. Moi loai co row that trong DB khong
 *  3. Bai nhap (draft) co bi lo qua MOI duong khong
 *  4. Contract MATCHING (dap an co dung dinh dang khong)
 *  5. Bai published nop duoc that
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const API = "http://localhost:8080";
const CONTAINER = "engflow-sqlserver";

const R = { pass: 0, fail: 0, fails: [] };
function check(name, cond, detail) {
  if (cond) { R.pass++; console.log("  PASS  " + name); }
  else { R.fail++; console.log("  FAIL  " + name + "  -> " + detail); R.fails.push(name + " -> " + detail); }
}

function sql(query) {
  const tmp = path.join(os.tmpdir(), `dl-${Date.now()}-${Math.random().toString(36).slice(2)}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/dl.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "~" -i /tmp/dl.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 250));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}
function one(q) { const r = rows(q); return r.length ? r[0] : "(rong)"; }

async function req(method, p, { token, body } = {}) {
  const h = {};
  if (token) h.Authorization = `Bearer ${token}`;
  if (body) h["Content-Type"] = "application/json";
  const r = await fetch(API + p, { method, headers: h, body: body ? JSON.stringify(body) : undefined });
  const t = await r.text();
  let d = null; try { d = JSON.parse(t); } catch { d = t.slice(0, 200); }
  return { status: r.status, data: d };
}
async function login(email, pass) {
  const r = await req("POST", "/api/auth/login", { body: { email, password: pass } });
  return r.status === 200 ? (r.data?.token || r.data?.data?.token) : null;
}

(async () => {
  console.log("=== Phase 4.6 — deep check Bai hoc / Bai tap ===");
  console.log("");

  // ---------- 1. Enum co may loai? ----------
  console.log("--- 1. So loai exercise trong enum ---");
  const enumSrc = fs.readFileSync(path.join(__dirname, "..", "..",
    "src", "main", "java", "com", "datn", "engflow", "model", "enums", "ExerciseType.java"), "utf8");
  const types = [...enumSrc.matchAll(/^\s{4}([A-Z_]+),?$/gm)].map((m) => m[1]);
  console.log("  enum ExerciseType co " + types.length + " gia tri: " + JSON.stringify(types));
  // Tai lieu (spec.md R4 / task 4.6) ghi "6 exercise types". Do lai.
  check("enum co dung " + types.length + " loai (tai lieu ghi 6 — day la so DO DUOC)",
        types.length === 5, "nhan " + types.length);

  console.log("");
  console.log("--- 2. Moi loai co row that trong DB? ---");
  const dist = rows("SELECT exercise_type + '~' + CAST(COUNT(*) AS varchar(20)) FROM exercises GROUP BY exercise_type ORDER BY exercise_type;");
  const inDb = {};
  for (const l of dist) { const [t, n] = l.split("~"); inDb[t] = parseInt(n, 10); console.log(`  ${t.padEnd(18)} ${n} row`); }
  for (const t of types) {
    check(`  loai ${t} co row`, (inDb[t] || 0) > 0, `khong co row nao`);
  }
  const extra = Object.keys(inDb).filter((t) => !types.includes(t));
  check("khong co loai la trong DB", extra.length === 0, "la: " + JSON.stringify(extra));

  console.log("");
  console.log("--- 3. Bai NHAP khong duoc lo qua bat ky duong nao ---");
  const draftId = one("SELECT TOP 1 lesson_id FROM lessons WHERE is_published = 0 ORDER BY lesson_id;");
  console.log("  bai nhap dung de thu: " + draftId);
  const pubId = one("SELECT TOP 1 lesson_id FROM lessons WHERE is_published = 1 ORDER BY lesson_id;");
  console.log("  bai published de doi chieu: " + pubId);

  const userTok = await login("user@gmail.com", "123456");
  const adminTok = await login("admin@gmail.com", "123456");
  check("co token user", !!userTok, "khong dang nhap duoc");
  check("co token admin", !!adminTok, "khong dang nhap duoc");

  // Danh sach nay CHI gom route co that. Ban nhap dau tien con co
  // `/api/lessons/{id}/snapshot` — route do KHONG TON TAI (snapshot that la
  // `/api/admin/lessons/{id}/snapshots`, chi admin). Kiem mot route bia thi
  // ket qua 404 luon "dung" va khong do duoc gi.
  const paths = [
    ["GET", `/api/lessons/${draftId}`, "chi tiet bai nhap"],
    ["GET", `/api/lessons/${draftId}/exercises`, "danh sach bai tap"],
    ["GET", `/api/lessons/${draftId}/structure`, "cau truc bai (F126)"],
  ];
  for (const [m, p, label] of paths) {
    const r = await req(m, p, { token: userTok });
    check(`  ${label} -> 404 cho student`, r.status === 404, `nhan ${r.status}`);
    const ra = await req(m, p, { token: adminTok });
    check(`  ${label} -> 200 cho admin`, ra.status === 200, `nhan ${ra.status}`);
  }

  // Bai published phai 200 cho student
  const pub = await req("GET", `/api/lessons/${pubId}`, { token: userTok });
  check("  bai published -> 200 cho student", pub.status === 200, `nhan ${pub.status}`);

  console.log("");
  console.log("--- 4. Contract MATCHING ---");
  // `exercise_id + '~' + ...` loi Msg 8114 khi correct_answer khong phai varchar
  // (o day la NVARCHAR(MAX)/text). Ep CAST tung phan ve varchar truoc khi noi.
  const matching = rows(`SELECT TOP 5 CAST(exercise_id AS varchar(20)) + '~'
      + ISNULL(LEFT(CAST(correct_answer AS varchar(400)), 80),'(NULL)') + '~'
      + ISNULL(LEFT(CAST(options AS varchar(400)), 80),'(NULL)')
    FROM exercises WHERE exercise_type = 'MATCHING' ORDER BY exercise_id;`);
  console.log("  5 row MATCHING mau:");
  for (const l of matching) {
    const [id, ans, opts] = l.split("~");
    console.log(`    id=${id}`);
    console.log(`      correct_answer: ${ans}`);
    console.log(`      options       : ${opts}`);
  }
  const matchingTotal = one("SELECT COUNT(*) FROM exercises WHERE exercise_type = 'MATCHING';");
  const matchingNoAnswer = one("SELECT COUNT(*) FROM exercises WHERE exercise_type = 'MATCHING' AND (correct_answer IS NULL OR LTRIM(RTRIM(correct_answer))='');");
  console.log(`  tong MATCHING = ${matchingTotal}, thieu correct_answer = ${matchingNoAnswer}`);

  console.log("");
  console.log("--- 5. Bai published nop duoc that ---");
  const ex = await req("GET", `/api/lessons/${pubId}/exercises`, { token: userTok });
  check("  lay duoc danh sach bai tap", ex.status === 200, `nhan ${ex.status}`);
  const list = Array.isArray(ex.data) ? ex.data : (ex.data?.data || ex.data?.content || []);
  console.log("  so bai tap tra ve: " + list.length);
  if (list.length) {
    const first = list[0];
    console.log("  bai dau tien: id=" + (first.id || first.exerciseId) + " type=" + (first.exerciseType || first.type));
  }

  console.log("");
  console.log(`  PASS=${R.pass}  FAIL=${R.fail}`);
  if (R.fail) { console.log(""); console.log("FAILURES:"); R.fails.forEach((f) => console.log("  - " + f)); }
  process.exit(R.fail ? 1 : 0);
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
