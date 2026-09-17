const lib = require("./lib.js");
(async () => {
  await lib.initTokens(); lib.flushLimits(true);
  const t0 = Date.now();
  const r = await fetch("http://localhost:8080/api/v1/admin/speaking-prompts/ai-generate", {
    method: "POST", headers: { "Content-Type": "application/json", Authorization: "Bearer " + lib.getAdmin() },
    body: JSON.stringify({ topic: "local vs cloud check " + Date.now() })
  });
  console.log("ai-generate", r.status, ((Date.now() - t0) / 1000).toFixed(1) + "s", (await r.text()).slice(0, 80));
})().catch(e => { console.error("FATAL", e.message); process.exit(1); });
