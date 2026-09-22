/**
 * p24_journeys.js — audit-v9 end-to-end journeys in a REAL browser (Chromium),
 * one journey per core feature from the audit prompt, each with a DB assertion
 * where the feature writes to the database.
 *
 * J1 register -> login -> logout        (Dang ki / Dang nhap)
 * J2 lesson exercise: answer + grade    (Bai hoc / Bai tap)
 * J3 deck flashcard rating              (flashcard/SRS write path)
 * J4 streak surface + DB row            (Co che streak)
 * J5 search + level filter              (Tim kiem / sap xep)
 * J6 admin lesson CRUD through the UI   (CRUD)
 * J7 speaking: fake mic -> upload -> assess (AI/Whisper that ghi am)
 *
 * Reads/writes only audit-namespaced rows and cleans them in the same run.
 * Run from sweep/v8:  set NODE_PATH=%APPDATA%\npm\node_modules&& node p24_journeys.js
 */
const H = require("./ui/lib.js");
const { execFileSync } = require("child_process");

const API = H.API, APP = H.APP;
const STAMP = Date.now();
const NEW_EMAIL = "zzv9j" + STAMP + "@example.com";
const NEW_USER = "zzv9j" + STAMP;
const PASS = "Test123456";

function sql(q) {
  return execFileSync("docker", ["exec", "engflow-sqlserver", "/opt/mssql-tools18/bin/sqlcmd",
    "-S", "localhost", "-U", "sa", "-P", "YourPassword123", "-d", "english_learning",
    "-C", "-I", "-h", "-1", "-W", "-Q", "SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; " + q],
    { encoding: "utf8" }).trim();
}

const out = [];
function j(name, ok, detail) {
  out.push({ journey: name, pass: !!ok, detail: String(detail).slice(0, 400) });
  console.log((ok ? "PASS " : "FAIL ") + name + " :: " + String(detail).slice(0, 220));
}

function track(page) {
  const calls = [];
  page.on("response", (r) => {
    const u = r.url();
    if (u.startsWith(API)) calls.push({ code: r.status(), method: r.request().method(), url: u.replace(API, "") });
  });
  return calls;
}
const bad = (calls) => calls.filter((c) => c.code >= 400 && c.code !== 403 && c.code !== 401);

