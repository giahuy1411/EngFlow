const https = require('https');
const http = require('http');
const fs = require('fs');
const path = require('path');

const cheerio = require('cheerio');

const BASE_URL = 'https://english-practice.net';
let viaWayback = false;

function waybackUrl(url) {
  if (!viaWayback) return url;
  if (url.includes('web.archive.org')) return url;
  return `https://web.archive.org/web/2025/${url}`;
}

function stripWaybackBanner(html) {
  // Remove the Wayback Machine banner/overlay elements
  return html.replace(/<script\b[^>]*>.*?<\/script>/gs, '')
             .replace(/<link[^>]*wbars[^>]*>/gi, '')
             .replace(/<!-- BEGIN WAYBACK TOOLBAR -->[\s\S]*?<!-- END WAYBACK TOOLBAR -->/g, '')
             .replace(/<div[^>]*id="donate"[^>]*>[\s\S]*?<\/div>/gi, '')
             .replace(/<div[^>]*class="[^"]*wbars[^"]*"[^>]*>[\s\S]*?<\/div>/gi, '');
}

const WAYBACK_RETRIES = 15;
const WAYBACK_RETRY_DELAY = 10000;
const WAYBACK_TIMEOUT = 120000;

// SSRF guard: crawler chỉ được fetch đúng nguồn dữ liệu đã cho phép.
// Chặn protocol lạ, hostname ngoài allowlist, và host loopback/private/reserved.
const ALLOWED_HOSTS = new Set(['english-practice.net', 'www.english-practice.net', 'web.archive.org']);
const BLOCKED_HOST_RE = /^(127\.|10\.|0\.|169\.254\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.|::1|f[cd][0-9a-f]{2}:)/i;

function assertSafeUrl(rawUrl) {
  let parsed;
  try { parsed = new URL(rawUrl); } catch { throw new Error(`Blocked non-URL: ${rawUrl}`); }
  if (parsed.protocol !== 'https:' && parsed.protocol !== 'http:') {
    throw new Error(`Blocked protocol ${parsed.protocol}: ${rawUrl}`);
  }
  const host = parsed.hostname.toLowerCase();
  if (!ALLOWED_HOSTS.has(host) || BLOCKED_HOST_RE.test(host)) {
    throw new Error(`Blocked host ${host}: not in crawl allowlist`);
  }
  return rawUrl;
}

function fetchUrl(url, retries = null) {
  const maxRetries = retries !== null ? retries : (viaWayback ? WAYBACK_RETRIES : 3);
  return new Promise((resolve, reject) => {
    let actualUrl;
    try {
      actualUrl = waybackUrl(assertSafeUrl(url));
      if (actualUrl !== url) assertSafeUrl(actualUrl);
    } catch (err) {
      reject(err);
      return;
    }
    const client = actualUrl.startsWith('https') ? https : http;
    const req = client.get(actualUrl, { headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' } }, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        const redirectUrl = res.headers.location.startsWith('http') ? res.headers.location : new URL(res.headers.location, actualUrl).href;
        fetchUrl(redirectUrl, maxRetries).then(resolve).catch(reject);
        return;
      }
      res.setEncoding('utf8');
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve(data));
    });
    req.on('error', (err) => {
      if (maxRetries > 0) {
        const delay = viaWayback ? WAYBACK_RETRY_DELAY : 3000;
        console.log(`    Retry in ${delay}ms... (${maxRetries} left) - ${err.message}`);
        setTimeout(() => fetchUrl(url, maxRetries - 1).then(resolve).catch(reject), delay);
      } else {
        reject(err);
      }
    });
    req.setTimeout(viaWayback ? WAYBACK_TIMEOUT : 30000, () => { req.destroy(); req.emit('error', new Error('timeout')); });
  });
}

