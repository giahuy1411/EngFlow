/**
 * p8 — bucket-routing coverage for EXPENSIVE endpoints.
 *
 * Finding under test: RateLimitFilter picks the :ai bucket solely by the literal
 * prefix `/api/ai/`. Every endpoint that actually calls Ollama but lives under a
 * different prefix therefore falls through to the 100/min :global bucket instead
 * of the intended 10/min :ai bucket. Same defect CLASS as the already-fixed F83
 * (the :order bucket pointed at paths no controller served).
 *
 * Method: RateLimitFilter runs before the DispatcherServlet, so appending a
 * trailing `__probe_<runid>` segment still selects the same bucket (prefix match)
 * while producing a 404 from the controller — ZERO business side effects, no
 * Ollama call, no payment, no upload. A valid ADMIN token is required so the
 * request is not short-circuited by Spring Security (see p7 report).
 */
const { execSync } = require("child_process");
const BASE = "http://localhost:8080";
const redis = (c) => execSync("docker exec engflow-redis redis-cli " + c, { encoding: "utf8" }).trim();
const keys = () => redis("--scan --pattern rate_limit:*").split(/\r?\n/).filter(Boolean);
const flush = () => {
  const k = keys();
  if (k.length) execSync("docker exec engflow-redis redis-cli del " + k.map(x => '"' + x + '"').join(" "), { encoding: "utf8" });
};

async function login(email, pw) {
  const r = await fetch(BASE + "/api/auth/login", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password: pw })
  });
  const j = await r.json();
  return (j.data && j.data.token) || j.token;
}

const TAG = "/__probe_" + Date.now();

// label, method, prefix, declared-intent-bucket, expected limit
const CASES = [
  // --- control: the literal /api/ai/ prefix really does select :ai ---
  ["CONTROL /api/ai/** (declared :ai 10)",        "POST", "/api/ai/enrich-word", "ai", 10],
  // --- endpoints that ACTUALLY call Ollama, but off-prefix ---
  ["Ollama: admin exercises ai/generate-async",   "POST", "/api/admin/exercises/ai/generate-async", "AI?", 10],
  ["Ollama: admin exercises ai/generate",         "POST", "/api/admin/exercises/ai/generate", "AI?", 10],
  ["Ollama: admin exercises ai/generate-all",     "POST", "/api/admin/exercises/ai/generate-all", "AI?", 10],
  ["Ollama: admin exercises ai/generate-batch",   "POST", "/api/admin/exercises/ai/generate-batch", "AI?", 10],
  ["Ollama: admin exercises ai/validate",         "POST", "/api/admin/exercises/ai/validate", "AI?", 10],
  ["Ollama: backfill-answers",                    "POST", "/api/admin/exercises/ai/backfill-answers", "AI?", 10],
  ["Ollama: speaking-prompts ai-generate",        "POST", "/api/v1/admin/speaking-prompts/ai-generate", "AI?", 10],
  ["Ollama: speaking-prompts ai-generate-full",   "POST", "/api/v1/admin/speaking-prompts/ai-generate-full", "AI?", 10],
  ["Ollama: video-prompts ai-generate (ALIAS)",   "POST", "/api/v1/admin/video-prompts/ai-generate", "AI?", 10],
  ["Ollama: video-lessons translate-transcript",  "POST", "/api/v1/admin/video-lessons/translate-transcript", "AI?", 10],
  ["Ollama: video-lessons fetch-youtube",         "POST", "/api/v1/admin/video-lessons/fetch-youtube", "AI?", 10],
  ["Ollama: video-attempts ai-grade",             "POST", "/api/v1/admin/video-attempts/1/ai-grade", "AI?", 10],
  // --- endpoints that WRITE FILES / call Cloudinary, off the upload prefixes ---
  ["Cloud: auth avatar/upload",                   "POST", "/api/auth/avatar/upload", "upload 15", 15],
  ["Upload: video-lessons/upload",                "POST", "/api/v1/admin/video-lessons/upload", "upload 15", 15],
  // --- control: a declared upload prefix ---
  ["CONTROL /api/admin/audio-upload (upload 15)", "POST", "/api/admin/audio-upload", "upload", 15],
];

(async () => {
  const admin = await login("admin@gmail.com", "123456");
  console.log("admin token:", !!admin);
  console.log("\nbucket key observed per prefix (RateLimitFilter.java:47-75):\n");
  const out = [];
  for (const [label, method, prefix, declared, lim] of CASES) {
    flush();
    const r = await fetch(BASE + prefix + TAG, {
      method, headers: { "Content-Type": "application/json", Authorization: "Bearer " + admin },
      body: method === "GET" ? undefined : "{}"
    });
    await new Promise(x => setTimeout(x, 120));
    const k = keys();
    const bucket = k.length ? k[0].split(":").pop() : "<none>";
    out.push({ label, method, prefix, declared, observedBucket: bucket, status: r.status });
    console.log("  " + label.padEnd(46) + " -> :" + String(bucket).padEnd(9) +
      " declared=" + String(declared).padEnd(10) + " status=" + r.status);
  }
  flush();
  require("fs").writeFileSync("p8.json", JSON.stringify(out, null, 1));
  console.log("\nwrote p8.json   (rate-limit keys cleared)");
})().catch(e => { console.error("ERR", e); process.exit(1); });
