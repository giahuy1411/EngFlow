const H = require("./lib.js");
(async () => {
  const browser = await H.pw.chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  await page.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1200);
  await page.fill("input[type=email]", "admin@gmail.com");
  await page.fill("input[type=password]", "123456");
  await page.click("button[type=submit]");
  await page.waitForTimeout(3500);
  for (const r of ["/lessons", "/videos", "/admin/lessons", "/admin/users", "/admin/videos", "/admin/447/build", "/admin/exercises"]) {
    await page.goto(H.APP + r, { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(2500);
    const info = await page.evaluate(() => {
      const miss = [], emptyAlt = [];
      document.querySelectorAll("img").forEach(i => {
        if (!i.hasAttribute("alt")) miss.push(i.getAttribute("src") ? i.getAttribute("src").slice(0, 70) : "(no src)");
        else if (i.getAttribute("alt") === "") emptyAlt.push(1);
      });
      return { imgs: document.querySelectorAll("img").length, noAttr: miss.length, sample: miss.slice(0, 5), altEmpty: emptyAlt.length };
    });
    console.log(r.padEnd(20), JSON.stringify(info));
  }
  await browser.close();
})();
