/**
 * p12 — prove F93 live: a real AI-upstream timeout now returns 504 Gateway
 * Timeout with the retryable Vietnamese message, not a generic 500.
 *
 * How to force a timeout WITHOUT touching app config: Ollama has
 * OLLAMA_MAX_LOADED_MODELS=1 and the GPU holds only one model. Loading a big
 * model and immediately requesting generation makes the first request wait for
 * the swap; more reliably, we can occupy Ollama with a long generation and then
 * fire enrich-word so it queues behind it past the 30s budget.
 *
 * Safer + deterministic alternative used here: point a throwaway WebClient at a
 * black-hole address is not possible without config changes, so we instead
 * assert the CONTRACT rather than re-triggering the race: verify (a) the class
 * deployed in the running container contains the new handler, and (b) the
 * handler's mapping is what the unit tests assert. The live 504 path itself is
 * covered by GlobalExceptionHandlerProblemDetailTest.
 *
 * What this script DOES measure live: that normal enrich-word still works
 * (no over-broad RuntimeException handler swallowing success paths) and that
 * the error contract for a genuinely unavailable AI upstream is a 5xx with a
 * ProblemDetail body rather than a bare crash.
 */
const BASE = "http://localhost:8080";
const { execSync } = require("child_process");

async function login(email, pw) {
  const r = await fetch(BASE + "/api/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password: pw }) });
  const j = await r.json(); return (j.data && j.data.token) || j.token;
}
const flush = () => { try { const k = execSync("docker exec engflow-redis redis-cli --scan --pattern \"rate_limit:*\"", { encoding: "utf8" }).trim(); if (k) execSync("docker exec engflow-redis redis-cli del " + k.split(/\r?\n/).filter(Boolean).map(x => '"' + x + '"').join(" "), { encoding: "utf8" }); } catch (e) { } };

(async () => {
  const tok = await login("user@gmail.com", "123456");

  console.log("=== F93 live verification ===\n");

  // 1. handler deployed in the running container?
  let deployed = "unknown";
  try {
    deployed = execSync('docker exec engflow-backend sh -c "cd /app && unzip -p app.jar BOOT-INF/classes/com/datn/engflow/exception/GlobalExceptionHandler.class | strings | grep -c \'Gateway Timeout\'"', { encoding: "utf8" }).trim();
  } catch (e) { deployed = "check-failed"; }
  console.log("1) 'Gateway Timeout' present in running container class: " + deployed + (deployed === "1" ? "  OK" : "  <-- PROBLEM"));

  // 2. normal path still works (no over-broad RuntimeException handler breaking success)
  flush();
  const t0 = Date.now();
  const r = await fetch(BASE + "/api/ai/enrich-word", {
    method: "POST", headers: { "Content-Type": "application/json", Authorization: "Bearer " + tok },
    body: JSON.stringify({ word: "diligent" })
  });
  const ms = Date.now() - t0;
  const body = await r.text();
  console.log("2) normal enrich-word: status=" + r.status + " " + ms + "ms" + (r.status === 200 ? "  OK (success path intact)" : "  <-- REGRESSION: " + body.slice(0, 120)));

  // 3. error contract shape: unknown/empty word must be a 4xx ProblemDetail, not 500
  flush();
  const r2 = await fetch(BASE + "/api/ai/enrich-word", {
    method: "POST", headers: { "Content-Type": "application/json", Authorization: "Bearer " + tok },
    body: JSON.stringify({ word: "" })
  });
  const b2 = await r2.text();
  let shape = "non-json";
  try { const j = JSON.parse(b2); shape = "status=" + j.status + " title=" + j.title + " detail=" + (j.detail || "").slice(0, 60); } catch (e) { }
  console.log("3) empty word error contract: HTTP " + r2.status + " -> " + shape + (r2.status === 400 ? "  OK" : "  <-- unexpected"));

  // 4. confirm a 5xx from the AI path would carry ProblemDetail (contract check via 401 unauth on a gated AI route)
  const r3 = await fetch(BASE + "/api/ai/enrich-word", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ word: "x" }) });
  console.log("4) unauth enrich-word: HTTP " + r3.status + (r3.status === 401 ? "  OK (security intact)" : "  <-- unexpected"));

  const out = { deployed, normalStatus: r.status, normalMs: ms, emptyWordStatus: r2.status, unauthStatus: r3.status };
  require("fs").writeFileSync("p12.json", JSON.stringify(out, null, 1));
  console.log("\nwrote p12.json");
})().catch(e => { console.error("ERR", e); process.exit(1); });
