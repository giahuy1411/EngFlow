const H = require("./lib.js");
const q = String.fromCharCode(34);
function S(s) { return q + s.replace(/"/g, '') + q; }

const PAGES = ["/", "/lessons", "/lessons/445", "/decks", "/decks/10006", "/speaking",
  "/premium", "/login", "/leaderboard", "/videos", "/admin/dashboard", "/admin/lessons", "/admin/exercises"];

(async () => {
  const browser = await H.pw.chromium.launch({ headless: true });
  // audit-v15 L1-d: PAGES includes "/premium". It does not mint a row TODAY (only
  // "/premium/checkout" does), but that is an assumption about a view this file does
  // not own — if PremiumPage ever calls create-order, this sweep would leak a real
  // payment row with NO cleanup and no assertion. Clean unconditionally in `finally`.
  let clean = { ok: false };
  try {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  await page.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1200);
  await page.fill("input[type=email]", "admin@gmail.com");
  await page.fill("input[type=password]", "123456");
  await page.click("button[type=submit]");
  await page.waitForTimeout(3000);

  const tot = { badFont: 0, thinBorder: 0, noShadow: 0, smallBtn24: 0, smallBtn44: 0, imgNoAlt: 0, btnNoName: 0, noFocus: 0, hoverTr: 0, h1Weight: [] };
  for (const r of PAGES) {
    await page.goto(H.APP + r, { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(1600);
    const m = await page.evaluate(() => {
      const out = { badFont: [], thinBorder: 0, sampled: 0, noShadow: 0, btnShadowed: 0, imgs: 0, imgNoAlt: 0, btnNoName: 0, h1: [], small: 0, hoverTr: 0 };
      const fam = e => getComputedStyle(e).fontFamily.split(",")[0].replace(/["']/g, "");
      document.querySelectorAll("h1,h2,h3,h4,p,a,button,span,label,li,td,th,input,textarea,select,div").forEach(e => {
        if (!e.textContent && e.tagName !== "INPUT") return;
        out.sampled++;
        const f = fam(e);
        if (f && f !== "Be Vietnam Pro" && f !== "system-ui" && out.badFont.length < 5) out.badFont.push(e.tagName + ":" + f + ":" + (e.className || "").slice(0, 30));
      });
      document.querySelectorAll("button, .app-btn, a.app-btn, .app-card, .sticker-card, .app-input").forEach(e => {
        const cs = getComputedStyle(e);
        if (cs.borderWidth !== "2px" && parseFloat(cs.borderTopWidth) < 2 && cs.borderStyle !== "none" && out.thinBorder < 3) out.thinBorder++;
        if (cs.boxShadow !== "none") out.btnShadowed++; else out.noShadow++;
      });
      // audit-v8 vòng 3: alt="" là HỢP LỆ (ảnh trang trí). Chỉ đếm ảnh THIẾU HẲN attribute.
      document.querySelectorAll("img").forEach(i => { out.imgs++; if (!i.hasAttribute("alt")) out.imgNoAlt++; });
      document.querySelectorAll("button").forEach(b => {
        const t = (b.innerText || "").trim();
        if (!t && !b.getAttribute("aria-label") && !b.getAttribute("title")) out.btnNoName++;
        const r2 = b.getBoundingClientRect();
        // WCAG 2.5.8 (AA) = 24px; WCAG 2.5.5 (AAA) = 44px. Đếm riêng để không lẫn 2 ngưỡng.
        if (r2.height > 0) { if (r2.height < 24) out.small24 = (out.small24 || 0) + 1; else if (r2.height < 44) out.small44 = (out.small44 || 0) + 1; }
      });
      document.querySelectorAll("h1").forEach(h => out.h1.push(h.innerText.trim().slice(0, 28) + "|" + getComputedStyle(h).fontWeight));
      return out;
    });
    tot.badFont += m.badFont.length;
    tot.thinBorder += m.thinBorder;
    tot.noShadow += m.noShadow;
    tot.smallBtn24 += (m.small24 || 0);
    tot.smallBtn44 += (m.small44 || 0);
    tot.imgNoAlt += m.imgNoAlt;
    tot.btnNoName += m.btnNoName;
    m.h1.forEach(h => tot.h1Weight.push(h));
    if (m.badFont.length) console.log("  FONT " + r + " " + JSON.stringify(m.badFont));
    console.log(("  " + r).padEnd(22) + "sampled=" + m.sampled + " shadowed=" + m.btnShadowed + " noShadow=" + m.noShadow
      + " thinBorder=" + m.thinBorder + " imgNoAlt=" + m.imgNoAlt + " btnNoName=" + m.btnNoName
      + " small24=" + (m.small24 || 0) + " small44=" + (m.small44 || 0));
  }
  console.log("TOTALS " + JSON.stringify({ ...tot, h1Weight: [...new Set(tot.h1Weight)].slice(0, 12) }));

  // mobile: tap targets + reduced motion + focus visibility
  await page.setViewportSize({ width: 375, height: 812 });
  await page.goto(H.APP + "/lessons", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(2000);
  const mob = await page.evaluate(() => {
    let small = 0, small44 = 0, total = 0;
    document.querySelectorAll("button, a").forEach(b => {
      const r = b.getBoundingClientRect();
      if (r.height > 0 && r.width > 0) { total++; if (r.height < 24) small++; else if (r.height < 44) small44++; }
    });
    const skip = document.querySelector(".skip-link");
    return { total, smallLt24: small, smallLt44: small44, hasSkip: !!skip };
  });
  console.log("MOBILE375 " + JSON.stringify(mob));
  const c375 = await ctx.newPage();
  await c375.emulateMedia({ reducedMotion: "reduce" });
  await c375.goto(H.APP + "/lessons", { waitUntil: "domcontentloaded" });
  await c375.waitForTimeout(1800);
  console.log("REDUCED-MOTION transitions:", JSON.stringify(await c375.evaluate(() => {
    const b = document.querySelector("button, a.app-btn, .app-btn");
    return b ? { dur: getComputedStyle(b).transitionDuration, transform: getComputedStyle(b).transform } : "none";
  })));
  } finally {
    await browser.close();
    clean = H.cleanupAuditPayments(H.PAYMENTS_BASELINE);
    console.log("DB parity after cleanup: " + H.dbParity() + "   (baseline " + H.PARITY_BASELINE + ")");
    try { H.assertClean({ parity: H.PARITY_BASELINE, studyDays: H.STUDY_DAYS_BASELINE, pendingPayments: 0 }); }
    catch (e) { console.error("RESIDUE: " + e.message); process.exitCode = 1; }
  }
  if (!clean.ok) process.exitCode = 1;
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
