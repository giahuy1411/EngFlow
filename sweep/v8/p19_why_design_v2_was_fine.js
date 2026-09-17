/**
 * p19_why_design_v2_was_fine.js — resolve a contradiction before reporting it.
 *
 * routes-all.js (fresh context per role, 38 routes walked in order) recorded the
 * 9 admin routes as textLen=1524, i.e. the HOME page.
 * design-v2.js (same token-only seed) sampled 68/196/214 elements on those same
 * admin routes — the real admin pages.
 *
 * Both cannot be explained by "token-only seed" alone. Hypothesis: the auth
 * store hydrates `user` from an API call during navigation, so an admin route
 * reached LATE in a long walk renders correctly, while one reached FIRST in a
 * cold context gets bounced.
 *
 * This matters for the report: it decides whether design-v2's 60 combos were
 * valid or whether 15 of them measured the wrong page.
 */
const { pw, APP, login, flushLimits } = require("./ui/lib.js");

async function run(label, steps) {
  const browser = await pw.chromium.launch({ headless: true });
  const token = await login("admin@gmail.com", "123456");
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  await page.evaluate(t => localStorage.setItem("token", t), token); // token ONLY, the old way
  await page.waitForTimeout(600);

  const trace = [];
  for (const s of steps) {
    await page.goto(APP + s, { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(1500);
    const r = await page.evaluate(() => ({
      path: location.pathname,
      h1: (document.querySelector("h1") || {}).innerText || null,
      sampled: (() => {
        let n = 0;
        document.querySelectorAll("h1,h2,h3,h4,h5,p,a,button,span,label,li,td,th,input,textarea,select,strong,em,small,code")
          .forEach(e => {
            const hasText = (e.textContent || "").trim().length > 0 || /INPUT|TEXTAREA|SELECT/.test(e.tagName);
            if (!hasText) return;
            const cs = getComputedStyle(e);
            if (cs.display === "none" || cs.visibility === "hidden") return;
            n++;
          });
        return n;
      })(),
      userInLs: !!localStorage.getItem("user"),
    }));
    trace.push(s + " -> " + r.path + "  h1=" + JSON.stringify((r.h1 || "").slice(0, 22)) + " sampled=" + r.sampled + " userSeeded=" + r.userInLs);
  }
  await ctx.close();
  await browser.close();
  console.log("--- " + label);
  trace.forEach(t => console.log("    " + t));
  return trace;
}

(async () => {
  // A: cold context, admin route FIRST (what p18 did)
  await run("A: /admin/dashboard first, token only", ["/admin/dashboard"]);
  // B: warm the app first with public routes, then the admin route (design-v2's order)
  await run("B: public routes first, then /admin/dashboard", ["/", "/lessons", "/lessons/445", "/admin/dashboard"]);
  // C: exactly design-v2's route order, ending at the admin routes
  await run("C: design-v2 order", ["/", "/lessons", "/lessons/445", "/decks", "/speaking", "/premium", "/login", "/leaderboard", "/videos", "/admin/dashboard", "/admin/lessons", "/admin/exercises"]);
  process.exit(0);
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
