const H = require("./lib.js");
const PAGES = ["/", "/lessons", "/lessons/445", "/decks", "/speaking", "/premium", "/leaderboard", "/videos", "/admin/dashboard", "/admin/lessons", "/admin/exercises", "/admin/users", "/profile"];
(async () => {
  const browser = await H.pw.chromium.launch({ headless: true });
  const page = await (await browser.newContext({ viewport: { width: 1440, height: 900 } })).newPage();
  await page.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1200);
  await page.fill("input[type=email]", "admin@gmail.com");
  await page.fill("input[type=password]", "123456");
  await page.click("button[type=submit]");
  await page.waitForTimeout(3000);
  let noAltAtAll = 0, under24 = 0, under44desktop = [];
  for (const r of PAGES) {
    await page.goto(H.APP + r, { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(1500);
    const m = await page.evaluate(() => {
      const imgs = [...document.querySelectorAll("img")];
      const noAlt = imgs.filter(i => !i.hasAttribute("alt"));
      const empty = imgs.filter(i => i.getAttribute("alt") === "");
      const tiny = [...document.querySelectorAll("button, a")]
        .map(b => { const rc = b.getBoundingClientRect(); return { t: (b.innerText || b.getAttribute("aria-label") || "").trim().slice(0, 18), h: Math.round(rc.height), w: Math.round(rc.width) }; })
        .filter(x => x.h > 0 && x.w > 0 && (Math.min(x.h, x.w) < 24));
      return { imgs: imgs.length, noAlt: noAlt.length, noAltSample: noAlt.slice(0, 3).map(i => (i.className || "").slice(0, 30)), empty: empty.length, tiny };
    });
    noAltAtAll += m.noAlt;
    under24 += m.tiny.length;
    if (m.noAlt || m.tiny.length) console.log("  " + r + " imgs=" + m.imgs + " missingAlt=" + m.noAlt + " decorativeAltEmpty=" + m.empty + " tinyTargets=" + m.tiny.length + " " + JSON.stringify(m.tiny.slice(0, 4)));
    m.tiny.forEach(x => under44desktop.push(r + ":" + x.t + ":" + x.h + "x" + x.w));
  }
  console.log("IMG without alt attribute at all: " + noAltAtAll + " (decorative alt='' ones are valid)");
  console.log("Targets under 24px (WCAG 2.5.8 floor): " + under24);
  console.log("  samples: " + JSON.stringify(under44desktop.slice(0, 10)));
  await browser.close();
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