function extractTopicLinks(html, level, skillKey) {
  const $ = cheerio.load(html);
  const links = [];
  const pats = [
    `/topic-english-${skillKey}-exercises-for-${level}/`,
    `/topic-${skillKey}-exercises-for-${level}/`,
    `/english-${skillKey}-exercises-for-${level}`,
    `/${skillKey}-exercises-for-${level}`
  ];
  const isNoise = (href, text) => {
    if (!text || text.length < 3) return true;
    if (href.includes('web.archive.org') && !href.includes('english-practice.net')) return true;
    if (text.match(/^\d+ captures$/i) || text === skillKey) return true;
    // Skip if URL points to the index page itself
    const slug = href.split('/').filter(Boolean).pop() || '';
    if (!slug || slug === `english-${skillKey}-exercises-for-${level}` || slug === skillKey) return true;
    return false;
  };
  // Dedup by URL
  const addLink = (href, text) => {
    if (!href || isNoise(href, text)) return;
    let fullUrl = href.startsWith('http') ? href : new URL(href, BASE_URL).href;
    const wbMatch = fullUrl.match(/web\.archive\.org\/web\/\d+\/(https?:\/\/english-practice\.net\/.+)/);
    if (wbMatch) fullUrl = wbMatch[1];
    if (!links.find(l => l.url === fullUrl)) {
      links.push({ title: text, url: fullUrl });
    }
  };
  // Search the ENTIRE page for topic links
  for (const pat of pats) {
    $(`a[href*="${pat}"]`).each((i, el) => {
      addLink($(el).attr('href'), $(el).text().trim());
    });
  }
  return links;
}

function extractImages($, content) {
  content.find('img').each((i, el) => {
    let src = $(el).attr('src') || '';
    if (src && !src.startsWith('http')) {
      $(el).attr('src', new URL(src, BASE_URL).href);
    }
  });
}

