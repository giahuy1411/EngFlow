/**
 * audit-v13-full Phase 3 — UI sweep (Playwright driver).
 *
 * Covers, on REAL pages against the LIVE stack:
 *   1. Route x role matrix (anonymous / student / admin) over the full router
 *      inventory, asserting page.url() after each navigation. A guard redirect
 *      is CORRECT behaviour, not a bug — we assert the EXPECTED landing.
 *   2. Console + network health per route (uncaught errors, unexpected 4xx/5xx).
 *   3. Responsive overflow at 360/768/1280/1440/1920, raw scrollWidth-clientWidth.
 *   4. Accessibility: missing alt ATTRIBUTE (alt="" is valid), tap targets at the
 *      24px AA floor, heading order, skip link, lang, focus-visible.
 *   5. Design-system conformance + LIVE contrast with BOTTOM-UP ALPHA COMPOSITING.
 *
 * Rate-limit buckets are flushed before each batch (100/min/IP global bucket), or
 * the harness manufactures 429s that look like app defects.
 *
 * Run: node sweep/v13/ui-sweep.js
 */
const path = require("path");
const fs = require("fs");
const H = require(path.join(__dirname, "..", "v8", "ui", "lib.js"));

const APP = H.APP;
const OUTDIR = path.join(__dirname, "..", "..", ".specify", "specs", "audit-v13-full", "evidence");
const SHOTDIR = path.join(OUTDIR, "shots");
fs.mkdirSync(SHOTDIR, { recursive: true });

// Real IDs read from the live DB (tmp/v13/ids.sql):
const L = 445;    // lessons.lesson_id
const D = 10006;  // decks.deck_id
const P = 3;      // speaking_prompts.id
const V = 1;      // video_lessons.id
const S = 2;      // speaking_submissions.id

