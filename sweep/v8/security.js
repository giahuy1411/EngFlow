/**
 * security.js - Task 12 evidence: authorization matrix, error-leakage scan,
 * AI-output-safety probe, and real-browser XSS execution proof.
 * Read-only against the app (the one write is a temporary vocab row, cleaned up).
 *
 * The point is to test the boundaries the audit prompt names explicitly:
 *   - every admin route must reject user and anon with 401/403 (never 200)
 *   - no response body may leak a stack trace, SQL, or internal path
 *   - AI output is untrusted: it must not be echoed as executable markup
 *   - expired/garbage tokens must be rejected, not silently accepted
 */
const H = require("./lib.js");
const fs = require("fs");

// Root lib.js exports BASE (the API) but not the SPA origin; declare it here
// rather than silently passing `undefined` into page.goto (which produced
// "Cannot navigate to invalid URL: undefined/login" in an earlier revision).
const APP = "http://localhost:5173";

// Admin-only surfaces. Kept as explicit method+path so a route that silently
// becomes public shows up as a failure instead of being skipped.
// Paths verified against the real controller mappings:
//   - AdminController  @RequestMapping("/api/admin")  -> /api/admin/stats
//   - DashboardController @RequestMapping("/api/dashboard") -> /api/dashboard/stats,
//     which is the USER self-stats endpoint (service keys off authentication.getName()
//     and returns only that user's own points/streak). It is therefore correctly
//     200 for a normal user and must NOT be asserted as admin-only; it lives in
//     the USER_SELF list below.
const ADMIN = [
  ["GET", "/api/admin/stats"],
  ["GET", "/api/admin/lessons?page=0&size=5"],
  ["GET", "/api/admin/lessons/445/structure"],
  ["GET", "/api/admin/exercises?page=0&size=5"],
  ["GET", "/api/admin/users?page=0&size=5"],
  ["POST", "/api/admin/lessons", { title: "x", level: "A1", content: "x" }],
  ["DELETE", "/api/admin/lessons/99999999"],
  ["GET", "/api/admin/exercises/ai/status"],
  ["GET", "/api/v1/admin/speaking-submissions?page=0&size=5"],
  ["GET", "/api/v1/admin/video-lessons"],
];

// Authenticated-but-not-admin: must be 200 for a normal user, 401 for anon.
// These are self-scoped reads; calling them admin-only would be a false finding.
const USER_SELF = [
  ["GET", "/api/dashboard/stats"],
  // AuthController @GetMapping("/me") under @RequestMapping("/api/auth").
  // There is no /api/profile endpoint; that guess returned 404 and would have
  // been reported as a broken self-scoped surface.
  ["GET", "/api/auth/me"],
];

// Must stay reachable without a token (SecurityConfig permitAll).
const PUBLIC = [
  ["GET", "/api/v1/video-lessons"],
  ["GET", "/api/v1/video-lessons/1"],
  ["GET", "/api/vocabulary/search?keyword=work"],
  ["POST", "/api/auth/login", { email: "nobody@example.com", password: "wrong" }],
];

const LEAK = [
  /at com\.datn\.engflow/i,
  /java\.lang\.\w+Exception/,
  /org\.springframework\.\w+\.\w+Exception/,
  /SELECT .+ FROM .+/i,
  /INSERT INTO .+/i,
  /C:\\Users\\ASUS/i,
  /\/app\/src\/main/i,
  /BOOT-INF/i,
  /hibernate/i,
  /stacktrace/i,
];

