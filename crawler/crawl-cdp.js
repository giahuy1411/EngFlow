const http = require('http');
const fs = require('fs');
const path = require('path');
const cheerio = require('cheerio');

const BASE_URL = 'https://english-practice.net';

// === CDP Client ===
class CDPPage {
  constructor(wsUrl) {
    this.wsUrl = wsUrl;
    this.ws = null;
    this._msgId = 0;
    this._pending = {};
  }

  async connect() {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(this.wsUrl);
      this.ws.onopen = () => resolve();
      this.ws.onerror = reject;
      this.ws.onmessage = (event) => {
        const msg = JSON.parse(event.data);
        if (msg.id && this._pending[msg.id]) {
          this._pending[msg.id](msg);
          delete this._pending[msg.id];
        }
      };
      setTimeout(() => reject(new Error('CDP connect timeout')), 10000);
    });
  }

  async send(method, params = {}) {
    return new Promise((resolve, reject) => {
      const id = ++this._msgId;
      this._pending[id] = (msg) => {
        if (msg.error) reject(new Error(msg.error.message));
        else resolve(msg.result);
      };
      this.ws.send(JSON.stringify({ id, method, params }));
      setTimeout(() => {
        delete this._pending[id];
        reject(new Error(`CDP timeout: ${method}`));
      }, 60000);
    });
  }

  async navigate(url) {
    await this.send('Page.enable');
    const navResult = await this.send('Page.navigate', { url });
    await new Promise((resolve) => {
      const handler = (event) => {
        const msg = JSON.parse(event.data);
        if (msg.method === 'Page.frameStoppedLoading') {
          this.ws.removeEventListener('message', handler);
          setTimeout(resolve, 2000);
        }
      };
      this.ws.addEventListener('message', handler);
      setTimeout(() => {
        this.ws.removeEventListener('message', handler);
        resolve();
      }, 20000);
    });
    await new Promise(r => setTimeout(r, 2000));
  }

  async getHTML() {
    const result = await this.send('Runtime.evaluate', {
      expression: 'document.documentElement.outerHTML',
      returnByValue: true
    });
    return result.result.value;
  }

  close() {
    if (this.ws) this.ws.close();
  }
}

async function getCDPPage() {
  const res = await fetch('http://localhost:9222/json');
  const raw = await res.text();
  const pages = JSON.parse(raw);
  const list = Array.isArray(pages[0]) ? pages[0] : (Array.isArray(pages) ? pages : [pages]);
  let target = list.find(p => p.url && p.url.includes('localhost:5173')) || list[0];
  if (!target) {
    const newRes = await fetch('http://localhost:9222/json/new');
    target = await newRes.json();
  }
  const cdp = new CDPPage(target.webSocketDebuggerUrl);
  await cdp.connect();
  return cdp;
}

// === Parsing (shared with crawl.js) ===
function extractTopicLinks(html, level, skillKey) {
  const $ = cheerio.load(html);
  const links = [];
  const pat1 = new RegExp('/' + skillKey.replace('-', '[-]?') + '-exercises-for-' + level + '-');
  const pat2 = new RegExp('/topic-english-' + skillKey.replace('-', '[-]?') + '-');
  const pat3 = new RegExp('/english-' + skillKey.replace('-', '[-]?') + '-exercises-for-' + level + '-');

  $('a[href]').each((i, el) => {
    const href = $(el).attr('href');
    const title = $(el).text().trim();
    if (!title || !href) return;
    if (href.includes('web.archive.org') || href.includes('wp-admin') || href === '#') return;
    if (!pat1.test(href) && !pat2.test(href) && !pat3.test(href)) return;
    // Skip index pages
    if (new RegExp('/english-?(word-skills|vocabulary|grammar|listening|reading|writing|speaking)-exercises-for-' + level + '/$').test(href)) return;
    if (href === '/') return;
    const fullUrl = href.startsWith('http') ? href : new URL(href, BASE_URL).href;
    if (!links.find(l => l.url === fullUrl)) {
      links.push({ title, url: fullUrl });
    }
  });
  return links;
}

function extractExerciseContent(html) {
  const $ = cheerio.load(html);
  $('img').each((i, el) => {
    let src = $(el).attr('src') || '';
    if (src && !src.startsWith('http')) {
      $(el).attr('src', new URL(src, BASE_URL).href);
    }
  });
  $('audio').each((i, el) => {
    let src = $(el).attr('src') || '';
    if (src && !src.startsWith('http')) {
      $(el).attr('src', new URL(src, BASE_URL).href);
    }
    $(el).find('source').each((j, source) => {
      let ssrc = $(source).attr('src') || '';
      if (ssrc && !ssrc.startsWith('http')) {
        $(source).attr('src', new URL(ssrc, BASE_URL).href);
      }
    });
    $(el).attr('controls', 'true');
  });
  const contentDiv = $('div.entry-content, div.post-content, article, main, .content-area');
  if (contentDiv.length === 0) return $('body').html() || html;
  let content = contentDiv.first();
  return content.html() || html;
}

