// Node script tạo GitHub issues (tránh PS here-string pitfalls)
const { execSync } = require('child_process');

function getGithubToken() {
  const out = execSync('git credential fill', { input: 'protocol=https\nhost=github.com\n\n', encoding: 'utf8' });
  const line = out.split('\n').find(l => l.startsWith('password='));
  if (!line) throw new Error('no stored github credential');
  return line.slice('password='.length).trim();
}

const ISSUES = [
  {
    title: 'perf(payments): payment status latency ~3.5s inside polling window',
    labels: ['performance', 'payments'],
    body: `## Phát hiện\n\`GET /api/payments/status/{orderCode}\` mất ~3,5s trong cửa sổ polling (đo bằng perf script, audit-v3 2026-09-02).\n\n## Nguyên nhân\nFrontend poll theo chu kỳ + chuỗi fallback query SePay; không có cơ chế push.\n\n## Đề xuất\n1. Webhook-driven: SePay webhook cập nhật \`payment_transactions\` + Redis pub/sub, frontend nhận qua SSE hoặc poll Redis nhanh.\n2. Giảm poll interval có chủ đích (1.5s → 800ms) chỉ khi trang checkout đang mở.\n3. Cache negative-status 1-2s để bớt query lặp.\n\n## Bằng chứng\n- audit-v3 REPORT.md §4.2\n- Đo 5 lần liên tiếp: 3.2-3.8s`
  },
  {
    title: 'fix(listening): 89/449 listening exercises missing audio_url',
    labels: ['bug', 'content', 'listening'],
    body: `## Phát hiện\n89/449 bài listening trong DB không có \`audio_url\` — sinh ra khi MCP venv chưa cài supertonic.\n\n## Hiện trạng\n- Fallback giọng máy trình duyệt đang hoạt động: \`frontend/src/utils/speech.js\` (\`speakEnglish\`, \`blankOutForSpeech\` che \`____\` thành \`...\`).\n- UX vẫn dùng được nhưng chất lượng giọng không đồng nhất.\n\n## Đề xuất\nChạy lại \`generate_listening\` qua MCP (venv có supertonic) cho các bài thiếu, hoặc script so DB vs crawler data → tạo WAV → \`POST /api/admin/audio-upload\` → update \`audio_url\`.\n\n## Bằng chứng\n- audit-v3 REPORT.md §4.3`
  },
  {
    title: 'a11y: dynamically rendered form fields missing id/name attributes (39 inputs)',
    labels: ['accessibility', 'frontend', 'low-priority'],
    binding: 'low',
    body: `## Phát hiện\nChrome DevTools console: "A form field element should have an id or name attribute" — 39 lần ở tab BÀI TẬP của lesson (input render động cho từng câu hỏi). Không ảnh hưởng chức năng.\n\n## Đề xuất\nSinh \`id\`/\`name\` từ \`exercise.id\` + \`exerciseType\` khi render form, gắn \`<label :for>\` tương ứng.\n\n## Bằng chứng\n- audit-v3 REPORT.md §1.2 console log, §4.4`
  },
  {
    title: 'chore(constitution): update backend test baseline 196 → 221',
    labels: ['documentation'],
    body: `## Phát hiện\n\`.specify/memory/constitution.md\` ghi baseline backend = 196 tests; thực tế sau audit-v3 là **221 tests** (221/221 PASS).\n\n## Việc cần làm\n- Sửa số baseline trong constitution.\n- Quy tắc: PR thay đổi baseline phải update constitution cùng commit.\n\n## Bằng chứng\n- audit-v3 REPORT.md §4.5, §5`
  }
];

async function main() {
  const token = getGithubToken();
  const h = {
    'Authorization': 'token ' + token,
    'User-Agent': 'engflow-audit',
    'Accept': 'application/vnd.github+json',
    'Content-Type': 'application/json; charset=utf-8'
  };
  for (const issue of ISSUES) {
    const payload = JSON.stringify({ title: issue.title, body: issue.body, labels: issue.labels });
    const res = await fetch('https://api.github.com/repos/giahuy1411/EngFlow/issues', { method: 'POST', headers: h, body: payload });
    const data = await res.json();
    if (res.status === 201) {
      console.log('CREATED #' + data.number + ': ' + data.title + ' -> ' + data.html_url);
    } else {
      console.log('FAILED (' + res.status + '): ' + issue.title + ' — ' + JSON.stringify(data).slice(0, 200));
    }
    await new Promise(r => setTimeout(r, 800));
  }
}
main().catch(e => { console.error('FATAL:', e.message); process.exit(1); });