(async () => {
  const admin = await H.login("admin@gmail.com", "123456");
  const user = await H.login("user@gmail.com", "123456");

  const out = { adminMatrix: [], userSelfMatrix: [], publicMatrix: [], leaks: [], tokenTests: [], aiSafety: [] };
  let fails = 0;

  async function call(method, path, token, body) {
    H.flushLimits(true);
    const r = await fetch(H.BASE + path, {
      method,
      headers: {
        ...(body ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: "Bearer " + token } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    });
    let text = "";
    try { text = await r.text(); } catch (e) {}
    return { status: r.status, text };
  }

  console.log("=== [1] ADMIN SURFACE x ROLE (admin=2xx/4xx-business, user/anon=401|403) ===");
  for (const [method, path, body] of ADMIN) {
    const a = await call(method, path, admin, body);
    const u = await call(method, path, user, body);
    const n = await call(method, path, null, body);
    const userOk = u.status === 401 || u.status === 403;
    const anonOk = n.status === 401 || n.status === 403;
    // A GET must actually succeed for the admin, otherwise the "role-gated"
    // pass is meaningless (the URL might not exist at all). Write methods may
    // legitimately answer 400 (validation) or 404 (absent target).
    const adminOk = method === "GET" ? a.status === 200 : (a.status < 500 && a.status !== 401 && a.status !== 403);
    const ok = userOk && anonOk && adminOk;
    if (!ok) fails++;
    console.log((ok ? "  OK   " : "  FAIL ") + (method + " " + path).padEnd(52)
      + " admin=" + a.status + " user=" + u.status + " anon=" + n.status);
    out.adminMatrix.push({ method, path, admin: a.status, user: u.status, anon: n.status, ok });

    for (const [role, res] of [["admin", a], ["user", u], ["anon", n]]) {
      for (const re of LEAK) {
        if (re.test(res.text)) {
          out.leaks.push({ path, role, pattern: String(re), sample: res.text.slice(0, 200) });
          console.log("    LEAK " + role + " " + path + " matched " + re);
        }
      }
    }
  }

  console.log("\n=== [1b] AUTHENTICATED SELF-SCOPED (200 for user, 401 for anon) ===");
  for (const [method, path, body] of USER_SELF) {
    const u = await call(method, path, user, body);
    const n = await call(method, path, null, body);
    const ok = u.status === 200 && n.status === 401;
    if (!ok) fails++;
    console.log((ok ? "  OK   " : "  FAIL ") + (method + " " + path).padEnd(52)
      + " user=" + u.status + " anon=" + n.status);
    out.userSelfMatrix.push({ method, path, user: u.status, anon: n.status, ok });
    for (const re of LEAK) {
      if (re.test(u.text)) {
        out.leaks.push({ path, role: "user", pattern: String(re), sample: u.text.slice(0, 200) });
        console.log("    LEAK user " + path + " matched " + re);
      }
    }
  }

  console.log("\n=== [2] PUBLIC SURFACE (must NOT be 401/403) ===");
  for (const [method, path, body] of PUBLIC) {
    const n = await call(method, path, null, body);
    const ok = n.status !== 401 && n.status !== 403;
    if (!ok) fails++;
    console.log((ok ? "  OK   " : "  FAIL ") + (method + " " + path).padEnd(52) + " anon=" + n.status);
    out.publicMatrix.push({ method, path, anon: n.status, ok });
    for (const re of LEAK) {
      if (re.test(n.text)) {
        out.leaks.push({ path, role: "anon", pattern: String(re), sample: n.text.slice(0, 200) });
        console.log("    LEAK anon " + path + " matched " + re);
      }
    }
  }

  console.log("\n=== [3] TOKEN HANDLING ===");
  const tokenCases = [
    ["no token", null],
    ["garbage token", "not.a.jwt"],
    ["well-formed but wrong signature", admin.slice(0, -6) + "AAAAAA"],
    ["empty bearer", ""],
  ];
  for (const [label, tok] of tokenCases) {
    const r = await call("GET", "/api/admin/dashboard/stats", tok);
    const ok = r.status === 401 || r.status === 403;
    if (!ok) fails++;
    console.log((ok ? "  OK   " : "  FAIL ") + label.padEnd(40) + " -> " + r.status);
    out.tokenTests.push({ label, status: r.status, ok });
  }

  console.log("\n=== [4] AI OUTPUT — raw-tag echo in API response (OBSERVATION, not a verdict) ===");
  // IMPORTANT: this section is deliberately INFORMATIONAL and does not increment
  // `fails`. Reflecting a raw tag inside a JSON string is not a defect: JSON does
  // not escape '<' or '>', and the value is inert until something renders it as
  // markup. The local model echoes a raw tag nondeterministically (measured: 1 of
  // 3 runs for "<svg/onload=alert(1)>"), so scoring it pass/fail here would make
  // the whole harness flaky.
  //
  // The authoritative verdict belongs to sweep/v8/xss-prove.js, which proves the
  // payload cannot execute: it asserts the real sanitizeText allowlist neutralises
  // every payload AND that a payload driven through save-vocab into a rendered
  // deck page does not run in Chromium. Measured 2026-09-16: PASS on all three.
  //
  // Two earlier revisions of this file were wrong in opposite directions: the
  // first matched a bare "onload=" substring and reported a false positive on
  // {"word":"svg/onload=alert(1)"} (brackets already stripped by the model); the
  // second counted a genuine raw-tag echo as a hard failure. Neither is evidence.
  const payloads = [
    '<script>alert(1)</script>',
    '"><img src=x onerror=alert(1)>',
    "'; DROP TABLE exercises;--",
    "<svg/onload=alert(1)>",
    '<iframe src="javascript:alert(1)">',
  ];
  const TAG = /<\s*(script|svg|img|iframe|object|embed|body|details|marquee)\b/i;
  let rawEchoCount = 0;
  for (const p of payloads) {
    const r = await call("POST", "/api/ai/enrich-word", user, { word: p });
    const tagReflected = TAG.test(r.text);
    if (tagReflected) rawEchoCount++;
    console.log("  " + (tagReflected ? "NOTE " : "OK   ") + ("payload " + JSON.stringify(p).slice(0, 34)).padEnd(42)
      + " status=" + r.status + " rawTagInResponse=" + tagReflected);
    out.aiSafety.push({ payload: p, status: r.status, rawTagInResponse: tagReflected, sample: r.text.slice(0, 200) });
  }
  console.log("  raw-tag echoes: " + rawEchoCount + "/" + payloads.length
    + "  (verdict owned by xss-prove.js, not counted as a failure here)");

  // (b) Real execution proof lives in sweep/v8/xss-prove.js.
  // It was originally inlined here, but it saved whatever noun the model
  // extracted from the payload into `vocabulary` (4 rows of word='img' on
  // 2026-09-16), i.e. this read-only audit script was mutating business data.
  // xss-prove.js owns that write, proves non-execution in a real browser, and
  // its rows are removed by sweep/v8/clean-xss-probe.sql. Section [4] above is
  // the response-shape check and stays read-only on purpose.
  console.log("\n=== [5] AI OUTPUT XSS (browser execution proof) ===");
  console.log("  -> moved to sweep/v8/xss-prove.js (see xss-prove.json);");
  console.log("     it writes test rows and cleans them via clean-xss-probe.sql.");
  out.aiXss = { delegatedTo: "sweep/v8/xss-prove.js" };

  console.log("\n=== SECURITY SUMMARY ===");
  console.log("failures        : " + fails);
  console.log("leak matches    : " + out.leaks.length);
  console.log("admin surfaces  : " + out.adminMatrix.length + " (all role-gated: " + out.adminMatrix.every(x => x.ok) + ")");
  console.log("user-self       : " + out.userSelfMatrix.length + " (all self-scoped: " + out.userSelfMatrix.every(x => x.ok) + ")");
  console.log("public surfaces : " + out.publicMatrix.length + " (all reachable: " + out.publicMatrix.every(x => x.ok) + ")");

  fs.writeFileSync("security.json", JSON.stringify({ fails, ...out }, null, 1));
  console.log("wrote security.json");
})().catch(e => { console.log("ERR", e.message); process.exit(1); });
