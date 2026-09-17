const H = require("./lib.js");
(async () => {
  const b = await H.pw.chromium.launch({ headless: true });
  const ctx = await b.newContext({ viewport: { width: 1440, height: 900 } });
  const p = await ctx.newPage();
  const errs = [], bad = [];
  p.on("console", m => { if (m.type() === "error") errs.push(m.text().slice(0, 120)); });
  p.on("response", r => { if (r.url().startsWith(H.API) && r.status() >= 400) bad.push(r.status() + " " + r.url().replace(H.API, "").slice(0, 60)); });
  await p.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
  await p.waitForTimeout(1000);
  await p.fill("input[type=email]", "user@gmail.com");
  await p.fill("input[type=password]", "123456");
  await p.click("button[type=submit]");
  await p.waitForTimeout(2500);
  await p.goto(H.APP + "/videos/1", { waitUntil: "domcontentloaded" });
  await p.waitForTimeout(4000);
  const info = await p.evaluate(() => {
    const ifr = document.querySelector("iframe");
    return {
      title: (document.querySelector("h1,h2") || {}).innerText,
      iframe: ifr ? ifr.src.slice(0, 60) : null,
      transcriptLines: document.querySelectorAll("button, [class*=line]").length,
      bodyLen: document.body.innerText.length,
      tabs: [...document.querySelectorAll("button")].map(x => (x.innerText || "").trim()).filter(t => t && t.length < 24).slice(0, 14)
    };
  });
  console.log("video1:", JSON.stringify(info, null, 1));
  // click each tab and make sure nothing explodes
  for (const t of (info.tabs || [])) {
    try {
      await p.click("text=" + t, { timeout: 2500 });
      await p.waitForTimeout(900);
    } catch (e) { /* not clickable, ignore */ }
  }
  console.log("after tab clicks, apiErrors:", JSON.stringify(bad.slice(0, 6)), "consoleErrors:", JSON.stringify(errs.slice(0, 6)));
  await p.screenshot({ path: "ui/video1.png", fullPage: true });
  await b.close();
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
