/**
 * audit-v10 Phase 4.1 — API sweep KHONG can Playwright (chi fetch).
 * Kiem tra 6 vung chuc nang: Bai hoc/Bai tap, Streak, Dang nhap/Dang ki,
 * Tim kiem/Sap xep, CRUD, AI.
 *
 * NGUYEN TAC (F109 cu): phai flush bucket rate-limit cua chinh harness truoc
 * moi batch, neu khong harness tu do chinh minh (429 gia).
 *
 * Chay: node sweep/v10/api-sweep.js
 */
const { execFileSync } = require("child_process");
const API = "http://localhost:8080";

const R = { pass: 0, fail: 0, skip: 0, findings: [] };

function flushBuckets() {
  try {
    // execFileSync (khong phai execSync): key doc tu Redis duoc truyen thang
    // thanh argv, khong qua shell. Mot key chua metacharacter se khong the
    // tro thanh lenh. Redis key la du lieu, khong phai input nguoi dung, nhung
    // van khong duoc noi chuoi lenh tu no.
    const out = execFileSync(
      "docker",
      ["exec", "engflow-redis", "redis-cli", "--scan", "--pattern", "rate_limit:*"],
      { encoding: "utf8" }
    ).trim();
    if (!out) return;
    const keys = out.split(/\r?\n/).filter(Boolean);
    if (!keys.length) return;
    execFileSync("docker", ["exec", "engflow-redis", "redis-cli", "del", ...keys], {
      encoding: "utf8",
    });
  } catch (e) {
    console.log("  (flush warn: " + e.message.slice(0, 50) + ")");
  }
}

async function req(method, path, { token, body, raw } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body && !raw) headers["Content-Type"] = "application/json";
  try {
    const res = await fetch(API + path, {
      method,
      headers,
      body: body ? (raw ? body : JSON.stringify(body)) : undefined,
    });
    let data = null;
    const text = await res.text();
    try { data = JSON.parse(text); } catch { data = text.slice(0, 200); }
    return { status: res.status, data };
  } catch (e) {
    return { status: "ERR", data: e.message };
  }
}

function check(name, cond, detail) {
  if (cond) { R.pass++; console.log(`  PASS  ${name}`); }
  else { R.fail++; console.log(`  FAIL  ${name}  -> ${detail}`); R.findings.push({ name, detail }); }
}

async function login(email, pass) {
  const r = await req("POST", "/api/auth/login", { body: { email, password: pass } });
  if (r.status !== 200) return null;
  return (r.data?.data?.token) || r.data?.token || null;
}

