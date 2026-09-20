/**
 * audit-v10 Phase 1.13 — 5 kich ban streak end-to-end, chay that qua HTTP.
 *
 * DAY LA PHAN COT LOI CUA CAI REFACTOR. Cau hoi no phai tra loi duoc:
 *   "dang nhap KHONG phai la ngay hoc; chi hoan thanh hoat dong hoc moi tinh."
 * Neu chi doc code thi chua chung minh duoc gi — phai ghi row that roi doc lai.
 *
 * NGUYEN TAC AN TOAN
 * ------------------
 * - Tao mot user RIENG cho kich ban (email sinh tu timestamp) de khong bao gio
 *   cham vao du lieu cua user that. Xoa sach o cuoi, bang ID cu the.
 * - Moi buoc deu doc lai bang SQL, khong tin response HTTP tu bao.
 * - Neu mot buoc nem, cleanup van chay (finally).
 *
 * Chay: node sweep/v10/streak-scenarios.js
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const API = "http://localhost:8080";
const CONTAINER = "engflow-sqlserver";

const R = { pass: 0, fail: 0, steps: [] };

function check(name, cond, detail) {
  if (cond) { R.pass++; console.log(`  PASS  ${name}`); }
  else { R.fail++; console.log(`  FAIL  ${name}  -> ${detail}`); }
  R.steps.push({ name, cond, detail });
}

/**
 * Chay SQL qua sqlcmd trong container.
 *
 * BA CACH SAI DA BI LOAI (do bang sweep/v10/diag-sql.js, khong suy doan):
 *
 * 1. `-Q ${JSON.stringify(query)}` — JSON.stringify bien newline thanh hai ky
 *    tu `\` + `n` LITERAL. sqlcmd nhan duoc chuoi mot dong chua `\n` va bao
 *    `Msg 102 Incorrect syntax near '\'`. Query nhieu dong (moi cleanup) vi
 *    the KHONG BAO GIO chay, ma sqlcmd van exit 0.
 * 2. Truyen `-i /dev/stdin` qua `docker exec` khong co stdin -> khong chay gi.
 * 3. Regex `/-?\d+/` tren output sqlcmd bat SO DAU TIEN trong chuoi — ke ca
 *    khi do la ma loi. Voi `SELECT id FROM users` (cot khong ton tai) output
 *    la `Msg 207, Level 16, ...` nen no tra ve **207** va ham trong nhu thanh
 *    cong. Do la ly do user rac mang id "207" va vocabId "207".
 *
 * Cach dung: ghi query ra file tam, `docker cp` vao container, chay `-i file`.
 * Khong di qua shell string nao, nen newline va metacharacter deu an toan.
 */
function sql(query) {
  const tmp = path.join(os.tmpdir(), `auditv10-${process.pid}-${Date.now()}.sql`);
  fs.writeFileSync(tmp, query, "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/q.sql`], { encoding: "utf8" });
    const out = execFileSync("docker", [
      "exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -i /tmp/q.sql',
    ], { encoding: "utf8" });
    return out;
  } finally {
    try { fs.unlinkSync(tmp); } catch (e) {}
  }
}

/**
 * Doc mot so nguyen duy nhat. TU CHOI tra ve gia tri neu output co `Msg N`
 * — mot ma loi khong bao gio duoc phep tro thanh mot ket qua.
 */
function sqlNum(query) {
  const out = sql("SET NOCOUNT ON;\n" + query + "\n");
  if (/Msg \d+/.test(out)) {
    throw new Error(`SQL loi khi chay [${query.replace(/\s+/g, " ").slice(0, 60)}]: ${out.trim().slice(0, 160)}`);
  }
  const m = out.match(/-?\d+/);
  return m ? parseInt(m[0], 10) : null;
}

/** Chay mot batch ghi; nem neu co Msg (sqlcmd exit 0 ke ca khi that bai). */
function sqlExec(query) {
  const out = sql("SET QUOTED_IDENTIFIER ON;\n" + query + "\n");
  const bad = out.match(/Msg \d+/g);
  if (bad) throw new Error(`SQL loi: ${bad.join(", ")} — ${out.trim().slice(0, 200)}`);
  return out;
}

/** Doc mot gia tri ngay (YYYY-MM-DD); tu choi ket qua neu output co Msg. */
function sqlDate(query) {
  const out = sql("SET NOCOUNT ON;\n" + query + "\n");
  if (/Msg \d+/.test(out)) throw new Error(`SQL loi khi doc ngay: ${out.trim().slice(0, 160)}`);
  const m = out.match(/\d{4}-\d{2}-\d{2}/);
  return m ? m[0] : null;
}

