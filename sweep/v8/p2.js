const lib = require("./lib.js");
const { probe, sleep } = lib;
const J = JSON.stringify;

(async () => {
  await lib.initTokens();
  const stamp = Date.now();

  // ---------- LESSON CRUD (admin) ----------
  let lessonId = null;
  let r = await probe("L create", "POST", "/api/admin/lessons", "admin", 200,
    { title: "ZZ v8 lesson " + stamp, content: "Probe lesson with **markdown** and enough length to pass.", level: "ELEMENTARY", category: "GRAMMAR", durationMinutes: 10, isPublished: false, orderIndex: 9999 });
  try { lessonId = JSON.parse(r.txt).id; } catch (e) {}
  console.log("  lessonId=" + lessonId);
  await probe("L read", "GET", "/api/admin/lessons/" + lessonId, "admin", 200);
  await probe("L update", "PUT", "/api/admin/lessons/" + lessonId, "admin", 200,
    { title: "ZZ v8 lesson " + stamp + "b", content: "Updated content for the probe lesson, still long enough.", level: "INTERMEDIATE", category: "GRAMMAR", durationMinutes: 12, isPublished: false, orderIndex: 9999 });
  await probe("L publish toggle", "PUT", "/api/admin/lessons/" + lessonId + "/toggle-publish", "admin", 200);
  await probe("L publish toggle back", "PUT", "/api/admin/lessons/" + lessonId + "/toggle-publish", "admin", 200);
  await probe("L user-forbidden", "PUT", "/api/admin/lessons/" + lessonId, "user", 403, { title: "nope", content: "nope nope nope", level: "ELEMENTARY" });

  // ---------- EXERCISE CRUD on the temp lesson ----------
  let exId = null;
  r = await probe("E create", "POST", "/api/admin/exercises", "admin", 200,
    { lessonId: lessonId, question: "Choose the correct option for the probe: 2 + 2 = ?", options: J(["5", "4", "9"]), correctAnswer: "4", exerciseType: "MULTIPLE_CHOICE", difficulty: "EASY", explanation: "basic arithmetic", orderIndex: 1 });
  try { exId = JSON.parse(r.txt).id; } catch (e) {}
  console.log("  exId=" + exId);
  await probe("E read", "GET", "/api/admin/exercises/" + exId, "admin", 200);
  await probe("E update", "PUT", "/api/admin/exercises/" + exId, "admin", 200,
    { lessonId: lessonId, question: "Choose the correct option for the probe: 2 + 3 = ?", options: J(["5", "4", "9"]), correctAnswer: "5", exerciseType: "MULTIPLE_CHOICE", difficulty: "EASY", explanation: "updated", orderIndex: 1 });
  await probe("E public content hides answer", "GET", "/api/lessons/" + lessonId + "/exercises/content", "none", 200);
  await probe("E public list hides answer", "GET", "/api/lessons/" + lessonId + "/exercises", "none", 200);
  await probe("E grade correct", "POST", "/api/lessons/" + lessonId + "/exercises/grade", "user", 200, { answers: [{ exerciseId: exId, userAnswer: "5" }] });
  await probe("E grade wrong", "POST", "/api/lessons/" + lessonId + "/exercises/grade", "user", 200, { answers: [{ exerciseId: exId, userAnswer: "9" }] });
  await probe("E submit persists", "POST", "/api/lessons/" + lessonId + "/exercises/submit", "user", 200, { answers: [{ exerciseId: exId, userAnswer: "5" }] });
  let attemptId = null;
  r = await probe("E attempts", "GET", "/api/lessons/" + lessonId + "/exercises/attempts", "user", 200);
  try { attemptId = JSON.parse(r.txt)[0].id; } catch (e) {}
  await probe("E attempt detail", "GET", "/api/lessons/" + lessonId + "/exercises/attempts/" + attemptId, "user", 200);
  await probe("E attempt detail other-user 4xx", "GET", "/api/lessons/" + lessonId + "/exercises/attempts/" + attemptId, "none", 401);

  // ---------- VOCABULARY CRUD ----------
  let vid = null;
  r = await probe("V create", "POST", "/api/admin/vocabulary", "admin", 200,
    { word: "zzprobe" + stamp, meaning: "probe meaning", exampleSentence: "A probe sentence.", cefrLevel: "B2", wordType: "NOUN" });
  try { vid = JSON.parse(r.txt).id || JSON.parse(r.txt).vocabId; } catch (e) {}
  console.log("  vocabId=" + vid);
  await probe("V update", "PUT", "/api/admin/vocabulary/" + vid, "admin", 200,
    { word: "zzprobe" + stamp, meaning: "probe meaning updated", exampleSentence: "Another probe sentence.", cefrLevel: "B2", wordType: "NOUN" });
  await probe("V search sees it", "GET", "/api/vocabulary/search?q=zzprobe" + stamp, "none", 200);
  await probe("V dup create 4xx", "POST", "/api/admin/vocabulary", "admin", [200, 400, 409],
    { word: "zzprobe" + stamp, meaning: "again", exampleSentence: "again", cefrLevel: "B2", wordType: "NOUN" });
  let vid2 = vid;
  r = await probe("V delete", "DELETE", "/api/admin/vocabulary/" + vid, "admin", [200, 204]);
  await probe("V delete twice 404", "DELETE", "/api/admin/vocabulary/" + vid2, "admin", 404);

  // ---------- SPEAKING PROMPT CRUD ----------
  let pid = null;
  r = await probe("SP create", "POST", "/api/v1/admin/speaking-prompts", "admin", 201,
    { title: "ZZ v8 prompt " + stamp, description: "probe", prompt: "Describe your favourite room for 60 seconds.", level: "B1", category: "DAILY", isPublished: true, isPremium: false, mode: "FREE_SPEAKING", maxDurationSeconds: 120, attemptLimit: 5, orderIndex: 999 });
  try { pid = JSON.parse(r.txt).id; } catch (e) {}
  console.log("  promptId=" + pid);
  await probe("SP read public", "GET", "/api/v1/speaking-prompts/" + pid, "none", 200);
  await probe("SP update partial keeps fields", "PUT", "/api/v1/admin/speaking-prompts/" + pid, "admin", 200,
    { title: "ZZ v8 prompt " + stamp + "b", description: "probe", prompt: "Describe your favourite room for 60 seconds.", level: "B1", category: "DAILY", isPublished: true, isPremium: false, mode: "FREE_SPEAKING", maxDurationSeconds: 120, attemptLimit: 5, orderIndex: 999 });
  await probe("SP search by q", "GET", "/api/v1/speaking-prompts?q=" + encodeURIComponent("ZZ v8 prompt"), "none", 200);
  await probe("SP blank title 400", "POST", "/api/v1/admin/speaking-prompts", "admin", 400,
    { title: "", description: "d", prompt: "p", level: "B1" });

  // ---------- DECK CRUD + ownership ----------
  let deckId = null;
  r = await probe("D create", "POST", "/api/decks", "user", 200, { name: "ZZ v8 deck " + stamp, description: "probe", cefrLevel: "B1" });
  try { deckId = JSON.parse(r.txt).id; } catch (e) {}
  console.log("  deckId=" + deckId);
  await probe("D add word", "POST", "/api/decks/" + deckId + "/words", "user", 200, { vocabId: 10017 });
  await probe("D add word again (dup)", "POST", "/api/decks/" + deckId + "/words", "user", [200, 400, 409], { vocabId: 10017 });
  await probe("D add bogus word", "POST", "/api/decks/" + deckId + "/words", "user", [400, 404], { vocabId: 99999999 });
  await probe("D read as owner", "GET", "/api/decks/" + deckId, "user", 200);
  await probe("D update", "PUT", "/api/decks/" + deckId, "user", 200, { name: "ZZ v8 deck b", description: "probe2", cefrLevel: "B2" });
  await probe("D games quiz on my deck", "GET", "/api/games/quiz/" + deckId, "user", [200, 400]);
  await probe("D delete", "DELETE", "/api/decks/" + deckId, "user", 200);
  await probe("D read after delete 404", "GET", "/api/decks/" + deckId, "user", 404);

  // ---------- SRS / FLASHCARD / GAME WRITE PATHS ----------
  await probe("SRS review", "POST", "/api/srs/review", "user", 200, { vocabId: 10017, quality: 4 });
  await probe("SRS review bad quality", "POST", "/api/srs/review", "user", [400, 200], { vocabId: 10017, quality: 99 });
  await probe("SRS review missing word", "POST", "/api/srs/review", "user", [400, 404], { quality: 3 });
  await probe("Flashcard review known", "POST", "/api/flashcards/review", "user", 200, { vocabularyId: 10017, isKnown: true });
  await probe("Flashcard review unknown", "POST", "/api/flashcards/review", "user", 200, { vocabularyId: 10017, isKnown: false });
  await probe("Flashcard status", "GET", "/api/flashcards/status/10017", "user", 200);
  r = await probe("Game quiz start", "GET", "/api/games/quiz/10006", "user", 200);
  let sess = null, uiAnswers = null;
  try { const g = JSON.parse(r.txt); sess = g.sessionId; uiAnswers = (g.data || []).map(it => ({ vocabId: it.vocabId, answer: (it.options || [""])[0] })); } catch (e) {}
  console.log("  gameSession=" + !!sess + " items=" + (uiAnswers ? uiAnswers.length : 0));
  await probe("Game submit (UI shape)", "POST", "/api/games/submit", "user", 200, { sessionId: sess, answers: uiAnswers, correctAnswers: 0 });
  await probe("Game submit bogus session", "POST", "/api/games/submit", "user", [400, 404], { sessionId: "not-a-session", answers: {}, correctAnswers: {} });

  // ---------- STRUCTURE CRUD on the temp lesson ----------
  let secId = null, blkId = null;
  r = await probe("ST add section", "POST", "/api/admin/lessons/" + lessonId + "/sections", "admin", 200, { title: "ZZ section", orderIndex: 1 });
  try { secId = JSON.parse(r.txt).id; } catch (e) {}
  console.log("  sectionId=" + secId);
  r = await probe("ST add block", "POST", "/api/admin/sections/" + secId + "/blocks", "admin", 200, { blockType: "TEXT", data: J({ text: "probe block" }), orderIndex: 1 });
  try { blkId = JSON.parse(r.txt).blocks[JSON.parse(r.txt).blocks.length - 1].id; } catch (e) {}
  await probe("ST update section", "PUT", "/api/admin/sections/" + secId, "admin", 200, { title: "ZZ section b", orderIndex: 1 });
  await probe("ST update block", "PUT", "/api/admin/blocks/" + blkId, "admin", 200, { blockType: "TEXT", data: J({ text: "probe block b" }), orderIndex: 1 });
  await probe("ST public structure", "GET", "/api/lessons/" + lessonId + "/structure", "none", 200);
  await probe("ST delete block", "DELETE", "/api/admin/blocks/" + blkId, "admin", 204);
  await probe("ST delete section", "DELETE", "/api/admin/sections/" + secId, "admin", 204);
  await probe("ST delete missing (idempotent 204)", "DELETE", "/api/admin/sections/99999999", "admin", [204, 404]);

  // ---------- SNAPSHOTS on the temp lesson ----------
  r = await probe("SNAP create", "POST", "/api/admin/lessons/" + lessonId + "/snapshots", "admin", 200);
  const rl = await probe("SNAP list", "GET", "/api/admin/lessons/" + lessonId + "/snapshots", "admin", 200);
  let snapId = null;
  try { snapId = JSON.parse(rl.txt)[0].id; } catch (e) {}
  console.log("  snapshotId=" + snapId);
  await probe("SNAP restore", "POST", "/api/admin/lessons/" + lessonId + "/snapshots/" + snapId + "/restore", "admin", 200);

  // ---------- cleanup lesson (cascade exercises) ----------
  await probe("L delete", "DELETE", "/api/admin/lessons/" + lessonId, "admin", [200, 204]);
  await probe("L read after delete 404", "GET", "/api/admin/lessons/" + lessonId, "admin", 404);
  await probe("E orphaned after cascade", "GET", "/api/admin/exercises/" + exId, "admin", 404);
  await probe("SP delete", "DELETE", "/api/v1/admin/speaking-prompts/" + pid, "admin", 200);

  lib.report("PHASE2-CRUD");
  lib.dump("p2.json");
})().catch(e => { console.error("FATAL", e); process.exit(1); });
