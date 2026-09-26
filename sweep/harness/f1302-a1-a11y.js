/**
 * a11y of the lesson content panel AFTER the Lesson Builder removal.
 *
 * The v13 original measured the authored-blocks section, which no longer exists. The
 * surviving "Nội dung" tab is the learner-facing surface, so the same WCAG 2.2 AA
 * contrast discipline is applied there, reusing the repo's proven COMPOSITE_FN
 * (composite-alpha, bottom-up) rather than a second contrast implementation.
 *
 * Run: node sweep/harness/f1302-a1-a11y.js [--audit <name>] [--out <dir>]
 */
const path = require("path");
const fs = require("fs");
const { OUT: EVID } = require("./_config.js");
const H = require(path.join(__dirname, "..", "v8", "ui", "lib.js"));
const { pw, APP, loginFull, seedAuth } = H;

const COMPOSITE_FN = `
function parseColor(s){ const m=/rgba?\(([^)]+)\)/.exec(s||''); if(!m) return null;
  const p=m[1].split(',').map(x=>parseFloat(x.trim())); return {r:p[0],g:p[1],b:p[2],a:p.length>3?p[3]:1}; }
function over(fg,bg){ const a=fg.a+bg.a*(1-fg.a); if(a===0) return {r:255,g:255,b:255,a:0};
  return {r:(fg.r*fg.a+bg.r*bg.a*(1-fg.a))/a,g:(fg.g*fg.a+bg.g*bg.a*(1-fg.a))/a,b:(fg.b*fg.a+bg.b*bg.a*(1-fg.a))/a,a}; }
function effectiveBg(el){ const layers=[]; let n=el;
  while(n&&n!==document.documentElement.parentNode){ const c=parseColor(getComputedStyle(n).backgroundColor);
    if(c&&c.a>0) layers.push(c); if(c&&c.a===1) break; n=n.parentElement; }
  let base={r:255,g:255,b:255,a:1}; for(let i=layers.length-1;i>=0;i--) base=over(layers[i],base); return base; }
function lum(c){ const f=v=>{v/=255; return v<=0.03928?v/12.92:Math.pow((v+0.055)/1.055,2.4);}; return 0.2126*f(c.r)+0.7152*f(c.g)+0.0722*f(c.b); }
function ratio(a,b){ const L1=lum(a),L2=lum(b); return (Math.max(L1,L2)+0.05)/(Math.min(L1,L2)+0.05); }
function isLarge(cs){ const s=parseFloat(cs.fontSize); const w=parseInt(cs.fontWeight,10)||400; return s>=24||(s>=18.66&&w>=700); }
function measure(el){ const cs=getComputedStyle(el); const fg=parseColor(cs.color); const bg=effectiveBg(el);
  const fgEff=fg.a<1?over(fg,bg):fg; const r=ratio(fgEff,bg);
  return { ratio:+r.toFixed(2), need:isLarge(cs)?3:4.5, color:cs.color,
    bg:'rgb('+Math.round(bg.r)+','+Math.round(bg.g)+','+Math.round(bg.b)+')',
    size:parseFloat(cs.fontSize), weight:parseInt(cs.fontWeight,10)||400,
    cls:(el.className||'').toString().slice(0,70), text:(el.textContent||'').trim().slice(0,30) }; }
`;

const OUT = [];
function check(name, ok, detail) {
  OUT.push({ name, ok: !!ok, detail: detail || "" });
  console.log(`${ok ? "PASS" : "FAIL"}  ${name}${detail ? "  -- " + detail : ""}`);
}

(async () => {
  const CHROME = "C:/Users/ASUS/AppData/Local/ms-playwright/chromium-1237/chrome-win64/chrome.exe";
  const browser = await pw.chromium.launch(
    fs.existsSync(CHROME) ? { headless: true, executablePath: CHROME } : { headless: true }
  );
  try {
    const session = await loginFull("user@gmail.com", "123456");
    const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
    const page = await ctx.newPage();
    await seedAuth(page, session);
    await page.goto(APP + "/lessons/447", { waitUntil: "networkidle" }).catch(() => {});
    await page.waitForTimeout(1500);

    const struct = await page.evaluate(() => {
      const panel = document.querySelector("#tabpanel-content");
      if (!panel) return { found: false };
      const ids = Array.from(panel.querySelectorAll("[id]")).map((e) => e.id);
      return {
        found: true,
        h1: document.querySelectorAll("h1").length,
        imgsMissingAlt: Array.from(panel.querySelectorAll("img")).filter((i) => !i.hasAttribute("alt")).length,
        idsUnique: ids.length === new Set(ids).size,
      };
    });
    check("content panel found on 447", struct.found);
    check("exactly one h1 on the page", struct.h1 === 1, `n=${struct.h1}`);
    check("no <img> missing alt in content", struct.imgsMissingAlt === 0, `missing=${struct.imgsMissingAlt}`);
    check("no duplicate ids in content panel", struct.idsUnique);

    const contrastExpr = `(() => {
      ${COMPOSITE_FN}
      const panel = document.querySelector("#tabpanel-content");
      if (!panel) return [];
      return Array.from(panel.querySelectorAll("p,h1,h2,h3,h4,th,td,figcaption,li,span,a")).map(measure);
    })()`;
    const measured = await page.evaluate(contrastExpr);
    const fails = measured.filter((m) => m.ratio < m.need);
    check(
      `contrast AA for all ${measured.length} text nodes in the content panel`,
      fails.length === 0,
      fails.length ? JSON.stringify(fails.slice(0, 5)) : `min ratio=${Math.min(...measured.map((m) => m.ratio))}`
    );

    await ctx.close();
  } finally {
    await browser.close();
  }

  const failed = OUT.filter((r) => !r.ok);
  console.log(`\n=== content a11y: ${OUT.length - failed.length}/${OUT.length} PASS ===`);
  fs.mkdirSync(EVID, { recursive: true });
  fs.writeFileSync(path.join(EVID, "f15-content-a11y.json"),
    JSON.stringify({ ranAt: new Date().toISOString(), results: OUT }, null, 2));
  process.exit(failed.length ? 1 : 0);
})();
