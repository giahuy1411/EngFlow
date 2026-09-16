/**
 * p13 — error-contract consistency. AGENTS.md states GlobalExceptionHandler
 * returns RFC 7807 ProblemDetail "consistent error contract across the API".
 * p12 surfaced a hint that a 400 from /api/ai/enrich-word parsed as JSON but had
 * NO status/title/detail fields. Measure the actual body shape across a spread of
 * 4xx/5xx responses and report which ones deviate from ProblemDetail.
 *
 * A deviation is only a real finding if a frontend call site reads `.detail` or
 * `.message` from it and would show nothing useful — so record the body verbatim
 * for judgement rather than assuming.
 */
const BASE = "http://localhost:8080";
const { execSync } = require("child_process");

async function login(email, pw) {
  const r = await fetch(BASE + "/api/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password: pw }) });
  const j = await r.json(); return (j.data && j.data.token) || j.token;
}
const flush = () => { try { const k = execSync("docker exec engflow-redis redis-cli --scan --pattern \"rate_limit:*\"", { encoding: "utf8" }).trim(); if (k) execSync("docker exec engflow-redis redis-cli del " + k.split(/\r?\n/).filter(Boolean).map(x => '"' + x + '"').join(" "), { encoding: "utf8" }); } catch (e) { } };

const CASES = [
  ["empty word (validation)", "POST", "/api/ai/enrich-word", "user", { word: "" }],
  ["missing word field", "POST", "/api/ai/enrich-word", "user", {}],
  ["bad JSON body", "POST", "/api/ai/enrich-word", "user", "RAW:{not json"],
  ["nonexistent lesson", "GET", "/api/lessons/999999999", "none", null],
  ["bad enum level", "GET", "/api/lessons?level=NOT_A_LEVEL", "none", null],
  ["nonexistent exercise submit", "POST", "/api/lessons/999999999/exercises/999999999/submit", "user", { answer: "x" }],
  ["unknown route", "GET", "/api/definitely-not-a-route", "none", null],
  ["wrong method on login", "GET", "/api/auth/login", "none", null],
  ["save-vocab empty array", "POST", "/api/ai/save-vocab", "user", []],
  ["vocab search no q", "GET", "/api/vocabulary/search", "none", null],
];

(async () => {
  const user = await login("user@gmail.com", "123456");
  console.log("=== error-contract shape audit (AGENTS.md claims RFC 7807 ProblemDetail) ===\n");
  const out = [];
  for (const [label, method, path, as, body] of CASES) {
    flush();
    const headers = { "Content-Type": "application/json" };
    if (as === "user") headers.Authorization = "Bearer " + user;
    let payload;
    if (body === null) payload = undefined;
    else if (typeof body === "string" && body.startsWith("RAW:")) payload = body.slice(4);
    else payload = JSON.stringify(body);

    let status = 0, text = "";
    try {
      const r = await fetch(BASE + path, { method, headers, body: method === "GET" ? undefined : payload });
      status = r.status; text = await r.text();
    } catch (e) { status = -1; text = e.message; }

    let kind = "non-json";
    let fields = [];
    try {
      const j = JSON.parse(text);
      fields = Object.keys(j).slice(0, 8);
      if (j.title !== undefined && j.status !== undefined) kind = "ProblemDetail";
      else if (j.error !== undefined) kind = "LEGACY{error}";
      else if (j.message !== undefined) kind = "LEGACY{message}";
      else kind = "OTHER";
    } catch (e) { kind = "non-json"; }

    out.push({ label, method, path, status, kind, fields, body: text.slice(0, 180) });
    console.log("  " + label.padEnd(30) + " HTTP " + String(status).padEnd(4) + kind.padEnd(16) + "fields=" + JSON.stringify(fields));
    if (kind !== "ProblemDetail" && status >= 400) {
      console.log("      body: " + text.slice(0, 150));
    }
  }
  require("fs").writeFileSync("p13.json", JSON.stringify(out, null, 1));
  const dev = out.filter(x => x.status >= 400 && x.kind !== "ProblemDetail");
  console.log("\nKHÔNG theo ProblemDetail: " + dev.length + "/" + out.length);
  for (const d of dev) console.log("   - " + d.label + " (" + d.kind + ")");
  console.log("wrote p13.json");
})().catch(e => { console.error("ERR", e); process.exit(1); });
