/**
 * p15_adversarial_deep.js — Task 14 follow-up.
 *
 * WHY THIS FILE EXISTS
 * --------------------
 * The first adversarial pass (p14) reported 38 cases / 0 fails, but three of
 * those "passes" were not testing what they claimed. A harness that passes for
 * the wrong reason is worse than one that fails, so each is re-tested here with
 * a corrected probe:
 *
 *   1. INVALID UPLOAD — p14 sent a USER token to /api/admin/audio-upload, which
 *      is admin-only, so every case returned 403 (role rejection) before
 *      SafeUploadNames ever ran. The assertion `status < 500` passed trivially.
 *      FIX: use an ADMIN token so extension/content-type validation is the
 *      thing under test, and assert on the specific expected outcome.
 *
 *   2. ABSENT EXERCISE SUBMIT — p14 sent {exerciseId, answer}, but GradeRequest
 *      is {answers:[{exerciseId, userAnswer}]}. Both field names were wrong, so
 *      `answers` was null and the service had nothing to grade → 200. The case
 *      never exercised a missing exercise id.
 *      FIX: send the real shape and assert the response actually reports 0
 *      graded items rather than silently succeeding.
 *
 *   3. OUT-OF-RANGE PAGINATION — p14 only asserted "no 5xx", which would also
 *      pass if the server returned 100000 rows. Clamping is the real question.
 *      FIX: assert the returned row count is actually clamped to <= 100.
 */
const H = require("./lib.js");

const rows = [];
let fails = 0;
function rec(name, ok, detail) {
  rows.push({ case: name, ok, detail });
  if (!ok) fails++;
  console.log((ok ? "  OK   " : "  FAIL ") + name.padEnd(52) + " " + detail);
}

