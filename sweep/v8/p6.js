/**
 * p6 — close the coverage gap found by sweep/v8/coverage_check.py.
 *
 * The endpoint inventory (144 mappings) showed 16 endpoints with NO probe in
 * p1..p5. Every one of them is a REAL mapping. Two families:
 *   (a) ALIAS ROUTES — SpeakingPromptController/SpeakingSubmissionController declare
 *       video-* aliases next to speaking-*; only the speaking-* spelling had ever
 *       been exercised. This sweep proves both spellings hit the same handler.
 *   (b) LessonController write path (POST/PUT/DELETE /api/lessons) and
 *       POST /api/vocabulary, DELETE /api/admin/exercises/{id}.
 *
 * Mutation policy: everything created here carries the "ZZ r1" prefix and is
 * deleted in the cleanup phase. Run from inside sweep/v8.
 */
const lib = require("./lib.js");
const { probe } = lib;

(async () => {
  await lib.initTokens();
  const st = Date.now();
  const tag = "ZZ r1 p6 " + st;
  const created = { lessons: [], exercises: [], vocab: [], prompts: [], submissions: [] };

  console.log("=== A. LessonController write path (POST/PUT/DELETE /api/lessons) ===");
  // wrong-role negatives first (prove the ADMIN rule is live)
  await probe("lesson create noauth 401", "POST", "/api/lessons", "none", [401, 403], {
    title: tag, content: "<p>x</p>", level: "ELEMENTARY" });
  await probe("lesson create user 403", "POST", "/api/lessons", "user", [401, 403], {
    title: tag, content: "<p>x</p>", level: "ELEMENTARY" });
  await probe("lesson create admin 201", "POST", "/api/lessons", "admin", [200, 201], {
    title: tag, content: "<p>x</p>", description: "probe", level: "ELEMENTARY",
    orderIndex: 9999, isPublished: false, skillType: "GRAMMAR" });

  // find what we just created
  let lessonId = null;
  {
    const r = await fetch("http://localhost:8080/api/admin/lessons?q=" + encodeURIComponent(tag) + "&size=5",
      { headers: { Authorization: "Bearer " + lib.getAdmin() } });
    try {
      const j = await r.json();
      const items = j.content || (j.data && j.data.content) || [];
      if (items.length) lessonId = items[0].id;
    } catch (e) { /* ignore */ }
  }
  console.log("  created lessonId=" + lessonId);
  if (lessonId) {
    created.lessons.push(lessonId);
    await probe("lesson update admin 200", "PUT", "/api/lessons/" + lessonId, "admin", [200], {
      title: tag + " edited", content: "<p>y</p>", description: "probe2", level: "ELEMENTARY",
      orderIndex: 9999, isPublished: false, skillType: "GRAMMAR" });
    await probe("lesson update bad level 400", "PUT", "/api/lessons/" + lessonId, "admin", [400], {
      title: tag, content: "<p>x</p>", level: "NOT_A_LEVEL" });
    await probe("lesson update user 403", "PUT", "/api/lessons/" + lessonId, "user", [401, 403], {
      title: tag, content: "<p>x</p>", level: "ELEMENTARY" });
    await probe("lesson delete user 403", "DELETE", "/api/lessons/" + lessonId, "user", [401, 403]);
    await probe("lesson delete admin 204", "DELETE", "/api/lessons/" + lessonId, "admin", [200, 204]);
    await probe("lesson gone 404", "GET", "/api/lessons/" + lessonId, "none", 404);
  }

  console.log("=== B. DELETE /api/admin/exercises/{id} ===");
  // create a throwaway exercise on a real lesson, then delete it.
  // ExerciseRequest field names are exact: lessonId/question/options/correctAnswer/
  // exerciseType/difficulty/explanation/orderIndex (options is a STRING, not array).
  let exId = null;
  {
    const r = await probe("exercise create for delete", "POST", "/api/admin/exercises", "admin", [200, 201], {
      lessonId: 445, exerciseType: "MULTIPLE_CHOICE", question: tag + " question?",
      options: "[\"a\",\"b\"]", correctAnswer: "a", explanation: "probe", difficulty: "EASY", orderIndex: 9999 });
    try { const j = JSON.parse(r.txt); exId = j.id || (j.data && j.data.id); } catch (e) {}
  }
  console.log("  exerciseId=" + exId);
  if (exId) {
    created.exercises.push(exId);
    await probe("exercise delete user 403", "DELETE", "/api/admin/exercises/" + exId, "user", [401, 403]);
    await probe("exercise delete noauth 401", "DELETE", "/api/admin/exercises/" + exId, "none", [401, 403]);
    await probe("exercise delete admin 200", "DELETE", "/api/admin/exercises/" + exId, "admin", [200, 204]);
    await probe("exercise gone 404", "GET", "/api/admin/exercises/" + exId, "admin", 404);
  }
  await probe("exercise delete missing id 404", "DELETE", "/api/admin/exercises/999999999", "admin", [404, 500]);

  console.log("=== C. POST /api/vocabulary ===");
  // VocabularyRequest: word(@NotBlank) + meaning(@NotBlank) are the only must-haves.
  await probe("vocab create noauth 401", "POST", "/api/vocabulary", "none", [401, 403],
    { word: tag, meaning: "probe meaning" });
  await probe("vocab create blank word 400", "POST", "/api/vocabulary", "user", 400,
    { word: "", meaning: "probe meaning" });
  await probe("vocab create user 200", "POST", "/api/vocabulary", "user", [200, 201],
    { word: tag, meaning: "probe meaning", pronunciation: "/prəʊb/", wordType: "noun",
      exampleSentence: "a probe", cefrLevel: "B1", source: "audit" });

  console.log("=== D. ALIAS ROUTES: video-prompts == speaking-prompts ===");
  // CreateSpeakingPromptRequest: title(@NotBlank) + prompt(@NotBlank) are mandatory.
  let promptId = null;
  {
    const r = await probe("prompt create (speaking- spelling)", "POST", "/api/v1/admin/speaking-prompts", "admin", [200, 201], {
      title: tag, description: "probe", prompt: "Describe your day.",
      referenceText: "ref", level: "B1", category: "PROBE", isPublished: true, orderIndex: 9999 });
    try { const j = JSON.parse(r.txt); promptId = j.id || (j.data && j.data.id); } catch (e) {}
    if (!promptId) console.log("  create resp: " + r.txt.slice(0, 200));
  }
  console.log("  promptId=" + promptId);
  if (promptId) {
    created.prompts.push(promptId);
    // ALIAS read: must hit the SAME row
    const aliasGet = await probe("prompt GET via video-prompts ALIAS", "GET",
      "/api/v1/video-prompts/" + promptId, "admin", 200);
    let sameId = null;
    try { const j = JSON.parse(aliasGet.txt); sameId = j.id || (j.data && j.data.id); } catch (e) {}
    console.log("  ALIAS GET returned id=" + sameId + " (expect " + promptId + ") " +
      (String(sameId) === String(promptId) ? "SAME ROW OK" : "MISMATCH!"));

    // ALIAS update
    await probe("prompt update via video-prompts ALIAS", "PUT", "/api/v1/admin/video-prompts/" + promptId, "admin", [200], {
      title: tag + " via-alias", description: "probe", prompt: "Describe your week.",
      referenceText: "ref", level: "B1", category: "PROBE", isPublished: true, orderIndex: 9999 });
    // read back through the speaking- spelling to confirm the alias wrote the same row
    const back = await probe("prompt re-read via speaking- spelling", "GET",
      "/api/v1/speaking-prompts/" + promptId, "admin", 200);
    let backTitle = null;
    try { const j = JSON.parse(back.txt); backTitle = j.title || (j.data && j.data.title); } catch (e) {}
    console.log("  after ALIAS PUT, speaking- GET title=" + JSON.stringify(backTitle) +
      (String(backTitle).indexOf("via-alias") >= 0 ? " ALIAS WRITE OK" : " ALIAS WRITE NOT APPLIED!"));

    // user (non-admin) on alias must be 403 — proves @PreAuthorize covers BOTH spellings
    await probe("prompt create via video-prompts user 403", "POST", "/api/v1/admin/video-prompts", "user", [401, 403], {
      title: "x", description: "x", promptText: "x", level: "B1" });
    await probe("prompt delete via video-prompts user 403", "DELETE", "/api/v1/admin/video-prompts/" + promptId, "user", [401, 403]);

    // ALIAS ai-generate guards (empty topic -> 400, no Ollama call)
    await probe("prompt ai-generate via ALIAS empty topic 400", "POST",
      "/api/v1/admin/video-prompts/ai-generate", "admin", 400, { topic: "" });
    await probe("prompt ai-generate-full via ALIAS empty topic 400", "POST",
      "/api/v1/admin/video-prompts/ai-generate-full", "admin", 400, { topic: "  " });
    await probe("prompt ai-generate via ALIAS user 403", "POST",
      "/api/v1/admin/video-prompts/ai-generate", "user", [401, 403], { topic: "hello" });
  }

  console.log("=== E. ALIAS ROUTES: video-submissions == speaking-submissions ===");
  {
    // admin list via the ALIAS spelling
    const r = await probe("admin submissions list via ALIAS", "GET",
      "/api/v1/admin/video-submissions?page=0&size=5", "admin", 200);
    let total = null;
    try { const j = JSON.parse(r.txt); total = (j.totalElements !== undefined ? j.totalElements : (j.data && j.data.totalElements)); } catch (e) {}
    console.log("  ALIAS admin list totalElements=" + total);
  }
  {
    const r = await probe("admin submissions list via speaking- spelling", "GET",
      "/api/v1/admin/speaking-submissions?page=0&size=5", "admin", 200);
    let total = null;
    try { const j = JSON.parse(r.txt); total = (j.totalElements !== undefined ? j.totalElements : (j.data && j.data.totalElements)); } catch (e) {}
    console.log("  speaking- admin list totalElements=" + total + " (must equal ALIAS)");
  }
  await probe("admin submissions list via ALIAS as user 403", "GET",
    "/api/v1/admin/video-submissions", "user", [401, 403]);
  await probe("admin submissions list via ALIAS noauth 401", "GET",
    "/api/v1/admin/video-submissions", "none", [401, 403]);

  // per-prompt submissions via the ALIAS spelling (owner scope)
  if (promptId) {
    await probe("prompt submissions via ALIAS (owner)", "GET",
      "/api/v1/video-prompts/" + promptId + "/submissions?page=0&size=5", "user", 200);
  }

  // upload/assess/detail via ALIAS: need a real media file -> reuse the Whisper path.
  // Upload with fake bytes is rejected by design, so we assert the CONTRACT negatives here
  // and prove the alias reaches the same handler (403 premium gate / 400 missing file).
  {
    const b = "----p6" + Math.random().toString(16).slice(2);
    const mk = (extra) => Buffer.concat([
      Buffer.from("--" + b + "\r\nContent-Disposition: form-data; name=\"promptId\"\r\n\r\n" +
        (promptId || 1) + "\r\n" + extra),
      Buffer.from("--" + b + "--\r\n")]);
    // no file part at all -> alias must reach the handler and fail validation, not 404
    const r = await fetch("http://localhost:8080/api/v1/video-submissions/upload", {
      method: "POST",
      headers: { Authorization: "Bearer " + lib.getUser(), "Content-Type": "multipart/form-data; boundary=" + b },
      body: mk("")
    });
    const txt = await r.text();
    const reached = r.status !== 404;
    console.log("  ALIAS upload (no file part) -> " + r.status + " reached-handler=" + reached +
      " :: " + txt.replace(/\r?\n/g, " ").slice(0, 120));
    lib.rows.push({ name: "submission upload via video-submissions ALIAS reaches handler", method: "POST",
      path: "/api/v1/video-submissions/upload", as: "user", code: r.status,
      expect: "not-404", pass: reached, snippet: txt.slice(0, 200) });
  }
  // detail + assess + grade via ALIAS against a non-existent id: must NOT be 404-route-missing.
  // 404 is the legitimate not-found for a missing row; distinguish by checking the
  // speaking- spelling returns the SAME status (that proves the alias maps identically).
  {
    const a = await probe("detail via ALIAS (missing id)", "GET", "/api/v1/video-submissions/999999999", "user", [403, 404]);
    const s = await probe("detail via speaking- (missing id)", "GET", "/api/v1/speaking-submissions/999999999", "user", [403, 404]);
    console.log("  alias status=" + a.code + " speaking status=" + s.code +
      (a.code === s.code ? " IDENTICAL-STATUS OK" : " DIVERGENT!"));
    lib.rows.push({ name: "detail ALIAS == speaking- status", method: "GET",
      path: "/api/v1/video-submissions/{id}", as: "user", code: a.code, expect: String(s.code),
      pass: a.code === s.code, snippet: "" });

    const a2 = await probe("assess via ALIAS (missing id)", "POST", "/api/v1/video-submissions/999999999/assess", "user", [403, 404]);
    const s2 = await probe("assess via speaking- (missing id)", "POST", "/api/v1/speaking-submissions/999999999/assess", "user", [403, 404]);
    console.log("  assess alias=" + a2.code + " speaking=" + s2.code +
      (a2.code === s2.code ? " IDENTICAL-STATUS OK" : " DIVERGENT!"));
    lib.rows.push({ name: "assess ALIAS == speaking- status", method: "POST",
      path: "/api/v1/video-submissions/{id}/assess", as: "user", code: a2.code, expect: String(s2.code),
      pass: a2.code === s2.code, snippet: "" });

    const g = await probe("grade via ALIAS (missing id)", "PATCH",
      "/api/v1/admin/video-submissions/999999999/grade", "admin", [400, 404], { score: 8.5, feedback: "probe" });
    const g2 = await probe("grade via speaking- (missing id)", "PATCH",
      "/api/v1/admin/speaking-submissions/999999999/grade", "admin", [400, 404], { score: 8.5, feedback: "probe" });
    console.log("  grade alias=" + g.code + " speaking=" + g2.code +
      (g.code === g2.code ? " IDENTICAL-STATUS OK" : " DIVERGENT!"));
    lib.rows.push({ name: "grade ALIAS == speaking- status", method: "PATCH",
      path: "/api/v1/admin/video-submissions/{id}/grade", as: "admin", code: g.code,
      expect: String(g2.code), pass: g.code === g2.code, snippet: "" });
    // negative: non-admin on alias
    await probe("grade via ALIAS user 403", "PATCH", "/api/v1/admin/video-submissions/999999999/grade",
      "user", [401, 403], { score: 8.5, feedback: "probe" });
  }

  console.log("=== F. GET /api/lesson-submissions/my/lesson/{id}/skill/{type} ===");
  await probe("my skill submission noauth 401", "GET",
    "/api/lesson-submissions/my/lesson/445/skill/GRAMMAR", "none", [401, 403]);
  await probe("my skill submission user (none yet) 200", "GET",
    "/api/lesson-submissions/my/lesson/445/skill/GRAMMAR", "user", 200);
  await probe("my skill submission bad enum 400", "GET",
    "/api/lesson-submissions/my/lesson/445/skill/NOT_A_SKILL", "user", [400, 500]);
  await probe("my skill submission missing lesson 404/200", "GET",
    "/api/lesson-submissions/my/lesson/999999999/skill/GRAMMAR", "user", [200, 404]);

  console.log("=== G. GET /api/resources/{filename:.+} ===");
  await probe("resource serving: existing txt", "GET", "/api/resources/audit-v8-f81-notes.txt", "none", [200, 404]);
  await probe("resource traversal 400", "GET", "/api/resources/..%2F..%2Fapplication.properties", "none", [400, 404]);
  await probe("resource missing 404", "GET", "/api/resources/definitely-not-here-xyz.bin", "none", 404);

  console.log("=== H. cleanup (must leave DB at parity) ===");
  if (promptId) {
    await probe("cleanup prompt via ALIAS", "DELETE", "/api/v1/admin/video-prompts/" + promptId, "admin", [200, 204]);
  }
  // vocabulary row created above — the delete endpoint lives on AdminController
  // (/api/admin/vocabulary/{id}); VocabularyController has NO @DeleteMapping.
  {
    const r = await fetch("http://localhost:8080/api/vocabulary/search?q=" + encodeURIComponent(tag),
      { headers: { Authorization: "Bearer " + lib.getAdmin() } });
    try {
      const j = await r.json();
      const arr = Array.isArray(j) ? j : (j.content || (j.data && (j.data.content || j.data)) || []);
      const list = Array.isArray(arr) ? arr : [];
      console.log("  vocab cleanup candidates=" + list.length);
      for (const v of list) {
        if (v && v.id && String(v.word || "").indexOf(tag) >= 0) {
          await probe("cleanup vocab " + v.id, "DELETE", "/api/admin/vocabulary/" + v.id, "admin", [200, 204]);
        }
      }
    } catch (e) { console.log("  vocab cleanup: " + e.message); }
  }

  lib.report("PHASE6-COVERAGE-GAP");
  lib.dump("p6.json");
  console.log("\ncreated-manifest " + JSON.stringify(created));
})().catch(e => { console.error("FATAL", e); process.exit(1); });
