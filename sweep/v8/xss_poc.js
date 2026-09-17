const pw = require("playwright-core");

const BASE = "http://localhost:8080";
const APP = "http://localhost:5173";

async function login(email, pass) {
  const r = await fetch(BASE + "/api/auth/login", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: email, password: pass })
  });
  const j = await r.json();
  return (j.data && j.data.token) || j.token;
}

async function upload(token, fname, content, ctype) {
  const b = "----pwb" + Math.random().toString(16).slice(2);
  const head = "--" + b + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
    + fname + "\"\r\nContent-Type: " + ctype + "\r\n\r\n";
  const body = Buffer.concat([Buffer.from(head), Buffer.from(content), Buffer.from("\r\n--" + b + "--\r\n")]);
  const r = await fetch(BASE + "/api/lesson-submissions/upload-audio", {
    method: "POST",
    headers: { Authorization: "Bearer " + token, "Content-Type": "multipart/form-data; boundary=" + b },
    body: body
  });
  const t = await r.text();
  let url = null;
  try { url = JSON.parse(t).audioUrl || null; } catch (e) { /* not json */ }
  return { status: r.status, url: url, body: t.slice(0, 120) };
}

(async () => {
  const tok = await login("user@gmail.com", "123456");
  const js = "window.__XSS_PWNED__ = { origin: location.origin, token: localStorage.getItem('token') };";
  const upJs = await upload(tok, "evil.js", js, "text/javascript");
  console.log("upload evil.js   ->", upJs.status, upJs.body);
  const html = "<html><body><h1>Free English quiz</h1><script src=\""
    + (upJs.url || "/api/resources/planted.js") + "\"></script></body></html>";
  const upHtml = await upload(tok, "lesson.html", html, "text/html");
  console.log("upload lesson.html ->", upHtml.status, upHtml.body);

  // If a browser-active object is somehow ALREADY on disk (pre-fix era), prove it
  // cannot render either. The last pre-fix PoC names are recorded for the audit.
  const targets = [];
  if (upHtml.url && upHtml.url.startsWith("/api/resources/")) targets.push(upHtml.url);
  targets.push("/api/resources/184176ba-00ad-4678-9f58-b013c7a84deb.html",
               "/api/resources/2e71c5b4-e233-4740-9cb1-1224e80c2fdb.js");

  const browser = await pw.chromium.launch({ headless: true });
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  await page.goto(APP + "/", { waitUntil: "domcontentloaded" });
  await page.evaluate((t) => localStorage.setItem("token", t), tok);

  for (const t of targets) {
    await page.goto(APP + t, { waitUntil: "domcontentloaded" }).catch(() => {});
    await page.waitForTimeout(800);
    const st = await page.evaluate(() => ({ pwned: !!window.__XSS_PWNED__, ct: document.contentType }));
    console.log("render", t, "=>", JSON.stringify(st), st.pwned ? "STILL VULNERABLE" : "safe");
  }
  const hdr = await fetch(APP + "/api/resources/184176ba-00ad-4678-9f58-b013c7a84deb.html");
  console.log("headers of a pre-fix .html on disk:", hdr.status,
      "| CT=" + hdr.headers.get("content-type"), "| DISP=" + hdr.headers.get("content-disposition"));
  await browser.close();
})().catch(e => { console.error("POC-ERR", e.message); process.exit(1); });
