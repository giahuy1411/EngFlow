/**
 * audit-v8 vòng 3 — verify UI-3 bằng browser THẬT (2 ảnh preview admin không render với dữ liệu seed,
 * nên phải tự tạo state tạm: lesson → section → block IMAGE có imageUrl → mở builder → đọc alt).
 * Cleanup: xoá lesson tạm (cascade) + parity.
 */
const H = require("./lib.js");
const { execFileSync } = require("child_process");
const API = "http://localhost:8080";
const STAMP = Date.now().toString().slice(-6);
let pass = 0, fail = 0;
const out = [];
function note(name, got, want, extra) {
  const ok = String(got) === String(want);
  ok ? pass++ : fail++;
  out.push({ name, got: String(got), want: String(want), pass: ok });
  console.log((ok ? "  OK   " : "  FAIL ") + name + " | got=" + got + " want=" + want + (extra ? " | " + String(extra).slice(0, 140) : ""));
}
function sql(q) {
  return execFileSync("docker", ["exec", "engflow-sqlserver", "/opt/mssql-tools18/bin/sqlcmd",
    "-S", "localhost", "-U", "sa", "-P", "YourPassword123", "-d", "english_learning",
    "-C", "-I", "-h", "-1", "-W", "-Q", "SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; " + q], { encoding: "utf8" }).trim();
}
async function api(method, path, token, body) {
  const headers = { Authorization: "Bearer " + token };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const r = await fetch(API + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  return { code: r.status, txt: await r.text() };
}

(async () => {
  const admin = await H.login("admin@gmail.com", "123456");
  if (!admin) throw new Error("admin login failed");
  const lessonsBefore = Number(sql("SELECT COUNT(*) FROM lessons"));
  const sectionsBefore = Number(sql("SELECT COUNT(*) FROM lesson_sections"));
  const blocksBefore = Number(sql("SELECT COUNT(*) FROM lesson_blocks"));

  let r = await api("POST", "/api/admin/lessons", admin,
    { title: "ZZ v3 alt lesson " + STAMP, content: "Bài học tạm để kiểm tra alt của ảnh preview trong builder — đủ dài.", level: "ELEMENTARY", category: "GRAMMAR", durationMinutes: 5, isPublished: false, orderIndex: 9999 });
  note("tạo lesson tạm (admin)", r.code, 200, r.txt.slice(0, 80));
  const lessonId = JSON.parse(r.txt).id;

  r = await api("POST", "/api/admin/lessons/" + lessonId + "/sections", admin, { title: "ZZ v3 section", orderIndex: 1 });
  const secId = JSON.parse(r.txt).id;
  note("tạo section", r.code, 200, "secId=" + secId);

  const CAPTION = "ZZ v3 caption ảnh minh hoạ";
  r = await api("POST", "/api/admin/sections/" + secId + "/blocks", admin,
    { blockType: "IMAGE", data: JSON.stringify({ imageUrl: "/api/resources/4.png", caption: CAPTION }), orderIndex: 1 });
  note("tạo block IMAGE có imageUrl", r.code, 200, r.txt.slice(0, 90));
  const blocks = JSON.parse(r.txt).blocks || [];
  const blkId = blocks.length ? blocks[blocks.length - 1].id : null;
  note("DB có block IMAGE với imageUrl", Number(sql("SELECT COUNT(*) FROM lesson_blocks WHERE data LIKE '%imageUrl%'")), 1, "blkId=" + blkId);

  const browser = await H.pw.chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  await page.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1500);
  await page.fill("input[type=email]", "admin@gmail.com");
  await page.fill("input[type=password]", "123456");
  await page.click("button[type=submit]");
  await page.waitForTimeout(3500);
  await page.goto(H.APP + "/admin/" + lessonId + "/build", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(3500);
  let imgs = await page.evaluate(() => [...document.querySelectorAll("img")].map(i => ({
    src: (i.getAttribute("src") || "").slice(0, 40), alt: i.getAttribute("alt"), has: i.hasAttribute("alt"),
    visible: i.getBoundingClientRect().height > 0 })));
  if (!imgs.some(i => i.src.includes("/api/resources/"))) {
    const head = await page.$$("button");
    for (const b of head) { const t = (await b.innerText()).trim(); if (/SECTION|Chương|Mở|▼|⌄/i.test(t)) { await b.click().catch(() => {}); await page.waitForTimeout(600); } }
    await page.waitForTimeout(1500);
    imgs = await page.evaluate(() => [...document.querySelectorAll("img")].map(i => ({
      src: (i.getAttribute("src") || "").slice(0, 40), alt: i.getAttribute("alt"), has: i.hasAttribute("alt"),
      visible: i.getBoundingClientRect().height > 0 })));
  }
  const dbg = await page.evaluate(() => ({
    text: document.body.innerText.replace(/\s+/g, " ").slice(0, 260),
    hasSection: document.body.innerText.includes("ZZ v3 section"),
    imgs: [...document.querySelectorAll("img")].length,
    inputs: [...document.querySelectorAll("input")].map(i => (i.value || "").slice(0, 40)).slice(0, 8)
  }));
  console.log("  DEBUG", JSON.stringify(dbg).slice(0, 700));
  const preview = imgs.find(i => i.src.includes("/api/resources/"));
  note("builder render ảnh preview của block IMAGE", !!preview, "true", JSON.stringify(imgs.slice(0, 4)));
  if (preview) {
    note("ảnh preview CÓ attribute alt (UI-3)", preview.has, "true", "alt=" + JSON.stringify(preview.alt));
    note("alt lấy từ caption của block", preview.alt, CAPTION);
  }
  note("trang builder: 0 ảnh thiếu hẳn attribute alt", await page.evaluate(() => [...document.querySelectorAll("img")].filter(i => !i.hasAttribute("alt")).length), 0);

  // ---- phần 2: ảnh preview trong modal bài tập (không cần dữ liệu thật, chỉ mở form) ----
  await page.goto(H.APP + "/admin/exercises", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(3000);
  const addBtn = await page.$("text=Thêm bài tập");
  if (addBtn) await addBtn.click(); else await page.click("button.app-btn");
  await page.waitForTimeout(1500);
  const urlInput = await page.$('input[type="url"][placeholder="https://example.com/image.jpg"]');
  note("modal bài tập có input URL hình ảnh", !!urlInput, "true");
  if (urlInput) {
    await urlInput.fill("/api/resources/4.png");
    await page.waitForTimeout(1200);
    const prev = await page.evaluate(() => {
      const i = [...document.querySelectorAll("img")].find(x => (x.getAttribute("src") || "").includes("/api/resources/"));
      return i ? { has: i.hasAttribute("alt"), alt: i.getAttribute("alt"), visible: i.getBoundingClientRect().height > 0 } : null;
    });
    note("modal render ảnh preview", !!prev, "true", JSON.stringify(prev));
    if (prev) {
      note("ảnh preview trong modal CÓ attribute alt (UI-3)", prev.has, "true", "alt=" + JSON.stringify(prev.alt));
      note("alt đúng nội dung mô tả", prev.alt, "Xem trước hình ảnh của câu hỏi");
    }
    note("trang admin/exercises: 0 ảnh thiếu hẳn alt", await page.evaluate(() => [...document.querySelectorAll("img")].filter(i => !i.hasAttribute("alt")).length), 0);
  }
  await browser.close();

  console.log("=== CLEANUP ===");
  if (blkId) { await api("DELETE", "/api/admin/blocks/" + blkId, admin); }
  await api("DELETE", "/api/admin/sections/" + secId, admin);
  r = await api("DELETE", "/api/admin/lessons/" + lessonId, admin);
  note("xoá lesson tạm (cascade)", (r.code === 200 || r.code === 204), "true", "code=" + r.code);
  note("lessons về baseline", Number(sql("SELECT COUNT(*) FROM lessons")), lessonsBefore);
  note("lesson_sections về baseline", Number(sql("SELECT COUNT(*) FROM lesson_sections")), sectionsBefore);
  note("lesson_blocks về baseline", Number(sql("SELECT COUNT(*) FROM lesson_blocks")), blocksBefore);

  require("fs").writeFileSync(__dirname + "/v3b.json", JSON.stringify(out, null, 1));
  console.log("ASSERT PASS=" + pass + " FAIL=" + fail);
})().catch(e => { console.error("FATAL", e); process.exit(1); });
