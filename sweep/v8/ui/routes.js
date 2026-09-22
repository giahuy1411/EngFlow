const H = require("./lib.js");
const fs = require("fs");

const GUEST = ["/", "/login", "/register", "/forgot-password", "/reset-password",
  "/lessons", "/lessons/445", "/lessons/447", "/videos", "/videos/1",
  "/leaderboard", "/decks", "/decks/10006", "/nonexistent-route-zz"];

const AUTH = ["/profile", "/search", "/decks/create", "/decks/10006/play/flashcard",
  "/decks/10006/play/quiz", "/decks/10006/play/memory", "/decks/10006/play/typing",
  "/decks/10006/play/listening", "/decks/10006/play/mixed", "/ai-vocab-generator",
  "/speaking", "/speaking/history", "/speaking/60014", "/speaking/60014/record",
  "/premium", "/premium/checkout",
  "/admin/dashboard", "/admin/lessons", "/admin/exercises", "/admin/users",
  "/admin/speaking-prompts", "/admin/speaking-submissions", "/admin/videos",
  "/admin/video-attempts", "/admin/447/build"];

async function formLogin(page, email, pass) {
  await page.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1500);
  const em = await page.$('input[type="email"], input[name="email"]');
  const pw = await page.$('input[type="password"]');
  if (!em || !pw) return "LOGIN FORM NOT FOUND";
  await em.fill(email);
  await pw.fill(pass);
  await page.click('button[type="submit"]');
  await page.waitForTimeout(4000);
  const st = await page.evaluate(() => ({
    token: !!localStorage.getItem("token"),
    user: !!localStorage.getItem("user"),
    path: location.pathname
  }));
  return st.token && st.user ? "logged in at " + st.path : "FAILED " + JSON.stringify(st);
}

(async () => {
  const browser = await H.pw.chromium.launch({ headless: true });
  const viewport = process.argv[3] === "mobile" ? { width: 375, height: 812 } : { width: 1440, height: 900};
  const tag = process.argv[2] || "admin";

  // Everything that can throw is inside try, and the cleanup that undoes this
  // run's real payment rows sits in `finally`. Writing the JSON report used to
  // run before cleanup on the happy path, which was fine until it threw: the
  // ENOENT on 2026-09-17 killed the process with the row still in the table.
  // A cleanup that only runs when nothing goes wrong is not a cleanup.
  try {
    const ctx = await browser.newContext({ viewport: viewport });
    const page = await ctx.newPage();

    if (tag === "guest") {
      console.log("guest session");
      for (const r of GUEST) await H.visit(page, r, "guest", { expect404: r.indexOf("nonexistent") >= 0 });
    } else {
      console.log("login:", await formLogin(page, "admin@gmail.com", "123456"));
      for (const r of GUEST.concat(AUTH)) {
        const rec = await H.visit(page, r, tag, { expect404: r.indexOf("nonexistent") >= 0 });
        const flag = (rec.errors.length || rec.failed.length || !rec.info.hasApp) ? " <<<" : "";
        console.log(("  " + r).padEnd(34) + " mounted=" + (rec.info.hasApp ? "Y" : "N")
          + " text=" + String(rec.info.textLen).padStart(5)
          + " err=" + rec.errors.length + " bad=" + rec.failed.length + flag);
        if (rec.errors.length) console.log("        ERR " + JSON.stringify(rec.errors.slice(0, 2)));
        if (rec.failed.length) console.log("        BAD " + JSON.stringify(rec.failed.slice(0, 3)));
      }
    }
    H.summarize(tag + " " + viewport.width + "px");
    // Resolve against THIS FILE, not process.cwd(). The old relative "ui/..." path
    // only worked when the operator happened to run from sweep/v8; running from
    // sweep/v8/ui threw ENOENT on write, and because the write sat BEFORE the
    // cleanup call it killed the process while the real payment row it had just
    // created was still in the table. v3.js/v3b.js already use __dirname.
    fs.writeFileSync(__dirname + "/routes-" + tag + "-" + viewport.width + ".json",
      JSON.stringify(H.results, null, 1));
  } finally {
    // Self-clean, and PROVE it.
    //
    // AUTH contains "/premium/checkout", whose component (PremiumCheckout.vue)
    // calls POST /api/v1/payment/create-order on mount to build the QR. That is a
    // REAL write to payment_transactions, so this sweep leaves business data
    // behind unless it undoes its own writes.
    //
    // MEASURED 2026-09-17: running this file as `admin` moved the table 126 -> 127
    // and nothing removed it. routes-all.js already cleans up; this sibling did
    // not, so the leak depended on which harness an operator happened to run.
    // A guest pass never reaches the mount (the guard bounces it to /login), so
    // only the authenticated tags can leak.
    if (tag !== "guest") {
      H.cleanupAuditPayments(126); // audit-v11 F130: baseline informational; assertion is self-clean
      console.log("DB parity after cleanup: " + H.dbParity()
        + "   (baseline 1471|43737|76|127|28|15|4|126|14|5)");
    }
    await browser.close();
  }
})().catch(e => { console.error("FATAL", e); process.exit(1); });
