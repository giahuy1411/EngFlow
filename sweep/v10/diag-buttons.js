/**
 * Chan doan: liet ke TAT CA .app-btn tren Home + trang thai connector that.
 * Muc dich: phan biet "UI sai" voi "probe chon sai element".
 */
const { launch, APP, loginFull, mapUser } = require("./ui-lib");

(async () => {
  const admin = await loginFull("admin@gmail.com", "123456");
  const browser = await launch();
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();

  await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((x) => {
    localStorage.setItem("token", x.token);
    localStorage.setItem("user", JSON.stringify(x.user));
  }, { token: admin.token, user: mapUser(admin.user) });
  await page.goto(APP + "/", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1800);

  const info = await page.evaluate(() => {
    const btns = [...document.querySelectorAll(".app-btn")].map((b, i) => {
      const cs = getComputedStyle(b);
      return {
        i,
        cls: b.className,
        text: (b.textContent || "").trim().slice(0, 24),
        variant: [...b.classList].find((c) => c.startsWith("app-btn--")) || "?",
        shadow: cs.boxShadow,
        border: cs.borderTopWidth,
        radius: cs.borderRadius,
        hasArrow: !!b.querySelector(".app-btn__arrow"),
        hasSvg: !!b.querySelector("svg"),
        inHeader: !!b.closest("header, .app-navbar, nav"),
      };
    });

    // connector: doc ca <svg> lan <path> ben trong
    const conn = document.querySelector(".app-features__connector");
    const connInfo = conn ? {
      svgAttr: conn.getAttribute("stroke-dasharray"),
      svgDisplay: getComputedStyle(conn).display,
      paths: [...conn.querySelectorAll("path")].map((p) => ({
        d: p.getAttribute("d"),
        stroke: p.getAttribute("stroke"),
        width: p.getAttribute("stroke-width"),
        dasharrayAttr: p.getAttribute("stroke-dasharray"),
        dasharrayComputed: getComputedStyle(p).strokeDasharray,
        strokeComputed: getComputedStyle(p).stroke,
      })),
    } : null;

    return { btns, connInfo };
  });

  console.log("=== .app-btn tren Home (1440px) ===");
  for (const b of info.btns) {
    console.log(`[${b.i}] ${b.variant.padEnd(22)} "${b.text}"`);
    console.log(`     shadow=${b.shadow}`);
    console.log(`     border=${b.border} radius=${b.radius} arrow=${b.hasArrow} svg=${b.hasSvg} header=${b.inHeader}`);
  }

  console.log("");
  console.log("=== .app-features__connector ===");
  console.log(JSON.stringify(info.connInfo, null, 2));

  await browser.close();
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
