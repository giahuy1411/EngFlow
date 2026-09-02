// Restore lesson 444 (accidentally deleted during audit-v3 UI test)
// Pattern mirror: sibling lesson 445 = 1 section "Nội dung bài học" + 1 TEXT block
const BASE = 'http://localhost:8080';

async function main() {
  const login = await fetch(BASE + '/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'admin@gmail.com', password: '123456' })
  }).then(r => r.json());
  const token = login.token;
  const auth = { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' };

  const fs = require('fs');
  const crawler = JSON.parse(fs.readFileSync('crawler/data/a1-grammar.json', 'utf8'));
  const unit = crawler.units[0]; // "English Grammar Exercises for A1 – be, possessives and pronouns"
  const rawHtml = unit.skills.grammar.content;

  // 1. Section
  const section = await fetch(BASE + '/api/admin/lessons/91900/sections', {
    method: 'POST', headers: auth,
    body: JSON.stringify({ title: 'Nội dung bài học', orderIndex: 10 })
  }).then(r => r.json());
  console.log('section:', JSON.stringify(section).slice(0, 120));
  const sectionId = (section.data || section).sectionId || (section.data || section).id;

  // 2. Block TEXT với cleaned HTML (trích phần entry-content)
  const startMark = '<div class="entry-content">';
  const endMark = '<div class="skill-footer';
  let clean = rawHtml;
  const sIdx = clean.indexOf(startMark);
  if (sIdx >= 0) {
    clean = clean.slice(sIdx + startMark.length);
    const eIdx = clean.indexOf(endMark);
    if (eIdx >= 0) clean = clean.slice(0, eIdx);
  }
  console.log('clean html len:', clean.length);

  const block = await fetch(BASE + '/api/admin/sections/' + sectionId + '/blocks', {
    method: 'POST', headers: auth,
    body: JSON.stringify({ blockType: 'TEXT', data: clean, orderIndex: 10 })
  }).then(r => r.json());
  console.log('block:', JSON.stringify(block).slice(0, 120));

  // 3. Re-seed exercises (force=false: chỉ sinh cho lessons thiếu exercises)
  const seed = await fetch(BASE + '/api/admin/exercises/seed?force=false', {
    method: 'POST', headers: auth
  }).then(r => r.json());
  console.log('seed result:', JSON.stringify(seed).slice(0, 300));
}

main().catch(e => { console.error('FAIL:', e.message); process.exit(1); });