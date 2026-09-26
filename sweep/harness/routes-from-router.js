/**
 * routes-from-router.js — derive the frontend route inventory from the ROUTER itself.
 *
 * WHY THIS EXISTS (audit-v15 hardening, weakness L3-f)
 * ----------------------------------------------------
 * `sweep/v8/ui/routes-all.js` used to read
 * `.specify/specs/audit-v8-full/evidence/route-inventory.json` — a file that is
 * BOTH gitignored and untracked. So the harness was broken on a fresh clone: the
 * documented "sweep every route" command could not run at all, and nothing said so.
 *
 * The router is the source of truth for routes AND for their guards, so parse it
 * directly. No generated JSON to go stale, no untracked dependency.
 *
 * Exports: routes() -> [{ path, guard }]
 *   guard is one of the values routes-all.js asserts against:
 *     "guestOnly"    meta.guestOnly            (a logged-in user is bounced away)
 *     "admin"        meta.requiresAdmin
 *     "auth+premium" meta.requiresAuth + requiresPremium
 *     "auth"         meta.requiresAuth
 *     "public"       everything else
 */
const fs = require("fs");
const path = require("path");

const ROUTER = path.join(__dirname, "..", "..", "frontend", "src", "router", "index.js");

/**
 * Parse route objects from the router source.
 *
 * Regex-based on purpose: the router imports Vue/Pinia/store modules that cannot run
 * under plain node, and evaluating it would execute the guards. Reading literals keeps
 * this read-only and dependency-free.
 *
 * Child routes are written relative and inherit the parent prefix, so track the last
 * absolute path seen. The `meta:` for a route is the first `meta: {...}` appearing
 * after its `path:` and before the next `path:`.
 */
function routes() {
  const src = fs.readFileSync(ROUTER, "utf8");
  const out = [];
  const seen = new Set();
  const pathRe = /path:\s*['"]([^'"]+)['"]/g;

  // Collect (index, path) then look ahead for the meta block of each.
  const hits = [];
  let m;
  while ((m = pathRe.exec(src)) !== null) hits.push({ idx: m.index, path: m[1] });

  let parent = "";
  let parentGuard = "public";
  for (let i = 0; i < hits.length; i++) {
    let p = hits[i].path;
    const isChild = !p.startsWith("/");
    if (!isChild) {
      parent = p;
    } else {
      p = parent.replace(/\/$/, "") + "/" + p.replace(/^\//, "");
    }
    if (seen.has(p)) continue;
    seen.add(p);

    // meta block = text from this path to the next path (or 400 chars).
    const from = hits[i].idx;
    const to = i + 1 < hits.length ? hits[i + 1].idx : Math.min(src.length, from + 400);
    const chunk = src.slice(from, to);

    let guard = "public";
    if (/requiresAdmin:\s*true/.test(chunk)) guard = "admin";
    else if (/requiresPremium:\s*true/.test(chunk)) guard = "auth+premium";
    else if (/requiresAuth:\s*true/.test(chunk)) guard = "auth";
    else if (/guestOnly:\s*true/.test(chunk)) guard = "guestOnly";

    // A child route with no meta of its own INHERITS the parent's guard. Vue Router
    // merges meta down the chain, so /admin/lessons is admin-only via the /admin
    // parent's `meta: { requiresAdmin: true }` even though the child declares none.
    if (isChild && guard === "public") guard = parentGuard;
    if (!isChild) parentGuard = guard;

    // A route with its own `redirect:` always lands elsewhere by DESIGN (e.g. /admin ->
    // /admin/dashboard). routes-all.js must not treat that as an unexpected bounce.
    const redirects = /redirect:\s*['"]([^'"]+)['"]/.test(chunk);

    out.push({ path: p, guard, redirects });
  }
  return out;
}

module.exports = { routes, ROUTER };

if (require.main === module) {
  const r = routes();
  console.log("routes found: " + r.length + " (from " + path.relative(process.cwd(), ROUTER) + ")");
  for (const x of r) console.log("  " + x.path.padEnd(36) + "[" + x.guard + "]");
}
