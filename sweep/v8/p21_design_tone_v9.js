/**
 * p21_design_tone_v9.js — audit-v9: measure the landing "Playful Geometric" tone
 * items the design prompt names explicitly, so prompt-gap claims are MEASURED
 * rather than asserted:
 *
 *   1. layout rhythm: how often the prompt's anchor utilities (`py-24`,
 *      `max-w-6xl`) actually appear, and the computed section padding/container
 *      width they produce;
 *   2. hero decoration: massive circle, dot pattern, rotated badge, floating
 *      icon - present or absent, with the computed `transform` matrix;
 *   3. scale/rotate on badges: the prompt mentions scale(1.1) and rotate(15deg)
 *      for a pricing/featured badge.
 *
 * Run: set NODE_PATH=%APPDATA%\npm\node_modules&& node p21_design_tone_v9.js
 */
const H = require("./ui/lib.js");

(async () => {
  const browser = await H.pw.chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  const out = {};

  // NOTE (measured 2026-09-17 22:37): this container's Vite dev server stopped
  // answering `GET /` and `GET /src/main.js` (30 s, then no response) while
  // `GET /index.html` and `GET /lessons` kept returning 200 in ~40 ms. Clearing
  // node_modules/.vite and restarting the container did NOT restore it. Workaround
  // used here: load /index.html and reach the SPA routes by CLIENT-side navigation
  // (router links), which never asks the server for `/`. The built assets are
  // unaffected (vite build succeeded the same evening).
  for (const route of ["/", "/premium"]) {
    await page.goto(H.APP + "/index.html", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(1500);
    const link = page.locator("a[href='" + route + "']").first();
    if (await link.count()) {
      // el.click() via JS: the router link may live in a collapsed/desktop-only
      // menu, and Playwright refuses to click an invisible element.
      await link.evaluate((el) => el.click());
    } else {
      await page.evaluate((r) => {
        const a = document.createElement("a");
        a.href = r;
        a.style.cssText = "position:fixed;top:0;left:0;z-index:9999;width:8px;height:8px;";
        document.body.appendChild(a);
        a.click();
        a.remove();
      }, route);
    }
    await page.waitForTimeout(2500);
    if (page.url().replace(H.APP, "").split("?")[0] !== route) {
      console.log("  WARN: client-side nav to " + route + " landed on " + page.url().replace(H.APP, ""));
    }
    out[route] = await page.evaluate(() => {
      const all = [...document.querySelectorAll("*")];
      const byClass = (token) => all.filter((e) => typeof e.className === "string" && e.className.split(/\\s+/).includes(token)).length;
      const containsClass = (frag) => all.filter((e) => typeof e.className === "string" && e.className.includes(frag)).length;

      // Section rhythm: the biggest vertical padding actually computed.
      const pads = all.map((e) => parseFloat(getComputedStyle(e).paddingTop) || 0);
      const maxPad = Math.max(...pads);
      const wideContainers = all
        .map((e) => ({ c: typeof e.className === "string" ? e.className : "", w: Math.round(e.getBoundingClientRect().width) }))
        .filter((x) => /max-w-6xl|max-w-7xl|container|mx-auto/.test(x.c))
        .map((x) => x.w);
      const containerWidths = [...new Set(wideContainers)].sort((a, b) => b - a).slice(0, 5);

      // Decoration: circles, dot patterns, rotated/scaled elements.
      const radii = [...new Set(all.map((e) => getComputedStyle(e).borderRadius))];
      const bigCircles = all.filter((e) => {
        const cs = getComputedStyle(e);
        const r = parseFloat(cs.borderRadius) || 0;
        const w = e.getBoundingClientRect().width;
        return r >= 999 && w >= 60;
      }).length;
      const dotish = all.filter((e) => {
        const cs = getComputedStyle(e);
        return /radial-gradient|repeating-/.test(cs.backgroundImage) || /dot/i.test(cs.backgroundImage);
      }).length;
      const dotComponents = document.querySelectorAll("[class*=dot], [data-deco=dot], .deco-dot").length;
      const transforms = all
        .map((e) => getComputedStyle(e).transform)
        .filter((t) => t && t !== "none");
      const rotated = transforms.filter((t) => /matrix/.test(t) && !t.endsWith(", 0, 0, 1, 0)")).length;
      const scaled = transforms.length;
      const svgs = document.querySelectorAll("svg").length;

      return {
        py24: byClass("py-24"), py16: byClass("py-16"), py20: byClass("py-20"),
        maxW6xl: byClass("max-w-6xl"), maxW7xl: byClass("max-w-7xl"),
        containerish: containsClass("mx-auto"),
        maxPadTop: maxPad,
        containerWidths,
        bigCircles, dotish, dotComponents,
        transformCount: scaled, rotatedish: rotated,
        svgIcons: svgs,
        radiusVariants: radii.length,
        hardShadowEls: all.filter((e) => {
          const s = getComputedStyle(e).boxShadow;
          return s && s !== "none" && /0px 0px/.test(s);
        }).length
      };
    });
    console.log("=== " + route);
    console.log(JSON.stringify(out[route], null, 1));
  }

  await browser.close();
  require("fs").writeFileSync(__dirname + "/p21_design_tone.json", JSON.stringify(out, null, 1));
  console.log("wrote p21_design_tone.json");
})().catch((e) => { console.log("ERR", e.message); process.exit(1); });