// ── ROUTE INVENTORY ───────────────────────────────────────────────────────────
// expect: exact pathname the guard SHOULD land on for that role.
//   anon   -> no token
//   student-> user@gmail.com  (isAdmin:false, isPremium:true)
//   admin  -> admin@gmail.com (isAdmin:true)
const ROUTES = [
  { p: "/",                       expect: { anon: "/",             student: "/",             admin: "/" } },
  { p: "/login",                  expect: { anon: "/login",        student: "/lessons",      admin: "/lessons" } },
  { p: "/register",               expect: { anon: "/register",     student: "/lessons",      admin: "/lessons" } },
  { p: "/forgot-password",        expect: { anon: "/forgot-password", student: "/lessons",   admin: "/lessons" } },
  { p: "/reset-password",         expect: { anon: "/reset-password",  student: "/lessons",   admin: "/lessons" } },
  { p: "/lessons",                expect: { anon: "/lessons",      student: "/lessons",      admin: "/lessons" } },
  { p: `/lessons/${L}`,           expect: { anon: `/lessons/${L}`, student: `/lessons/${L}`, admin: `/lessons/${L}` } },
  { p: "/videos",                 expect: { anon: "/login",        student: "/videos",       admin: "/videos" } },
  { p: `/videos/${V}`,            expect: { anon: "/login",        student: `/videos/${V}`,  admin: `/videos/${V}` } },
  { p: "/profile",                expect: { anon: "/login",        student: "/profile",      admin: "/profile" } },
  { p: "/search",                 expect: { anon: "/search",       student: "/search",       admin: "/search" } },
  { p: "/leaderboard",            expect: { anon: "/leaderboard",  student: "/leaderboard",  admin: "/leaderboard" } },
  { p: "/decks",                  expect: { anon: "/decks",        student: "/decks",        admin: "/decks" } },
  { p: `/decks/${D}`,             expect: { anon: "/login",        student: `/decks/${D}`,   admin: `/decks/${D}` } },
  { p: `/decks/${D}/play/flashcard`, expect: { anon: "/login",     student: `/decks/${D}/play/flashcard`, admin: `/decks/${D}/play/flashcard` } },
  { p: `/decks/${D}/play/quiz`,   expect: { anon: "/login",        student: `/decks/${D}/play/quiz`,   admin: `/decks/${D}/play/quiz` } },
  { p: `/decks/${D}/play/memory`, expect: { anon: "/login",        student: `/decks/${D}/play/memory`, admin: `/decks/${D}/play/memory` } },
  { p: `/decks/${D}/play/typing`, expect: { anon: "/login",        student: `/decks/${D}/play/typing`, admin: `/decks/${D}/play/typing` } },
  { p: `/decks/${D}/play/listening`, expect: { anon: "/login",     student: `/decks/${D}/play/listening`, admin: `/decks/${D}/play/listening` } },
  { p: `/decks/${D}/play/mixed`,  expect: { anon: "/login",        student: `/decks/${D}/play/mixed`,  admin: `/decks/${D}/play/mixed` } },
  { p: "/ai-vocab-generator",     expect: { anon: "/login",        student: "/ai-vocab-generator", admin: "/ai-vocab-generator" } },
  { p: "/decks/create",           expect: { anon: "/login",        student: "/decks/create", admin: "/decks/create" } },
  { p: "/speaking",               expect: { anon: "/login",        student: "/speaking",     admin: "/speaking" } },
  { p: "/speaking/history",       expect: { anon: "/login",        student: "/speaking/history", admin: "/speaking/history" } },
  { p: `/speaking/${P}`,          expect: { anon: "/login",        student: `/speaking/${P}`, admin: `/speaking/${P}` } },
  { p: `/speaking/${P}/record`,   expect: { anon: "/login",        student: `/speaking/${P}/record`, admin: `/speaking/${P}/record` } },
  { p: "/premium",                expect: { anon: "/premium",      student: "/premium",      admin: "/premium" } },
  { p: "/premium/checkout",       expect: { anon: "/login",        student: "/premium/checkout", admin: "/premium/checkout" } },
  { p: "/admin",                  expect: { anon: "/login",        student: "/",             admin: "/admin/dashboard" } },
  { p: "/admin/dashboard",        expect: { anon: "/login",        student: "/",             admin: "/admin/dashboard" } },
  { p: "/admin/lessons",          expect: { anon: "/login",        student: "/",             admin: "/admin/lessons" } },
  { p: "/admin/exercises",        expect: { anon: "/login",        student: "/",             admin: "/admin/exercises" } },
  { p: "/admin/users",            expect: { anon: "/login",        student: "/",             admin: "/admin/users" } },
  { p: "/admin/speaking-prompts", expect: { anon: "/login",        student: "/",             admin: "/admin/speaking-prompts" } },
  { p: "/admin/speaking-submissions", expect: { anon: "/login",    student: "/",             admin: "/admin/speaking-submissions" } },
  { p: "/admin/videos",           expect: { anon: "/login",        student: "/",             admin: "/admin/videos" } },
  { p: "/admin/video-attempts",   expect: { anon: "/login",        student: "/",             admin: "/admin/video-attempts" } },
  { p: `/admin/${L}/build`,       expect: { anon: "/login",        student: "/",             admin: `/admin/${L}/build` } },
  { p: "/definitely-not-a-route", expect: { anon: "/",             student: "/",             admin: "/" } },
];

const WIDTHS = [360, 768, 1280, 1440, 1920];

// Bottom-up alpha compositing over white. Returns [r,g,b] of what the eye sees.
const COMPOSITE_FN = `
function parseColor(s){
  const m = /rgba?\\(([^)]+)\\)/.exec(s||''); if(!m) return null;
  const p = m[1].split(',').map(x=>parseFloat(x.trim()));
  return { r:p[0], g:p[1], b:p[2], a: p.length>3 ? p[3] : 1 };
}
function over(fg, bg){
  const a = fg.a + bg.a*(1-fg.a);
  if(a===0) return {r:255,g:255,b:255,a:0};
  return { r:(fg.r*fg.a + bg.r*bg.a*(1-fg.a))/a, g:(fg.g*fg.a + bg.g*bg.a*(1-fg.a))/a, b:(fg.b*fg.a + bg.b*bg.a*(1-fg.a))/a, a };
}
function effectiveBg(el){
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
function isLarge(cs){ const size=parseFloat(cs.fontSize); const weight=parseInt(cs.fontWeight,10)||400; return size>=24 || (size>=18.66 && weight>=700); }
`;

