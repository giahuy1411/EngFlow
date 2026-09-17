/**
 * perf.js — Task 9 evidence: measured baseline latency for representative
 * endpoints. No claim of "optimised" without these numbers.
 *
 * Deliberately separates:
 *   - warm (median of N runs) from cold (first call)  -> AI latency includes
 *     model load, which is NOT an application regression (PLAN.md Phase 6).
 *   - read-only endpoints from ones that write.
 *
 * Reports p50/p95/min/max per endpoint plus the status distribution, so a 500
 * can never be silently averaged into a latency figure.
 */
const H = require("./lib.js");
const fs = require("fs");

const N = 7; // odd -> p50 is a real sample, not an average of two

const CASES = [
  // [label, method, path, as, body]
  // Paths verified against real controller mappings. Two earlier guesses were
  // wrong and produced misleading "no 2xx/3xx samples" rows:
  //   /api/admin/dashboard/stats -> 404, real admin stats is /api/admin/stats
  //   /api/exercises/grade       -> 403, real path is /api/lessons/{id}/exercises/grade
  ["GET public video lessons", "GET", "/api/v1/video-lessons", "anon"],
  ["GET lesson list (public)", "GET", "/api/lessons?page=0&size=20", "user"],
  ["GET lesson detail 445", "GET", "/api/lessons/445", "user"],
  ["GET admin exercises (no filter)", "GET", "/api/admin/exercises?page=0&size=20", "admin"],
  ["GET admin exercises (leading-wildcard q)", "GET", "/api/admin/exercises?q=the&page=0&size=20", "admin"],
  ["GET leaderboard", "GET", "/api/leaderboard", "user"],
  ["GET streak current", "GET", "/api/streak/current", "user"],
  ["GET vocabulary search", "GET", "/api/vocabulary/search?keyword=work", "anon"],
  ["GET admin stats", "GET", "/api/admin/stats", "admin"],
  ["GET user dashboard stats", "GET", "/api/dashboard/stats", "user"],
  ["GET decks list", "GET", "/api/decks", "user"],
  ["GET speaking prompts", "GET", "/api/v1/speaking-prompts", "user"],
  ["POST login (auth cost)", "POST", "/api/auth/login", "anon", { email: "user@gmail.com", password: "123456" }],
  ["POST enrich-word (AI, local Ollama)", "POST", "/api/ai/enrich-word", "user", { word: "resilient" }],
  ["POST grade exercise (lesson-scoped)", "POST", "/api/lessons/445/exercises/grade", "user", { exerciseId: 1, answer: "x" }],
];

function stats(arr) {
  if (!arr.length) return null;
  const s = [...arr].sort((a, b) => a - b);
  const p = q => s[Math.min(s.length - 1, Math.floor(q * s.length))];
  return {
    n: s.length, min: s[0], p50: p(0.5), p95: p(0.95), max: s[s.length - 1],
    mean: Math.round(s.reduce((a, b) => a + b, 0) / s.length),
  };
}

(async () => {
  const admin = await H.login("admin@gmail.com", "123456");
  const user = await H.login("user@gmail.com", "123456");

  const out = [];
  console.log("=== PERFORMANCE BASELINE (median of %d, live services) ===", N);
  console.log("endpoint".padEnd(44) + "status".padEnd(14) + "p50".padStart(7) + "p95".padStart(7) + "min".padStart(7) + "max".padStart(7));

  for (const [label, method, path, as, body] of CASES) {
    const token = as === "admin" ? admin : as === "user" ? user : null;
    const lat = [];
    const codes = {};
    for (let i = 0; i < N; i++) {
      H.flushLimits(true);
      const t0 = Date.now();
      let status = 0;
      try {
        const r = await fetch(H.BASE + path, {
          method,
          headers: {
            ...(body ? { "Content-Type": "application/json" } : {}),
            ...(token ? { Authorization: "Bearer " + token } : {}),
          },
          body: body ? JSON.stringify(body) : undefined,
        });
        status = r.status;
        await r.text();
      } catch (e) { status = -1; }
      const ms = Date.now() - t0;
      // a failed call's latency is meaningless; keep it out of the stats
      if (status >= 200 && status < 400) lat.push(ms);
      codes[status] = (codes[status] || 0) + 1;
    }
    const st = stats(lat);
    const codeStr = Object.entries(codes).map(([k, v]) => k + "x" + v).join(",");
    console.log(label.padEnd(44) + codeStr.padEnd(14)
      + (st ? String(st.p50).padStart(7) + String(st.p95).padStart(7) + String(st.min).padStart(7) + String(st.max).padStart(7)
        : "no 2xx/3xx samples".padStart(28)));
    out.push({ label, method, path, as, statuses: codes, latency: st, samples: lat });
  }

  // DB logical reads for the hot admin list path, measured separately.
  // perf-reads.sql excludes the audit's own statements from the plan-cache
  // query, otherwise the report ends up measuring the audit harness.
  console.log("\n=== SQL LOGICAL READS (admin exercise list) ===");
  try {
    const sql = require("child_process").execSync(
      "python sweep/v8/sqlrun.py sweep/v8/perf-reads.sql",
      { cwd: require("path").join(__dirname, "..", ".."), encoding: "utf8" });
    fs.writeFileSync("perf-reads-out.txt", sql);
    console.log(sql.trim());
  } catch (e) {
    console.log("perf-reads failed: " + e.message);
  }

  fs.writeFileSync("perf.json", JSON.stringify({ n: N, cases: out }, null, 1));
  console.log("\nwrote perf.json");
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
