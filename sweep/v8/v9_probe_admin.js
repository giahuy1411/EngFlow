const H = require("./ui/lib.js");
(async () => {
  const b = await H.pw.chromium.launch({ headless: true });
  const s = await H.loginFull("admin@gmail.com", "123456");
  const ctx = await b.newContext({ viewport: { width: 1440, height: 900 } });
  const p = await ctx.newPage();
  await H.seedAuth(p, s);
  await p.goto(H.APP + "/admin/lessons", { waitUntil: "domcontentloaded" });
  await p.waitForTimeout(3000);
  const before = await p.evaluate(() => [...document.querySelectorAll("button")].slice(0, 30).map(x => (x.id ? "#" + x.id + " " : "") + (x.innerText || "").trim().slice(0, 26)));
  console.log("buttons before:", JSON.stringify(before));
  const add = p.locator("button:has-text('Thêm')").first();
  console.log("add count:", await add.count());
  if (await add.count()) {
    await add.click(); await p.waitForTimeout(1500);
    const dlg = await p.evaluate(() => ({
      inputs: [...document.querySelectorAll("input")].map(i => (i.id ? "#" + i.id + " " : "") + (i.type || "text") + "|" + (i.placeholder || i.name || "")),
      areas: [...document.querySelectorAll("textarea")].map(t => (t.id ? "#" + t.id + " " : "") + (t.placeholder || t.name || "")),
      selects: [...document.querySelectorAll("select")].map(t => (t.id ? "#" + t.id + " " : "") + (t.name || "")),
      btns: [...document.querySelectorAll("button")].map(x => (x.innerText || "").trim().slice(0, 26)).slice(-12),
      text: document.body.innerText.slice(0, 500)
    }));
    console.log("DIALOG inputs:", JSON.stringify(dlg.inputs));
    console.log("DIALOG areas:", JSON.stringify(dlg.areas));
    console.log("DIALOG selects:", JSON.stringify(dlg.selects));
    console.log("DIALOG btns:", JSON.stringify(dlg.btns));
  }
  await b.close();
})().catch(e => { console.log("ERR", e.message); process.exit(1); });