const H = require("./lib.js");
(async () => {
  const b = await H.pw.chromium.launch({ headless: true });
  const p = await (await b.newContext({ viewport: { width: 1440, height: 900 } })).newPage();
  await p.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
  await p.waitForTimeout(1000);
  await p.fill("input[type=email]", "user@gmail.com");
  await p.fill("input[type=password]", "123456");
  await p.click("button[type=submit]");
  await p.waitForTimeout(2500);
  await p.goto(H.APP + "/lessons/445", { waitUntil: "domcontentloaded" });
  await p.waitForTimeout(3000);
  const r = await p.evaluate(() => [...document.querySelectorAll("img")].map(i => ({
    src: (i.currentSrc || i.src || "").slice(0, 70),
    alt: i.getAttribute("alt"),
    cls: String(i.className).slice(0, 40),
    parent: i.parentElement ? i.parentElement.tagName + "." + String(i.parentElement.className).slice(0, 40) : "",
    role: i.getAttribute("role"), aria: i.getAttribute("aria-hidden")
  })));
  console.log(JSON.stringify(r, null, 1));
  await b.close();
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
