const H = require("./lib.js");

(async () => {
  const browser = await H.pw.chromium.launch({ headless: true });
  const page = await (await browser.newContext({ viewport: { width: 1440, height: 900 } })).newPage();
  console.log("login:", await H.formLogin(page, "admin@gmail.com", "123456"));
  for (const route of ["/", "/lessons", "/premium"]) {
    await page.goto(H.APP + route, { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(2500);
    const out = await page.evaluate(() => {
      const de = document.documentElement;
      const docW = de.clientWidth, sw = de.scrollWidth;
      const bad = [];
      de.querySelectorAll("*").forEach(el => {
        const r = el.getBoundingClientRect();
        if (r.right > docW + 1) {
          const cs = getComputedStyle(el);
          bad.push(el.tagName.toLowerCase() + " ." + String(el.className || "").slice(0, 70)
            + " [" + Math.round(r.left) + ".." + Math.round(r.right) + "] pos=" + cs.position);
        }
      });
      return { docW, sw, overflow: sw > docW + 1, bad: bad.slice(0, 8), n: bad.length };
    });
    console.log("== " + route + " client=" + out.docW + " scrollW=" + out.sw + " OVERFLOW=" + out.overflow + " offenders=" + out.n);
    out.bad.forEach(b => console.log("    " + b));
    await page.screenshot({ path: "ui/shot" + route.replace(/\//g, "_") + ".png" });
  }
  await browser.close();
})().catch(e => { console.error("ERR", e.message); process.exit(1); });
