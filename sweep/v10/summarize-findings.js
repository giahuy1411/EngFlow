/** Tom tat findings.json — dem theo status va severity. */
const fs = require("fs");
const path = require("path");
const ROOT = path.join(__dirname, "..", "..");
const j = JSON.parse(fs.readFileSync(
  path.join(ROOT, ".specify", "specs", "audit-v10-full", "findings.json"), "utf8"));

console.log("audit:", j.audit, " started:", j.started);
console.log("findings:", j.findings.length);
console.log("");

const byStatus = {};
const bySeverity = {};
for (const f of j.findings) {
  byStatus[f.status] = (byStatus[f.status] || 0) + 1;
  bySeverity[f.severity] = (bySeverity[f.severity] || 0) + 1;
}
console.log("theo status:");
for (const [k, v] of Object.entries(byStatus)) console.log("  " + k.padEnd(32) + v);
console.log("");
console.log("theo severity:");
for (const [k, v] of Object.entries(bySeverity)) console.log("  " + k.padEnd(32) + v);
console.log("");
console.log("danh sach:");
for (const f of j.findings) {
  console.log("  " + f.id.padEnd(6) + f.severity.padEnd(8) + f.status.padEnd(32)
    + (f.title || "").slice(0, 60));
}
console.log("");
console.log("blockers:", j.blockers.length);
for (const b of j.blockers) {
  console.log("  " + b.id + ": " + (b.what || "").slice(0, 100));
  if (b.remaining_blocked) console.log("      remaining: " + JSON.stringify(b.remaining_blocked));
}