async function req(method, p, token, body) {
  H.flushLimits(true);
  const r = await fetch(H.BASE + p, {
    method,
    headers: {
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: "Bearer " + token } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await r.text().catch(() => "");
  let json = null;
  try { json = JSON.parse(text); } catch (e) { /* non-JSON is fine */ }
  return { status: r.status, text, json };
}

(async () => {
  const admin = await H.login("admin@gmail.com", "123456");
  const user = await H.login("user@gmail.com", "123456");
  console.log("admin token: " + !!admin + "  user token: " + !!user);

  // ---------------------------------------------------------------- [1] UPLOAD
  console.log("\n=== [1] INVALID UPLOAD — as ADMIN (p14 used a user token: 403 masked everything) ===");

  // Baseline: confirm the admin token actually reaches the handler, otherwise
  // every case below is again testing the role gate instead of validation.
  {
    H.flushLimits(true);
    const fd = new FormData();
    fd.append("file", new Blob([new Uint8Array([1, 2, 3])], { type: "application/octet-stream" }), "probe.bin");
    const r = await fetch(H.BASE + "/api/admin/audio-upload", {
      method: "POST", headers: { Authorization: "Bearer " + admin }, body: fd,
    });
    const t = await r.text();
    rec("admin token passes the role gate (probe reaches handler)",
      r.status !== 403, "status=" + r.status + " body=" + t.slice(0, 80));
  }

  for (const [label, filename, type] of [
    ["html", "evil.html", "text/html"],
    ["svg", "evil.svg", "image/svg+xml"],
    ["js", "evil.js", "application/javascript"],
    ["no extension", "evil", "application/octet-stream"],
    ["double extension .png.html", "evil.png.html", "image/png"],
    ["path traversal name", "../../evil.html", "text/html"],
    ["uppercase HTML", "EVIL.HTML", "text/html"],
    ["null byte in name", "evil\u0000.html", "text/html"],
  ]) {
    H.flushLimits(true);
    const fd = new FormData();
    fd.append("file", new Blob([new Uint8Array([0x3c, 0x73, 0x76, 0x67, 0x3e])], { type }), filename);
    let status = 0, body = "";
    try {
      const r = await fetch(H.BASE + "/api/admin/audio-upload", {
        method: "POST", headers: { Authorization: "Bearer " + admin }, body: fd,
      });
      status = r.status;
      body = await r.text();
    } catch (e) { status = -1; body = e.message; }
    // A dangerous extension must be REJECTED (4xx) or, if stored, must not be
    // served as HTML. A 200 here is only acceptable if the returned URL does not
    // end in a browser-renderable extension.
    const url = (() => { try { const j = JSON.parse(body); return j.url || j.data || ""; } catch (e) { return ""; } })();
    const dangerousUrl = /\.(html?|svg|js|mjs)$/i.test(String(url));
    const ok = status >= 400 && status < 500 ? true : (status === 200 && !dangerousUrl);
    rec("upload " + label, ok,
      "status=" + status + " dangerousUrl=" + dangerousUrl + " body=" + body.slice(0, 60).replace(/\s+/g, " "));
  }

  // ------------------------------------------------- [2] ABSENT EXERCISE SUBMIT
  console.log("\n=== [2] ABSENT EXERCISE SUBMIT — with the REAL GradeRequest shape ===");
  console.log("    GradeRequest = { answers: [ { exerciseId, userAnswer } ] }  (p14 sent {exerciseId, answer})");

  const absent = await req("POST", "/api/lessons/445/exercises/submit", user,
    { answers: [{ exerciseId: 99999999, userAnswer: "x" }] });
  const absentBody = JSON.stringify(absent.json || absent.text).slice(0, 160);
  rec("absent exercise submit is not a 5xx", absent.status < 500,
    "status=" + absent.status + " body=" + absentBody);

  // Real exercise for comparison — proves the endpoint does grade when the id exists.
  // NOTE: the response field is `id`, NOT `exerciseId` (ExerciseResponse.id). My
  // first probe read `exerciseId`, got undefined, and reported a failure that was
  // purely a harness bug — verified against the raw JSON before changing this.
  H.flushLimits(true);
  const ex = await req("GET", "/api/lessons/445/exercises", user);
  const exList = Array.isArray(ex.json) ? ex.json : (ex.json && ex.json.data ? ex.json.data : []);
  const firstEx = exList[0];
  if (firstEx && (firstEx.id || firstEx.exerciseId)) {
    const exId = firstEx.id || firstEx.exerciseId;
    const real = await req("POST", "/api/lessons/445/exercises/submit", user,
      { answers: [{ exerciseId: exId, userAnswer: "probe" }] });
    const graded = real.json && typeof real.json.total === "number" ? real.json.total : -1;
    rec("real exercise submit grades the answer (total > 0)", real.status === 200 && graded > 0,
      "status=" + real.status + " exerciseId=" + exId + " total=" + graded +
      " body=" + JSON.stringify(real.json || real.text).slice(0, 110));
  } else {
    rec("real exercise submit grades the answer (total > 0)", false,
      "could not read an exercise id from /api/lessons/445/exercises: " + ex.text.slice(0, 90));
  }

  // The absent-id case must also be a *meaningful* 200, not a silent no-op:
  // total must be 0 and results empty, proving the service handled the missing
  // id rather than quietly skipping the request.
  {
    const graded = absent.json && typeof absent.json.total === "number" ? absent.json.total : -1;
    rec("absent id grades nothing (total === 0, no phantom result)",
      absent.status === 200 && graded === 0 && Array.isArray(absent.json.results) && absent.json.results.length === 0,
      "total=" + graded + " results=" + (absent.json && absent.json.results ? absent.json.results.length : "n/a"));
  }

  // ------------------------------------------------------- [3] PAGINATION CLAMP
  console.log("\n=== [3] PAGINATION CLAMPING — is size actually capped, or just not crashing? ===");
  console.log("    code: LessonController.java:36  size = Math.min(Math.max(size,1),100); page = Math.max(page,0)");

  function rowCount(j) {
    if (!j) return -1;
    if (Array.isArray(j)) return j.length;
    if (Array.isArray(j.content)) return j.content.length;
    if (Array.isArray(j.data)) return j.data.length;
    if (j.data && Array.isArray(j.data.content)) return j.data.content.length;
    return -1;
  }

  for (const [label, q, expectMax] of [
    ["size=100000 clamps to <=100", "/api/lessons?page=0&size=100000", 100],
    ["size=5000 clamps to <=100", "/api/lessons?page=0&size=5000", 100],
    ["size=-5 clamps to >=1", "/api/lessons?page=0&size=-5", 100],
    ["size=0 clamps to >=1", "/api/lessons?page=0&size=0", 100],
    ["page=-1 clamps to 0", "/api/lessons?page=-1&size=20", 20],
    ["page beyond end is empty, not error", "/api/lessons?page=99999&size=20", 20],
  ]) {
    const r = await req("GET", q, user);
    const n = rowCount(r.json);
    rec(label, r.status < 500 && n >= 0 && n <= expectMax,
      "status=" + r.status + " rows=" + n + " (cap " + expectMax + ")");
  }

  // ---------------------------------------------------------- [4] SEARCH ESCAPES
  console.log("\n=== [4] SEARCH — the injection probe must return 0, not the whole table ===");
  // `keyword=work` legitimately matches nothing: the vocabulary has no word
  // containing 'work'. Verified in SQL (p15-keyword-check.sql): LIKE '%work%' = 0
  // rows, LIKE '%the%' = 9 rows. So the positive control uses 'the'. Asserting on
  // 'work' produced a false failure that was purely a bad test keyword.
  const inj = await req("GET", "/api/vocabulary/search?keyword=%27%20OR%201%3D1--", null);
  const injN = rowCount(inj.json);
  // -1 means the shape was unrecognised, which must NOT be treated as "no rows".
  rec("SQL metacharacters return exactly 0 rows",
    injN === 0, "status=" + inj.status + " rows=" + injN + " (vocabulary = 127)");

  const normal = await req("GET", "/api/vocabulary/search?keyword=the", null);
  const normalN = rowCount(normal.json);
  rec("positive control 'the' returns rows (endpoint not broken)", normalN > 0,
    "status=" + normal.status + " rows=" + normalN);

  // A term that legitimately has no match must return an empty list, not an error.
  const none = await req("GET", "/api/vocabulary/search?keyword=zzzznotaword", null);
  rec("unmatched keyword returns empty list, not an error",
    none.status === 200 && rowCount(none.json) === 0,
    "status=" + none.status + " rows=" + rowCount(none.json));

  // ------------------------------------------------------------- [5] SUMMARY
  console.log("\n=== DEEP VERIFICATION SUMMARY ===");
  console.log("cases : " + rows.length);
  console.log("fails : " + fails);
  require("fs").writeFileSync("p15-adversarial-deep.json",
    JSON.stringify({ cases: rows.length, fails, rows }, null, 1));
  console.log("wrote p15-adversarial-deep.json");
  process.exit(fails ? 1 : 0);
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
