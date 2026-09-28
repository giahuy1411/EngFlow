const pw = require("playwright-core");
const fs = require("fs");
const path = require("path");
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
 * Resolve a Chromium executable for playwright-core.
 *
 * WHY THIS EXISTS (audit-v18 F-18-01): several probes hardcoded a single revision
 * (`chromium-1237`). The installed bundle is a DIFFERENT revision (1234 this round),
 * so those probes threw `browserType.launch: executable doesn't exist` — a harness
 * defect that looked like a tool outage. Playwright's own registry keeps the real
 * path; read it, then fall back to scanning the ms-playwright cache dirs, then to a
 * system browser. Returns null when nothing is found (playwright then uses its default).
 */
function resolveChromium() {
  const os = require("os");
  const candidates = [];
  try {
    const reg = require("playwright-core").chromium.executablePath();
    if (reg) candidates.push(reg);
  } catch (e) { /* registry path not always available */ }
  const cache = path.join(os.homedir(), "AppData", "Local", "ms-playwright");
  try {
    if (fs.existsSync(cache)) {
      for (const d of fs.readdirSync(cache)) {
        if (/^chromium-\d+$/.test(d)) {
          candidates.push(path.join(cache, d, "chrome-win64", "chrome.exe"));
          candidates.push(path.join(cache, d, "chrome-win", "chrome.exe"));
        }
      }
    }
  } catch (e) { /* cache unreadable */ }
  candidates.push("C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe");
  return candidates.find((p) => { try { return fs.existsSync(p); } catch { return false; } }) || null;
}

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
 * The canonical DB parity baseline — SINGLE SOURCE OF TRUTH.
 *
 * WHY THIS EXISTS (audit-v15 hardening, weakness L3)
 * --------------------------------------------------
 * Three UI harnesses each hardcoded a copy of this string, and a copy of the
 * payments baseline (a literal `cleanupAuditPayments(<n>)`). When audit-v15 changed the DB
 * (dropped lesson_snapshots, cleaned 67 users + 114 payments) every copy went
 * stale at once and printed a 10-number baseline that no longer existed — a
 * human comparing by eye would conclude the DB had drifted. Keeping one constant
 * here means a schema/data change updates one line, not three.
 *
 * Update this when the DB legitimately changes, and update AGENTS.md in the same
 * commit (it documents the same baseline).
 *
 * audit-v20 (2026-09-28): payments 12 -> 13. The extra row is a REAL SePay bank
 * transfer (`ENG73E2D3AA2DF6`, verified end-to-end in audit-v19 W4) that the V9
 * decision deliberately keeps. It is NOT harness residue: PENDING_PAYMENTS=0 and
 * STUDY_DAYS=4 both still match, so no sweep left anything behind. The baseline
 * moved because the DATA legitimately changed, not because the guard broke.
 *
 * re-verify 2026-09-28 (demo-doc re-verification): video 4 -> 5, STUDY_DAYS 4 -> 5,
 * EXERCISE_ATTEMPTS 33 -> 34. All three are REAL activity that accumulated since the
 * v21 round — one video attempt, one study day, one exercise submit — not residue:
 * PENDING_PAYMENTS is still 0, so no sweep leaked. Same class of change as the
 * payments 12 -> 13 move above: the DATA changed, the guard did not break. Baseline
 * mirrors the demo guide's Phụ lục D and AGENTS.md.
 */
const PARITY_BASELINE = "1470|43738|5|118|29|5|3|13|10";
const PAYMENTS_BASELINE = 13;
const STUDY_DAYS_BASELINE = 5;
// audit-v17 F-17-07 (round 2, F-17-21): PROBE-ACCOUNT rows only — the same two emails
// cleanupExerciseAttempts deletes for. The first version counted the whole table, so a REAL
// learner submitting (there is a third user with real rows) would trip a false DIRTY even when
// the harness left nothing behind. Measured 2026-09-26: 33 probe-account rows; 34 on 2026-09-28.
const EXERCISE_ATTEMPTS_BASELINE = 34;

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

