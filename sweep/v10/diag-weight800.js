/**
 * Chan doan: weight 800 co duoc tai that, hay bi trinh duyet GIA LAP?
 *
 * Cach phan biet: do chieu rong cung mot chuoi o 4 truong hop.
 *   - 800 "that" (neu co face) se KHAC 700 va KHAC 900
 *   - 800 bi gia lap tu 700 se TRUNG KHIT 700
 *   - 800 bi gia lap tu 900 se TRUNG KHIT 900
 *
 * Neu chieu rong cua 800 bang dung 700 hoac 900 -> do la gia lap.
 */
const { launch, APP } = require("./ui-lib");

(async () => {
  const browser = await launch();
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  await page.goto(APP + "/", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(2000);

  const r = await page.evaluate(async () => {
    await document.fonts.ready;

    // Do chieu rong mot chuoi o tung weight
    const probe = document.createElement("span");
    probe.textContent = "Hamburgefonstiv 0123456789";
    probe.style.cssText = "position:absolute;left:-9999px;white-space:nowrap;font-size:40px;font-family:'Be Vietnam Pro',sans-serif;";
    document.body.appendChild(probe);

    const widths = {};
    for (const w of [400, 500, 600, 700, 800, 900]) {
      probe.style.fontWeight = String(w);
      widths[w] = probe.getBoundingClientRect().width;
    }
    probe.remove();

    // Cac face thuc su co trong FontFaceSet
    const faces = [...document.fonts]
      .filter((f) => /Be Vietnam/i.test(f.family))
      .map((f) => ({ weight: f.weight, status: f.status }));

    // Cac element dang dung weight 800 — chung la AI?
    const at800 = [];
    document.querySelectorAll("*").forEach((el) => {
      if (el.children.length) return;
      if (!(el.textContent || "").trim()) return;
      const cs = getComputedStyle(el);
      if (cs.display === "none") return;
      if (cs.fontWeight === "800") {
        at800.push({
          tag: el.tagName,
          cls: String(el.className).slice(0, 40),
          text: (el.textContent || "").trim().slice(0, 24),
          font: cs.fontFamily.split(",")[0].replace(/["']/g, "").trim(),
        });
      }
    });

    return { widths, faces, at800: at800.slice(0, 12), at800Count: at800.length };
  });

  console.log("=== weight 800: that hay gia lap? ===");
  console.log("");
  console.log("chieu rong chuoi mau o tung weight (font-size 40px):");
  for (const [w, px] of Object.entries(r.widths)) {
    console.log(`  ${w}: ${px.toFixed(3)}px`);
  }
  console.log("");
  const w700 = r.widths[700], w800 = r.widths[800], w900 = r.widths[900];
  console.log("so sanh:");
  console.log("  800 vs 700 : " + (Math.abs(w800 - w700) < 0.01 ? "TRUNG KHIT -> 800 bi GIA LAP tu 700" : "khac (" + (w800 - w700).toFixed(3) + "px)"));
  console.log("  800 vs 900 : " + (Math.abs(w800 - w900) < 0.01 ? "TRUNG KHIT -> 800 bi GIA LAP tu 900" : "khac (" + (w800 - w900).toFixed(3) + "px)"));
  console.log("  700 vs 900 : " + (Math.abs(w700 - w900) < 0.01 ? "trung khit (bat thuong)" : "khac (" + (w700 - w900).toFixed(3) + "px) — 2 face rieng biet"));
  console.log("");
  console.log("face Be Vietnam Pro dang co:");
  const byW = {};
  for (const f of r.faces) byW[f.weight] = (byW[f.weight] || 0) + 1;
  console.log("  " + JSON.stringify(byW));
  console.log("");
  console.log(`element dung weight 800: ${r.at800Count}`);
  r.at800.forEach((e) => console.log(`  ${e.tag} "${e.text}" class="${e.cls}" font=${e.font}`));

  await browser.close();
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
