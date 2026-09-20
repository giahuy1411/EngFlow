/** Doc route inventory cua v9 va doi chieu voi router hien tai. */
const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "..", "..");
const inv = JSON.parse(fs.readFileSync(
  path.join(ROOT, ".specify", "specs", "audit-v9-full", "evidence", "route-inventory.json"), "utf8"));

console.log("totalRoutes:", inv.totalRoutes);
console.log("routes:", inv.routes.length);
const g = {};
for (const r of inv.routes) g[r.guard] = (g[r.guard] || 0) + 1;
console.log("guards:", JSON.stringify(g));
console.log("");
console.log("danh sach:");
for (const r of inv.routes) {
  console.log(`  ${String(r.guard).padEnd(16)} ${r.path}`);
}

// Doi chieu voi router that: dem `path:` trong index.js
const routerSrc = fs.readFileSync(path.join(ROOT, "frontend", "src", "router", "index.js"), "utf8");
const paths = [...routerSrc.matchAll(/path:\s*'([^']+)'/g)].map((m) => m[1]);
console.log("");
console.log("router hien tai co", paths.length, "path");
const invPaths = new Set(inv.routes.map((r) => r.path));
const missing = paths.filter((p) => !invPaths.has(p));
const extra = inv.routes.map((r) => r.path).filter((p) => !paths.includes(p));
console.log("route MOI chua co trong inventory:", missing.length ? JSON.stringify(missing) : "(khong)");
console.log("route trong inventory nhung khong con:", extra.length ? JSON.stringify(extra) : "(khong)");
