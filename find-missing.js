const https = require('https');
const http = require('http');
const fs = require('fs');
const path = require('path');
const cheerio = require('cheerio');

const BASE = 'https://english-practice.net';

function fetchUrl(url, retries = 10) {
  return new Promise((resolve, reject) => {
    const wbUrl = `https://web.archive.org/web/2025/${url}`;
    const client = wbUrl.startsWith('https') ? https : http;
    const req = client.get(wbUrl, { headers: { 'User-Agent': 'Mozilla/5.0' }, timeout: 60000 }, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve(data));
    });
    req.on('error', (err) => {
      if (retries > 0) {
        setTimeout(() => fetchUrl(url, retries - 1).then(resolve).catch(reject), 5000);
      } else {
        reject(err);
      }
    });
    req.setTimeout(60000, () => { req.destroy(); req.emit('error', new Error('timeout')); });
  });
}

async function main() {
  console.log('Fetching A1 grammar index from Wayback...');
  const html = await fetchUrl(`${BASE}/english-grammar-exercises-for-a1/`);
  
  const $ = cheerio.load(html);
  
  // Find ALL links that contain grammar-exercises-for-a1
  const allLinks = [];
  $('a[href*="grammar-exercises-for-a1"]').each((i, el) => {
    const href = $(el).attr('href');
    const title = $(el).text().trim();
    if (title && href) {
      // Extract original URL from Wayback URL
      let cleanUrl = href;
      const wbMatch = href.match(/web\.archive\.org\/web\/\d+\/(https?:\/\/english-practice\.net\/.+)/);
      if (wbMatch) cleanUrl = wbMatch[1];
      
      if (!allLinks.find(l => l.url === cleanUrl)) {
        allLinks.push({ title, url: cleanUrl });
      }
    }
  });
  
  console.log(`\nFound ${allLinks.length} total links:`);
  allLinks.forEach(l => console.log(`  ${l.title}`));
  
  // Find links that DON'T match the standard pattern
  const standardPattern = `/english-grammar-exercises-for-a1-`;
  const nonStandard = allLinks.filter(l => !l.url.includes(standardPattern));
  const standard = allLinks.filter(l => l.url.includes(standardPattern));
  
  console.log(`\n=== Standard pattern (${standard.length} links): ===`);
  standard.forEach(l => console.log(`  ${l.title}`));
  
  console.log(`\n=== Non-standard pattern (${nonStandard.length} links) ===`);
  nonStandard.forEach(l => console.log(`  ${l.title} | ${l.url}`));
}

main().catch(err => { console.error(err); process.exit(1); });
