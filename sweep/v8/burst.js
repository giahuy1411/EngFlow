const BASE = "http://localhost:8080";
async function login(email, pw) {
  const r = await fetch(BASE + "/api/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password: pw }) });
  const j = await r.json(); return (j.data && j.data.token) || j.token;
}
async function flush() { const { execSync } = require("child_process"); try {
  const k = execSync("docker exec engflow-redis redis-cli --scan --pattern rate_limit:*", { encoding: "utf8" }).trim();
  if (k) execSync("docker exec engflow-redis redis-cli del " + k.split(/\r?\n/).filter(Boolean).map(x => chr(x)).join(" "), { encoding: "utf8" });
} catch (e) { console.log("flush warn", e.message.slice(0, 60)); } }
function chr(x) { return String.fromCharCode(34) + x + String.fromCharCode(34); }
async function hit(path, tok, n) {
  const codes = [];
  for (let i = 0; i < n; i++) {
    const r = await fetch(BASE + path, { method: "POST", headers: { "Content-Type": "application/json", ...(tok ? { Authorization: "Bearer " + tok } : {}) }, body: "{}" });
    codes.push(r.status);
    await new Promise(res => setTimeout(res, 15));
  }
  const c200 = codes.filter(c => c === 200).length, c429 = codes.filter(c => c === 429).length;
  const first429 = codes.findIndex(c => c === 429);
  const hist = {}; for (const c of codes) hist[c] = (hist[c] || 0) + 1;
  console.log("  " + path.padEnd(42) + "n=" + n + " 200=" + c200 + " 429=" + c429 + " first429at=" + (first429 < 0 ? "never" : first429 + 1) + " hist=" + JSON.stringify(hist));
}
(async () => {
  const u = await login("user@gmail.com", "123456");
  console.log("burst: global limit 100/min, ai 10/min, order 10/min");
  await flush(); await hit("/api/ai/enrich-word", u, 14);
  await flush(); await hit("/api/v1/payment/create-order", u, 13);
  await flush(); await hit("/api/lessons?page=0&size=1", null, 105);
  await flush();
  console.log("(rate-limit buckets cleared for later phases)");
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
