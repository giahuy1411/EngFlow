const BASE = "http://localhost:8080";
const { execSync } = require("child_process");
async function login(email, pw) {
  const r = await fetch(BASE + "/api/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password: pw }) });
  const j = await r.json(); return (j.data && j.data.token) || j.token;
}
function flush() { try { const k = execSync("docker exec engflow-redis redis-cli --scan --pattern rate_limit:*", { encoding: "utf8" }).trim();
  if (k) execSync("docker exec engflow-redis redis-cli del " + k.split(/\r?\n/).filter(Boolean).map(x => JSON.stringify(x)).join(" "), { encoding: "utf8" }); } catch (e) {} }
function keys() { try { return execSync("docker exec engflow-redis redis-cli --scan --pattern rate_limit:*", { encoding: "utf8" }).trim().split(/\r?\n/).filter(Boolean); } catch (e) { return []; } }
async function hit(path, tok, n, method) {
  const c = {};
  for (let i = 0; i < n; i++) {
    const r = await fetch(BASE + path, { method: method || "POST", headers: { "Content-Type": "application/json", ...(tok ? { Authorization: "Bearer " + tok } : {}) }, body: method ? undefined : "{\"planType\":\"MONTHLY\"}" });
    c[r.status] = (c[r.status] || 0) + 1;
    await new Promise(res => setTimeout(res, 12));
  }
  console.log("  " + path.padEnd(40) + JSON.stringify(c));
}
(async () => {
  const u = await login("user@gmail.com", "123456");
  flush();
  await hit("/api/v1/payment/create-order", u, 13);
  console.log("  buckets:", keys().map(k => k.split(":").pop()).join(" ") || "none");
  flush();
})().catch(e => console.log("ERR", e.message));
