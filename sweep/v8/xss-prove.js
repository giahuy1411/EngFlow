/**
 * xss-prove.js — Task 12 evidence: does the stored AI output ACTUALLY execute?
 *
 * This closes the one open question from security.js: the local model sometimes
 * echoes a raw tag back inside `exampleSentence` (nondeterministic -- 1 of 3 runs
 * did it for "<svg/onload=alert(1)>"). A raw tag in the API RESPONSE is only a
 * finding if it survives to the DOM, so this drives the real render path:
 *
 *   POST /api/ai/enrich-word  ->  POST /api/ai/save-vocab  ->  GET /api/decks  ->  open /decks/:id
 *
 * and then checks, in Chromium, whether the injected handler ran or whether any
 * inline on* attribute / script node exists in the DOM.
 *
 * It also unit-tests the exact sanitizer config the app uses (ALLOWED_TAGS
 * allowlist) against the same payloads, so the result does not depend on the
 * model happening to echo a tag on this particular run.
 */
const H = require("./lib.js");
const fs = require("fs");
const APP = "http://localhost:5173";

const MARK = "XSSPROBE" + Date.now();

// Captured BEFORE any write, in the same naive-VN wall clock the DB stores.
// This must be taken at module load: computing it later (at cleanup time) makes
// the "created_at >= start" window begin AFTER the rows were written, so the
// cleanup matches nothing and silently leaves residue. That exact mistake left
// 3 rows behind on the first attempt and pushed vocabulary 127 -> 130.
const RUN_START_NAIVE = new Date(Date.now() + 7 * 3600 * 1000)
  .toISOString().slice(0, 19).replace("T", " ");

const PAYLOADS = [
  '<img src=x onerror="window.__p=\'' + MARK + '\'">',
  "<svg/onload=window.__p='" + MARK + "'>",
  "<script>window.__p='" + MARK + "<\/script>",
  '<iframe src="javascript:window.__p=\'' + MARK + '\'">',
];

