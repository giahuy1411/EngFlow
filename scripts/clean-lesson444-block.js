// Clean the restored block: strip <script>/<style>/wrapper divs
const BASE = 'http://localhost:8080';
async function main() {
  const login = await fetch(BASE + '/api/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'admin@gmail.com', password: '123456' })
  }).then(r => r.json());
  const auth = { 'Authorization': 'Bearer ' + login.token, 'Content-Type': 'application/json' };

  const fs = require('fs');
  const crawler = JSON.parse(fs.readFileSync('crawler/data/a1-grammar.json', 'utf8'));
  const rawHtml = crawler.units[0].skills.grammar.content;

  // extract entry-content, strip scripts/styles/comments/ads wrappers
  const startMark = '<div class="entry-content">';
  const sIdx = rawHtml.indexOf(startMark);
  let clean = sIdx >= 0 ? rawHtml.slice(sIdx + startMark.length) : rawHtml;
  const endMark = '<div class="skill-footer';
  const eIdx = clean.indexOf(endMark);
  if (eIdx >= 0) clean = clean.slice(0, eIdx);
  clean = clean
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/<ins[\s\S]*?<\/ins>/gi, '')
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
  console.log('clean len:', clean.length, '| has script:', /<script/i.test(clean));

  // find existing block id
  const structure = await fetch(BASE + '/api/admin/lessons/91900/structure', { headers: { 'Authorization': 'Bearer ' + login.token } }).then(r => r.json());
  const sec = (structure.data || structure)[0];
  const blockId = (sec.blocks || [])[0]?.blockId || (sec.blocks || [])[0]?.id;
  console.log('blockId:', blockId);

  const upd = await fetch(BASE + '/api/admin/blocks/' + blockId, {
    method: 'PUT', headers: auth,
    body: JSON.stringify({ blockType: 'TEXT', data: clean, orderIndex: 10 })
  }).then(r => r.json());
  console.log('updated block:', JSON.stringify(upd).slice(0, 100));
}
main().catch(e => { console.error('FAIL:', e.message); process.exit(1); });