const H = require("./lib.js");
const APP = "http://localhost:5173";
(async () => {
  const admin = await H.login("admin@gmail.com", "123456");
  const browser = await H.pw.chromium.launch({ headless: true });
  const ctx = await H.mkContext(browser, admin, { width: 1440, height: 950 });
  const page = await ctx.newPage();
  page.on("console", m => console.log("  console." + m.type() + ":", m.text().slice(0, 200)));
  page.on("response", r => { if (r.url().includes("/api/")) console.log("  api", r.status(), r.url().replace("http://localhost:8080", "")); });
  await H.seedToken(page, admin);
  await page.goto(APP + "/admin/videos", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(7000);
  const info = await page.evaluate(() => ({ url: location.href, rows: document.querySelectorAll("tbody tr").length, txt: document.body.innerText.slice(0, 500) }));
  console.log(JSON.stringify(info, null, 1));
  await browser.close();
})().catch(e => { console.error("FATAL", e.message); process.exit(1); });
