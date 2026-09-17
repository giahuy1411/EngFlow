/**
 * p20_routes_all_old_seed_ab.js — A/B the OLD token-only seed against the NEW
 * full-session seed, walking the SAME order routes-all.js uses.
 *
 * WHY: p19 showed the app self-hydrates `user` from /api/auth/me during earlier
 * navigations, so a long sequential walk may render admin routes correctly even
 * with a token-only seed. If that holds, routes-all.js's admin half was NOT
 * invalid, and only the fresh-context-per-case probes (p17 screenshots, p18
 * redirect audit) were. Reporting "the whole 152-visit sweep was invalid" would
 * overstate the damage; reporting "nothing was invalid" would hide a real one.
 *
 * This probe answers it with a measurement instead of an argument.
 */
const fs = require("fs");
const path = require("path");
const { pw, APP, login, loginFull, mapUser, flushLimits } = require("./ui/lib.js");

const INV = JSON.parse(fs.readFileSync(
  path.join(__dirname, "..", "..", ".specify", "specs", "audit-v8-full",
    "evidence", "route-inventory.json"), "utf8"));

function concrete(p) {
  if (p.includes(":pathMatch")) return "/definitely-not-a-real-route-xyz";
  let out = p;
  if (out.startsWith("/decks/")) out = out.replace(":id", "10006");
  else if (out.startsWith("/speaking/")) out = out.replace(":id", "50007");
  else if (out.startsWith("/videos/")) out = out.replace(":id", "1");
  else out = out.replace(":id", "445");
  return out;
}

async function walk(mode) {
  const browser = await pw.chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();

  const tokenOnly = mode === "old";
  const t = await login("admin@gmail.com", "123456");
  const s = tokenOnly ? null : await loginFull("admin@gmail.com", "123456");

  await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  if (tokenOnly) {
    await page.evaluate(x => localStorage.setItem("token", x), t);
  } else {
    await page.evaluate(x => {
      localStorage.setItem("token", x.token);
      localStorage.setItem("user", JSON.stringify(x.user));
    }, { token: s.token, user: mapUser(s.user) });
  }

  const adminRows = [];
  let n = 0;
  for (const r of INV.routes) {
    if (r.guard === "admin-redirect") continue;
    const url = concrete(r.path);
    n++;
    await page.goto(APP + url, { waitUntil: "domcontentloaded", timeout: 30000 }).catch(() => {});
    await page.waitForTimeout(700); // shorter than the real sweep: we only need landing + text
    if (r.guard === "admin") {
      const info = await page.evaluate(() => ({
        path: location.pathname,
        h1: (document.querySelector("h1") || {}).innerText || null,
        textLen: (document.body.innerText || "").trim().length,
      }));
      adminRows.push({ url, visitIndex: n, ...info, landed: info.path === url });
    }
    if (n % 8 === 0) flushLimits(true);
  }
  await ctx.close();
  await browser.close();
  return adminRows;
}

(async () => {
  const oldRows = await walk("old");
  const newRows = await walk("new");

  console.log("=== ADMIN ROUTES, OLD token-only seed (walk order) ===");
  for (const r of oldRows) {
    console.log("  #" + String(r.visitIndex).padStart(2) + " " + r.url.padEnd(30)
      + " -> " + r.path.padEnd(28) + " text=" + String(r.textLen).padStart(5)
      + (r.landed ? "  OK" : "  BOUNCED"));
  }
  const oldBounced = oldRows.filter(r => !r.landed);
  console.log("  bounced: " + oldBounced.length + "/" + oldRows.length);

  console.log("\n=== ADMIN ROUTES, NEW full-session seed ===");
  for (const r of newRows) {
    console.log("  #" + String(r.visitIndex).padStart(2) + " " + r.url.padEnd(30)
      + " -> " + r.path.padEnd(28) + " text=" + String(r.textLen).padStart(5)
      + (r.landed ? "  OK" : "  BOUNCED"));
  }
  const newBounced = newRows.filter(r => !r.landed);
  console.log("  bounced: " + newBounced.length + "/" + newRows.length);

  console.log("\n=== VERDICT ===");
  if (oldBounced.length === 0) {
    console.log("routes-all.js was NOT affected by the token-only seed: the app");
    console.log("self-hydrates `user` via /api/auth/me during the walk, so admin");
    console.log("routes reached late in the sequence rendered correctly.");
    console.log("Affected harnesses are the FRESH-CONTEXT ones: p17 (screenshots)");
    console.log("and p18 (redirect audit), which navigate to an admin route first.");
  } else {
    console.log("routes-all.js WAS affected: " + oldBounced.length + " admin visits bounced.");
  }

  fs.writeFileSync(path.join(__dirname, "p20_ab.json"),
    JSON.stringify({ oldRows, newRows, oldBounced: oldBounced.length, newBounced: newBounced.length }, null, 1));
  console.log("wrote p20_ab.json");
  process.exit(0);
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
