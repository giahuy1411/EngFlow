/**
 * audit-v10 UI harness — boc lop mong quanh sweep/v8/ui/lib.js de dung duoc
 * browser CO SAN tren may nay.
 *
 * VAN DE
 * ------
 * Moi script trong sweep/v8/ui/*.js goi thang `H.pw.chromium.launch(...)`, tuc
 * doi hoi playwright phai tai rieng mot Chromium (~150MB) ve
 * %LOCALAPPDATA%\ms-playwright. May nay KHONG co ban do, va `playwright-core`
 * co tinh KHONG tu tai browser (do la khac biet duy nhat giua `playwright` va
 * `playwright-core`).
 *
 * CACH GIAI
 * ---------
 * `playwright-core` van dieu khien duoc mot browser da cai san qua
 * `executablePath`. May nay co Microsoft Edge. Edge la Chromium that (cung
 * engine, cung CDP), nen moi phep do layout/CSS/console deu dung y nghia.
 *
 * GIOI HAN PHAI GHI RO
 * --------------------
 * - Day la EDGE, khong phai Chrome. So sanh screenshot giua cac vong audit la
 *   hop le vi moi vong cua v10 deu chay tren Edge; nhung so voi screenshot
 *   Chromium cua v8 thi co the lech font rendering. Khong duoc tron hai nguon.
 * - `channel: 'msedge'` cua playwright-core doi hoi ban playwright day du moi
 *   tu tim registry; o day ta truyen thang duong dan nen khong phu thuoc
 *   registry.
 *
 * Chay: const { launch, H } = require("./ui-lib");
 */
const fs = require("fs");
const path = require("path");

const H = require("../v8/ui/lib.js");

// Thu tu uu tien: Chrome that -> Brave -> Edge.
//
// Brave duoc them vao 2026-09-20. No cung engine Chromium nen moi phep do
// layout/CSS/console deu hop le y nhu Chrome. Ly do them: may nay KHONG co
// Google Chrome, va chrome-devtools MCP (cung chay tren Chromium) da duoc tro
// sang Brave — dung cung mot browser cho ca hai harness thi ket qua doi chieu
// duoc voi nhau, thay vi mot ben Brave mot ben Edge.
const CANDIDATES = [
  "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
  "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
  "C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe",
  "C:\\Program Files (x86)\\BraveSoftware\\Brave-Browser\\Application\\brave.exe",
  "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
  "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
];

let EXECUTABLE = null;
for (const c of CANDIDATES) {
  try { if (fs.existsSync(c)) { EXECUTABLE = c; break; } } catch (e) {}
}

/**
 * Launch browser bang binary co san.
 * Tra ve Browser (giong het pw.chromium.launch).
 */
async function launch(opts = {}) {
  if (!EXECUTABLE) {
    throw new Error(
      "Khong tim thay Chrome/Edge nao. Cai mot trong hai, hoac chay " +
      "`npx playwright install chromium` de tai Chromium cua playwright."
    );
  }
  return H.pw.chromium.launch({
    headless: true,
    executablePath: EXECUTABLE,
    ...opts,
  });
}

module.exports = { ...H, launch, EXECUTABLE };
