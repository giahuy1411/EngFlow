const H = require("./lib.js");
const fs = require("fs");
const path = require("path");

const EDIR = path.join(__dirname, "..", "evidence", "verify-fixes");
fs.mkdirSync(EDIR, { recursive: true });

const out = { pass: true, checks: [] };
function check(name, cond, detail) {
  out.checks.push({ name: name, ok: !!cond, detail: String(detail) });
  console.log((cond ? "PASS " : "FAIL ") + name + " :: " + detail);
  if (!cond) out.pass = false;
}
function unwrap(j) { return (j && j.data !== undefined) ? j.data : j; }
async function api(p, token) {
  const r = await fetch(H.API + p, { headers: { Authorization: "Bearer " + token } });
  const j = await r.json().catch(() => null);
  return { status: r.status, body: unwrap(j) };
}

(async () => {
  try { H.flushLimits(true); } catch (e) { console.log("flushLimits warn: " + e.message); }
  const session = await H.loginFull("user@gmail.com", "123456");
  check("login", !!session.token, "token " + (session.token ? "ok" : "MISSING"));
  if (!session.token) throw new Error("no token");

  const me = await api("/api/auth/me", session.token);
  const rawStreak = me.body && me.body.currentStreak;
  check("auth-me", me.status === 200, "status=" + me.status + " rawStreak=" + rawStreak);

  const st = await api("/api/streak/current", session.token);
  const eff = st.body && st.body.currentStreak;
  const today = st.body && st.body.today;
  check("streak-current", st.status === 200, "status=" + st.status + " effective=" + eff + " today=" + today);

  const dash = await api("/api/dashboard/stats", session.token);
  const totalLessons = dash.body && dash.body.totalLessons;
  check("dashboard-stats", dash.status === 200, "status=" + dash.status + " totalLessons=" + totalLessons);

  const browser = await H.pw.chromium.launch({ headless: true });
  const ctx = await H.mkContext(browser, session.token);
  const page = await ctx.newPage();
  const errs = [];
  page.on("console", m => { if (m.type() === "error") errs.push(m.text().slice(0, 160)); });
  page.on("pageerror", e => errs.push(String(e).slice(0, 160)));
  await H.seedAuth(page, session);
  await page.goto(H.APP + "/profile", { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(2500);
  const url = page.url();
  check("profile-url", url.endsWith("/profile"), url);
  const headerNums = await page.$$eval("p.font-black.text-2xl", els => els.map(e => e.textContent.trim()));
  check("B1-header-equals-effective", headerNums.length > 0 && headerNums[0] === String(eff),
    "header=" + JSON.stringify(headerNums) + " effective=" + eff + " raw=" + rawStreak);
  const calTitle = await page.$eval(".streak-calendar h3", e => e.textContent.trim()).catch(() => null);
  check("profile-calendar-title", calTitle === "Chuoi ngay hoc" || (calTitle && calTitle.indexOf("Chu") === 0), "h3=" + calTitle);
  const calNum = await page.$eval(".streak-calendar p.text-3xl span:nth-child(2)", e => e.textContent.trim()).catch(() => null);
  check("B1-calendar-equals-effective", calNum === String(eff), "calendar=" + calNum + " effective=" + eff);
  const cells = await page.$$eval(".streak-calendar [role='gridcell']", els => els.length).catch(() => -1);
  check("profile-calendar-cells", cells >= 28, "gridcells=" + cells);
  check("profile-console", errs.length === 0, "consoleErrors=" + errs.length + (errs.length ? " :: " + errs.slice(0, 3).join(" | ") : ""));
  await page.screenshot({ path: path.join(EDIR, "profile.png"), fullPage: true });
  await browser.close();

  fs.writeFileSync(path.join(EDIR, "verify-fixes.json"), JSON.stringify(
    { at: new Date().toISOString(), api: { rawStreak: rawStreak, effective: eff, today: today, totalLessons: totalLessons }, checks: out.checks, pass: out.pass }, null, 2));
  console.log("OVERALL " + (out.pass ? "PASS" : "FAIL"));
  process.exit(out.pass ? 0 : 1);
})().catch(e => { console.error("FATAL " + (e && e.stack || e)); process.exit(2); });