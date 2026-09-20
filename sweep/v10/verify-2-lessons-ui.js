/**
 * Verify UI 2 lesson sau khi xoa: khong con nut "🔊 Nghe" doc cau lenh.
 */
const { launch, APP, loginFull, mapUser } = require("./ui-lib");

const LESSONS = [11477, 11539];

(async () => {
  const user = await loginFull("user@gmail.com", "123456");
  const browser = await launch();
  console.log("browser:", browser.version());
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 1400 } });
  const page = await ctx.newPage();

  await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((x) => {
    localStorage.setItem("token", x.token);
    localStorage.setItem("user", JSON.stringify(x.user));
  }, { token: user.token, user: mapUser(user.user) });

  let fail = 0;
  for (const L of LESSONS) {
    const errs = [];
    const onErr = (m) => { if (m.type() === "error") errs.push(m.text().slice(0, 120)); };
    page.on("console", onErr);

    await page.goto(APP + `/lessons/${L}`, { waitUntil: "networkidle" });
    await page.waitForTimeout(3000);

    const clicked = await page.evaluate(() => {
      const tab = [...document.querySelectorAll("a,button")]
        .find((e) => /^BÀI TẬP$/i.test((e.textContent || "").trim()));
      if (tab) { tab.click(); return true; }
      return false;
    });
    await page.waitForTimeout(4000);

    const info = await page.evaluate(() => ({
      nutNghe: [...document.querySelectorAll("button")].filter((b) => /Nghe/i.test(b.textContent || "")).length,
      chuNghe: (document.body.innerText.match(/\bNGHE\b/g) || []).length,
      tracNghiem: (document.body.innerText.match(/TRẮC NGHIỆM/g) || []).length,
      theAudio: document.querySelectorAll("audio").length,
      soCau: (document.body.innerText.match(/Nộp bài \(\d+ câu\)/) || [])[0] || "(khong thay)",
    }));

    page.off("console", onErr);

    console.log("");
    console.log(`=== lesson ${L} ===`);
    console.log("  bam tab BAI TAP:", clicked);
    console.log("  nut 'Nghe'      :", info.nutNghe);
    console.log("  chu 'NGHE'      :", info.chuNghe);
    console.log("  'TRẮC NGHIỆM'   :", info.tracNghiem);
    console.log("  the <audio>     :", info.theAudio);
    console.log("  so cau          :", info.soCau);
    console.log("  console errors  :", errs.length);

    if (info.nutNghe !== 0) { console.log("  FAIL: van con nut Nghe"); fail++; }
    if (info.chuNghe !== 0) { console.log("  FAIL: van con chu NGHE"); fail++; }
    if (errs.length) { console.log("  FAIL: co console error"); fail++; }
  }

  await browser.close();
  console.log("");
  console.log(fail ? `KET LUAN: ${fail} FAIL` : "KET LUAN: 2 lesson da sach — khong con dau hieu bai nghe sai");
  process.exit(fail ? 1 : 0);
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
