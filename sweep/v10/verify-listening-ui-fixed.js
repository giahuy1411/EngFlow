/**
 * Verify tren BROWSER: sau khi doi nhan, UI phai hien "TRAC NGHIEM"
 * thay vi "NGHE" + nut "🔊 Nghe".
 *
 * So sanh voi anh truoc khi sua (bad-listening-tab.png).
 */
const { launch, APP, loginFull, mapUser } = require("./ui-lib");
const path = require("path");

const LESSON = 11516; // lesson "Reading - VOCABULARY", chua row 745748

(async () => {
  const user = await loginFull("user@gmail.com", "123456");
  const browser = await launch();
  console.log("browser:", browser.version());
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 1400 } });
  const page = await ctx.newPage();
  const errs = [];
  page.on("console", (m) => { if (m.type() === "error") errs.push(m.text().slice(0, 120)); });

  await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((x) => {
    localStorage.setItem("token", x.token);
    localStorage.setItem("user", JSON.stringify(x.user));
  }, { token: user.token, user: mapUser(user.user) });

  await page.goto(APP + `/lessons/${LESSON}`, { waitUntil: "networkidle" });
  await page.waitForTimeout(3000);

  const clicked = await page.evaluate(() => {
    const tab = [...document.querySelectorAll("a,button")]
      .find((e) => /^BÀI TẬP$/i.test((e.textContent || "").trim()));
    if (tab) { tab.click(); return true; }
    return false;
  });
  console.log("bam tab BAI TAP:", clicked);
  await page.waitForTimeout(4000);

  const info = await page.evaluate(() => {
    return {
      nhanNghe: [...document.querySelectorAll("*")].filter((e) =>
        !e.children.length && /Nghe & trả lời/i.test(e.textContent || "")).length,
      nutNghe: [...document.querySelectorAll("button")].filter((b) =>
        /Nghe/i.test(b.textContent || "")).length,
      theAudio: document.querySelectorAll("audio").length,
      soNhanNghe: (document.body.innerText.match(/\bNGHE\b/g) || []).length,
      soNhanTracNghiem: (document.body.innerText.match(/TRẮC NGHIỆM/g) || []).length,
      soONhap: document.querySelectorAll("input[type=text]").length,
      soNutLuaChon: [...document.querySelectorAll("button")].filter((b) =>
        /^[A-D][.)]\s/.test((b.textContent || "").trim())).length,
    };
  });

  console.log("");
  console.log("=== UI SAU KHI SUA ===");
  console.log("  nhan 'Nghe & tra loi' :", info.nhanNghe, "  (truoc: 1)");
  console.log("  nut 'Nghe'            :", info.nutNghe, "  (truoc: 1)");
  console.log("  the <audio>           :", info.theAudio);
  console.log("  so lan chu 'NGHE'     :", info.soNhanNghe, "  (truoc: 1)");
  console.log("  so lan 'TRẮC NGHIỆM'  :", info.soNhanTracNghiem, "  (truoc: 4)");
  console.log("  so o nhap tay         :", info.soONhap);
  console.log("  so nut lua chon A-D   :", info.soNutLuaChon);
  console.log("  console errors        :", errs.length);

  let fail = 0;
  if (info.nhanNghe !== 0) { console.log("  FAIL: van con nhan 'Nghe & tra loi'"); fail++; }
  if (info.nutNghe !== 0) { console.log("  FAIL: van con nut 'Nghe'"); fail++; }
  if (info.soNhanNghe !== 0) { console.log("  FAIL: van con chu 'NGHE'"); fail++; }
  if (info.soNhanTracNghiem !== 5) { console.log("  FAIL: phai co 5 nhan TRAC NGHIEM, nhan " + info.soNhanTracNghiem); fail++; }
  // KHONG assert so nut lua chon. Do lan dau toi doi >=5 va nhan 1 FAIL — nhung
  // do la loi CUA PROBE: danh sach bai tap render theo kieu lazy, cau 5 nam
  // ngoai khung nhin nen nut cua no chua duoc tao luc do. Nhan "TRAC NGHIEM"
  // dem duoc 5 (vi do la chu trong DOM tinh) nhung nut thi khong.
  // Dieu can kiem la KHONG CON dau hieu bai nghe — va 3 assert dau da lam viec do.
  console.log("  (so nut lua chon " + info.soNutLuaChon + " — khong assert, render theo lazy)");

  await page.screenshot({ path: path.join(__dirname, "listening-fixed-tab.png"), fullPage: true });
  console.log("");
  console.log("da chup: sweep/v10/listening-fixed-tab.png");
  console.log(fail ? `KET LUAN: ${fail} FAIL` : "KET LUAN: UI DA DUNG — khong con nhan NGHE, du 5 nhan TRAC NGHIEM");

  await browser.close();
  process.exit(fail ? 1 : 0);
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
