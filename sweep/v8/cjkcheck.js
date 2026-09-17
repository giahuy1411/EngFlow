const lib = require("./lib.js");
const fs = require("fs");
(async () => {
  await lib.initTokens();
  lib.flushLimits(true);
  const topics = ["hobbies on Sunday", "school memories", "online shopping", "healthy habits", "city traffic", "volunteering", "favorite food", "music taste", "climate change", "part-time job"];
  const hits = [];
  for (const t of topics) {
    const r = await fetch("http://localhost:8080/api/v1/admin/speaking-prompts/ai-generate", {
      method: "POST", headers: { "Content-Type": "application/json", Authorization: "Bearer " + lib.getAdmin() },
      body: JSON.stringify({ topic: t })
    });
    const txt = await r.text();
    const m = txt.match(/[\u4e00-\u9fff\u3040-\u30ff\uac00-\ud7af]+/g);
    if (m) hits.push({ topic: t, sample: m.slice(0, 6).join(" "), len: txt.length });
    console.log(`${r.status} topic="${t}" cjk=${m ? m.slice(0, 3).join(" ") : "none"}`);
    fs.writeFileSync("cjk_" + t.replace(/\W+/g, "_") + ".txt", txt);
  }
  fs.writeFileSync("cjk_result.json", JSON.stringify(hits, null, 1));
  console.log("=== HITS:", hits.length + "/" + topics.length, JSON.stringify(hits).slice(0, 700));
})();
