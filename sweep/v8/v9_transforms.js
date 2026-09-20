const H = require("./ui/lib.js");
(async () => {
  const b = await H.pw.chromium.launch({ headless: true });
  const ctx = await b.newContext({ viewport: { width: 1440, height: 900 } });
  const p = await ctx.newPage();
  await p.goto(H.APP + "/index.html", { waitUntil: "domcontentloaded" });
  await p.waitForTimeout(2500);
  const info = await p.evaluate(() => {
    const out = [];
    document.querySelectorAll("*").forEach((e) => {
      const t = getComputedStyle(e).transform;
      if (t && t !== "none" && t.startsWith("matrix")) {
        const m = t.match(/matrix\(([^)]+)\)/);
        if (!m) return;
        const v = m[1].split(",").map((x) => parseFloat(x));
        const [a, b2, c, d, e2, f] = v;
        const scaleX = Math.hypot(a, b2), scaleY = Math.hypot(c, d);
        const rot = Math.round(Math.atan2(b2, a) * 180 / Math.PI * 100) / 100;
        const cls = typeof e.className === "string" ? e.className.slice(0, 42) : "";
        out.push({ tag: e.tagName, cls, rot, scaleX: Math.round(scaleX * 1000) / 1000, scaleY: Math.round(scaleY * 1000) / 1000 });
      }
    });
    return { transforms: out.slice(0, 14), counts: { total: out.length,
      nonIdentityRot: out.filter((x) => Math.abs(x.rot) > 0.01).length,
      scaled: out.filter((x) => Math.abs(x.scaleX - 1) > 0.001 || Math.abs(x.scaleY - 1) > 0.001).length } };
  });
  console.log(JSON.stringify(info, null, 1));
  await b.close();
})().catch((e) => console.log("ERR", e.message));