/** Tim browser binary de playwright-core dung (channel hoac executablePath). */
const fs = require("fs");
const candidates = [
  "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
  "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
  "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
  "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
  process.env.LOCALAPPDATA + "\\Google\\Chrome\\Application\\chrome.exe",
];
let found = null;
for (const c of candidates) {
  try { if (fs.existsSync(c)) { console.log("FOUND  " + c); if (!found) found = c; } }
  catch (e) {}
}
if (!found) console.log("KHONG TIM THAY browser nao trong danh sach chuan");
else console.log("\nDung: " + found);
