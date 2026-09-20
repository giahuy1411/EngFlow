/**
 * audit-v12-full Phase 3 — UI probe (Playwright driver).
 *
 * Covers, on REAL pages:
 *   - a11y: img missing the alt ATTRIBUTE (alt="" is valid), tap targets at both
 *     thresholds (24px = WCAG 2.5.8 AA floor; 24-44 is only AAA), heading order,
 *     skip-link presence, lang attribute
 *   - contrast AA with BOTTOM-UP ALPHA COMPOSITING (the v11 round-1 probe resolved the
 *     first opaque ancestor and therefore missed F138; that is why this composites)
 *   - responsive overflow as raw scrollWidth - clientWidth at 5 widths
 *   - console errors / unhandled rejections / UI-caused API >= 400
 *
 * Rate-limit buckets are flushed before each batch, or the harness manufactures 429s.
 *
 * Run: node sweep/v12/ui-probe.js
 * Out: .specify/specs/audit-v12-full/evidence/ui-probe.json
 */
const path = require("path");
const fs = require("fs");
const H = require(path.join(__dirname, "..", "v8", "ui", "lib.js"));

const ROUTES = [
  { p: "/", role: "anon" }, { p: "/lessons", role: "anon" }, { p: "/videos", role: "anon" },
  { p: "/leaderboard", role: "anon" }, { p: "/decks", role: "anon" }, { p: "/search", role: "anon" },
  { p: "/premium", role: "anon" }, { p: "/login", role: "anon" }, { p: "/register", role: "anon" },
  { p: "/profile", role: "student" }, { p: "/speaking", role: "student" }, { p: "/speaking/history", role: "student" },
  { p: "/decks/create", role: "student" }, { p: "/ai-vocab-generator", role: "student" },
  { p: "/admin/dashboard", role: "admin" }, { p: "/admin/lessons", role: "admin" },
  { p: "/admin/exercises", role: "admin" }, { p: "/admin/users", role: "admin" },
  { p: "/admin/videos", role: "admin" }, { p: "/admin/speaking-prompts", role: "admin" },
];
const WIDTHS = [360, 768, 1280, 1440, 1920];

// Bottom-up alpha compositing over white. Returns [r,g,b] of what the eye actually sees.
const COMPOSITE_FN = `
function parseColor(s){
  const m = /rgba?\\(([^)]+)\\)/.exec(s||''); if(!m) return null;
  const p = m[1].split(',').map(x=>parseFloat(x.trim()));
  return { r:p[0], g:p[1], b:p[2], a: p.length>3 ? p[3] : 1 };
}
function over(fg, bg){ // fg over bg
  const a = fg.a + bg.a*(1-fg.a);
  if(a===0) return {r:255,g:255,b:255,a:0};
  return { r:(fg.r*fg.a + bg.r*bg.a*(1-fg.a))/a, g:(fg.g*fg.a + bg.g*bg.a*(1-fg.a))/a, b:(fg.b*fg.a + bg.b*bg.a*(1-fg.a))/a, a };
}
function effectiveBg(el){
  // walk ancestors collecting background layers, composite bottom-up over white
  const layers = [];
  let n = el;
  while(n && n !== document.documentElement.parentNode){
    const c = parseColor(getComputedStyle(n).backgroundColor);
    if(c && c.a > 0) layers.push(c);
    if(c && c.a === 1) break;
    n = n.parentElement;
  }
  let base = { r:255, g:255, b:255, a:1 };
  for(let i=layers.length-1;i>=0;i--) base = over(layers[i], base);
  return base;
}
function lum(c){ const f=v=>{v/=255; return v<=0.03928? v/12.92 : Math.pow((v+0.055)/1.055,2.4);}; return 0.2126*f(c.r)+0.7152*f(c.g)+0.0722*f(c.b); }
function ratio(a,b){ const L1=lum(a), L2=lum(b); return (Math.max(L1,L2)+0.05)/(Math.min(L1,L2)+0.05); }
`;

