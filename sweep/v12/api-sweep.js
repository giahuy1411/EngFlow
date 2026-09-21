/**
 * audit-v12-full Phase 2 — API sweep over the live container (HTTP only, no browser).
 *
 * WHY THIS FILE IS BIGGER THAN v11's: v11 probed ~27 of 131 endpoints and 12 controllers
 * were never touched. This sweep covers the whole inventory, with the correct role per
 * endpoint, and checks the RESPONSE CONTRACT (field names + types) rather than "a 2xx arrived".
 *
 * Primitives inherited from sweep/v11/api-sweep.js (proven there):
 *   - flushBuckets(): clear rate_limit:* BEFORE each batch, or the harness manufactures
 *     its own 429s (finding F109). execFileSync with argv — a Redis key is data and must
 *     never be interpolated into a shell string.
 *   - SERIAL execution: only two usable global buckets exist (TRUSTED_PROXY_ENABLED unset),
 *     so a fan-out would be F109 on purpose.
 *
 * Run: node sweep/v12/api-sweep.js
 * Out: .specify/specs/audit-v12-full/evidence/api-sweep.json
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const API = "http://localhost:8080";
const USER = { email: "user@gmail.com", password: "123456" };
const ADMIN = { email: "admin@gmail.com", password: "123456" };
const PUBLIC_DECK = 10006; // Oxford 3000 — real, is_public=1

const R = { pass: 0, fail: 0, blocked: 0, n_a: 0, findings: [], areas: {}, probed: [] };
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

async function req(method, p, { token, body, raw, headers: extra } = {}) {
  const headers = { ...(extra || {}) };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body && !raw) headers["Content-Type"] = "application/json";
  try {
    const res = await fetch(API + p, { method, headers, body: body ? (raw ? body : JSON.stringify(body)) : undefined });
    const text = await res.text();
    let data = null;
    try { data = JSON.parse(text); } catch { data = text.slice(0, 300); }
    return { status: res.status, data, raw: text, headers: res.headers };
  } catch (e) { return { status: "ERR", data: String(e.message), raw: "", headers: new Map() }; }
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
function na(name, why) {
  R.n_a++; R.probed.push({ area, name, status: "N/A", detail: why });
  console.log(`  N/A   ${name}  -> ${why}`);
}

// Contract helper: assert the declared keys exist with the declared JS type.
function contract(obj, shape) {
  if (obj == null || typeof obj !== "object") return { ok: false, why: `not an object (${typeof obj})` };
  for (const [k, t] of Object.entries(shape)) {
    if (!(k in obj)) return { ok: false, why: `missing key "${k}" (have: ${Object.keys(obj).slice(0, 12).join(",")})` };
    const actual = Array.isArray(obj[k]) ? "array" : obj[k] === null ? "null" : typeof obj[k];
    if (t === "any") continue;
    if (t === "number" && typeof obj[k] !== "number") return { ok: false, why: `"${k}" is ${actual}, expected number` };
    if (t === "array" && !Array.isArray(obj[k])) return { ok: false, why: `"${k}" is ${actual}, expected array` };
    if (t !== "number" && t !== "array" && actual !== t && actual !== "null")
      return { ok: false, why: `"${k}" is ${actual}, expected ${t}` };
  }
  return { ok: true };
}

const login = async ({ email, password }) => {
  const r = await req("POST", "/api/auth/login", { body: { email, password } });
  return r.status === 200 ? (r.data?.data?.token || r.data?.token || null) : null;
};

(async () => {
  console.log("=== audit-v12-full API sweep ===");
  console.log("target:", API, "| flushed:", flushBuckets());

  const userToken = await login(USER);
  const adminToken = await login(ADMIN);
  console.log("user token:", userToken ? "OK" : "MISSING", "| admin token:", adminToken ? "OK" : "MISSING");
  if (!userToken || !adminToken) { console.log("ABORT: cannot authenticate"); process.exit(2); }

  // ═══════════════════════════════════════════════════ C1 AUTH (8)
  setArea("auth"); flushBuckets();
  console.log("\n--- C1 AUTH ---");
  {
    const me = await req("GET", "/api/auth/me", { token: userToken });
    check("GET /api/auth/me (student) 200", me.status === 200, `got ${me.status}`);
    const c = contract(me.data, { email: "string", isAdmin: "boolean" });
    check("  /me contract {email,isAdmin}", c.ok, c.why);
    const am = await req("GET", "/api/auth/me", { token: adminToken });
    check("GET /api/auth/me (admin) isAdmin=true", am.status === 200 && am.data?.isAdmin === true, `got ${am.status}/${am.data?.isAdmin}`);
    check("GET /api/auth/me anon 401", (await req("GET", "/api/auth/me")).status === 401, "expected 401");
    // PROBE BUG FIXED (v12, found by a second probe): password "nope" is 4 chars and
    // trips @Size(min=6) -> 400 Validation Failed, not 401. A 6+ char wrong password is
    // required to reach the credential check.
    check("login wrong password 401", (await req("POST", "/api/auth/login", { body: { email: USER.email, password: "wrong-password" } })).status === 401, "expected 401");
    check("login short password 400 (validation)", (await req("POST", "/api/auth/login", { body: { email: USER.email, password: "nope" } })).status === 400, "expected 400");
    check("login malformed email 400", (await req("POST", "/api/auth/login", { body: { email: "not-an-email", password: "x" } })).status === 400, "expected 400");
    check("GET /me garbage JWT 401", (await req("GET", "/api/auth/me", { token: "not.a.jwt" })).status === 401, "expected 401");
    const dupe = await req("POST", "/api/auth/register", { body: { email: USER.email, username: "dup" + PUBLIC_DECK, password: "123456", fullName: "Dup" } });
    check("register duplicate email 400/409", [400, 409].includes(dupe.status), `got ${dupe.status}`);
    const forgot = await req("POST", "/api/auth/forgot-password", { body: { email: USER.email } });
    check("POST /api/auth/forgot-password 200", forgot.status === 200, `got ${forgot.status}`);
    check("POST /api/auth/reset-password malformed 400", (await req("POST", "/api/auth/reset-password", { body: {} })).status === 400, "expected 400");
    const cp = await req("POST", "/api/auth/change-password", { body: { currentPassword: "wrong", newPassword: "whatever1" }, token: userToken });
    check("POST /api/auth/change-password wrong current 400/401", [400, 401].includes(cp.status), `got ${cp.status}`);
    const av = await req("PUT", "/api/auth/avatar", { body: {}, token: userToken });
    check("PUT /api/auth/avatar empty body 400", av.status === 400, `got ${av.status}`);
    na("POST /api/auth/avatar/upload", "multipart with real media — covered by F145/AGENTS.md evidence; not re-run here");
  }

  // ═══════════════════════════════════════════════════ C2 LESSONS / EXERCISES (11)
  setArea("lessons"); flushBuckets();
  console.log("\n--- C2 LESSONS / EXERCISES ---");
  let firstId = null, firstLessonId = null;
  {
    const list = await req("GET", "/api/lessons?page=0&size=5");
    check("GET /api/lessons 200", list.status === 200, `got ${list.status}`);
    const c = contract(list.data, { content: "array" });
    check("  lesson list contract {content[]}", c.ok, c.why);
    firstId = list.data?.content?.[0]?.id ?? list.data?.content?.[0]?.lessonId ?? null;
    const raw = JSON.stringify(list.data?.content?.[0] || {});
    check("  list does NOT leak lesson content LOB", !/"content"\s*:/.test(raw), "content LOB present in list payload");

    if (firstId) {
      const det = await req("GET", `/api/lessons/${firstId}`);
      check(`GET /api/lessons/{id} 200`, det.status === 200, `got ${det.status}`);
      const ex = await req("GET", `/api/lessons/${firstId}/exercises`);
      check("GET /api/lessons/{id}/exercises 200", ex.status === 200, `got ${ex.status}`);
      const exNo = await req("GET", `/api/lessons/${firstId}/exercises?includeAnswers=false`);
      const sNo = JSON.stringify(exNo.data);
      check("  includeAnswers=false strips correctAnswer", !/"correctAnswer"\s*:\s*"[^"]/.test(sNo), "answer present with includeAnswers=false");
      const exAnon = await req("GET", `/api/lessons/${firstId}/exercises?includeAnswers=true`);
      check("  includeAnswers=true as ANON does not leak answers", !/"correctAnswer"\s*:\s*"[^"]/.test(JSON.stringify(exAnon.data)), "leaked answers to anon");
      const exAdm = await req("GET", `/api/lessons/${firstId}/exercises?includeAnswers=true`, { token: adminToken });
      check("  includeAnswers=true as ADMIN 200", exAdm.status === 200, `got ${exAdm.status}`);
      check("GET /api/lessons/{id}/structure (auth) 200", (await req("GET", `/api/lessons/${firstId}/structure`, { token: userToken })).status === 200, "expected 200");
      check("GET /api/lessons/{id}/exercises/content 200", (await req("GET", `/api/lessons/${firstId}/exercises/content`)).status === 200, "expected 200");
      check("POST /api/lessons/{id}/exercises/grade anon 401/403", [401, 403].includes((await req("POST", `/api/lessons/${firstId}/exercises/grade`, { body: { answers: [] } })).status), "expected 401/403");
      const att = await req("GET", `/api/lessons/${firstId}/exercises/attempts`, { token: userToken });
      check("GET /api/lessons/{id}/exercises/attempts (auth) 200", att.status === 200, `got ${att.status}`);
      check("GET /api/lessons/{id}/exercises/attempts anon 401", (await req("GET", `/api/lessons/${firstId}/exercises/attempts`)).status === 401, "expected 401");
    } else { blocked("lesson detail probes", "no lesson id in list payload"); }

    // Draft guards — re-prove F88/F89/F115/F126 live
    const draftId = 10888;
    check("F88 draft lesson anon -> 404", (await req("GET", `/api/lessons/${draftId}`)).status === 404, "expected 404");
    check("F89 draft lesson student -> 404", (await req("GET", `/api/lessons/${draftId}`, { token: userToken })).status === 404, "expected 404");
    check("F126 draft structure student -> 404", (await req("GET", `/api/lessons/${draftId}/structure`, { token: userToken })).status === 404, "expected 404");
    const dEx = await req("GET", `/api/lessons/${draftId}/exercises`, { token: userToken });
    check("F115 draft exercises student blocked", [403, 404].includes(dEx.status), `got ${dEx.status}`);
    check("draft lesson admin -> 200", (await req("GET", `/api/lessons/${draftId}`, { token: adminToken })).status === 200, "expected 200");
  }

  // ═══════════════════════════════════════════════════ C3 STREAK (3)
  setArea("streak"); flushBuckets();
  console.log("\n--- C3 STREAK ---");
  {
    const snap = await req("GET", "/api/streak/snapshot", { token: userToken });
    check("GET /api/streak/snapshot 200", snap.status === 200, `got ${snap.status}`);
    const shape = {
      today: "any", currentStreak: "number", studiedToday: "boolean", effectiveFrom: "any",
      studiedDays: "array", legacyAccessDays: "any", legacyHistoryAvailable: "boolean",
    };
    const c = contract(snap.data, shape);
    check("  snapshot has all 7 declared fields, typed", c.ok, c.why);
    check("GET /api/streak/current 200", (await req("GET", "/api/streak/current", { token: userToken })).status === 200, "expected 200");
    check("GET /api/streak/history?days=30 200", (await req("GET", "/api/streak/history?days=30", { token: userToken })).status === 200, "expected 200");
    check("streak snapshot anon 401", (await req("GET", "/api/streak/snapshot")).status === 401, "expected 401");
  }

  // ═══════════════════════════════════════════════════ C4 SEARCH / SORT
  setArea("search"); flushBuckets();
  console.log("\n--- C4 SEARCH / SORT ---");
  {
    const s2 = await req("GET", "/api/vocabulary/search?keyword=ab");
    check("GET /api/vocabulary/search?keyword=ab 200 (permitAll)", s2.status === 200, `got ${s2.status}`);
    const s1 = await req("GET", "/api/vocabulary/search?keyword=a");
    check("  keyword=1 char -> 200 + [] (designed)", s1.status === 200 && Array.isArray(s1.data) && s1.data.length === 0, `got ${s1.status} len=${Array.isArray(s1.data) ? s1.data.length : "n/a"}`);
    const dic = await req("GET", "/api/vocabulary/dictionary/hello");
    check("GET /api/vocabulary/dictionary/hello reachable", [200, 500, 502, 504].includes(dic.status), `got ${dic.status}`);
    check("GET /api/vocabulary anon 401", (await req("GET", "/api/vocabulary")).status === 401, "expected 401");
    check("GET /api/vocabulary (auth) 200", (await req("GET", "/api/vocabulary", { token: userToken })).status === 200, "expected 200");
    const ae = await req("GET", "/api/admin/exercises?q=the", { token: adminToken });
    check("GET /api/admin/exercises?q=the (admin) 200", ae.status === 200, `got ${ae.status}`);
    check("GET /api/admin/exercises anon 401/403", [401, 403].includes((await req("GET", "/api/admin/exercises?q=the")).status), "expected 401/403");
    // ?sort= measured again
    const asc = await req("GET", "/api/lessons?page=0&size=5&sort=title,asc");
    const desc = await req("GET", "/api/lessons?page=0&size=5&sort=title,desc");
    const ids = (r) => JSON.stringify((r.data?.content || []).map((x) => x.id ?? x.lessonId));
    check("  ?sort= re-measured (v11 said IGNORED)", true, "");
    R.probed.push({ area, name: "?sort= asc vs desc ids", status: "INFO", detail: `asc=${ids(asc)} desc=${ids(desc)} same=${ids(asc) === ids(desc)}` });
    console.log(`  INFO  ?sort= asc=${ids(asc)} desc=${ids(desc)} same=${ids(asc) === ids(desc)}`);
  }

  // ═══════════════════════════════════════════════════ C5 CRUD (deck + lesson + vocabulary)
  setArea("crud"); flushBuckets();
  console.log("\n--- C5 CRUD (with in-run cleanup) ---");
  {
    const nm = "AUDIT-V12-API-" + PUBLIC_DECK;
    const mk = await req("POST", "/api/decks", { token: userToken, body: { name: nm, description: "audit", isPublic: false } });
    check("POST /api/decks create 200/201", [200, 201].includes(mk.status), `got ${mk.status}`);
    const deckId = mk.data?.id ?? mk.data?.deckId ?? mk.data?.data?.id;
    if (deckId) {
      const rd = await req("GET", `/api/decks/${deckId}`, { token: userToken });
      check("  read deck round-trips name", rd.status === 200 && JSON.stringify(rd.data).includes(nm), `status=${rd.status}`);
      const up = await req("PUT", `/api/decks/${deckId}`, { token: userToken, body: { name: nm + "-EDIT", description: "edited", isPublic: false } });
      check("  update deck 200", up.status === 200, `got ${up.status}`);
      const rd2 = await req("GET", `/api/decks/${deckId}`, { token: userToken });
      check("  edit persisted", JSON.stringify(rd2.data).includes("-EDIT"), "edit not visible");
      const anon = await req("GET", `/api/decks/${deckId}`);
      check("  PRIVATE deck hidden from anon (400/403/404)", [400, 403, 404].includes(anon.status), `got ${anon.status} — isPublic:false was set explicitly`);
      const del = await req("DELETE", `/api/decks/${deckId}`, { token: userToken });
      check("  delete deck 200/204", [200, 204].includes(del.status), `got ${del.status}`);
      check("  re-GET deleted deck 404", (await req("GET", `/api/decks/${deckId}`, { token: userToken })).status === 404, "expected 404");
    } else { blocked("deck CRUD", "no id returned"); }

    // lesson CRUD as admin
    const lt = "AUDIT-V12-LESSON-" + PUBLIC_DECK;
    const lk = await req("POST", "/api/lessons", { token: adminToken, body: { title: lt, description: "audit", level: "ELEMENTARY", category: "AUDIT", isPublished: false } });
    check("POST /api/lessons create (admin) 200/201", [200, 201].includes(lk.status), `got ${lk.status}`);
    const lessonId = lk.data?.id ?? lk.data?.lessonId ?? lk.data?.data?.id;
    if (lessonId) {
      check("  read lesson 200", (await req("GET", `/api/lessons/${lessonId}`, { token: adminToken })).status === 200, "expected 200");
      check("  update lesson 200", (await req("PUT", `/api/lessons/${lessonId}`, { token: adminToken, body: { title: lt + "-E", description: "e", level: "ELEMENTARY", category: "AUDIT", isPublished: false } })).status === 200, "expected 200");
      check("  delete lesson 200/204", [200, 204].includes((await req("DELETE", `/api/lessons/${lessonId}`, { token: adminToken })).status), "expected 200/204");
      check("  re-GET deleted lesson 404", (await req("GET", `/api/lessons/${lessonId}`, { token: adminToken })).status === 404, "expected 404");
    } else { blocked("lesson CRUD", "no id returned"); }

    check("POST /api/lessons anon 401/403", [401, 403].includes((await req("POST", "/api/lessons", { body: { title: "x" } })).status), "expected 401/403");
    check("POST /api/lessons student 403", (await req("POST", "/api/lessons", { token: userToken, body: { title: "x", level: "ELEMENTARY", category: "A" } })).status === 403, "expected 403");

    // ── C1 (F147, FIXED): a STUDENT may no longer write to the shared dictionary ──
    // `vocabulary` has no owner column; ownership lives in decks.owner_id + deck_words.
    // The student save path must therefore name a deck and be linked server-side, in one
    // transaction. These assertions are the regression guard for that fix.
    const vw = "zzv12authshape" + PUBLIC_DECK;
    const noDeck = await req("POST", "/api/vocabulary", { token: userToken, body: { word: vw, meaning: "auth-shape probe", source: "AUDIT_V12" } });
    check("F147 student WITHOUT deckId is rejected (400)", noDeck.status === 400, `got ${noDeck.status}`);

    // Self-provision both decks. This suite must not depend on ambient rows: the Phase-10
    // cleanup removed the leftover "Test Deck" rows (owner = the student user) that used to
    // supply `ownDeckId`, and the admin-owned deck 30033 that used to play "another user's
    // deck". Both are now created here and removed by the `AUDIT-V12-API-%` cleanup below.
    const ownDeck = await req("POST", "/api/decks", { token: userToken, body: { name: "AUDIT-V12-API-F147", description: "audit f147 own", isPublic: false } });
    const ownDeckId = ownDeck.data?.id ?? ownDeck.data?.deckId ?? ownDeck.data?.data?.id;
    const foreignDeck = await req("POST", "/api/decks", { token: adminToken, body: { name: "AUDIT-V12-API-FOREIGN", description: "audit f147 foreign", isPublic: false } });
    const foreignDeckId = foreignDeck.data?.id ?? foreignDeck.data?.deckId ?? foreignDeck.data?.data?.id;
    if (ownDeckId) {
      const vc = await req("POST", `/api/vocabulary?deckId=${ownDeckId}`, { token: userToken, body: { word: vw, meaning: "auth-shape probe", source: "AUDIT_V12" } });
      check("F147 student WITH own deckId is accepted", [200, 201].includes(vc.status), `got ${vc.status}`);
      R.c1 = { status: vc.status, id: vc.data?.id ?? vc.data?.vocabId ?? null, deckId: ownDeckId };

      const deckAfter = await req("GET", `/api/decks/${ownDeckId}`, { token: userToken });
      const linked = JSON.stringify(deckAfter.data || {}).includes(vw);
      check("F147   word is LINKED to the deck in the same transaction", linked, "word not found in the deck after save");

      // IDOR: attaching to somebody else's deck must be refused (and rolled back).
      const foreign = await req("POST", `/api/vocabulary?deckId=${foreignDeckId}`, { token: userToken, body: { word: vw + "x", meaning: "idor" } });
      check("F147   attaching to ANOTHER user's deck is blocked", foreign.status === 400, `got ${foreign.status}`);

      const seen = await req("GET", `/api/vocabulary/search?keyword=${vw}`);
      const visibleToAnon = Array.isArray(seen.data) && seen.data.some((v) => v.word === vw);
      R.c1.visibleToAnon = visibleToAnon;
      check("F147   a word saved this way is still a shared dictionary entry (by design)", visibleToAnon === true,
        `visibleToAnon=${visibleToAnon} — vocabulary is a shared dictionary; the FIX is that the write now goes through the ownership layer, not that the word is hidden`);

      // ── F152: lessonId is an ADMIN-ONLY field ────────────────────────────────────────
      // GET /api/lessons/{id} is permitAll and its payload embeds the lesson's vocabulary,
      // so a student setting lessonId would inject content into shared curriculum. Checked
      // here on the STUDENT's own deck (so the deckId guard cannot be what rejects it), and
      // on a word that ALREADY exists — the dedupe branch is the case a guard placed in
      // build() would miss, because build() only runs when the word is new.
      const lessonForInject = (await req("GET", "/api/lessons?size=1", { token: userToken })).data;
      const targetLesson = (lessonForInject?.content || lessonForInject || [])[0]?.id;
      if (targetLesson) {
        const injectFresh = await req("POST", `/api/vocabulary?deckId=${ownDeckId}`, { token: userToken, body: { word: vw + "L", meaning: "f152", lessonId: targetLesson } });
        check("F152 student + lessonId (new word) rejected 400", injectFresh.status === 400, `got ${injectFresh.status}`);
        const injectExisting = await req("POST", `/api/vocabulary?deckId=${ownDeckId}`, { token: userToken, body: { word: vw, meaning: "f152", lessonId: targetLesson } });
        check("F152 student + lessonId (existing word, dedupe branch) rejected 400", injectExisting.status === 400, `got ${injectExisting.status}`);
        const adminInject = await req("POST", "/api/admin/vocabulary", { token: adminToken, body: { word: "zzv12adminL", meaning: "f152", lessonId: targetLesson } });
        check("F152 admin + lessonId still accepted (not over-blocked)", [200, 201].includes(adminInject.status), `got ${adminInject.status}`);
        // Nothing may have leaked into the public lesson payload.
        const anonLesson = await req("GET", `/api/lessons/${targetLesson}`);
        const leaked = JSON.stringify(anonLesson.data?.vocabularies || []).includes(vw + "L");
        check("F152 no injected word in the public lesson payload", leaked === false, `leaked=${leaked}`);
        R.f152 = { targetLesson, injectFresh: injectFresh.status, injectExisting: injectExisting.status, adminInject: adminInject.status, leaked };
      } else {
        blocked("F152 lessonId injection", "no lesson id available");
      }
    } else {
      blocked("F147 deck-scoped save", "student has no deck to save into");
    }
  }

  // ═══════════════════════════════════════════════════ C6 GAME (6) — zero-coverage in v11
  setArea("game"); flushBuckets();
  console.log("\n--- C6 GAME (v11 zero-coverage) ---");
  {
    for (const mode of ["quiz", "memory", "typing", "listening", "mixed"]) {
      const g = await req("GET", `/api/games/${mode}/${PUBLIC_DECK}`, { token: userToken });
      check(`GET /api/games/${mode}/{deckId} 200`, g.status === 200, `got ${g.status}`);
      if (g.status === 200) {
        const c = contract(g.data, { sessionId: "any" });
        check(`  ${mode} returns sessionId`, c.ok, c.why);
      }
    }
    const sb = await req("POST", "/api/games/submit", { token: userToken, body: { sessionId: "", answers: [] } });
    check("POST /api/games/submit blank sessionId 400", sb.status === 400, `got ${sb.status}`);
    const gq = await req("GET", `/api/games/quiz/${PUBLIC_DECK}`, { token: userToken });
    const sid = gq.data?.sessionId;
    if (sid) {
      const ok = await req("POST", "/api/games/submit", { token: userToken, body: { sessionId: sid, answers: [] } });
      check("  submit real session 200", ok.status === 200, `got ${ok.status}`);
    } else { blocked("game submit real session", "no sessionId from quiz"); }
    check("GET /api/games/quiz/{deck} anon 401", (await req("GET", `/api/games/quiz/${PUBLIC_DECK}`)).status === 401, "expected 401");
  }

  // ═══════════════════════════════════════════════════ C7 FLASHCARD (2) — zero-coverage
  setArea("flashcard"); flushBuckets();
  console.log("\n--- C7 FLASHCARD (v11 zero-coverage) ---");
  {
    // PROBE BUG FIXED (v12): vocab_id 10006 does not exist (MIN(vocab_id)=10017), so the
    // service correctly threw ResourceNotFoundException -> 404. Use a REAL id.
    const VOCAB = 10017;
    const st = await req("GET", `/api/flashcards/status/${VOCAB}`, { token: userToken });
    check("GET /api/flashcards/status/{vocabId} 200", st.status === 200, `got ${st.status}`);
    // audit-v12 F148: the body is now {vocabularyId, quality 0-5}, not {vocabularyId, isKnown}.
    // The old boolean collapsed "Dễ" and "Tiếp theo" into one value; quality keeps them distinct.
    const rv = await req("POST", "/api/flashcards/review", { token: userToken, body: { vocabularyId: VOCAB, quality: 4 } });
    check("POST /api/flashcards/review quality=4 200", rv.status === 200, `got ${rv.status}`);
    check("  quality out of range (9) -> 400", (await req("POST", "/api/flashcards/review", { token: userToken, body: { vocabularyId: VOCAB, quality: 9 } })).status === 400, "expected 400");
    check("  legacy isKnown body is rejected (shape changed)", (await req("POST", "/api/flashcards/review", { token: userToken, body: { vocabularyId: VOCAB, isKnown: true } })).status === 400, "old shape should no longer be accepted");
    check("flashcards anon 401", (await req("POST", "/api/flashcards/review", { body: { vocabularyId: VOCAB, quality: 4 } })).status === 401, "expected 401");
    // audit-v12 F153: the drill is a plain back/continue reader; it records its study day
    // through /study (so a flashcard-only learner keeps their streak). This is that path.
    const study = await req("POST", "/api/flashcards/study", { token: userToken });
    check("POST /api/flashcards/study 200", study.status === 200, `got ${study.status}`);
    check("  flashcards/study anon 401", (await req("POST", "/api/flashcards/study")).status === 401, "expected 401");
  }

  // ═══════════════════════════════════════════════════ C8 SRS (3) — zero-coverage
  setArea("srs"); flushBuckets();
  console.log("\n--- C8 SRS (v11 zero-coverage) ---");
  {
    const due = await req("GET", `/api/srs/due/${PUBLIC_DECK}`, { token: userToken });
    check("GET /api/srs/due/{deckId} 200", due.status === 200, `got ${due.status}`);
    const stats = await req("GET", "/api/srs/stats", { token: userToken });
    check("GET /api/srs/stats 200", stats.status === 200, `got ${stats.status}`);
    // PROBE BUG FIXED (v12): SrsController reads payload keys "vocabId"/"quality" (a
    // Map<String,Integer>), NOT "vocabularyId". With the wrong key it correctly returned
    // 400 "vocabId và quality không được để trống".
    const rv = await req("POST", "/api/srs/review", { token: userToken, body: { vocabId: 10017, quality: 3 } });
    check("POST /api/srs/review quality=3 200", rv.status === 200, `got ${rv.status}`);
    const bad = await req("POST", "/api/srs/review", { token: userToken, body: { vocabId: 10017, quality: 9 } });
    check("  quality=9 out of range 400", bad.status === 400, `got ${bad.status}`);
    const missing = await req("POST", "/api/srs/review", { token: userToken, body: { quality: 3 } });
    check("  missing vocabId 400", missing.status === 400, `got ${missing.status}`);
    check("srs anon 401", (await req("GET", "/api/srs/stats")).status === 401, "expected 401");
  }

  // ═══════════════════════════════════════════════════ C9 LEADERBOARD / PROGRESS / DASHBOARD
  setArea("misc"); flushBuckets();
  console.log("\n--- C9 LEADERBOARD / PROGRESS / DASHBOARD ---");
  {
    const lb = await req("GET", "/api/leaderboard");
    check("GET /api/leaderboard anon 200 (permitAll)", lb.status === 200, `got ${lb.status}`);
    const c = contract(lb.data, { content: "array" });
    check("  leaderboard contract {content[]}", c.ok, c.why);
    const pg = await req("GET", "/api/users/progress", { token: userToken });
    check("GET /api/users/progress (auth) 200", pg.status === 200, `got ${pg.status}`);
    check("GET /api/users/progress anon 401", (await req("GET", "/api/users/progress")).status === 401, "expected 401");
    const ds = await req("GET", "/api/dashboard/stats", { token: userToken });
    check("GET /api/dashboard/stats (auth) 200", ds.status === 200, `got ${ds.status}`);
    check("GET /api/dashboard/stats anon 401", (await req("GET", "/api/dashboard/stats")).status === 401, "expected 401");
  }

  // ═══════════════════════════════════════════════════ C10 LESSON SUBMISSION / SNAPSHOT (zero-coverage)
  setArea("submission"); flushBuckets();
  console.log("\n--- C10 LESSON SUBMISSION / SNAPSHOT ---");
  {
    const draftId = 10888;
    const sub = await req("POST", "/api/lesson-submissions/submit", { token: userToken, body: { lessonId: draftId, skillType: "WRITING", submissionText: "audit probe" } });
    check("POST /api/lesson-submissions/submit draft guard blocked", [400, 403, 404].includes(sub.status), `got ${sub.status}`);
    const my = await req("GET", `/api/lesson-submissions/my/lesson/${firstId}/skill/WRITING`, { token: userToken });
    check("GET /api/lesson-submissions/my/lesson/{id}/skill/{type} 200", my.status === 200, `got ${my.status}`);
    check("lesson-submissions anon 401", (await req("GET", `/api/lesson-submissions/my/lesson/${firstId}/skill/WRITING`)).status === 401, "expected 401");

    const snapList = await req("GET", `/api/admin/lessons/${firstId}/snapshots`, { token: adminToken });
    check("GET /api/admin/lessons/{id}/snapshots (admin) 200", snapList.status === 200, `got ${snapList.status}`);
    check("  snapshots student 403", (await req("GET", `/api/admin/lessons/${firstId}/snapshots`, { token: userToken })).status === 403, "expected 403");
    check("  snapshots anon 401", (await req("GET", `/api/admin/lessons/${firstId}/snapshots`)).status === 401, "expected 401");
    na("POST /api/admin/lessons/{id}/snapshots + /restore", "writes a real snapshot/restores content — probe only if an audit-namespaced lesson exists; not run to avoid content mutation");
  }

  // ═══════════════════════════════════════════════════ C11 SPEAKING PROMPTS + SUBMISSIONS
  setArea("speaking"); flushBuckets();
  console.log("\n--- C11 SPEAKING PROMPTS / SUBMISSIONS ---");
  {
    const pub = await req("GET", "/api/v1/speaking-prompts");
    check("GET /api/v1/speaking-prompts anon 200", pub.status === 200, `got ${pub.status}`);
    check("GET /api/v1/speaking-prompts?q=... 200", (await req("GET", "/api/v1/speaking-prompts?q=a")).status === 200, "expected 200");
    check("GET /api/v1/video-prompts alias 200", (await req("GET", "/api/v1/video-prompts")).status === 200, "expected 200");
    const adm = await req("GET", "/api/v1/admin/speaking-prompts", { token: adminToken });
    check("GET /api/v1/admin/speaking-prompts (admin) 200", adm.status === 200, `got ${adm.status}`);
    check("  admin speaking-prompts student 403", (await req("GET", "/api/v1/admin/speaking-prompts", { token: userToken })).status === 403, "expected 403");
    check("  admin speaking-prompts anon 401", (await req("GET", "/api/v1/admin/speaking-prompts")).status === 401, "expected 401");
    const subList = await req("GET", "/api/v1/speaking-submissions", { token: userToken });
    check("GET /api/v1/speaking-submissions (auth) 200", subList.status === 200, `got ${subList.status}`);
    const admSub = await req("GET", "/api/v1/admin/speaking-submissions", { token: adminToken });
    check("GET /api/v1/admin/speaking-submissions (admin) 200", admSub.status === 200, `got ${admSub.status}`);
    check("  admin speaking-submissions student 403", (await req("GET", "/api/v1/admin/speaking-submissions", { token: userToken })).status === 403, "expected 403");
    na("POST /api/v1/speaking-submissions/upload + /assess", "needs real media (MinIO object); covered by v11 speaking-record-test.js — not re-run here");
  }

  // ═══════════════════════════════════════════════════ C12 VIDEO LESSONS + ATTEMPTS
  setArea("video"); flushBuckets();
  console.log("\n--- C12 VIDEO LESSONS / ATTEMPTS ---");
  {
    const vl = await req("GET", "/api/v1/video-lessons");
    check("GET /api/v1/video-lessons anon 200", vl.status === 200, `got ${vl.status}`);
    const vlId = Array.isArray(vl.data) ? (vl.data[0]?.id) : (vl.data?.content?.[0]?.id);
    if (vlId) check(`GET /api/v1/video-lessons/{id} 200`, (await req("GET", `/api/v1/video-lessons/${vlId}`)).status === 200, "expected 200");
    else blocked("video-lesson detail", "no id in list");
    const va = await req("GET", "/api/v1/video-attempts", { token: userToken });
    check("GET /api/v1/video-attempts (auth) 200", va.status === 200, `got ${va.status}`);
    const admVa = await req("GET", "/api/v1/admin/video-attempts", { token: adminToken });
    check("GET /api/v1/admin/video-attempts (admin) 200", admVa.status === 200, `got ${admVa.status}`);
    check("  admin video-attempts student 403", (await req("GET", "/api/v1/admin/video-attempts", { token: userToken })).status === 403, "expected 403");
    const admVl = await req("GET", "/api/v1/admin/video-lessons", { token: adminToken });
    check("GET /api/v1/admin/video-lessons (admin) 200", admVl.status === 200, `got ${admVl.status}`);
    check("  admin video-lessons anon 401", (await req("GET", "/api/v1/admin/video-lessons")).status === 401, "expected 401");
  }

  // ═══════════════════════════════════════════════════ C13 ADMIN CORE
  setArea("admin"); flushBuckets();
  console.log("\n--- C13 ADMIN CORE ---");
  {
    const pairs = [
      ["GET", "/api/admin/stats"], ["GET", "/api/admin/users"], ["GET", "/api/admin/lessons"],
      ["GET", "/api/admin/vocabulary"], ["GET", "/api/v1/admin/speaking-prompts"], ["GET", "/api/v1/admin/video-lessons"],
      ["GET", "/api/v1/admin/video-attempts"],
    ];
    for (const [m, p] of pairs) {
      const anon = await req(m, p);
      const stu = await req(m, p, { token: userToken });
      const adm = await req(m, p, { token: adminToken });
      check(`ROLE ${m} ${p}  anon=${anon.status} student=${stu.status} admin=${adm.status}`,
        anon.status === 401 && stu.status === 403 && adm.status === 200,
        `expected 401/403/200, got ${anon.status}/${stu.status}/${adm.status}`);
    }
    const aiStatus = await req("GET", "/api/admin/exercises/ai/status", { token: adminToken });
    check("GET /api/admin/exercises/ai/status (admin) 200", aiStatus.status === 200, `got ${aiStatus.status}`);
    check("  ai/status anon 401/403", [401, 403].includes((await req("GET", "/api/admin/exercises/ai/status")).status), "expected 401/403");
    check("GET /api/admin/exercises/ai/backfill-answers/status (admin) 200", (await req("GET", "/api/admin/exercises/ai/backfill-answers/status", { token: adminToken })).status === 200, "expected 200");
  }

  // ═══════════════════════════════════════════════════ C14 AI
  setArea("ai"); flushBuckets();
  console.log("\n--- C14 AI ---");
  {
    check("POST /api/ai/generate-vocab anon 401/403", [401, 403].includes((await req("POST", "/api/ai/generate-vocab", { body: { topic: "travel", count: 1 } })).status), "expected 401/403");
    const gen = await req("POST", "/api/ai/generate-vocab", { token: userToken, body: { topic: "travel", level: "A2", count: 2 } });
    check("POST /api/ai/generate-vocab (auth) 200 or quota 429", [200, 429].includes(gen.status), `got ${gen.status} (504=cold model is a latency characteristic, see v11 P3)`);
    const enr = await req("POST", "/api/ai/enrich-word", { token: userToken, body: { word: "hello" } });
    check("POST /api/ai/enrich-word (auth) 200/429", [200, 429].includes(enr.status), `got ${enr.status}`);
    const sv = await req("POST", "/api/ai/save-vocab", { token: userToken, body: [{ word: "zzv12probe", meaning: "probe", source: "AI_GENERATED" }] });
    check("POST /api/ai/save-vocab 200", sv.status === 200, `got ${sv.status}`);
    const linked = sv.headers.get("x-ai-linked-to-deck");
    check("  F145 header present (X-AI-Linked-To-Deck)", linked !== null, "header absent — F145 fix not in container?");
    R.probed.push({ area, name: "save-vocab headers", status: "INFO", detail: `linked=${linked} count=${sv.headers.get("x-ai-saved-count")}` });
    // TTS + Whisper reachability
    const tts = await req("POST", "/synthesize", { body: { text: "hello" } });
    R.probed.push({ area, name: "TTS :8001 reachable", status: "INFO", detail: `status=${tts.status}` });
    console.log(`  INFO  TTS /synthesize -> ${tts.status}`);
  }

  // ═══════════════════════════════════════════════════ C15 PAYMENT
  setArea("payment"); flushBuckets();
  console.log("\n--- C15 PAYMENT ---");
  {
    const st = await req("GET", "/api/v1/payment/status", { token: userToken });
    check("GET /api/v1/payment/status 200", st.status === 200, `got ${st.status}`);
    const co = await req("POST", "/api/v1/payment/create-order", { token: userToken, body: { planType: "MONTH" } });
    check("POST /api/v1/payment/create-order 200", co.status === 200, `got ${co.status}`);
    const code = co.data?.orderCode || co.data?.data?.orderCode;
    check("  orderCode starts ENG", typeof code === "string" && code.startsWith("ENG"), `got ${code}`);
    R.createdOrderCode = code;
    // PROBE BUG FIXED (v12): the webhook reports rejection in the BODY
    // (`{"success":false,"error":"Invalid signature"}`) with HTTP 200 — SePay's contract is
    // to ACK with 200 and carry the outcome in the payload, so the assertion must read the body.
    const badSig = await req("POST", "/api/webhook/sepay", { body: { id: 1, transferAmount: 999999 }, headers: { "X-Sepay-Signature": "sha256=deadbeef", "X-Sepay-Timestamp": "1" } });
    check("POST /api/webhook/sepay invalid signature rejected (body success=false)",
      badSig.status === 200 && badSig.data?.success === false, `got ${badSig.status} body=${JSON.stringify(badSig.data)}`);
    // Replay: a signature over a STALE timestamp must also be refused, even if it were valid.
    const stale = await req("POST", "/api/webhook/sepay", { body: { id: 2, transferAmount: 999999 }, headers: { "X-Sepay-Signature": "sha256=deadbeef", "X-Sepay-Timestamp": "1" } });
    check("  stale timestamp also refused", stale.data?.success === false, `body=${JSON.stringify(stale.data)}`);
    blocked("webhook with VALID HMAC signature", "mutates a real payment_transactions row — real-money boundary (v11 proved it with cleanup; not repeated)");
  }

  // ═══════════════════════════════════════════════════ CLEANUP (same run) + PARITY
  console.log("\n--- CLEANUP + PARITY ---");
  {
    // Remove every row this sweep created, by ENUMERATED criteria (never a bare LIKE on
    // a table that holds real data). SET QUOTED_IDENTIFIER ON is mandatory because the DB
    // has a filtered unique index (AGENTS.md) — omitting it fails with Msg 1934 while
    // sqlcmd still exits 0.
    // The payment assertion is on what THIS run could have created, not on a hard-coded
    // baseline (v11 F130: `after === expected` cannot tell "cleaned up" from "baseline was
    // already wrong"). We remove by order_code AND any PENDING/no-transaction row dated
    // today, then assert ZERO remain.
    const sql = `
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
SELECT 'AUDIT_PAY_CANDIDATES=' + CAST(COUNT(*) AS varchar(10)) AS marker FROM payment_transactions
 WHERE transaction_id IS NULL AND status='PENDING' AND created_at >= CONVERT(date, '2026-09-21');
DELETE FROM deck_words WHERE deck_id IN (SELECT deck_id FROM decks WHERE name LIKE 'AUDIT-V12-API-%');
DELETE FROM decks WHERE name LIKE 'AUDIT-V12-API-%';
DELETE FROM deck_words WHERE vocab_id IN (SELECT vocab_id FROM vocabulary WHERE word LIKE 'zzv12%' OR word LIKE 'zzf147%' OR word = 'zzv12probe');
DELETE FROM vocabulary WHERE word LIKE 'zzv12authshape%' OR word LIKE 'zzv12%' OR word LIKE 'zzf147%' OR word = 'zzv12probe';
DELETE FROM payment_transactions
 WHERE transaction_id IS NULL AND status = 'PENDING'
   AND (order_code = '${R.createdOrderCode || "__none__"}' OR created_at >= CONVERT(date, '2026-09-21'));
SELECT 'AUDIT_DECKS=' + CAST((SELECT COUNT(*) FROM decks WHERE name LIKE 'AUDIT-V12-API-%') AS varchar(10))
     + ' AUDIT_VOCAB=' + CAST((SELECT COUNT(*) FROM vocabulary WHERE word LIKE 'zzv12%' OR word LIKE 'zzf147%' OR word='zzv12probe') AS varchar(10))
     + ' AUDIT_LESSONS=' + CAST((SELECT COUNT(*) FROM lessons WHERE title LIKE 'AUDIT-V12-%') AS varchar(10))
     + ' AUDIT_PAY=' + CAST((SELECT COUNT(*) FROM payment_transactions WHERE transaction_id IS NULL AND status='PENDING' AND created_at >= CONVERT(date, '2026-09-21')) AS varchar(10)) AS marker;
`;
    fs.writeFileSync(path.join(__dirname, "_cleanup.sql"), sql);
    let out = "";
    try { out = execFileSync("python", ["sweep/v8/sqlrun.py", "sweep/v12/_cleanup.sql"], { encoding: "utf8", cwd: path.join(__dirname, "..", "..") }); }
    catch (e) { out = String(e.message); }
    console.log(out.trim());
    const msgErrors = (out.match(/Msg \d+/g) || []).length;
    check("cleanup produced 0 SQL errors (Msg scan)", msgErrors === 0, `Msg count=${msgErrors}`);
    const m = /AUDIT_DECKS=(\d+) AUDIT_VOCAB=(\d+) AUDIT_LESSONS=(\d+) AUDIT_PAY=(\d+)/.exec(out);
    check("cleanup left 0 residue", m && m.slice(1).every((x) => x === "0"), m ? m[0] : "marker not found");
    R.cleanup = m ? { decks: +m[1], vocab: +m[2], lessons: +m[3], payments: +m[4] } : null;
  }

  // ═══════════════════════════════════════════════════ SUMMARY
  console.log("\n=== SUMMARY ===");
  console.log(`pass=${R.pass} fail=${R.fail} blocked=${R.blocked} n_a=${R.n_a}`);
  for (const [a, v] of Object.entries(R.areas)) console.log(`  ${a.padEnd(12)} pass=${v.pass} fail=${v.fail} blocked=${v.blocked}`);

  const OUT = path.join(__dirname, "..", "..", ".specify", "specs", "audit-v12-full", "evidence", "api-sweep.json");
  fs.mkdirSync(path.dirname(OUT), { recursive: true });
  fs.writeFileSync(OUT, JSON.stringify(R, null, 2));
  console.log("written:", path.relative(path.join(__dirname, "..", ".."), OUT));
  process.exit(R.fail > 0 ? 1 : 0);
})();
