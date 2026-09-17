/**
 * audit-v8 F88 — verify ĐƯỜNG UI THẬT: admin sửa bài học video qua nút "Sửa" mới thêm
 * (trước đây openForm(lesson) là code chết vì bảng chỉ có Xem/Xóa) và lưu với ô phụ đề
 * bỏ trống (ngữ nghĩa "giữ nguyên phụ đề cũ", audit-v6 F21).
 * Trước fix: PUT mang transcript:null → 400 Validation Failed → toast "Lưu thất bại".
 */
const H = require("./lib.js");
const API = "http://localhost:8080";
const APP = "http://localhost:5173";
let pass = 0, fail = 0;
function note(name, got, want, extra) {
  const ok = String(got) === String(want);
  ok ? pass++ : fail++;
  console.log((ok ? "  OK   " : "  FAIL ") + name + " | got=" + got + " want=" + want + (extra ? " | " + String(extra).slice(0, 160) : ""));
}
async function api(method, path, token, body) {
  const headers = token ? { Authorization: "Bearer " + token } : {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const r = await fetch(API + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  let j = null; const t = await r.text(); try { j = JSON.parse(t); } catch (e) { }
  return { code: r.status, json: j, txt: t };
}
(async () => {
  const admin = await H.login("admin@gmail.com", "123456");
  const stamp = Date.now();
  // seed 1 bài học video có 2 dòng phụ đề
  const tr = [{ start: 0, end: 2, textEn: "Keep me one." }, { start: 2, end: 4, textEn: "Keep me two." }];
  let r = await api("POST", "/api/v1/admin/video-lessons", admin, {
    title: "ZZ ui edit " + stamp, description: "seed", youtubeUrl: "https://www.youtube.com/watch?v=2VeQTuSSiI0",
    level: "ELEMENTARY", isPublished: true, transcript: tr
  });
  const id = r.json && r.json.id;
  note("seed video lesson qua API (201)", r.code, 201);

  const browser = await H.pw.chromium.launch({ headless: true });
  const ctx = await H.mkContext(browser, admin, { width: 1440, height: 950 });
  const page = await ctx.newPage();
  const consoleErrors = [];
  const putCalls = [];
  page.on("console", m => { if (m.type() === "error") consoleErrors.push(m.text().slice(0, 160)); });
  page.on("response", async res => {
    const u = res.url();
    if (u.includes("/api/v1/admin/video-lessons/") && res.request().method() === "PUT") {
      putCalls.push({ url: u.replace(API, ""), status: res.status(), body: res.request().postData() });
    }
  });
  // dang nhap bang FORM THAT (router guard doc auth store, chi set localStorage.token la khong du)
  await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1500);
  await page.fill('input[type="email"], input[name="email"]', "admin@gmail.com");
  await page.fill('input[type="password"]', "123456");
  await page.click('button[type="submit"]');
  await page.waitForTimeout(4000);
  await page.goto(APP + "/admin/videos", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(3000);

  const rowLink = page.locator("tr", { hasText: "ZZ ui edit " + stamp }).first();
  note("dòng bài học mới có mặt trong bảng admin", await rowLink.count() >= 1 ? 1 : 0, 1);
  const editBtn = rowLink.getByRole("button", { name: "Sửa" });
  note("nút 'Sửa' render trong bảng", await editBtn.count(), 1);
  await editBtn.click();
  await page.waitForTimeout(700);

  const modal = page.locator("#video-lesson-form");
  note("modal sửa mở với tiêu đề cũ", (await page.locator("input").first().inputValue()).includes("ZZ ui edit"), true);
  await modal.locator("input").first().fill("ZZ ui edit " + stamp + " v2");
  // ô phụ đề để TRỐNG = giữ nguyên phụ đề cũ (đúng luồng audit-v6 F21)
  const transcriptBox = page.locator("#vl-transcript");
  note("ô phụ đề trống khi vào form sửa", (await transcriptBox.inputValue()) === "", true);
  await page.getByRole("button", { name: /Lưu bài học/ }).click();
  // toast auto-dismiss nhanh -> poll thay vi cho 1 nhip co dinh
  let toastSeen = false;
  for (let i = 0; i < 20; i++) {
    await page.waitForTimeout(200);
    // innerText tra ve chu HOA (CSS text-transform) -> so sanh khong phan biet hoa/thuong
    if (/đã lưu bài học video/i.test(await page.locator("body").innerText())) { toastSeen = true; break; }
  }

  const put = putCalls[putCalls.length - 1];
  note("PUT từ UI trả 200", put ? put.status : "không có PUT", 200, put ? put.body : "");
  note("payload UI có transcript:null (đúng như audit mô tả)", /"transcript":null/.test(put ? put.body : "") ? 1 : 0, 1);
  note("toast thành công hiển thị", toastSeen, true);

  const detail = await api("GET", "/api/v1/video-lessons/" + id, admin);
  note("tiêu đề đã đổi trong DB", detail.json.title.endsWith("v2"), true);
  note("phụ đề giữ nguyên 2 dòng", (detail.json.transcript || []).length, 2);
  note("nội dung dòng 1 không đổi", (detail.json.transcript || [])[0].textEn, "Keep me one.");
  note("console error trong lượt này", consoleErrors.length, 0, consoleErrors.join(" ; "));

  const cleanup = await api("DELETE", "/api/v1/admin/video-lessons/" + id, admin);
  note("cleanup xoá bài học tạm", cleanup.code, 200);
  await browser.close();
  console.log("F88 UI total=" + (pass + fail) + " pass=" + pass + " fail=" + fail);
  process.exit(fail ? 1 : 0);
})().catch(e => { console.error("FATAL", e.message); process.exit(1); });
