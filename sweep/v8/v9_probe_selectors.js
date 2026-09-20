const H = require("./ui/lib.js");
(async () => {
  const b = await H.pw.chromium.launch({ headless: true });
  const s = await H.loginFull("user@gmail.com", "123456");
  const ctx = await b.newContext({ viewport: { width: 1440, height: 900 } });
  const p = await ctx.newPage();
  await H.seedAuth(p, s);
  for (const r of ["/decks", "/lessons", "/speaking"]) {
    await p.goto(H.APP + r, { waitUntil: "domcontentloaded" });
    await p.waitForTimeout(2500);
    const info = await p.evaluate(() => {
      const anchors = [...document.querySelectorAll("a")].slice(0, 40).map(a => a.getAttribute("href") + " | " + (a.innerText || "").trim().slice(0, 30));
      const btns = [...document.querySelectorAll("button")].slice(0, 40).map(x => (x.id ? "#" + x.id + " " : "") + (x.innerText || "").trim().slice(0, 28));
      return { anchors, btns, url: location.pathname };
    });
    console.log("=====", r, info.url);
    console.log("  anchors:", JSON.stringify(info.anchors.slice(0, 22)));
    console.log("  buttons:", JSON.stringify(info.btns.slice(0, 22)));
  }
  await b.close();
})().catch(e => { console.log("ERR", e.message); process.exit(1); });