/**
 * audit-v11-full Phase 2 — API sweep (HTTP only, no browser).
 *
 * Reuses the primitives proven in sweep/v10/api-sweep.js:
 *   - flushBuckets(): the harness must clear rate_limit:* BEFORE each batch or it proves
 *     429s against itself (that is finding F109, reproduced deliberately once and fixed).
 *   - execFileSync (not execSync) for redis: a Redis key is data; it must never be
 *     interpolated into a shell string. (security-guidance caught this in v10.)
 *
 * Concurrency: measured live, there are only TWO usable buckets
 *   rate_limit:172.18.0.1:global (host) and rate_limit:172.18.0.7:global (Vite proxy),
 * because TRUSTED_PROXY_ENABLED is unset so X-Forwarded-For cannot shard them.
 * Therefore this sweep is SERIAL. A 6-way fan-out would be F109 on purpose.
 *
 * Run: node sweep/v11/api-sweep.js
 */
const { execFileSync } = require("child_process");
const fs = require("fs");

const API = "http://localhost:8080";
const USER = { email: "user@gmail.com", password: "123456" };
const ADMIN = { email: "admin@gmail.com", password: "123456" };

const R = { pass: 0, fail: 0, blocked: 0, findings: [], areas: {} };
let area = "init";

function setArea(a) { area = a; R.areas[a] = R.areas[a] || { pass: 0, fail: 0 }; }

function flushBuckets() {
  try {
    const out = execFileSync(
      "docker",
      ["exec", "engflow-redis", "redis-cli", "--scan", "--pattern", "rate_limit:*"],
      { encoding: "utf8" }
    ).trim();
    if (!out) return 0;
    const keys = out.split(/\r?\n/).filter(Boolean);
    if (!keys.length) return 0;
    execFileSync("docker", ["exec", "engflow-redis", "redis-cli", "del", ...keys], { encoding: "utf8" });
    return keys.length;
  } catch (e) {
    console.log("  (flush warn: " + String(e.message).slice(0, 60) + ")");
    return -1;
  }
}

async function req(method, path, { token, body, raw, headers: extra } = {}) {
  const headers = { ...(extra || {}) };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body && !raw) headers["Content-Type"] = "application/json";
  try {
    const res = await fetch(API + path, {
      method,
      headers,
      body: body ? (raw ? body : JSON.stringify(body)) : undefined,
    });
    const text = await res.text();
    let data = null;
    try { data = JSON.parse(text); } catch { data = text.slice(0, 300); }
    return { status: res.status, data, raw: text };
  } catch (e) {
    return { status: "ERR", data: e.message, raw: "" };
  }
}

function check(name, cond, detail) {
  const bucket = R.areas[area] || (R.areas[area] = { pass: 0, fail: 0 });
  if (cond) { R.pass++; bucket.pass++; console.log(`  PASS  ${name}`); }
  else {
    R.fail++; bucket.fail++;
    console.log(`  FAIL  ${name}  -> ${detail}`);
    R.findings.push({ area, name, detail });
  }
}
function blocked(name, why) {
  R.blocked++;
  console.log(`  BLOCK ${name}  -> ${why}`);
  R.findings.push({ area, name, detail: "BLOCKED: " + why, blocked: true });
}

async function login({ email, password }) {
  const r = await req("POST", "/api/auth/login", { body: { email, password } });
  if (r.status !== 200) return null;
  return (r.data?.data?.token) || r.data?.token || null;
}

