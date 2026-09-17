const H = require("./lib.js");
const API = "http://localhost:8080", APP = "http://localhost:5173";
(async () => {
  const admin = await H.login("admin@gmail.com", "123456");
  const stamp = Date.now();
  const r = await fetch(API + "/api/v1/admin/video-lessons", { method: "POST", headers: { Authorization: "Bearer " + admin, "Content-Type": "application/json" }, body: JSON.stringify({ title: "ZZ dbg toast " + stamp, description: "seed", youtubeUrl: "https://www.youtube.com/watch?v=2VeQTuSSiI0", level: "ELEMENTARY", isPublished: true, transcript: [{ start: 0, end: 2, textEn: "A one." }, { start: 2, end: 4, textEn: "A two." }] }) });
  const seed = await r.json();
  const browser = await H.pw.chromium.launch({ headless: true });
  const ctx = await H.mkContext(browser, admin, { width: 1440, height: 950 });
  const page = await ctx.newPage();
  await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1500);
  await page.fill('input[type="email"], input[name="email"]', "admin@gmail.com");
  await page.fill('input[type="password"]', "123456");
  await page.click('button[type="submit"]');
  await page.waitForTimeout(4000);
  await page.goto(APP + "/admin/videos", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(3000);
  await page.locator("tr", { hasText: "ZZ dbg toast " + stamp }).first().getByRole("button", { name: "Sửa" }).click();
  await page.waitForTimeout(700);
  await page.locator("form#video-lesson-form input").first().fill("ZZ dbg toast " + stamp + " v2");
  await page.getByRole("button", { name: /Lưu bài học/ }).click();
  for (let i = 0; i < 8; i++) {
    await page.waitForTimeout(400);
    const snap = await page.evaluate(() => ({ len: document.body.children.length, txt: document.body.innerText.slice(-400), html: document.body.innerHTML.slice(-500) }));
    console.log("t+" + ((i + 1) * 400) + "ms tail=" + JSON.stringify(snap.txt.slice(-160)));
  }
  await fetch(API + "/api/v1/admin/video-lessons/" + seed.id, { method: "DELETE", headers: { Authorization: "Bearer " + admin } });
  await browser.close();
})().catch(e => { console.error("FATAL", e.message); process.exit(1); });