const A11Y_FN = `() => {
  const missingAlt = [...document.querySelectorAll('img')].filter(i => !i.hasAttribute('alt')).length;
  const imgs = document.querySelectorAll('img').length;
  const small = [], borderline = [];
  document.querySelectorAll('a[href], button, [role="button"], input, select, textarea').forEach(el => {
    const r = el.getBoundingClientRect();
    if (r.width === 0 || r.height === 0) return;
    const label = (el.getAttribute('aria-label')||el.innerText||'').trim().slice(0,30);
    const m = Math.min(r.width, r.height);
    if (m < 24) small.push({ t: el.tagName, label, w: Math.round(r.width), h: Math.round(r.height) });
    else if (m < 44) borderline.push({ t: el.tagName, label, w: Math.round(r.width), h: Math.round(r.height) });
  });
  const noName = [...document.querySelectorAll('button, a[href]')].filter(el => {
    const r = el.getBoundingClientRect(); if (r.width===0||r.height===0) return false;
    const name = (el.getAttribute('aria-label')||'').trim() || (el.innerText||'').trim()
      || (el.querySelector('img')?.getAttribute('alt')||'').trim() || (el.getAttribute('title')||'').trim();
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
    smallTargets: small.slice(0,10), smallCount: small.length, borderlineCount: borderline.length,
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
    const need = isLarge(cs) ? 3 : 4.5;
    if (ratioV < need) {
      fails.push({ text: el.textContent.trim().slice(0,32), cls: (el.className||'').toString().slice(0,60),
        tag: el.tagName, color: cs.color,
        bg: 'rgb('+Math.round(bg.r)+','+Math.round(bg.g)+','+Math.round(bg.b)+')',
        ratio: +ratioV.toFixed(2), need, size: parseFloat(cs.fontSize),
        weight: parseInt(cs.fontWeight,10)||400 });
    }
  }
  return { failures: fails.slice(0, 40), count: fails.length };
}`;

// Live font check (document.fonts.check) + token presence
const FONT_FN = `() => {
  const cs = getComputedStyle(document.body);
  return {
    bodyFamily: cs.fontFamily,
    checkBeVietnam: document.fonts.check('16px "Be Vietnam Pro"'),
    loadedFamilies: [...document.fonts].map(f => f.family + ' ' + f.status).slice(0, 20),
    fontCount: document.fonts.size
  };
}`;

