const H = require("./lib.js");
const fs = require("fs");
const path = require("path");
const EDIR = path.join(__dirname, "..", "evidence", "verify-fixes");
const token = process.env.TOKEN;
if (!token) { console.error("FATAL need TOKEN env"); process.exit(2); }
(async () => {
  const r = await fetch(H.API + "/api/streak/current", { headers: { Authorization: "Bearer " + token } });
  const j = await r.json(); const eff = (j.data || j).currentStreak;
  console.log("effective-after-drift=" + eff);
  const browser = await H.pw.chromium.launch({ headless: true });
  const ctx = await H.mkContext(browser, token);
  const page = await ctx.newPage();
  const errs = [];
  page.on("pageerror", e => errs.push(String(e).slice(0, 160)));
  await page.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((t) => {
    localStorage.setItem("token", t);
    localStorage.setItem("user", JSON.stringify({ id: 2, username: "u", email: "user@gmail.com", fullName: "U", isAdmin: false, currentStreak: 99, totalPoints: 55 }));
  }, token);
  await page.goto(H.APP + "/profile", { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(2500);
  const headerNums = await page.$$eval("p.font-black.text-2xl", els => els.map(e => e.textContent.trim()));
  console.log("header=" + JSON.stringify(headerNums) + " effective=" + eff);
  console.log(headerNums[0] === String(eff) ? "PASS B1-stale-tab-shows-effective" : "FAIL B1-stale-tab-shows-effective :: header=" + headerNums[0] + " eff=" + eff);
  console.log(headerNums[0] === "99" ? "FAIL stale-raw-leaked" : "PASS stale-raw-not-shown");
  await page.screenshot({ path: path.join(EDIR, "profile-stale.png"), fullPage: true });
  await browser.close();
})().catch(e => { console.error("FATAL " + (e && e.stack || e)); process.exit(2); });