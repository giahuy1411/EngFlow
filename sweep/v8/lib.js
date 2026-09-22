const BASE = "http://localhost:8080";
const { execSync } = require("child_process");
let adminT = "", userT = "";
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

async function login(email, password) {
  const r = await fetch(BASE + "/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: email, password: password })
  });
  const j = await r.json();
  const d = j.data || {};
  return d.token || j.token || null;
}
async function initTokens() {
  adminT = await login("admin@gmail.com", "123456");
  userT = await login("user@gmail.com", "123456");
  if (!adminT || !userT) throw new Error("login failed admin=" + !!adminT + " user=" + !!userT);
}
function tok(as) {
  if (as === "admin") return adminT;
  if (as === "user") return userT;
  return null;
}
// Sweep harness only: clears its OWN transient rate-limit counters (TTL 60s keys)
// so a 400-probe pass is not distorted by self-inflicted 429s. Never touches
// app data keys. Burst behaviour of the limiter is tested separately in p3.
let since = 0;

/**
 * audit-v8 Round 1 F91/F92: the limiter's tight buckets are now correctly
 * applied to the endpoints that actually do expensive work (:ai 10/min covers
 * ~13 Ollama endpoints, :upload 15/min covers 4 multipart writers). A sweep
 * legitimately fires more than 10 AI probes in a minute, so the harness must
 * clear ITS OWN bucket before every probe that lands in a tight bucket —
 * otherwise the sweep measures its own throttling rather than the app.
 *
 * This mirrors RateLimitFilter's routing. It deletes only rate_limit:* keys,
 * never business data, and it only affects the sweep's own IP bucket.
 */
const TIGHT_BUCKETS = [
  [":auth", (p) => p.startsWith("/api/auth/login") || p.startsWith("/api/auth/register")],
  [":mail", (p) => p.startsWith("/api/auth/forgot-password") || p.startsWith("/api/auth/reset-password")],
  [":ai", (p) => p.startsWith("/api/ai/") || p.startsWith("/api/admin/exercises/ai/")
    || p.startsWith("/api/v1/admin/speaking-prompts/ai-generate")
    || p.startsWith("/api/v1/admin/video-prompts/ai-generate")
    || p.startsWith("/api/v1/admin/video-lessons/translate-transcript")
    || p.startsWith("/api/v1/admin/video-lessons/fetch-youtube")
    || p.includes("/ai-grade")],
  [":upload", (p) => p.startsWith("/api/admin/upload") || p.startsWith("/api/admin/audio-upload")
    || p.startsWith("/api/auth/avatar/upload") || p.startsWith("/api/v1/admin/video-lessons/upload")
    || p.includes("/submissions") || p.includes("/video-attempts")],
  [":order", (p) => p.startsWith("/api/v1/payment/create-order")],
];

function tightBucketFor(path) {
  const p = path.split("?")[0];
  for (const [name, test] of TIGHT_BUCKETS) {
    if (test(p)) return name;
  }
  return null;
}

function flushBucket(bucket) {
  try {
    const found = execSync("docker exec engflow-redis redis-cli --scan --pattern \"rate_limit:*" + bucket + "\"",
      { encoding: "utf8" }).trim();
    if (!found) return;
    const list = found.split(/\r?\n/).filter(Boolean);
    execSync("docker exec engflow-redis redis-cli del " + list.map(k => '"' + k + '"').join(" "),
      { encoding: "utf8" });
  } catch (e) {
    console.log("flushBucket warn: " + e.message);
  }
}

function flushLimits(force) {
  if (!force && ++since < 55) return;
  since = 0;
  try {
    const keys = execSync("docker exec engflow-redis redis-cli --scan --pattern rate_limit:*", { encoding: "utf8" }).trim();
    if (!keys) return;
    const list = keys.split(/\r?\n/).filter(Boolean);
    execSync("docker exec engflow-redis redis-cli del " + list.map(k => '"' + k + '"').join(" "), { encoding: "utf8" });
  } catch (e) {
    console.log("flushLimits warn: " + e.message);
  }
}

const rows = [];
async function probe(name, method, path, as, expect, body) {
  // clear the sweep's own tight bucket so we measure the APP, not our own load
  const tb = tightBucketFor(path);
  if (tb) flushBucket(tb);
  flushLimits(false);
  if (as === "bogus") {
    const headers = { Authorization: "Bearer not.a.real.jwt" };
    const t0 = Date.now();
    const r = await fetch(BASE + path, { method: method, headers: headers });
    const txt = await r.text();
    const ok = (Array.isArray(expect) ? expect : [expect]).indexOf(r.status) >= 0;
    rows.push({ name: name, method: method, path: path, as: as, code: r.status, ms: Date.now() - t0, expect: String(expect), pass: ok, snippet: txt.slice(0, 200) });
    return { code: r.status, ok: ok, txt: txt };
  }
  const headers = {};
  const t = tok(as);
  if (t) headers.Authorization = "Bearer " + t;
  let payload;
  if (body !== undefined && body !== null) {
    headers["Content-Type"] = "application/json";
    payload = typeof body === "string" ? body : JSON.stringify(body);
  }
  const t0 = Date.now();
  let code = 0, txt = "";
  try {
    const r = await fetch(BASE + path, { method: method, headers: headers, body: payload });
    code = r.status;
    txt = await r.text();
  } catch (e) {
    code = -1;
    txt = String(e.message);
  }
  const ms = Date.now() - t0;
  const exp = Array.isArray(expect) ? expect : [expect];
  const ok = exp.indexOf(code) >= 0;
  rows.push({ name: name, method: method, path: path, as: as, code: code, ms: ms, expect: exp.join("/"), pass: ok, snippet: txt.slice(0, 200) });
  return { code: code, ok: ok, txt: txt, ms: ms };
}
function report(title) {
  const bad = rows.filter(r => !r.pass);
  console.log("=== " + title + " total=" + rows.length + " FAIL=" + bad.length);
  for (const r of bad) console.log("  FAIL " + r.name + " | " + r.method + " " + r.path + " as=" + r.as + " got=" + r.code + " want=" + r.expect + " :: " + r.snippet.replace(/\r?\n/g, " "));
  const slow = rows.slice().sort((a, b) => b.ms - a.ms).slice(0, 15);
  console.log("--- slowest ---");
  for (const r of slow) console.log("  " + String(r.ms).padStart(6) + "ms  " + r.name + " " + r.method + " " + r.path);
}
function dump(file) {
  require("fs").writeFileSync(file, JSON.stringify(rows, null, 1));
  console.log("wrote " + file);
}
module.exports = { initTokens, login, probe, report, dump, sleep, flushLimits, flushBucket, tightBucketFor, rows: rows, getAdmin: () => adminT, getUser: () => userT, BASE };
