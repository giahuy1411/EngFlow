/** Kiem tra Playwright co san khong (khong can classifier). */
const mods = ["playwright", "playwright-core", "puppeteer", "puppeteer-core"];
for (const m of mods) {
  try {
    const p = require.resolve(m);
    console.log(`OK      ${m}  -> ${p}`);
  } catch (e) {
    console.log(`MISSING ${m}`);
  }
}
console.log("");
console.log("node:", process.version);
console.log("cwd:", process.cwd());
