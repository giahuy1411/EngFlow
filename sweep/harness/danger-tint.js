const path = require("path");
const H = require(path.join(__dirname, "..", "v8", "ui", "lib.js"));

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
`;

const MEASURE_SEL = `(sel) => {
  ${COMPOSITE_FN}
  const out=[];
  document.querySelectorAll(sel).forEach(el=>{
    const cs=getComputedStyle(el); const fg=parseColor(cs.color); if(!fg) return;
    const bg=effectiveBg(el);
    const fgEff = fg.a < 1 ? over(fg,bg) : fg;
    out.push({ text:(el.textContent||'').trim().slice(0,34), color:cs.color,
      bg:'rgb('+Math.round(bg.r)+','+Math.round(bg.g)+','+Math.round(bg.b)+')',
      ratio:+ratio(fgEff,bg).toFixed(2), need:4.5 });
  });
  return out;
}`;

(async () => {
  const exe = "C:/Users/ASUS/AppData/Local/ms-playwright/chromium-1237/chrome-win64/chrome.exe";
  const b = await H.pw.chromium.launch({ headless: true, executablePath: exe });
  const adm = await H.loginFull("admin@gmail.com", "123456");
  const cases = [
    { p: "/admin/dashboard", api: "**/api/admin/**" },
    { p: "/decks",           api: "**/api/decks**" },
    { p: "/leaderboard",     api: "**/api/leaderboard**" },
  ];
  for (const c of cases) {
    H.flushLimits(true);
    const ctx = await b.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await ctx.newPage();
    await page.route(c.api, r => r.abort());
    await H.seedAuth(page, adm);
    await page.goto(H.APP + c.p, { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(2800);
    const r = await page.evaluate(`(${MEASURE_SEL})('[role="alert"] p, [role="alert"]')`);
    console.log(c.p, JSON.stringify(r, null, 0));
    await ctx.close();
  }
  await b.close();
})();