const A11Y_FN = `() => {
  const missingAlt = [...document.querySelectorAll('img')].filter(i => !i.hasAttribute('alt')).length;
  const imgs = document.querySelectorAll('img').length;
  const small = [], borderline = [];
  const interactive = document.querySelectorAll('a[href], button, [role="button"], input, select, textarea');
  interactive.forEach(el => {
    const r = el.getBoundingClientRect();
    if (r.width === 0 || r.height === 0) return;
    if (Math.min(r.width, r.height) < 24) small.push({ t: el.tagName, label: (el.getAttribute('aria-label')||el.innerText||'').trim().slice(0,30), w: Math.round(r.width), h: Math.round(r.height) });
    else if (Math.min(r.width, r.height) < 44) borderline.push({ t: el.tagName, label: (el.getAttribute('aria-label')||el.innerText||'').trim().slice(0,30), w: Math.round(r.width), h: Math.round(r.height) });
  });
  const noName = [...document.querySelectorAll('button, a[href]')].filter(el => {
    const r = el.getBoundingClientRect(); if (r.width===0||r.height===0) return false;
    const name = (el.getAttribute('aria-label')||'').trim() || (el.innerText||'').trim() || (el.querySelector('img')?.getAttribute('alt')||'').trim() || (el.getAttribute('title')||'').trim();
    return !name;
  }).length;
  const h1 = document.querySelectorAll('h1').length;
  const levels = [...document.querySelectorAll('h1,h2,h3,h4,h5,h6')].map(h => +h.tagName[1]);
  let headingSkip = null;
  for (let i=1;i<levels.length;i++) if (levels[i] - levels[i-1] > 1) { headingSkip = levels[i-1]+'->'+levels[i]; break; }
  return {
    imgs, missingAlt, noName, h1Count: h1, headingSkip,
    lang: document.documentElement.lang || null,
    skipLink: !!document.querySelector('a.skip-link, a[href="#main-content"]'),
    smallTargets: small.slice(0,8), smallCount: small.length,
    borderlineCount: borderline.length,
    focusVisibleRule: [...document.styleSheets].some(ss => { try { return [...ss.cssRules].some(r => (r.selectorText||'').includes('focus-visible')); } catch { return false; } }),
  };
}`;

const CONTRAST_FN = `() => {
  ${COMPOSITE_FN}
  const fails = [];
  const nodes = [...document.querySelectorAll('*')].filter(el => {
    if (el.children.length) return false;
    const t = (el.textContent||'').trim(); if (!t) return false;
    const r = el.getBoundingClientRect(); if (r.width===0||r.height===0) return false;
    const cs = getComputedStyle(el);
    if (cs.visibility==='hidden' || cs.display==='none' || parseFloat(cs.opacity) < 0.1) return false;
    if (el.getAttribute('aria-hidden')==='true') return false;
    return true;
  });
  for (const el of nodes) {
    const cs = getComputedStyle(el);
    const fg = parseColor(cs.color); if(!fg) continue;
    const bg = effectiveBg(el);
    const fgEff = fg.a < 1 ? over(fg, bg) : fg;
    const ratioV = ratio(fgEff, bg);
    const size = parseFloat(cs.fontSize);
    const weight = parseInt(cs.fontWeight, 10) || 400;
    const isLarge = size >= 24 || (size >= 18.66 && weight >= 700);
    const need = isLarge ? 3 : 4.5;
    if (ratioV < need) {
      fails.push({ text: el.textContent.trim().slice(0,28), cls: (el.className||'').toString().slice(0,50),
        color: cs.color, bg: 'rgb('+Math.round(bg.r)+','+Math.round(bg.g)+','+Math.round(bg.b)+')',
        ratio: +ratioV.toFixed(2), need, size, weight });
    }
  }
  return { failures: fails.slice(0, 25), count: fails.length };
}`;

