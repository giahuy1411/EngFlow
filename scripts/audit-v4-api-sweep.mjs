// audit-v4 API sweep — chạy: node scripts/audit-v4-api-sweep.mjs
// Kết quả: bảng console + audit-v4-api-results.json
const BASE = 'http://localhost:8080';
const LABEL = 'Audit v4';
const results = [];
let userTok = null, adminTok = null;

async function req(method, path, { token, body, raw } = {}) {
  const t0 = Date.now();
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  let res;
  try {
    res = await fetch(BASE + path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
  } catch (e) {
    return { status: 0, ms: Date.now() - t0, body: String(e) };
  }
  const ms = Date.now() - t0;
  let out;
  if (raw) out = await res.text();
  else { const txt = await res.text(); try { out = JSON.parse(txt); } catch { out = txt; } }
  return { status: res.status, ms, body: out };
}

function check(name, ok, detail = '') {
  results.push({ name, ok, detail });
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? '  — ' + detail : ''}`);
}

const expect = (cond, got, want) => cond ? 'ok' : `got ${JSON.stringify(got).slice(0, 90)}, want ${want}`;

async function main() {
  // ---------- AUTH ----------
  let r = await req('POST', '/api/auth/login', { body: { email: 'user@gmail.com', password: '123456' } });
  userTok = r.body?.data?.token || r.body?.token;
  check('auth/login user', r.status === 200 && !!userTok, `status=${r.status} ${r.ms}ms`);

  r = await req('POST', '/api/auth/login', { body: { email: 'admin@gmail.com', password: '123456' } });
  adminTok = r.body?.data?.token || r.body?.token;
  check('auth/login admin', r.status === 200 && !!adminTok, `status=${r.status}`);

  r = await req('POST', '/api/auth/login', { body: { email: 'user@gmail.com', password: 'wrong' } });
  check('auth/login sai mật khẩu → 400/401', [400, 401, 403].includes(r.status), `status=${r.status}`);

  const tEmail = `${LABEL.toLowerCase().replace(' ', '')}.${Date.now()}@test.local`;
  const tUsername = `AuditV4Test${Date.now()}`;
  r = await req('POST', '/api/auth/register', { body: { username: tUsername, email: tEmail, password: 'Test12345!', fullName: 'Audit v4 Test User' } });
  check('auth/register user test → 201', r.status === 201 || r.status === 200, `status=${r.status} ${JSON.stringify(r.body).slice(0, 80)}`);
  const tUserId = r.body?.data?.id ?? r.body?.id;
  const tLogin = await req('POST', '/api/auth/login', { body: { email: tEmail, password: 'Test12345!' } });
  const tTok = tLogin.body?.data?.token || tLogin.body?.token;
  check('auth/login user test', tLogin.status === 200 && !!tTok, `status=${tLogin.status}`);

  r = await req('POST', '/api/auth/register', { body: { email: 'bad', password: 'x' } });
  check('auth/register body sai → 400', r.status === 400, `status=${r.status}`);

  r = await req('GET', '/api/auth/me', { token: userTok });
  check('auth/me user', r.status === 200 && !!r.body?.data?.email || !!r.body?.email, `status=${r.status}`);

  r = await req('POST', '/api/auth/forgot-password', { body: { email: tEmail } });
  check('auth/forgot-password (email test) → 200 generic', r.status === 200, `status=${r.status}`);

  // ---------- LESSONS ----------
  r = await req('GET', '/api/lessons?page=0&size=5', { token: userTok });
  const lessons = r.body?.data?.content || r.body?.content || [];
  check('lessons list p0s5', r.status === 200 && lessons.length === 5, `status=${r.status} n=${lessons.length} ${r.ms}ms`);
  const lessonId = lessons[0]?.lessonId || lessons[0]?.id;
  r = await req('GET', `/api/lessons/${lessonId}`, { token: userTok });
  check('lessons detail', r.status === 200 && !!((r.body?.data?.title) || (r.body?.title)) && r.status !== 404, `id=${lessonId} status=${r.status}`);
  r = await req('GET', `/api/lessons/99999999`, { token: userTok });
  check('lessons detail id sai → 404', r.status === 404, `status=${r.status}`);
  r = await req('GET', `/api/admin/lessons/${lessonId}/structure`, { token: adminTok });
  check('lessons structure (admin)', r.status === 200, `status=${r.status}`);

  // lesson có exercises (tìm qua vài id đầu danh sách)
  let exLesson = null, exList = [];
  for (const l of lessons.slice(0, 5)) {
    const id = l.lessonId || l.id;
    const rr = await req('GET', `/api/lessons/${id}/exercises`, { token: userTok });
    const arr = rr.body?.data || rr.body || [];
    if (rr.status === 200 && Array.isArray(arr) && arr.length > 0) { exLesson = id; exList = arr; break; }
  }
  check('tìm lesson có exercises', !!exLesson, `lesson=${exLesson} n=${exList.length}`);

  // ---------- EXERCISES grade + submit + attempts ----------
  if (exLesson) {
    const answers = exList.slice(0, 3).map(e => ({ exerciseId: e.exerciseId || e.id, userAnswer: 'audit-v4-dummy' }));
    r = await req('POST', `/api/lessons/${exLesson}/exercises/grade`, { token: userTok, body: { answers } });
    check('exercises/grade 3 câu (không lưu attempt)', r.status === 200 && Array.isArray(r.body?.results || r.body?.data?.results), `status=${r.status} ${JSON.stringify(r.body).slice(0, 60)}`);
    r = await req('POST', `/api/lessons/${exLesson}/exercises/submit`, { token: userTok, body: { answers } });
    check('exercises/submit (lưu attempt)', r.status === 200, `status=${r.status}`);
    r = await req('GET', `/api/lessons/${exLesson}/exercises/attempts`, { token: userTok });
    const hist = r.body?.data || r.body || [];
    check('exercises/attempts history sau submit', r.status === 200 && Array.isArray(hist) && hist.length >= 1, `n=${hist.length}`);
  }

  // ---------- DECKS / FLASHCARDS / GAMES ----------
  r = await req('GET', '/api/decks', { token: userTok });
  const decks = r.body?.data?.content || r.body?.content || [];
  check('decks list', r.status === 200 && decks.length >= 1, `n=${decks.length}`);
  const deckId = decks.find(d => (d.wordCount || 0) > 0)?.id || decks[0]?.id || 10007;
  r = await req('GET', `/api/decks/${deckId}`, { token: userTok });
  check('deck detail', r.status === 200 && r.status !== 404, `deck=${deckId} status=${r.status}`);
  const words = r.body?.words || r.body?.data?.words || [];
  check('deck words (embedded)', r.status === 200 && words.length >= 1, `n=${words.length}`);
  const vocabId = words[0]?.vocabularyId || words[0]?.vocabulary?.id || words[0]?.id;
  r = await req('POST', '/api/flashcards/review', { token: userTok, body: { vocabularyId: vocabId, isKnown: true } });
  check('flashcards/review', [200, 201].includes(r.status), `status=${r.status} vocab=${vocabId}`);
  for (const g of ['quiz', 'typing', 'mixed']) {
    r = await req('GET', `/api/games/${g}/${deckId}`, { token: userTok });
    check(`games/${g}/${deckId}`, r.status === 200, `status=${r.status}`);
  }
  r = await req('GET', `/api/games/quiz/999999`, { token: userTok });
  check('games/quiz deck sai → 404 (fix GameService)', r.status === 404, `status=${r.status}`);

  // ---------- VIDEO ----------
  r = await req('GET', '/api/v1/video-lessons', { token: userTok });
  const vids = r.body?.data?.content || r.body?.content || r.body?.data || r.body || [];
  check('video-lessons list', r.status === 200 && vids.length >= 1, `n=${vids.length}`);
  const vidId = Array.isArray(vids) ? (vids[0]?.id || vids[0]?.videoId || 1) : 1;
  r = await req('GET', `/api/v1/video-lessons/${vidId}`, { token: userTok });
  check('video-lesson detail', r.status === 200 && r.status !== 404, `status=${r.status}`);
  // attempts là multipart (lineIndex + file wav) — test bằng user test để không bẩn data user thật
  {
    const sr = 8000, n = 800;
    const buf = Buffer.alloc(44 + n * 2);
    buf.write('RIFF', 0); buf.writeUInt32LE(36 + n * 2, 4); buf.write('WAVE', 8);
    buf.write('fmt ', 12); buf.writeUInt32LE(16, 16); buf.writeUInt16LE(1, 20); buf.writeUInt16LE(1, 22);
    buf.writeUInt32LE(sr, 24); buf.writeUInt32LE(sr * 2, 28); buf.writeUInt16LE(2, 32); buf.writeUInt16LE(16, 34);
    buf.write('data', 36); buf.writeUInt32LE(n * 2, 40);
    for (let i = 0; i < n; i++) buf.writeInt16LE(0, 44 + i * 2);
    const fd = new FormData();
    fd.append('lineIndex', '0');
    fd.append('file', new Blob([buf], { type: 'audio/wav' }), 'audit-v4-test.wav');
    const t0va = Date.now();
    const vres = await fetch(`${BASE}/api/v1/video-lessons/${vidId}/attempts`, { method: 'POST', headers: { Authorization: `Bearer ${tTok}` }, body: fd });
    check('video attempt POST multipart', [200, 201].includes(vres.status), `status=${vres.status} ${Date.now() - t0va}ms ${(await vres.text()).slice(0, 80)}`);
    // JSON body lên multipart endpoint → giờ phải 400 (fix GlobalExceptionHandler)
    const vjson = await fetch(`${BASE}/api/v1/video-lessons/${vidId}/attempts`, { method: 'POST', headers: { Authorization: `Bearer ${tTok}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ lineIndex: 0 }) });
    check('video attempt POST JSON → 400 (fix 500)', vjson.status === 400, `status=${vjson.status}`);
  }

  // ---------- VOCABULARY + DICTIONARY ----------
  r = await req('GET', '/api/vocabulary?page=0&size=5', { token: userTok });
  check('vocabulary list', r.status === 200, `status=${r.status}`);
  const t0dict = Date.now();
  r = await req('GET', '/api/vocabulary/dictionary/sunny', { token: userTok });
  check('dictionary/sunny (proxy+cache)', r.status === 200, `status=${r.status} ${Date.now() - t0dict}ms`);
  const t1dict = Date.now();
  r = await req('GET', '/api/vocabulary/dictionary/sunny', { token: userTok });
  const dictMs = Date.now() - t1dict;
  check('dictionary/sunny lần 2 (cache hit)', r.status === 200 && dictMs < 1500, `${dictMs}ms`);
  r = await req('GET', `/api/flashcards/status/${vocabId}`, { token: userTok });
  check('flashcards/status SRS', r.status === 200, `status=${r.status}`);

  // ---------- SPEAKING ----------
  r = await req('GET', '/api/v1/speaking-prompts?page=0&size=5', { token: userTok });
  const prompts = r.body?.data?.content || r.body?.content || [];
  check('speaking prompts list', r.status === 200 && prompts.length >= 1, `n=${prompts.length}`);
  const promptId = prompts[0]?.id || prompts[0]?.promptId;
  r = await req('GET', `/api/v1/speaking-submissions?page=0&size=5`, { token: userTok });
  check('speaking submissions list', r.status === 200 && r.status !== 404, `status=${r.status}`);

  // ---------- LEADERBOARD / STREAK / PROGRESS / SRS / DASHBOARD ----------
  r = await req('GET', '/api/leaderboard', { token: userTok });
  check('leaderboard', r.status === 200, `status=${r.status}`);
  r = await req('GET', '/api/streak/current', { token: userTok });
  check('streak/current', r.status === 200, `status=${r.status}`);
  r = await req('GET', '/api/streak/history', { token: userTok });
  check('streak/history', r.status === 200, `status=${r.status}`);
  r = await req('GET', '/api/users/progress', { token: userTok });
  check('users/progress', r.status === 200, `status=${r.status}`);
  r = await req('GET', '/api/srs/stats', { token: userTok });
  check('srs/stats', r.status === 200, `status=${r.status}`);
  r = await req('GET', `/api/srs/due/${deckId}`, { token: userTok });
  check('srs/due/{deck}', r.status === 200, `status=${r.status}`);
  r = await req('GET', '/api/dashboard/stats', { token: userTok });
  check('dashboard/stats', r.status === 200, `status=${r.status}`);

  // ---------- PAYMENT ----------
  r = await req('POST', '/api/v1/payment/create-order', { token: userTok, body: { planType: 'MONTH' } });
  check('payment/create-order', r.status === 200, `status=${r.status} ${JSON.stringify(r.body).slice(0, 100)}`);
  const st = [];
  for (let i = 0; i < 10; i++) { const rr = await req('GET', '/api/v1/payment/status', { token: userTok }); st.push(rr.ms); }
  const avg = Math.round(st.reduce((a, b) => a + b) / st.length);
  check('payment/status ×10 (baseline latency)', avg < 1500, `avg=${avg}ms min=${Math.min(...st)} max=${Math.max(...st)} [${st.join(',')}]`);
  r = await req('POST', '/api/webhook/sepay', { raw: true, body: JSON.stringify({ id: 'auditv4-fake' }) });
  check('webhook sepay payload lạ → 200 + success:false', r.status === 200 && /"success"\s*:\s*false/.test(String(r.body)), `status=${r.status} body=${String(r.body).slice(0, 80)}`);

  // ---------- NEGATIVE: roles ----------
  r = await req('GET', '/api/admin/users');
  check('admin không token → 401', r.status === 401, `status=${r.status}`);
  r = await req('GET', '/api/admin/users', { token: userTok });
  check('admin bằng token user → 403', r.status === 403, `status=${r.status}`);
  r = await req('POST', '/api/v1/payment/create-order');
  check('payment create không token → 401', r.status === 401, `status=${r.status}`);

  // ---------- ADMIN CRUD (có nhãn, dọn sau) ----------
  r = await req('POST', '/api/admin/lessons', { token: adminTok, body: { title: `${LABEL} API Test Lesson`, description: 'Tao boi script audit v4 — se bi xoa', content: '<p>audit v4</p>', level: 'ELEMENTARY', category: 'audit-v4', durationMinutes: 10, skillType: 'READING', isPublished: false, orderIndex: 99001 } });
  const created = r.body?.data || r.body || {};
  const testLessonId = created.lessonId || created.id;
  check('admin/lessons create (nhãn Audit v4)', [200, 201].includes(r.status) && !!testLessonId, `status=${r.status} id=${testLessonId}`);
  if (testLessonId) {
    r = await req('PUT', `/api/admin/lessons/${testLessonId}`, { token: adminTok, body: { title: `${LABEL} API Test Lesson (edited)`, level: 'ELEMENTARY', skillType: 'READING' } });
    check('admin/lessons update', [200, 201].includes(r.status), `status=${r.status}`);
    r = await req('PUT', `/api/admin/lessons/${testLessonId}/toggle-publish`, { token: adminTok });
    check('admin/lessons toggle-publish', [200, 201].includes(r.status), `status=${r.status}`);
    r = await req('POST', `/api/admin/lessons/${testLessonId}/sections`, { token: adminTok, body: { title: 'Audit v4 section' } });
    const secId = r.body?.id || r.body?.sectionId || r.body?.data?.id || r.body?.data?.sectionId;
    check('admin sections create', [200, 201].includes(r.status) && !!secId, `status=${r.status} sec=${secId}`);
    if (secId) {
      r = await req('POST', `/api/admin/sections/${secId}/blocks`, { token: adminTok, body: { blockType: 'TEXT', data: '<p>audit v4 block</p>' } });
      check('admin blocks create', [200, 201].includes(r.status), `status=${r.status} ${JSON.stringify(r.body).slice(0, 80)}`);
    }
    r = await req('POST', `/api/admin/lessons/${testLessonId}/snapshots`, { token: adminTok });
    check('admin snapshots create', [200, 201].includes(r.status), `status=${r.status}`);
    r = await req('GET', `/api/admin/lessons/${testLessonId}/snapshots`, { token: adminTok });
    check('admin snapshots list', r.status === 200, `status=${r.status}`);
    // AI async generate trên lesson test (đếm 2, dễ dọn)
    const t0ai = Date.now();
    r = await req('POST', '/api/admin/exercises/ai/generate-async', { token: adminTok, body: { lessonId: testLessonId, count: 2, exerciseType: 'FILL_BLANK' } });
    const batchId = r.body?.batchId;
    check('ai generate-async → 202 batchId', [200, 202].includes(r.status) && !!batchId, `status=${r.status} ${Date.now() - t0ai}ms ${JSON.stringify(r.body).slice(0, 80)}`);
    // poll status tối đa 150s — status shape: {running, generated, processed, errors, ...}
    let done = false, lastP = '';
    for (let i = 0; i < 30 && !done && batchId; i++) {
      await new Promise(s => setTimeout(s, 5000));
      const pr = await req('GET', `/api/admin/exercises/ai/status?batchId=${batchId}`, { token: adminTok });
      lastP = JSON.stringify(pr.body).slice(0, 150);
      if (pr.body?.running === false && (pr.body?.generated || 0) >= 1 && (pr.body?.errors || 0) === 0) done = true;
    }
    check('ai status COMPLETED (≤150s)', done, `last=${lastP}`);
    r = await req('DELETE', `/api/admin/lessons/${testLessonId}`, { token: adminTok });
    check('admin/lessons delete test lesson (dọn dẹp)', [200, 204].includes(r.status), `status=${r.status}`);
  }

  // admin lists
  r = await req('GET', '/api/admin/users?page=0&size=10', { token: adminTok });
  check('admin/users list', r.status === 200, `status=${r.status}`);
  if (tUserId) {
    r = await req('PUT', `/api/admin/users/${tUserId}/toggle-premium`, { token: adminTok });
    check('admin toggle-premium ON (user test)', [200, 201].includes(r.status), `status=${r.status}`);
    r = await req('PUT', `/api/admin/users/${tUserId}/revoke-premium`, { token: adminTok });
    check('admin revoke-premium (user test)', [200, 201].includes(r.status), `status=${r.status}`);
    r = await req('PUT', `/api/admin/users/${tUserId}/toggle-active`, { token: adminTok });
    check('admin toggle-active OFF (user test)', [200, 201].includes(r.status), `status=${r.status}`);
  }
  r = await req('GET', '/api/admin/exercises?page=0&size=10&exerciseType=FILL_BLANK', { token: adminTok });
  check('admin/exercises filter type', r.status === 200, `status=${r.status}`);
  r = await req('GET', '/api/admin/exercises/seed/status', { token: adminTok });
  check('admin/exercises seed status', r.status === 200 || r.status === 404, `status=${r.status}`);
  r = await req('GET', '/api/v1/admin/video-lessons', { token: adminTok });
  check('admin video-lessons list', r.status === 200, `status=${r.status}`);
  r = await req('GET', '/api/v1/admin/speaking-prompts?page=0&size=5', { token: adminTok });
  check('admin speaking-prompts', r.status === 200, `status=${r.status}`);
  r = await req('GET', '/api/v1/admin/speaking-submissions?page=0&size=5', { token: adminTok });
  check('admin speaking-submissions', r.status === 200, `status=${r.status}`);

  // ---------- AI vocab (sync, đo thời gian) ----------
  const t0 = Date.now();
  r = await req('POST', '/api/ai/generate-vocab', { token: adminTok, body: { topic: 'auditv4-travel', level: 'A1', count: 3 } });
  check('ai/generate-vocab (Ollama 1.5b)', [200, 201].includes(r.status), `status=${r.status} ${Date.now() - t0}ms ${JSON.stringify(r.body).slice(0, 80)}`);

  // ---------- SUMMARY ----------
  const fails = results.filter(x => !x.ok);
  console.log(`\n===== TOTAL ${results.length} — PASS ${results.length - fails.length} — FAIL ${fails.length} =====`);
  const fs = await import('fs');
  fs.writeFileSync('audit-v4-api-results.json', JSON.stringify({ at: new Date().toISOString(), results, paymentStatusMs: st, dictMs }, null, 2));
  if (fails.length) { console.log('FAILED:'); fails.forEach(f => console.log(' -', f.name, '|', f.detail)); }
}

main().catch(e => { console.error('SCRIPT ERROR', e); process.exit(1); });