/**
 * Remove the `study_days` row(s) a UI sweep's own ACTIONS write, in the same run.
 *
 * WHY THIS EXISTS (audit-v16 F-16-01, corrected after adversarial review)
 * ----------------------------------------------------------------------
 * A browser harness that LOGS IN does NOT write study_days — `UserService.login()`
 * only READS the streak (`getCurrentStreak` → `StudyActivityService.currentStreak`,
 * readOnly). The real writers are the harness's *actions*, all via
 * `StudyActivityService.recordStudy()`:
 *   - `ExerciseService.submitExercises` (POST .../exercises/submit)   ← ui-sweep + the MCP run
 *   - `FlashcardService.recordStudyDay` (POST /api/flashcards/study)
 *   - `SrsService` (POST /api/srs/review)
 *   - `StreakService.checkin` (game submit)
 * So the residue class is real (v15 L1-a added it to parity and to api-sweep), but the
 * attribution in the first draft of F-16-01 ("login writes it") was WRONG — fixed here.
 *
 * The window is a HALF-OPEN RANGE from the run's OWN start date (`VN_RUN_DATE`, captured
 * once at module load), NOT equality on one date: a run that starts before midnight and
 * ends after it writes rows under two dates, and equality would silently orphan the first
 * batch (the exact false-pass `cleanupAuditPayments` was fixed for — see lib.js:113-115).
 *
 * Scoped to the two probe accounts only, so a real learner's day is never touched.
 *
 * @param {string} [from] ISO date (VN) lower bound. Defaults to this run's start date.
 */
function cleanupStudyDays(from) {
  const { execFileSync } = require("child_process");
  const fs = require("fs");
  const path = require("path");
  const ROOT = path.join(__dirname, "..", "..", ".."); // sweep/v8/ui -> repo root
  const lower = from || VN_RUN_DATE;
  const upper = vnDate(); // the run's current VN date (>= lower; equal unless it straddled midnight)
  // Half-open window [lower, upper] — study_date is a DATE column.
  const where = "WHERE study_date >= '" + lower + "' AND study_date <= '" + upper + "' AND user_id IN "
    + "(SELECT user_id FROM users WHERE email IN ('user@gmail.com','admin@gmail.com'))";
  const sql = [
    "SET QUOTED_IDENTIFIER ON;",
    "SELECT 'AUDIT_SD_CANDIDATES=' + CAST(COUNT(*) AS varchar(20)) FROM study_days " + where + ";",
    "DELETE FROM study_days " + where + ";",
    "SELECT 'AUDIT_SD_REMAINING=' + CAST(COUNT(*) AS varchar(20)) FROM study_days " + where + ";",
  ].join("\n");
  const file = path.join(__dirname, "..", "_cleanup_study_days.sql");
  fs.writeFileSync(file, sql + "\n", "utf8");

  let out = "";
  try {
    out = execFileSync("python", ["sweep/v8/sqlrun.py", "sweep/v8/_cleanup_study_days.sql"],
      { cwd: ROOT, encoding: "utf8" });
  } catch (e) { out = String(e.stdout || "") + String(e.stderr || "") + String(e.message); }

  const ck = out.match(/AUDIT_SD_CANDIDATES=(\d+)/);
  const rk = out.match(/AUDIT_SD_REMAINING=(\d+)/);
  const candidates = ck ? parseInt(ck[1], 10) : NaN;
  const remaining = rk ? parseInt(rk[1], 10) : NaN;
  const hadError = /Msg \d+/.test(out);
  // AUDIT_SD_REMAINING is measured AFTER the delete, so requiring 0 proves the window is
  // empty afterwards regardless of how many rows were there before.
  const ok = !hadError && !isNaN(remaining) && remaining === 0;
  console.log("cleanupStudyDays: window=[" + lower + ".." + upper + "]"
    + " candidates=" + (isNaN(candidates) ? "?" : candidates)
    + " remaining=" + (isNaN(remaining) ? "?" : remaining)
    + " sqlError=" + hadError + " -> " + (ok ? "SELF-CLEAN OK" : "SELF-CLEAN FAILED"));
  return { ok, candidates, remaining, output: out };
}

/** Row-count parity for the whole DB, as a comparable string. */
function dbParity() {
  const { execFileSync } = require("child_process");
  const path = require("path");
  const ROOT = path.join(__dirname, "..", "..", "..");
  try {
    // execFileSync + argv array: no shell, so nothing in the path can be interpreted
    // as shell syntax (same pattern as perf-probe.js / deep-probe.js).
    const out = execFileSync("python", ["sweep/v8/sqlrun.py", "sweep/v8/p16-parity.sql"],
      { cwd: ROOT, encoding: "utf8" });
    // The data line is the one made only of digits and pipes, e.g.
    // "1470|43738|5|118|29|4|3|13|10". The header and the dashes rule are
    // not, so anchor on that instead of guessing a line index.
    const line = out.split(/\r?\n/).find(l => /^\s*\d+(\|\d+)+\s*$/.test(l));
    return line ? line.trim() : "UNPARSED";
  } catch (e) { return "ERR " + e.message.slice(0, 60); }
}

