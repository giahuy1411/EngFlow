/**
 * audit-v15 harness — focused verification probe.
 *
 * Deterministically reproduces / pins down the findings the broad sweep surfaced:
 *   1. Leaderboard error-state contrast: force the API failure with route
 *      interception (so the state is reproducible, not dependent on a flaky API).
 *   2. AdminLessonBuilder contrast: measure each suspect element live.
 *   3. Video transcript tap targets: measure each button.
 *   4. The named "suspect" sites (AdminDashboard text-tertiary, Profile
 *      text-tertiary, AdminLayout dark sidebar text-tertiary) — measure to
 *      CONFIRM or REFUTE the false-positive warning.
 *   5. Are the ERR_EMPTY_RESPONSE console errors transient?
 */
const path = require("path");
const fs = require("fs");
const H = require(path.join(__dirname, "..", "v8", "ui", "lib.js"));

const COMPOSITE_FN = `
function parseColor(s){ const m=/rgba?\\(([^)]+)\\)/.exec(s||''); if(!m) return null;
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

const measureByText = `(txt, sel) => {
  ${COMPOSITE_FN}
  const els=[...document.querySelectorAll(sel||'*')].filter(e=>e.children.length===0 && (e.textContent||'').trim()===txt);
  return els.map(measure);
}`;

const measureBySelector = `(sel) => {
  ${COMPOSITE_FN}
  const out=[]; document.querySelectorAll(sel).forEach(e=>{ const r=e.getBoundingClientRect(); if(r.width===0||r.height===0) return; out.push(measure(e)); });
  return out;
}`;

const targets = `(sel) => {
  const out=[]; document.querySelectorAll(sel).forEach(e=>{ const r=e.getBoundingClientRect();
    if(r.width===0||r.height===0) return;
    out.push({ tag:e.tagName, label:(e.getAttribute('aria-label')||e.innerText||'').trim().slice(0,24),
      w:Math.round(r.width), h:Math.round(r.height) }); });
  return out;
}`;

(async () => {
  const exe = path.join(require("os").homedir(), "AppData", "Local", "ms-playwright", "chromium-1237", "chrome-win64", "chrome.exe");
  const browser = await H.pw.chromium.launch({ headless: true, executablePath: exe });
  const stu = await H.loginFull("user@gmail.com", "123456");
  const adm = await H.loginFull("admin@gmail.com", "123456");
  const out = {};

  // ── 1. Leaderboard error state — FORCED with route interception ─────────────
  {
    H.flushLimits(true);
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await ctx.newPage();
    await page.route("**/api/**/leaderboard**", r => r.abort());
    await page.route("**/api/leaderboard**", r => r.abort());
    await H.seedAuth(page, stu);
    await page.goto(H.APP + "/leaderboard", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(2500);
    const role = await page.evaluate(() => !!document.querySelector('[role="alert"]'));
    const res = await page.evaluate(`(${measureBySelector})('[role="alert"] p, [role="alert"]')`);
    out.leaderboardError = { alertRendered: role, measurements: res };
    console.log("leaderboard forced-error alert=" + role, JSON.stringify(res));
    await ctx.close();
  }

  // ── 2. AdminLessonBuilder contrast — REMOVED in audit-v15 ───────────────────
  // The Lesson Builder "Đường B" was deleted, so /admin/:id/build falls through to the
  // catch-all (home). Measuring there would report home-page contrast under a "builder"
  // label — misleading, so this block is gone. Admin contrast is still covered by the
  // adminDashboard + adminSidebar blocks below; the removal is proven by
  // evidence/f15-route-b-removal-live.json.
  out.builder = { removed: "audit-v15 — Lesson Builder gone" };

  // ── 3. Video transcript tap targets (student) ───────────────────────────────
  {
    H.flushLimits(true);
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await ctx.newPage();
    await H.seedAuth(page, stu);
    await page.goto(H.APP + "/videos/1", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(3500);
    const btns = await page.evaluate(`(${targets})('button')`);
    const small = btns.filter(b => Math.min(b.w, b.h) < 24);
    out.videoTranscript = { totalButtons: btns.length, smallCount: small.length, small: small.slice(0, 20) };
    console.log(`video transcript buttons=${btns.length} small(<24)=${small.length}`, JSON.stringify(small.slice(0,6)));
    await ctx.close();
  }

  // ── 4. The named suspect sites — CONFIRM or REFUTE ──────────────────────────
  {
    // AdminDashboard: text-tertiary / text-quaternary on light card
    H.flushLimits(true);
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await ctx.newPage();
    await H.seedAuth(page, adm);
    await page.goto(H.APP + "/admin/dashboard", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(2500);
    out.adminDashboard = {
      tertiaryIcons: await page.evaluate(`(${measureBySelector})('.text-tertiary')`),
      quaternaryIcons: await page.evaluate(`(${measureBySelector})('.text-quaternary')`),
      tertiaryInk: await page.evaluate(`(${measureBySelector})('.text-tertiary-ink')`),
      quaternaryInk: await page.evaluate(`(${measureBySelector})('.text-quaternary-ink')`),
    };
    console.log("adminDashboard tertiary=" + JSON.stringify(out.adminDashboard.tertiaryIcons)
      + " quaternary=" + JSON.stringify(out.adminDashboard.quaternaryIcons));
    await ctx.close();
  }
  {
    // Profile: text-tertiary (line 44 area)
    H.flushLimits(true);
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await ctx.newPage();
    await H.seedAuth(page, stu);
    await page.goto(H.APP + "/profile", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(2500);
    out.profile = {
      tertiary: await page.evaluate(`(${measureBySelector})('.text-tertiary')`),
      tertiaryInk: await page.evaluate(`(${measureBySelector})('.text-tertiary-ink')`),
      secondaryInk: await page.evaluate(`(${measureBySelector})('.text-secondary-ink')`),
    };
    console.log("profile tertiary=" + JSON.stringify(out.profile.tertiary) + " tertiaryInk=" + JSON.stringify(out.profile.tertiaryInk));
    await ctx.close();
  }
  {
    // AdminLayout dark sidebar: text-tertiary on #1E293B
    H.flushLimits(true);
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await ctx.newPage();
    await H.seedAuth(page, adm);
    await page.goto(H.APP + "/admin/dashboard", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(2000);
    const sidebar = await page.evaluate(`(() => {
      ${COMPOSITE_FN}
      const aside = document.querySelector('aside, [class*="bg-foreground"], nav');
      const out = [];
      if (aside) {
        const cs = getComputedStyle(aside);
        out.push({ el: 'container', bg: cs.backgroundColor });
        aside.querySelectorAll('span,a,div,p').forEach(e => {
          const t=(e.textContent||'').trim(); if(!t || e.children.length) return;
          const r=e.getBoundingClientRect(); if(r.width===0) return;
          const m = measure(e); m.el = e.tagName; out.push(m);
        });
      }
      return out.slice(0, 25);
    })()`);
    out.adminSidebar = sidebar;
    console.log("adminSidebar:", JSON.stringify(sidebar.slice(0, 12)));
    await ctx.close();
  }

  // ── 5. Are the ERR_EMPTY_RESPONSE console errors transient? ─────────────────
  {
    const results = [];
    for (const p of ["/profile", "/search", "/leaderboard", "/videos/1"]) {
      H.flushLimits(true);
      const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
      const page = await ctx.newPage();
      const errs = [];
      page.on("console", m => { if (m.type() === "error") errs.push(m.text().slice(0, 120)); });
      page.on("requestfailed", r => errs.push("REQFAIL " + r.url().slice(0, 80) + " " + (r.failure() && r.failure().errorText)));
      await H.seedAuth(page, stu);
      try { await page.goto(H.APP + p, { waitUntil: "domcontentloaded", timeout: 30000 }); await page.waitForTimeout(3000); } catch (e) {}
      results.push({ route: p, errors: errs });
      console.log("recheck " + p + " -> " + JSON.stringify(errs));
      await ctx.close();
    }
    out.consoleRecheck = results;
  }

  await browser.close();
  fs.writeFileSync(path.join(__dirname, "..", "..", ".specify", "specs", "audit-v15-full", "evidence", "focused-probe.json"), JSON.stringify(out, null, 2));
  console.log("\nwritten focused-probe.json");
})();
