const pw = require("playwright-core");
const fs = require("fs");
const APP = "http://localhost:5173";
const DIR = "C:/Users/ASUS/Documents/LAPTRINH/engflow/uploads/";

(async () => {
  const names = ["zzv8-planted.html", "zzv8-planted.svg", "zzv8-planted.js"];
  const html = "<html><body><h1>planted</h1><script>window.__XSS_PWNED__=1;"
    + "fetch('/api/auth/me').then(r=>r.status).then(s=>{window.__CODE=s});</script></body></html>";
  for (const n of names) fs.writeFileSync(DIR + n, html);
  const browser = await pw.chromium.launch({ headless: true });
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  await page.goto(APP + "/", { waitUntil: "domcontentloaded" });
  await page.evaluate(() => localStorage.setItem("token", "victim-jwt-value"));
  for (const n of names) {
    await page.goto(APP + "/api/resources/" + n, { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(900);
    const st = await page.evaluate(() => ({
      pwned: !!window.__XSS_PWNED__,
      fetchedApi: window.__CODE || null,
      ct: document.contentType,
      text: (document.body ? document.body.innerText : "").slice(0, 30).replace(/\s+/g, " ")
    }));
    console.log("BROWSER", n, JSON.stringify(st), st.pwned ? "STILL VULNERABLE" : "SAFE (no script exec)");
  }
  for (const n of names) {
    const r = await fetch(APP + "/api/resources/" + n);
    console.log("HEADERS", n, r.status, "CT=" + r.headers.get("content-type"),
        "DISP=" + r.headers.get("content-disposition"));
  }
  await browser.close();
  for (const n of names) fs.unlinkSync(DIR + n);
  console.log("planted files removed:", names.every(n => !fs.existsSync(DIR + n)));
})().catch(e => { console.error("ERR", e.message); process.exit(1); });
