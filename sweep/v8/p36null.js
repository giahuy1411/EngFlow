const lib = require("./lib.js");
(async () => {
  await lib.initTokens();
  const B = "http://localhost:8080", J = JSON.stringify;
  const h = { "Content-Type": "application/json", Authorization: "Bearer " + lib.getAdmin() };
  const base = { title: "ZZ t36 null-tr " + Date.now(), description: "x", youtubeUrl: "https://www.youtube.com/watch?v=2VeQTuSSiI0", level: "ELEMENTARY", transcript: [{ start: 0, end: 2, textEn: "Hello." }, { start: 2, end: 4, textEn: "Bye." }] };
  let r = await fetch(B + "/api/v1/admin/video-lessons", { method: "POST", headers: h, body: J(base) });
  const id = (await r.json()).id; console.log("create", r.status, "id=" + id);
  r = await fetch(B + "/api/v1/admin/video-lessons/" + id, { method: "PUT", headers: h, body: J({ ...base, transcript: null }) });
  console.log("PUT transcript:null ->", r.status, (await r.text()).slice(0, 140));
  r = await fetch(B + "/api/v1/admin/video-lessons/" + id);
  let d = await r.json(); console.log("after: lines=" + (d.transcript || []).length);
  r = await fetch(B + "/api/v1/admin/video-lessons/" + id, { method: "PUT", headers: h, body: J({ title: base.title, description: "y", youtubeUrl: base.youtubeUrl, level: "ELEMENTARY" }) });
  console.log("PUT transcript:missing ->", r.status, (await r.text()).slice(0, 140));
  r = await fetch(B + "/api/v1/admin/video-lessons/" + id, { method: "DELETE", headers: { Authorization: "Bearer " + lib.getAdmin() } });
  console.log("cleanup", r.status);
})().catch(e => { console.error("FATAL", e.message); process.exit(1); });
