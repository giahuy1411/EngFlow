/**
 * Live verification that the Lesson Builder "Đường B" section is GONE.
 *
 * audit-v15 removed the Lesson Builder: lesson content comes from the scrape
 * (lessons.content) and exercises live in `exercises`. The v13 original asserted the
 * authored section RENDERED; this asserts the opposite and stronger property — on every
 * lesson that used to carry one, the "Tài liệu bổ sung cho bài học này" block is ABSENT
 * and the scraped content still renders.
 *
 * Run: node sweep/harness/f1302-a1-blocks-live.js [--audit <name>] [--out <dir>]
 */
const path = require("path");
const fs = require("fs");
const { OUT: EVID } = require("./_config.js");
const H = require(path.join(__dirname, "..", "v8", "ui", "lib.js"));
const { pw, APP, loginFull, seedAuth, flushLimits } = H;

const OUT = [];
function check(name, ok, detail) {
  OUT.push({ name, ok: !!ok, detail: detail || "" });
  console.log(`${ok ? "PASS" : "FAIL"}  ${name}${detail ? "  -- " + detail : ""}`);
}

const LESSONS = [446, 447, 567, 10889, 11300, 11301, 41881, 91900];

(async () => {
  const CHROME = "C:/Users/ASUS/AppData/Local/ms-playwright/chromium-1237/chrome-win64/chrome.exe";
  const browser = await pw.chromium.launch(
    fs.existsSync(CHROME) ? { headless: true, executablePath: CHROME } : { headless: true }
  );
  try {
    const session = await loginFull("user@gmail.com", "123456");

    for (const id of LESSONS) {
      const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
      const page = await ctx.newPage();
      await flushLimits(true);
      await seedAuth(page, session);
      await page.goto(APP + `/lessons/${id}`, { waitUntil: "networkidle" }).catch(() => {});
      await page.waitForTimeout(1200);

      const probe = await page.evaluate(() => {
        const t = document.body.innerText;
        return {
          hasAuthoredHeading: t.includes("Tài liệu bổ sung cho bài học này"),
          hasAuthoredKicker: t.includes("Nội dung biên soạn"),
          authoredSections: document.querySelectorAll("section[aria-labelledby='lesson-authored-title']").length,
          hasOldTableHeader: !!Array.from(document.querySelectorAll("th")).find(e => e.textContent.trim() === "Dai tu"),
        };
      });

      check(`lesson ${id}: authored section ABSENT`,
        !probe.hasAuthoredHeading && !probe.hasAuthoredKicker && probe.authoredSections === 0,
        `heading=${probe.hasAuthoredHeading} kicker=${probe.hasAuthoredKicker} n=${probe.authoredSections}`);
      check(`lesson ${id}: old authored table header absent`, !probe.hasOldTableHeader);
      await ctx.close();
    }

    // Scraped content still renders on the two published proof lessons.
    for (const id of [446, 447]) {
      const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
      const page = await ctx.newPage();
      await flushLimits(true);
      await seedAuth(page, session);
      await page.goto(APP + `/lessons/${id}`, { waitUntil: "networkidle" }).catch(() => {});
      await page.waitForTimeout(1200);
      const chars = await page.evaluate(() =>
        (document.querySelector("#tabpanel-content") || {}).innerText?.length || 0);
      check(`lesson ${id}: scraped content still renders`, chars > 500, `chars=${chars}`);
      await ctx.close();
    }
  } finally {
    await browser.close();
  }

  const failed = OUT.filter((r) => !r.ok);
  console.log(`\n=== Route B removal live: ${OUT.length - failed.length}/${OUT.length} PASS ===`);
  fs.mkdirSync(EVID, { recursive: true });
  fs.writeFileSync(path.join(EVID, "f15-route-b-removal-live.json"),
    JSON.stringify({ ranAt: new Date().toISOString(), results: OUT }, null, 2));
  process.exit(failed.length ? 1 : 0);
})();
