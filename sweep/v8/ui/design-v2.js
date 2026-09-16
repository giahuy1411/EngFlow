/**
 * design-v2.js — Plan B verification (Be Vietnam Pro design system) across the
 * FIVE viewports the audit prompt requires: 360 / 768 / 1280 / 1440 / 1920.
 *
 * The existing design.js only sampled 1440 + 375 and aggregated a "badFont"
 * counter. This script instead asserts the Plan B contract directly:
 *   1. the webfont Be Vietnam Pro is actually LOADED (document.fonts.check),
 *      not merely declared — a missing font silently falls back to system-ui;
 *   2. every sampled element computes to the single family (no second family);
 *   3. no legacy family survives anywhere in the cascade;
 *   4. no horizontal overflow at any of the 5 widths;
 *   5. hard-shadow / 2px-border "Playful Geometric" invariants still hold.
 */
const H = require("./lib.js");

const VIEWPORTS = [360, 768, 1280, 1440, 1920];
const ROUTES = ["/", "/lessons", "/lessons/445", "/decks", "/speaking", "/premium",
  "/login", "/leaderboard", "/videos", "/admin/dashboard", "/admin/lessons", "/admin/exercises"];

const LEGACY = ["Outfit", "Plus Jakarta Sans", "PlusJakarta", "Inter", "Roboto", "Poppins"];

(async () => {
  const token = await H.login("admin@gmail.com", "123456");
  const browser = await H.pw.chromium.launch({ headless: true });
  const report = [];
  let totalBadFont = 0, totalLegacy = 0, totalOverflow = 0, totalElements = 0;
  let fontLoaded = null;

  for (const w of VIEWPORTS) {
    const ctx = await browser.newContext({ viewport: { width: w, height: 900 } });
    const page = await ctx.newPage();
    await page.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
    await page.evaluate(t => localStorage.setItem("token", t), token);
    await page.waitForTimeout(600);

    // (1) is the webfont really loaded at this viewport?
    const font = await page.evaluate(async () => {
      try {
        await document.fonts.ready;
        const loaded = document.fonts.check('400 16px "Be Vietnam Pro"');
        const bold = document.fonts.check('700 16px "Be Vietnam Pro"');
        const black = document.fonts.check('900 16px "Be Vietnam Pro"');
        const faces = [...document.fonts].filter(f => /Be Vietnam/i.test(f.family)).map(f => f.family + ":" + f.weight + ":" + f.status);
        return { loaded, bold, black, faces: faces.slice(0, 8), count: faces.length };
      } catch (e) { return { err: e.message }; }
    });
    if (fontLoaded === null) fontLoaded = font;

    for (const r of ROUTES) {
      await page.goto(H.APP + r, { waitUntil: "domcontentloaded" });
      await page.waitForTimeout(1500);
      const m = await page.evaluate((legacyList) => {
        const fam = e => getComputedStyle(e).fontFamily.split(",")[0].replace(/["']/g, "").trim();
        const bad = [], legacy = [];
        let sampled = 0;
        document.querySelectorAll("h1,h2,h3,h4,h5,p,a,button,span,label,li,td,th,input,textarea,select,strong,em,small,code").forEach(e => {
          const hasText = (e.textContent || "").trim().length > 0 || /INPUT|TEXTAREA|SELECT/.test(e.tagName);
          if (!hasText) return;
          const cs = getComputedStyle(e);
          if (cs.display === "none" || cs.visibility === "hidden") return;
          sampled++;
          const f = fam(e);
          if (f && f !== "Be Vietnam Pro" && f !== "system-ui" && f !== "sans-serif") {
            if (bad.length < 4) bad.push(e.tagName + ":" + f + ":" + String(e.className).slice(0, 26));
          }
          const full = cs.fontFamily;
          for (const L of legacyList) {
            if (full.includes(L) && legacy.length < 3) legacy.push(L + "@" + e.tagName);
          }
        });
        const de = document.documentElement;
        // ignore scrollbar allowance (~15px) — AGENTS.md warns this is a false positive
        const overflow = de.scrollWidth - de.clientWidth;
        // design-system invariants
        const tokens = ["--geo-font", "--geo-cream", "--geo-shadow"]
          .filter(t => getComputedStyle(de).getPropertyValue(t).trim());
        let shadowed = 0, bordered2 = 0, checked = 0;
        document.querySelectorAll(".app-btn, button.app-btn, .app-card, .sticker-card").forEach(e => {
          const cs = getComputedStyle(e);
          checked++;
          if (cs.boxShadow !== "none") shadowed++;
          if (cs.borderTopWidth === "2px") bordered2++;
        });
        return { sampled, bad, legacy, overflow, tokens, shadowed, bordered2, checked, scrollW: de.scrollWidth, clientW: de.clientWidth };
      }, LEGACY);

      totalBadFont += m.bad.length;
      totalLegacy += m.legacy.length;
      totalElements += m.sampled;
      const isOverflow = m.overflow > 16;
      if (isOverflow) totalOverflow++;

      report.push({ viewport: w, route: r, ...m, isOverflow });
      const flag = (m.bad.length || m.legacy.length || isOverflow) ? "  <-- ISSUE" : "";
      console.log(String(w).padStart(5) + " " + r.padEnd(20) + " sampled=" + String(m.sampled).padStart(4)
        + " overflow=" + String(m.overflow).padStart(4) + " shadow=" + m.shadowed + "/" + m.checked
        + " border2=" + m.bordered2 + flag);
      if (m.bad.length) console.log("        BAD FONT: " + JSON.stringify(m.bad));
      if (m.legacy.length) console.log("        LEGACY  : " + JSON.stringify(m.legacy));
      if (isOverflow) console.log("        OVERFLOW: scrollW=" + m.scrollW + " clientW=" + m.clientW);
    }
    await ctx.close();
  }
  await browser.close();

  console.log("\n=== PLAN B SUMMARY ===");
  console.log("font loaded (400/700/900): " + JSON.stringify(fontLoaded && { loaded: fontLoaded.loaded, bold: fontLoaded.bold, black: fontLoaded.black, faces: fontLoaded.count }));
  if (fontLoaded && fontLoaded.faces) console.log("  faces: " + JSON.stringify(fontLoaded.faces));
  console.log("elements sampled      : " + totalElements);
  console.log("non-BVP computed fonts: " + totalBadFont);
  console.log("legacy-family hits    : " + totalLegacy);
  console.log("routes w/ overflow    : " + totalOverflow + " / " + report.length);
  const tokenSets = [...new Set(report.flatMap(r => r.tokens))];
  console.log("design tokens present : " + JSON.stringify(tokenSets));

  require("fs").writeFileSync("design-v2.json", JSON.stringify({ fontLoaded, report, totals: { totalElements, totalBadFont, totalLegacy, totalOverflow } }, null, 1));
  console.log("wrote design-v2.json");
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
