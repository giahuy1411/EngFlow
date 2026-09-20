const http = require('http');
const targets = [
  ['snapshot', 'http://localhost:8080/api/streak/snapshot'],
  ['current',  'http://localhost:8080/api/streak/current'],
  ['history',  'http://localhost:8080/api/streak/history'],
  ['lessons',  'http://localhost:8080/api/lessons'],
  ['frontend', 'http://localhost:5173/'],
];
(async () => {
  for (const [name, url] of targets) {
    await new Promise((resolve) => {
      const req = http.get(url, (res) => {
        let body = '';
        res.on('data', (c) => { if (body.length < 200) body += c; });
        res.on('end', () => {
          console.log(`${name.padEnd(9)} status=${res.statusCode} body=${JSON.stringify(body.slice(0, 120))}`);
          resolve();
        });
      });
      req.on('error', (e) => { console.log(`${name.padEnd(9)} ERROR ${e.code || e.message}`); resolve(); });
      req.setTimeout(6000, () => { req.destroy(); console.log(`${name.padEnd(9)} TIMEOUT`); resolve(); });
    });
  }
})();
