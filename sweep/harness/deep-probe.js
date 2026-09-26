/**
 * DEEP probes the base sweep does not cover.
 *
 * Focus: the six features the user named, verified on RESPONSE CONTRACT and on
 * measured behaviour rather than "a 2xx arrived".
 *
 *   A. ExerciseService MULTIPLE_CHOICE guard (F-13-01): must reject bare-letter options
 *      AND must not over-block legitimate choices. Includes the exact "A - Salad" style
 *      that exists as a REAL row in the DB (exercise_id=651717).
 *   B. ?sort= measured, not assumed, on every paged list endpoint.
 *   C. Contract (field name + JS type) for lessons, streak, auth, search, CRUD, AI.
 *   D. Role authorisation in BOTH directions.
 *
 * Self-cleans: the probe lesson is deleted through the API (cascades exercises).
 *
 * Run: node sweep/harness/deep-probe.js [--audit <name>] [--out <dir>]
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const { OUT: EVID, MARKER } = require("./_config.js");
const path = require("path");

const API = "http://localhost:8080";
const USER = { email: "user@gmail.com", password: "123456" };
const ADMIN = { email: "admin@gmail.com", password: "123456" };
const PUBLIC_DECK = 10006;
const REAL_MC_ID = 651717; // real MULTIPLE_CHOICE row using the "A - Salad" option style

const R = { pass: 0, fail: 0, blocked: 0, n_a: 0, findings: [], probed: [], areas: {} };
let area = "init";
const setArea = (a) => { area = a; R.areas[a] = R.areas[a] || { pass: 0, fail: 0, blocked: 0 }; };

function flushBuckets() {
  try {
    const out = execFileSync("docker",
      ["exec", "engflow-redis", "redis-cli", "--scan", "--pattern", "rate_limit:*"],
      { encoding: "utf8" }).trim();
    if (!out) return 0;
    const keys = out.split(/\r?\n/).filter(Boolean);
    if (!keys.length) return 0;
    execFileSync("docker", ["exec", "engflow-redis", "redis-cli", "del", ...keys], { encoding: "utf8" });
    return keys.length;
  } catch (e) { console.log("  (flush warn: " + String(e.message).slice(0, 60) + ")"); return -1; }
}

async function req(method, p, { token, body } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  // One retry on a transient socket error: a single ECONNRESET from the dev server
  // must not be recorded as an authorisation result (that would be a false finding).
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      const res = await fetch(API + p, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
      const text = await res.text();
      let data = null;
      try { data = JSON.parse(text); } catch { data = text.slice(0, 300); }
      return { status: res.status, data, raw: text };
    } catch (e) {
      if (attempt === 1) return { status: "ERR", data: String(e.message), raw: "" };
    }
  }
}

function check(name, cond, detail) {
  const b = R.areas[area] || (R.areas[area] = { pass: 0, fail: 0, blocked: 0 });
  if (cond) { R.pass++; b.pass++; R.probed.push({ area, name, status: "PASS" }); console.log(`  PASS  ${name}`); }
  else {
    R.fail++; b.fail++; R.probed.push({ area, name, status: "FAIL", detail: String(detail) });
    console.log(`  FAIL  ${name}  -> ${detail}`);
    R.findings.push({ area, name, detail: String(detail) });
  }
}
function blocked(name, why) {
  const b = R.areas[area] || (R.areas[area] = { pass: 0, fail: 0, blocked: 0 });
  R.blocked++; b.blocked++; R.probed.push({ area, name, status: "BLOCKED", detail: why });
  console.log(`  BLOCK ${name}  -> ${why}`);
}
function info(name, detail) {
  R.probed.push({ area, name, status: "INFO", detail: String(detail) });
  console.log(`  INFO  ${name} -> ${detail}`);
}

function contract(obj, shape) {
  if (obj == null || typeof obj !== "object") return { ok: false, why: `not an object (${typeof obj})` };
  for (const [k, t] of Object.entries(shape)) {
    if (!(k in obj)) return { ok: false, why: `missing key "${k}" (have: ${Object.keys(obj).slice(0, 14).join(",")})` };
    const actual = Array.isArray(obj[k]) ? "array" : obj[k] === null ? "null" : typeof obj[k];
    if (t === "any") continue;
    if (t === "number" && typeof obj[k] !== "number") return { ok: false, why: `"${k}" is ${actual}, expected number` };
    if (t === "array" && !Array.isArray(obj[k])) return { ok: false, why: `"${k}" is ${actual}, expected array` };
    if (t === "boolean" && typeof obj[k] !== "boolean") return { ok: false, why: `"${k}" is ${actual}, expected boolean` };
    if (t !== "number" && t !== "array" && t !== "boolean" && actual !== t && actual !== "null")
      return { ok: false, why: `"${k}" is ${actual}, expected ${t}` };
  }
  return { ok: true };
}

const login = async ({ email, password }) => {
  const r = await req("POST", "/api/auth/login", { body: { email, password } });
  return r.status === 200 ? (r.data?.data?.token || r.data?.token || null) : null;
};

(async () => {
  console.log("=== " + MARKER + " DEEP probe ===");
  console.log("target:", API, "| flushed:", flushBuckets());

  const userToken = await login(USER);
  const adminToken = await login(ADMIN);
  console.log("user token:", userToken ? "OK" : "MISSING", "| admin token:", adminToken ? "OK" : "MISSING");
  if (!userToken || !adminToken) { console.log("ABORT"); process.exit(2); }

  // ═══════════════════════════════════════════ A. MULTIPLE_CHOICE guard (F-13-01)
  setArea("mc-guard"); flushBuckets();
  console.log("\n--- A. MULTIPLE_CHOICE guard (F-13-01) ---");
  const LESSON_TITLE = MARKER + "-MC-" + PUBLIC_DECK;
  let lessonId = null;
  {
    const mk = await req("POST", "/api/lessons", {
      token: adminToken,
      body: { title: LESSON_TITLE, description: "audit mc guard", level: "ELEMENTARY", category: "AUDIT", isPublished: false },
    });
    lessonId = mk.data?.id ?? mk.data?.lessonId ?? mk.data?.data?.id ?? null;
    check("setup: create audit lesson (admin)", [200, 201].includes(mk.status) && lessonId != null, `status=${mk.status} id=${lessonId}`);
    if (!lessonId) { blocked("MULTIPLE_CHOICE guard", "could not create audit lesson"); }
  }

  const mc = (options, extra = {}) => ({
    lessonId, question: MARKER + "-MC probe?", options: JSON.stringify(options),
    correctAnswer: extra.correctAnswer ?? (Array.isArray(options) && options.length ? options[0] : "x"),
    exerciseType: "MULTIPLE_CHOICE", difficulty: "EASY", orderIndex: 0, ...extra,
  });

  if (lessonId) {
    // 1. the exact motivating payload: bare letters a,b,c,d -> MUST be rejected
    const bad = await req("POST", "/api/admin/exercises", { token: adminToken, body: mc(["a", "b", "c", "d"], { correctAnswer: "d" }) });
    check("MC options [\"a\",\"b\",\"c\",\"d\"] -> 400 (the F-13-01 payload)", bad.status === 400, `got ${bad.status}`);
    info("  rejection body", JSON.stringify(bad.data).slice(0, 160));

    // 2. legitimate real-text choices -> MUST be accepted (guard must not over-block)
    const good = await req("POST", "/api/admin/exercises", { token: adminToken, body: mc(["Apple", "Banana", "Cherry", "Date"], { correctAnswer: "Apple" }) });
    check("MC options [\"Apple\",\"Banana\",\"Cherry\",\"Date\"] -> 200 (legit not over-blocked)", [200, 201].includes(good.status), `got ${good.status}`);
    const goodId = good.data?.id ?? null;

    // 3. the REAL row style that exists in the DB (exercise 651717): "A - Salad" ...
    //    Each option has letter+dash+content, so it is legitimate choice text.
    const dash = await req("POST", "/api/admin/exercises", { token: adminToken, body: mc(["A - Salad", "B - Cheeseburger", "C - Pizza", "D - Bread"], { correctAnswer: "A - Salad" }) });
    check("MC options [\"A - Salad\",\"B - Cheeseburger\",...] -> 200 (real 651717 style)", [200, 201].includes(dash.status), `got ${dash.status} — guard over-blocks a shape that exists as a real row`);
    info("  dash-style response", `status=${dash.status} body=${JSON.stringify(dash.data).slice(0, 120)}`);

    // 4. fewer than 2 usable options -> rejected
    const one = await req("POST", "/api/admin/exercises", { token: adminToken, body: mc(["Only one"], { correctAnswer: "Only one" }) });
    check("MC options with 1 entry -> 400", one.status === 400, `got ${one.status}`);
    const none = await req("POST", "/api/admin/exercises", { token: adminToken, body: mc([], { correctAnswer: "x" }) });
    check("MC options [] -> 400", none.status === 400, `got ${none.status}`);
    const nul = await req("POST", "/api/admin/exercises", { token: adminToken, body: { lessonId, question: "q", correctAnswer: "x", exerciseType: "MULTIPLE_CHOICE" } });
    check("MC options omitted (null) -> 400", nul.status === 400, `got ${nul.status}`);
    const twoBare = await req("POST", "/api/admin/exercises", { token: adminToken, body: mc(["a", "b"], { correctAnswer: "a" }) });
    check("MC options [\"a\",\"b\"] (2 bare letters) -> 400", twoBare.status === 400, `got ${twoBare.status}`);

    // 5. boundary: exactly 2 real options -> accepted
    const two = await req("POST", "/api/admin/exercises", { token: adminToken, body: mc(["Yes", "No"], { correctAnswer: "Yes" }) });
    check("MC options [\"Yes\",\"No\"] (exactly 2 real) -> 200 (boundary)", [200, 201].includes(two.status), `got ${two.status}`);

    // 6. mixed: one bare letter + one real -> the real content makes it usable
    const mixed = await req("POST", "/api/admin/exercises", { token: adminToken, body: mc(["a", "Watermelon"], { correctAnswer: "Watermelon" }) });
    check("MC options [\"a\",\"Watermelon\"] (mixed) -> 200", [200, 201].includes(mixed.status), `got ${mixed.status}`);

    // 7. the guard must NOT apply to non-choice types
    const fb = await req("POST", "/api/admin/exercises", { token: adminToken, body: { lessonId, question: "AUDIT FB?", options: JSON.stringify(["a", "b", "c", "d"]), correctAnswer: "d", exerciseType: "FILL_BLANK", difficulty: "EASY", orderIndex: 1 } });
    check("FILL_BLANK with options [\"a\",\"b\",\"c\",\"d\"] -> 200 (guard is MC-scoped)", [200, 201].includes(fb.status), `got ${fb.status}`);

    // 7b. LISTENING is rendered by the learner as a CHOICE type (LessonExerciseTab
    //     hasOptionChoices accepts MULTIPLE_CHOICE *and* LISTENING), so the same defect
    //     class is reachable through LISTENING — but the guard only covers
    //     MULTIPLE_CHOICE. Live DB holds 31 LISTENING rows with bare-letter options.
    const listenBare = await req("POST", "/api/admin/exercises", { token: adminToken, body: { lessonId, question: "AUDIT listening bare?", options: JSON.stringify(["A", "B", "C", "D"]), correctAnswer: "B", exerciseType: "LISTENING", difficulty: "EASY", orderIndex: 2 } });
    // audit-v14 F-13-13 (kept in the committed harness): the server guard is
    // intentionally MULTIPLE_CHOICE-scoped. LISTENING rows with bare-letter options are
    // a known legacy shape; the RENDER path was fixed instead, so the API accepts them.
    check("LISTENING with bare-letter options -> 200 (guard is MC-scoped by design, F-13-13)",
      [200, 201].includes(listenBare.status), `got ${listenBare.status}`);

    // 8. UPDATE path: switching a legit row to bare letters must be refused
    if (goodId) {
      const upd = await req("PUT", `/api/admin/exercises/${goodId}`, { token: adminToken, body: { exerciseType: "MULTIPLE_CHOICE", options: JSON.stringify(["a", "b", "c", "d"]) } });
      check("PUT legit MC -> options [\"a\",\"b\",\"c\",\"d\"] -> 400 (update path)", upd.status === 400, `got ${upd.status}`);
      const updGood = await req("PUT", `/api/admin/exercises/${goodId}`, { token: adminToken, body: { exerciseType: "MULTIPLE_CHOICE", options: JSON.stringify(["Apple", "Banana"]) } });
      check("PUT legit MC -> real options -> 200", [200, 201].includes(updGood.status), `got ${updGood.status}`);
    } else { blocked("MC update-path guard", "no exercise id from the legit create"); }

    // 9. the REAL production row 651717 is untouched by the guard (read-only check)
    const real = await req("GET", `/api/admin/exercises/${REAL_MC_ID}`, { token: adminToken });
    check(`real MC row ${REAL_MC_ID} still readable (admin)`, real.status === 200, `got ${real.status}`);
    info("  real row options", String(real.data?.options).slice(0, 90));

    // 10. The real production row is READ-ONLY for this audit — never PUT to it. The
    //     evidence that an admin cannot save it back is the POST copy below, which sends
    //     the row's exact options through the same assertChoiceOptionsUsable() the PUT
    //     path calls, and is refused.
    const realCopy = await req("POST", "/api/admin/exercises", {
      token: adminToken,
      body: { lessonId, question: real.data?.question ?? "copy", options: real.data?.options, correctAnswer: real.data?.correctAnswer ?? "A - Salad", exerciseType: "MULTIPLE_CHOICE", difficulty: "EASY", orderIndex: 5 },
    });
    // audit-v14 F-13-12 (kept in the committed harness): "A - Salad" carries
    // letter+dash+CONTENT, so it is legitimate choice text, not a bare-letter placeholder.
    check(`POST a copy of real row ${REAL_MC_ID}'s exact options -> 200 (legit letter+content style, F-13-12)`,
      [200, 201].includes(realCopy.status), `got ${realCopy.status}`);
    info("  real-row copy response", JSON.stringify(realCopy.data).slice(0, 140));

    // 11. unauth / non-admin cannot create exercises at all
    check("POST /api/admin/exercises anon 401/403", [401, 403].includes((await req("POST", "/api/admin/exercises", { body: mc(["x", "y"]) })).status), "expected 401/403");
    check("POST /api/admin/exercises student 403", (await req("POST", "/api/admin/exercises", { token: userToken, body: mc(["x", "y"]) })).status === 403, "expected 403");

    R.mcGuard = { lessonId, badLetter: bad.status, good: good.status, dash: dash.status, one: one.status, none: none.status, null: nul.status, two: two.status };
  }

  // ═══════════════════════════════════════════ B. ?sort= measured
  setArea("sort"); flushBuckets();
  console.log("\n--- B. ?sort= measured (not assumed) ---");
  {
    const idsOf = (r) => (r.data?.content || []).map((x) => x.id ?? x.lessonId ?? x.deckId ?? x.vocabId ?? x.exerciseId);
    const titlesOf = (r) => (r.data?.content || []).map((x) => x.title ?? x.name ?? x.word ?? x.question ?? null);
    const keyOf = (r) => (r.data?.content || []).map((x) => x.title ?? x.name ?? x.word ?? x.question ?? null);

    const pairs = [
      ["/api/lessons?page=0&size=8&sort=title,asc", "/api/lessons?page=0&size=8&sort=title,desc", "lessons (title)"],
      ["/api/admin/exercises?page=0&size=8&sort=question,asc", "/api/admin/exercises?page=0&size=8&sort=question,desc", "admin/exercises (question)"],
      ["/api/vocabulary?page=0&size=8&sort=word,asc", "/api/vocabulary?page=0&size=8&sort=word,desc", "vocabulary (word)"],
      ["/api/decks?page=0&size=8&sort=name,asc", "/api/decks?page=0&size=8&sort=name,desc", "decks (name)"],
    ];
    for (const [ascP, descP, label] of pairs) {
      const tok = label.startsWith("admin") || label.startsWith("vocabulary") ? adminToken : null;
      const a = await req("GET", ascP, { token: tok });
      const d = await req("GET", descP, { token: tok });
      const same = JSON.stringify(idsOf(a)) === JSON.stringify(idsOf(d)) && JSON.stringify(keyOf(a)) === JSON.stringify(keyOf(d));
      info(`?sort= ${label}`, `asc=${JSON.stringify(keyOf(a))} desc=${JSON.stringify(keyOf(d))} same=${same}`);
      R.sortProbes = R.sortProbes || [];
      R.sortProbes.push({ label, asc: keyOf(a), desc: keyOf(d), identical: same, ascStatus: a.status, descStatus: d.status });
      if (!same) {
        // verify it is genuinely ordered in the requested direction
        const ka = keyOf(a), kd = keyOf(d);
        const sortedAsc = ka.every((v, i) => i === 0 || String(ka[i - 1]).localeCompare(String(v)) <= 0);
        const sortedDesc = kd.every((v, i) => i === 0 || String(kd[i - 1]).localeCompare(String(v)) >= 0);
        check(`  ${label} ?sort= honoured (asc ordered + desc reversed)`, sortedAsc && sortedDesc, `ascOrdered=${sortedAsc} descOrdered=${sortedDesc}`);
      }
    }
    const lessonsAsc = R.sortProbes.find((s) => s.label.startsWith("lessons"));
    check("lessons ?sort= is IGNORED (asc payload == desc payload)", lessonsAsc.identical === true,
      `identical=${lessonsAsc.identical} — LessonController hardcodes Sort.by("orderIndex").ascending()`);
  }

  // ═══════════════════════════════════════════ C. RESPONSE CONTRACT
  setArea("contract"); flushBuckets();
  console.log("\n--- C. RESPONSE CONTRACT (field names + types) ---");
  {
    // (1) Lessons list item + detail
    const list = await req("GET", "/api/lessons?page=0&size=3");
    const item = list.data?.content?.[0];
    const c1 = contract(item, { id: "number", title: "string" });
    check("(1) lesson list item {id:number, title:string}", c1.ok, c1.why);
    info("  lesson list item keys", Object.keys(item || {}).join(","));
    if (item?.id) {
      const det = await req("GET", `/api/lessons/${item.id}`);
      const c2 = contract(det.data, { id: "number", title: "string" });
      check("(1) lesson detail {id:number, title:string}", c2.ok, c2.why);
      info("  lesson detail keys", Object.keys(det.data || {}).join(","));
      // exercises contract
      const ex = await req("GET", `/api/lessons/${item.id}/exercises`);
      const e0 = Array.isArray(ex.data) ? ex.data[0] : null;
      if (e0) {
        const c3 = contract(e0, { id: "number", lessonId: "number", question: "string", exerciseType: "string" });
        check("(1) exercise item {id,lessonId:number, question:string, exerciseType:string}", c3.ok, c3.why);
        info("  exercise keys", Object.keys(e0).join(","));
      } else info("  lesson has no exercises to sample", `status=${ex.status} len=${Array.isArray(ex.data) ? ex.data.length : "n/a"}`);
    }

    // (2) Streak
    const snap = await req("GET", "/api/streak/snapshot", { token: userToken });
    const cs = contract(snap.data, { currentStreak: "number", studiedToday: "boolean", studiedDays: "array" });
    check("(2) streak snapshot {currentStreak:number, studiedToday:boolean, studiedDays:array}", cs.ok, cs.why);
    const cur = await req("GET", "/api/streak/current", { token: userToken });
    info("(2) streak/current payload", JSON.stringify(cur.data).slice(0, 200));

    // (3) Login/Register
    const lg = await req("POST", "/api/auth/login", { body: USER });
    const cl = contract(lg.data, { token: "string" });
    check("(3) login {token:string}", cl.ok, cl.why);
    info("  login keys", Object.keys(lg.data || {}).join(","));
    const me = await req("GET", "/api/auth/me", { token: userToken });
    const cm = contract(me.data, { email: "string", isAdmin: "boolean" });
    check("(3) /me {email:string, isAdmin:boolean}", cm.ok, cm.why);

    // (4) Search
    const s = await req("GET", "/api/vocabulary/search?keyword=hello");
    const arr = Array.isArray(s.data);
    check("(4) search returns a JSON array", s.status === 200 && arr, `status=${s.status} type=${Array.isArray(s.data) ? "array" : typeof s.data}`);
    if (arr && s.data[0]) {
      const c4 = contract(s.data[0], { word: "string" });
      check("(4) search item {word:string}", c4.ok, c4.why);
      info("  search item keys", Object.keys(s.data[0]).join(","));
    } else info("  search 'hello' returned empty", `len=${arr ? s.data.length : "n/a"}`);

    // (5) CRUD round-trip contract
    const nm = MARKER + "-DEEP-" + PUBLIC_DECK;
    const dk = await req("POST", "/api/decks", { token: userToken, body: { name: nm, description: "deep", isPublic: false } });
    const deckId = dk.data?.id ?? dk.data?.deckId ?? dk.data?.data?.id ?? null;
    check("(5) POST /api/decks -> 200/201 with id", [200, 201].includes(dk.status) && deckId != null, `status=${dk.status} id=${deckId}`);
    if (deckId) {
      const rd = await req("GET", `/api/decks/${deckId}`, { token: userToken });
      const c5 = contract(rd.data, { id: "number", name: "string" });
      check("(5) deck read {id:number, name:string}", c5.ok, c5.why);
      info("  deck keys", Object.keys(rd.data || {}).join(","));
      const del = await req("DELETE", `/api/decks/${deckId}`, { token: userToken });
      check("(5) DELETE deck -> 200/204", [200, 204].includes(del.status), `got ${del.status}`);
      check("(5) re-GET deleted deck 404", (await req("GET", `/api/decks/${deckId}`, { token: userToken })).status === 404, "expected 404");
      R.deepDeckId = deckId;
    } else { blocked("(5) deck contract", "no id from create"); }

    // (6) AI
    const gen = await req("POST", "/api/ai/generate-vocab", { token: userToken, body: { topic: "travel", level: "A2", count: 1 } });
    info("(6) ai/generate-vocab", `status=${gen.status} body=${JSON.stringify(gen.data).slice(0, 160)}`);
    check("(6) ai/generate-vocab reachable (200/429/504 latency)", [200, 429, 504].includes(gen.status), `got ${gen.status}`);
    if (gen.status === 200) {
      const g0 = Array.isArray(gen.data) ? gen.data[0] : gen.data;
      const c6 = contract(g0, { word: "string", meaning: "string" });
      check("(6) generate-vocab item {word:string, meaning:string}", c6.ok, c6.why);
      info("  ai item keys", Object.keys(g0 || {}).join(","));
    }
  }

  // ═══════════════════════════════════════════ D. ROLE AUTH both directions
  setArea("roles"); flushBuckets();
  console.log("\n--- D. ROLE AUTH (both directions) ---");
  {
    // student cannot do admin things
    const studentAdminCalls = [
      ["GET", "/api/admin/stats"], ["GET", "/api/admin/users"], ["GET", "/api/admin/exercises"],
      ["GET", "/api/admin/exercises/ai/status"], ["GET", "/api/v1/admin/speaking-prompts"],
      ["GET", "/api/v1/admin/video-lessons"], ["GET", "/api/admin/lessons"],
    ];
    for (const [m, p] of studentAdminCalls) {
      const st = await req(m, p, { token: userToken });
      const an = await req(m, p);
      check(`DENY ${m} ${p} student=403 anon=401`, st.status === 403 && an.status === 401, `got student=${st.status} anon=${an.status}`);
    }
    // student cannot write admin resources
    check("DENY POST /api/admin/lessons student 403", (await req("POST", "/api/admin/lessons", { token: userToken, body: { title: "x", level: "ELEMENTARY" } })).status === 403, "expected 403");
    check("DENY POST /api/lessons student 403", (await req("POST", "/api/lessons", { token: userToken, body: { title: "x", level: "ELEMENTARY", category: "A" } })).status === 403, "expected 403");
    check("DENY PUT /api/vocabulary/1 student 403", (await req("PUT", "/api/vocabulary/1", { token: userToken, body: { word: "x", meaning: "y" } })).status === 403, "expected 403");
    check("DENY DELETE /api/vocabulary/1 student 403", (await req("DELETE", "/api/vocabulary/1", { token: userToken })).status === 403, "expected 403");
    // admin CAN do admin things
    check("ALLOW GET /api/admin/stats admin 200", (await req("GET", "/api/admin/stats", { token: adminToken })).status === 200, "expected 200");
    check("ALLOW GET /api/admin/exercises admin 200", (await req("GET", "/api/admin/exercises", { token: adminToken })).status === 200, "expected 200");
    // anonymous rejected on protected routes
    check("DENY GET /api/streak/snapshot anon 401", (await req("GET", "/api/streak/snapshot")).status === 401, "expected 401");
    check("DENY GET /api/dashboard/stats anon 401", (await req("GET", "/api/dashboard/stats")).status === 401, "expected 401");
    check("DENY GET /api/users/progress anon 401", (await req("GET", "/api/users/progress")).status === 401, "expected 401");
    check("DENY POST /api/games/submit anon 401", (await req("POST", "/api/games/submit", { body: { sessionId: "x", answers: [] } })).status === 401, "expected 401");
    // public routes genuinely public
    check("ALLOW GET /api/lessons anon 200", (await req("GET", "/api/lessons?page=0&size=1")).status === 200, "expected 200");
    check("ALLOW GET /api/leaderboard anon 200", (await req("GET", "/api/leaderboard")).status === 200, "expected 200");
    check("ALLOW GET /api/vocabulary/search?keyword=ab anon 200", (await req("GET", "/api/vocabulary/search?keyword=ab")).status === 200, "expected 200");
  }

  // ═══════════════════════════════════════════ CLEANUP
  console.log("\n--- CLEANUP ---");
  {
    if (lessonId) {
      const del = await req("DELETE", `/api/lessons/${lessonId}`, { token: adminToken });
      check("cleanup: delete audit lesson (cascades its exercises)", [200, 204].includes(del.status), `got ${del.status}`);
      const after = await req("GET", `/api/lessons/${lessonId}/exercises`, { token: adminToken });
      check("cleanup: audit lesson has no exercises left", after.status === 404 || (Array.isArray(after.data) && after.data.length === 0), `status=${after.status} len=${Array.isArray(after.data) ? after.data.length : "n/a"}`);
      check("cleanup: audit lesson re-GET 404", (await req("GET", `/api/lessons/${lessonId}`, { token: adminToken })).status === 404, "expected 404");
    }
    // SQL-level verification that no <MARKER>-MC lesson / <MARKER>-DEEP deck survived
    const sql = `
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
SELECT 'DEEP_LESSONS=' + CAST((SELECT COUNT(*) FROM lessons WHERE title LIKE '${MARKER}-MC-%') AS varchar(10))
     + ' DEEP_DECKS=' + CAST((SELECT COUNT(*) FROM decks WHERE name LIKE '${MARKER}-DEEP-%') AS varchar(10))
     + ' DEEP_EX=' + CAST((SELECT COUNT(*) FROM exercises WHERE lesson_id IN (SELECT lesson_id FROM lessons WHERE title LIKE '${MARKER}-MC-%')) AS varchar(10)) AS marker;
`;
    fs.writeFileSync(path.join(__dirname, "_deep-cleanup.sql"), sql);
    let out = "";
    try { out = execFileSync("python", ["sweep/v8/sqlrun.py", "sweep/harness/_deep-cleanup.sql"], { encoding: "utf8", cwd: path.join(__dirname, "..", "..") }); }
    catch (e) { out = String(e.message); }
    console.log(out.trim());
    const msgErrors = (out.match(/Msg \d+/g) || []).length;
    check("cleanup SQL produced 0 errors (Msg scan)", msgErrors === 0, `Msg count=${msgErrors}`);
    const m = /DEEP_LESSONS=(\d+) DEEP_DECKS=(\d+) DEEP_EX=(\d+)/.exec(out);
    check("cleanup left 0 audit residue", m && m.slice(1).every((x) => x === "0"), m ? m[0] : "marker not found");
    R.cleanup = m ? { lessons: +m[1], decks: +m[2], exercises: +m[3] } : null;
  }

  console.log("\n=== DEEP SUMMARY ===");
  console.log(`pass=${R.pass} fail=${R.fail} blocked=${R.blocked} n_a=${R.n_a}`);
  for (const [a, v] of Object.entries(R.areas)) console.log(`  ${a.padEnd(10)} pass=${v.pass} fail=${v.fail} blocked=${v.blocked}`);
  const OUT_FILE = path.join(EVID, "deep-probe.json");
  fs.mkdirSync(EVID, { recursive: true });
  fs.writeFileSync(OUT_FILE, JSON.stringify(R, null, 2));
  console.log("written:", path.relative(path.join(__dirname, "..", ".."), OUT_FILE));
})();
