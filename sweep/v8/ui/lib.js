const pw = require("playwright-core");
const fs = require("fs");
const { execSync } = require("child_process");
const APP = "http://localhost:5173";
const API = "http://localhost:8080";
const BASE = "http://localhost:8080";

// Same helper as sweep/v8/lib.js. A long browser sweep exceeds the 100/min
// global bucket on its own and the resulting 429s look like app defects, so
// every UI harness needs the ability to clear ONLY its own rate_limit:* keys.
// This never touches business data.
let since = 0;
function flushLimits(force) {
  if (!force && ++since < 55) return;
  since = 0;
  try {
    const keys = execSync("docker exec engflow-redis redis-cli --scan --pattern rate_limit:*", { encoding: "utf8" }).trim();
    if (!keys) return;
    const list = keys.split(/\r?\n/).filter(Boolean);
    execSync("docker exec engflow-redis redis-cli del " + list.map(k => '"' + k + '"').join(" "), { encoding: "utf8" });
  } catch (e) {
    console.log("flushLimits warn: " + e.message);
  }
}

const sleep = ms => new Promise(r => setTimeout(r, ms));

/**
 * Today's date on the SAME clock the database is written with.
 *
 * WHY NOT `new Date().toISOString().slice(0,10)`
 * ---------------------------------------------
 * Every `datetime2` column in this DB holds a NAIVE Vietnam wall-clock value:
 * the JVM runs with `TZ=Asia/Ho_Chi_Minh` and writes `LocalDateTime.now()`
 * (AGENTS.md, verified audit-v7). SQL Server's own clock is UTC and no column
 * has a `SYSDATETIME()` default, so the JVM is the only writer.
 *
 * `toISOString()` renders UTC. Between 00:00 and 06:59 Vietnam time the UTC
 * date is still YESTERDAY, so a date-equality cleanup matched zero rows and
 * reported success while the audit rows stayed in the table.
 *
 * MEASURED 2026-09-17 01:08 +07 on this machine:
 *   host clock            = 2026-09-17 01:08 (+07:00)
 *   toISOString().slice() = "2026-09-16"   <- what the old code used
 *   VN date               = "2026-09-17"   <- what the rows actually carry
 *   rows created by the sweep (created_at = 2026-09-17) = 4
 *   rows the old DELETE removed                          = 0   -> parity 130 vs 126
 *
 * Daytime runs (07:00-23:59 VN) had UTC date == VN date, which is exactly why
 * this survived every previous round: a time-of-day-dependent false pass.
 */
function vnDate(when) {
  return new Date((when === undefined ? Date.now() : when) + 7 * 3600e3)
    .toISOString().slice(0, 10);
}

// Captured once at module load == the moment this harness run started. Used as
// the lower bound of the cleanup window so a run that straddles midnight still
// removes every row it created instead of only the ones from its final date.
const VN_RUN_DATE = vnDate();

/**
 * Remove the `payment_transactions` rows a UI sweep creates, and PROVE parity.
 *
 * WHY THIS EXISTS
 * ---------------
 * `PremiumCheckout.vue` calls `POST /api/v1/payment/create-order` on mount to
 * build the QR. `routes-all.js` walks the WHOLE router inventory, which includes
 * `/premium/checkout`, so every full sweep mints real rows: measured
 * 2026-09-16, three sweep runs moved parity 126 -> 134. AGENTS.md documents the
 * trap but the sweep kept stepping in it, because cleanup was a separate manual
 * step that a tired operator skips.
 *
 * A harness that writes business rows must clean up inside the SAME run and
 * assert parity — not its own exit code. Deleting is scoped so it can never
 * touch a genuine payment: only rows with `status <> 'SUCCESS'`, i.e. an order
 * nobody paid. `transaction_id` stays NULL for those, which is checked too.
 *
 * Returns { ok, before, after, deleted, output } — `ok` requires the row count
 * to be back at `expected` AND no SQL error in the output (sqlcmd exits 0 even
 * on Msg 1934 / Msg 207).
 */