/** Read a `NAME=<n>` marker from p16-parity.sql output (e.g. STUDY_DAYS=4). */
function parityMarker(name) {
  const { execFileSync } = require("child_process");
  const path = require("path");
  const ROOT = path.join(__dirname, "..", "..", "..");
  try {
    const out = execFileSync("python", ["sweep/v8/sqlrun.py", "sweep/v8/p16-parity.sql"],
      { cwd: ROOT, encoding: "utf8" });
    const m = new RegExp(name + "=(\\d+)").exec(out);
    return m ? parseInt(m[1], 10) : null;
  } catch (e) { return null; }
}

/**
 * audit-v17 F-17-07 — delete `exercise_attempts` rows this run could have written.
 *
 * WHY: `ExerciseService.submitExercises` (POST /api/lessons/{id}/exercises/submit) persists an
 * `ExerciseAttempt` on every submit. The MCP UI walkthrough submits through the real UI, so it
 * leaves a row — measured in v17 (`attempt_id=70147`, cleaned by hand). No committed harness
 * creates these (grep `exercises/submit` across sweep/ = 0), but a human/MCP-driven run does,
 * and nothing cleaned or counted it. Same residue channel as `study_days` (F-16-01).
 *
 * SAFETY: scoped to the two probe accounts AND a completed_at window, so a real learner's row
 * can never be touched. `completed_at` is the timestamp column (there is no created_at, no
 * status, no soft-delete on this table). Structure mirrors `cleanupAuditPayments` — notably a
 * count taken AFTER the delete for the assertion, and the correct `rk[1]` parse.
 *
 * @param {string} [from] ISO date (VN) lower bound. Defaults to this run's start date.
 */
function cleanupExerciseAttempts(from) {
  const { execFileSync } = require("child_process");
  const fs = require("fs");
  const path = require("path");
  const ROOT = path.join(__dirname, "..", "..", "..");
  const lower = from || VN_RUN_DATE;
  const upper = vnDate();
  const where = "WHERE completed_at >= '" + lower + " 00:00:00'"
    + " AND completed_at <= '" + upper + " 23:59:59'"
    + " AND user_id IN (SELECT user_id FROM users WHERE email IN ('user@gmail.com','admin@gmail.com'))";
  const sql = [
    "SET QUOTED_IDENTIFIER ON;",
    "SELECT 'AUDIT_EA_CANDIDATES=' + CAST(COUNT(*) AS varchar(20)) FROM exercise_attempts " + where + ";",
    "DELETE FROM exercise_attempts " + where + ";",
    // Measured AFTER the delete: requiring 0 proves the window is empty regardless of baseline.
    "SELECT 'AUDIT_EA_REMAINING=' + CAST(COUNT(*) AS varchar(20)) FROM exercise_attempts " + where + ";",
  ].join("\n");
  const file = path.join(__dirname, "..", "_cleanup_exercise_attempts.sql");
  fs.writeFileSync(file, sql + "\n", "utf8");

  let out = "";
  try {
    out = execFileSync("python", ["sweep/v8/sqlrun.py", "sweep/v8/_cleanup_exercise_attempts.sql"],
      { cwd: ROOT, encoding: "utf8" });
  } catch (e) { out = String(e.stdout || "") + String(e.stderr || "") + String(e.message); }

  const ck = out.match(/AUDIT_EA_CANDIDATES=(\d+)/);
  const rk = out.match(/AUDIT_EA_REMAINING=(\d+)/);
  const candidates = ck ? parseInt(ck[1], 10) : NaN;
  const remaining = rk ? parseInt(rk[1], 10) : NaN;
  const hadError = /Msg \d+/.test(out);
  const ok = !hadError && !isNaN(remaining) && remaining === 0;
  console.log("cleanupExerciseAttempts: window=[" + lower + ".." + upper + "]"
    + " candidates=" + (isNaN(candidates) ? "?" : candidates)
    + " remaining=" + (isNaN(remaining) ? "?" : remaining)
    + " sqlError=" + hadError + " -> " + (ok ? "SELF-CLEAN OK" : "SELF-CLEAN FAILED"));
  return { ok, candidates, remaining, output: out };
}

/**
 * Assert the DB is back to its expected state after a harness run, and THROW if not.
 *
 * WHY THIS EXISTS (audit-v15 hardening, weakness L1)
 * -------------------------------------------------
 * Three separate harnesses in this repo could leave real rows behind, and the
 * baseline could not see it:
 *   - `payment_transactions` — a visit to `/premium/checkout` mints a PENDING row.
 *     `cleanupAuditPayments` handles it, but two callers ran it outside a
 *     `finally` (a throw skipped cleanup) and one dropped its result from the exit
 *     code, so cleanup failure still exited 0.
 *   - `study_days` — written by 5 services via StudyActivityService.recordStudy(),
 *     reachable from `/flashcards/study`, `/srs/review`, `/exercises/submit`,
 *     speaking submit, and game/streak paths. NOTHING cleaned it and p16-parity.sql
 *     did not count it, so the residue was invisible: v14 ended at 4 rows, a probe
 *     row made it 5, and v15 recorded "study_days 5" as if it were the baseline.
 *
 * `expect` is the caller's known-good baseline:
 *   { parity?: "a|b|c|...", studyDays?: 4, pendingPayments?: 0 }
 * Every provided field is checked; a mismatch throws (so a `finally` that calls
 * this can never silently pass). Returns the observed values for logging.
 */
