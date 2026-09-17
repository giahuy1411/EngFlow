const lib = require("./lib.js");
(async () => {
  await lib.initTokens();
  const B = "http://localhost:8080", J = JSON.stringify;
  const h = { "Content-Type": "application/json", Authorization: "Bearer " + lib.getAdmin() };
  const mk = (pub) => ({ title: "ZZ t37 pub=" + pub + " " + Date.now(), description: "x", youtubeUrl: "https://www.youtube.com/watch?v=2VeQTuSSiI0", level: "ELEMENTARY", isPublished: pub, transcript: [{ start: 0, end: 2, textEn: "Hello." }, { start: 2, end: 4, textEn: "Bye." }] });
  for (const pub of [true, false]) {
    let r = await fetch(B + "/api/v1/admin/video-lessons", { method: "POST", headers: h, body: J(mk(pub)) });
    const id = (await r.json()).id;
    r = await fetch(B + "/api/v1/video-lessons/" + id);
    const d = await r.json();
    console.log("pub=" + pub + " GET=" + r.status + " lines=" + (d.transcript || []).length);
    await fetch(B + "/api/v1/admin/video-lessons/" + id, { method: "DELETE", headers: { Authorization: "Bearer " + lib.getAdmin() } });
  }
})().catch(e => { console.error("FATAL", e.message); process.exit(1); });
