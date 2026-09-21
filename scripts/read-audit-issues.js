const { execFileSync } = require('child_process');

function getGithubToken() {
  // execFileSync with an argument array — no shell, nothing to inject.
  const out = execFileSync('git', ['credential', 'fill'], {
    input: 'protocol=https\nhost=github.com\n\n',
    encoding: 'utf8'
  });
  const line = out.split('\n').find(l => l.startsWith('password='));
  if (!line) throw new Error('no stored github credential');
  return line.slice('password='.length).trim();
}

const REPO = 'giahuy1411/EngFlow';

async function main() {
  const token = getGithubToken();
  const h = {
    'Authorization': 'token ' + token,
    'User-Agent': 'engflow-audit',
    'Accept': 'application/vnd.github+json',
    'Content-Type': 'application/json; charset=utf-8'
  };
  for (const n of [5, 6, 7, 8]) {
    const res = await fetch(`https://api.github.com/repos/${REPO}/issues/${n}`, { headers: h });
    const d = await res.json();
    console.log(`#${n} [${d.state}] ${d.title}`);
    console.log('   comments:', d.comments, '| labels:', (d.labels || []).map(l => l.name).join(','));
    console.log('   body:', (d.body || '').replace(/\r?\n/g, ' ').slice(0, 400));
    console.log('');
  }
}
main().catch(e => { console.error('FATAL:', e.message); process.exit(1); });
