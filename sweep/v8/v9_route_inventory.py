"""route_inventory.py - Task 10 / Task 5 "route-map.md" evidence.

Deterministically extracts every frontend route from frontend/src/router/index.js
so the count is REPRODUCIBLE FROM SOURCE rather than copied from a previous
report. Also resolves the auth guard each route actually falls under by
replaying router.beforeEach's precedence order. A route with no `meta` block is
NOT public: the global guard still runs and the /admin parent's requiresAdmin is
inherited by every child. Getting this wrong is how an audit overstates how many
routes are reachable anonymously.

Guard precedence in router/index.js (order matters - first match wins):
  1. meta.requiresPremium && !isAdmin && !isPremium -> /premium?redirect=...
  2. meta.requiresAuth && !isLoggedIn               -> /login
  3. meta.guestOnly && isLoggedIn                   -> /lessons
  4. meta.requiresAdmin && !isAdmin                 -> /
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ROUTER = ROOT / "frontend" / "src" / "router" / "index.js"

src = ROUTER.read_text(encoding="utf-8")

# ---- strip comments so commented-out routes can't be counted -----------------
src_nc = re.sub(r"//[^\n]*", "", src)
src_nc = re.sub(r"/\*.*?\*/", "", src_nc, flags=re.S)

# ---- top-level routes: entries at 2-space indent inside `const routes = [` ---
# Children live at 6-space indent inside a `children: [` block.
lines = src_nc.split("\n")

routes = []
admin_children = []
in_children = False
depth_children = 0

pending = {}
for raw in lines:
    line = raw.strip()
    if not line:
        continue

    m = re.match(r"path:\s*'([^']*)'", line)
    if m:
        pending["path"] = m.group(1)
    m = re.match(r"name:\s*'([^']*)'", line)
    if m:
        pending["name"] = m.group(1)
    m = re.match(r"component:\s*\(\)\s*=>\s*import\('([^']+)'\)", line)
    if m:
        pending["component"] = m.group(1)
    m = re.match(r"redirect:\s*'([^']*)'", line)
    if m:
        pending["redirect"] = m.group(1)

    if re.match(r"meta:\s*\{", line):
        meta = {}
        block = line
        # meta may be single-line or span several lines
        if line.count("{") > line.count("}"):
            continue
        for k in ("requiresAuth", "requiresAdmin", "requiresPremium", "guestOnly"):
            mm = re.search(k + r":\s*(true|false)", block)
            if mm:
                meta[k] = mm.group(1) == "true"
        pending["meta"] = meta

    if "children:" in line:
        in_children = True
        # flush the admin parent entry before collecting children
        if "path" in pending:
            admin_children.append(("__PARENT__", dict(pending)))
            pending = {}
        depth_children = 1
        continue

    if in_children:
        if re.match(r"^\s*\]", raw) and depth_children == 1:
            in_children = False
            continue
        if line.startswith("{ path:"):
            # compact child form: { path: 'x', name: 'Y', component: ..., props: true }
            body = line.strip().lstrip("{").rstrip("},")
            entry = {}
            for part in re.split(r",\s*(?![^()]*\))", body):
                part = part.strip()
                mm = re.match(r"path:\s*'([^']*)'", part)
                if mm:
                    entry["path"] = mm.group(1)
                mm = re.match(r"name:\s*'([^']*)'", part)
                if mm:
                    entry["name"] = mm.group(1)
                mm = re.match(r"component:\s*\(\)\s*=>\s*import\('([^']+)'\)", part)
                if mm:
                    entry["component"] = mm.group(1)
                mm = re.match(r"redirect:\s*'([^']*)'", part)
                if mm:
                    entry["redirect"] = mm.group(1)
            if entry:
                admin_children.append(("__CHILD__", entry))
        continue

    if line.startswith("},") or line == "}":
        if "path" in pending:
            routes.append(dict(pending))
        pending = {}
        continue
    if line.startswith("{") and "path" in pending:
        # new object begins before we flushed -> flush previous
        routes.append(dict(pending))
        pending = {}

if "path" in pending:
    routes.append(dict(pending))

# ---- build the final table with resolved guard behaviour ---------------------
def resolve(meta, is_parent_admin=False):
    meta = meta or {}
    if meta.get("requiresPremium"):
        return "auth+premium", "/premium?redirect=..."
    if meta.get("requiresAuth"):
        return "auth", "/login"
    if meta.get("guestOnly"):
        return "guestOnly", "/lessons"
    if meta.get("requiresAdmin"):
        return "admin", "/"
    return "public", ""

out = []
for r in routes:
    if r.get("path") == "/admin":
        continue  # expanded below
    meta = r.get("meta", {})
    guard, redirect = resolve(meta)
    out.append({
        "path": r["path"],
        "name": r.get("name", ""),
        "component": r.get("component", ""),
        "guard": guard,
        "redirectTo": redirect,
        "metaRequiresAuth": meta.get("requiresAuth", None),
        "metaRequiresPremium": meta.get("requiresPremium", None),
        "metaGuestOnly": meta.get("guestOnly", None),
    })

# admin children inherit requiresAuth+requiresAdmin from the parent
for kind, e in admin_children:
    if kind == "__PARENT__":
        continue
    child_path = "/admin/" + e["path"] if e.get("path") else "/admin"
    out.append({
        "path": child_path,
        "name": e.get("name", ""),
        "component": e.get("component", ""),
        "guard": "admin" if not e.get("redirect") else "admin-redirect",
        "redirectTo": e.get("redirect", ""),
        "metaRequiresAuth": None,
        "metaRequiresPremium": None,
        "metaGuestOnly": None,
        "inheritedFrom": "/admin",
    })

# NOTE: the catch-all /:pathMatch(.*)* is already emitted by the parser above
# because it is a normal top-level entry. Do NOT append it again -- an earlier
# revision did, and reported 40 routes when the true count is 39.

# ---- sanity checks: fail loudly rather than emit a quietly wrong count -------
seen = {}
for r in out:
    seen[r["path"]] = seen.get(r["path"], 0) + 1
dupes = {k: v for k, v in seen.items() if v > 1}
if dupes:
    print("FATAL: duplicate route paths parsed: %s" % dupes, file=sys.stderr)
    sys.exit(2)

# every named route must resolve a component or a redirect
for r in out:
    if r.get("name") and not r.get("component") and not r.get("redirectTo"):
        print("FATAL: named route %s has neither component nor redirect" % r["name"],
              file=sys.stderr)
        sys.exit(2)

guards = {}
for r in out:
    guards[r["guard"]] = guards.get(r["guard"], 0) + 1

result = {
    "source": str(ROUTER.relative_to(ROOT)).replace("\\", "/"),
    "withName": len([r for r in out if r["name"]]),
    "totalRoutes": len(out),
    "byGuard": guards,
    "routes": out,
}

evidence = ROOT / ".specify" / "specs" / "audit-v9-full" / "evidence"
evidence.mkdir(parents=True, exist_ok=True)
(evidence / "route-inventory.json").write_text(
    json.dumps(result, ensure_ascii=False, indent=1), encoding="utf-8")

print("source      : %s" % result["source"])
print("routes      : %d  (named: %d)" % (result["totalRoutes"], result["withName"]))
print("by guard    : %s" % json.dumps(guards))
print("wrote       : evidence/route-inventory.json")
