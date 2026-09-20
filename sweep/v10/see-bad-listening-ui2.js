/**
 * Lan 2: cho UI render lau hon, va chup man hinh de XEM THAT.
 */
const { launch, APP, loginFull, mapUser } = require("./ui-lib");

const LESSON = 11516;

(async () => {
  const user = await loginFull("user@gmail.com", "123456");
  const browser = await launch();
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 1400 } });
  const page = await ctx.newPage();

  const errs = [];
  page.on("console", (m) => { if (m.type() === "error") errs.push(m.text().slice(0, 150)); });

  await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((x) => {
    localStorage.setItem("token", x.token);
    localStorage.setItem("user", JSON.stringify(x.user));
  }, { token: user.token, user: mapUser(user.user) });

  await page.goto(APP + `/lessons/${LESSON}`, { waitUntil: "networkidle" });
  await page.waitForTimeout(6000);

  console.log("=== lesson " + LESSON + " ===");
  console.log("url:", page.url());
  console.log("console errors:", errs.length);
  errs.slice(0, 3).forEach((e) => console.log("   " + e));

  const txt = await page.evaluate(() => (document.body.innerText || "").slice(0, 2500));
  console.log("");
  console.log("=== TOAN BO CHU TREN TRANG (2500 ky tu dau) ===");
  console.log(txt);

  await page.screenshot({ path: require("path").join(__dirname, "bad-listening-ui.png"), fullPage: true });
  console.log("");
  console.log("da chup: sweep/v10/bad-listening-ui.png");

  await browser.close();
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
