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
  console.log("after login path:", p.url());
  for (const route of ["/", "/lessons"]) {
    await p.goto(APP + route, { waitUntil: "domcontentloaded" });
    await p.waitForTimeout(2500);
    const out = await p.evaluate(() => {
      const de = document.documentElement;
      const docW = de.clientWidth;
      const sw = de.scrollWidth;
      const bad = [];
      de.querySelectorAll("*").forEach(el => {
        const r = el.getBoundingClientRect();
        if (r.right > docW + 1) {
          const cs = getComputedStyle(el);
          bad.push(el.tagName.toLowerCase() + " [" + Math.round(r.left) + ".." + Math.round(r.right) + "] "
            + cs.position + " ." + String(el.className || "").slice(0, 60));
        }
      });
      return { docW, sw, bad: bad.slice(0, 10), n: bad.length };
    });
    console.log(route, "client=" + out.docW, "scrollW=" + out.sw, "OVERFLOW=" + (out.sw > out.docW + 1), "offenders=" + out.n);
    out.bad.forEach(x => console.log("   ", x));
  }
  await b.close();
})().catch(e => { console.error("ERR", e.message); process.exit(1); });
