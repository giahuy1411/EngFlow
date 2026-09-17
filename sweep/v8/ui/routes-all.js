/**
 * routes-all.js — Task 10 evidence: EVERY route in evidence/route-inventory.json
 * exercised in real Chromium at desktop + mobile, with console/API capture.
 *
 * The previous ui/routes.js only covered a hand-picked subset, which is how a
 * route can be "in the inventory" yet never actually opened. This script reads
 * the generated inventory so the two can never drift apart: if a route is added
 * to the router, it appears here automatically.
 *
 * Per route it records: mount status, console errors, unhandled rejections,
 * API responses >= 400, horizontal overflow (raw numbers), text length, and the
 * computed font family set. Admin routes are swept with an admin token, the
 * premium-gated ones with a premium-capable account, and everything is swept
 * again anonymously to confirm the guard actually redirects.
 */
const H = require("./lib.js");
const fs = require("fs");
const path = require("path");

const INV = JSON.parse(fs.readFileSync(
  path.join(__dirname, "..", "..", "..", ".specify", "specs", "audit-v8-full",
    "evidence", "route-inventory.json"), "utf8"));

const VIEWPORTS = [
  { name: "desktop-1440", w: 1440, h: 900 },
  { name: "mobile-360", w: 360, h: 812 },
];

// Real ids verified against the live DB on 2026-09-16 (NOT guessed):
//   lessons.lesson_id=445 exists; decks.deck_id=10006 (Oxford 3000, public);
//   speaking_prompts.id=50007 (published); video_lessons.id=1..5 (published).
// An earlier revision substituted lesson id 445 into EVERY ':id', which made
// /decks/445, /speaking/445 and /videos/445 return legitimate 404s that looked
// like 20 app defects. Each route family must get its own valid id.
function concrete(p) {
  if (p.includes(":pathMatch")) return "/definitely-not-a-real-route-xyz";
  let out = p;
  if (out.startsWith("/decks/")) out = out.replace(":id", "10006");
  else if (out.startsWith("/speaking/")) out = out.replace(":id", "50007");
  else if (out.startsWith("/videos/")) out = out.replace(":id", "1");
  else if (out.startsWith("/admin/")) out = out.replace(":id", "445");
  else out = out.replace(":id", "445"); // /lessons/:id
  return out;
}

const results = [];
let totalErr = 0, totalApi400 = 0, totalOverflow = 0, totalNotMounted = 0;
const badLanding = [];

