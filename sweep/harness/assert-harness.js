/**
 * assert-harness.js — static self-checks for the audit harness suite.
 *
 * Complements the JUnit HarnessDriftTest (which covers endpoints/tables/docs that the
 * backend can see). This one checks things only the frontend + node side knows:
 *
 *   1. Every route a browser harness visits exists in the real router
 *      (frontend/src/router/index.js) — catches a route removed but still swept.
 *   2. Every internal `require()` in the harness resolves to a file that exists
 *      (catches the audit-v15 L3-f class: a harness depending on an untracked file).
 *   3. No harness hardcodes a stale parity/payments baseline literal — those live in
 *      sweep/v8/ui/lib.js (single source of truth) so they cannot drift per-file.
 *   4. Every browser harness that navigates a WRITE-ON-MOUNT route (/premium/checkout
 *      mints a real payment row) must call a cleanup helper — a harness that writes real
 *      data and never cleans up is the L1 class this whole pass exists to kill (the
 *      audit-v15 F-15-09 defect: ui-sweep minted payment rows with no cleanup).
 *   5. The harness-restore record exists and is non-empty — a fix made in one round
 *      must be written down, or the next round rebuilds blind (the L2 mechanism).
 *
 * Run: node sweep/harness/assert-harness.js [--audit <name>]
 * Exit 0 = all clean; 1 = at least one problem (each printed).
 */
const fs = require("fs");
const path = require("path");
const { ROOT, AUDIT } = require("./_config.js");
const { routes: routerRoutes } = require("./routes-from-router.js");

const problems = [];
function check(name, ok, detail) {
  console.log((ok ? "PASS  " : "FAIL  ") + name + (detail ? "  -- " + detail : ""));
  if (!ok) problems.push(name + (detail ? ": " + detail : ""));
}

const HARNESS_DIR = __dirname;
const ROUTER_SOURCE = fs.readFileSync(
  path.join(ROOT, "frontend", "src", "router", "index.js"), "utf8");
const ROUTE_PATHS = new Set(routerRoutes().map((r) => r.path));

/** Every file the suite scans: the tracked harness + the shared tooling. */
function suiteFiles() {
  const dirs = [HARNESS_DIR, path.join(ROOT, "sweep", "v8", "ui"), path.join(ROOT, "sweep", "v12")];
  const out = [];
  for (const d of dirs) {
    if (!fs.existsSync(d)) continue;
    for (const f of fs.readdirSync(d)) {
      if (f.endsWith(".js")) out.push(path.join(d, f));
    }
  }
  return out;
}

