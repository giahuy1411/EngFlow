/**
 * Lan 3: bam tab "BAI TAP" roi xem that.
 */
const { launch, APP, loginFull, mapUser } = require("./ui-lib");
const path = require("path");

const LESSON = 11516;

(async () => {
  const user = await loginFull("user@gmail.com", "123456");
  const browser = await launch();
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 1400 } });
  const page = await ctx.newPage();

  await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((x) => {
    localStorage.setItem("token", x.token);
    localStorage.setItem("user", JSON.stringify(x.user));
  }, { token: user.token, user: mapUser(user.user) });

  await page.goto(APP + `/lessons/${LESSON}`, { waitUntil: "networkidle" });
  await page.waitForTimeout(3000);

  // Bam tab "BAI TAP"
  const clicked = await page.evaluate(() => {
    const els = [...document.querySelectorAll("a,button")];
    const tab = els.find((e) => /^BÀI TẬP$/i.test((e.textContent || "").trim()));
    if (tab) { tab.click(); return true; }
    return false;
  });
  console.log("bam duoc tab BAI TAP:", clicked);
  await page.waitForTimeout(4000);

  console.log("url sau khi bam:", page.url());

  const info = await page.evaluate(() => {
    const out = {};
    out.nutNghe = [...document.querySelectorAll("button")].filter((b) =>
      /Nghe/i.test(b.textContent || "")).map((b) => (b.textContent || "").trim().slice(0, 40));
    out.theAudio = [...document.querySelectorAll("audio")].map((a) => a.getAttribute("src"));
    out.oNhap = document.querySelectorAll("input[type=text]").length;
    out.nutLuaChon = [...document.querySelectorAll("button")].filter((b) =>
      /^[A-D][.)]\s/.test((b.textContent || "").trim())).length;
    out.nhanNghe = [...document.querySelectorAll("*")].filter((e) =>
      !e.children.length && /Nghe & trả lời/i.test(e.textContent || ""))
      .map((e) => (e.textContent || "").trim().slice(0, 60));
    return out;
  });

  console.log("");
  console.log("=== UI trang BAI TAP ===");
  console.log("  nhan 'Nghe & tra loi' :", JSON.stringify(info.nhanNghe));
  console.log("  nut 'Nghe'            :", JSON.stringify(info.nutNghe));
  console.log("  the <audio>           :", JSON.stringify(info.theAudio));
  console.log("  so o nhap tay         :", info.oNhap);
  console.log("  so nut lua chon A-D   :", info.nutLuaChon);

  const txt = await page.evaluate(() => (document.body.innerText || "").slice(0, 1800));
  console.log("");
  console.log("=== CHU TREN TRANG ===");
  console.log(txt);

  await page.screenshot({ path: path.join(__dirname, "bad-listening-tab.png"), fullPage: true });
  console.log("");
  console.log("da chup: sweep/v10/bad-listening-tab.png");

  await browser.close();
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
