const H = require("./lib.js");
(async () => {
  const b = await H.pw.chromium.launch({ headless: true });
  const p = await (await b.newContext({ viewport: { width: 1440, height: 900 } })).newPage();
  await p.goto(H.APP + "/", { waitUntil: "domcontentloaded" });
  await p.waitForTimeout(2500);
  console.log("baseline", await p.evaluate(() => ({ client: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth, offset: document.documentElement.offsetWidth })));
  const link = await p.$(".skip-link");
  console.log("skip-link found:", !!link, link && await link.evaluate(el => { const s = getComputedStyle(el); return { left: s.left, pos: s.position, width: s.width }; }));
  console.log("after display:none", await p.evaluate(() => { const a = document.querySelector(".skip-link"); if (a) a.style.display = "none"; return { client: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth }; }));
  await p.reload(); await p.waitForTimeout(2000);
  console.log("after clip-path hide", await p.evaluate(() => { const a = document.querySelector(".skip-link"); if (a) { a.style.left = "0px"; a.style.clipPath = "inset(50%)"; a.style.width = "1px"; a.style.height = "1px"; a.style.overflow = "hidden"; a.style.position = "absolute"; } return { client: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth }; }));
  await b.close();
})();
