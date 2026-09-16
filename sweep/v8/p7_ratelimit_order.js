/**
 * F-R1-RATELIMIT-ORDER investigation.
 *
 * Observation: POST /api/ai/enrich-word (authenticated route) returns 401 and
 * creates NO redis rate_limit key, while POST /api/auth/login (permitAll)
 * returns 400 and DOES create the :auth key.
 *
 * Hypothesis: RateLimitFilter is annotated @Component @Order(1). Spring Boot
 * registers the security filter chain at SecurityProperties.DEFAULT_FILTER_ORDER
 * = -100, so @Order(1) places RateLimitFilter AFTER Spring Security. Requests
 * rejected by the security chain (401/403) therefore never reach the limiter.
 *
 * Test: flood an AUTHENTICATED endpoint anonymously and count 429s. If the
 * limiter is truly downstream of security, the flood never 429s and never
 * writes a bucket key. Also test the same endpoint WITH a valid token, which
 * should reach the limiter and 429 at the declared threshold.
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

async function flood(path, tok, n, method) {
  const codes = [];
  for (let i = 0; i < n; i++) {
    const r = await fetch(BASE + path, {
      method: method || "POST",
      headers: { "Content-Type": "application/json", ...(tok ? { Authorization: "Bearer " + tok } : {}) },
      body: (method === "GET") ? undefined : "{}"
    });
    codes.push(r.status);
  }
  const h = {}; for (const c of codes) h[c] = (h[c] || 0) + 1;
  const first429 = codes.findIndex(c => c === 429);
  return { hist: h, first429: first429 < 0 ? null : first429 + 1 };
}

(async () => {
  const u = await login("user@gmail.com", "123456");
  console.log("user token acquired:", !!u);

  console.log("\n=== T1: ANONYMOUS flood on an AUTHENTICATED route (AI) ===");
  flush();
  {
    const r = await flood("/api/ai/enrich-word", null, 60, "POST");
    await new Promise(x => setTimeout(x, 250));
    console.log("  60 anonymous POST /api/ai/enrich-word -> hist=" + JSON.stringify(r.hist) +
      " first429=" + r.first429 + "  keys=" + JSON.stringify(keys()));
  }

  console.log("\n=== T2: AUTHENTICATED flood on the same route (control) ===");
  flush();
  {
    const r = await flood("/api/ai/enrich-word", u, 14, "POST");
    await new Promise(x => setTimeout(x, 250));
    console.log("  14 authed POST /api/ai/enrich-word -> hist=" + JSON.stringify(r.hist) +
      " first429=" + r.first429 + "  keys=" + JSON.stringify(keys()));
  }

  console.log("\n=== T3: ANONYMOUS flood on an AUTHENTICATED route (order) ===");
  flush();
  {
    const r = await flood("/api/v1/payment/create-order", null, 60, "POST");
    await new Promise(x => setTimeout(x, 250));
    console.log("  60 anonymous POST create-order -> hist=" + JSON.stringify(r.hist) +
      " first429=" + r.first429 + "  keys=" + JSON.stringify(keys()));
  }

  console.log("\n=== T4: ANONYMOUS flood on an ADMIN route (upload family) ===");
  flush();
  {
    const r = await flood("/api/admin/audio-upload", null, 60, "POST");
    await new Promise(x => setTimeout(x, 250));
    console.log("  60 anonymous POST /api/admin/audio-upload -> hist=" + JSON.stringify(r.hist) +
      " first429=" + r.first429 + "  keys=" + JSON.stringify(keys()));
  }

  console.log("\n=== T5: GLOBAL fallback on a permitAll route (control, limit 100) ===");
  flush();
  {
    const r = await flood("/api/lessons?page=0&size=1", null, 105, "GET");
    await new Promise(x => setTimeout(x, 250));
    console.log("  105 anonymous GET /api/lessons -> hist=" + JSON.stringify(r.hist) +
      " first429=" + r.first429 + "  keys=" + JSON.stringify(keys()));
  }

  flush();
  console.log("\n(all rate_limit keys cleared)");
})().catch(e => { console.error("ERR", e); process.exit(1); });
