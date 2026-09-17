const { execSync } = require("child_process");
const redis = (c) => execSync("docker exec engflow-redis redis-cli " + c, { encoding: "utf8" }).trim();
const keys = () => redis("--scan --pattern rate_limit:*").split(/\r?\n/).filter(Boolean);

(async () => {
  // clean
  const k0 = keys();
  if (k0.length) execSync("docker exec engflow-redis redis-cli del " + k0.map(x => '"' + x + '"').join(" "), { encoding: "utf8" });
  console.log("start keys:", keys());

  const probes = [
    ["POST", "http://localhost:8080/api/auth/login"],
    ["POST", "http://localhost:8080/api/auth/login/__probe"],
    ["POST", "http://localhost:8080/api/ai/enrich-word/__probe"],
    ["POST", "http://localhost:8080/api/v1/payment/create-order/__probe"],
    ["GET",  "http://localhost:8080/api/lessons/__probe"],
  ];
  for (const [m, url] of probes) {
    const before = keys().slice();
    const r = await fetch(url, { method: m, headers: { "Content-Type": "application/json" }, body: m === "POST" ? "{}" : undefined });
    await new Promise(x => setTimeout(x, 150));
    const after = keys().slice();
    const added = after.filter(x => !before.includes(x));
    console.log(m.padEnd(5), url.replace("http://localhost:8080", "").padEnd(50),
      "status=" + r.status, "newKeys=" + JSON.stringify(added));
  }
  const kf = keys();
  if (kf.length) execSync("docker exec engflow-redis redis-cli del " + kf.map(x => '"' + x + '"').join(" "), { encoding: "utf8" });
  console.log("cleaned, keys:", keys());
})().catch(e => { console.error("ERR", e); process.exit(1); });
