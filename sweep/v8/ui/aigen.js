const H = require("./lib.js");
(async () => {
  const b = await H.pw.chromium.launch({ headless: true });
  const page = await (await b.newContext({ viewport: { width: 1440, height: 900 } })).newPage();
  const t0 = Date.now();
  page.on("request", r => { if (r.url().includes("generate-vocab")) console.log("+" + ((Date.now() - t0) / 1000).toFixed(1) + "s REQUEST", r.method()); });
  page.on("response", r => { if (r.url().includes("generate-vocab")) console.log("+" + ((Date.now() - t0) / 1000).toFixed(1) + "s RESPONSE", r.status()); });
  page.on("requestfailed", r => { if (r.url().includes("generate-vocab")) console.log("+" + ((Date.now() - t0) / 1000).toFixed(1) + "s FAILED", r.failure() && r.failure().errorText); });
  page.on("console", m => { if (m.type() === "error") console.log("CONSOLE_ERR", m.text().slice(0, 160)); });

  await page.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1200);
  await page.fill("input[type=email]", "user@gmail.com");
  await page.fill("input[type=password]", "123456");
  await page.click("button[type=submit]");
  await page.waitForTimeout(2500);
  await page.goto(H.APP + "/ai-vocab-generator", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1500);
  await page.fill("#ai-topic", "travel");
  await page.selectOption("#ai-level", "B1");
  await page.fill("#ai-count", "3");
  const state = await page.evaluate(() => {
    const f = document.querySelector("form");
    const btn = f.querySelector("button[type=submit]");
    return { valid: f.checkValidity(), disabled: btn.disabled, btnText: (btn.innerText || "").trim() };
  });
  console.log("form state", JSON.stringify(state));
  await page.click("form button[type=submit]");
  const ok = await page.waitForResponse(r => r.url().includes("generate-vocab"), { timeout: 300000 }).catch(() => null);
  console.log("resp:", ok ? ok.status() : "TIMEOUT", "elapsed", ((Date.now() - t0) / 1000).toFixed(1) + "s");
  await page.waitForTimeout(1500);
  const txt = await page.evaluate(() => document.body.innerText.replace(/\s+/g, " ").match(/KẾT QURA[\s\S]{0,220}|CHƯA CÓ KẾT QUẢ[\s\S]{0,80}|SINH TỪ THẤT BẠI[\s\S]{0,80}|[Tt]ravel[\s\S]{0,200}/) || null);
  console.log("page result:", JSON.stringify(txt && txt[0] && txt[0].slice(0, 240)));
  await b.close();
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
