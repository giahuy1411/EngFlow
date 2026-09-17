const { execSync } = require("child_process");
const redis = (c) => execSync("docker exec engflow-redis redis-cli " + c, { encoding: "utf8" }).trim();
const keys = () => redis("--scan --pattern rate_limit:*").split(/\r?\n/).filter(Boolean);
const flush = () => { const k = keys(); if (k.length) execSync("docker exec engflow-redis redis-cli del " + k.map(x => '"' + x + '"').join(" "), { encoding: "utf8" }); };

async function repeat(m, path, n) {
  const r = await fetch("http://localhost:8080" + path, { method: m, headers: { "Content-Type": "application/json" }, body: m === "POST" ? "{}" : undefined });
  return r.status;
}

(async () => {
  flush();
  const cases = [
    ["POST", "/api/auth/login", 3],
    ["POST", "/api/auth/login/__probe", 3],
    ["POST", "/api/ai/enrich-word", 3],
    ["POST", "/api/ai/enrich-word/__probe", 3],
    ["POST", "/api/v1/payment/create-order/__probe", 3],
    ["GET", "/api/lessons", 3],
    ["GET", "/api/lessons/__probe", 3],
  ];
  for (const [m, p, n] of cases) {
    flush();
    const codes = [];
    for (let i = 0; i < n; i++) codes.push(await repeat(m, p, n));
    await new Promise(x => setTimeout(x, 200));
    const k = keys();
    console.log(m.padEnd(5), p.padEnd(44), "codes=" + JSON.stringify(codes).padEnd(22), "keys=" + JSON.stringify(k));
  }
  flush();
})().catch(e => { console.error("ERR", e); process.exit(1); });
