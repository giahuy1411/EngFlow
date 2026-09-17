const lib = require("./lib.js");
const B = "http://localhost:8080";
(async () => {
  await lib.initTokens();
  const admin = lib.getAdmin(), user = lib.getUser();
  const J = JSON.stringify;
  const hj = t => ({ "Content-Type": "application/json", ...(t ? { Authorization: "Bearer " + t } : {}) });
  const h = t => (t ? { Authorization: "Bearer " + t } : {});
  async function st(p, t, headersFn) { const r = await fetch(B + p, { headers: (headersFn || h)(t) }); return r.status; }
  const pub = 445;
  const unpub = 10888;
  const rows = [];
  rows.push(["lessons/" + unpub + " guest", await st("/api/lessons/" + unpub, null), 404]);
  rows.push(["lessons/" + unpub + " student", await st("/api/lessons/" + unpub, user), 404]);
  rows.push(["lessons/" + unpub + " admin", await st("/api/lessons/" + unpub, admin), 200]);
  rows.push(["lessons/" + pub + " guest", await st("/api/lessons/" + pub, null), 200]);
  rows.push(["lessons/" + unpub + "/exercises guest", await st("/api/lessons/" + unpub + "/exercises", null), 404]);
  rows.push(["lessons/" + unpub + "/exercises admin", await st("/api/lessons/" + unpub + "/exercises", admin), 200]);
  rows.push(["lessons/" + unpub + "/exercises/content guest", await st("/api/lessons/" + unpub + "/exercises/content", null), 404]);
  rows.push(["lessons/" + unpub + "/exercises/content admin", await st("/api/lessons/" + unpub + "/exercises/content", admin), 200]);

  // video lesson draft
  let r = await fetch(B + "/api/v1/admin/video-lessons", { method: "POST", headers: hj(admin), body: J({
    title: "ZZ draft vid " + Date.now(), description: "draft", youtubeUrl: "https://www.youtube.com/watch?v=2VeQTuSSiI0",
    level: "ELEMENTARY", isPublished: false, transcript: [{ start: 0, end: 2, textEn: "Draft one." }, { start: 2, end: 4, textEn: "Draft two." }] }) });
  const vid = (await r.json()).id;
  rows.push(["video-lessons/" + vid + " guest (draft)", await st("/api/v1/video-lessons/" + vid, null), 404]);
  rows.push(["video-lessons/" + vid + " student (draft)", await st("/api/v1/video-lessons/" + vid, user), 404]);
  rows.push(["video-lessons/" + vid + " admin (draft)", await st("/api/v1/video-lessons/" + vid, admin), 200]);

  // PUT keep transcript (transcript null) — duong di cua form admin
  r = await fetch(B + "/api/v1/admin/video-lessons/" + vid, { method: "PUT", headers: hj(admin), body: J({
    title: "ZZ draft vid " + vid + " v2", description: "draft", youtubeUrl: "https://youtu.be/2VeQTuSSiI0", level: "ELEMENTARY", transcript: null }) });
  const putStatus = r.status;
  const detail = await (await fetch(B + "/api/v1/video-lessons/" + vid, { headers: h(admin) })).json();
  rows.push(["PUT transcript=null -> " + putStatus + " (2)", putStatus, 200]);
  rows.push(["transcript giu nguyen sau PUT (" + (detail.transcript || []).length + " dong)", (detail.transcript || []).length, 2]);
  rows.push(["title da doi", detail.title.endsWith("v2") ? 1 : 0, 1]);
  // publish + cleanup
  await fetch(B + "/api/v1/admin/video-lessons/" + vid, { method: "PUT", headers: hj(admin), body: J({ title: "ZZ draft vid " + vid, description: "d", youtubeUrl: "https://youtu.be/2VeQTuSSiI0", level: "ELEMENTARY", isPublished: true, transcript: null }) });
  rows.push(["video-lessons/" + vid + " guest sau publish", await st("/api/v1/video-lessons/" + vid, null), 200]);
  await fetch(B + "/api/v1/admin/video-lessons/" + vid, { method: "DELETE", headers: h(admin) });
  rows.push(["cleanup delete", await st("/api/v1/video-lessons/" + vid, admin), 404]);

  let fail = 0;
  for (const [name, got, want] of rows) { const ok = got === want; if (!ok) fail++; console.log((ok ? "  OK  " : " FAIL ") + name + " got=" + got + " want=" + want); }
  console.log("F88 LIVE total=" + rows.length + " FAIL=" + fail);
  process.exit(fail ? 1 : 0);
})().catch(e => { console.error("FATAL", e.message); process.exit(1); });
