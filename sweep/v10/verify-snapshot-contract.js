/**
 * audit-v10 T1.11 + T1.13 — verify contract /api/streak/snapshot tren container MOI.
 * Kiem tra tung field, khong doc luot.
 *
 * Chay: node sweep/v10/verify-snapshot-contract.js
 */
const API = "http://localhost:8080";

const R = { pass: 0, fail: 0, info: [] };

/**
 * Ngay hom nay theo gio VN — DUNG dong ho ma DB duoc ghi bang.
 *
 * KHONG dung `new Date().toISOString().slice(0,10)`: do la ngay UTC. Tu 00:00
 * den 06:59 gio VN, ngay UTC van la HOM QUA, nen mot assertion kieu do se sai
 * dung 7 tieng moi ngay (xem ghi chu cung van de trong sweep/v8/ui/lib.js).
 *
 * Truoc day file nay hard-code "2026-09-19" va no dung — cho toi khi dong ho
 * sang 20/09 thi assertion do FAIL, du app tra ve dung. Hard-code mot ngay
 * khong phai la kiem tra, do la mot qua bom hen gio.
 */
function vnToday(offsetDays = 0) {
  return new Date(Date.now() + 7 * 3600e3 + offsetDays * 86400e3)
    .toISOString().slice(0, 10);
}

function check(name, cond, detail) {
  if (cond) { R.pass++; console.log(`  PASS  ${name}`); }
  else { R.fail++; console.log(`  FAIL  ${name}  -> ${detail}`); }
}

async function login(email, pass) {
  const res = await fetch(API + "/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password: pass }),
  });
  if (res.status !== 200) return { status: res.status, token: null };
  const j = await res.json();
  return { status: res.status, token: j.token || j.data?.token || null };
}

async function get(path, token) {
  const res = await fetch(API + path, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  const text = await res.text();
  let data = null;
  try { data = JSON.parse(text); } catch { data = text.slice(0, 300); }
  return { status: res.status, data };
}

(async () => {
  console.log("=== verify /api/streak/snapshot contract ===");

  const u = await login("user@gmail.com", "123456");
  check("login user -> token", !!u.token, `status=${u.status}`);
  if (!u.token) { console.log("ABORT"); process.exit(1); }

  const snap = await get("/api/streak/snapshot", u.token);
  check("snapshot -> 200", snap.status === 200, `nhan ${snap.status}`);

  const d = snap.data || {};
  const NEED = ["today", "currentStreak", "studiedToday", "effectiveFrom",
                "studiedDays", "legacyAccessDays", "legacyHistoryAvailable"];
  for (const k of NEED) {
    check(`field '${k}' ton tai`, Object.prototype.hasOwnProperty.call(d, k),
          `thieu. co: ${Object.keys(d).join(",")}`);
  }
  const CUTOVER = "2026-09-20";
  const today = vnToday();
  const onOrAfterCutover = today >= CUTOVER;

  check(`effectiveFrom = ${CUTOVER}`, d.effectiveFrom === CUTOVER, `nhan ${d.effectiveFrom}`);
  check(`today = ${today} (TZ Asia/Ho_Chi_Minh)`, d.today === today, `nhan ${d.today}`);
  check("currentStreak = 0 (khong con dem ngay dang nhap)", d.currentStreak === 0, `nhan ${d.currentStreak}`);
  check("studiedDays la array", Array.isArray(d.studiedDays), `nhan ${typeof d.studiedDays}`);

  // Nhanh nay phu thuoc NGAY CHAY, va do chinh la y nghia cua cutover.
  if (!onOrAfterCutover) {
    // Truoc cutover: streak ngu, khong ghi gi.
    check(`studiedToday = false (${today} < cutover ${CUTOVER})`, d.studiedToday === false,
          `nhan ${d.studiedToday}`);
    check("studiedDays rong (chua toi cutover)",
          Array.isArray(d.studiedDays) && d.studiedDays.length === 0,
          `nhan ${JSON.stringify(d.studiedDays)}`);
  } else {
    // Tu cutover tro di: endpoint VAN PHAI tra 200 va studiedToday phai la
    // boolean. Day moi la khang dinh co gia tri — no chung minh nhanh
    // "today >= effectiveFrom" chay duoc that, khong nem 500.
    check("studiedToday la boolean (nhanh sau cutover chay duoc)",
          typeof d.studiedToday === "boolean", `nhan ${typeof d.studiedToday} = ${d.studiedToday}`);
    check("studiedDays la array (khong nem loi sau cutover)",
          Array.isArray(d.studiedDays), `nhan ${typeof d.studiedDays}`);
    R.info.push(`DA QUA CUTOVER (${today} >= ${CUTOVER}) — nhanh before-cutover KHONG con kiem duoc bang probe nay.`);
    R.info.push("  Xac minh nhanh do bang unit test beforeCutoverRecordsNothingAndDoesNotFailTheCaller.");
  }
  check("legacyAccessDays la array", Array.isArray(d.legacyAccessDays), `nhan ${typeof d.legacyAccessDays}`);
  check("legacyAccessDays co du lieu (Redis con key)", Array.isArray(d.legacyAccessDays) && d.legacyAccessDays.length > 0,
        `nhan ${Array.isArray(d.legacyAccessDays) ? d.legacyAccessDays.length + " phan tu" : d.legacyAccessDays}`);
  check("legacyHistoryAvailable la boolean", typeof d.legacyHistoryAvailable === "boolean",
        `nhan ${typeof d.legacyHistoryAvailable}`);

  R.info.push(`legacyAccessDays: ${Array.isArray(d.legacyAccessDays) ? d.legacyAccessDays.length : "?"} ngay`);
  if (Array.isArray(d.legacyAccessDays) && d.legacyAccessDays.length) {
    R.info.push(`  tu ${d.legacyAccessDays[0]} den ${d.legacyAccessDays[d.legacyAccessDays.length - 1]}`);
  }

  // Endpoint cu van phai chay
  const cur = await get("/api/streak/current", u.token);
  check("/api/streak/current -> 200 (backward compat)", cur.status === 200, `nhan ${cur.status}`);

  // Khong token -> 401
  const noauth = await get("/api/streak/snapshot", null);
  check("snapshot khong token -> 401", noauth.status === 401, `nhan ${noauth.status}`);

  console.log("");
  console.log(`  PASS=${R.pass}  FAIL=${R.fail}`);
  for (const i of R.info) console.log("  " + i);
  console.log("");
  console.log("GHI CHU: cutover = " + CUTOVER + ", hom nay = " + today
    + (onOrAfterCutover ? " (DA QUA cutover)" : " (TRUOC cutover)"));
  if (R.fail) process.exit(1);
})();