(async () => {
  const out = { driver: "playwright (chromium)", routes: [], responsive: [], totals: {} };
  let totalContrast = 0, totalMissingAlt = 0, totalSmall = 0, consoleErrors = 0, apiErrors = 0, pageErrors = 0;
  // playwright-core 1.63 wants chromium build 1243, but this machine has 1237 installed.
  // Point at the installed binary explicitly (the AGENTS.md executablePath pattern)
  // rather than failing the whole sweep on a version bump.
  const os = require("os");
  const CANDIDATES = [
    path.join(os.homedir(), "AppData", "Local", "ms-playwright", "chromium-1237", "chrome-win64", "chrome.exe"),
    path.join(os.homedir(), "AppData", "Local", "ms-playwright", "chromium-1234", "chrome-win64", "chrome.exe"),
    "C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe",
  ];
  const exe = CANDIDATES.find((p) => fs.existsSync(p));
  console.log("browser executable:", exe || "(bundled default)");
  const browser = await H.pw.chromium.launch(exe ? { headless: true, executablePath: exe } : { headless: true });

  // sessions (loginFull takes email + password strings)
  const stu = await H.loginFull("user@gmail.com", "123456");
  const adm = await H.loginFull("admin@gmail.com", "123456");

  for (const rt of ROUTES) {
    H.flushLimits && H.flushLimits(true);
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await ctx.newPage();
    const errors = [], pageErrs = [], api4xx = [];
    page.on("console", m => { if (m.type() === "error") errors.push(m.text().slice(0, 160)); });
    page.on("pageerror", e => pageErrs.push(String(e).slice(0, 160)));
    page.on("response", r => { if (r.url().includes(":8080") && r.status() >= 400 && r.status() !== 401 && r.status() !== 403 && r.status() !== 404) api4xx.push(r.status() + " " + r.url().replace(/^.*:8080/, "")); });

    const sess = rt.role === "admin" ? adm : rt.role === "student" ? stu : null;
    if (sess) await H.seedAuth(page, sess);
    try {
      await page.goto(H.APP + rt.p, { waitUntil: "domcontentloaded", timeout: 30000 });
      await page.waitForTimeout(2200);
    } catch (e) { /* recorded via textLen */ }

    const url = page.url();
    // NOTE: page.evaluate() treats a bare "() => {...}" STRING as an expression and returns
    // undefined in this playwright version, so the string is wrapped and invoked explicitly.
    const a11y = await page.evaluate(`(${A11Y_FN})()`);
    const contrast = await page.evaluate(`(${CONTRAST_FN})()`);
    const textLen = await page.evaluate(() => (document.body.innerText || "").trim().length);
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);

    out.routes.push({ route: rt.p, role: rt.role, landed: url.replace(H.APP, ""), textLen,
      missingAlt: a11y.missingAlt, imgs: a11y.imgs, smallTargets: a11y.smallCount, smallList: a11y.smallTargets,
      noName: a11y.noName, h1: a11y.h1Count, headingSkip: a11y.headingSkip, lang: a11y.lang, skipLink: a11y.skipLink,
      contrastFails: contrast.count, contrastList: contrast.failures,
      consoleErrors: errors, pageErrors: pageErrs, api4xx, overflow });

    totalContrast += contrast.count; totalMissingAlt += a11y.missingAlt; totalSmall += a11y.smallCount;
    consoleErrors += errors.length; apiErrors += api4xx.length; pageErrors += pageErrs.length;
    console.log(`${rt.role.padEnd(7)} ${rt.p.padEnd(26)} landed=${url.replace(H.APP, "").padEnd(24)} text=${String(textLen).padStart(5)} contrast=${String(contrast.count).padStart(3)} small=${a11y.smallCount} altMiss=${a11y.missingAlt} err=${errors.length} api4xx=${api4xx.length} ovf=${overflow}`);
    await ctx.close();
  }

  // Responsive at 5 widths on 3 representative routes
  for (const p of ["/", "/lessons", "/premium"]) {
    for (const w of WIDTHS) {
      const ctx = await browser.newContext({ viewport: { width: w, height: 900 } });
      const page = await ctx.newPage();
      try { await page.goto(H.APP + p, { waitUntil: "domcontentloaded", timeout: 30000 }); await page.waitForTimeout(1500); } catch {}
      const ovf = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
      out.responsive.push({ route: p, width: w, overflow: ovf });
      console.log(`responsive ${p} @${w} -> overflow ${ovf}`);
      await ctx.close();
    }
  }

  await browser.close();
  out.totals = { contrastFails: totalContrast, missingAlt: totalMissingAlt, smallTargets: totalSmall, consoleErrors, apiErrors, pageErrors };
  console.log("\n=== TOTALS ===", JSON.stringify(out.totals));
  const OUT = path.join(__dirname, "..", "..", ".specify", "specs", "audit-v12-full", "evidence", "ui-probe.json");
  fs.writeFileSync(OUT, JSON.stringify(out, null, 2));
  console.log("written:", OUT);
  process.exit(totalContrast > 0 || totalMissingAlt > 0 || apiErrors > 0 ? 1 : 0);
})();
