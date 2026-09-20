const H = require("./ui/lib.js");
(async () => {
  const b = await H.pw.chromium.launch({ headless: true, args: ["--use-fake-ui-for-media-stream", "--use-fake-device-for-media-stream"] });
  const s = await H.loginFull("user@gmail.com", "123456");
  const ctx = await b.newContext({ viewport: { width: 1440, height: 900 } });
  await ctx.grantPermissions(["microphone"], { origin: H.APP });
  const p = await ctx.newPage();
  await H.seedAuth(p, s);
  for (const r of ["/decks/10007", "/speaking/50006"]) {
    await p.goto(H.APP + r, { waitUntil: "domcontentloaded" });
    await p.waitForTimeout(3000);
    const info = await p.evaluate(() => ({
      url: location.pathname,
      btns: [...document.querySelectorAll("button")].map(x => (x.id ? "#" + x.id + " " : "") + (x.innerText || "").trim().slice(0, 24)),
      anchors: [...document.querySelectorAll("a")].map(a => a.getAttribute("href")).filter(h => h && h.includes("deck") || (h||"").includes("speaking")).slice(0, 12),
      text: document.body.innerText.replace(/\s+/g, " ").slice(0, 400)
    }));
    console.log("=====", r, info.url);
    console.log(" btns:", JSON.stringify(info.btns));
    console.log(" anchors:", JSON.stringify(info.anchors));
    console.log(" text:", info.text);
  }
  await b.close();
})().catch(e => { console.log("ERR", e.message); process.exit(1); });