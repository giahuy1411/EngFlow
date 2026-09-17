/**
 * p18_redirect_audit.js — why do 15 routes render someone else's page?
 *
 * p17_screenshots.js wrote 36/36 shots and reported PASS, but the numbers gave
 * it away: 9 admin routes all had textLen=1524 (home page is 1513) and 6
 * premium routes all had textLen=565. A screenshot harness that never records
 * page.url() cannot tell "rendered this route" from "redirected away from it".
 *
 * This probe records the FINAL url after each navigation and the h1 actually
 * rendered, so a redirect is visible instead of being mistaken for content.
 *
 * Read-only: navigation only, no form submits, never mounts /premium/checkout.
 */
const path = require("path");
const fs = require("fs");
const { pw, APP, login, flushLimits } = require("./ui/lib.js");

// route, role, and what we EXPECT the h1/url to be if the route really rendered.
const CASES = [
  ["/admin/dashboard",           "admin", "/admin/dashboard"],
  ["/admin/lessons",             "admin", "/admin/lessons"],
  ["/admin/exercises",           "admin", "/admin/exercises"],
  ["/admin/users",               "admin", "/admin/users"],
  ["/admin/videos",              "admin", "/admin/videos"],
  ["/admin/video-attempts",      "admin", "/admin/video-attempts"],
  ["/admin/speaking-prompts",    "admin", "/admin/speaking-prompts"],
  ["/admin/speaking-submissions","admin", "/admin/speaking-submissions"],
  ["/admin/445/build",           "admin", "/admin/445/build"],
  ["/videos",                    "user",  "/videos"],
  ["/videos/1",                  "user",  "/videos/1"],
  ["/speaking",                  "user",  "/speaking"],
  ["/speaking/history",          "user",  "/speaking/history"],
  ["/speaking/50007",            "user",  "/speaking/50007"],
  ["/premium",                   "user",  "/premium"],
  ["/profile",                   "user",  "/profile"],
  ["/lessons",                   "user",  "/lessons"],
  ["/",                          "user",  "/"],
];

(async () => {
  const userT = await login("user@gmail.com", "123456");
  const adminT = await login("admin@gmail.com", "123456");
  const browser = await pw.chromium.launch({ headless: true });

  const rows = [];
  for (const [route, role, expect] of CASES) {
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await ctx.newPage();
    const token = role === "admin" ? adminT : userT;

    // Seed the token first, then navigate to the target route.
    await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
    await page.evaluate(t => localStorage.setItem("token", t), token);

    const resp = await page.goto(APP + route, { waitUntil: "domcontentloaded", timeout: 30000 }).catch(() => null);
    await page.waitForTimeout(2500);

    const finalUrl = page.url().replace(APP, "");
    const info = await page.evaluate(() => ({
      h1: (document.querySelector("h1") || {}).innerText || null,
      textLen: (document.body.innerText || "").trim().length,
      // Count real data rows / cards: a page that redirected shows the home page's
      // structure instead of a table.
      tables: document.querySelectorAll("table").length,
      rows: document.querySelectorAll("tbody tr").length,
      cards: document.querySelectorAll("[class*=card],[class*=rounded-2xl]").length,
      hasSidebar: !!document.querySelector("aside,[class*=sidebar]"),
    })).catch(e => ({ err: e.message }));

    const redirected = finalUrl !== expect;
    rows.push({
      route, role, expect, finalUrl, redirected,
      httpStatus: resp ? resp.status() : null,
      h1: info.h1, textLen: info.textLen,
      tables: info.tables, rows: info.rows, cards: info.cards, hasSidebar: info.hasSidebar,
    });

    console.log(
      (redirected ? "REDIRECT " : "OK       ") + route.padEnd(30)
      + " -> " + finalUrl.padEnd(30)
      + " h1=" + JSON.stringify((info.h1 || "").slice(0, 30)).padEnd(34)
      + " text=" + String(info.textLen).padStart(5)
      + " tbl=" + info.tables + " rows=" + info.rows
    );
    await ctx.close();
    flushLimits();
  }

  await browser.close();

  const redir = rows.filter(r => r.redirected);
  console.log("\n=== REDIRECT AUDIT ===");
  console.log("cases            : " + rows.length);
  console.log("redirected away  : " + redir.length);
  for (const r of redir) console.log("   " + r.route + "  ->  " + r.finalUrl + "   (h1=" + JSON.stringify((r.h1 || "").slice(0, 40)) + ")");

  fs.writeFileSync(path.join(__dirname, "p18_redirect_audit.json"), JSON.stringify({ rows, redirected: redir.length }, null, 1));
  console.log("wrote p18_redirect_audit.json");
  process.exit(0);
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