async function req(method, path, { token, body } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body) headers["Content-Type"] = "application/json";
  const res = await fetch(API + path, {
    method, headers, body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let data = null;
  try { data = JSON.parse(text); } catch { data = text.slice(0, 200); }
  return { status: res.status, data };
}

async function login(email, pass) {
  const r = await req("POST", "/api/auth/login", { body: { email, password: pass } });
  return r.status === 200 ? (r.data?.token || r.data?.data?.token) : null;
}

const STAMP = Date.now();
const EMAIL = `zzauditv10s${STAMP}@example.com`;
const PASS = "AuditV10#2026";
let USER_ID = null;
let TOKEN = null;
const CREATED_PROGRESS = [];

function cleanup() {
  console.log("");
  console.log("--- cleanup ---");
  try {
    if (USER_ID) {
      // Xoa theo ID CU THE. Khong dung LIKE 'zz%' — bai hoc F111 cua v9 da
      // xoa nham 4 user that vi kieu filter do.
      // PK cua `users` la `user_id`, KHONG phai `id` (User.java dong 27).
      // Da tung viet `WHERE id = ...` va nhan Msg 207; vi sqlNum cu bat so dau
      // tien trong output nen no tra ve 207 va cleanup trong nhu thanh cong.
      sqlExec(`
        DELETE FROM user_vocabulary_progress WHERE user_id = ${USER_ID};
        DELETE FROM study_days WHERE user_id = ${USER_ID};
        DELETE FROM users WHERE user_id = ${USER_ID};`);
      console.log("  da xoa user " + USER_ID + " + du lieu cua no");
      const left = sqlNum(`SELECT COUNT(*) FROM users WHERE user_id = ${USER_ID};`);
      console.log("  verify: users con lai voi id do = " + left + " (phai = 0)");
    }
  } catch (e) {
    console.log("  CLEANUP LOI: " + e.message);
  }
}

(async () => {
  console.log("=== audit-v10 Phase 1.13 — 5 kich ban streak ===");
  console.log("user kich ban: " + EMAIL);
  console.log("");

  const todayVN = new Date(Date.now() + 7 * 3600e3).toISOString().slice(0, 10);

  try {
    // ============================================================
    // SETUP — dang ki mot user rieng
    // ============================================================
    console.log("--- SETUP: dang ki user kich ban ---");
    const reg = await req("POST", "/api/auth/register", {
      // `username` la @NotBlank trong RegisterRequest — thieu no thi 400, va
      // loi se trong nhu "khong dang ki duoc" chu khong nhu "thieu field".
      body: { username: "zzauditv10s" + STAMP, email: EMAIL, password: PASS, fullName: "ZZ Audit V10 Streak" },
    });
    check("dang ki user moi -> 200/201", [200, 201].includes(reg.status), `nhan ${reg.status}: ${JSON.stringify(reg.data).slice(0, 150)}`);

    USER_ID = sqlNum(`SELECT user_id FROM users WHERE email = '${EMAIL}';`);
    check("user co trong DB sau dang ki", USER_ID !== null, "khong tim thay id");
    if (!USER_ID) throw new Error("khong tao duoc user kich ban");

    // ============================================================
    // KICH BAN 1 — DANG NHAP KHONG TAO NGAY HOC
    // Day chinh la hop dong cua ca cai refactor.
    // ============================================================
    console.log("");
    console.log("--- KB1: dang nhap KHONG duoc tao study_day ---");
    TOKEN = await login(EMAIL, PASS);
    check("KB1 dang nhap lay duoc token", !!TOKEN, "khong co token");
    if (!TOKEN) throw new Error("khong dang nhap duoc");

    let days = sqlNum(`SELECT COUNT(*) FROM study_days WHERE user_id = ${USER_ID};`);
    check("KB1 sau dang nhap: study_days = 0", days === 0, `nhan ${days}`);

    // Dang nhap lan hai — van khong duoc tao gi
    await login(EMAIL, PASS);
    await login(EMAIL, PASS);
    days = sqlNum(`SELECT COUNT(*) FROM study_days WHERE user_id = ${USER_ID};`);
    check("KB1 sau 3 lan dang nhap: study_days VAN = 0", days === 0, `nhan ${days}`);

    // ============================================================
    // KICH BAN 2 — HOAN THANH HOAT DONG HOC -> CO NGAY HOC
    // ============================================================
    console.log("");
    console.log("--- KB2: on SRS -> phai ghi study_day ---");
    // Lay mot vocabId that bat ky
    // PK cua `vocabulary` la `vocab_id` (Vocabulary.java dong 24), khong phai `id`.
    const vocabId = sqlNum("SELECT TOP 1 vocab_id FROM vocabulary ORDER BY vocab_id;");
    check("lay duoc vocabId that", vocabId !== null, "khong co vocabulary nao");
    if (!vocabId) throw new Error("khong co vocabulary");

    const rev = await req("POST", "/api/srs/review", { token: TOKEN, body: { vocabId, quality: 5 } });
    check("KB2 POST /api/srs/review -> 200", rev.status === 200, `nhan ${rev.status}: ${JSON.stringify(rev.data).slice(0, 200)}`);

    days = sqlNum(`SELECT COUNT(*) FROM study_days WHERE user_id = ${USER_ID} AND study_date = '${todayVN}';`);
    check(`KB2 study_days co ngay HOM NAY (${todayVN})`, days === 1, `nhan ${days}`);

    // On lai cung tu -> khong duoc nhan doi ngay
    const rev2 = await req("POST", "/api/srs/review", { token: TOKEN, body: { vocabId, quality: 4 } });
    check("KB2 on lai -> 200", rev2.status === 200, `nhan ${rev2.status}`);
    days = sqlNum(`SELECT COUNT(*) FROM study_days WHERE user_id = ${USER_ID};`);
    check("KB2 on lai KHONG nhan doi ngay (unique index giu)", days === 1, `nhan ${days} (phai = 1)`);

    // ============================================================
    // KICH BAN 3 — SNAPSHOT PHAN ANH DUNG SU THAT
    // ============================================================
    console.log("");
    console.log("--- KB3: snapshot phan anh dung ngay hoc vua ghi ---");
    const snap = await req("GET", "/api/streak/snapshot", { token: TOKEN });
    check("KB3 snapshot -> 200", snap.status === 200, `nhan ${snap.status}`);
    const d = snap.data || {};
    check("KB3 studiedToday = true (da hoc that)", d.studiedToday === true, `nhan ${d.studiedToday}`);
    check("KB3 currentStreak = 1", d.currentStreak === 1, `nhan ${d.currentStreak}`);
    check("KB3 studiedDays chua hom nay", Array.isArray(d.studiedDays) && d.studiedDays.includes(todayVN),
          `nhan ${JSON.stringify(d.studiedDays)}`);
    check("KB3 legacyAccessDays RONG (user moi, khong co login cu)",
          Array.isArray(d.legacyAccessDays) && d.legacyAccessDays.length === 0,
          `nhan ${JSON.stringify(d.legacyAccessDays)}`);
    // `legacyHistoryAvailable` KHONG co nghia "co du lieu legacy hay khong".
    // Doc StudyActivityService.snapshot(): no bat dau la `true`, va chi bi ha
    // xuong `false` khi (a) Redis nem, hoac (b) co member khong parse duoc
    // thanh LocalDate. Vay `true` voi user moi la DUNG — no bao "nguon legacy
    // doc duoc", khong phai "nguon legacy co noi dung".
    // Bang chung nguoc lai: StudyActivityServiceTest
    // `redisFailureDoesNotEraseSqlHistoryOrBecomeLegacyAbsence` (dong 88-95)
    // ep Redis nem va khang dinh co `isFalse()`.
    check("KB3 legacyHistoryAvailable = true (Redis doc duoc, du khong co du lieu)",
          d.legacyHistoryAvailable === true, `nhan ${d.legacyHistoryAvailable}`);

    // ============================================================
    // KICH BAN 4 — CUTOVER CHAN NGAY TRUOC NO (do duoc qua HTTP)
    // ============================================================
    //
    // GIOI HAN PHAI GHI RO, khong duoc lan tranh:
    //
    // Y dinh ban dau cua KB4 la "ngay gian doan pha streak" bang cach chen
    // study_days cho HOM QUA roi ky vong streak = 2. Dieu do BAT KHA THI khi
    // cutover = hom nay, va do la hanh vi DUNG:
    //
    //   currentStreak() -> days.findDates(userId, effectiveFrom(), today)
    //                                       ^^^^^^^^^^^^^^^^ san chan
    //
    // Da do that (sweep/v10/diag-streak-kb4.js): chen 2026-09-19 thanh cong
    // (bang co 2 row: 09-19 va 09-20), nhung snapshot van tra streak = 1 va
    // `so ngay trong [effectiveFrom, today]` = 1. Ngay truoc cutover bi loai
    // co chu dich — no la "lich su truy cap", khong phai "ngay hoc".
    //
    // Vi vay o day kiem DUNG thu do duoc qua HTTP: cutover chan duoc ngay
    // truoc no. Con "gap pha streak" duoc phu boi unit test
    // `fourDayJourneyDoesNotInheritLegacyStreak` (StudyActivityServiceTest
    // dong 64-75): stored=[start], o start+2 tra streak=0.
    console.log("");
    console.log("--- KB4: cutover chan ngay TRUOC no (gap-break do unit test phu) ---");
    // audit-v12 PROBE FIX: v10 computed "yesterday", which equalled the cutover day once
    // the calendar advanced past 2026-09-20 — so the inserted day was no longer BEFORE the
    // cutover and the scenario failed for the wrong reason. Derive the date from the actual
    // policy cutover instead, so the scenario keeps testing what it claims to test.
    const cutover = sqlDate(`SELECT CONVERT(varchar(10), effective_from, 120) AS d FROM study_policy;`);
    const dayBeforeCutover = new Date(new Date(cutover + 'T00:00:00Z').getTime() - 86400e3).toISOString().slice(0, 10);
    console.log("  (cutover =", cutover, "-> inserting", dayBeforeCutover + ")");
    sqlExec(`INSERT INTO study_days (user_id, study_date) VALUES (${USER_ID}, '${dayBeforeCutover}');`);

    const rowsInDb = sqlNum(`SELECT COUNT(*) FROM study_days WHERE user_id = ${USER_ID};`);
    check("KB4 DB THUC SU co 2 row (INSERT thanh cong, khong phai probe hong)",
          rowsInDb === 2, `nhan ${rowsInDb}`);

    const snap2 = await req("GET", "/api/streak/snapshot", { token: TOKEN });
    check("KB4 nhung streak VAN = 1 (ngay truoc cutover bi loai dung thiet ke)",
          snap2.data?.currentStreak === 1, `nhan ${snap2.data?.currentStreak}`);
    check("KB4 studiedDays KHONG chua ngay truoc cutover",
          Array.isArray(snap2.data?.studiedDays) && !snap2.data.studiedDays.includes(dayBeforeCutover),
          `nhan ${JSON.stringify(snap2.data?.studiedDays)}`);
    check("KB4 ngay truoc cutover KHONG bi coi la legacyAccessDays",
          Array.isArray(snap2.data?.legacyAccessDays) && !snap2.data.legacyAccessDays.includes(dayBeforeCutover),
          `nhan ${JSON.stringify(snap2.data?.legacyAccessDays)}`);

    sqlExec(`DELETE FROM study_days WHERE user_id = ${USER_ID} AND study_date = '${dayBeforeCutover}';`);
    console.log("  (da xoa row thu nghiem)");

    // ============================================================
    // KICH BAN 5 — KHONG LAM GI HOM NAY -> KHONG CO NGAY HOC HOM NAY
    // (user thu hai, chi dang nhap, khong hoc)
    // ============================================================
    console.log("");
    console.log("--- KB5: user chi dang nhap, khong hoc -> studiedToday=false ---");
    const EMAIL2 = `zzauditv10t${STAMP}@example.com`;
    await req("POST", "/api/auth/register", {
      body: { username: "zzauditv10t" + STAMP, email: EMAIL2, password: PASS, fullName: "ZZ Audit V10 Login Only" },
    });
    const uid2 = sqlNum(`SELECT user_id FROM users WHERE email = '${EMAIL2}';`);
    check("tao duoc user thu 2", uid2 !== null, "khong tim thay");
    if (uid2) {
      const tok2 = await login(EMAIL2, PASS);
      const s2 = await req("GET", "/api/streak/snapshot", { token: tok2 });
      check("KB5 snapshot -> 200", s2.status === 200, `nhan ${s2.status}`);
      check("KB5 studiedToday = FALSE (chi dang nhap)", s2.data?.studiedToday === false,
            `nhan ${s2.data?.studiedToday}`);
      check("KB5 currentStreak = 0", s2.data?.currentStreak === 0, `nhan ${s2.data?.currentStreak}`);
      const c2 = sqlNum(`SELECT COUNT(*) FROM study_days WHERE user_id = ${uid2};`);
      check("KB5 study_days cua user 2 = 0", c2 === 0, `nhan ${c2}`);
      sqlExec(`DELETE FROM users WHERE user_id = ${uid2};`);
      console.log("  da don user 2 (id=" + uid2 + ")");
    }

    // ============================================================
    console.log("");
    console.log("============================================");
    console.log(`  PASS=${R.pass}  FAIL=${R.fail}`);
    console.log("============================================");
  } catch (e) {
    console.log("");
    console.log("!!! KICH BAN DUT: " + e.message);
    R.fail++;
  } finally {
    cleanup();
  }

  if (R.fail) {
    console.log("");
    console.log("FAILURES:");
    for (const s of R.steps.filter((x) => !x.cond)) console.log("  - " + s.name + " -> " + s.detail);
    process.exit(1);
  }
})();
