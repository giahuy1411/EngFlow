const H = require("./lib.js");

function watch(page, acc) {
  page.on("response", r => {
    if (!r.url().startsWith(H.API)) return;
    acc.calls.push(r.request().method() + " " + r.url().replace(H.API, "").slice(0, 70) + " " + r.status());
    if (r.status() >= 400) acc.bad.push(r.request().method() + " " + r.url().replace(H.API, "").slice(0, 60) + " " + r.status());
  });
  page.on("console", m => { if (m.type() === "error") acc.errs.push(m.text().slice(0, 140)); });
}

async function login(page, email) {
  await page.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1200);
  await page.fill("input[type=email]", email);
  await page.fill("input[type=password]", "123456");
  await page.click("button[type=submit]");
  await page.waitForTimeout(2500);
}

async function scenario(name, email, route, fn) {
  const b = await H.pw.chromium.launch({ headless: true });
  const page = await (await b.newContext({ viewport: { width: 1440, height: 900 } })).newPage();
  const acc = { calls: [], bad: [], errs: [] };
  watch(page, acc);
  console.log("###### " + name);
  try {
    if (email !== "none") await login(page, email);
    await page.goto(H.APP + route, { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(1800);
    await fn(page);
  } catch (e) { console.log("  THREW " + e.message.split("\n")[0].slice(0, 110)); }
  console.log("  API " + JSON.stringify(acc.calls.slice(-8)));
  if (acc.bad.length) console.log("  NON2XX " + JSON.stringify(acc.bad));
  if (acc.errs.length) console.log("  ERRS " + JSON.stringify(acc.errs.slice(0, 3)));
  await b.close();
}

(async () => {
  await scenario("search: dictionary lookup", "user@gmail.com", "/search", async page => {
    const inp = page.locator('input[placeholder*="tra"]');
    console.log("  input count:", await inp.count());
    await inp.first().fill("travel");
    const p = page.waitForResponse(r => /dictionary|vocabulary/.test(r.url()), { timeout: 40000 }).catch(() => null);
    await inp.first().press("Enter");
    const res = await p;
    console.log("  lookup api:", res ? res.url().replace(H.API, "").slice(0, 50) + " " + res.status() : "NONE");
    await page.waitForTimeout(1500);
    const t = await page.evaluate(() => document.body.innerText.replace(/\s+/g, " ").slice(0, 260));
    console.log("  result text:", JSON.stringify(t));
  });

  await scenario("quiz game: click an option", "user@gmail.com", "/decks/10006/play/quiz", async page => {
    const before = await page.evaluate(() => document.body.innerText.slice(0, 120));
    const btns = page.locator("button");
    const n = await btns.count();
    console.log("  buttons:", n);
    for (let i = 0; i < n; i++) {
      const t = (await btns.nth(i).innerText()).trim();
      if (t.length > 2 && t.length < 24 && !/thoát|engflow|©/i.test(t)) {
        await btns.nth(i).click();
        console.log("  clicked option:", JSON.stringify(t));
        break;
      }
    }
    await page.waitForTimeout(2500);
    const after = await page.evaluate(() => document.body.innerText.replace(/\s+/g, " ").slice(0, 150));
    console.log("  after:", JSON.stringify(after));
    console.log("  changed:", before.slice(0, 40) !== after.slice(0, 40));
  });

  await scenario("ai vocab generator (real Ollama)", "user@gmail.com", "/ai-vocab-generator", async page => {
    await page.fill("#ai-topic", "travel");
    await page.fill("#ai-count", "3");
    const p = page.waitForResponse(r => r.url().includes("/api/ai/generate-vocab"), { timeout: 180000 }).catch(() => null);
    await page.click("text=Sinh từ vựng");
    const res = await p;
    console.log("  generate-vocab:", res ? res.status() : "NO RESPONSE");
    await page.waitForTimeout(2500);
    const t = await page.evaluate(() => document.body.innerText.replace(/\s+/g, " ").slice(0, 300));
    console.log("  page:", JSON.stringify(t));
  });

  await scenario("admin lessons: filter + open builder", "admin@gmail.com", "/admin/lessons", async page => {
    const box = page.locator('input[placeholder*="Tiêu đề"], input[placeholder*="tìm" i]');
    console.log("  search box:", await box.count());
    if (await box.count()) {
      await box.first().fill("English");
      await page.waitForTimeout(2500);
      const rows = await page.evaluate(() => document.querySelectorAll("table tbody tr").length);
      console.log("  rows after filter:", rows);
      await box.first().fill("");
      await page.waitForTimeout(1500);
    }
    const edit = page.locator("text=/Sửa|Biên tập|build/i").first();
    if (await edit.count()) { await edit.click().catch(() => {}); await page.waitForTimeout(2500); }
    console.log("  path:", await page.evaluate(() => location.pathname));
    const s = await page.evaluate(() => document.body.innerText.replace(/\s+/g, " ").slice(0, 160));
    console.log("  text:", JSON.stringify(s));
  });

  await scenario("register: client vs server validation", "none", "/register", async page => {
    const n = await page.locator("input").count();
    console.log("  inputs:", n);
    const sent = [];
    page.on("request", r => { if (r.url().includes("/register")) sent.push("REQUEST SENT"); });
    await page.click("button[type=submit]");
    await page.waitForTimeout(2500);
    console.log("  native validation blocked submit:", sent.length === 0);
    const errs = await page.evaluate(() => document.body.innerText.replace(/\s+/g, " ").slice(0, 220));
    console.log("  page:", JSON.stringify(errs));
  });

  await scenario("lesson exercise grade via UI", "user@gmail.com", "/lessons/445", async page => {
    await page.click("text=BÀI TẬP");
    await page.waitForTimeout(2500);
    const inputs = await page.evaluate(() => [...document.querySelectorAll("input,textarea,select")].map(e => e.type + "|" + (e.placeholder || "")).slice(0, 6));
    console.log("  answer inputs:", JSON.stringify(inputs));
    const btnLabels = await page.evaluate(() => [...document.querySelectorAll("button")].map(b => (b.innerText || "").trim().replace(/\s+/g, " ")).filter(Boolean).slice(0, 12));
    console.log("  buttons:", JSON.stringify(btnLabels));
  });
})().catch(e => { console.log("FATAL", e.message); process.exit(1); });