// ── 1. routes a harness visits must exist in the router ──────────────────────
{
  const routeLiteral = /["'`](\/[A-Za-z0-9_:/{}.*-]*)["'`]/g;
  const missing = [];
  for (const f of suiteFiles()) {
    const base = path.basename(f);
    // Only browser harnesses navigate routes; skip pure API/CLI helpers.
    const src = fs.readFileSync(f, "utf8");
    const isBrowser = /pw\.chromium|page\.goto|APP \+/.test(src);
    if (!isBrowser) continue;
    // Only look at strings that are actually navigated to: `APP + "<path>"` or
    // `goto(... "<path>")`. A bare string literal anywhere else (a filename, an API
    // path, a log label) is not a route claim.
    const navLiteral = /(?:APP\s*\+\s*|goto\([^)]*?)["'`](\/[A-Za-z0-9_:/{}.*-]*)["'`]/g;
    for (const m of src.matchAll(navLiteral)) {
      let p = m[1];
      if (p.startsWith("/api")) continue;                        // API paths are JUnit's job
      // A path ending in "/" or one that is a prefix of a real route is a partial
      // template (e.g. "/decks/" + id), not a route claim.
      if (p.endsWith("/")) continue;
      // Normalise concrete ids back to the router's :id form so /lessons/445 matches /lessons/:id.
      const norm = p.replace(/\/\d+(?=\/|$)/g, "/:id").replace(/\/[0-9a-f-]{8,}(?=\/|$)/g, "/:id");
      if (ROUTE_PATHS.has(p) || ROUTE_PATHS.has(norm)) continue;
      if ([...ROUTE_PATHS].some((rp) => rp.startsWith(p + "/"))) continue;   // prefix of a real route
      if (/definitely-not|nonexistent|#/.test(p)) continue;
      missing.push(base + " -> " + p);
    }
  }
  check("every route a browser harness visits exists in the router",
    missing.length === 0, missing.slice(0, 8).join(" ; "));
}

// ── 2. internal require()s must resolve to a real file ───────────────────────
{
  const bad = [];
  for (const f of suiteFiles()) {
    const src = fs.readFileSync(f, "utf8");
    for (const m of src.matchAll(/require\(\s*["'](\.[^"']+)["']\s*\)/g)) {
      const target = path.resolve(path.dirname(f), m[1]);
      const candidates = [target, target + ".js", path.join(target, "index.js")];
      if (!candidates.some((c) => fs.existsSync(c))) {
        bad.push(path.basename(f) + " -> " + m[1]);
      }
    }
  }
  check("every internal require() resolves to an existing file",
    bad.length === 0, bad.slice(0, 8).join(" ; "));
}

// ── 3. no stale baseline literals outside the single source of truth ─────────
{
  // The 10-number parity string and cleanupAuditPayments(<n>) with a literal arg.
  const staleParity = /1471\|43737|76\|127\|28\|15\|4\|126/;
  const literalPayments = /cleanupAuditPayments\(\s*\d+\s*\)/;
  const hits = [];
  for (const f of suiteFiles()) {
    const base = path.basename(f);
    if (base === "lib.js" || base === "_config.js") continue;      // the source of truth
    const src = fs.readFileSync(f, "utf8");
    src.split("\n").forEach((text, i) => {
      // A comment that explains the old baseline is fine.
      if (/^\s*(\/\/|\*|--)/.test(text)) return;
      if (staleParity.test(text)) hits.push(base + ":" + (i + 1) + " stale parity literal");
      if (literalPayments.test(text)) hits.push(base + ":" + (i + 1) + " hardcoded payments baseline");
    });
  }
  check("no hardcoded parity/payments baselines outside lib.js",
    hits.length === 0, hits.slice(0, 8).join(" ; "));
}

// ── 4. a harness that navigates a write-on-mount route must clean up ─────────
{
  // `/premium/checkout` mounts PremiumCheckout.vue, which POSTs
  // /api/v1/payment/create-order and mints a REAL payment_transactions row. Any
  // browser harness whose source names that route MUST also call a cleanup helper,
  // or it leaks business data — the exact audit-v15 F-15-09 defect. This is the
  // static half of the plan's "harness-ghi-phải-có-cleanup" requirement; assertClean()
  // is the runtime half.
  const WRITE_ROUTE = /premium\/checkout/;
  const CLEANUP_CALL = /cleanupAuditPayments|assertClean/;
  const offenders = [];
  for (const f of suiteFiles()) {
    const src = fs.readFileSync(f, "utf8");
    const isBrowser = /pw\.chromium|page\.goto|APP \+/.test(src);
    if (!isBrowser) continue;
    // A harness is allowed to MENTION the route only to say it avoids it; judge by
    // whether it also navigates. If it names the route at all we require cleanup.
    if (!WRITE_ROUTE.test(src)) continue;
    if (!CLEANUP_CALL.test(src)) offenders.push(path.basename(f));
  }
  check("every harness that visits a write-on-mount route calls a cleanup helper",
    offenders.length === 0, offenders.join(" ; "));
}

// ── 5. the harness-restore record must exist and be non-empty ────────────────
{
  const rec = path.join(ROOT, ".specify", "specs", AUDIT, "evidence", "harness-restore.md");
  let ok = false, detail = "missing";
  if (fs.existsSync(rec)) {
    const body = fs.readFileSync(rec, "utf8").trim();
    // Non-empty AND actually says something (a placeholder file was the v15 failure).
    ok = body.length > 200 && /nguồn|source/i.test(body) && /fix/i.test(body);
    detail = ok ? body.length + " chars" : "too short or missing Source/Fix sections";
  }
  check("harness-restore.md exists, non-empty, documents source + fixes", ok, detail);
}

console.log("\n=== assert-harness: " + (problems.length === 0 ? "ALL CLEAN" : problems.length + " PROBLEM(S)") + " ===");
process.exit(problems.length === 0 ? 0 : 1);
