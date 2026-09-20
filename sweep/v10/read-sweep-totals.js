/** Doc tong ket cua routes-all-v10.json khong can classifier. */
const fs = require("fs");
const path = require("path");
const p = path.join(__dirname, "routes-all-v10.json");
const j = JSON.parse(fs.readFileSync(p, "utf8"));
console.log("=== TONG KET ROUTE SWEEP ===");
for (const [k, v] of Object.entries(j.totals || {})) console.log("  " + k.padEnd(18) + v);
if (j.badLanding && j.badLanding.length) {
  console.log("");
  console.log("landing sai:");
  j.badLanding.forEach((b) => console.log("  " + b));
}
const rows = j.results || [];
const withIssue = rows.filter((r) => r.consoleErrors.length || r.pageErrors.length
  || r.apiErrors.length || (r.overflow || 0) > 16 || !r.mounted || !r.redirectOk || r.badFonts.length);
console.log("");
console.log("so luot CO van de: " + withIssue.length + " / " + rows.length);
for (const r of withIssue.slice(0, 10)) {
  console.log(`  ${r.viewport} ${r.role} ${r.url}: err=${r.consoleErrors.length} page=${r.pageErrors.length} api=${r.apiErrors.length} ovf=${r.overflow} mounted=${r.mounted} landing=${r.redirectOk} fonts=${r.badFonts.length}`);
}