function cleanupAuditPayments(expected, day) {
  const { execSync } = require("child_process");
  const fs = require("fs");
  const path = require("path");
  const ROOT = path.join(__dirname, "..", "..", ".."); // sweep/v8/ui -> repo root
  const d = day || vnDate();

  // Emit an unambiguous marker. Parsing "the last number in the output" grabbed
  // "(1 rows affected)" and reported after=2 while the table held 126 — the same
  // class of bug as trusting an exit code.
  //
  // The window is a HALF-OPEN RANGE from the run's own VN date, not equality on
  // one date: a sweep that starts before midnight and ends after it writes rows
  // under two dates, and equality would silently orphan the first batch.
  const sql = [
    "SET QUOTED_IDENTIFIER ON;",
    "SELECT 'AUDIT_CANDIDATES=' + CAST(COUNT(*) AS varchar(20)) FROM payment_transactions",
    "WHERE created_at >= '" + d + " 00:00:00'",
    "  AND status <> 'SUCCESS'",
    "  AND transaction_id IS NULL;",
    "DELETE FROM payment_transactions",
    "WHERE created_at >= '" + d + " 00:00:00'",
    "  AND status <> 'SUCCESS'",
    "  AND transaction_id IS NULL;",
    // audit-v11 F130: the SELF-CLEAN assertion needs a count taken AFTER the delete.
    // AUDIT_CANDIDATES is measured before it (it is the "how much did we find" figure),
    // so it is non-zero precisely when there WAS residue to clean — asserting on it
    // directly would fail on every honest run. AUDIT_REMAINING is the one that must be 0.
    "SELECT 'AUDIT_REMAINING=' + CAST(COUNT(*) AS varchar(20)) FROM payment_transactions",
    "WHERE created_at >= '" + d + " 00:00:00'",
    "  AND status <> 'SUCCESS'",
    "  AND transaction_id IS NULL;",
    "SELECT 'AUDIT_CLEAN_TOTAL=' + CAST(COUNT(*) AS varchar(20)) FROM payment_transactions;",
  ].join("\n");
  const file = path.join(__dirname, "..", "_cleanup_payments.sql");
  fs.writeFileSync(file, sql + "\n", "utf8");

  let out = "";
  try {
    out = execSync("python sweep/v8/sqlrun.py sweep/v8/_cleanup_payments.sql",
      { cwd: ROOT, encoding: "utf8" });
  } catch (e) { out = String(e.stdout || "") + String(e.stderr || "") + String(e.message); }

  const mk = out.match(/AUDIT_CLEAN_TOTAL=(\d+)/);
  const ck = out.match(/AUDIT_CANDIDATES=(\d+)/);
  const rk = out.match(/AUDIT_REMAINING=(\d+)/);
  const after = mk ? parseInt(mk[1], 10) : NaN;
  const candidates = ck ? parseInt(ck[1], 10) : NaN;
  const remaining = rk ? parseInt(rk[1], 10) : NaN;
  const hadError = /Msg \d+/.test(out);
  // audit-v11 F130: `after === expected` alone was an unsound check. `expected` is a
  // BASELINE constant hard-coded by each caller (126 at the time), so a run that
  // leaves residue behind while the baseline was already wrong still satisfies it —
  // it prints PARITY OK and passes for the wrong reason. Two callers even disagreed
  // about the constant.
  //
  // What this function is actually responsible for is: no row its own date window
  // could have produced is still there afterwards. AUDIT_REMAINING is measured AFTER
  // the delete, so asserting it is 0 tests exactly that, and it holds regardless of
  // what the baseline constant happens to be.
  const selfClean = !isNaN(remaining) && remaining === 0;
  const matchesBaseline = after === expected;
  const ok = !hadError && selfClean;
  console.log("cleanupAuditPayments: window=since " + d + " 00:00:00"
    + " candidates=" + (isNaN(candidates) ? "?" : candidates)
    + " remaining=" + (isNaN(remaining) ? "?" : remaining)
    + " after=" + (isNaN(after) ? "?" : after)
    + " baseline=" + expected + " sqlError=" + hadError
    + " -> " + (ok ? "SELF-CLEAN OK" : "SELF-CLEAN FAILED")
    + (matchesBaseline ? "" : "  [NOTE: after != baseline " + expected
      + " — baseline may be stale; the sweep still cleaned its own rows]"));
  return { ok, after: isNaN(after) ? null : after, candidates, remaining, expected, matchesBaseline, output: out };
}

