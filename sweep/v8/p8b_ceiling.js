/**
 * p8b — prove the routing fix actually ENFORCES the intended ceilings, not just
 * that the redis key name changed. The side-effect-free probe path can't be used
 * for a ceiling test (the trailing segment makes it a 404 before the controller,
 * but the limiter still counts, so it IS safe to flood). These paths do zero
 * business work and never reach Ollama: the limiter rejects before the
 * DispatcherServlet resolves the handler, and the ones that pass get a 404.
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
  const r = await fetch(BASE + "/api/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password: pw }) });
  const j = await r.json(); return (j.data && j.data.token) || j.token;
}

async function flood(method, path, n, tok) {
  const codes = [];
  for (let i = 0; i < n; i++) {
    const r = await fetch(BASE + path, {
      method, headers: { "Content-Type": "application/json", ...(tok ? { Authorization: "Bearer " + tok } : {}) },
      body: method === "GET" ? undefined : "{}"
    });
    codes.push(r.status);
    await new Promise(x => setTimeout(x, 12));
  }
  const h = {}; for (const c of codes) h[c] = (h[c] || 0) + 1;
  const f = codes.findIndex(c => c === 429);
  return { hist: h, first429: f < 0 ? null : f + 1 };
}

const TAG = "/__ceiling_" + Date.now();

(async () => {
  const admin = await login("admin@gmail.com", "123456");
  console.log("=== CEILING ENFORCEMENT after F91/F92 fix ===");
  console.log("(mọi path đều có hậu tố 404 an toàn — không gọi Ollama, không tạo file)\n");

  const cases = [
    ["BEFORE-FIX HOLE: /api/admin/exercises/ai/generate-async (now :ai 10)", "POST", "/api/admin/exercises/ai/generate-async" + TAG, 14],
    ["BEFORE-FIX HOLE: speaking-prompts/ai-generate (now :ai 10)", "POST", "/api/v1/admin/speaking-prompts/ai-generate" + TAG, 14],
    ["BEFORE-FIX HOLE: video-prompts/ai-generate ALIAS (now :ai 10)", "POST", "/api/v1/admin/video-prompts/ai-generate" + TAG, 14],
    ["BEFORE-FIX HOLE: video-lessons/translate-transcript (now :ai 10)", "POST", "/api/v1/admin/video-lessons/translate-transcript" + TAG, 14],
    ["CONTROL: /api/ai/enrich-word (still :ai 10)", "POST", "/api/ai/enrich-word" + TAG, 14],
    ["BEFORE-FIX HOLE: /api/auth/avatar/upload (now :upload 15)", "POST", "/api/auth/avatar/upload" + TAG, 19],
    ["BEFORE-FIX HOLE: video-lessons/upload (now :upload 15)", "POST", "/api/v1/admin/video-lessons/upload" + TAG, 19],
    ["CONTROL: /api/admin/audio-upload (still :upload 15)", "POST", "/api/admin/audio-upload" + TAG, 19],
    ["NOT-AFFECTED: GET /api/admin/exercises/ai/status stays :global", "GET", "/api/admin/exercises/ai/status" + TAG, 105],
  ];
  const out = [];
  for (const [label, m, p, n] of cases) {
    flush();
    const r = await flood(m, p, n, admin);
    await new Promise(x => setTimeout(x, 150));
    const k = keys();
    const bucket = k.length ? k[0].split(":").pop() : "<none>";
    out.push({ label, method: m, path: p, n, first429: r.first429, hist: r.hist, bucket });
    console.log("  " + label.padEnd(62) + " :" + bucket.padEnd(8) + " first429=" + String(r.first429).padStart(5) + " hist=" + JSON.stringify(r.hist));
  }
  flush();
  require("fs").writeFileSync("p8b.json", JSON.stringify(out, null, 1));
  console.log("\nwrote p8b.json (rate-limit keys cleared)");
})().catch(e => { console.error("ERR", e); process.exit(1); });
