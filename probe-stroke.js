const pw = require("playwright-core");
(async () => {
  const b = await pw.chromium.launch({ headless: true });
  const p = await (await b.newContext({viewport:{width:1440,height:900}})).newPage();
  await p.goto("http://localhost:5173/", { waitUntil: "domcontentloaded" });
  await p.waitForTimeout(2500);
  const r = await p.evaluate(() => {
    const svs = [...document.querySelectorAll("svg.lucide")];
    if (!svs.length) return { none: true, anySvg: document.querySelectorAll("svg").length };
    const sv = svs[0];
    const cs = getComputedStyle(sv);
    return {
      count: svs.length,
      rawStrokeWidth: JSON.stringify(cs.strokeWidth),
      parseFloat: parseFloat(cs.strokeWidth),
      attrStrokeWidth: sv.getAttribute("stroke-width"),
      cssRule: [...document.styleSheets].flatMap(s => { try { return [...s.cssRules] } catch(e){ return [] } })
        .filter(r => r.selectorText === ".lucide").map(r => r.style.strokeWidth),
      distinct: [...new Set(svs.map(s => getComputedStyle(s).strokeWidth))]
    };
  });
  console.log(JSON.stringify(r, null, 1));
  await b.close();
})();
