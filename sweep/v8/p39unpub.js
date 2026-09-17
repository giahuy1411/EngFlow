(async () => {
  const B = "http://localhost:8080";
  async function tok(email) {
    const r = await fetch(B + "/api/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password: "123456" }) });
    const j = await r.json(); return (j.data && j.data.token) || j.token;
  }
  const admin = await tok("admin@gmail.com"), user = await tok("user@gmail.com");
  async function get(p, t) { const r = await fetch(B + p, { headers: t ? { Authorization: "Bearer " + t } : {} }); let b = ""; try { b = JSON.stringify(await r.json()).slice(0, 90); } catch (e) {} return r.status + " " + b; }
  for (const id of [10888, 11300]) {
    console.log("lesson " + id + " guest : " + await get("/api/lessons/" + id, null));
    console.log("lesson " + id + " user  : " + await get("/api/lessons/" + id, user));
    console.log("lesson " + id + " admin : " + await get("/api/lessons/" + id, admin));
    console.log("lesson " + id + " struct: " + await get("/api/v1/lessons/" + id + "/structure", null));
    console.log("lesson " + id + " in-list guest: " + await get("/api/lessons?page=0&size=100&q=", null));
  }
  const pubLesson = await get("/api/lessons/1", null);
  console.log("published lesson 1 guest: " + pubLesson);
})().catch(e => { console.error("FATAL", e.message); process.exit(1); });
