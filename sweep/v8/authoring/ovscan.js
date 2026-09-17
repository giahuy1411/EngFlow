const pw = require("playwright-core");
const APP = "http://localhost:5173";
const WIDTHS = [375, 640, 768, 1024, 1280, 1366, 1440, 1535, 1536, 1600, 1920];
(async () => {
  const b = await pw.chromium.launch({ headless: true });
  for (const mode of ["guest", "auth"]) {
    const p = await (await b.newContext({ viewport: { width: 1920, height: 900 } })).newPage();
    if (mode === "auth") {
      await p.goto(APP + "/login", { waitUntil: "domcontentloaded" });
      await p.waitForTimeout(1200);
      await p.fill("input[type=email]", "admin@gmail.com");
      await p.fill("input[type=password]", "123456");
      await p.click("button[type=submit]");
      await p.waitForTimeout(3500);
    }
    for (const w of WIDTHS) {
      await p.setViewportSize({ width: w, height: 900 });
      await p.goto(APP + "/lessons", { waitUntil: "domcontentloaded" });
      await p.waitForTimeout(1500);
      const r = await p.evaluate(() => {
        const d = document.documentElement;
        let n = 0, worst = 0;
        d.querySelectorAll("*").forEach(el => {
          const q = el.getBoundingClientRect();
          if (q.width > 0 && q.right > d.clientWidth + 1) { n++; worst = Math.max(worst, Math.round(q.right - d.clientWidth)); }
        });
        const links = [...document.querySelectorAll(".app-navbar__link")];
        const vis = links.filter(l => l.getBoundingClientRect().width > 0);
        const burger = document.querySelector(".app-navbar__hamburger");
        const bvis = burger && getComputedStyle(burger).display !== "none";
        const clipped = vis.filter(l => l.scrollWidth > l.clientWidth + 1).length;
        return { over: d.scrollWidth > d.clientWidth + 1, sw: d.scrollWidth, cw: d.clientWidth,
                 offenders: n, worst, navLinks: vis.length, minLinkW: Math.min(0, ...vis.map(l => Math.round(l.getBoundingClientRect().width))), clipped, burger: !!bvis };
      });
      const bad = r.over || r.offenders > 0 || r.clipped > 0;
      const navOk = r.burger ? true : r.navLinks >= 8;
      console.log((bad || !navOk ? "XX " : "OK ") + mode.padEnd(5) + "w=" + String(w).padStart(4)
        + " scroll=" + r.sw + "/" + r.cw + " off=" + r.offenders + " worst=" + r.worst
        + " links=" + r.navLinks + " clipped=" + r.clipped + " burger=" + r.burger + " navVisible=" + navOk);
    }
    await p.close();
  }
  await b.close();
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
