const pw = require("playwright-core");
const APP = "http://localhost:5173";

(async () => {
  const browser = await pw.chromium.launch({ headless: true });
  const ctx = await browser.newContext({ acceptDownloads: true });
  const page = await ctx.newPage();
  await page.goto(APP + "/", { waitUntil: "domcontentloaded" });
  await page.evaluate(() => localStorage.setItem("token", "VICTIM-TOKEN-DO-NOT-LEAK"));

  const target = "/api/resources/zzv8-planted.html";
  let outcome = {};
  const [download] = await Promise.all([
    page.waitForEvent("download", { timeout: 8000 }).catch(() => null),
    page.goto(APP + target, { waitUntil: "domcontentloaded" }).catch(e => ({ err: String(e.message).slice(0, 60) })),
  ]);
  if (download) {
    outcome = { rendered: false, why: "browser downloaded it as an attachment", suggested: download.suggestedFilename() };
  } else {
    await page.waitForTimeout(600);
    const st = await page.evaluate(() => ({
      href: location.pathname,
      pwned: !!window.__XSS_PWNED__,
      ct: document.contentType,
      bodyText: document.body ? document.body.innerText.slice(0, 30) : null,
    }));
    outcome = { rendered: st.pwned || st.bodyText === "PLANTED", detail: st };
  }
  console.log("PLANTED .html RESULT:", JSON.stringify(outcome, null, 1));
  const stolen = await page.evaluate(() => localStorage.getItem("token"));
  console.log("victim token still present:", stolen === "VICTIM-TOKEN-DO-NOT-LEAK");
  await browser.close();
})().catch(e => { console.error("ERR", e.message); process.exit(1); });