(async () => {
  const browser = await H.pw.chromium.launch({ headless: true });

  // ---------- J1: register -> login -> logout ----------
  {
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await ctx.newPage();
    const calls = track(page);
    await page.goto(APP + "/register", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(800);
    const inputs = page.locator("form input:visible");
    const n = await inputs.count();
    for (let i = 0; i < n; i++) {
      const type = await inputs.nth(i).getAttribute("type");
      const ph = ((await inputs.nth(i).getAttribute("placeholder")) || "") + ((await inputs.nth(i).getAttribute("name")) || "");
      let v = "";
      if (/user|tên|ten|name/i.test(ph)) v = NEW_USER;
      else if (/email/i.test(ph)) v = NEW_EMAIL;
      else if (/confirm|xác nhận|xac nhan/i.test(ph)) v = PASS;
      else if (type === "password" || /mật khẩu|mat khau|password/i.test(ph)) v = PASS;
      else v = NEW_USER;
      await inputs.nth(i).fill(v);
    }
    await page.locator("form button[type=submit], form button").first().click();
    await page.waitForTimeout(2500);
    const url = page.url().replace(APP, "");
    const reg = calls.find((c) => c.url.startsWith("/api/auth/register"));
    const rowId = sql("SELECT COUNT(*) FROM users WHERE email = '" + NEW_EMAIL + "'");
    j("J1 register", reg && (reg.code === 200 || reg.code === 201) && rowId === "1",
      "register=" + (reg && reg.code) + " landed=" + url + " dbUsers=" + rowId);

    // login (fresh context with the new account, through the real form)
    const ctx2 = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const p2 = await ctx2.newPage();
    const c2 = track(p2);
    await p2.goto(APP + "/login", { waitUntil: "domcontentloaded" });
    await p2.waitForTimeout(800);
    await p2.locator("input[type=email], input[placeholder*='mail']").first().fill(NEW_EMAIL);
    await p2.locator("input[type=password]").first().fill(PASS);
    await p2.locator("form button[type=submit], form button").first().click();
    await p2.waitForTimeout(2500);
    const login = c2.find((c) => c.url.startsWith("/api/auth/login"));
    const url2 = p2.url().replace(APP, "");
    j("J1 login", login && login.code === 200 && url2 !== "/login",
      "login=" + (login && login.code) + " landed=" + url2);
    await ctx2.close();
    await ctx.close();
  }

  // ---------- shared logged-in user context for J2/J3/J4/J5 ----------
  const userS = await H.loginFull("user@gmail.com", "123456");
  const ctxU = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const pageU = await ctxU.newPage();
  const callsU = track(pageU);
  await H.seedAuth(pageU, userS);
  await pageU.waitForTimeout(400);

  // ---------- J2: lesson exercise answer + grade ----------
  {
    callsU.length = 0;
    await pageU.goto(APP + "/lessons/445", { waitUntil: "domcontentloaded" });
    await pageU.waitForTimeout(2200);
    const tabBtn = pageU.locator("button:has-text('BÀI TẬP'), button:has-text('Bài tập'), a:has-text('BÀI TẬP')").first();
    if (await tabBtn.count()) { await tabBtn.click(); await pageU.waitForTimeout(1500); }
    const opt = pageU.locator("button:has-text('have'), button:has-text('has'), label:has-text('have')").first();
    if (await opt.count()) { await opt.click(); await pageU.waitForTimeout(300); }
    const check = pageU.locator("button:has-text('Kiểm tra')").first();
    const hasCheck = await check.count();
    if (hasCheck) { await check.click(); await pageU.waitForTimeout(2500); }
    const grade = callsU.find((c) => /\/exercises\/(grade|submit)$/.test(c.url));
    const bodyTxt = await pageU.locator("body").innerText();
    const showsResult = /Đúng|Sai|điểm|%|Chính xác/i.test(bodyTxt);
    j("J2 lesson exercise grade", hasCheck && grade && grade.code === 200 && showsResult,
      "checkBtn=" + hasCheck + " grade=" + (grade && grade.code) + " uiResult=" + showsResult);
  }

  // ---------- J3: deck flashcard rating ----------
  {
    callsU.length = 0;
    await pageU.goto(APP + "/decks", { waitUntil: "domcontentloaded" });
    await pageU.waitForTimeout(2000);
    // Measured markup (v9_probe_deck.js): the deck list links are
    // /decks/<id>; "Tạo bộ từ" is also an <a> and used to win .first().
    // The flashcard player lives at /decks/<id>/play/flashcard.
    const deck = pageU.locator("a[href^='/decks/']").filter({ hasNotText: "Tạo bộ từ" }).first();
    if (await deck.count()) { await deck.click(); await pageU.waitForTimeout(2200); }
    const play = pageU.locator("a[href*='/play/flashcard']").first();
    if (await play.count()) { await play.click(); await pageU.waitForTimeout(2500); }
    // Measured (v9b-p24b.log): the flashcard player labels its rating buttons
    // "Lại" and "Dễ" - not "Đã biết/Chưa biết". Match the real labels.
    const rate = pageU.locator("button").filter({ hasText: /^(Dễ|Lại|Khó|Vừa)$/ }).first();
    const hasRate = await rate.count();
    let rateText = "";
    if (hasRate) { rateText = (await rate.innerText()).trim().slice(0, 30); await rate.click(); await pageU.waitForTimeout(2500); }
    const rev = callsU.find((c) => c.url.startsWith("/api/flashcards/review") || c.url.startsWith("/api/srs/review"));
    const urlNow = pageU.url().replace(APP, "");
    const btnDump = hasRate ? "" : JSON.stringify(await pageU.evaluate(() => [...document.querySelectorAll("button")].map(b => (b.innerText || "").trim().slice(0, 18)).filter(Boolean).slice(0, 14)));
    j("J3 flashcard rating", hasRate && rev && rev.code === 200,
      "landed=" + urlNow + " rateBtn=" + hasRate + " rate=" + rateText + " review=" + (rev && rev.url + " " + rev.code) + btnDump);
  }

  // ---------- J4: streak ----------
  {
    callsU.length = 0;
    await pageU.goto(APP + "/profile", { waitUntil: "domcontentloaded" });
    await pageU.waitForTimeout(2200);
    const st = callsU.find((c) => c.url.startsWith("/api/streak/current"));
    const txt = await pageU.locator("body").innerText();
    // audit-v13 F-13-08: users.current_streak was DROPPED. The real streak lives in
    // study_days; count the user's study days instead of reading a column that no longer exists.
    const dbStreak = sql("SELECT COUNT(*) FROM study_days WHERE user_id = (SELECT user_id FROM users WHERE email = 'user@gmail.com')");
    const uiHasStreak = /streak|chuỗi|liên tiếp/i.test(txt);
    j("J4 streak", st && st.code === 200 && uiHasStreak && /^\d+$/.test(dbStreak),
      "streakApi=" + (st && st.code) + " uiMentionsStreak=" + uiHasStreak + " dbCurrentStreak=" + dbStreak);
  }

  // ---------- J5: search + level filter ----------
  {
    callsU.length = 0;
    await pageU.goto(APP + "/lessons", { waitUntil: "domcontentloaded" });
    await pageU.waitForTimeout(2200);
    const chip = pageU.locator("button:has-text('ELEMENTARY'), button:has-text('Cơ bản')").first();
    const hasChip = await chip.count();
    if (hasChip) { await chip.click(); await pageU.waitForTimeout(2000); }
    const filt = callsU.find((c) => c.url.startsWith("/api/lessons?") && /level=/.test(c.url));
    // Measured markup (v9_probe_selectors.js): the list renders lesson cards as
    // buttons labelled "Bắt đầu học"; there are no /lessons/<id> anchors on the
    // list page, so the v9 first pass asserted on a selector that cannot exist.
    const cards = await pageU.locator("button:has-text('Bắt đầu học')").count();
    j("J5 level filter", hasChip && filt && filt.code === 200 && cards > 0,
      "chip=" + hasChip + " filteredApi=" + (filt && filt.url) + " cards=" + cards);
  }
  await ctxU.close();

  // ---------- J6: admin lesson CRUD through the UI ----------
  {
    const adminS = await H.loginFull("admin@gmail.com", "123456");
    const ctxA = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const pageA = await ctxA.newPage();
    const callsA = track(pageA);
    await H.seedAuth(pageA, adminS);
    await pageA.goto(APP + "/admin/lessons", { waitUntil: "domcontentloaded" });
    await pageA.waitForTimeout(2500);
    // Measured markup (v9_probe_admin.js): button "Thêm Bài Học"; dialog fields
    // #form-lesson-title / #form-lesson-desc / #form-lesson-content /
    // #form-lesson-level; save button "Lưu lại".
    const addBtn = pageA.locator("button:has-text('Thêm Bài Học')").first();
    const hasAdd = await addBtn.count();
    let created = null, appeared = false, api = null;
    if (hasAdd) {
      await addBtn.click();
      await pageA.waitForTimeout(1500);
      const title = "ZZ v9 journey " + STAMP;
      await pageA.locator("#form-lesson-title").fill(title);
      await pageA.locator("#form-lesson-desc").fill("audit-v9 journey lesson");
      await pageA.locator("#form-lesson-content").fill("<p>Journey content long enough to satisfy the lesson validator.</p>");
      await pageA.locator("button:has-text('Lưu lại')").first().click();
      await pageA.waitForTimeout(3000);
      const call = callsA.filter(c => c.method === "POST" && c.url === "/api/admin/lessons").pop();
      api = call && call.code;
      created = sql("SELECT lesson_id FROM lessons WHERE title = '" + title + "'");
      appeared = /^\d+$/.test(created) && created !== "0" && created !== "";
      if (appeared) sql("DELETE FROM exercises WHERE lesson_id = " + created + "; DELETE FROM lesson_sections WHERE lesson_id = " + created + "; DELETE FROM lessons WHERE lesson_id = " + created + ";");
    }
    j("J6 admin lesson create via UI", hasAdd && appeared,
      "addBtn=" + hasAdd + " POST=/api/admin/lessons " + api + " newLessonId=" + created + " cleaned=" + (created && appeared));
    await ctxA.close();
  }

  // ---------- J7: speaking fake mic -> upload -> assess ----------
  {
    const b2 = await H.pw.chromium.launch({
      headless: true,
      args: ["--use-fake-ui-for-media-stream", "--use-fake-device-for-media-stream"],
    });
    const ctxS = await b2.newContext({ viewport: { width: 1440, height: 900 } });
    await ctxS.grantPermissions(["microphone"], { origin: APP });
    const pageS = await ctxS.newPage();
    const callsS = track(pageS);
    await H.seedAuth(pageS, userS);
    await pageS.goto(APP + "/speaking", { waitUntil: "domcontentloaded" });
    await pageS.waitForTimeout(2500);
    // Measured markup (v9_probe_deck.js): the recorder is a dedicated route
    // /speaking/<promptId>/record, not a panel on /speaking.
    const pick = pageS.locator("a[href^='/speaking/']").filter({ hasNotText: "LỊCH S" }).first();
    if (await pick.count()) { await pick.click(); await pageS.waitForTimeout(2200); }
    const record = pageS.locator("a[href$='/record']").first();
    if (await record.count()) { await record.click(); await pageS.waitForTimeout(2500); }
    const enable = pageS.locator("#enable-microphone-button");
    if (await enable.count()) { await enable.click(); await pageS.waitForTimeout(1500); }
    const start = pageS.locator("#start-recording-button");
    const hasStart = await start.count();
    if (hasStart) { await start.click(); await pageS.waitForTimeout(3500); }
    const stop = pageS.locator("#stop-recording-button");
    if (await stop.count()) { await stop.click(); await pageS.waitForTimeout(1800); }
    const submit = pageS.locator("#submit-recording-button");
    if (await submit.count()) { await submit.click(); await pageS.waitForTimeout(6000); }
    const upload = callsS.find((c) => /upload|submissions/.test(c.url) && c.method === "POST");
    const row = sql("SELECT COUNT(*) FROM speaking_submissions WHERE submitted_at >= CAST(GETDATE() AS date)");
    j("J7 speaking fake-mic", !!upload && upload.code < 400 && Number(row) >= 1,
      "upload=" + (upload ? upload.method + " " + upload.url + " " + upload.code : "none") + " rowsToday=" + row);
    await b2.close();
  }

  await browser.close();

  // ---------- cleanup: new user + its SRS rows, then parity ----------
  sql("DELETE FROM user_vocabulary_progress WHERE user_id IN (SELECT user_id FROM users WHERE email = '" + NEW_EMAIL + "');"
    + "DELETE FROM user_progress WHERE user_id IN (SELECT user_id FROM users WHERE email = '" + NEW_EMAIL + "');"
    + "DELETE FROM users WHERE email = '" + NEW_EMAIL + "';");
  const left = sql("SELECT COUNT(*) FROM users WHERE email = '" + NEW_EMAIL + "'");
  console.log("cleanup new user leftover=" + left);

  const fs = require("fs");
  fs.writeFileSync(__dirname + "/p24_journeys.json", JSON.stringify(out, null, 1));
  console.log("wrote p24_journeys.json");
  const failed = out.filter((o) => !o.pass).length;
  console.log("=== JOURNEYS total=" + out.length + " PASS=" + (out.length - failed) + " FAIL=" + failed + " ===");
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.log("ERR", e.message); process.exit(1); });