(async () => {
  console.log("=== audit-v10 API sweep (khong Playwright) ===");
  console.log("target:", API);
  console.log("");

  // ---------- 1. DANG NHAP / DANG KI ----------
  console.log("--- 1. Dang nhap / Dang ki ---");
  flushBuckets();
  const userTok = await login("user@gmail.com", "123456");
  const adminTok = await login("admin@gmail.com", "123456");
  check("login user -> co token", !!userTok, "khong lay duoc token");
  check("login admin -> co token", !!adminTok, "khong lay duoc token");

  const badLogin = await req("POST", "/api/auth/login", { body: { email: "user@gmail.com", password: "sai-mat-khau" } });
  check("login sai mat khau -> 401", badLogin.status === 401, `nhan ${badLogin.status}`);

  const me = await req("GET", "/api/auth/me", { token: userTok });
  check("/api/auth/me voi token -> 200", me.status === 200, `nhan ${me.status}`);
  check("me tra ve email dung", me.data?.email === "user@gmail.com" || me.data?.data?.email === "user@gmail.com", JSON.stringify(me.data).slice(0, 80));

  const noTok = await req("GET", "/api/auth/me");
  check("/api/auth/me khong token -> 401", noTok.status === 401, `nhan ${noTok.status}`);

  // ---------- 2. BAI HOC / BAI TAP ----------
  console.log("");
  console.log("--- 2. Bai hoc / Bai tap ---");
  flushBuckets();
  const lessons = await req("GET", "/api/lessons?page=0&size=10");
  check("GET /api/lessons -> 200", lessons.status === 200, `nhan ${lessons.status}`);

  const lessonId = lessons.data?.content?.[0]?.id || lessons.data?.data?.content?.[0]?.id;
  if (lessonId) {
    const detail = await req("GET", `/api/lessons/${lessonId}`, { token: userTok });
    check(`GET /api/lessons/${lessonId} -> 200`, detail.status === 200, `nhan ${detail.status}`);

    const ex = await req("GET", `/api/lessons/${lessonId}/exercises`, { token: userTok });
    check("GET exercises -> 200", ex.status === 200, `nhan ${ex.status}`);

    // Bai nhap phai 404 cho student (F88/F105/F115)
    const draftProbe = await req("GET", "/api/lessons/102049/exercises", { token: userTok });
    check("bai nhap 102049 -> 404 cho student", draftProbe.status === 404, `nhan ${draftProbe.status} (neu 200 = LO HONG)`);
  } else {
    R.skip++; console.log("  SKIP  khong lay duoc lessonId");
  }

  // ---------- 3. STREAK ----------
  console.log("");
  console.log("--- 3. Streak ---");
  flushBuckets();
  const snap = await req("GET", "/api/streak/snapshot", { token: userTok });
  if (snap.status === 404) {
    R.skip++; console.log("  SKIP  /api/streak/snapshot = 404 (container chua rebuild)");
  } else {
    check("/api/streak/snapshot -> 200", snap.status === 200, `nhan ${snap.status}`);
    const d = snap.data || {};
    const need = ["today","currentStreak","studiedToday","effectiveFrom","studiedDays","legacyAccessDays","legacyHistoryAvailable"];
    for (const k of need) {
      check(`snapshot co field '${k}'`, k in d, `thieu ${k}`);
    }
    check("effectiveFrom = 2026-09-20", d.effectiveFrom === "2026-09-20", `nhan ${d.effectiveFrom}`);
    // KHONG hard-code ngay. Truoc day dong nay ghi "(19/09 < cutover)" va no
    // dung — cho toi khi dong ho sang 20/09 thi no FAIL du app tra ve dung.
    // Mot ngay hard-code khong phai la kiem tra, do la mot qua bom hen gio.
    // Sau cutover, `studiedToday` hop le o CA HAI gia tri (true neu user nay
    // da hoc hom nay, false neu chua) — nen kiem KIEU, khong kiem gia tri.
    const todayVN = new Date(Date.now() + 7 * 3600e3).toISOString().slice(0, 10);
    check("today = ngay VN hien tai", d.today === todayVN, `nhan ${d.today}, ky vong ${todayVN}`);
    if (todayVN < "2026-09-20") {
      check("studiedToday = false (truoc cutover)", d.studiedToday === false, `nhan ${d.studiedToday}`);
    } else {
      check("studiedToday la boolean (sau cutover, ca 2 gia tri deu hop le)",
            typeof d.studiedToday === "boolean", `nhan ${typeof d.studiedToday}`);
    }
  }

  const cur = await req("GET", "/api/streak/current", { token: userTok });
  check("/api/streak/current -> 200", cur.status === 200, `nhan ${cur.status}`);

  const snapNoAuth = await req("GET", "/api/streak/snapshot");
  check("snapshot khong token -> 401", snapNoAuth.status === 401, `nhan ${snapNoAuth.status}`);

  // ---------- 4. TIM KIEM / SAP XEP ----------
  console.log("");
  console.log("--- 4. Tim kiem / Sap xep ---");
  flushBuckets();
  const search = await req("GET", "/api/vocabulary/search?q=hello");
  check("vocab/search (permitAll) -> 200", search.status === 200, `nhan ${search.status}`);

  const sortTest = await req("GET", "/api/lessons?page=0&size=5&sort=id,desc");
  check("lessons sort=id,desc -> 200", sortTest.status === 200, `nhan ${sortTest.status}`);

  const adminSearch = await req("GET", "/api/admin/exercises?q=the&page=0&size=5", { token: adminTok });
  check("admin/exercises?q= -> 200", adminSearch.status === 200, `nhan ${adminSearch.status}`);

  const pageNeg = await req("GET", "/api/lessons?page=-1&size=10");
  check("page=-1 -> khong 500", pageNeg.status !== 500, `nhan ${pageNeg.status}`);

  // ---------- 5. CRUD (admin) ----------
  console.log("");
  console.log("--- 5. CRUD (admin) ---");
  flushBuckets();
  const aLessons = await req("GET", "/api/admin/lessons?page=0&size=5", { token: adminTok });
  check("admin/lessons -> 200", aLessons.status === 200, `nhan ${aLessons.status}`);

  const aVocab = await req("GET", "/api/admin/vocabulary?page=0&size=5", { token: adminTok });
  check("admin/vocabulary -> 200", aVocab.status === 200, `nhan ${aVocab.status}`);

  const aUsers = await req("GET", "/api/admin/users?page=0&size=5", { token: adminTok });
  check("admin/users -> 200", aUsers.status === 200, `nhan ${aUsers.status}`);

  // Guard 2 chieu: admin endpoint phai chan student
  const studentOnAdmin = await req("GET", "/api/admin/users?page=0&size=5", { token: userTok });
  check("admin/users voi token student -> 403", studentOnAdmin.status === 403, `nhan ${studentOnAdmin.status} (neu 200 = LO HONG)`);

  const anonOnAdmin = await req("GET", "/api/admin/users?page=0&size=5");
  check("admin/users khong token -> 401/403", [401, 403].includes(anonOnAdmin.status), `nhan ${anonOnAdmin.status}`);

  // ---------- 6. AI ----------
  console.log("");
  console.log("--- 6. AI ---");
  flushBuckets();
  const aiEmpty = await req("POST", "/api/ai/generate-vocab", { token: userTok, body: { topic: "" } });
  check("ai/generate-vocab topic rong -> 400", aiEmpty.status === 400, `nhan ${aiEmpty.status}`);
  check("loi tra ProblemDetail (co title)", !!(aiEmpty.data && aiEmpty.data.title) || !!(aiEmpty.data && aiEmpty.data.error), JSON.stringify(aiEmpty.data).slice(0, 80));

  const aiNoAuth = await req("POST", "/api/ai/generate-vocab", { body: { topic: "travel" } });
  check("ai/generate-vocab khong token -> 401", aiNoAuth.status === 401, `nhan ${aiNoAuth.status}`);

  // ---------- TONG KET ----------
  console.log("");
  console.log("============================================");
  console.log(`  PASS=${R.pass}  FAIL=${R.fail}  SKIP=${R.skip}`);
  console.log("============================================");
  if (R.findings.length) {
    console.log("");
    console.log("FINDINGS:");
    for (const f of R.findings) console.log(`  - ${f.name}: ${f.detail}`);
  }
  console.log("");
  console.log("GHI CHU: sweep nay KHONG tao row nao (chi GET + 2 POST loi co chu dich).");
  console.log("         Khong can cleanup, nhung van nen chay parity sau.");
})();
