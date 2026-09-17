const pw = require("playwright-core");
(async () => {
  const b = await pw.chromium.launch({ headless: true });
  const p = await (await b.newContext({ viewport: { width: 1280, height: 900 } })).newPage();
  await p.goto("http://localhost:5173/login", { waitUntil: "domcontentloaded" });
  await p.waitForTimeout(1500);
  await p.fill("input[type=email]", "admin@gmail.com");
  await p.fill("input[type=password]", "123456");
  await p.click("button[type=submit]");
  await p.waitForTimeout(4000);
  await p.goto("http://localhost:5173/lessons", { waitUntil: "domcontentloaded" });
  await p.waitForTimeout(2000);
  console.log(JSON.stringify(await p.evaluate(() => {
    const inner = document.querySelector(".app-navbar__inner");
    const cs = getComputedStyle(inner);
    const kids = [...inner.children].map(k => ({
      cls: String(k.className).slice(0, 42),
      w: Math.round(k.getBoundingClientRect().width),
      shrink: getComputedStyle(k).flexShrink,
      basis: getComputedStyle(k).flexBasis
    }));
    const links = [...document.querySelectorAll(".app-navbar__link")].map(l => ({
      t: l.innerText.trim(), w: Math.round(l.getBoundingClientRect().width)
    }));
    return { gap: cs.gap, padL: cs.paddingLeft, maxW: cs.maxWidth, innerW: Math.round(inner.getBoundingClientRect().width),
      avail: Math.round(inner.clientWidth - parseFloat(cs.paddingLeft) - parseFloat(cs.paddingRight)),
      kids, links, linksTotal: links.reduce((a, x) => a + x.w, 0) };
  }), null, 1));
  await b.close();
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
