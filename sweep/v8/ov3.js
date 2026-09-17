const H = require("./lib.js");
(async () => {
  const br = await H.pw.chromium.launch({ headless: true });
  const pg = await (await br.newContext({ viewport: { width: 1440, height: 900 } })).newPage();
  await pg.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
  await pg.waitForTimeout(1500);
  await pg.fill(String.fromCharCode(39) + "input[type=\\"email\\"]" + String.fromCharCode(39), "admin@gmail.com");
  await pg.fill(String.fromCharCode(39) + "input[type=\\"password\\"]" + String.fromCharCode(39), "123456");
  await pg.click(String.fromCharCode(39) + "button[type=\\"submit\\"]" + String.fromCharCode(39));
  await pg.waitForTimeout(4000);
  for (const r of ["/", "/lessons", "/premium"]) {
    await pg.goto(H.APP + r, { waitUntil: "domcontentloaded" });
    await pg.waitForTimeout(2500);
    const o = await pg.evaluate(() => {
      const d = document.documentElement, w = d.clientWidth, out = [];
      d.querySelectorAll("*").forEach(e => {
        const b = e.getBoundingClientRect();
        if (b.right > w + 1 && getComputedStyle(e).position !== "fixed") {
          out.push(e.tagName.toLowerCase() + " ." + String(e.className || "").slice(0, 60) + " L" + Math.round(b.left) + " R" + Math.round(b.right));
        }
      });
      return { w: w, s: d.scrollWidth, out: out.slice(0, 10), n: out.length };
    });
    console.log(r, "client=" + o.w, "scroll=" + o.s, "over=" + (o.s > o.w + 1), "n=" + o.n);
    o.out.forEach(x => console.log("   ", x));
  }
  await br.close();
})().catch(e => { console.error("ERR", e.message); process.exit(1); });
