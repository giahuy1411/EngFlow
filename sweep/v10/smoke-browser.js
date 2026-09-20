/**
 * audit-v10 — smoke test: browser co that su chay duoc khong.
 * Mo trang chu, do vai chi so, dong. Khong ket luan gi ve design.
 */
const { launch, APP, EXECUTABLE } = require("./ui-lib");

(async () => {
  console.log("executable:", EXECUTABLE);
  console.log("APP:", APP);

  const browser = await launch();
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();

  const errors = [];
  page.on("console", (m) => { if (m.type() === "error") errors.push(m.text().slice(0, 200)); });
  page.on("pageerror", (e) => errors.push("PAGEERROR " + e.message.slice(0, 200)));

  const t0 = Date.now();
  const resp = await page.goto(APP + "/", { waitUntil: "domcontentloaded", timeout: 30000 });
  const loadMs = Date.now() - t0;

  console.log("status:", resp && resp.status());
  console.log("load:", loadMs + "ms");
  console.log("title:", await page.title());
  console.log("url:", page.url());

  const probe = await page.evaluate(() => {
    const cs = getComputedStyle(document.body);
    const fonts = new Set();
    document.querySelectorAll("*").forEach((el) => {
      if (el.children.length === 0 && el.textContent && el.textContent.trim()) {
        fonts.add(getComputedStyle(el).fontFamily);
      }
    });
    return {
      bodyFont: cs.fontFamily,
      bodyBg: cs.backgroundColor,
      docWidth: document.documentElement.scrollWidth,
      winWidth: window.innerWidth,
      fontFamilies: [...fonts].slice(0, 6),
    };
  });

  console.log("body font:", probe.bodyFont);
  console.log("body bg  :", probe.bodyBg);
  console.log("overflow :", probe.docWidth, "vs", probe.winWidth,
              probe.docWidth > probe.winWidth ? "  <<< OVERFLOW" : "  ok");
  console.log("font families dung trong text node:", JSON.stringify(probe.fontFamilies));
  console.log("console errors:", errors.length);
  errors.slice(0, 5).forEach((e) => console.log("   ", e));

  await browser.close();
  console.log("\nSMOKE: browser chay duoc.");
})().catch((e) => { console.error("FAIL:", e.message); process.exit(1); });