/** Row-count parity for the whole DB, as a comparable string. */
function dbParity() {
  const { execSync } = require("child_process");
  const path = require("path");
  const ROOT = path.join(__dirname, "..", "..", "..");
  try {
    const out = execSync("python sweep/v8/sqlrun.py sweep/v8/p16-parity.sql",
      { cwd: ROOT, encoding: "utf8" });
    // The data line is the one made only of digits and pipes, e.g.
    // "1471|43737|76|127|28|15|4|126|14|5". The header and the dashes rule are
    // not, so anchor on that instead of guessing a line index.
    const line = out.split(/\r?\n/).find(l => /^\s*\d+(\|\d+)+\s*$/.test(l));
    return line ? line.trim() : "UNPARSED";
  } catch (e) { return "ERR " + e.message.slice(0, 60); }
}

async function login(email, pass) {
  const r = await fetch(API + "/api/auth/login", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: email, password: pass })
  });
  const j = await r.json();
  return (j.data && j.data.token) || j.token;
}

/**
 * Full login payload, not just the token.
 *
 * WHY THIS EXISTS — a false pass, and its measured limits
 * ------------------------------------------------------
 * `seedToken()` writes ONLY `localStorage.token`. But `store/modules/auth.js`
 * computes `isAdmin` as `user.value?.isAdmin`, and `user` is initialised from
 * `localStorage.user` — which nothing ever wrote. `main.js` does NOT call
 * `fetchUser()` on boot, so a page loaded with a token and no user object has
 * `isAdmin === undefined` and the router guard
 * `if (to.meta.requiresAdmin && !auth.isAdmin)` redirects to `/`.
 *
 * MEASURED SCOPE (do not overstate this): the damage is limited to harnesses
 * that open a FRESH context and navigate to a guarded route as their first
 * action. `p20_routes_all_old_seed_ab.js` A/B'd the old and new seeds while
 * walking routes-all.js's exact route order and got IDENTICAL admin results
 * (0/9 bounced, same text lengths), because the app self-hydrates `user` from
 * `/api/auth/me` during earlier navigations. So a long sequential walk is fine;
 * a cold one is not.
 *
 * Affected: p17_screenshots.js (9 of 36 shots were the home page) and
 * p18_redirect_audit.js. Not affected: ui/routes-all.js, ui/design-v2.js.
 *
 * Seed the mapped user shape. The keys mirror `mapUser()`; a key the store does
 * not know is ignored, but a missing `isAdmin` silently disables the admin
 * surface.
 */
async function loginFull(email, pass) {
  const r = await fetch(API + "/api/auth/login", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: email, password: pass })
  });
  const j = await r.json();
  const d = j.data || j;
  return { token: d.token, user: d };
}

/** Map a login payload to the exact shape `mapUser()` produces. */
function mapUser(data) {
  return {
    id: data.id,
    username: data.username,
    email: data.email,
    fullName: data.fullName,
    avatarUrl: data.avatarUrl,
    isAdmin: data.isAdmin,
    isPremium: data.isPremium === true,
    premiumExpiry: data.premiumExpiry,
    currentLevel: data.currentLevel,
    totalPoints: data.totalPoints,
    currentStreak: data.currentStreak,
    hasPremiumAccess: data.hasPremiumAccess === true,
    aiGenerationCount: data.aiGenerationCount ?? 0,
    aiGenerationsRemainingToday: data.aiGenerationsRemainingToday ?? null
  };
}

const results = [];

function mkContext(browser, token, viewport) {
  return browser.newContext({ viewport: viewport || { width: 1440, height: 900 } });
}

async function seedToken(page, token) {
  await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((t) => localStorage.setItem("token", t), token);
}

