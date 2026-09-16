/**
 * p11 — F93: is POST /api/ai/enrich-word intermittently 500?
 *
 * Observed in p3a run 1: `FAIL ai enrich-word ... got=500 want=200
 * {"detail":"Đã xảy ra lỗi hệ thống..."}` while run 2 passed, i.e. FLAKY.
 * Backend log showed `AiVocabService: OpenRouter request failed` +
 * `GlobalExceptionHandler: Internal server error` at AiVocabController.enrichWord:95.
 * Note the class/message names are legacy: openrouter.base-url is set to the LOCAL
 * Ollama (host.docker.internal:11434/v1) and the key is ignored by Ollama, so this
 * is really a local-model call.
 *
 * Method: N sequential enrich-word calls, count status codes, and time them —
 * a timeout would show as a latency spike.
 */
const BASE = "http://localhost:8080";

async function login(email, pw) {
  const r = await fetch(BASE + "/api/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password: pw }) });
  const j = await r.json(); return (j.data && j.data.token) || j.token;
}

const WORDS = ["diligent", "resilient", "meticulous", "pragmatic", "eloquent",
  "tenacious", "ambiguous", "candid", "lucid", "novel"];

(async () => {
  const { execSync } = require("child_process");
  const flush = () => { try { const k = execSync("docker exec engflow-redis redis-cli --scan --pattern \"rate_limit:*\"", { encoding: "utf8" }).trim(); if (k) execSync("docker exec engflow-redis redis-cli del " + k.split(/\r?\n/).filter(Boolean).map(x => '"' + x + '"').join(" "), { encoding: "utf8" }); } catch (e) { } };
  const tok = await login("user@gmail.com", "123456");
  const N = Number(process.argv[2] || 10);
  console.log("=== enrich-word reliability: " + N + " sequential calls ===\n");
  const results = [];
  for (let i = 0; i < N; i++) {
    flush(); // clear :ai so we measure the app, not our own throttling
    const w = WORDS[i % WORDS.length];
    const t0 = Date.now();
    let status = 0, body = "";
    try {
      const r = await fetch(BASE + "/api/ai/enrich-word", {
        method: "POST", headers: { "Content-Type": "application/json", Authorization: "Bearer " + tok },
        body: JSON.stringify({ word: w })
      });
      status = r.status; body = await r.text();
    } catch (e) { status = -1; body = e.message; }
    const ms = Date.now() - t0;
    results.push({ i, word: w, status, ms, body: body.slice(0, 160) });
    console.log("  #" + String(i + 1).padStart(2) + " " + w.padEnd(12) + " status=" + String(status).padEnd(5) + ms + "ms" + (status !== 200 ? "  <- " + body.slice(0, 110) : ""));
  }
  const h = {}; for (const r of results) h[r.status] = (h[r.status] || 0) + 1;
  const times = results.map(r => r.ms).sort((a, b) => a - b);
  console.log("\nstatus histogram:", JSON.stringify(h));
  console.log("latency ms: min=" + times[0] + " med=" + times[Math.floor(times.length / 2)] + " max=" + times[times.length - 1]);
  require("fs").writeFileSync("p11.json", JSON.stringify(results, null, 1));
  console.log("wrote p11.json");
})().catch(e => { console.error("ERR", e); process.exit(1); });