function extractAudio($, content) {
  content.find('audio').each((i, el) => {
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
}

function extractExerciseContent(html) {
  const cleaned = stripWaybackBanner(html);
  const $ = cheerio.load(cleaned);
  const contentDiv = $('div.entry-content, div.post-content, article, main, .content-area');
  if (contentDiv.length === 0) {
    return $('body').html() || html;
  }
  let content = contentDiv.first();
  extractImages($, content);
  extractAudio($, content);
  return content.html() || html;
}

async function main() {
  const args = process.argv.slice(2);
  let level = 'a1';
  let skill = 'vocabulary';

  // Whitelist level/skill (dùng trong URL pattern và tên file output — chặn path traversal)
  const ALLOWED_LEVELS = new Set(['a1', 'a2', 'b1', 'b2', 'c1', 'c2']);
  const ALLOWED_SKILLS = new Set(['vocabulary', 'grammar', 'word_skills', 'reading', 'listening']);

  // Sanitizer: chỉ cho phép segment chữ-số/-/_ — mọi giá trị argv chảy vào URL
  // pattern, selector, tên file đều phải qua hàm này.
  function safeSegment(value, label) {
    if (typeof value !== 'string' || !/^[a-z][a-z0-9_-]*$/i.test(value)) {
      throw new Error(`Unsafe ${label}: ${value}`);
    }
    return value;
  }

  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--level' && i + 1 < args.length) level = args[i + 1].toLowerCase();
    if (args[i] === '--skill' && i + 1 < args.length) skill = args[i + 1].toLowerCase();
    if (args[i] === '--via-wayback') viaWayback = true;
  }
  if (!ALLOWED_LEVELS.has(level) || !ALLOWED_SKILLS.has(skill)) {
    console.error(`Refusing to run: level="${level}" / skill="${skill}" không nằm trong whitelist.`);
    process.exit(1);
  }
  level = safeSegment(level, 'level');
  skill = safeSegment(skill, 'skill');

  const validLevels = ['a1', 'a2', 'b1', 'b2'];
  const validSkills = ['vocabulary', 'grammar', 'listening', 'reading', 'speaking', 'writing', 'word_skills'];

  if (!validLevels.includes(level)) {
    console.error(`Invalid level: ${level}. Valid: ${validLevels.join(', ')}`);
    process.exit(1);
  }
  if (!validSkills.includes(skill)) {
    console.error(`Invalid skill: ${skill}. Valid: ${validSkills.join(', ')}`);
    process.exit(1);
  }

  // Slug lookup từ literal map — giá trị chảy vào URL pattern/selector/tên file
  // chỉ có thể là các chuỗi literal dưới đây, không thể mang ../ hay ký tự lạ.
  const LEVEL_SLUGS = { a1: 'a1', a2: 'a2', b1: 'b1', b2: 'b2' };
  const SKILL_SLUGS = { vocabulary: 'vocabulary', grammar: 'grammar', reading: 'reading', listening: 'listening', 'word_skills': 'word-skills' };
  const levelSlug = LEVEL_SLUGS[level];
  const skillSlug = SKILL_SLUGS[skill];
  if (!levelSlug || !skillSlug) {
    console.error(`Refusing to run: level="${level}" / skill="${skill}" không có slug an toàn.`);
    process.exit(1);
  }

  const skillKey = skillSlug;
  const indexUrl = `${BASE_URL}/english-${skillSlug}-exercises-for-${levelSlug}/`;

  console.log(`Crawling level=${level}, skill=${skill}`);
  console.log(`Index URL: ${indexUrl}`);

  const indexHtml = await fetchUrl(indexUrl);
  let topicLinks = extractTopicLinks(indexHtml, level, skillKey);

  if (topicLinks.length === 0) {
    console.log('No topic links found. Trying full page fallback...');
    const $ = cheerio.load(indexHtml);
    const pats = [
      `/topic-english-${skillKey}-exercises-for-${level}/`,
      `/topic-${skillKey}-exercises-for-${level}/`,
      `/english-${skillKey}-exercises-for-${level}`,
      `/${skillKey}-exercises-for-${level}`
    ];
    for (const pat of pats) {
      $(`a[href*="${pat}"]`).each((i, el) => {
        const href = $(el).attr('href');
        const title = $(el).text().trim();
        if (title && href) {
          let fullUrl = href.startsWith('http') ? href : new URL(href, BASE_URL).href;
          const wbMatch = fullUrl.match(/web\.archive\.org\/web\/\d+\/(https?:\/\/english-practice\.net\/.+)/);
          if (wbMatch) fullUrl = wbMatch[1];
          if (!topicLinks.find(l => l.url === fullUrl)) {
            topicLinks.push({ title, url: fullUrl });
          }
        }
      });
      if (topicLinks.length > 0) break;
    }
  }

  console.log(`Found ${topicLinks.length} topics:`);
  topicLinks.forEach(t => console.log(`  - ${t.title}`));

  // level/skill đã qua whitelist ở trên → file output luôn nằm trong crawler/data
  const dataDir = path.join(__dirname, 'data');
  const outputFile = path.join(dataDir, `${level}-${skill}.json`);
  fs.mkdirSync(path.dirname(outputFile), { recursive: true });

  // Resume: load existing progress
  let doneUrls = new Set();
  let doneTitles = new Set();
  let units = [];
  if (fs.existsSync(outputFile)) {
    try {
      const existing = JSON.parse(fs.readFileSync(outputFile, 'utf-8'));
      units = existing.units || [];
      existing.units.forEach(u => {
        if (u._url) doneUrls.add(u._url);
        if (u.title) doneTitles.add(u.title);
      });
      console.log(`Resuming from ${units.length} already-saved units`);
    } catch (e) {
      console.log('Could not parse existing file, starting fresh');
    }
  }

  for (let i = 0; i < topicLinks.length; i++) {
    const topic = topicLinks[i];
    if (doneUrls.has(topic.url) || doneTitles.has(topic.title)) {
      console.log(`[${i + 1}/${topicLinks.length}] ${topic.title}... SKIP (already saved)`);
      continue;
    }
    process.stdout.write(`[${i + 1}/${topicLinks.length}] ${topic.title}... `);
    try {
      const html = await fetchUrl(topic.url);
      const $ = cheerio.load(html);

      const h1 = $('h1.entry-title, h1.post-title, h1').first().text().trim();
      const title = h1 || topic.title;

      // Skip if already saved (matches by title or url)
      if (doneTitles.has(title) || doneUrls.has(topic.url)) {
        console.log(' SKIP (already saved)');
        continue;
      }

      const contentHtml = extractExerciseContent(html);

      units.push({
        unit: units.length + 1,
        _url: topic.url,
        title,
        skills: {
          [skill]: { content: `<div class="skill-html">${contentHtml}</div>` }
        }
      });
      doneUrls.add(topic.url);
      doneTitles.add(title);
      // Save incrementally after each success (keep _url for resume)
      const saveData = { level: level.toUpperCase(), skill, units: units.map(u => ({ ...u })) };
      fs.writeFileSync(outputFile, JSON.stringify(saveData, null, 2), 'utf-8');
      console.log(' OK');
    } catch (err) {
      console.log(` FAIL: ${err.message}`);
    }
    await new Promise(r => setTimeout(r, 1500));
  }

  // Final save (keep _url for resume)
  const output = { level: level.toUpperCase(), skill, units: units.map(u => ({ ...u })) };
  fs.writeFileSync(outputFile, JSON.stringify(output, null, 2), 'utf-8');
  console.log(`\nDone! Saved ${units.length} units to ${outputFile}`);
}

main().catch(err => {
  console.error('Fatal:', err);
  process.exit(1);
});