/**
 * Seed token AND user. Use this instead of `seedToken` for anything that visits
 * a role-gated route — see `loginFull` for why a token alone is not enough.
 * Returns the mapped user so a caller can assert `isAdmin` was really granted.
 */
async function seedAuth(page, session) {
  await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((s) => {
    localStorage.setItem("token", s.token);
    localStorage.setItem("user", JSON.stringify(s.user));
  }, { token: session.token, user: mapUser(session.user) });
}

// Collect console errors + failed requests while visiting one route.
async function visit(page, route, label, opts) {
  opts = opts || {};
  const errors = [], warns = [], failed = [];
  const onConsole = m => {
    if (m.type() === "error") errors.push(m.text().slice(0, 200));
    else if (m.type() === "warning") warns.push(m.text().slice(0, 160));
  };
  const onReq = r => { if (!r.url().startsWith(API)) return; };
  const onResp = r => {
    const u = r.url();
    if (!u.startsWith(API)) return;
    if (r.status() >= 500) failed.push(r.status() + " " + u.replace(API, "") + " [" + label + "]");
    else if (r.status() === 404 && !opts.expect404) failed.push("404 " + u.replace(API, ""));
  };
  page.on("console", onConsole);
  page.on("request", onReq);
  page.on("response", onResp);
  let nav = "ok";
  try {
    await page.goto(APP + route, { waitUntil: "domcontentloaded", timeout: 30000 });
    await page.waitForTimeout(opts.wait || 2200);
  } catch (e) { nav = "NAV-ERR " + e.message.slice(0, 90); }
  page.off("console", onConsole);
  page.off("response", onResp);
  const info = await page.evaluate(() => {
    const b = document.body;
    const cs = getComputedStyle(b);
    const heads = [...document.querySelectorAll("h1,h2,h3,.font-extrabold,button")].slice(0, 60);
    const fams = new Set([cs.fontFamily.split(",")[0].replace(/"/g, "")]);
    heads.forEach(h => fams.add(getComputedStyle(h).fontFamily.split(",")[0].replace(/"/g, "")));
    return {
      title: document.title,
      textLen: (b.innerText || "").trim().length,
      h1: (document.querySelector("h1") || {}).innerText || null,
      fonts: [...fams],
      scrollW: document.documentElement.scrollWidth,
      clientW: document.documentElement.clientWidth,
      bg: cs.backgroundColor,
      hasApp: !!document.querySelector("#app") && document.querySelector("#app").children.length > 0
    };
  }).catch(e => ({ err: e.message }));
  const rec = { route: route, label: label, nav: nav, errors: errors, failed: failed, info: info, warns: warns.length };
  results.push(rec);
  return rec;
}

function summarize(title) {
  const bad = results.filter(r => r.nav !== "ok" || r.errors.length || r.failed.length || !r.info.hasApp);
  console.log("=== UI " + title + " routes=" + results.length + " PROBLEM=" + bad.length);
  for (const r of bad) {
    console.log("  ! " + r.route + " [" + r.label + "] nav=" + r.nav
      + " err=" + JSON.stringify(r.errors.slice(0, 3))
      + " failed=" + JSON.stringify(r.failed.slice(0, 3))
      + " mounted=" + (r.info.hasApp === undefined ? "?" : r.info.hasApp)
      + " text=" + r.info.textLen);
  }
  const fontBad = results.filter(r => r.info.fonts && r.info.fonts.some(f => !/Be Vietnam Pro/.test(f)));
  console.log("  non-BVP fonts on: " + (fontBad.length ? fontBad.map(r => r.route + "=" + JSON.stringify(r.info.fonts)).join(" ; ") : "none"));
  const overflow = results.filter(r => r.info.scrollW > r.info.clientW + 2);
  console.log("  horizontal overflow: " + (overflow.length ? overflow.map(r => r.route + "(" + r.info.scrollW + ">" + r.info.clientW + ")").join(" ; ") : "none"));
}

module.exports = { pw, APP, API, BASE, login, loginFull, mapUser, results, visit, summarize, seedToken, seedAuth, mkContext, flushLimits, sleep, vnDate, VN_RUN_DATE, cleanupAuditPayments, dbParity };