// === Main ===
async function main() {
  const args = process.argv.slice(2);
  let level = 'a1', skill = 'vocabulary';
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--level' && i + 1 < args.length) level = args[i + 1].toLowerCase();
    if (args[i] === '--skill' && i + 1 < args.length) skill = args[i + 1].toLowerCase();
  }

  const validLevels = ['a1', 'a2', 'b1', 'b2'];
  const validSkills = ['vocabulary', 'grammar', 'listening', 'reading', 'speaking', 'writing', 'word_skills'];
  if (!validLevels.includes(level) || !validSkills.includes(skill)) {
    console.error(`Usage: node crawl-cdp.js --level a1|a2|b1|b2 --skill ${validSkills.join('|')}`);
    process.exit(1);
  }

  const skillKey = skill === 'word_skills' ? 'word-skills' : skill;
  const indexUrl = `${BASE_URL}/english-${skillKey}-exercises-for-${level}/`;
  console.log(`CDP Crawl: level=${level}, skill=${skill}`);
  console.log(`Index: ${indexUrl}`);

  const cdp = await getCDPPage();
  console.log('CDP connected');

  // Fetch index page
  console.log('Fetching index...');
  await cdp.navigate(indexUrl);
  const indexHtml = await cdp.getHTML();
  let topicLinks = extractTopicLinks(indexHtml, level, skillKey);

  if (topicLinks.length === 0) {
    console.log('Fallback: searching all links...');
    const $ = cheerio.load(indexHtml);
    $('a[href]').each((i, el) => {
      const href = $(el).attr('href');
      const title = $(el).text().trim();
      if (title && href && href.includes(`/${skillKey}-exercises-for-${level}`)) {
        const fullUrl = href.startsWith('http') ? href : new URL(href, BASE_URL).href;
        if (!topicLinks.find(l => l.url === fullUrl)) {
          topicLinks.push({ title, url: fullUrl });
        }
      }
    });
  }

  console.log(`Found ${topicLinks.length} topics:`);
  topicLinks.forEach(t => console.log(`  - ${t.title}`));

  // Resume support
  const outputFile = path.resolve(__dirname, 'data', `${level}-${skill}.json`);
  fs.mkdirSync(path.dirname(outputFile), { recursive: true });
  let doneUrls = new Set();
  let units = [];
  if (fs.existsSync(outputFile)) {
    try {
      const existing = JSON.parse(fs.readFileSync(outputFile, 'utf-8'));
      units = existing.units || [];
      existing.units.forEach(u => {
        if (u._url) doneUrls.add(u._url);
      });
      console.log(`Resuming: ${units.length} already saved`);
    } catch (e) { /* ignore */ }
  }

  for (let i = 0; i < topicLinks.length; i++) {
    const topic = topicLinks[i];
    if (doneUrls.has(topic.url)) {
      console.log(`[${i + 1}/${topicLinks.length}] ${topic.title}... SKIP`);
      continue;
    }
    process.stdout.write(`[${i + 1}/${topicLinks.length}] ${topic.title}... `);
    try {
      await cdp.navigate(topic.url);
      const html = await cdp.getHTML();
      const $ = cheerio.load(html);
      const h1 = $('h1.entry-title, h1.post-title, h1').first().text().trim();
      const title = h1 || topic.title;

      if (doneUrls.has(topic.url)) {
        console.log(' SKIP');
        continue;
      }

      const contentHtml = extractExerciseContent(html);
      units.push({
        unit: units.length + 1, _url: topic.url, title,
        skills: { [skill]: { content: `<div class="skill-html">${contentHtml}</div>` } }
      });
      doneUrls.add(topic.url);
      const saveData = { level: level.toUpperCase(), skill, units: units.map(u => ({ ...u })) };
      fs.writeFileSync(outputFile, JSON.stringify(saveData, null, 2), 'utf-8');
      console.log(' OK');
    } catch (err) {
      console.log(` FAIL: ${err.message}`);
    }
    await new Promise(r => setTimeout(r, 2000));
  }

  cdp.close();
  const output = { level: level.toUpperCase(), skill, units: units.map(u => ({ ...u })) };
  fs.writeFileSync(outputFile, JSON.stringify(output, null, 2), 'utf-8');
  console.log(`\nDone! Saved ${units.length} units to ${outputFile}`);
}

main().catch(err => {
  console.error('Fatal:', err);
  process.exit(1);
});
