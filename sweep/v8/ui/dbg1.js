const H = require("./lib.js");
const APP = "http://localhost:5173";
(async () => {
  const admin = await H.login("admin@gmail.com", "123456");
  const browser = await H.pw.chromium.launch({ headless: true });
  const ctx = await H.mkContext(browser, admin, { width: 1440, height: 950 });
  const page = await ctx.newPage();
  const errs = [];
  page.on("console", m => { if (m.type() === "error") errs.push(m.text().slice(0, 200)); });
  page.on("response", r => { if (r.url().includes("/api/v1/admin/video-lessons")) console.log("  api", r.status(), r.url().replace("http://localhost:8080", "")); });
  await H.seedToken(page, admin);
  await page.goto(APP + "/admin/video-lessons", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(6000);
  const info = await page.evaluate(() => ({
    rows: [...document.querySelectorAll("tbody tr")].length,
    txt: (document.querySelector("tbody") ? document.querySelector("tbody").innerText : "").slice(0, 400),
    btns: [...document.querySelectorAll("button")].map(b => b.innerText.trim()).slice(0, 20)
  }));
  console.log(JSON.stringify(info, null, 1));
  console.log("console errors:", errs.length, errs.slice(0, 3));
  await browser.close();
})().catch(e => { console.error("FATAL", e.message); process.exit(1); });
