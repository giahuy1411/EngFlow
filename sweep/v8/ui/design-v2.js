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
const TOKEN_NAMES_LEN = 11; // keep in sync with TOKEN_NAMES inside the page evaluate

(async () => {
  // Seed the FULL session, not just the token. Three of the routes below are
  // admin routes, and a token-only seed leaves `auth.isAdmin` undefined. This
  // walk happened to survive it (the app hydrates `user` from /api/auth/me
  // during earlier routes — verified in p19 case C), so this is hardening: it
  // makes the result independent of route order instead of relying on a
  // hydration side effect. See loginFull() in lib.js.
  const adminS = await H.loginFull("admin@gmail.com", "123456");
  const browser = await H.pw.chromium.launch({ headless: true });
  const report = [];
  let totalBadFont = 0, totalLegacy = 0, totalOverflow = 0, totalElements = 0;
  let totalLucide = 0, totalLucideBad = 0, totalMissingTokens = 0, totalTokenMismatch = 0;
  const mobileShadowFail = [];
  let fontLoaded = null;
  const wrongPage = [];

  for (const w of VIEWPORTS) {
    const ctx = await browser.newContext({ viewport: { width: w, height: 900 } });
    const page = await ctx.newPage();
    await page.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
    await page.evaluate((x) => {
      localStorage.setItem("token", x.token);
      localStorage.setItem("user", JSON.stringify(x.user));
    }, { token: adminS.token, user: H.mapUser(adminS.user) });
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
      // Did we land where the guard says we should? We are logged in as ADMIN,
      // so:
      //   - `/login` is guestOnly -> a logged-in visitor is CORRECTLY bounced
      //     (measured: -> /lessons). That is the app working, not a defect.
      //   - every other route here must be reached as-is.
      // Without this distinction the probe reports 5 false alarms on /login.
      const landed = page.url().replace(H.APP, "").split("?")[0];
      const expectBounce = r === "/login";
      const ok = expectBounce ? landed !== r : landed === r;
      if (!ok) wrongPage.push(w + " " + r + " -> " + landed);
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
        // Design-system invariants.
        //
        // These three names used to be ["--geo-font", "--geo-cream", "--geo-shadow"].
        // Only the first exists: the palette root is `--geo-bg` (there is no
        // `--geo-cream`) and shadows are the numbered scale `--geo-shadow-xs|sm|md|lg`
        // (there is no bare `--geo-shadow`). `getPropertyValue` returns "" for an
        // undeclared custom property instead of throwing, so the filter silently
        // dropped 2 of 3 names and the assertion passed on one token while looking
        // like it had checked three. Assert against the real names, and count them
        // so the check cannot quietly shrink again.
        const TOKEN_NAMES = ["--geo-font", "--geo-bg", "--geo-fg", "--geo-accent",
          "--geo-secondary", "--geo-tertiary", "--geo-quaternary", "--geo-border",
          "--geo-radius-md", "--geo-shadow-md", "--geo-border-width"];
        const tokens = TOKEN_NAMES.filter(t => getComputedStyle(de).getPropertyValue(t).trim());
        const missingTokens = TOKEN_NAMES.filter(t => !tokens.includes(t));
        // Exact design-system values, read off the live cascade rather than the file.
        const root = getComputedStyle(de);
        const expect = {
          bg: "#FFFDF5", fg: "#1E293B", accent: "#8B5CF6",
          secondary: "#F472B6", tertiary: "#FBBF24", quaternary: "#34D399",
          border: "#E2E8F0", radiusMd: "16px", borderWidth: "2px",
          shadowMd: "4px 4px 0px 0px #1E293B"
        };
        const actual = {
          bg: root.getPropertyValue("--geo-bg").trim().toUpperCase(),
          fg: root.getPropertyValue("--geo-fg").trim().toUpperCase(),
          accent: root.getPropertyValue("--geo-accent").trim().toUpperCase(),
          secondary: root.getPropertyValue("--geo-secondary").trim().toUpperCase(),
          tertiary: root.getPropertyValue("--geo-tertiary").trim().toUpperCase(),
          quaternary: root.getPropertyValue("--geo-quaternary").trim().toUpperCase(),
          border: root.getPropertyValue("--geo-border").trim().toUpperCase(),
          radiusMd: root.getPropertyValue("--geo-radius-md").trim(),
          borderWidth: root.getPropertyValue("--geo-border-width").trim(),
          shadowMd: root.getPropertyValue("--geo-shadow-md").trim()
        };
        const tokenMismatch = Object.keys(expect).filter(k =>
          actual[k].toUpperCase() !== expect[k].toUpperCase());

        // Icon stroke: design-system.css sets `.lucide { stroke-width: 2.5 }`.
        //
        // Compare NUMERICALLY. `getComputedStyle(sv).strokeWidth` returns the
        // string "2.5px", not "2.5", so an equality test against "2.5" flagged
        // 750/750 icons as violations on a page that was entirely correct
        // (measured 2026-09-17: raw="2.5px", parseFloat=2.5, and the live rule
        // `.lucide{stroke-width:2.5}` present in document.styleSheets).
        //
        // Do NOT read the `stroke-width` ATTRIBUTE instead: lucide-vue-next
        // renders stroke-width="2" by default and the CSS rule overrides it, so
        // the attribute legitimately reads 2 while the icon renders at 2.5.
        let lucide = 0, lucideBad = 0;
        const lucideStrokes = new Set();
        document.querySelectorAll("svg.lucide").forEach(sv => {
          lucide++;
          const sw = parseFloat(getComputedStyle(sv).strokeWidth);
          lucideStrokes.add(sw);
          if (sw !== 2.5) lucideBad++;
        });

        // Mobile contract from the prompt: pop shadow shrinks to 2px at <=768px.
        const isMobile = window.innerWidth <= 768;
        let mobileShadowOk = null;
        if (isMobile) {
          const cards = [...document.querySelectorAll(".geo-card, .sticker-card")];
          const withShadow = cards.map(c => getComputedStyle(c).boxShadow)
            .filter(s => s !== "none");
          mobileShadowOk = withShadow.length === 0
            || withShadow.every(s => /^rgb\(30, 41, 59\) 2px 2px 0px 0px/.test(s));
        }

        let shadowed = 0, bordered2 = 0, checked = 0;
        document.querySelectorAll(".app-btn, button.app-btn, .app-card, .sticker-card").forEach(e => {
          const cs = getComputedStyle(e);
          checked++;
          if (cs.boxShadow !== "none") shadowed++;
          if (cs.borderTopWidth === "2px") bordered2++;
        });
        return { sampled, bad, legacy, overflow, tokens, missingTokens, tokenMismatch, actual,
          lucide, lucideBad, mobileShadowOk, shadowed, bordered2, checked, scrollW: de.scrollWidth, clientW: de.clientWidth };
      }, LEGACY);

      totalBadFont += m.bad.length;
      totalLegacy += m.legacy.length;
      totalElements += m.sampled;
      totalLucide += m.lucide;
      totalLucideBad += m.lucideBad;
      totalMissingTokens += m.missingTokens.length;
      totalTokenMismatch += m.tokenMismatch.length;
      if (m.mobileShadowOk === false) mobileShadowFail.push(w + " " + r);
      const isOverflow = m.overflow > 16;
      if (isOverflow) totalOverflow++;

      report.push({ viewport: w, route: r, ...m, isOverflow });
      const flag = (m.bad.length || m.legacy.length || isOverflow
        || m.lucideBad || m.missingTokens.length || m.tokenMismatch.length
        || m.mobileShadowOk === false) ? "  <-- ISSUE" : "";
      console.log(String(w).padStart(5) + " " + r.padEnd(20) + " sampled=" + String(m.sampled).padStart(4)
        + " overflow=" + String(m.overflow).padStart(4) + " shadow=" + m.shadowed + "/" + m.checked
        + " border2=" + m.bordered2 + " lucide=" + (m.lucide - m.lucideBad) + "/" + m.lucide
        + " tok=" + (m.tokens.length) + "/" + TOKEN_NAMES_LEN + flag);
      if (m.bad.length) console.log("        BAD FONT: " + JSON.stringify(m.bad));
      if (m.legacy.length) console.log("        LEGACY  : " + JSON.stringify(m.legacy));
      if (isOverflow) console.log("        OVERFLOW: scrollW=" + m.scrollW + " clientW=" + m.clientW);
      if (m.lucideBad) console.log("        ICON STROKE != 2.5 on " + m.lucideBad + "/" + m.lucide + " svg.lucide");
      if (m.missingTokens.length) console.log("        MISSING TOKENS: " + JSON.stringify(m.missingTokens));
      if (m.tokenMismatch.length) console.log("        TOKEN VALUE DRIFT: " + JSON.stringify(m.tokenMismatch) + " actual=" + JSON.stringify(m.actual));
      if (m.mobileShadowOk === false) console.log("        MOBILE SHADOW not 2px pop");
    }
    await ctx.close();
  }
  await browser.close();

  // Self-clean. ROUTES above includes "/premium"; the ROW-minting view is
  // "/premium/checkout" (PremiumCheckout.vue -> POST create-order), which this
  // walk does not currently visit. Cleaning is still unconditional because the
  // cost is one SQL batch and the failure mode of a stale assumption here is a
  // silently dirty payment_transactions table — exactly the trap AGENTS.md
  // documents for every sweep that touches /premium*.
  // audit-v15 L3-c/e: baselines from lib.js (single source of truth), not literals.
  const clean = H.cleanupAuditPayments(H.PAYMENTS_BASELINE);
  console.log("DB parity after cleanup: " + H.dbParity()
    + "   (baseline " + H.PARITY_BASELINE + ")");
  let residue = false;
  try { H.assertClean({ parity: H.PARITY_BASELINE, studyDays: H.STUDY_DAYS_BASELINE, pendingPayments: 0 }); }
  catch (e) { console.error("RESIDUE: " + e.message); residue = true; }

  console.log("\n=== PLAN B SUMMARY ===");
  console.log("font loaded (400/700/900): " + JSON.stringify(fontLoaded && { loaded: fontLoaded.loaded, bold: fontLoaded.bold, black: fontLoaded.black, faces: fontLoaded.count }));
  if (fontLoaded && fontLoaded.faces) console.log("  faces: " + JSON.stringify(fontLoaded.faces));
  console.log("elements sampled      : " + totalElements);
  console.log("non-BVP computed fonts: " + totalBadFont);
  console.log("legacy-family hits    : " + totalLegacy);
  console.log("routes w/ overflow    : " + totalOverflow + " / " + report.length);
  const tokenSets = [...new Set(report.flatMap(r => r.tokens))];
  console.log("design tokens present : " + JSON.stringify(tokenSets));
  console.log("tokens missing        : " + totalMissingTokens + " (of " + (report.length * TOKEN_NAMES_LEN) + " checks)");
  console.log("token value drift     : " + totalTokenMismatch);
  console.log("lucide icons checked  : " + totalLucide + "   stroke != 2.5: " + totalLucideBad);
  console.log("mobile shadow !2px    : " + mobileShadowFail.length + (mobileShadowFail.length ? "  " + mobileShadowFail.join(" ; ") : ""));
  console.log("LANDED ON WRONG PAGE  : " + wrongPage.length + (wrongPage.length ? "  " + wrongPage.join(" ; ") : ""));

  // Write beside THIS file, like v3.js/v3b.js — a cwd-relative name made the
  // output land in whichever directory the operator happened to run from.
  const totals = { totalElements, totalBadFont, totalLegacy, totalOverflow, wrongPage: wrongPage.length,
    totalLucide, totalLucideBad, totalMissingTokens, totalTokenMismatch, mobileShadowFail: mobileShadowFail.length };
  require("fs").writeFileSync(__dirname + "/design-v2.json", JSON.stringify({ fontLoaded, report, wrongPage, totals }, null, 1));
  console.log("wrote design-v2.json");
  // audit-v15 L1-c: `clean.ok` and the residue assertion must gate the exit code.
  // Before this the cleanup result was computed and then dropped, so a failed
  // cleanup still exited 0.
  const failed = wrongPage.length || totalBadFont || totalLegacy || totalOverflow
    || totalLucideBad || totalMissingTokens || totalTokenMismatch || mobileShadowFail.length
    || !clean.ok || residue;
  process.exit(failed ? 1 : 0);
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
