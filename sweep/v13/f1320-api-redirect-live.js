/**
 * audit-v13 Phase 1 — live verification that the auth bounce KEEPS the destination.
 *
 * Reproduces the real scenario: a logged-in user sitting on a deep page whose token
 * expires mid-session. The request interceptor must bounce them to
 * /login?redirect=<that page>, and Login must return them there after re-login.
 *
 * Two things this proves that the unit test cannot:
 *   1. vue-router really resolves { path:'/login', query:{redirect} } into a URL with
 *      the query intact (no silent drop by the guard).
 *   2. The full round trip works in a browser (interceptor -> guard -> Login -> back).
 */
const path = require("path");
const H = require(path.join(__dirname, "..", "v8", "ui", "lib.js"));
const { pw, APP, loginFull, seedAuth, flushLimits } = H;

const OUT = [];
function check(name, ok, detail) {
  OUT.push({ name, ok: !!ok, detail: detail || "" });
  console.log(`${ok ? "PASS" : "FAIL"}  ${name}${detail ? "  -- " + detail : ""}`);
}

/** Build a JWT with a given exp, so the client-side expiry check fires deterministically. */
function jwtWithExp(expSeconds) {
  const b64 = (o) => Buffer.from(JSON.stringify(o)).toString("base64").replace(/=+$/, "");
  return `${b64({ alg: "HS512" })}.${b64({ sub: "2", exp: expSeconds })}.sig`;
}

(async () => {
  // The globally-installed playwright-core expects a browser build that is not on this
  // machine (1243); 1237 is. Point at the real binary instead of re-downloading.
  const CHROME = "C:/Users/ASUS/AppData/Local/ms-playwright/chromium-1237/chrome-win64/chrome.exe";
  const fs = require("fs");
  const launchOpts = fs.existsSync(CHROME) ? { headless: true, executablePath: CHROME } : { headless: true };
  const browser = await pw.chromium.launch(launchOpts);
  try {
    const session = await loginFull("user@gmail.com", "123456");
    check("login user@gmail.com", !!session.token, session.token ? "token issued" : "no token");

    // ---------- Case 1: deep page + expired token -> /login?redirect=<deep page> ----------
    {
      const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
      const page = await ctx.newPage();
      await flushLimits(true);
      await seedAuth(page, session);
      // Land on a real protected deep page, then poison the token with a past exp.
      await page.goto(APP + "/decks/10006", { waitUntil: "domcontentloaded" });
      await page.evaluate((t) => localStorage.setItem("token", t), jwtWithExp(Math.floor(Date.now() / 1000) - 60));
      // Trigger any API call from that page (a reload re-runs the page's fetches).
      await page.goto(APP + "/decks/10006", { waitUntil: "networkidle" }).catch(() => {});
      await page.waitForTimeout(1200);
      const url = new URL(page.url());
      const redirect = url.searchParams.get("redirect");
      check(
        "expired token on /decks/10006 -> /login with redirect=/decks/10006",
        url.pathname === "/login" && redirect === "/decks/10006",
        `final=${page.url()}`
      );
      await ctx.close();
    }

    // ---------- Case 2: full round trip — login returns to the deep page ----------
    {
      const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
      const page = await ctx.newPage();
      await flushLimits(true);
      await page.goto(APP + "/login?redirect=%2Fdecks%2F10006", { waitUntil: "domcontentloaded" });
      await page.fill('input[type="email"]', "user@gmail.com").catch(() => {});
      await page.fill('input[type="password"]', "123456").catch(() => {});
      await page.click('button[type="submit"]').catch(() => {});
      await page.waitForTimeout(2500);
      const u = new URL(page.url());
      check(
        "login with ?redirect returns user to /decks/10006",
        u.pathname === "/decks/10006",
        `final=${page.url()}`
      );
      await ctx.close();
    }

    // ---------- Case 3: open-redirect still blocked end to end ----------
    {
      const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
      const page = await ctx.newPage();
      await flushLimits(true);
      await page.goto(APP + "/login?redirect=%2F%2Fevil.com", { waitUntil: "domcontentloaded" });
      await page.fill('input[type="email"]', "user@gmail.com").catch(() => {});
      await page.fill('input[type="password"]', "123456").catch(() => {});
      await page.click('button[type="submit"]').catch(() => {});
      await page.waitForTimeout(2500);
      const u = new URL(page.url());
      check(
        "?redirect=//evil.com does NOT leave the site",
        u.hostname === "localhost" && u.pathname === "/lessons",
        `final=${page.url()}`
      );
      await ctx.close();
    }
  } finally {
    await browser.close();
  }

  const failed = OUT.filter((r) => !r.ok);
  console.log(`\n=== Phase 1 live: ${OUT.length - failed.length}/${OUT.length} PASS ===`);
  require("fs").writeFileSync(
    path.join(__dirname, "..", "..", ".specify", "specs", "audit-v13-full", "evidence", "f13-20-api-redirect-live.json"),
    JSON.stringify({ ranAt: new Date().toISOString(), results: OUT }, null, 2)
  );
  process.exit(failed.length ? 1 : 0);
})();
