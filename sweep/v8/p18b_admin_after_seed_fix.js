/**
 * p18b_admin_after_seed_fix.js — re-run the redirect audit with token+user
 * seeded, to prove the 9 admin redirects were a harness artifact, not an app bug.
 *
 * If seeding `localStorage.user` makes /admin/* render, then:
 *   - the app's admin guard is CORRECT (it refused an unidentified user), and
 *   - every earlier "0 errors on 39 routes" claim was invalid for admin routes.
 *
 * If the redirects persist, it is a real defect and must be reported as one.
 */
const path = require("path");
const fs = require("fs");
const { pw, APP, loginFull, flushLimits } = require("./ui/lib.js");

const CASES = [
  ["/admin/dashboard",            "admin"],
  ["/admin/lessons",              "admin"],
  ["/admin/exercises",            "admin"],
  ["/admin/users",                "admin"],
  ["/admin/videos",               "admin"],
  ["/admin/video-attempts",       "admin"],
  ["/admin/speaking-prompts",     "admin"],
  ["/admin/speaking-submissions", "admin"],
  ["/admin/445/build",            "admin"],
  ["/videos",                     "user"],
  ["/videos/1",                   "user"],
  ["/speaking",                   "user"],
  ["/speaking/history",           "user"],
  ["/speaking/50007",             "user"],
  ["/premium",                    "user"],
  ["/profile",                    "user"],
];

(async () => {
  const userS = await loginFull("user@gmail.com", "123456");
  const adminS = await loginFull("admin@gmail.com", "123456");
  console.log("admin session isAdmin=" + adminS.user.isAdmin
    + " hasPremiumAccess=" + adminS.user.hasPremiumAccess);
  console.log("user  session isAdmin=" + userS.user.isAdmin
    + " hasPremiumAccess=" + userS.user.hasPremiumAccess);

  const browser = await pw.chromium.launch({ headless: true });
  const rows = [];

  for (const [route, role] of CASES) {
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await ctx.newPage();
    const s = role === "admin" ? adminS : userS;

    // THE FIX: seed token AND user, on the app origin.
    await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
    await page.evaluate((x) => {
      localStorage.setItem("token", x.token);
      localStorage.setItem("user", JSON.stringify(x.user));
    }, { token: s.token, user: {
      id: s.user.id, username: s.user.username, email: s.user.email,
      fullName: s.user.fullName, avatarUrl: s.user.avatarUrl,
      isAdmin: s.user.isAdmin, isPremium: s.user.isPremium === true,
      premiumExpiry: s.user.premiumExpiry, currentLevel: s.user.currentLevel,
      totalPoints: s.user.totalPoints, currentStreak: s.user.currentStreak,
      hasPremiumAccess: s.user.hasPremiumAccess === true,
      aiGenerationCount: s.user.aiGenerationCount ?? 0,
      aiGenerationsRemainingToday: s.user.aiGenerationsRemainingToday ?? null,
    } });

    await page.goto(APP + route, { waitUntil: "domcontentloaded", timeout: 30000 }).catch(() => null);
    await page.waitForTimeout(2500);

    const finalUrl = page.url().replace(APP, "");
    const info = await page.evaluate(() => ({
      h1: (document.querySelector("h1") || {}).innerText || null,
      textLen: (document.body.innerText || "").trim().length,
      tables: document.querySelectorAll("table").length,
      rows: document.querySelectorAll("tbody tr").length,
      hasSidebar: !!document.querySelector("aside"),
    })).catch(() => ({}));

    const ok = finalUrl === route;
    rows.push({ route, role, finalUrl, ok, ...info });
    console.log((ok ? "OK       " : "REDIRECT ") + route.padEnd(30)
      + " -> " + finalUrl.padEnd(26)
      + " h1=" + JSON.stringify((info.h1 || "").slice(0, 26)).padEnd(30)
      + " text=" + String(info.textLen).padStart(5)
      + " tbl=" + info.tables + " tr=" + info.rows);
    await ctx.close();
    flushLimits();
  }

  await browser.close();
  const redir = rows.filter(r => !r.ok);
  console.log("\n=== AFTER SEEDING token+user ===");
  console.log("cases           : " + rows.length);
  console.log("still redirected: " + redir.length + (redir.length ? "  " + redir.map(r => r.route).join(", ") : ""));
  const adminOk = rows.filter(r => r.role === "admin" && r.ok).length;
  console.log("admin routes rendering: " + adminOk + "/9");
  console.log(redir.length === 0
    ? "VERDICT: the redirects were a HARNESS ARTIFACT (token-only seed). App guard is correct."
    : "VERDICT: real redirects remain -- investigate as app defects.");

  fs.writeFileSync(path.join(__dirname, "p18b_admin_after_seed_fix.json"),
    JSON.stringify({ rows, redirected: redir.length }, null, 1));
  console.log("wrote p18b_admin_after_seed_fix.json");
  process.exit(0);
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
