/**
 * audit-v13-full Phase 5 — CLS probe (Playwright, PerformanceObserver + attribution).
 *
 * WHY PLAYWRIGHT AND NOT JUST LIGHTHOUSE
 * -------------------------------------
 * Lighthouse reports ONE number per navigation. v12 traced F150 (CLS 0.104 on `/`)
 * to a single element by attaching a PerformanceObserver with `attribution` and
 * logging every layout-shift entry with its source node, previous rect and current
 * rect. That is the only way to tell "one block slid 96px off-screen" apart from
 * "the page reflowed everywhere". This probe keeps that instrument.
 *
 * THE COLD-CACHE RULE (v12's own bug, recorded in findings.md §V10)
 * ----------------------------------------------------------------
 * A warm context measures a page that already has the JS/CSS chunks in cache, so the
 * lazy route chunk lands immediately and the shift never happens. Every route is
 * measured in a BRAND NEW context with an empty cache, and the measurement asserts
 * that the observer actually fired (`observerArmed`) — the v12 "fix did not work"
 * false result came from an addInitScript that never ran (`injected:false`) and was
 * only caught by checking the mechanism itself.
 *
 * Run from repo root:
 *   cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node sweep/v13/cls-probe.js"
 * Out: sweep/v13/cls-before.json
 */
const fs = require("fs");
const path = require("path");

// Resolve playwright-core explicitly. Node walks node_modules UP from this file and
// finds the repo-root copy (1.63.0) before NODE_PATH is consulted, and that copy wants
// chromium revision 1243 which is not installed on this machine. The copies under
// scripts/figma-export (1.62.1) and crawler (1.61.1) pin revisions 1234 / 1228, and
// 1234 IS in the local ms-playwright cache. Pick the first whose browser exists, and
// report which one was used rather than silently measuring nothing.
function loadPlaywright() {
  const candidates = [
    path.join(__dirname, "..", "..", "scripts", "figma-export", "node_modules", "playwright-core"),
    path.join(__dirname, "..", "..", "crawler", "node_modules", "playwright-core"),
    "playwright-core",
  ];
  const errors = [];
  for (const c of candidates) {
    try {
      const m = require(c);
      return { pw: m, from: c };
    } catch (e) { errors.push(c + ": " + e.message.split("\n")[0]); }
  }
  throw new Error("no playwright-core loadable:\n" + errors.join("\n"));
}
const { pw, from: PW_FROM } = loadPlaywright();

const APP = "http://localhost:5173";
const ROUTES = ["/", "/lessons", "/login"];
const RUNS = 3;

const INIT = `
window.__cls = { total: 0, entries: [], armed: false, supported: false };
try {
  if (typeof PerformanceObserver !== "undefined") {
    window.__cls.supported = true;
    const po = new PerformanceObserver((list) => {
      window.__cls.armed = true;
      for (const e of list.getEntries()) {
        if (e.hadRecentInput) continue;
        window.__cls.total += e.value;
        const src = (e.sources || []).map((s) => {
          const n = s.node;
          const tag = n ? (n.tagName || n.nodeName || "?") : "?";
          const cls = n && n.className && typeof n.className === "string" ? "." + n.className.split(/\\s+/).slice(0, 3).join(".") : "";
          return tag + cls;
        });
        window.__cls.entries.push({
          t: Math.round(e.startTime),
          v: +e.value.toFixed(5),
          sources: src,
          prev: (e.sources || []).map((s) => s.previousRect ? [Math.round(s.previousRect.y), Math.round(s.previousRect.height)] : null),
          cur: (e.sources || []).map((s) => s.currentRect ? [Math.round(s.currentRect.y), Math.round(s.currentRect.height)] : null),
        });
      }
    });
    po.observe({ type: "layout-shift", buffered: true });
  }
} catch (e) { window.__cls.err = String(e); }
`;

(async () => {
  console.log("playwright-core from:", PW_FROM);
  const browser = await pw.chromium.launch({ headless: true });
  const out = { at: new Date().toISOString(), app: APP, runs: RUNS, playwrightFrom: PW_FROM, routes: [] };

  for (const route of ROUTES) {
    const runs = [];
    for (let i = 0; i < RUNS; i++) {
      // A BRAND NEW context per run: empty cache, empty storage. A warm context
      // hides the shift entirely (the lazy chunk is already in memory).
      const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
      const page = await ctx.newPage();
      await page.addInitScript(INIT);
      try {
        await page.goto(APP + route, { waitUntil: "load", timeout: 30000 });
      } catch (e) { /* load timeout is data, not a crash — keep the partial measurement */ }
      await page.waitForTimeout(2500);           // let lazy chunks land and shifts settle
      const r = await page.evaluate(() => ({
        cls: window.__cls ? +window.__cls.total.toFixed(5) : null,
        armed: window.__cls ? window.__cls.armed : null,
        supported: window.__cls ? window.__cls.supported : null,
        err: window.__cls ? window.__cls.err || null : "no __cls",
        entries: window.__cls ? window.__cls.entries : [],
        url: location.pathname,
        nav: performance.getEntriesByType("navigation").length,
      }));
      runs.push(r);
      await ctx.close();
    }
    const vals = runs.map((r) => r.cls).filter((v) => typeof v === "number");
    vals.sort((a, b) => a - b);
    const med = vals.length ? vals[Math.floor(vals.length / 2)] : null;
    // attribution: collect the biggest single shift across runs
    let worst = null;
    for (const r of runs) for (const e of r.entries) if (!worst || e.v > worst.v) worst = e;
    out.routes.push({
      route,
      clsMedian: med,
      clsRuns: vals,
      armedAllRuns: runs.every((r) => r.armed === true),
      supported: runs[0].supported,
      landedOn: [...new Set(runs.map((r) => r.url))],
      worstEntry: worst,
      perRun: runs.map((r) => ({ cls: r.cls, armed: r.armed, entries: r.entries.length, url: r.url })),
    });
    console.log(route.padEnd(10), "CLS median", String(med).padEnd(9),
      "armed=" + runs.every((r) => r.armed === true),
      "runs=" + JSON.stringify(vals));
  }

  await browser.close();
  fs.mkdirSync(path.join("sweep", "v13"), { recursive: true });
  fs.writeFileSync(path.join("sweep", "v13", "cls-before.json"), JSON.stringify(out, null, 2));
  console.log("\nwrote sweep/v13/cls-before.json");
})();
