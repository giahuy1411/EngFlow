/**
 * p17_screenshots.js — capture the before/after visual evidence PLAN.md Phase 5
 * asks for ("chụp screenshot trước/sau các flow quan trọng").
 *
 * WHY THIS EXISTS
 * ---------------
 * The audit produced strong *numeric* evidence (0 console errors, 0 overflow,
 * 0 non-BVP fonts across 212 visits) but **zero screenshots**. Numbers prove
 * absence of defects; they do not show a human what the flow looks like. PLAN.md
 * Phase 5 explicitly requires screenshots for the mandatory flows, and the
 * required-evidence layout lists "screenshots" alongside logs and JSON.
 *
 * WHAT IT DOES
 * ------------
 * Drives real Chromium through the mandatory flows from PLAN.md Phase 5 and
 * writes PNGs into `.specify/specs/audit-v8-full/evidence/screens/`.
 *
 * READ-ONLY DISCIPLINE
 * --------------------
 * Every step is a read or a *render*. It never submits a grade, never creates a
 * deck, never saves a vocabulary word and never mounts /premium, because that
 * last one is the documented trap that creates real `payment_transactions` rows.
 * Premium is captured from the /premium pricing view only, which does not call
 * create-order. Parity is re-checked at the end to prove nothing was written.
 *
 * Run:  cmd /c "set NODE_PATH=%APPDATA%\npm\node_modules&& node p17_screenshots.js"
 *       (from sweep/v8)
 */
const path = require("path");
const fs = require("fs");
const { pw, APP, loginFull, mapUser, flushLimits, cleanupAuditPayments, dbParity } = require("./ui/lib.js");

const OUT = path.join(__dirname, "..", "..", ".specify", "specs", "audit-v8-full", "evidence", "screens");

// Mandatory flows from PLAN.md Phase 5, as (name, route, role, viewport).
// `role: "anon"` means no token is seeded. Every path below is copied from
// `evidence/route-inventory.json` (generated from the real router) — an invented
// path would produce a catch-all 404 page and a useless screenshot.
const SHOTS = [
  ["01-home",                 "/",                          "anon",  { width: 1440, height: 900 }],
  ["02-login",                "/login",                     "anon",  { width: 1440, height: 900 }],
  ["03-register",             "/register",                  "anon",  { width: 1440, height: 900 }],
  ["04-forgot-password",      "/forgot-password",           "anon",  { width: 1440, height: 900 }],
  ["05-lessons-list",         "/lessons",                   "user",  { width: 1440, height: 900 }],
  ["06-lesson-detail",        "/lessons/445",               "user",  { width: 1440, height: 900 }],
  ["07-videos-list",          "/videos",                    "user",  { width: 1440, height: 900 }],
  ["08-video-detail",         "/videos/1",                  "user",  { width: 1440, height: 900 }],
  ["09-profile-streak",       "/profile",                   "user",  { width: 1440, height: 900 }],
  ["10-leaderboard",          "/leaderboard",               "user",  { width: 1440, height: 900 }],
  ["11-search",               "/search",                    "user",  { width: 1440, height: 900 }],
  ["12-decks",                "/decks",                     "user",  { width: 1440, height: 900 }],
  ["13-deck-detail",          "/decks/10006",               "user",  { width: 1440, height: 900 }],
  ["14-deck-flashcard",       "/decks/10006/play/flashcard","user",  { width: 1440, height: 900 }],
  ["15-deck-quiz",            "/decks/10006/play/quiz",     "user",  { width: 1440, height: 900 }],
  ["16-deck-create",          "/decks/create",              "user",  { width: 1440, height: 900 }],
  ["17-speaking",             "/speaking",                  "user",  { width: 1440, height: 900 }],
  ["18-speaking-history",     "/speaking/history",          "user",  { width: 1440, height: 900 }],
  ["19-speaking-detail",      "/speaking/50007",            "user",  { width: 1440, height: 900 }],
  ["20-ai-vocab-generator",   "/ai-vocab-generator",        "user",  { width: 1440, height: 900 }],
  ["21-premium-pricing",      "/premium",                   "user",  { width: 1440, height: 900 }],
  ["22-admin-dashboard",      "/admin/dashboard",           "admin", { width: 1440, height: 900 }],
  ["23-admin-lessons",        "/admin/lessons",             "admin", { width: 1440, height: 900 }],
  ["24-admin-exercises",      "/admin/exercises",           "admin", { width: 1440, height: 900 }],
  ["25-admin-users",          "/admin/users",               "admin", { width: 1440, height: 900 }],
  ["26-admin-videos",         "/admin/videos",              "admin", { width: 1440, height: 900 }],
  ["27-admin-video-attempts", "/admin/video-attempts",      "admin", { width: 1440, height: 900 }],
  ["28-admin-prompts",        "/admin/speaking-prompts",    "admin", { width: 1440, height: 900 }],
  ["29-admin-submissions",    "/admin/speaking-submissions","admin", { width: 1440, height: 900 }],
  ["30-admin-lesson-builder", "/admin/445/build",           "admin", { width: 1440, height: 900 }],
  // Mobile: the viewport where the design system's responsive rules actually bite.
  ["31-mobile-home",          "/",                          "anon",  { width: 360, height: 812 }],
  ["32-mobile-lessons",       "/lessons",                   "user",  { width: 360, height: 812 }],
  ["33-mobile-lesson-detail", "/lessons/445",               "user",  { width: 360, height: 812 }],
  ["34-mobile-decks",         "/decks",                     "user",  { width: 360, height: 812 }],
  ["35-mobile-speaking",      "/speaking",                  "user",  { width: 360, height: 812 }],
  ["36-mobile-admin",         "/admin/dashboard",           "admin", { width: 360, height: 812 }],
];