(async () => {
  // loginFull + full-session seed, NOT login + setItem("token").
  //
  // `store/modules/auth.js` derives `isAdmin` from `localStorage.user`, and
  // nothing wrote that key. This walk happens to survive it — the app hydrates
  // `user` from /api/auth/me during earlier routes, and an A/B in
  // `p20_routes_all_old_seed_ab.js` showed the old seed produced identical admin
  // results (0/9 bounced). So this change is hardening, not a bug fix: it makes
  // the sweep correct by construction instead of correct by accident of route
  // order, and it removes the dependency on a hydration side effect. The
  // fresh-context harnesses (p17 screenshots) were NOT so lucky.
  let adminS = await H.loginFull("admin@gmail.com", "123456");
  let userS = await H.loginFull("user@gmail.com", "123456");
  const browser = await H.pw.chromium.launch({ headless: true });

  for (const vp of VIEWPORTS) {
    for (const asRole of ["admin", "user", "anon"]) {
      const ctx = await browser.newContext({ viewport: { width: vp.w, height: vp.h } });
      const page = await ctx.newPage();

      await page.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
      if (asRole === "anon") {
        await page.evaluate(() => { localStorage.removeItem("token"); localStorage.removeItem("user"); });
      } else {
        const s = asRole === "admin" ? adminS : userS;
        await page.evaluate((x) => {
          localStorage.setItem("token", x.token);
          localStorage.setItem("user", JSON.stringify(x.user));
        }, { token: s.token, user: H.mapUser(s.user) });
      }

      let visited = 0;
      for (const r of INV.routes) {
        if (r.guard === "admin-redirect") continue; // covered by /admin/dashboard
        const url = concrete(r.path);

        // AGENTS.md: the JWT TTL is 900s and the global bucket is 100/min/IP.
        // A sweep this long exceeds both, and the resulting 429 on /api/auth/me
        // plus the expiry-driven logout() produced 3 phantom 401/429 "defects"
        // in an earlier revision. Re-seed the session and clear the buckets
        // periodically so the sweep measures the APP, not its own footprint.
        if (visited > 0 && visited % 8 === 0) {
          if (asRole === "admin") {
            adminS = await H.loginFull("admin@gmail.com", "123456");
            await page.evaluate((x) => {
              localStorage.setItem("token", x.token);
              localStorage.setItem("user", JSON.stringify(x.user));
            }, { token: adminS.token, user: H.mapUser(adminS.user) });
          } else if (asRole === "user") {
            userS = await H.loginFull("user@gmail.com", "123456");
            await page.evaluate((x) => {
              localStorage.setItem("token", x.token);
              localStorage.setItem("user", JSON.stringify(x.user));
            }, { token: userS.token, user: H.mapUser(userS.user) });
          }
          H.flushLimits(true);
          await H.sleep(400);
        }
        visited++;
        const consoleErrs = [], apiErrs = [], rejects = [];
        const onConsole = m => {
          if (m.type() === "error") {
            const t = m.text();
            // Classified as NOT app defects, each verified individually:
            //  - favicon / youtube / googlevideo: third-party + referrer noise
            //  - compute-pressure: a Permissions-Policy informational notice the
            //    browser emits for an embedded YouTube iframe, not our code
            //  - ERR_BLOCKED_BY_RESPONSE: YouTube embed referrer block, a known
            //    false negative documented in AGENTS.md
            if (/favicon|ERR_BLOCKED_BY_RESPONSE|youtube|googlevideo|compute-pressure/i.test(t)) return;
            consoleErrs.push(t.slice(0, 160));
          }
        };
        const onPageErr = e => rejects.push(String(e.message).slice(0, 160));
        const onResp = res => {
          const u = res.url();
          if (!u.startsWith(H.API)) return;
          if (res.status() >= 400) apiErrs.push(res.status() + " " + u.replace(H.API, ""));
        };
        page.on("console", onConsole);
        page.on("pageerror", onPageErr);
        page.on("response", onResp);

        let nav = "ok";
        try {
          await page.goto(H.APP + url, { waitUntil: "domcontentloaded", timeout: 30000 });
          await page.waitForTimeout(1500);
        } catch (e) { nav = "NAV-ERR " + e.message.slice(0, 80); }

        page.off("console", onConsole);
        page.off("pageerror", onPageErr);
        page.off("response", onResp);

        const info = await page.evaluate(() => {
          const de = document.documentElement;
          const app = document.querySelector("#app");
          const fams = new Set();
          document.querySelectorAll("h1,h2,h3,p,a,button,span,label,li,td,th").forEach(e => {
            if (!(e.textContent || "").trim()) return;
            const cs = getComputedStyle(e);
            if (cs.display === "none") return;
            fams.add(cs.fontFamily.split(",")[0].replace(/["']/g, "").trim());
          });
          return {
            url: location.pathname,
            mounted: !!app && app.children.length > 0,
            textLen: (document.body.innerText || "").trim().length,
            overflow: de.scrollWidth - de.clientWidth,
            scrollW: de.scrollWidth, clientW: de.clientWidth,
            fonts: [...fams],
            h1: (document.querySelector("h1") || {}).innerText || null,
          };
        }).catch(e => ({ err: e.message }));

        const badFonts = (info.fonts || []).filter(f => f && f !== "Be Vietnam Pro" && f !== "system-ui" && f !== "sans-serif");
        const overflow = (info.overflow || 0) > 16;
        if (consoleErrs.length) totalErr += consoleErrs.length;
        if (apiErrs.length) totalApi400 += apiErrs.length;
        if (overflow) totalOverflow++;
        if (!info.mounted) totalNotMounted++;

        // Where did we actually END UP? Without this the sweep cannot tell
        // "rendered the route" from "was bounced to a different page", which is
        // exactly how 9 admin routes passed while showing the home page.
        const finalPath = info.url || "";
        const landedElsewhere = finalPath !== url;

        // The landing contract, per guard x role. A privileged visit must STAY;
        // an under-privileged one must be BOUNCED. Encoding both directions is
        // what makes this an assertion rather than a note.
        //
        //   guard        | anon | user | admin
        //   -------------+------+------+------
        //   public       | stay | stay | stay
        //   guestOnly    | stay | away | away   (a logged-in user leaves /login)
        //   auth         | away | stay | stay
        //   auth+premium | away | stay | stay   (the test user IS premium)
        //   admin        | away | away | stay
        //
        // The catch-all `:pathMatch(.*)*` is an unknown URL by construction, so
        // it always redirects to `/` regardless of role.
        const isCatchAll = r.path.includes(":pathMatch");
        let shouldStay;
        if (isCatchAll) shouldStay = false;
        else if (r.guard === "public") shouldStay = true;
        else if (r.guard === "guestOnly") shouldStay = asRole === "anon";
        else if (r.guard === "admin") shouldStay = asRole === "admin";
        else shouldStay = asRole !== "anon"; // auth, auth+premium

        const redirectOk = shouldStay ? !landedElsewhere : landedElsewhere;
        if (!redirectOk) {
          badLanding.push(asRole + " " + url + " -> " + finalPath
            + " [" + r.guard + "] expected " + (shouldStay ? "to stay" : "to be bounced"));
        }

        results.push({
          route: r.path, url, finalPath, landedElsewhere, redirectOk,
          guard: r.guard, role: asRole, viewport: vp.name,
          nav, mounted: !!info.mounted, textLen: info.textLen || 0,
          overflow: info.overflow, scrollW: info.scrollW, clientW: info.clientW,
          consoleErrors: consoleErrs, pageErrors: rejects, apiErrors: apiErrs,
          badFonts, h1: info.h1,
          redirected: asRole === "anon" && landedElsewhere,
          shouldStay, redirectOk,
        });

        const flag = (consoleErrs.length || rejects.length || apiErrs.length || overflow || !info.mounted || !redirectOk) ? "  <-- ISSUE" : "";
        console.log((vp.name + " " + asRole).padEnd(24) + url.padEnd(34)
          + " mounted=" + (info.mounted ? "Y" : "N")
          + " err=" + consoleErrs.length + " page=" + rejects.length
          + " api4xx=" + apiErrs.length + " ovf=" + (info.overflow || 0)
          + (landedElsewhere ? " ->" + finalPath : "") + flag);
      }
      await ctx.close();
    }
  }
  await browser.close();

  console.log("\n=== ROUTE SWEEP TOTALS ===");
  console.log("visits            : " + results.length + " (" + INV.totalRoutes + " routes x " + VIEWPORTS.length + " viewports x 3 roles)");
  console.log("console errors    : " + totalErr);
  console.log("API >=400         : " + totalApi400);
  console.log("overflow routes   : " + totalOverflow);
  console.log("not mounted       : " + totalNotMounted);
  const badFontRoutes = results.filter(r => r.badFonts.length);
  console.log("routes w/ bad font: " + badFontRoutes.length);
  const anonGuarded = results.filter(r => r.role === "anon" && r.guard !== "public" && r.guard !== "guestOnly");
  const notRedirected = anonGuarded.filter(r => !r.redirected);
  console.log("anon on guarded   : " + anonGuarded.length + " (not redirected: " + notRedirected.length + ")");
  if (notRedirected.length) {
    console.log("  NOT REDIRECTED: " + JSON.stringify(notRedirected.map(r => r.url + " [" + r.guard + "]")));
  }
  // The assertion that would have caught the token-only seed: a privileged
  // visit must actually stay on the route it asked for.
  console.log("wrong landing     : " + badLanding.length);
  for (const b of badLanding) console.log("   " + b);

  // Per-role rendering proof: an admin route must show a real admin page, not a
  // healthy-looking home page. textLen alone cannot tell them apart.
  const adminRows = results.filter(r => r.role === "admin" && r.guard === "admin");
  const adminReal = adminRows.filter(r => !r.landedElsewhere && r.mounted && r.textLen > 100);
  console.log("admin visits rendering a real admin page: " + adminReal.length + "/" + adminRows.length);

  // Self-clean. This sweep walks /premium/checkout, whose component mints a real
  // payment_transactions row on mount, so the sweep MUST undo its own writes in
  // the same run. Measured 2026-09-16: three runs took parity 126 -> 134.
  const clean = H.cleanupAuditPayments(126);
  console.log("DB parity after cleanup: " + H.dbParity() + "   (baseline 1471|43737|76|127|28|15|4|126|14|5)");

  fs.writeFileSync("routes-all.json", JSON.stringify({ inventory: INV.totalRoutes, results, badLanding, cleanup: clean, totals: { totalErr, totalApi400, totalOverflow, totalNotMounted, badLanding: badLanding.length } }, null, 1));
  console.log("wrote routes-all.json");
  process.exit((badLanding.length === 0 && clean.ok) ? 0 : 1);
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
