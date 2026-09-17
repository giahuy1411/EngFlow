const H = require("./lib.js");

async function login(page, email) {
  await page.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1000);
  await page.fill("input[type=email]", email);
  await page.fill("input[type=password]", "123456");
  await page.click("button[type=submit]");
  await page.waitForTimeout(2500);
  return page.evaluate(() => location.pathname);
}

// run one scenario in a fresh browser context so the 15-min JWT never expires mid-test
async function scenario(name, email, body) {
  const browser = await H.pw.chromium.launch({ headless: true });
  const page = await (await browser.newContext({ viewport: { width: 1440, height: 900 } })).newPage();
  const net = [];
  page.on("response", async r => {
    if (!r.url().startsWith(H.API)) return;
    net.push(r.request().method() + " " + r.url().replace(H.API, "").slice(0, 70) + " " + r.status());
  });
  const errs = [];
  page.on("pageerror", e => errs.push("PAGEERROR " + e.message.slice(0, 120)));
  page.on("console", m => { if (m.type() === "error") errs.push("CONSOLE " + m.text().slice(0, 120)); });
  console.log("###### " + name);
  try {
    console.log("  login ->", await login(page, email));
    await body(page);
  } catch (e) {
    console.log("  THREW " + e.message.slice(0, 160));
  }
  console.log("  API calls: " + net.length);
  net.slice(0, 18).forEach(n => console.log("     " + n));
  const bad = net.filter(n => / [45]\d\d$/.test(n));
  console.log("  non-2xx: " + (bad.length ? JSON.stringify(bad) : "none"));
  console.log("  errors: " + (errs.length ? JSON.stringify(errs.slice(0, 4)) : "none"));
  await browser.close();
}

const click = async (page, text) => {
  const bs = await page.$$("button, a");
  for (const b of bs) {
    const t = ((await b.innerText()) || "").trim().toLowerCase();
    if (t === text.toLowerCase()) { await b.click(); return true; }
  }
  for (const b of bs) {
    const t = ((await b.innerText()) || "").trim().toLowerCase();
    if (t.includes(text.toLowerCase())) { await b.click(); return true; }
  }
  return false;
};

(async () => {
  await scenario("search dictionary (user)", "user@gmail.com", async page => {
    await page.goto(H.APP + "/search", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(1500);
    await page.fill("input[type=text]", "travel");
    await page.press("input[type=text]", "Enter");
    await page.waitForTimeout(4000);
    console.log("  textLen=" + (await page.evaluate(() => document.body.innerText.length)));
  });

  await scenario("lessons filter + detail tabs (user)", "user@gmail.com", async page => {
    await page.goto(H.APP + "/lessons", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(2000);
    console.log("  chip:", await click(page, "Sơ cấp"));
    await page.waitForTimeout(2000);
    await page.goto(H.APP + "/lessons/445", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(2000);
    console.log("  tab BÀI TẬP:", await click(page, "BÀI TẬP"));
    await page.waitForTimeout(2500);
    console.log("  textLen=" + (await page.evaluate(() => document.body.innerText.length)));
    console.log("  tab LỊCH SỬ:", await click(page, "LỊCH SỬ"));
    await page.waitForTimeout(2000);
    console.log("  after history textLen=" + (await page.evaluate(() => document.body.innerText.length)));
  });

  await scenario("quiz game answer (user)", "user@gmail.com", async page => {
    await page.goto(H.APP + "/decks/10006/play/quiz", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(3000);
    const before = await page.evaluate(() => document.body.innerText.length);
    console.log("  answer:", await click(page, "xác định"));
    await page.waitForTimeout(2500);
    const after = await page.evaluate(() => document.body.innerText.slice(0, 400));
    console.log("  before=" + before + " after-snippet=" + JSON.stringify(after.slice(-160)));
  });

  await scenario("flashcard review SRS (user)", "user@gmail.com", async page => {
    await page.goto(H.APP + "/decks/10006/play/flashcard", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(3000);
    console.log("  next:", await click(page, "Tiếp theo"));
    await page.waitForTimeout(2000);
    console.log("  easy:", await click(page, "Dễ"));
    await page.waitForTimeout(2500);
  });

  await scenario("speaking history open (user)", "user@gmail.com", async page => {
    await page.goto(H.APP + "/speaking/history", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(2500);
    console.log("  open:", await click(page, "Mở bản ghi"));
    await page.waitForTimeout(2500);
    console.log("  path=" + page.url() + " textLen=" + (await page.evaluate(() => document.body.innerText.length)));
  });

  await scenario("AI vocab generator (user, real Ollama)", "user@gmail.com", async page => {
    await page.goto(H.APP + "/ai-vocab-generator", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(1500);
    await page.fill("input[type=text]", "Travel");
    await page.fill("input[type=number]", "3");
    await click(page, "Sinh từ vựng");
    await page.waitForTimeout(45000);
    console.log("  rows=" + (await page.evaluate(() => document.querySelectorAll("table tr, .vocab-card, li").length))
      + " textLen=" + (await page.evaluate(() => document.body.innerText.length)));
  });

  await scenario("admin lessons CRUD via UI", "admin@gmail.com", async page => {
    await page.goto(H.APP + "/admin/lessons", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(2500);
    console.log("  textLen=" + (await page.evaluate(() => document.body.innerText.length)));
    console.log("  search:", await page.evaluate(() => {
      const i = document.querySelector("input"); if (!i) return "no input";
      return "has input " + (i.placeholder || "");
    }));
    const inp = await page.$("input");
    if (inp) { await inp.fill("Reading"); await page.waitForTimeout(2500); }
    console.log("  after filter textLen=" + (await page.evaluate(() => document.body.innerText.length)));
  });

  await scenario("admin dashboard + users", "admin@gmail.com", async page => {
    await page.goto(H.APP + "/admin/dashboard", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(3000);
    const donut = await page.evaluate(() => {
      const el = document.querySelector("[style*=conic-gradient]");
      return el ? getComputedStyle(el).backgroundImage.slice(0, 90) : "no donut";
    });
    console.log("  donut: " + JSON.stringify(donut));
    await page.goto(H.APP + "/admin/users", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(2500);
    console.log("  users textLen=" + (await page.evaluate(() => document.body.innerText.length)));
  });

  await scenario("guest register form validation", "user@gmail.com", async page => {
    await page.goto(H.APP + "/register", { waitUntil: "domcontentloaded" });
    await page.evaluate(() => localStorage.clear());
    await page.reload({ waitUntil: "domcontentloaded" });
    await page.waitForTimeout(1500);
    const n = await page.evaluate(() => document.querySelectorAll("input").length);
    console.log("  inputs=" + n);
    await page.click("button[type=submit]").catch(() => {});
    await page.waitForTimeout(2000);
    console.log("  after empty submit textLen=" + (await page.evaluate(() => document.body.innerText.length)));
  });

  await scenario("logout clears session", "user@gmail.com", async page => {
    console.log("  logged in at", await page.evaluate(() => location.pathname));
    console.log("  Thoát:", await click(page, "Thoát"));
    await page.waitForTimeout(2500);
    console.log("  after: " + JSON.stringify(await page.evaluate(() => ({ path: location.pathname, tok: !!localStorage.getItem("token") }))));
  });
})().catch(e => { console.log("FATAL", e.message); process.exit(1); });
