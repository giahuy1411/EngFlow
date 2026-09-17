const pw = require("playwright-core");
const APP = "http://localhost:5173";
(async () => {
  const b = await pw.chromium.launch({ headless: true });
  const p = await (await b.newContext({ viewport: { width: 1440, height: 900 } })).newPage();
  await p.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  await p.waitForTimeout(1500);
  await p.fill("input[type=email]", "admin@gmail.com");
  await p.fill("input[type=password]", "123456");
  await p.click("button[type=submit]");
  await p.waitForTimeout(4000);
  console.log("after login:", JSON.stringify(await p.evaluate(() => ({ path: location.pathname, tok: !!localStorage.getItem("token") }))));
  for (const rt of ["/", "/lessons", "/premium"]) {
    await p.goto(APP + rt, { waitUntil: "domcontentloaded" });
    await p.waitForTimeout(2500);
    const info = await p.evaluate(() => {
      const d = document.documentElement;
      const docW = d.clientWidth, sw = d.scrollWidth;
      const hits = [];
      d.querySelectorAll("*").forEach(el => {
        const r = el.getBoundingClientRect();
        if (r.width > 0 && r.right > docW + 1) {
          hits.push(el.tagName.toLowerCase() + "|" + String(el.className).slice(0, 58) + "|L" + Math.round(r.left) + " R" + Math.round(r.right));
        }
      });
      return { docW: docW, sw: sw, over: sw > docW + 1, n: hits.length, hits: hits.slice(0, 6) };
    });
    console.log(rt, "client=" + info.docW, "scrollW=" + info.sw, "OVERFLOW=" + info.over, "offenders=" + info.n);
    info.hits.forEach(h => console.log("    " + h));
  }
  await b.close();
})().catch(e => { console.log("ERR " + e.message); process.exit(1); });