(async () => {
  fs.mkdirSync(OUT, { recursive: true });

  const userS = await loginFull("user@gmail.com", "123456");
  const adminS = await loginFull("admin@gmail.com", "123456");
  console.log("sessions: user isAdmin=" + userS.user.isAdmin + " / admin isAdmin=" + adminS.user.isAdmin);
  if (!userS.token || !adminS.token) { console.log("FATAL: could not log in"); process.exit(1); }
  if (!adminS.user.isAdmin) { console.log("FATAL: admin account lacks isAdmin -- admin shots would be home-page shots"); process.exit(1); }

  const browser = await pw.chromium.launch({
    headless: true,
    args: ["--use-fake-ui-for-media-stream", "--use-fake-device-for-media-stream"],
  });

  const rows = [];
  for (const [name, route, role, viewport] of SHOTS) {
    // A fresh context per shot so localStorage/cookies never leak between roles.
    const ctx = await browser.newContext({ viewport, deviceScaleFactor: 1 });
    const page = await ctx.newPage();
    const consoleErrors = [];
    page.on("console", m => { if (m.type() === "error") consoleErrors.push(m.text().slice(0, 160)); });

    let status = "ok";
    try {
      // Seed the FULL session (token + user) on the app origin. Seeding only the
      // token leaves `auth.isAdmin` undefined and every /admin/* shot silently
      // becomes a screenshot of the home page.
      if (role !== "anon") {
        const s = role === "admin" ? adminS : userS;
        await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
        await page.evaluate((x) => {
          localStorage.setItem("token", x.token);
          localStorage.setItem("user", JSON.stringify(x.user));
        }, { token: s.token, user: mapUser(s.user) });
      }
      await page.goto(APP + route, { waitUntil: "domcontentloaded", timeout: 30000 });
      await page.waitForTimeout(2200);
    } catch (e) {
      status = "NAV-ERR " + e.message.slice(0, 80);
    }

    // A screenshot of the wrong page is worse than no screenshot: it looks like
    // evidence. Record where we actually landed and refuse to call it a pass.
    const finalPath = page.url().replace(APP, "").split("?")[0];
    const landedElsewhere = finalPath !== route;

    const probe = await page.evaluate(() => {
      const app = document.querySelector("#app");
      const fams = new Set();
      [...document.querySelectorAll("h1,h2,h3,p,button,a,span")]
        .slice(0, 400)
        .forEach(el => fams.add(getComputedStyle(el).fontFamily.split(",")[0].replace(/["']/g, "")));
      return {
        mounted: !!app && app.children.length > 0,
        textLen: (document.body.innerText || "").trim().length,
        h1: (document.querySelector("h1") || {}).innerText || null,
        scrollW: document.documentElement.scrollWidth,
        clientW: document.documentElement.clientWidth,
        fonts: [...fams],
      };
    }).catch(e => ({ err: e.message }));

    const file = path.join(OUT, name + ".png");
    await page.screenshot({ path: file, fullPage: true }).catch(e => { status = "SHOT-ERR " + e.message.slice(0, 60); });

    const nonBvp = (probe.fonts || []).filter(f => !/Be Vietnam Pro/.test(f));
    const row = {
      name, route, role, viewport: viewport.width + "x" + viewport.height,
      status,
      finalPath, landedElsewhere,
      mounted: probe.mounted === true,
      textLen: probe.textLen || 0,
      h1: probe.h1,
      nonBvpFonts: nonBvp,
      overflow: probe.scrollW > probe.clientW + 2
        ? (probe.scrollW + ">" + probe.clientW) : null,
      consoleErrors,
      bytes: fs.existsSync(file) ? fs.statSync(file).size : 0,
    };
    rows.push(row);
    console.log(
      (row.bytes ? "OK  " : "MISS") + " " + name.padEnd(24)
      + " mounted=" + (row.mounted ? "Y" : "N")
      + " text=" + String(row.textLen).padStart(5)
      + " ovf=" + (row.overflow || "-")
      + " nonBVP=" + nonBvp.length
      + " err=" + consoleErrors.length
      + (landedElsewhere ? "  <-- LANDED ON " + finalPath : "")
      + " " + (row.bytes / 1024).toFixed(0) + "kB"
    );

    await ctx.close();
    flushLimits(); // a 36-shot sweep can trip the global bucket on its own
  }

  await browser.close();

  const missing = rows.filter(r => !r.bytes);
  const blank = rows.filter(r => r.mounted && r.textLen < 40);
  const notMounted = rows.filter(r => !r.mounted);
  const overflow = rows.filter(r => r.overflow);
  const badFont = rows.filter(r => r.nonBvpFonts.length);
  const errs = rows.filter(r => r.consoleErrors.length);
  const wrongPage = rows.filter(r => r.landedElsewhere);
  const totalBytes = rows.reduce((a, r) => a + r.bytes, 0);

  console.log("\n=== SCREENSHOT VERDICT ===");
  console.log("shots written        : " + (rows.length - missing.length) + "/" + rows.length);
  console.log("total size           : " + (totalBytes / 1048576).toFixed(1) + " MB");
  console.log("not mounted          : " + notMounted.length + (notMounted.length ? " " + notMounted.map(r => r.name).join(",") : ""));
  console.log("mounted but blank    : " + blank.length + (blank.length ? " " + blank.map(r => r.name + "(" + r.textLen + ")").join(",") : ""));
  console.log("LANDED ON WRONG PAGE : " + wrongPage.length + (wrongPage.length ? " " + wrongPage.map(r => r.name + "->" + r.finalPath).join(",") : ""));
  console.log("horizontal overflow  : " + overflow.length + (overflow.length ? " " + overflow.map(r => r.name).join(",") : ""));
  console.log("non-BVP fonts        : " + badFont.length + (badFont.length ? " " + badFont.map(r => r.name).join(",") : ""));
  console.log("console errors       : " + errs.length + (errs.length ? " " + errs.map(r => r.name).join(",") : ""));

  // Same discipline as every other harness: prove the DB was not touched, and
  // undo it if it was. `/premium` renders PremiumPage (no order), but the
  // redirect to /premium/checkout on some flows does mint a row, so clean
  // unconditionally rather than reason about which path was taken.
  const clean = cleanupAuditPayments(126); // audit-v11 F130: baseline informational; assertion is self-clean
  const parity = dbParity();
  console.log("DB parity after sweep: " + parity + "   (baseline 1471|43737|76|127|28|15|4|126|14|5)");

  const pass = missing.length === 0 && notMounted.length === 0 && blank.length === 0
    && overflow.length === 0 && badFont.length === 0 && errs.length === 0
    && wrongPage.length === 0 && clean.ok
    && parity === "1471|43737|76|127|28|15|4|126|14|5";

  fs.writeFileSync(path.join(__dirname, "p17_screenshots.json"),
    JSON.stringify({ out: OUT, rows, parity, cleanup: clean, pass }, null, 1));
  console.log("VERDICT: " + (pass ? "PASS - every flow rendered its own page and was captured" : "FAIL"));
  console.log("wrote p17_screenshots.json");
  process.exit(pass ? 0 : 1);
})().catch(e => { console.log("ERR", e.message, e.stack); process.exit(1); });