(async () => {
  const out = { driver: "playwright (chromium)", app: APP, startedAt: new Date().toISOString(),
    routes: [], responsive: [], totals: {}, shots: [] };
  let totalContrast = 0, totalMissingAlt = 0, totalSmall = 0, totalNoName = 0,
      consoleErrors = 0, apiErrors = 0, pageErrors = 0, guardFails = 0, overflowRoutes = 0;
  const allContrast = [];
  const allA11y = {};

  const os = require("os");
  const CANDIDATES = [
    path.join(os.homedir(), "AppData", "Local", "ms-playwright", "chromium-1237", "chrome-win64", "chrome.exe"),
    path.join(os.homedir(), "AppData", "Local", "ms-playwright", "chromium-1234", "chrome-win64", "chrome.exe"),
    "C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe",
  ];
  const exe = CANDIDATES.find((p) => fs.existsSync(p));
  console.log("browser executable:", exe || "(bundled default)");
  const browser = await H.pw.chromium.launch(exe ? { headless: true, executablePath: exe } : { headless: true });

  const stu = await H.loginFull("user@gmail.com", "123456");
  const adm = await H.loginFull("admin@gmail.com", "123456");
  console.log("seeded student isAdmin=" + stu.user.isAdmin + " isPremium=" + stu.user.isPremium);
  console.log("seeded admin   isAdmin=" + adm.user.isAdmin);

  const roles = ["anon", "student", "admin"];
  for (const rt of ROUTES) {
    for (const role of roles) {
      H.flushLimits && H.flushLimits(true);
      const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
      const page = await ctx.newPage();
      const errors = [], pageErrs = [], api4xx = [];
      page.on("console", m => { if (m.type() === "error") errors.push(m.text().slice(0, 200)); });
      page.on("pageerror", e => pageErrs.push(String(e).slice(0, 200)));
      page.on("response", r => {
        if (!r.url().includes(":8080")) return;
        const st = r.status();
        // 401/403/404 are EXPECTED on probe routes (missing entities, premium gates).
        if (st >= 400 && st !== 401 && st !== 403 && st !== 404) api4xx.push(st + " " + r.url().replace(/^.*:8080/, ""));
      });

      const sess = role === "admin" ? adm : role === "student" ? stu : null;
      if (sess) await H.seedAuth(page, sess);
      try {
        await page.goto(APP + rt.p, { waitUntil: "domcontentloaded", timeout: 30000 });
        await page.waitForTimeout(2300);
      } catch (e) { /* recorded via textLen */ }

      const landed = page.url().replace(APP, "") || "/";
      const want = rt.expect[role];
      const guardOk = landed === want;
      if (!guardOk) guardFails++;

      const a11y = await page.evaluate(`(${A11Y_FN})()`);
      const contrast = await page.evaluate(`(${CONTRAST_FN})()`);
      const textLen = await page.evaluate(() => (document.body.innerText || "").trim().length);
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
      if (overflow > 0) overflowRoutes++;

      const rec = { route: rt.p, role, landed, expected: want, guardOk, textLen,
        missingAlt: a11y.missingAlt, imgs: a11y.imgs, smallTargets: a11y.smallCount, smallList: a11y.smallTargets,
        noName: a11y.noName, h1: a11y.h1Count, headingSkip: a11y.headingSkip, lang: a11y.lang, skipLink: a11y.skipLink,
        contrastFails: contrast.count, contrastList: contrast.failures,
        consoleErrors: errors, pageErrors: pageErrs, api4xx, overflow };
      out.routes.push(rec);
      if (contrast.count) allContrast.push({ route: rt.p, role, failures: contrast.failures });
      if (!allA11y[rt.p]) allA11y[rt.p] = { missingAlt: a11y.missingAlt, noName: a11y.noName, smallCount: a11y.smallCount, smallList: a11y.smallTargets, headingSkip: a11y.headingSkip, h1: a11y.h1Count, lang: a11y.lang, skipLink: a11y.skipLink };

      totalContrast += contrast.count; totalMissingAlt += a11y.missingAlt; totalSmall += a11y.smallCount;
      totalNoName += a11y.noName;
      consoleErrors += errors.length; apiErrors += api4xx.length; pageErrors += pageErrs.length;
      const flag = guardOk ? " " : "!";
      console.log(`${flag}${role.padEnd(7)} ${rt.p.padEnd(30)} -> ${landed.padEnd(28)} txt=${String(textLen).padStart(5)} con=${String(contrast.count).padStart(2)} small=${a11y.smallCount} alt=${a11y.missingAlt} err=${errors.length} api=${api4xx.length} ovf=${overflow}`);
      await ctx.close();
    }
  }

  // ── Responsive sweep at 5 widths on representative routes × roles ────────────
  const RESP_ROUTES = [
    { p: "/", role: "anon" }, { p: "/lessons", role: "anon" }, { p: "/premium", role: "anon" },
    { p: "/decks", role: "student" }, { p: "/profile", role: "student" },
    { p: "/admin/dashboard", role: "admin" }, { p: "/admin/users", role: "admin" },
  ];
  for (const rt of RESP_ROUTES) {
    for (const w of WIDTHS) {
      const ctx = await browser.newContext({ viewport: { width: w, height: 900 } });
      const page = await ctx.newPage();
      const sess = rt.role === "admin" ? adm : rt.role === "student" ? stu : null;
      if (sess) await H.seedAuth(page, sess);
      try { await page.goto(APP + rt.p, { waitUntil: "domcontentloaded", timeout: 30000 }); await page.waitForTimeout(1600); } catch {}
      const ovf = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
      const widest = await page.evaluate(() => {
        const vw = document.documentElement.clientWidth; let worst = null;
        document.querySelectorAll('*').forEach(el => {
          const r = el.getBoundingClientRect();
          if (r.width === 0) return;
          const right = r.right;
          if (right > vw + 1) { const over = Math.round(right - vw); if (!worst || over > worst.over) worst = { over, tag: el.tagName, cls: (el.className||'').toString().slice(0,50) }; }
        });
        return worst;
      });
      out.responsive.push({ route: rt.p, role: rt.role, width: w, overflow: ovf, widest });
      if (ovf > 0) console.log(`  OVERFLOW ${rt.role} ${rt.p} @${w} = ${ovf}px widest=${JSON.stringify(widest)}`);
      await ctx.close();
    }
  }
  console.log(`responsive: ${out.responsive.length} cells, ${out.responsive.filter(r=>r.overflow>0).length} overflowing`);

  // ── Design-system + font live check on 3 key routes ─────────────────────────
  out.design = { routes: [] };
  for (const { p, role } of [{ p: "/", role: "anon" }, { p: "/lessons", role: "anon" }, { p: "/admin/dashboard", role: "admin" }]) {
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await ctx.newPage();
    const sess = role === "admin" ? adm : null;
    if (sess) await H.seedAuth(page, sess);
    try { await page.goto(APP + p, { waitUntil: "domcontentloaded", timeout: 30000 }); await page.waitForTimeout(2000); } catch {}
    const font = await page.evaluate(`(${FONT_FN})()`);
    const ds = await page.evaluate(() => {
      const cs = getComputedStyle(document.documentElement);
      const tok = n => cs.getPropertyValue(n).trim();
      return {
        geoBg: tok('--geo-bg'), geoFg: tok('--geo-fg'), geoMuted: tok('--geo-muted'),
        geoMutedFg: tok('--geo-muted-fg'), geoAccent: tok('--geo-accent'),
        geoAccentStrong: tok('--geo-accent-strong'), geoAccentInk: tok('--geo-accent-ink'),
        geoSecondaryStrong: tok('--geo-secondary-strong'), geoTertiaryInk: tok('--geo-tertiary-ink'),
        geoQuaternaryInk: tok('--geo-quaternary-ink'), geoRadius: tok('--geo-radius-md'),
        geoBorderWidth: tok('--geo-border-width'),
        bodyBg: getComputedStyle(document.body).backgroundColor,
        bodyFont: getComputedStyle(document.body).fontFamily,
        hasLucide: !!document.querySelector('.lucide, svg[class*="lucide"]'),
        lucideStroke: (() => { const s = document.querySelector('.lucide'); return s ? getComputedStyle(s).strokeWidth : null; })(),
        skipLink: !!document.querySelector('a.skip-link, a[href="#main-content"]'),
        scrollGutter: getComputedStyle(document.documentElement).scrollbarGutter || null,
      };
    });
    out.design.routes.push({ route: p, role, font, tokens: ds });
    console.log(`design ${p}: font=${ds.bodyFont} checkBV=${font.checkBeVietnam} lucide=${ds.hasLucide} stroke=${ds.lucideStroke}`);
    await ctx.close();
  }

  // ── Screenshots (evidence) ──────────────────────────────────────────────────
  const SHOTS = [
    { p: "/", role: "anon", w: 1440, h: 900, name: "v13-home-1440" },
    { p: "/lessons", role: "anon", w: 360, h: 800, name: "v13-lessons-360" },
    { p: "/admin/dashboard", role: "admin", w: 1440, h: 900, name: "v13-admin-dashboard-1440" },
    { p: "/profile", role: "student", w: 1280, h: 900, name: "v13-profile-1280" },
    { p: "/premium", role: "anon", w: 768, h: 900, name: "v13-premium-768" },
    { p: "/lessons", role: "anon", w: 1920, h: 1000, name: "v13-lessons-1920" },
  ];
  for (const s of SHOTS) {
    const ctx = await browser.newContext({ viewport: { width: s.w, height: s.h } });
    const page = await ctx.newPage();
    const sess = s.role === "admin" ? adm : s.role === "student" ? stu : null;
    if (sess) await H.seedAuth(page, sess);
    try {
      await page.goto(APP + s.p, { waitUntil: "domcontentloaded", timeout: 30000 });
      await page.waitForTimeout(2500);
      const f = path.join(SHOTDIR, s.name + ".png");
      await page.screenshot({ path: f });
      out.shots.push(path.relative(path.join(__dirname, "..", ".."), f).replace(/\\/g, "/"));
      console.log("shot:", f);
    } catch (e) { console.log("shot FAILED", s.name, e.message.slice(0,80)); }
    await ctx.close();
  }

  await browser.close();
  out.totals = { contrastFails: totalContrast, missingAlt: totalMissingAlt, smallTargets: totalSmall,
    noName: totalNoName, consoleErrors, apiErrors, pageErrors, guardFails, overflowRoutes };
  out.contrastByRoute = allContrast;
  out.a11yByRoute = allA11y;
  console.log("\n=== TOTALS ===", JSON.stringify(out.totals));
  fs.writeFileSync(path.join(OUTDIR, "ui-sweep.json"), JSON.stringify(out, null, 2));
  console.log("written:", path.join(OUTDIR, "ui-sweep.json"));
  process.exit(0);
})();
