const H = require("./lib.js");

const finders = () => {
  const de = document.documentElement;
  const limit = de.clientWidth;
  const out = [];
  for (const el of document.querySelectorAll("body *")) {
    const r = el.getBoundingClientRect();
    if (r.width === 0 && r.height === 0) continue;
    if (r.right > limit + 1 || r.left < -1) {
      const cs = getComputedStyle(el);
      if (cs.position === "fixed") continue;
      out.push({
        tag: el.tagName.toLowerCase(),
        cls: (el.className && el.className.baseVal !== undefined ? el.className.baseVal : String(el.className || "")).slice(0, 90),
        left: Math.round(r.left), right: Math.round(r.right), w: Math.round(r.width),
        pos: cs.position, ov: cs.overflowX,
        parentOverflow: (() => { let p = el.parentElement; while (p) { const s = getComputedStyle(p); if (s.overflowX !== "visible") return p.tagName.toLowerCase() + "." + String(p.className).split(" ")[0] + ":" + s.overflowX; p = p.parentElement; } return "none"; })()
      });
    }
  }
  return { clientW: limit, scrollW: de.scrollWidth, bodyOvX: getComputedStyle(document.body).overflowX, htmlOvX: getComputedStyle(de).overflowX, offenders: out.slice(0, 12), count: out.length };
};

(async () => {
  const browser = await H.pw.chromium.launch({ headless: true });
  for (const [w, h] of [[1440, 900], [375, 812]]) {
    const ctx = await browser.newContext({ viewport: { width: w, height: h } });
    const page = await ctx.newPage();
    for (const route of ["/", "/lessons", "/decks", "/videos", "/leaderboard"]) {
      await page.goto(H.APP + route, { waitUntil: "domcontentloaded" });
      await page.waitForTimeout(2200);
      const info = await page.evaluate(finders);
      console.log("== " + w + "px " + route + " client=" + info.clientW + " scroll=" + info.scrollW
        + " bodyOvX=" + info.bodyOvX + " htmlOvX=" + info.htmlOvX + " offenders=" + info.count);
      for (const o of info.offenders) {
        console.log("   " + o.tag + " ." + o.cls.slice(0, 60) + " [" + o.left + ".." + o.right + "] w=" + o.w + " pos=" + o.pos + " parentClip=" + o.parentOverflow);
      }
    }
    await ctx.close();
  }
  await browser.close();
})().catch(e => { console.error("FATAL", e); process.exit(1); });
