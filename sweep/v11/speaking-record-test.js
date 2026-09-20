/**
 * audit-v11 — speaking RECORDING end-to-end with a fake microphone.
 *
 * Follows the recipe recorded in AGENTS.md: Chromium with
 *   --use-fake-ui-for-media-stream --use-fake-device-for-media-stream
 * plus context.grantPermissions(['microphone']), driving
 *   #enable-microphone-button -> #start-recording-button -> #stop-recording-button -> #submit-recording-button
 *
 * Expected outcome, stated up front so a "failure" is not misread as a defect:
 *   The fake mic emits SILENCE. Whisper therefore transcribes an empty string, and the
 *   submission is stored with status FAILED. That is the designed behaviour, not a bug —
 *   AGENTS.md records the same measurement from 2026-09-16.
 *
 * What this proves: the whole chain runs — getUserMedia -> MediaRecorder -> upload ->
 * MinIO object -> assess (Whisper) -> DB row. What it does NOT prove: real pronunciation
 * scoring, which needs real speech.
 *
 * Run: node sweep/v11/speaking-record-test.js
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const API = "http://localhost:8080";
const APP = "http://localhost:5173";
const R = { pass: 0, fail: 0, notes: [] };
function check(name, cond, detail) {
  if (cond) { R.pass++; console.log("  PASS  " + name); }
  else { R.fail++; console.log("  FAIL  " + name + "  -> " + detail); }
  R.notes.push((cond ? "PASS " : "FAIL ") + name + (cond ? "" : " :: " + detail));
}

function sqlScalar(sql) {
  fs.writeFileSync("sweep/v11/_sp_q.sql", "SET NOCOUNT ON;\n" + sql + "\n", "utf8");
  const out = execFileSync("python", ["sweep/v8/sqlrun.py", "sweep/v11/_sp_q.sql"], { cwd: ".", encoding: "utf8" });
  const m = out.match(/VALUE=(\S+)/);
  return m ? m[1] : "UNPARSED";
}

(async () => {
  const { chromium } = require("playwright-core");
  const H = require("../v8/ui/lib.js");

  // Reuse the repo's browser finder; it only probes for Edge, and on this machine the
  // installed browser is a cached Playwright Chromium, so fall back to that.
  // (Without this the launch fails with "Executable doesn't exist", which is a HARNESS
  // failure, not an app failure — worth distinguishing.)
  let executablePath = H.EXECUTABLE || undefined;
  if (!executablePath) {
    const base = path.join(process.env.LOCALAPPDATA || "", "ms-playwright");
    for (const dir of ["chromium-1237", "chromium-1234", "chromium-1236", "chromium-1235"]) {
      const cand = path.join(base, dir, "chrome-win64", "chrome.exe");
      if (fs.existsSync(cand)) { executablePath = cand; break; }
      const cand2 = path.join(base, dir, "chrome-win", "chrome.exe");
      if (fs.existsSync(cand2)) { executablePath = cand2; break; }
    }
  }
  if (!executablePath) {
    const edge = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
    if (fs.existsSync(edge)) executablePath = edge;
  }
  console.log("=== speaking recording E2E (fake mic) ===");
  console.log("browser:", executablePath || "(playwright default — may not exist)");
  check("a usable browser binary was found", !!executablePath, "no chromium/edge found");

  const browser = await chromium.launch({
    executablePath,
    args: [
      "--use-fake-ui-for-media-stream",
      "--use-fake-device-for-media-stream",
      "--autoplay-policy=no-user-gesture-required",
    ],
  });
  const context = await browser.newContext({ permissions: ["microphone"] });
  const page = await context.newPage();

  try {
    // ── login, seeding BOTH token and user (HARNESS-TOKEN-ONLY-SEED) ──
    await page.goto(APP + "/", { waitUntil: "domcontentloaded" });
    const me = await page.evaluate(async (api) => {
      const r = await fetch(api + "/api/auth/login", {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: "user@gmail.com", password: "123456" }),
      });
      return r.ok ? await r.json() : null;
    }, API);
    check("login as user", !!me, "no session");
    if (!me) { await browser.close(); process.exit(1); }
    await page.evaluate((c) => {
      localStorage.setItem("token", c.token);
      localStorage.setItem("user", JSON.stringify(c));
    }, me);

    // ── fake mic actually produces a stream? ──
    const mic = await page.evaluate(async () => {
      try {
        const s = await navigator.mediaDevices.getUserMedia({ audio: true });
        const tracks = s.getAudioTracks().map(t => t.label);
        s.getTracks().forEach(t => t.stop());
        return { ok: true, tracks };
      } catch (e) { return { ok: false, err: String(e.name) }; }
    });
    check("getUserMedia works with the fake device", mic.ok, JSON.stringify(mic));
    console.log("      fake track label:", (mic.tracks || []).join(",") || "(none)");

    const before = parseInt(sqlScalar("SELECT 'VALUE=' + CAST(COUNT(*) AS varchar(10)) FROM speaking_submissions;"), 10);
    console.log("      submissions before = " + before);

    // ── drive the UI ──
    await page.goto(APP + "/speaking/50007/record", { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    console.log("      landed on:", page.url().replace(APP, ""));

    const clickIf = async (sel) => {
      const el = await page.$(sel);
      if (!el) return false;
      await el.click();
      await page.waitForTimeout(1200);
      return true;
    };

    check("#enable-microphone-button present", await clickIf("#enable-microphone-button"), "button not found");
    check("#start-recording-button present", await clickIf("#start-recording-button"), "button not found");
    await page.waitForTimeout(2500);            // let MediaRecorder accumulate some silence
    check("#stop-recording-button present", await clickIf("#stop-recording-button"), "button not found");

    // ── submit, and capture the network calls it makes ──
    const calls = [];
    const onResp = (res) => {
      const u = res.url();
      if (u.includes(":8080/api")) calls.push(res.request().method() + " " + res.status() + " " + u.replace(API, "").split("?")[0]);
    };
    page.on("response", onResp);
    check("#submit-recording-button present", await clickIf("#submit-recording-button"), "button not found");

    // give upload + assess time to finish
    for (let i = 0; i < 40; i++) {
      await page.waitForTimeout(1500);
      const n = parseInt(sqlScalar("SELECT 'VALUE=' + CAST(COUNT(*) AS varchar(10)) FROM speaking_submissions;"), 10);
      if (n > before) break;
    }
    page.off("response", onResp);
    console.log("      network: " + [...new Set(calls)].join(" | "));

    const after = parseInt(sqlScalar("SELECT 'VALUE=' + CAST(COUNT(*) AS varchar(10)) FROM speaking_submissions;"), 10);
    check("a speaking_submissions row was created", after > before, "before=" + before + " after=" + after);
    console.log("      submissions after  = " + after);

    if (after > before) {
      // PROBE FIX: the first version concatenated six values into one string and my regex
      // only matched the id, so it reported two false FAILs against a row that was in fact
      // correct. Query the fields separately and assert on the values themselves.
      const q = (sql) => sqlScalar(sql);
      const newId = q("SELECT 'VALUE=' + CAST(MAX(id) AS varchar(12)) FROM speaking_submissions;");
      const status = q("SELECT 'VALUE=' + ISNULL(MAX(status),'NULL') FROM speaking_submissions WHERE id = " + newId + ";");
      const keySet = q("SELECT 'VALUE=' + CAST(COUNT(*) AS varchar(4)) FROM speaking_submissions WHERE id = " + newId + " AND media_object_key IS NOT NULL;");
      const mediaType = q("SELECT 'VALUE=' + ISNULL(MAX(media_type),'NULL') FROM speaking_submissions WHERE id = " + newId + ";");
      const assessErr = q("SELECT 'VALUE=' + CASE WHEN MAX(assessment_error) IS NULL THEN 'NULL' ELSE 'set' END FROM speaking_submissions WHERE id = " + newId + ";");
      console.log("      newest row: id=" + newId + " status=" + status + " mediaType=" + mediaType + " assessment_error=" + assessErr);

      check("row has a media object key (upload reached MinIO)", keySet === "1", "keySet=" + keySet);
      check("status is a real terminal value (silence -> FAILED is by design)",
        ["FAILED", "COMPLETED", "GRADED", "SUBMITTED", "PROCESSING", "UPLOADED"].includes(status), "status=" + status);
      // With a SILENT fake mic the designed outcome is FAILED + an assessment_error.
      if (status === "FAILED") {
        check("FAILED row explains itself via assessment_error", assessErr === "set", "assessment_error=" + assessErr);
      }
    }

    console.log("\n=== SUMMARY ===");
    console.log("pass=" + R.pass + " fail=" + R.fail);
    console.log("\nNOTE: this test CREATED a real speaking submission + MinIO object.");
    console.log("      Cleanup is a separate step: sweep/v11/cleanup-speaking-test.sql");
  } catch (e) {
    console.log("ERROR: " + e.message);
    R.fail++;
  } finally {
    await browser.close();
  }
  process.exit(R.fail > 0 ? 1 : 0);
})();
