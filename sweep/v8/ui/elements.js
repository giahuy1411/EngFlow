const H = require("./lib.js");

async function login(page, email) {
  await page.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1200);
  await page.fill("input[type=email]", email);
  await page.fill("input[type=password]", "123456");
  await page.click("button[type=submit]");
  await page.waitForTimeout(3000);
  return page.evaluate(() => location.pathname);
}

const ROUTES = process.argv.slice(2);

(async () => {
  const browser = await H.pw.chromium.launch({ headless: true });
  const page = await (await browser.newContext({ viewport: { width: 1440, height: 900 } })).newPage();
  console.log("login ->", await login(page, "user@gmail.com"));
  for (const r of ROUTES) {
    await page.goto(H.APP + r, { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(2000);
    const info = await page.evaluate(() => {
      const pick = sel => [...document.querySelectorAll(sel)].slice(0, 14).map(e => ({
        t: (e.innerText || e.value || "").trim().replace(/\s+/g, " ").slice(0, 40),
        cls: String(e.className || "").slice(0, 46)
      })).filter(x => x.t);
      return {
        path: location.pathname,
        len: document.body.innerText.length,
        buttons: pick("button"),
        inputs: [...document.querySelectorAll("input,select,textarea")].slice(0, 8).map(e => ({
          type: e.type || e.tagName, ph: e.placeholder || "", name: e.name || ""
        })),
        heads: pick("h1,h2,h3").map(x => x.t)
      };
    });
    console.log("==== " + r + " path=" + info.path + " textLen=" + info.len);
    console.log("   heads: " + JSON.stringify(info.heads));
    console.log("   inputs: " + JSON.stringify(info.inputs));
    console.log("   buttons: " + JSON.stringify(info.buttons.map(b => b.t)));
  }
  await browser.close();
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
