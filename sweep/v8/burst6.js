/**
 * burst6 — measure RateLimitFilter BUCKET ROUTING for every prefix family.
 *
 * Method: RateLimitFilter is @Order(1) and runs BEFORE security and the
 * controller, so hitting "<prefix>/__bucket_probe_nonexistent" still increments
 * the bucket counter while producing a 404/401 with ZERO business side effects.
 * That lets us measure routing without firing real Ollama / payment / upload work.
 *
 * We record (a) which redis rate_limit keys appear and (b) where the first 429
 * lands, then compare against the thresholds declared in RateLimitFilter.java.
 */
const { execSync } = require("child_process");
const BASE = "http://localhost:8080";

const redis = (cmd) => execSync("docker exec engflow-redis redis-cli " + cmd, { encoding: "utf8" }).trim();

function flush() {
  try {
    const k = redis("--scan --pattern rate_limit:*");
    if (!k) return;
    const list = k.split(/\r?\n/).filter(Boolean).map(x => '"' + x + '"').join(" ");
    execSync("docker exec engflow-redis redis-cli del " + list, { encoding: "utf8" });
  } catch (e) { console.log("  flush warn: " + e.message.slice(0, 60)); }
}

function bucketsNow() {
  try {
    const k = redis("--scan --pattern rate_limit:*");
    if (!k) return [];
    return k.split(/\r?\n/).filter(Boolean).map(x => x.split(":").pop());
  } catch (e) { return ["<err " + e.message.slice(0, 30) + ">"]; }
}

async function hit(prefix, n, method) {
  const codes = [];
  for (let i = 0; i < n; i++) {
    const r = await fetch(BASE + prefix + "/__bucket_probe_nonexistent", {
      method: method || "GET", headers: { "Content-Type": "application/json" },
      body: (method === "POST") ? "{}" : undefined
    });
    codes.push(r.status);
    await new Promise(res => setTimeout(res, 12));
  }
  const first429 = codes.findIndex(c => c === 429);
  const hist = {}; for (const c of codes) hist[c] = (hist[c] || 0) + 1;
  return { first429: first429 < 0 ? null : first429 + 1, hist };
}

const CASES = [
  // label,                          prefix to probe,                              method, N
  ["auth bucket (limit 20)",         "/api/auth/login",                            "POST", 24],
  ["mail bucket (limit 5)",          "/api/auth/forgot-password",                  "POST", 9],
  ["ai bucket (limit 10)",           "/api/ai",                                    "POST", 14],
  ["order bucket (limit 10)",        "/api/v1/payment/create-order",               "POST", 14],
  ["upload bucket (limit 15)",       "/api/admin/upload",                          "POST", 19],
  ["upload via /submissions",        "/api/v1/speaking-submissions",               "POST", 19],
  ["GLOBAL fallback (limit 100)",    "/api/lessons",                               "GET", 105],
  // --- the interesting ones: AI work living OUTSIDE the /api/ai/ prefix ---
  ["AI: admin exercises ai-gen",     "/api/admin/exercises/ai/generate-async",     "POST", 14],
  ["AI: speaking-prompts ai-gen",    "/api/v1/admin/speaking-prompts/ai-generate", "POST", 14],
  ["AI: video-prompts ai-gen ALIAS", "/api/v1/admin/video-prompts/ai-generate",   "POST", 14],
  ["AI: transcript translate",       "/api/v1/admin/video-lessons/translate-transcript", "POST", 14],
  ["AI: backfill-answers",           "/api/admin/exercises/ai/backfill-answers",   "POST", 14],
];

(async () => {
  console.log("=== BUCKET ROUTING MATRIX (side-effect-free probe path) ===");
  console.log("RateLimitFilter thresholds: auth 20 · mail 5 · ai 10 · upload 15 · order 10 · global 100\n");
  const out = [];
  for (const [label, prefix, method, n] of CASES) {
    flush();
    const r = await hit(prefix, n, method);
    const bk = bucketsNow();
    const row = { label, prefix, method, n, first429: r.first429, hist: r.hist, buckets: bk };
    out.push(row);
    console.log("  " + label.padEnd(34) + " prefix=" + prefix.padEnd(46) +
      " first429=" + String(r.first429 === null ? "never" : r.first429).padStart(5) +
      "  buckets=" + JSON.stringify(bk));
  }
  flush();
  console.log("\n(rate-limit keys cleared)");
  require("fs").writeFileSync("burst6.json", JSON.stringify(out, null, 1));
  console.log("wrote burst6.json");
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