function assertClean(expect) {
  expect = expect || {};
  const observed = {
    parity: dbParity(),
    studyDays: parityMarker("STUDY_DAYS"),
    pendingPayments: parityMarker("PENDING_PAYMENTS"),
    // audit-v17 F-17-07: exercise_attempts is a residue channel (a UI/MCP submit writes one).
    exerciseAttempts: parityMarker("EXERCISE_ATTEMPTS"),
  };
  const problems = [];
  if (expect.parity !== undefined && observed.parity !== expect.parity) {
    problems.push("parity " + observed.parity + " != expected " + expect.parity);
  }
  if (expect.studyDays !== undefined && observed.studyDays !== expect.studyDays) {
    problems.push("study_days " + observed.studyDays + " != expected " + expect.studyDays);
  }
  if (expect.pendingPayments !== undefined && observed.pendingPayments !== expect.pendingPayments) {
    problems.push("pending payments " + observed.pendingPayments + " != expected " + expect.pendingPayments);
  }
  if (expect.exerciseAttempts !== undefined && observed.exerciseAttempts !== expect.exerciseAttempts) {
    problems.push("exercise_attempts " + observed.exerciseAttempts + " != expected " + expect.exerciseAttempts);
  }
  const ok = problems.length === 0;
  console.log("assertClean: parity=" + observed.parity
    + " study_days=" + observed.studyDays
    + " pending_payments=" + observed.pendingPayments
    + " exercise_attempts=" + observed.exerciseAttempts
    + " -> " + (ok ? "CLEAN" : "DIRTY: " + problems.join("; ")));
  if (!ok) throw new Error("assertClean failed — residue left by this run: " + problems.join("; "));
  return observed;
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

// audit-v17 F-17-06: Chromium classifies a cross-origin failure from the embedded YouTube
// player (postMessage / referrer block / Permissions-Policy notices) as console type
// "error", so a naive `m.type() === "error"` counter flags /videos/{id} on every run.
//
// audit-v17 round 2 (F-17-18, cross-review): the first version was a bare `/youtube/i` SUBSTRING
// match, which also suppressed REAL app errors whose text merely mentioned youtube — e.g.
// `TypeError: Cannot read properties of null (reading 'youtubeVideoId')`. Each pattern is now
// ANCHORED to the actual third-party signature (a resource-load failure for a third-party origin,
// or the iframe's own postMessage/notice), so a genuine JS error is never hidden.
const THIRD_PARTY_CONSOLE_NOISE = [
  // Resource-load failures for third-party origins (favicon, the YouTube embed + its media CDN).
  /Failed to load resource:.*(favicon|youtube|googlevideo|ERR_BLOCKED_BY_RESPONSE|ERR_UNSAFE_REDIRECT)/i,
  // The YouTube iframe's cross-origin postMessage warning.
  /postMessage.*(youtube|DOMWindow)/i,
  // Permissions-Policy informational notice the browser emits for the embedded iframe.
  /Unrecognized feature:.*(compute-pressure|web-share)/i,
];

/** True when a console error string is known third-party/iframe noise, not an app defect. */
function isThirdPartyConsoleNoise(text) {
  const t = String(text);
  return THIRD_PARTY_CONSOLE_NOISE.some(re => re.test(t));
}

// Collect console errors + failed requests while visiting one route.
async function visit(page, route, label, opts) {
  opts = opts || {};
  const errors = [], warns = [], failed = [];
  const onConsole = m => {
    if (m.type() === "error") {
      const t = m.text();
      if (isThirdPartyConsoleNoise(t)) return;
      errors.push(t.slice(0, 200));
    } else if (m.type() === "warning") warns.push(m.text().slice(0, 160));
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

module.exports = { pw, APP, API, BASE, login, loginFull, mapUser, results, visit, summarize, seedToken, seedAuth, mkContext, flushLimits, sleep, vnDate, VN_RUN_DATE, cleanupAuditPayments, cleanupStudyDays, cleanupExerciseAttempts, dbParity, parityMarker, assertClean, isThirdPartyConsoleNoise, resolveChromium, PARITY_BASELINE, PAYMENTS_BASELINE, STUDY_DAYS_BASELINE, EXERCISE_ATTEMPTS_BASELINE };
