const pw = require('playwright-core');
const b = await pw.chromium.launch({ headless: true });
console.log('launched', typeof b);