async function api(method, path, token, body) {
  H.flushLimits(true);
  const r = await fetch(H.BASE + path, {
    method,
    headers: {
      ...(body ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: "Bearer " + token } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  return { status: r.status, text: await r.text().catch(() => "") };
}

(async () => {
  const user = await H.login("user@gmail.com", "123456");
  const pw = require("playwright-core");
  const browser = await pw.chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await ctx.newPage();

  // ---- (A) static proof: the sanitizer config used by every call site -------
  console.log("=== (A) sanitizeText allowlist vs payloads (the actual app config) ===");
  await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  const san = await page.evaluate(async (payloads) => {
    const mod = await import("/src/utils/markdown.js");
    return payloads.map(p => ({ in: p, out: mod.sanitizeText(p) }));
  }, PAYLOADS);
  let staticBad = 0;
  for (const s of san) {
    const dangerous = /<\s*(script|svg|img|iframe|object|embed)\b/i.test(s.out) || /\son\w+\s*=/i.test(s.out) || /javascript:/i.test(s.out);
    if (dangerous) staticBad++;
    console.log((dangerous ? "  FAIL " : "  OK   ") + JSON.stringify(s.in).slice(0, 40).padEnd(42) + " -> " + JSON.stringify(s.out).slice(0, 60));
  }
  console.log("  dangerous outputs: " + staticBad + "/" + san.length);

  // ---- (B) dynamic proof: raw tag -> DB -> real page -----------------------
  console.log("\n=== (B) end-to-end: hostile word -> save-vocab -> render page ===");
  await page.evaluate(t => localStorage.setItem("token", t), user);
  const rows = [];
  for (const p of PAYLOADS) {
    const gen = await api("POST", "/api/ai/enrich-word", user, { word: p });
    let word = null, rawEchoed = false;
    try {
      const j = JSON.parse(gen.text);
      word = j.word;
      // did the MODEL echo an actual tag anywhere in the payload?
      rawEchoed = /<\s*(script|svg|img|iframe)\b/i.test(gen.text);
    } catch (e) {}
    let save = null;
    if (word) {
      try {
        const j = JSON.parse(gen.text);
        save = (await api("POST", "/api/ai/save-vocab", user, [j])).status;
      } catch (e) {}
    }
    rows.push({ payload: p, genStatus: gen.status, word, rawEchoed, saveStatus: save });
    console.log("  " + JSON.stringify(p).slice(0, 36).padEnd(38)
      + " gen=" + gen.status + " rawTagInResponse=" + rawEchoed + " savedWord=" + JSON.stringify(word) + " save=" + save);
  }

  // find the deck the saved words landed in and open it for real
  const decks = JSON.parse((await api("GET", "/api/decks", user)).text);
  const list = Array.isArray(decks) ? decks : (decks.data || decks.content || []);
  console.log("  decks visible to user: " + list.length);

  let fired = false;
  const handlerHits = [];
  for (const d of list.slice(0, 6)) {
    const id = d.deckId || d.id;
    if (!id) continue;
    await page.goto(APP + "/decks/" + id, { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(1500);
    const probe = await page.evaluate((mark) => {
      const bad = [];
      document.querySelectorAll("*").forEach(e => {
        for (const a of e.attributes) if (/^on/i.test(a.name)) bad.push(e.tagName + "[" + a.name + "]");
      });
      return {
        fired: window.__p === mark,
        inlineHandlers: bad.slice(0, 5),
        scripts: document.querySelectorAll("script:not([src])").length,
      };
    }, MARK).catch(() => ({ fired: false, inlineHandlers: [], scripts: -1 }));
    if (probe.fired) fired = true;
    handlerHits.push(...probe.inlineHandlers);
    if (probe.fired || probe.inlineHandlers.length) {
      console.log("  /decks/" + id + " fired=" + probe.fired + " handlers=" + JSON.stringify(probe.inlineHandlers));
    }
  }
  console.log("  pages opened: " + Math.min(list.length, 6) + "  executed=" + fired + "  inlineHandlerAttrs=" + handlerHits.length);

  await browser.close();

  // ---- (C) self-cleanup ----------------------------------------------------
  // This harness WRITES business rows (that is the whole point: a stored-XSS
  // proof has to store something). It must therefore remove exactly what it
  // created. Earlier revisions left 3 rows behind -- XSSPROBE*, 'iframe' -- and
  // the drift was only noticed when a later audit read 130 rows instead of the
  // 127 baseline. A harness that mutates data without cleaning it cannot be
  // trusted as evidence, so the cleanup is now part of the run, not a separate
  // manual step.
  //
  // Scope: the exact words we saved, lesson_id IS NULL (user-saved words, never
  // curriculum), and created_at >= this run's start. A legitimate word with the
  // same spelling but a real lesson_id can never be caught.
  console.log("\n=== (C) cleanup: removing the rows this run created ===");
  const savedWords = [...new Set(rows.map(r => r.word).filter(w => typeof w === "string" && w.length))];
  const startedNaive = RUN_START_NAIVE;
  const esc = s => "'" + String(s).replace(/'/g, "''") + "'";
  const wordList = savedWords.length ? savedWords.map(esc).join(", ") : "NULL";
  // Match EITHER the marker prefix OR one of the exact words we saved. The prefix
  // catches a saved word the model rewrote; the explicit list catches a short
  // noun the model extracted out of a payload (e.g. 'iframe'), which carries no
  // marker and would otherwise survive.
  const scope = "(word LIKE 'XSSPROBE%'"
    + (savedWords.length ? " OR word IN (" + wordList + ")" : "") + ")";
  const cleanSql = [
    "-- generated by xss-prove.js -- do not commit",
    "SET QUOTED_IDENTIFIER ON;", "GO",
    "PRINT '--- rows to remove ---';",
    "SELECT vocab_id, word, created_at FROM vocabulary",
    "WHERE lesson_id IS NULL AND created_at >= '" + startedNaive + "' AND " + scope,
    "ORDER BY vocab_id;", "GO",
    "DELETE FROM vocabulary",
    "WHERE lesson_id IS NULL AND created_at >= '" + startedNaive + "' AND " + scope + ";",
    "GO",
    "PRINT '--- remaining from this run (must be 0) ---';",
    "SELECT COUNT(*) AS remaining FROM vocabulary",
    "WHERE lesson_id IS NULL AND created_at >= '" + startedNaive + "' AND " + scope + ";",
    "GO",
    "PRINT '--- vocabulary total (baseline 127) ---';",
    "SELECT COUNT(*) AS vocabulary_total FROM vocabulary;", "GO", "",
  ].join("\n");

  const tmpSql = "xss-prove-cleanup.generated.sql";
  fs.writeFileSync(tmpSql, cleanSql);
  let cleanOut = "";
  try {
    const { execSync } = require("child_process");
    cleanOut = execSync("python sweep/v8/sqlrun.py sweep/v8/" + tmpSql,
      { cwd: require("path").join(__dirname, "..", ".."), encoding: "utf8" });
  } catch (e) {
    // sqlcmd exits 0 even when the batch fails (Msg 1934 etc.), so the output --
    // never the exit code alone -- decides whether the cleanup worked.
    cleanOut = e.stdout || e.message;
  }
  console.log(cleanOut.trim());
  const totalMatch = /vocabulary_total\s*\r?\n-+\r?\n(\d+)/.exec(cleanOut);
  const totalAfter = totalMatch ? Number(totalMatch[1]) : null;
  const remainMatch = /remaining\s*\r?\n-+\r?\n(\d+)/.exec(cleanOut);
  const remainingAfter = remainMatch ? Number(remainMatch[1]) : null;
  // "No SQL error" is NOT enough: the first attempt ran cleanly and still left
  // rows behind, because the WHERE clause matched nothing. Parity is the real
  // assertion, so require remaining === 0 AND the total back at the baseline.
  const BASELINE_VOCAB = 127;
  const cleanOk = !/Msg \d+/.test(cleanOut)
    && remainingAfter === 0
    && totalAfter === BASELINE_VOCAB;
  console.log("cleanup: no SQL error=" + !/Msg \d+/.test(cleanOut)
    + " remaining=" + remainingAfter + " total=" + totalAfter
    + " (baseline " + BASELINE_VOCAB + ") -> " + (cleanOk ? "PARITY RESTORED" : "RESIDUE PRESENT"));
  fs.unlinkSync(tmpSql);

  const pass = !fired && handlerHits.length === 0 && staticBad === 0 && cleanOk;
  console.log("\n=== XSS VERDICT ===");
  console.log("sanitizer neutralises all payloads : " + (staticBad === 0));
  console.log("no execution in real browser       : " + (!fired));
  // NOTE: the parens matter. `"str" + n === 0` parses as `("str" + n) === 0`,
  // i.e. a string compared to a number -> always false, which printed a bogus
  // "false" on this line while the verdict line (correctly parenthesised) said
  // PASS. Keep every boolean concatenation wrapped.
  console.log("no inline handler attr in DOM      : " + (handlerHits.length === 0));
  console.log("rows created by this run cleaned   : " + cleanOk + " (vocabulary now " + totalAfter + ", baseline 127)");
  console.log("VERDICT: " + (pass ? "PASS - stored AI output cannot execute" : "FAIL - exploitable"));

  fs.writeFileSync("xss-prove.json", JSON.stringify({ mark: MARK, runStart: RUN_START_NAIVE, staticSanitize: san, staticDangerous: staticBad, rows, fired, handlerHits, cleanOk, remainingAfter, totalAfter, pass }, null, 1));
  console.log("wrote xss-prove.json");
  process.exit(pass ? 0 : 1);
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
