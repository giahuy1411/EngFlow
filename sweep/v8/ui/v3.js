/**
 * audit-v8 vòng 3 — verify UI thật cho 4 luồng mà vòng 1/2 chỉ chạm ở mức render:
 *   1. Đăng ký bằng FORM THẬT (/register) → token + /me
 *   2. Lịch streak trong /profile khớp /api/streak/current (server clock)
 *   3. Ô tìm kiếm /lessons gọi API đúng tham số q + danh sách đổi
 *   4. Ghi âm THẬT (Chromium fake mic) → upload submission → chấm điểm
 * Cleanup ở cuối: user tạm, submission, request log.
 * Chạy: cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node ui\v3.js"
 */
const H = require("./lib.js");
const { execFileSync } = require("child_process");
const fs = require("fs");

function sql(q) {
  return execFileSync("docker", ["exec", "engflow-sqlserver", "/opt/mssql-tools18/bin/sqlcmd",
    "-S", "localhost", "-U", "sa", "-P", "YourPassword123", "-d", "english_learning",
    "-C", "-I", "-h", "-1", "-W", "-Q", "SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; " + q], { encoding: "utf8" }).trim();
}
function sh(cmd) { return execFileSync("docker", ["exec", "engflow-minio", "sh", "-c", cmd], { encoding: "utf8" }).trim(); }
const STAMP = Date.now().toString().slice(-8);
const EMAIL = "zzv3ui" + STAMP + "@example.com";
const USERNAME = "zzv3ui" + STAMP;
const PASS = "Test123456";
let pass = 0, fail = 0, perSubId = null, perSubKey = null;
const out = [];
function note(name, got, want, extra) {
  const ok = String(got) === String(want);
  ok ? pass++ : fail++;
  out.push({ name, got: String(got), want: String(want), pass: ok, extra: extra === undefined ? "" : String(extra).slice(0, 200) });
  console.log((ok ? "  OK   " : "  FAIL ") + name + " | got=" + got + " want=" + want + (extra ? " | " + String(extra).slice(0, 140) : ""));
}

