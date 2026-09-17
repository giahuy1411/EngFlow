const pw = require('playwright-core');
(async () => {
  const b = await pw.chromium.launch({ headless: true });
  const p = await b.newPage();
  await p.goto('http://localhost:5173/', { waitUntil: 'domcontentloaded' });
  await p.waitForTimeout(4000);
  console.log('TITLE', await p.title());
  console.log('BODYFONT', await p.evaluate(() => getComputedStyle(document.body).fontFamily));
  const els = await p.evaluate(() => { const s = new Set(); document.querySelectorAll('h1,h2,h3,button,a,p').forEach(e => s.add(getComputedStyle(e).fontFamily.split(',')[0])); return [...s]; });
  console.log('FONTS', JSON.stringify(els));
  await b.close();
})().catch(e => { console.error('ERR', e.message); process.exit(1); });
