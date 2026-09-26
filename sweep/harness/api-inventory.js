/**
 * Endpoint inventory, re-derived from SOURCE (read-only).
 *
 * Why this exists: v11's api-sweep probed ~27 of 131 endpoints. A sweep cannot be
 * "complete" if the list of endpoints is hand-written, so the list is generated from
 * the controllers themselves and reconciled against the annotation count.
 *
 * It walks every .java under src/main/java/.../controller, reads the class-level @RequestMapping,
 * and expands every @Get/Post/Put/Patch/DeleteMapping — including array-form aliases
 * ({"a","b"}) and the paths that differ only by `params=`.
 *
 * Run: node sweep/harness/api-inventory.js [--audit <name>] [--out <dir>]
 */
const fs = require("fs");
const path = require("path");
const { ROOT, OUT } = require("./_config.js");

const CTRL = path.join(ROOT, "src", "main", "java", "com", "datn", "engflow", "controller");
const OUT_FILE = path.join(OUT, "endpoint-inventory.json");

const VERB = { GetMapping: "GET", PostMapping: "POST", PutMapping: "PUT", PatchMapping: "PATCH", DeleteMapping: "DELETE" };

function walk(dir, acc = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, acc);
    else if (e.name.endsWith(".java")) acc.push(p);
  }
  return acc;
}

// Extract every quoted string inside a parenthesised argument list (handles {"a","b"}).
// Named args that are NOT paths (params/headers/produces/consumes) are stripped first,
// otherwise `params = "q"` is harvested as if it were a route.
function quotedArgs(s) {
  const cleaned = s
    .replace(/\b(params|headers|produces|consumes)\s*=\s*(\{[^}]*\}|"[^"]*")/g, "")
    .replace(/\bname\s*=\s*"[^"]*"/g, "");
  return [...cleaned.matchAll(/"([^"]*)"/g)].map((m) => m[1]);
}

// Find the balanced (...) immediately following an annotation name (whitespace allowed).
// Returns null when the annotation has no argument list — otherwise a later "(" in the
// method body would be mistaken for the annotation's, and its string literals harvested.
function balanced(src, startIdx) {
  let i = startIdx;
  while (i < src.length && /\s/.test(src[i])) i++;
  if (src[i] !== "(") return null;
  const open = i;
  let depth = 0;
  for (let j = open; j < src.length; j++) {
    if (src[j] === "(") depth++;
    else if (src[j] === ")") {
      depth--;
      if (depth === 0) return src.slice(open + 1, j);
    }
  }
  return null;
}

const rows = [];
let annotationCount = 0;

for (const file of walk(CTRL).sort()) {
  const src = fs.readFileSync(file, "utf8");
  const rel = path.relative(ROOT, file).replace(/\\/g, "/");
  const cls = path.basename(file, ".java");

  // class-level @RequestMapping
  let base = "";
  const ci = src.indexOf("@RequestMapping");
  if (ci !== -1) {
    const a = balanced(src, ci + "@RequestMapping".length);
    if (a) {
      const q = quotedArgs(a);
      if (q.length) base = q[0];
    }
  }

  // class-level @PreAuthorize (defense-in-depth signal)
  const classPreAuth = /@PreAuthorize/.test(src);

  for (const [ann, verb] of Object.entries(VERB)) {
    const re = new RegExp("@" + ann, "g");
    let m;
    while ((m = re.exec(src)) !== null) {
      annotationCount++;
      const line = src.slice(0, m.index).split("\n").length;
      const args = balanced(src, m.index + ann.length + 1) || "";
      let paths = quotedArgs(args);
      if (!paths.length) paths = [""]; // bare @GetMapping => class path only
      const hasParams = /params\s*=/.test(args);
      // method-level @PreAuthorize within the next ~200 chars (method annotation block)
      const after = src.slice(m.index, m.index + 400);
      const methodPreAuth = /@PreAuthorize/.test(after);
      for (const p of paths) {
        const full = (base + p).replace(/\/{2,}/g, "/") || "/";
        rows.push({
          controller: cls,
          javaFile: rel,
          line,
          httpMethod: verb,
          path: full,
          classBase: base,
          hasParams,
          preAuthorize: classPreAuth || methodPreAuth,
          preAuthorizeScope: classPreAuth ? "class" : methodPreAuth ? "method" : null,
        });
      }
    }
  }
}

// Reconciliation: annotation count vs expanded rows vs distinct literal paths
const distinctPaths = [...new Set(rows.map((r) => r.httpMethod + " " + r.path))];

const byController = {};
for (const r of rows) {
  byController[r.controller] = byController[r.controller] || { annotations: 0, rows: 0, methods: {} };
  byController[r.controller].methods[r.httpMethod] = (byController[r.controller].methods[r.httpMethod] || 0) + 1;
}
for (const [ann] of Object.entries(VERB)) { /* no-op, keeps shape stable */ }
// annotation count per controller
for (const r of rows) byController[r.controller].rows++;

const byVerb = {};
for (const r of rows) byVerb[r.httpMethod] = (byVerb[r.httpMethod] || 0) + 1;

const result = {
  generatedBy: "sweep/v12/api-inventory.js",
  sourceRoot: "src/main/java/com/datn/engflow/controller",
  annotationCount,
  expandedRows: rows.length,
  distinctMethodPaths: distinctPaths.length,
  controllers: Object.keys(byController).length,
  byVerb,
  byController,
  endpoints: rows,
};

fs.mkdirSync(OUT, { recursive: true });
fs.writeFileSync(OUT_FILE, JSON.stringify(result, null, 2));

console.log("annotationCount      :", annotationCount);
console.log("expandedRows         :", rows.length);
console.log("distinctMethodPaths  :", distinctPaths.length);
console.log("controllers          :", Object.keys(byController).length);
console.log("byVerb               :", JSON.stringify(byVerb));
console.log("written              :", path.relative(ROOT, OUT_FILE));
console.log("\ncontrollers (rows):");
for (const [c, v] of Object.entries(byController).sort((a, b) => b[1].rows - a[1].rows)) {
  console.log("  " + String(v.rows).padStart(3) + "  " + c);
}
