const pw = require('playwright-core');
console.log('resolved', require.resolve('playwright-core'));
console.log('chromium type', typeof pw.chromium);
pw.chromium.executablePath && console.log('exe:', pw.chromium.executablePath());
