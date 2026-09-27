/**
 * sweep/harness/_config.js — shared config for the audit harness suite.
 *
 * WHY THIS EXISTS (audit-v15 hardening, weakness L2)
 * --------------------------------------------------
 * Before this file, every harness hardcoded the audit namespace it belonged to
 * (`audit-v13-full`, `AUDIT-V13-`, `sweep/v13/...`). Each new audit round had to
 * find-and-replace all of them by hand, and because `sweep/**` is gitignored except
 * a whitelist, the per-round harness was deleted at cleanup — so the fixes from one
 * round never survived into the next. That is the measured mechanism behind "the
 * harness was rebuilt from an old snapshot and re-introduced fixed bugs".
 *
 * Now the namespace is DATA, not code:
 *   node sweep/harness/<probe>.js [--audit audit-v19-full] [--out <dir>]
 *
 * Defaults point at the current audit (`audit-v19-full`). A later round passes
 * `--audit audit-v20-full` and every path/marker follows — no edits, nothing lost.
 *
 * NOTE: the default is ALSO asserted by assert-harness.js (F-17-16). It must name an audit
 * directory that exists under .specify/specs/, so a stale default fails the static suite rather
 * than silently writing evidence into a previous round's folder.
 *
 * Exports:
 *   AUDIT   "audit-v19-full"      audit name (drives evidence dir)
 *   VER     "V19"                 short version tag
 *   MARKER  "AUDIT-V19"           prefix for every row this suite writes
 *   ROOT    <repo root>
 *   OUT     <evidence dir>        `.specify/specs/<AUDIT>/evidence`
 *   arg(name, def)                read a `--name value` CLI arg
 */
const path = require("path");

function arg(name, def) {
  const i = process.argv.indexOf("--" + name);
  return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : def;
}

const AUDIT = arg("audit", "audit-v19-full");
const ROOT = path.join(__dirname, "..", "..");
const OUT = arg("out", path.join(ROOT, ".specify", "specs", AUDIT, "evidence"));

// "audit-v17-full" -> "V17". Falls back to "VX" if the name is unrecognised, so a
// typo produces a visibly wrong marker rather than silently reusing the last one.
const vm = /^audit-v(\d+)/.exec(AUDIT);
const VER = vm ? "V" + vm[1] : "VX";
const MARKER = "AUDIT-" + VER;

/** Marker for a data class, e.g. rowMarker("API") -> "AUDIT-V17-API". */
function rowMarker(kind) {
  return MARKER + "-" + kind;
}

module.exports = { AUDIT, VER, MARKER, ROOT, OUT, arg, rowMarker };
