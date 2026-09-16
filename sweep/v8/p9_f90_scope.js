/**
 * p9 — honest scope determination for F90 (filter ordering) and F91/F92 (bucket routing).
 *
 * F90 asks: does the ordering (@Order(1) vs security -100) create a REAL bypass?
 *
 * Reasoning to test: a request rejected by Spring Security does no business work,
 * so failing to charge its budget is harmless. The bypass only matters where the
 * limiter is the ONLY thing standing between an unauthenticated caller and an
 * expensive operation — i.e. permitAll endpoints. So we must measure, per bucket:
 *   (a) permitAll endpoints  -> does the bucket still fire?  (if YES, F90 has no impact there)
 *   (b) authenticated endpoints -> only reachable with a token, where T2 already proved the bucket fires
 *
 * Side effects: auth/mail floods use EMPTY bodies, which fail validation before any
 * real work (no email sent, no user created). The limiter runs before the controller.
 */
const { execSync } = require("child_process");
const BASE = "http://localhost:8080";
const redis = (c) => execSync("docker exec engflow-redis redis-cli " + c, { encoding: "utf8" }).trim();
const keys = () => redis("--scan --pattern rate_limit:*").split(/\r?\n/).filter(Boolean);
const flush = () => {
  const k = keys();
  if (k.length) execSync("docker exec engflow-redis redis-cli del " + k.map(x => '"' + x + '"').join(" "), { encoding: "utf8" });
};

async function flood(method, path, n, body, tok) {
  const codes = [];
  for (let i = 0; i < n; i++) {
    const r = await fetch(BASE + path, {
      method,
      headers: { "Content-Type": "application/json", ...(tok ? { Authorization: "Bearer " + tok } : {}) },
      body: method === "GET" ? undefined : (body === undefined ? "{}" : body)
    });
    codes.push(r.status);
    await new Promise(x => setTimeout(x, 10));
  }
  const h = {}; for (const c of codes) h[c] = (h[c] || 0) + 1;
  const f = codes.findIndex(c => c === 429);
  return { hist: h, first429: f < 0 ? null : f + 1 };
}

(async () => {
  console.log("=== F90 SCOPE: do permitAll buckets still fire? ===");
  console.log("(nếu CÓ → F90 không tạo bypass ở nhóm permitAll; mọi endpoint authenticated đã được T2 chứng minh limiter vẫn chạy)\n");

  // :auth 20 — /api/auth/login is permitAll
  flush();
  let r = await flood("POST", "/api/auth/login", 24, JSON.stringify({ email: "x@x.com", password: "wrong" }));
  await new Promise(x => setTimeout(x, 200));
  console.log("  :auth  24x POST /api/auth/login (permitAll)         hist=" + JSON.stringify(r.hist) +
    " first429=" + r.first429 + "  keys=" + JSON.stringify(keys()));

  // :mail 5 — /api/auth/forgot-password is permitAll
  flush();
  r = await flood("POST", "/api/auth/forgot-password", 9, JSON.stringify({ email: "definitely-not-a-user@example.invalid" }));
  await new Promise(x => setTimeout(x, 200));
  console.log("  :mail  9x POST /api/auth/forgot-password (permitAll) hist=" + JSON.stringify(r.hist) +
    " first429=" + r.first429 + "  keys=" + JSON.stringify(keys()));

  // :mail via reset-password, also permitAll
  flush();
  r = await flood("POST", "/api/auth/reset-password", 9, JSON.stringify({ token: "bogus", newPassword: "x" }));
  await new Promise(x => setTimeout(x, 200));
  console.log("  :mail  9x POST /api/auth/reset-password (permitAll)  hist=" + JSON.stringify(r.hist) +
    " first429=" + r.first429 + "  keys=" + JSON.stringify(keys()));

  // webhook/sepay is permitAll POST -> which bucket?
  flush();
  {
    const resp = await fetch(BASE + "/api/webhook/sepay", { method: "POST", headers: { "Content-Type": "application/json" }, body: "{}" });
    await new Promise(x => setTimeout(x, 200));
    console.log("  webhook /api/webhook/sepay (permitAll POST) status=" + resp.status + " keys=" + JSON.stringify(keys()));
  }

  // permitAll GET static/media surface -> which bucket?
  flush();
  {
    for (const p of ["/api/resources/__probe_none.bin", "/api/v1/media/__probe_none.bin"]) {
      await fetch(BASE + p);
    }
    await new Promise(x => setTimeout(x, 200));
    console.log("  permitAll GET /api/resources + /api/v1/media keys=" + JSON.stringify(keys()));
  }

  // anonymous flood on an AUTHENTICATED expensive endpoint (the F90 case)
  flush();
  r = await flood("POST", "/api/admin/exercises/ai/generate-async", 60);
  await new Promise(x => setTimeout(x, 200));
  console.log("  [F90] 60x ANON POST ai/generate-async (auth-required) hist=" + JSON.stringify(r.hist) +
    " first429=" + r.first429 + "  keys=" + JSON.stringify(keys()) + "   <- no bucket, but no work done either");

  flush();
  console.log("\n(all rate_limit keys cleared)");
})().catch(e => { console.error("ERR", e); process.exit(1); });
