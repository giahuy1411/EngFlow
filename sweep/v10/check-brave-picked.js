/** Xac nhan harness chon Brave, va Brave chay duoc that. */
const { launch, APP, EXECUTABLE } = require("./ui-lib");

(async () => {
  console.log("harness chon browser:", EXECUTABLE);
  const browser = await launch();
  console.log("version:", await browser.version());

  const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await ctx.newPage();
  const errs = [];
  page.on("console", (m) => { if (m.type() === "error") errs.push(m.text().slice(0, 120)); });

  const r = await page.goto(APP + "/", { waitUntil: "domcontentloaded", timeout: 30000 });
  await page.waitForTimeout(2000);

  const probe = await page.evaluate(() => {
    const cs = getComputedStyle(document.body);
    const fams = new Set();
    document.querySelectorAll("*").forEach((el) => {
      if (el.children.length) return;
      if (!(el.textContent || "").trim()) return;
      fams.add(getComputedStyle(el).fontFamily.split(",")[0].replace(/["']/g, "").trim());
    });
    return {
      font: cs.fontFamily,
      bg: cs.backgroundColor,
      overflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
      fams: [...fams],
    };
  });

  console.log("status:", r.status());
  console.log("title :", await page.title());
  console.log("font  :", probe.font);
  console.log("bg    :", probe.bg);
  console.log("overflow:", probe.overflow);
  console.log("families:", JSON.stringify(probe.fams));
  console.log("console errors:", errs.length);

  await browser.close();
  console.log("");
  console.log("KET LUAN: Brave chay duoc voi harness.");
})().catch((e) => { console.error("FAIL:", e.message); process.exit(1); });
