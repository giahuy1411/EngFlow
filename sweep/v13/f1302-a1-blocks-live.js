/**
 * audit-v13 Phase 3 (A1) — live verification that authored blocks now reach the learner.
 *
 * Lesson 447 is the proof case: admin authored 3 sections (TEXT/IMAGE/AUDIO/TABLE only count
 * as renderable; it also holds 3 QUESTION + 1 SUBMISSION which must NOT render), and the
 * lesson ALSO has 6,621 chars of scraped lesson.content. So this checks both directions:
 *   - the authored TABLE/text appears (the fix), and
 *   - the page does not show the lesson body twice (the trap: the structure endpoint
 *     synthesizes a virtual section from lesson.content when nothing is materialized).
 */
const path = require("path");
const fs = require("fs");
const H = require(path.join(__dirname, "..", "v8", "ui", "lib.js"));
const { pw, APP, loginFull, seedAuth, flushLimits } = H;

const OUT = [];
function check(name, ok, detail) {
  OUT.push({ name, ok: !!ok, detail: detail || "" });
  console.log(`${ok ? "PASS" : "FAIL"}  ${name}${detail ? "  -- " + detail : ""}`);
}

(async () => {
  const CHROME = "C:/Users/ASUS/AppData/Local/ms-playwright/chromium-1237/chrome-win64/chrome.exe";
  const browser = await pw.chromium.launch(
    fs.existsSync(CHROME) ? { headless: true, executablePath: CHROME } : { headless: true }
  );
  try {
    const session = await loginFull("user@gmail.com", "123456");

    // ---------- lesson 447: has real blocks AND scraped content ----------
    {
      const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
      const page = await ctx.newPage();
      await flushLimits(true);
      await seedAuth(page, session);
      await page.goto(APP + "/lessons/447", { waitUntil: "networkidle" }).catch(() => {});
      await page.waitForTimeout(1500);

      const probe = await page.evaluate(() => {
        const t = document.body.innerText;
        return {
          hasAuthoredHeading: t.includes("Tài liệu bổ sung cho bài học này"),
          // the TABLE block authored in the builder. Compare textContent, not innerText:
          // the header carries Tailwind `uppercase`, and innerText reflects text-transform
          // (so innerText reads "DAI TU" while the DOM value is "Dai tu").
          hasTableHeader: !!Array.from(document.querySelectorAll("th")).find(e => e.textContent.trim() === "Dai tu"),
          tableRows: document.querySelectorAll("table tbody tr").length,
          // QUESTION block content must NOT appear (no learner view / no grading yet)
          leaksAnswer: t.includes("She ___ to school every day."),
          leaksSubmission: t.includes("Nhap bai viet cua ban o day"),
          // the trap: scraped content rendered twice
          authoredSections: document.querySelectorAll("section[aria-labelledby='lesson-authored-title']").length,
        };
      });

      check("447: authored blocks section renders", probe.hasAuthoredHeading);
      check("447: TABLE block renders (header 'Đại từ')", probe.hasTableHeader);
      check("447: TABLE block has rows", probe.tableRows >= 3, `rows=${probe.tableRows}`);
      check("447: QUESTION block NOT rendered", !probe.leaksAnswer);
      check("447: SUBMISSION block NOT rendered", !probe.leaksSubmission);
      check("447: exactly one authored section (no duplication)", probe.authoredSections === 1, `n=${probe.authoredSections}`);
      await ctx.close();
    }

    // ---------- lesson 445: NO blocks -> renderer must be absent (no duplicate body) ----------
    {
      const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
      const page = await ctx.newPage();
      await flushLimits(true);
      await seedAuth(page, session);
      await page.goto(APP + "/lessons/445", { waitUntil: "networkidle" }).catch(() => {});
      await page.waitForTimeout(1500);

      const probe = await page.evaluate(() => {
        const t = document.body.innerText;
        return {
          hasAuthoredHeading: t.includes("Tài liệu bổ sung cho bài học này"),
          authoredSections: document.querySelectorAll("section[aria-labelledby='lesson-authored-title']").length,
        };
      });
      check("445 (no blocks): renderer is absent", !probe.hasAuthoredHeading && probe.authoredSections === 0,
        `n=${probe.authoredSections}`);
      await ctx.close();
    }

    // ---------- the other 6 published lessons with blocks still render ----------
    for (const id of [446, 567, 11301, 41881, 91900]) {
      const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
      const page = await ctx.newPage();
      await flushLimits(true);
      await seedAuth(page, session);
      await page.goto(APP + `/lessons/${id}`, { waitUntil: "networkidle" }).catch(() => {});
      await page.waitForTimeout(1200);
      const n = await page.evaluate(() =>
        document.querySelectorAll("section[aria-labelledby='lesson-authored-title']").length);
      check(`lesson ${id}: authored section present (exactly 1)`, n === 1, `n=${n}`);
      await ctx.close();
    }
  } finally {
    await browser.close();
  }

  const failed = OUT.filter((r) => !r.ok);
  console.log(`\n=== Phase 3 live: ${OUT.length - failed.length}/${OUT.length} PASS ===`);
  fs.writeFileSync(
    path.join(__dirname, "..", "..", ".specify", "specs", "audit-v13-full", "evidence", "f13-02-a1-blocks-live.json"),
    JSON.stringify({ ranAt: new Date().toISOString(), results: OUT }, null, 2)
  );
  process.exit(failed.length ? 1 : 0);
})();
