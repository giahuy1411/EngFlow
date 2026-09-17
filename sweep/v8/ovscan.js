const H = require("./ui/lib.js");
const WIDTHS = [360, 375, 414, 768, 1024, 1280, 1366, 1440, 1500, 1536, 1920];
(async () => {
  const browser = await H.pw.chromium.launch({ headless: true });
  for (const state of ["guest", "admin"]) {
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await ctx.newPage();
    if (state === "admin") {
      await page.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
      await page.waitForTimeout(1200);
      await page.fill("input[type=email]", "admin@gmail.com");
      await page.fill("input[type=password]", "123456");
      await page.click("button[type=submit]");
      await page.waitForTimeout(3500);
    }
    console.log("== " + state.toUpperCase());
    for (const w of WIDTHS) {
      await page.setViewportSize({ width: w, height: 900 });
      await page.goto(H.APP + "/lessons", { waitUntil: "domcontentloaded" });
      await page.waitForTimeout(1600);
      const m = await page.evaluate(() => {
        const d = document.documentElement;
        const burger = document.querySelector(".app-navbar__hamburger");
        const nav = document.querySelector("nav[aria-label=Primary]");
        const links = [...document.querySelectorAll(".app-navbar__link")].map(e => e.getBoundingClientRect().right);
        return {
          cw: d.clientWidth, sw: d.scrollWidth,
          burger: burger ? getComputedStyle(burger).display : "absent",
          navOn: nav ? getComputedStyle(nav).display !== "none" : false,
          maxRight: links.length ? Math.round(Math.max.apply(null, links)) : 0
        };
      });
      console.log("   vw=" + w + " client=" + m.cw + " scrollW=" + m.sw
        + (m.sw > m.cw + 1 ? "  <<<OVERFLOW" : "  ok")
        + " nav=" + (m.navOn ? "on" : "off") + " burger=" + m.burger + " linkMaxRight=" + m.maxRight);
    }
    await ctx.close();
  }
  await browser.close();
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
