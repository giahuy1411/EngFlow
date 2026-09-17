const lib = require("./lib.js");
(async () => {
  await lib.initTokens();
  const B = "http://localhost:8080", J = JSON.stringify;
  const h = { "Content-Type": "application/json", Authorization: "Bearer " + lib.getAdmin() };
  const base = { title: "ZZ t36b tr " + Date.now(), description: "x", youtubeUrl: "https://www.youtube.com/watch?v=2VeQTuSSiI0", level: "ELEMENTARY", isPublished: false, transcript: [{ start: 0, end: 2, textEn: "Hello." }, { start: 2, end: 4, textEn: "Bye." }] };
  let r = await fetch(B + "/api/v1/admin/video-lessons", { method: "POST", headers: h, body: J(base) });
  const j = await r.json(); console.log("create", r.status, J(j).slice(0, 200));
  const id = j.id;
  r = await fetch(B + "/api/v1/video-lessons/" + id);
  const t = await r.text();
  console.log("public GET", r.status, t.slice(0, 260));
  r = await fetch(B + "/api/v1/admin/video-lessons/" + id, { method: "PUT", headers: h, body: J({ ...base, title: base.title + " v2", transcript: [] }) });
  console.log("PUT transcript:[] ->", r.status);
  r = await fetch(B + "/api/v1/video-lessons/" + id);
  const d = await r.json(); console.log("after [] lines=" + (d.transcript || []).length);
  r = await fetch(B + "/api/v1/admin/video-lessons/" + id, { method: "DELETE", headers: { Authorization: "Bearer " + lib.getAdmin() } });
  console.log("cleanup", r.status);
})().catch(e => { console.error("FATAL", e.message); process.exit(1); });