(async () => {
  const subsAtStart = Number(sql("SELECT COUNT(*) FROM speaking_submissions"));
  console.log("speaking_submissions baseline =", subsAtStart);
  const browser = await H.pw.chromium.launch({ headless: true,
    args: ["--use-fake-ui-for-media-stream", "--use-fake-device-for-media-stream", "--autoplay-policy=no-user-gesture-required"] });
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  await ctx.grantPermissions(["microphone"], { origin: H.APP });
  const page = await ctx.newPage();
  const consoleErrors = [];
  page.on("console", m => { if (m.type() === "error") consoleErrors.push(m.text().slice(0, 160)); });

  // ---------- 1. ĐĂNG KÝ BẰNG FORM THẬT ----------
  console.log("=== 1. ĐĂNG KÝ BẰNG FORM THẬT (/register) ===");
  await page.goto(H.APP + "/register", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1500);
  const inputs = await page.$$("form input");
  note("form đăng ký có 4 input (username/email/password/confirm)", inputs.length, 4);
  await inputs[0].fill(USERNAME);
  await inputs[1].fill(EMAIL);
  await inputs[2].fill(PASS);
  await inputs[3].fill(PASS);
  const regResp = page.waitForResponse(r => r.url().includes("/api/auth/register"), { timeout: 15000 }).catch(() => null);
  await page.click('form button[type="submit"]');
  const rr = await regResp;
  note("POST /api/auth/register từ UI -> 201", rr ? rr.status() : "no-response", 201);
  await page.waitForTimeout(2500);
  const st = await page.evaluate(() => ({ path: location.pathname, token: !!localStorage.getItem("token"), user: !!localStorage.getItem("user") }));
  note("đăng ký xong: có token trong localStorage", st.token, "true", JSON.stringify(st));
  note("rời khỏi /register sau đăng ký", st.path !== "/register", "true", "path=" + st.path);
  const me = await page.evaluate(async () => {
    const r = await fetch("http://localhost:8080/api/auth/me", { headers: { Authorization: "Bearer " + localStorage.getItem("token") } });
    return { code: r.status, body: await r.text() };
  });
  note("token vừa đăng ký gọi /api/auth/me -> 200", me.code, 200, me.body.slice(0, 90));
  note("email trong /me đúng tài khoản vừa đăng ký", me.body.includes(EMAIL), "true");
  await page.evaluate(() => { localStorage.removeItem("token"); localStorage.removeItem("user"); });

  // ---------- 2. LỊCH STREAK TRONG /profile ----------
  console.log("=== 2. LỊCH STREAK TRONG PROFILE ===");
  const userTok = await H.login("user@gmail.com", "123456");
  const api = await fetch("http://localhost:8080/api/streak/current", { headers: { Authorization: "Bearer " + userTok } });
  const apiStreak = await api.json();
  await H.seedToken(page, userTok);
  await page.goto(H.APP + "/profile", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(3000);
  const cal = await page.evaluate(() => {
    const root = document.querySelector(".streak-calendar");
    if (!root) return { missing: true };
    const cells = [...root.querySelectorAll('[role="gridcell"]')];
    const todayCells = cells.filter(c => /ring-accent/.test(c.className));
    const studied = cells.filter(c => /bg-secondary/.test(c.className));
    return {
      text: root.innerText.replace(/\s+/g, " ").slice(0, 120),
      cells: cells.length,
      todayCells: todayCells.length,
      todayLabel: todayCells[0] ? todayCells[0].getAttribute("aria-label") : null,
      studied: studied.length,
      gridRole: !!root.querySelector('[role="grid"]'),
      colHeaders: root.querySelectorAll('[role="columnheader"]').length
    };
  });
  note("profile render lịch streak (.streak-calendar)", !cal.missing, "true", JSON.stringify(cal).slice(0, 160));
  note("lịch có role=grid + 28+ cell", cal.gridRole === true && cal.cells >= 28, "true", "cells=" + cal.cells);
  note("đúng 1 ô được đánh dấu HÔM NAY (ring-accent)", cal.todayCells, 1, "label=" + cal.todayLabel);
  const todayLabel = cal.todayLabel || "";
  const dayNum = String(Number(apiStreak.today.slice(8, 10)));
  note("ô 'hôm nay' khớp ngày server " + apiStreak.today, todayLabel.includes(dayNum), "true", "label=" + todayLabel);
  note("số streak trên UI = /api/streak/current (" + apiStreak.currentStreak + ")",
    cal.text.includes(String(apiStreak.currentStreak)), "true", cal.text);
  note("ô đã học có marker 🔥", cal.studied >= 1, "true", "studied=" + cal.studied);

  // ---------- 3. Ô TÌM KIẾM /lessons ----------
  console.log("=== 3. TÌM KIẾM / SẮP XẾP QUA UI ===");
  const searchCalls = [];
  page.on("request", r => { const u = r.url(); if (u.includes("/api/lessons?") || u.includes("/api/lessons/?")) searchCalls.push(u); });
  await page.goto(H.APP + "/lessons", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(3000);
  const before = await page.evaluate(() => {
    const t = document.body.innerText;
    const m = t.match(/(\d+)\s*bài học/i);
    return { count: document.querySelectorAll("article h3").length, text: t.slice(0, 200), total: m ? m[1] : null };
  });
  const box = await page.$('input[type="search"]');
  note("trang /lessons có ô tìm kiếm", !!box, "true");
  await box.fill("Grammar");
  await page.waitForTimeout(3000);
  const after = await page.evaluate(() => ({
    count: document.querySelectorAll("article h3").length,
    titles: [...document.querySelectorAll("article h3")].slice(0, 6).map(a => a.innerText.replace(/\s+/g, " ").trim().slice(0, 60)),
    text: document.body.innerText.replace(/\s+/g, " ").slice(0, 200)
  }));
  note("gõ search -> có request /api/lessons?q=Grammar", searchCalls.some(u => /[?&]q=Grammar/.test(u)), "true",
    searchCalls.slice(-3).map(u => u.replace("http://localhost:8080", "")).join(" | ").slice(0, 160));
  note("mọi tiêu đề hiển thị sau search đều chứa từ khóa",
    after.titles.length > 0 && after.titles.every(t => /grammar/i.test(t)), "true",
    "rows=" + after.count + " titles=" + JSON.stringify(after.titles.slice(0, 2)));
  note("kết quả sau search không rỗng", after.count > 0, "true", "titles=" + JSON.stringify(after.titles.slice(0, 3)));
  await box.fill("");
  await page.waitForTimeout(2500);
  const cleared = await page.evaluate(() => document.querySelectorAll("article h3").length);
  note("xóa từ khóa -> danh sách quay lại như trước", cleared === before.count, "true", cleared + " vs " + before.count);

  // ---------- 4. GHI ÂM THẬT + NỘP BÀI ----------
  console.log("=== 4. GHI ÂM THẬT (fake mic) → UPLOAD SUBMISSION ===");
  await page.goto(H.APP + "/speaking/60014/record", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(3000);
  const micBtn = await page.$("#enable-microphone-button");
  note("trang record render (nút bật micro)", !!micBtn, "true");
  if (micBtn) {
    await micBtn.click();
    await page.waitForTimeout(2000);
    const startBtn = await page.$("#start-recording-button");
    note("sau khi cấp quyền -> hiện nút 'Bắt đầu ghi'", !!startBtn, "true");
    if (startBtn) {
      await startBtn.click();
      await page.waitForTimeout(3000);
      const stopBtn = await page.$("#stop-recording-button");
      const recState = await page.evaluate(() => document.body.innerText.match(/\d+\s*s/i) ? document.body.innerText.replace(/\s+/g, " ").slice(0, 160) : "");
      note("đang ghi: hiện nút 'Dừng ghi' + đồng hồ đếm", !!stopBtn, "true", recState);
      if (stopBtn) {
        await stopBtn.click();
        await page.waitForTimeout(1500);
        const submitBtn = await page.$("#submit-recording-button");
        note("dừng ghi -> hiện nút 'Gửi bài'", !!submitBtn, "true");
        if (submitBtn) {
          const upResp = page.waitForResponse(r => r.url().includes("/api/v1/speaking-submissions/upload"), { timeout: 60000 }).catch(() => null);
          const asmtResp = page.waitForResponse(r => /\/api\/v1\/speaking-submissions\/\d+\/assess/.test(r.url()), { timeout: 90000 }).catch(() => null);
          await submitBtn.click();
          const up = await upResp;
          let subId = null, body = "";
          if (up) { body = await up.text(); try { subId = JSON.parse(body).id; } catch (e) {} }
          note("POST /upload từ UI (blob webm thật) -> 200/201", up ? (up.status() === 200 || up.status() === 201) : "no-response", "true",
            up ? up.status() + " id=" + subId : "");
          const asmt = await asmtResp;
          note("assessment tự chạy sau upload (Whisper + rubric) -> 200", asmt ? String(asmt.status()) : "no-response", "200",
            asmt ? "status=" + asmt.status() : "");
          perSubId = subId;
          if (subId) {
            await page.waitForTimeout(4000);
            const shown = await page.evaluate(() => document.body.innerText.replace(/\s+/g, " ").slice(0, 600));
            note("UI hiển thị trạng thái/kết quả sau khi nộp", /AI|Điểm|chấm|Lỗi|thất bại|Đang/i.test(shown), "true",
              (shown.match(/(AI đã chấm|Đang chấm|Không thể[^|]{0,40}|[0-9]+(\.[0-9]+)?\s*\/\s*10)/) || [""])[0]);
            note("row submission có trong DB", Number(sql("SELECT COUNT(*) FROM speaking_submissions WHERE id = " + subId)), 1);
            note("trạng thái ghi nhận (mic giả = im lặng nên FAILED là đúng)",
              sql("SELECT status FROM speaking_submissions WHERE id = " + subId), "FAILED");
            const key = sql("SELECT media_object_key FROM speaking_submissions WHERE id = " + subId);
            perSubKey = key;
            const del = await fetch("http://localhost:8080/api/v1/admin/speaking-submissions/" + subId, {
              method: "DELETE", headers: { Authorization: "Bearer " + (await H.login("admin@gmail.com", "123456")) } });
            note("admin DELETE speaking-submission KHÔNG tồn tại (404) → dọn bằng SQL + MinIO", del.status, 404,
              "id=" + subId);
          }
        }
      }
    }
  }

  note("0 console error trong toàn bộ phiên UI", consoleErrors.length, 0, consoleErrors.slice(0, 3).join(" | "));
  await browser.close();

  console.log("=== CLEANUP ===");
  if (perSubId) {
    sql("DELETE FROM speaking_submissions WHERE id = " + perSubId);
    if (perSubKey) {
      sh("mc alias set local http://localhost:9000 \"$MINIO_ROOT_USER\" \"$MINIO_ROOT_PASSWORD\" >/dev/null 2>&1; " +
         "mc rm --force local/speaking-uploads/" + perSubKey);
    }
  }
  note("speaking_submissions về baseline đầu phiên", Number(sql("SELECT COUNT(*) FROM speaking_submissions")), subsAtStart);
  sql("DELETE FROM users WHERE email LIKE 'zzv3ui%'");
  note("user tạm (đăng ký qua UI) đã xóa", sql("SELECT COUNT(*) FROM users WHERE email LIKE 'zzv3ui%'"), "0");

  fs.writeFileSync(__dirname + "/v3ui.json", JSON.stringify(out, null, 1));
  console.log("ASSERT PASS=" + pass + " FAIL=" + fail + " | temp email=" + EMAIL);
})().catch(e => { console.error("FATAL", e); process.exit(1); });
