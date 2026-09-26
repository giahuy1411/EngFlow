/**
 * Performance probe, measured BEFORE any optimisation.
 *
 * Constitution P5: no optimisation without a number. This file only MEASURES.
 *
 * Method (inherited from sweep/v12/perf-probe.js, which inherited it from v11):
 *   - each endpoint is called RUNS=5 times and the MEDIAN is reported, not the mean —
 *     one GC pause must not be allowed to move the headline number.
 *   - the response body is fully read (res.arrayBuffer()) before the clock stops, so a
 *     fast TTFB cannot hide serialisation cost.
 *   - rate_limit:* is flushed BEFORE each endpoint's batch. The global bucket is
 *     100/min/IP; without the flush the last runs of a long sweep turn into 429s and the
 *     median is silently inflated (that was the v10 probe's bug).
 *   - docker is invoked with execFileSync + argv arrays: a Redis key is data and must
 *     never be interpolated into a shell string.
 *
 * Delta vs the v12 probe: v12 measured 22 endpoints once; this re-measures all of them plus
 * more of the v1 API surface (streak current/history, auth/me, lesson exercise content,
 * decks/my, admin speaking-prompts, video attempts, lesson submissions, vocabulary list,
 * game memory/mixed) so the BEFORE table is wider than the one it will be compared to.
 *
 * Run from repo root:  node sweep/harness/perf-probe.js [--audit <name>] [--out <dir>]
 * Out:                 <evidence>/perf-<label>.json
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const API = "http://localhost:8080";
const RUNS = 5;

function flushBuckets() {
  try {
    const out = execFileSync("docker",
      ["exec", "engflow-redis", "redis-cli", "--scan", "--pattern", "rate_limit:*"],
      { encoding: "utf8" }).trim();
    if (!out) return 0;
    const keys = out.split(/\r?\n/).filter(Boolean);
    if (keys.length) execFileSync("docker", ["exec", "engflow-redis", "redis-cli", "del", ...keys], { encoding: "utf8" });
    return keys.length;
  } catch (e) { return -1; }
}

async function login(email, password) {
  const r = await fetch(API + "/api/auth/login", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  if (!r.ok) return null;
  const j = await r.json();
  return j.data?.token || j.token || null;
}

async function timed(pathname, token) {
  const t0 = process.hrtime.bigint();
  const res = await fetch(API + pathname, { headers: token ? { Authorization: "Bearer " + token } : {} });
  const buf = await res.arrayBuffer();            // consume the body before stopping the clock
  const ms = Number(process.hrtime.bigint() - t0) / 1e6;
  return { ms: +ms.toFixed(1), status: res.status, bytes: buf.byteLength };
}

function median(xs) {
  const s = [...xs].sort((a, b) => a - b);
  const m = Math.floor(s.length / 2);
  return s.length % 2 ? s[m] : +((s[m - 1] + s[m]) / 2).toFixed(1);
}

(async () => {
  const label = process.argv[2] || "before";
  const userToken = await login("user@gmail.com", "123456");
  const adminToken = await login("admin@gmail.com", "123456");
  if (!userToken || !adminToken) { console.log("ABORT: login failed"); process.exit(2); }

  // [name, path, token, tag]  — tag groups the endpoint for the report
  const targets = [
    // --- core learner surface (v11 baseline, re-measured) ---
    ["lesson list (page 0)",        "/api/lessons?page=0&size=10",                null,       "learner"],
    ["lesson list (page 5)",        "/api/lessons?page=5&size=10",                null,       "learner"],
    ["lesson detail",               "/api/lessons/41881",                         null,       "learner"],
    ["exercise list (lesson)",      "/api/lessons/41881/exercises",               null,       "learner"],
    ["lesson exercise content",     "/api/lessons/41881/exercises/content",       null,       "learner"],
    ["vocabulary search",           "/api/vocabulary/search?keyword=the",         null,       "learner"],
    ["vocabulary list (auth)",      "/api/vocabulary?page=0&size=10",             userToken,  "learner"],
    ["leaderboard",                 "/api/leaderboard?page=0&size=10",            null,       "learner"],
    ["decks (public)",              "/api/decks?page=0&size=10",                  null,       "learner"],
    ["decks (my)",                  "/api/decks/my",                              userToken,  "learner"],
    ["streak snapshot",             "/api/streak/snapshot",                       userToken,  "learner"],
    ["streak current",              "/api/streak/current",                        userToken,  "learner"],
    ["streak history",              "/api/streak/history",                        userToken,  "learner"],
    ["auth me",                     "/api/auth/me",                               userToken,  "learner"],
    // --- the 9 endpoints the prior audit (v12) added; re-measured here ---
    ["user progress",               "/api/users/progress",                        userToken,  "v12-new"],
    ["dashboard stats",             "/api/dashboard/stats",                       userToken,  "v12-new"],
    ["game session (quiz)",         "/api/games/quiz/10006",                      userToken,  "v12-new"],
    ["srs stats",                   "/api/srs/stats",                             userToken,  "v12-new"],
    ["srs due (deck)",              "/api/srs/due/10006",                         userToken,  "v12-new"],
    ["flashcard status",            "/api/flashcards/status/10017",               userToken,  "v12-new"],
    ["speaking prompts (public)",   "/api/v1/speaking-prompts?page=0&size=10",    null,       "v12-new"],
    ["video lessons (public)",      "/api/v1/video-lessons?page=0&size=10",       null,       "v12-new"],
    ["payment status",              "/api/v1/payment/status",                     userToken,  "v12-new"],
    // --- rest of the v1 / gamification surface ---
    ["game session (memory)",       "/api/games/memory/10006",                    userToken,  "gamification"],
    ["game session (mixed)",        "/api/games/mixed/10006",                     userToken,  "gamification"],
    ["speaking prompts (admin)",    "/api/v1/admin/speaking-prompts?page=0&size=10", adminToken, "gamification"],
    ["video attempts (my)",         "/api/v1/video-attempts",                     userToken,  "gamification"],
    ["lesson submissions (my)",     "/api/lesson-submissions/my/lesson/41881/skill/SPEAKING", userToken, "gamification"],
    ["video lessons (admin)",       "/api/v1/admin/video-lessons?page=0&size=10",  adminToken, "gamification"],
    // --- admin surface ---
    ["admin stats",                 "/api/admin/stats",                           adminToken, "admin"],
    ["admin lessons",               "/api/admin/lessons?page=0&size=10",          adminToken, "admin"],
    ["admin users",                 "/api/admin/users?page=0&size=10",            adminToken, "admin"],
    ["admin exercise list (no q)",  "/api/admin/exercises?page=0&size=10",        adminToken, "admin"],
    ["admin exercise search q=the", "/api/admin/exercises?page=0&size=10&q=the",  adminToken, "admin"],
  ];

  const results = [];
  for (const [name, p, token, tag] of targets) {
    flushBuckets();
    const samples = [];
    let status = null, bytes = null;
    for (let i = 0; i < RUNS; i++) {
      const r = await timed(p, token);
      samples.push(r.ms);
      status = r.status;
      bytes = r.bytes;
      await new Promise((res) => setTimeout(res, 60));
    }
    const med = median(samples);
    results.push({
      name, path: p, tag, status, bytes,
      medianMs: med, min: Math.min(...samples), max: Math.max(...samples), samples,
    });
    console.log(String(status).padEnd(4) + " median " + String(med).padStart(7) + " ms  " +
      String(bytes).padStart(7) + " B   " + name);
  }

  const out = {
    label, runs: RUNS, at: new Date().toISOString(), api: API,
    endpointCount: results.length,
    results,
  };
  // audit-v14 D3 / audit-v15 F-15-13: write beside THIS script via the shared config,
  // never into a hardcoded old-version dir (writing to sweep/v13 mislabelled the artifact
  // and was lost when that dir was deleted at cleanup).
  const dir = require("./_config.js").OUT;
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, "perf-" + label + ".json"), JSON.stringify(out, null, 2));
  console.log("\nwrote " + path.join(dir, "perf-" + label + ".json") + " (" + results.length + " endpoints)");
})();
