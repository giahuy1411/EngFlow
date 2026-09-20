/**
 * audit-v11-full Phase 6 — performance probe.
 *
 * Method: median of >=3 runs per endpoint (P5: no optimisation claim without a before/after number).
 * Flushes rate_limit:* before each batch — otherwise the 100/min global bucket turns the last
 * measurements into 429s and silently inflates the median (the sweep/v10/perf-probe.js had no
 * flush logic at all; that is why this one does).
 *
 * Run: node sweep/v12/perf-probe.js
 */
const { execFileSync } = require("child_process");

const API = "http://localhost:8080";
const RUNS = 5;

function flushBuckets() {
  try {
    const out = execFileSync("docker", ["exec", "engflow-redis", "redis-cli", "--scan", "--pattern", "rate_limit:*"], { encoding: "utf8" }).trim();
    if (!out) return 0;
    const keys = out.split(/\r?\n/).filter(Boolean);
    if (keys.length) execFileSync("docker", ["exec", "engflow-redis", "redis-cli", "del", ...keys], { encoding: "utf8" });
    return keys.length;
  } catch (e) { return -1; }
}

async function login(email, password) {
  const r = await fetch(API + "/api/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password }) });
  if (!r.ok) return null;
  const j = await r.json();
  return j.token || j.data?.token;
}

async function timed(path, token) {
  const t0 = process.hrtime.bigint();
  const res = await fetch(API + path, { headers: token ? { Authorization: "Bearer " + token } : {} });
  await res.arrayBuffer();                       // consume the body: TTFB alone hides serialisation cost
  const ms = Number(process.hrtime.bigint() - t0) / 1e6;
  return { ms: +ms.toFixed(1), status: res.status };
}

function median(xs) {
  const s = [...xs].sort((a, b) => a - b);
  const m = Math.floor(s.length / 2);
  return s.length % 2 ? s[m] : +((s[m - 1] + s[m]) / 2).toFixed(1);
}

(async () => {
  const label = process.argv[2] || "run";
  const userToken = await login("user@gmail.com", "123456");
  const adminToken = await login("admin@gmail.com", "123456");
  if (!userToken || !adminToken) { console.log("ABORT: login failed"); process.exit(2); }

  const targets = [
    ["lesson list (page 0)",        "/api/lessons?page=0&size=10",                null],
    ["lesson list (page 5)",        "/api/lessons?page=5&size=10",                null],
    ["lesson detail",               "/api/lessons/41881",                         null],
    ["exercise list (lesson)",      "/api/lessons/41881/exercises",               null],
    ["streak snapshot",             "/api/streak/snapshot",                       userToken],
    ["vocabulary search",           "/api/vocabulary/search?keyword=the",         null],
    ["leaderboard",                 "/api/leaderboard?page=0&size=10",            null],
    ["decks (public)",              "/api/decks?page=0&size=10",                  null],
    ["admin exercise search q=the", "/api/admin/exercises?page=0&size=10&q=the",  adminToken],
    ["admin exercise list (no q)",  "/api/admin/exercises?page=0&size=10",        adminToken],
    ["admin lessons",               "/api/admin/lessons?page=0&size=10",          adminToken],
    ["admin users",                 "/api/admin/users?page=0&size=10",            adminToken],
    ["admin stats",                 "/api/admin/stats",                           adminToken],
    // v12 additions — endpoints v11 never measured (the zero-coverage controllers)
    ["user progress",               "/api/users/progress",                        userToken],
    ["dashboard stats",             "/api/dashboard/stats",                       userToken],
    ["game session (quiz)",         "/api/games/quiz/10006",                      userToken],
    ["srs stats",                   "/api/srs/stats",                             userToken],
    ["srs due (deck)",              "/api/srs/due/10006",                         userToken],
    ["flashcard status",            "/api/flashcards/status/10017",               userToken],
    ["speaking prompts (public)",   "/api/v1/speaking-prompts?page=0&size=10",    null],
    ["video lessons (public)",      "/api/v1/video-lessons?page=0&size=10",       null],
    ["payment status",              "/api/v1/payment/status",                     userToken],
  ];

  const results = [];
  for (const [name, path, token] of targets) {
    flushBuckets();
    const samples = [];
    let status = null;
    for (let i = 0; i < RUNS; i++) {
      const r = await timed(path, token);
      samples.push(r.ms);
      status = r.status;
      await new Promise((res) => setTimeout(res, 60));
    }
    results.push({ name, path, status, medianMs: median(samples), min: Math.min(...samples), max: Math.max(...samples), samples });
    console.log(String(status).padEnd(4) + " median " + String(median(samples)).padStart(7) + " ms   " + name);
  }

  const out = { label, runs: RUNS, at: new Date().toISOString(), results };
  require("fs").writeFileSync("sweep/v12/perf-" + label + ".json", JSON.stringify(out, null, 2));
  console.log("\nwrote sweep/v12/perf-" + label + ".json");
})();
