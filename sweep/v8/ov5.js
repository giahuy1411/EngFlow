const pw = require("playwright-core");
const APP = "http://localhost:5173";
(async () => {
  const b = await pw.chromium.launch({ headless: true });
  const p = await (await b.newContext({ viewport: { width: 1440, height: 900 } })).newPage();
  await p.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  await p.waitForTimeout(1200);
  await p.fill("input[type=email]", "admin@gmail.com");
  await p.fill("input[type=password]", "123456");
  await p.click("button[type=submit]");
  await p.waitForTimeout(3500);
  const widths = [1280, 1366, 1440, 1500, 1560, 1600, 1720, 1920, 2400];
  for (const w of widths) {
    await p.setViewportSize({ width: w, height: 900 });
    await p.goto(APP + "/lessons", { waitUntil: "domcontentloaded" });
    await p.waitForTimeout(1400);
    const m = await p.evaluate(() => {
      const d = document.documentElement;
      const inner = document.querySelector(".app-navbar__inner");
      const links = document.querySelector("nav[aria-label=Primary]");
      const right = inner ? inner.lastElementChild : null;
      return {
        client: d.clientWidth, scroll: d.scrollWidth,
        innerW: inner ? Math.round(inner.getBoundingClientRect().width) : null,
        linksW: links ? Math.round(links.getBoundingClientRect().width) : null,
        rightW: right ? Math.round(right.getBoundingClientRect().width) : null
      };
    });
    console.log("vw=" + w, "client=" + m.client, "scrollW=" + m.scroll,
      "OVERFLOW=" + (m.scroll > m.client + 1), "inner=" + m.innerW, "links=" + m.linksW, "rightCluster=" + m.rightW);
  }
  await b.close();
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
