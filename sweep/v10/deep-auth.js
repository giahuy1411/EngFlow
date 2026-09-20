/**
 * audit-v10 Phase 4.8 — deep check Dang nhap / Dang ki.
 *
 * Kiem: dang ki (thieu field, trung email, mat khau ngan), dang nhap (sai mat
 * khau, email khong ton tai), token (het han/khong hop le), va cac duong bao ve.
 */
const API = "http://localhost:8080";
const R = { pass: 0, fail: 0, fails: [] };
function check(name, cond, detail) {
  if (cond) { R.pass++; console.log("  PASS  " + name); }
  else { R.fail++; console.log("  FAIL  " + name + "  -> " + detail); R.fails.push(name + " -> " + detail); }
}
async function req(method, p, { token, body } = {}) {
  const h = {};
  if (token) h.Authorization = `Bearer ${token}`;
  if (body) h["Content-Type"] = "application/json";
  const r = await fetch(API + p, { method, headers: h, body: body ? JSON.stringify(body) : undefined });
  const t = await r.text();
  let d = null; try { d = JSON.parse(t); } catch { d = t.slice(0, 200); }
  return { status: r.status, data: d };
}

const STAMP = Date.now();
const NEW = `zzauth${STAMP}@example.com`;
const PASS = "AuditV10#2026";

(async () => {
  console.log("=== Phase 4.8 — deep check Dang nhap / Dang ki ===");
  console.log("");

  // ---------- DANG KI ----------
  console.log("--- dang ki ---");
  const ok = await req("POST", "/api/auth/register", {
    body: { username: "zzauth" + STAMP, email: NEW, password: PASS, fullName: "ZZ Auth" },
  });
  check("dang ki hop le -> 200/201", [200, 201].includes(ok.status), `nhan ${ok.status}: ${JSON.stringify(ok.data).slice(0, 150)}`);

  const dup = await req("POST", "/api/auth/register", {
    body: { username: "zzauthdup" + STAMP, email: NEW, password: PASS, fullName: "dup" },
  });
  check("dang ki TRUNG email -> 4xx", dup.status >= 400 && dup.status < 500, `nhan ${dup.status}`);

  const noUser = await req("POST", "/api/auth/register", {
    body: { email: `zzx${STAMP}@example.com`, password: PASS },
  });
  check("thieu username -> 400", noUser.status === 400, `nhan ${noUser.status}`);

  const noEmail = await req("POST", "/api/auth/register", {
    body: { username: "zzx" + STAMP, password: PASS },
  });
  check("thieu email -> 400", noEmail.status === 400, `nhan ${noEmail.status}`);

  const badEmail = await req("POST", "/api/auth/register", {
    body: { username: "zzx" + STAMP, email: "khong-phai-email", password: PASS },
  });
  check("email sai dinh dang -> 400", badEmail.status === 400, `nhan ${badEmail.status}`);

  const shortPw = await req("POST", "/api/auth/register", {
    body: { username: "zzx" + STAMP, email: `zzx${STAMP}@example.com`, password: "123" },
  });
  check("mat khau < 6 ky tu -> 400", shortPw.status === 400, `nhan ${shortPw.status}`);

  const shortUser = await req("POST", "/api/auth/register", {
    body: { username: "ab", email: `zzx${STAMP}@example.com`, password: PASS },
  });
  check("username < 3 ky tu -> 400", shortUser.status === 400, `nhan ${shortUser.status}`);

  // ---------- DANG NHAP ----------
  console.log("");
  console.log("--- dang nhap ---");
  const good = await req("POST", "/api/auth/login", { body: { email: NEW, password: PASS } });
  check("dang nhap dung -> 200", good.status === 200, `nhan ${good.status}`);
  const token = good.data?.token || good.data?.data?.token;
  check("tra ve token", !!token, JSON.stringify(good.data).slice(0, 150));

  const wrongPw = await req("POST", "/api/auth/login", { body: { email: NEW, password: "sai-mat-khau-xyz" } });
  check("sai mat khau -> 401", wrongPw.status === 401, `nhan ${wrongPw.status}`);

  const noSuchUser = await req("POST", "/api/auth/login", {
    body: { email: `khongtontai${STAMP}@example.com`, password: PASS },
  });
  check("email khong ton tai -> 401", noSuchUser.status === 401, `nhan ${noSuchUser.status}`);

  const emptyBody = await req("POST", "/api/auth/login", { body: {} });
  check("body rong -> 400", emptyBody.status === 400, `nhan ${emptyBody.status}`);

  // ---------- TOKEN ----------
  console.log("");
  console.log("--- token ---");
  const me = await req("GET", "/api/auth/me", { token });
  check("/me voi token -> 200", me.status === 200, `nhan ${me.status}`);

  const meNoTok = await req("GET", "/api/auth/me");
  check("/me khong token -> 401", meNoTok.status === 401, `nhan ${meNoTok.status}`);

  const meBadTok = await req("GET", "/api/auth/me", { token: "khong-phai-jwt" });
  check("/me token rac -> 401", meBadTok.status === 401, `nhan ${meBadTok.status}`);

  const meTampered = await req("GET", "/api/auth/me", { token: token ? token.slice(0, -4) + "XXXX" : "x" });
  check("/me token bi sua -> 401", meTampered.status === 401, `nhan ${meTampered.status}`);

  // ---------- THONG TIN KHONG DUOC LO ----------
  console.log("");
  console.log("--- khong lo thong tin nhay cam ---");
  const body = JSON.stringify(me.data || {});
  check("response /me KHONG chua passwordHash", !/passwordHash|password_hash/i.test(body), body.slice(0, 200));
  check("response /me KHONG chua password", !/"password"/i.test(body), body.slice(0, 200));

  const loginBody = JSON.stringify(good.data || {});
  check("response /login KHONG chua passwordHash", !/passwordHash|password_hash/i.test(loginBody), loginBody.slice(0, 200));

  // ---------- DO MANH MAT KHAU (khong lam gi, chi ghi nhan) ----------
  console.log("");
  console.log("--- ghi nhan: chinh sach mat khau ---");
  console.log("  da kiem: < 6 ky tu bi tu choi (400)");
  console.log("  KHONG kiem: do manh (chu hoa/thuong/so/ky tu dac biet) — chua co yeu cau");

  console.log("");
  console.log(`  PASS=${R.pass}  FAIL=${R.fail}`);
  if (R.fail) { console.log(""); console.log("FAILURES:"); R.fails.forEach((f) => console.log("  - " + f)); }
  console.log("");
  console.log("GHI CHU: user kich ban " + NEW + " se duoc don o buoc rieng.");
  process.exit(R.fail ? 1 : 0);
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