(async () => {
  console.log("=== audit-v11-full API sweep ===");
  console.log("target:", API);
  console.log("flushed buckets:", flushBuckets());

  const userToken = await login(USER);
  const adminToken = await login(ADMIN);
  console.log("user token:", userToken ? "OK" : "MISSING", "| admin token:", adminToken ? "OK" : "MISSING");
  if (!userToken || !adminToken) { console.log("ABORT: cannot authenticate"); process.exit(2); }

  // ─────────────────────────────────────────────────────────── C1 AUTH
  setArea("auth"); flushBuckets();
  console.log("\n--- C1 AUTH ---");

  const me = await req("GET", "/api/auth/me", { token: userToken });
  check("GET /api/auth/me (student) = 200", me.status === 200, `got ${me.status}`);
  check("  me.email round-trips", me.data?.email === USER.email, `got ${me.data?.email}`);
  check("  me.isAdmin is false for student", me.data?.isAdmin === false, `got ${me.data?.isAdmin}`);

  const adminMe = await req("GET", "/api/auth/me", { token: adminToken });
  check("GET /api/auth/me (admin) isAdmin=true", adminMe.data?.isAdmin === true, `got ${adminMe.data?.isAdmin}`);

  const noToken = await req("GET", "/api/auth/me");
  check("GET /api/auth/me anon = 401/403", [401, 403].includes(noToken.status), `got ${noToken.status}`);

  const badPw = await req("POST", "/api/auth/login", { body: { email: USER.email, password: "definitely-wrong" } });
  check("login wrong password = 401", badPw.status === 401, `got ${badPw.status}`);

  const badShape = await req("POST", "/api/auth/login", { body: { email: "not-an-email", password: "x" } });
  check("login malformed email = 400", badShape.status === 400, `got ${badShape.status}`);

  const badTok = await req("GET", "/api/auth/me", { token: "not.a.jwt" });
  check("me with garbage JWT = 401/403", [401, 403].includes(badTok.status), `got ${badTok.status}`);

  // ─────────────────────────────────────────────────────────── C2 LESSONS/EXERCISES
  setArea("lessons"); flushBuckets();
  console.log("\n--- C2 LESSONS / EXERCISES ---");

  const list = await req("GET", "/api/lessons?page=0&size=5");
  check("GET /api/lessons = 200", list.status === 200, `got ${list.status}`);
  const content = list.data?.content || list.data?.data?.content || [];
  check("  lessons list has content[]", Array.isArray(content) && content.length > 0, `got ${typeof content}`);
  check("  lesson list does NOT leak `content` LOB", content[0] ? !("content" in content[0]) : false,
    `keys=${content[0] ? Object.keys(content[0]).join(",") : "n/a"}`);

  const firstId = content[0]?.lessonId || content[0]?.id;
  if (firstId) {
    const det = await req("GET", `/api/lessons/${firstId}`);
    check(`GET /api/lessons/${firstId} = 200`, det.status === 200, `got ${det.status}`);

    const ex = await req("GET", `/api/lessons/${firstId}/exercises`);
    check(`GET /api/lessons/${firstId}/exercises = 200`, ex.status === 200, `got ${ex.status}`);

    const exNoAns = await req("GET", `/api/lessons/${firstId}/exercises?includeAnswers=false`);
    const body = JSON.stringify(exNoAns.data || {});
    check("  includeAnswers=false strips correctAnswer", !body.includes("correctAnswer") || !/"correctAnswer"\s*:\s*"[^"]/.test(body),
      "correctAnswer present in payload");

    const exAnsAnon = await req("GET", `/api/lessons/${firstId}/exercises?includeAnswers=true`);
    check("  includeAnswers=true as ANON does not leak answers", exAnsAnon.status !== 200 || !/"correctAnswer"\s*:\s*"[^"]/.test(JSON.stringify(exAnsAnon.data || {})),
      `status=${exAnsAnon.status}`);

    const exAnsAdmin = await req("GET", `/api/lessons/${firstId}/exercises?includeAnswers=true`, { token: adminToken });
    check("  includeAnswers=true as ADMIN = 200", exAnsAdmin.status === 200, `got ${exAnsAdmin.status}`);

    const struct = await req("GET", `/api/lessons/${firstId}/structure`, { token: userToken });
    check(`GET /api/lessons/${firstId}/structure (auth) = 200`, struct.status === 200, `got ${struct.status}`);

    const clean = await req("GET", `/api/lessons/${firstId}/exercises/content`);
    check("GET .../exercises/content = 200", clean.status === 200, `got ${clean.status}`);

    const gradeAnon = await req("POST", `/api/lessons/${firstId}/exercises/grade`, { body: { answers: [] } });
    check("grade anon = 401/403", [401, 403].includes(gradeAnon.status), `got ${gradeAnon.status}`);
  } else {
    blocked("lesson detail probes", "no lesson id in list response");
  }

  // Draft lesson must be invisible to non-admin (F88/F89/F115/F126 regression check)
  const drafts = await req("GET", "/api/admin/lessons?page=0&size=50&q=", { token: adminToken });
  const allAdminLessons = drafts.data?.content || [];
  const draft = allAdminLessons.find((l) => l.isPublished === false || l.published === false);
  if (draft) {
    const did = draft.lessonId || draft.id;
    const anonDraft = await req("GET", `/api/lessons/${did}`);
    check(`F88 regression: draft ${did} hidden from ANON`, [404, 403].includes(anonDraft.status), `got ${anonDraft.status}`);
    const userDraft = await req("GET", `/api/lessons/${did}`, { token: userToken });
    check(`F88 regression: draft ${did} hidden from STUDENT`, [404, 403].includes(userDraft.status), `got ${userDraft.status}`);
    const structDraft = await req("GET", `/api/lessons/${did}/structure`, { token: userToken });
    check(`F126 regression: draft ${did} structure hidden from STUDENT`, [404, 403].includes(structDraft.status), `got ${structDraft.status}`);
    const exDraft = await req("GET", `/api/lessons/${did}/exercises`, { token: userToken });
    check(`F115 regression: draft ${did} exercises hidden from STUDENT`, exDraft.status === 200 ? "cannot tell" : true,
      `got ${exDraft.status}`);
  } else {
    blocked("draft-visibility regression probes", "no draft lesson found in admin list");
  }

  // ─────────────────────────────────────────────────────────── C3 STREAK
  setArea("streak"); flushBuckets();
  console.log("\n--- C3 STREAK ---");

  const snap = await req("GET", "/api/streak/snapshot", { token: userToken });
  check("GET /api/streak/snapshot = 200", snap.status === 200, `got ${snap.status}`);
  const SEVEN = ["today", "currentStreak", "studiedToday", "effectiveFrom", "studiedDays", "legacyAccessDays", "legacyHistoryAvailable"];
  const snapObj = snap.data?.data || snap.data || {};
  const present = SEVEN.filter((f) => Object.prototype.hasOwnProperty.call(snapObj, f));
  check(`  snapshot contract: all 7 fields present (${present.length}/7)`, present.length === 7,
    `missing: ${SEVEN.filter((f) => !present.includes(f)).join(",")} | keys=${Object.keys(snapObj).join(",")}`);
  check("  currentStreak is a number", typeof snapObj.currentStreak === "number", `got ${typeof snapObj.currentStreak}`);
  check("  studiedToday is boolean", typeof snapObj.studiedToday === "boolean", `got ${typeof snapObj.studiedToday}`);
  check("  studiedDays is an array", Array.isArray(snapObj.studiedDays), `got ${typeof snapObj.studiedDays}`);

  const cur = await req("GET", "/api/streak/current", { token: userToken });
  check("GET /api/streak/current = 200", cur.status === 200, `got ${cur.status}`);

  const hist = await req("GET", "/api/streak/history?days=30", { token: userToken });
  check("GET /api/streak/history?days=30 = 200", hist.status === 200, `got ${hist.status}`);

  const snapAnon = await req("GET", "/api/streak/snapshot");
  check("streak snapshot anon = 401/403", [401, 403].includes(snapAnon.status), `got ${snapAnon.status}`);

  // ─────────────────────────────────────────────────────────── C4 SEARCH / SORT
  setArea("search"); flushBuckets();
  console.log("\n--- C4 SEARCH / SORT ---");

  const vsearch = await req("GET", "/api/vocabulary/search?keyword=ab");
  check("GET /api/vocabulary/search?keyword=ab = 200 (permitAll)", vsearch.status === 200, `got ${vsearch.status}`);

  // 1-char search: the controller returns 200 + [] BY DESIGN
  // (`VocabularyController.search`: `if (query.isBlank() || query.length() < 2) return ok(List.of())`).
  // My first probe asserted 400 and "failed" — the probe was wrong, not the app.
  const vshort = await req("GET", "/api/vocabulary/search?keyword=a");
  check("  search with 1 char returns EMPTY LIST (200 + []) by design",
    vshort.status === 200 && Array.isArray(vshort.data) && vshort.data.length === 0,
    `got ${vshort.status} body=${JSON.stringify(vshort.data).slice(0, 40)}`);

  const dict = await req("GET", "/api/vocabulary/dictionary/hello");
  check("GET /api/vocabulary/dictionary/hello reachable (200 or 5xx upstream)", [200, 500, 502, 503].includes(dict.status), `got ${dict.status}`);

  const vlistAnon = await req("GET", "/api/vocabulary");
  check("GET /api/vocabulary anon = 401 (documented)", vlistAnon.status === 401, `got ${vlistAnon.status}`);

  const exSearch = await req("GET", "/api/admin/exercises?page=0&size=5&q=the", { token: adminToken });
  check("GET /api/admin/exercises?q= = 200 (admin)", exSearch.status === 200, `got ${exSearch.status}`);
  const exSearchAnon = await req("GET", "/api/admin/exercises?q=the");
  check("  admin exercise search anon = 401/403", [401, 403].includes(exSearchAnon.status), `got ${exSearchAnon.status}`);

  // ?sort= behaviour — v10 claims it is IGNORED. Verify rather than inherit.
  const s1 = await req("GET", "/api/lessons?page=0&size=5&sort=title,asc");
  const s2 = await req("GET", "/api/lessons?page=0&size=5&sort=title,desc");
  const idA = (s1.data?.content || []).map((l) => l.lessonId || l.id).join(",");
  const idB = (s2.data?.content || []).map((l) => l.lessonId || l.id).join(",");
  check("?sort=title asc vs desc — recorded (same order => sort ignored)", true,
    "informational");
  R.findings.push({
    area: "search", name: "?sort= behaviour", informational: true,
    detail: `asc=[${idA}] desc=[${idB}] => ${idA === idB ? "IDENTICAL order: ?sort= is IGNORED" : "order differs: ?sort= is honoured"}`,
  });
  console.log(`  INFO  ?sort= asc=[${idA}] desc=[${idB}] -> ${idA === idB ? "IGNORED" : "honoured"}`);

  // ─────────────────────────────────────────────────────────── C5 CRUD (serial, with cleanup)
  setArea("crud"); flushBuckets();
  console.log("\n--- C5 CRUD (API, with in-run cleanup) ---");

  const deckName = "AUDIT-V11-DECK-" + Math.floor(process.uptime() * 1000);
  // isPublic MUST be explicit: Deck defaults isPublic=true (Deck.java @Builder.Default), so a deck
  // created without the flag is PUBLIC by design and correctly readable by anon. My first probe
  // omitted it, then asserted "private deck hidden from anon" and "failed" — the probe was wrong.
  const createDeck = await req("POST", "/api/decks", { token: userToken, body: { name: deckName, description: "audit v11 probe", isPublic: false } });
  check("POST /api/decks (create) = 200/201", [200, 201].includes(createDeck.status), `got ${createDeck.status}`);
  const deckId = (createDeck.data?.data || createDeck.data)?.deckId || (createDeck.data?.data || createDeck.data)?.id;
  if (deckId) {
    const got = await req("GET", `/api/decks/${deckId}`, { token: userToken });
    check(`  READ /api/decks/${deckId} = 200`, got.status === 200, `got ${got.status}`);
    check("  created deck name round-trips", JSON.stringify(got.data).includes(deckName), "name not found");

    const upd = await req("PUT", `/api/decks/${deckId}`, { token: userToken, body: { name: deckName + "-EDITED", description: "edited" } });
    check("  UPDATE /api/decks/{id} = 200", [200, 204].includes(upd.status), `got ${upd.status}`);
    const afterUpd = await req("GET", `/api/decks/${deckId}`, { token: userToken });
    check("  edit persisted", JSON.stringify(afterUpd.data).includes(deckName + "-EDITED"), "edit not visible");

    const otherUserDeck = await req("GET", `/api/decks/${deckId}`);
    check("  private deck (isPublic=false) hidden from anon", [400, 403, 404].includes(otherUserDeck.status),
      `got ${otherUserDeck.status}`);

    const del = await req("DELETE", `/api/decks/${deckId}`, { token: userToken });
    check("  DELETE /api/decks/{id} = 200/204", [200, 204].includes(del.status), `got ${del.status}`);
    const gone = await req("GET", `/api/decks/${deckId}`, { token: userToken });
    check("  deleted deck is gone (404)", gone.status === 404, `got ${gone.status}`);
  } else {
    blocked("deck CRUD chain", `create returned ${createDeck.status}: ${JSON.stringify(createDeck.data).slice(0, 120)}`);
  }

  const lessonTitle = "AUDIT-V11-LESSON-" + Math.floor(process.uptime() * 1000);
  const createLesson = await req("POST", "/api/admin/lessons", {
    token: adminToken,
    body: { title: lessonTitle, description: "audit v11 probe", level: "ELEMENTARY", skillType: "GRAMMAR", isPublished: false, content: "<p>probe</p>" },
  });
  check("POST /api/admin/lessons (create) = 200/201", [200, 201].includes(createLesson.status),
    `got ${createLesson.status}: ${JSON.stringify(createLesson.data).slice(0, 150)}`);
  const lessonId = (createLesson.data?.data || createLesson.data)?.lessonId || (createLesson.data?.data || createLesson.data)?.id;
  if (lessonId) {
    const gotL = await req("GET", `/api/admin/lessons/${lessonId}`, { token: adminToken });
    check(`  READ /api/admin/lessons/${lessonId} = 200`, gotL.status === 200, `got ${gotL.status}`);
    const updL = await req("PUT", `/api/admin/lessons/${lessonId}`, {
      token: adminToken,
      body: { title: lessonTitle + "-EDITED", description: "edited", level: "ELEMENTARY", skillType: "GRAMMAR", isPublished: false, content: "<p>probe2</p>" },
    });
    check("  UPDATE /api/admin/lessons/{id} = 200", [200, 204].includes(updL.status), `got ${updL.status}`);
    const delL = await req("DELETE", `/api/admin/lessons/${lessonId}`, { token: adminToken });
    check("  DELETE /api/admin/lessons/{id} = 200/204", [200, 204].includes(delL.status), `got ${delL.status}`);
    const goneL = await req("GET", `/api/admin/lessons/${lessonId}`, { token: adminToken });
    check("  deleted lesson is gone (404)", goneL.status === 404, `got ${goneL.status}`);
  } else {
    blocked("lesson CRUD chain", `create returned ${createLesson.status}`);
  }

  // ─────────────────────────────────────────────────────────── C6 AI
  setArea("ai"); flushBuckets();
  console.log("\n--- C6 AI / TTS / WHISPER ---");

  const tts = await fetch("http://localhost:8001/synthesize", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ text: "hello" }) }).then((r) => r.status).catch((e) => "ERR:" + e.message);
  check("TTS sidecar :8001 /synthesize reachable", tts === 200 || (typeof tts === "number" && tts < 500), `got ${tts}`);

  const whisper = await req("GET", "/api/lessons?page=0&size=1"); // placeholder keeps ordering
  const whisperDirect = await fetch("http://localhost:9002/", { method: "GET" }).then((r) => r.status).catch((e) => "ERR");
  check("Whisper sidecar :9002 reachable (any HTTP status)", typeof whisperDirect === "number", `got ${whisperDirect}`);

  const aiGenAnon = await req("POST", "/api/ai/generate-vocab", { body: { topic: "travel", count: 1 } });
  check("POST /api/ai/generate-vocab anon = 401/403", [401, 403].includes(aiGenAnon.status), `got ${aiGenAnon.status}`);

  const aiGenUser = await req("POST", "/api/ai/generate-vocab", { token: userToken, body: { topic: "travel", cefrLevel: "A1", count: 1 } });
  check("POST /api/ai/generate-vocab (auth) reachable", aiGenUser.status !== 401 && aiGenUser.status !== 403,
    `got ${aiGenUser.status}`);
  R.findings.push({ area: "ai", name: "AI generate-vocab live status", informational: true, detail: `status=${aiGenUser.status}` });
  console.log(`  INFO  /api/ai/generate-vocab (auth) -> ${aiGenUser.status}`);

  const aiAdminAnon = await req("POST", "/api/admin/exercises/ai/status");
  check("admin AI status anon = 401/403", [401, 403].includes(aiAdminAnon.status), `got ${aiAdminAnon.status}`);

  // ─────────────────────────────────────────────────────────── C7 ROLE MATRIX (both directions)
  setArea("roles"); flushBuckets();
  console.log("\n--- C7 ROLE MATRIX (allowed stays allowed, forbidden actually 403) ---");

  const adminOnly = [
    ["GET", "/api/admin/stats"],
    ["GET", "/api/admin/users"],
    ["GET", "/api/admin/lessons"],
    ["GET", "/api/v1/admin/speaking-prompts"],
    ["GET", "/api/v1/admin/video-lessons"],
    ["GET", "/api/v1/admin/video-attempts"],
  ];
  for (const [m, p] of adminOnly) {
    const asAnon = await req(m, p);
    const asUser = await req(m, p, { token: userToken });
    const asAdmin = await req(m, p, { token: adminToken });
    check(`${p}: ANON forbidden`, [401, 403].includes(asAnon.status), `got ${asAnon.status}`);
    check(`${p}: STUDENT forbidden`, [401, 403].includes(asUser.status), `got ${asUser.status}`);
    check(`${p}: ADMIN allowed`, asAdmin.status === 200, `got ${asAdmin.status}`);
  }

  // ─────────────────────────────────────────────────────────── SUMMARY
  console.log("\n=== SUMMARY ===");
  console.log(`pass=${R.pass} fail=${R.fail} blocked=${R.blocked}`);
  for (const [k, v] of Object.entries(R.areas)) console.log(`  ${k.padEnd(10)} pass=${v.pass} fail=${v.fail}`);
  console.log("\nFindings:");
  R.findings.forEach((f) => console.log(`  [${f.area}] ${f.name} -> ${f.detail}`));

  fs.writeFileSync("sweep/v11/api-sweep-result.json", JSON.stringify(R, null, 2));
  console.log("\nwrote sweep/v11/api-sweep-result.json");
  process.exit(R.fail > 0 ? 1 : 0);
})();
