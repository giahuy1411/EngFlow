const lib = require("./lib.js");
const { probe, sleep } = lib;
const J = JSON.stringify;

async function pollStatus(batchId, maxMs) {
  const t0 = Date.now();
  let last = null;
  while (Date.now() - t0 < maxMs) {
    const r = await probe("poll " + batchId.slice(0, 8), "GET",
      "/api/admin/exercises/ai/status?batchId=" + batchId, "admin", 200);
    try { last = JSON.parse(r.txt); } catch (e) { last = null; }
    if (last && last.running === false) return last;
    await sleep(5000);
  }
  return last;
}

(async () => {
  await lib.initTokens();
  const stamp = Date.now();

  // throwaway lesson with real teaching content (AI needs substance)
  const content = [
    "# Comparatives and Superlatives",
    "We form the comparative with -er for short adjectives: tall -> taller,",
    "and we use 'more' for long adjectives: expensive -> more expensive.",
    "The superlatives are the tallest and the most expensive.",
    "Irregular forms: good -> better -> best; bad -> worse -> worst.",
    "Example: Mount Everest is the highest mountain in the world.",
    "This year was more difficult than last year for many learners."
  ].join(" ");

  let lessonId = null;
  let r = await probe("AI temp lesson create", "POST", "/api/admin/lessons", "admin", 200,
    { title: "ZZ v8 AI lesson " + stamp, content: content, description: "comparatives",
      level: "ELEMENTARY", category: "GRAMMAR", durationMinutes: 8, isPublished: true, orderIndex: 9998 });
  try { lessonId = JSON.parse(r.txt).id; } catch (e) {}
  console.log("  temp lesson=" + lessonId);

  // ---------- async generation (202 + poll) ----------
  r = await probe("AI generate-async MC", "POST", "/api/admin/exercises/ai/generate-async", "admin", 202,
    { lessonId: lessonId, exerciseType: "MULTIPLE_CHOICE", count: 2 });
  let batchId = null;
  try { batchId = JSON.parse(r.txt).batchId; } catch (e) {}
  console.log("  batchId=" + batchId);
  if (batchId) {
    const st = await pollStatus(batchId, 300000);
    console.log("  final status: " + J(st));
    const okCount = (st && (st.generated || st.valid)) ? 1 : 0;
    console.log("  generated>0 ? " + (okCount ? "YES" : "NO/empty (guards rejected all)"));
  }
  // what actually landed?
  await probe("AI list new exercises", "GET",
    "/api/admin/exercises?lessonId=" + lessonId + "&page=0&size=20", "admin", 200);
  r = await probe("AI lesson exercises", "GET", "/api/lessons/" + lessonId + "/exercises", "none", 200);
  let created = 0, shapes = [];
  try { const arr = JSON.parse(r.txt); created = arr.length;
    shapes = arr.map(x => x.exerciseType + "|" + (x.options ? "opt" : "-") + "|" + (x.correctAnswer ? "ans" : "EMPTY"));
  } catch (e) {}
  console.log("  exercises created=" + created);
  for (const s of shapes) console.log("    " + s);

  // ---------- sync generate-all (all 5 types) ----------
  r = await probe("AI generate-all 3", "POST", "/api/admin/exercises/ai/generate-all", "admin", 200,
    { lessonId: lessonId, count: 3 });
  try { const g = JSON.parse(r.txt);
    console.log("  generate-all generated=" + g.generated + " valid=" + g.valid + " errors=" + g.errors);
    if (g.errorDetails && g.errorDetails.length) console.log("    errDetails[0..2]: " + J(g.errorDetails.slice(0, 3)));
  } catch (e) { console.log("  generate-all parse fail: " + r.txt.slice(0, 120)); }

  // ---------- validate endpoint: 1 good + 3 malformed ----------
  const drafts = [
    { exerciseType: "MULTIPLE_CHOICE", question: "Choose the correct comparative: tall -> ?",
      options: J(["taller", "more tall", "tallest", "most tall"]), correctAnswer: "taller", explanation: "-er for short adjectives" },
    { exerciseType: "MATCHING", question: "Match the pairs",
      options: J(["good / better", "bad / worse"]), correctAnswer: "good=better, bad=worse" },
    { exerciseType: "NOT_A_TYPE", question: "Bogus type", options: J(["a", "b"]), correctAnswer: "a" },
    { exerciseType: "MULTIPLE_CHOICE", question: "", options: null, correctAnswer: "" }
  ];
  r = await probe("AI validate drafts", "POST", "/api/admin/exercises/ai/validate", "admin", 200,
    { exercises: drafts, useAiReview: false });
  try { const v = JSON.parse(r.txt);
    console.log("  validate total=" + v.total + " valid=" + v.valid + " invalid=" + v.invalid);
    for (const s of (v.schemaResults || [])) console.log("    passed=" + s.passed + " q=" + String(s.question).slice(0, 28) + " err=" + (s.error || ""));
  } catch (e) { console.log("  validate parse fail " + r.txt.slice(0, 120)); }

  // ---------- bad-request handling on AI endpoints ----------
  await probe("AI generate missing lesson", "POST", "/api/admin/exercises/ai/generate", "admin", 404,
    { lessonId: 99999999, count: 1 });
  await probe("AI generate count over max", "POST", "/api/admin/exercises/ai/generate", "admin", 400,
    { lessonId: lessonId, count: 999 });
  await probe("AI generate user 403", "POST", "/api/admin/exercises/ai/generate", "user", 403,
    { lessonId: lessonId, count: 1 });
  await probe("AI status unknown batch", "GET", "/api/admin/exercises/ai/status?batchId=zz-none", "admin", [200, 404]);
  await probe("AI generate-batch user 403", "POST", "/api/admin/exercises/ai/generate-batch?force=false", "user", 403);
  await probe("seed force=false user 403", "POST", "/api/admin/exercises/seed?force=false", "user", 403);

  // ---------- backfill: dry-run only, tiny limit ----------
  r = await probe("backfill dry-run", "POST",
    "/api/admin/exercises/ai/backfill-answers?dryRun=true&limit=6&mode=deterministic", "admin", 202);
  try { console.log("  backfill start: " + r.txt.slice(0, 200)); } catch (e) {}
  let bf = null;
  for (let i = 0; i < 20; i++) {
    r = await probe("backfill status", "GET", "/api/admin/exercises/ai/backfill-answers/status", "admin", 200);
    try { bf = JSON.parse(r.txt); } catch (e) {}
    if (bf && bf.running === false) break;
    await sleep(3000);
  }
  console.log("  backfill dry-run result: " + J(bf));

  // ---------- AI vocab (Ollama) ----------
  r = await probe("ai generate-vocab", "POST", "/api/ai/generate-vocab", "user", 200,
    { topic: "travel airports", level: "B1", count: 3 });
  let words = [];
  try { words = JSON.parse(r.txt); console.log("  vocab items=" + words.length + " sample=" + (words[0] && words[0].word)); }
  catch (e) { console.log("  vocab raw: " + r.txt.slice(0, 160)); }
  await probe("ai generate-vocab blank topic", "POST", "/api/ai/generate-vocab", "user", 400, { topic: "  " });
  await probe("ai generate-vocab count too big", "POST", "/api/ai/generate-vocab", "user", 400, { topic: "x", count: 51 });
  await probe("ai generate-vocab count non-numeric", "POST", "/api/ai/generate-vocab", "user", 400, { topic: "x", count: "abc" });
  await probe("ai generate-vocab noauth", "POST", "/api/ai/generate-vocab", "none", 401, { topic: "x" });
  r = await probe("ai enrich-word", "POST", "/api/ai/enrich-word", "user", 200, { word: "diligent" });
  console.log("  enrich: " + r.txt.slice(0, 160));

  // save-vocab validation (no AI cost)
  await probe("save-vocab empty", "POST", "/api/ai/save-vocab", "user", 400, []);
  await probe("save-vocab blank word", "POST", "/api/ai/save-vocab", "user", 400,
    [{ word: "", meaning: "x" }]);
  const tooMany = []; for (let i = 0; i < 51; i++) tooMany.push({ word: "zzw" + i + stamp, meaning: "m" });
  await probe("save-vocab over 50", "POST", "/api/ai/save-vocab", "user", 400, tooMany);

  // ---------- AI speaking prompts ----------
  r = await probe("prompt ai-generate", "POST", "/api/v1/admin/speaking-prompts/ai-generate", "admin", [200, 502],
    { topic: "weekend hobbies", level: "A2" });
  console.log("  ai-generate: " + r.txt.slice(0, 200));
  r = await probe("prompt ai-generate-full", "POST", "/api/v1/admin/speaking-prompts/ai-generate-full", "admin", [200, 502],
    { topic: "ordering food", level: "B1", mode: "FREE_SPEAKING" });
  console.log("  ai-generate-full: " + r.txt.slice(0, 220));
  await probe("prompt ai-generate blank topic", "POST", "/api/v1/admin/speaking-prompts/ai-generate", "admin", [400, 502], { topic: "" });
  await probe("prompt ai-generate user 403", "POST", "/api/v1/admin/speaking-prompts/ai-generate", "user", 403, { topic: "x" });

  // ---------- video AI helpers ----------
  await probe("translate-transcript", "POST", "/api/v1/admin/video-lessons/translate-transcript", "admin", [200, 502],
    [{ start: 0, end: 3, textEn: "This is a sample sentence." },
     { start: 3, end: 6, textEn: "Here is a second one." }]);
  await probe("translate-transcript user 403", "POST", "/api/v1/admin/video-lessons/translate-transcript", "user", 403,
    [{ start: 0, end: 1, textEn: "x" }]);
  await probe("fetch-youtube bogus", "POST", "/api/v1/admin/video-lessons/fetch-youtube", "admin", [200, 400, 502],
    { url: "https://www.youtube.com/watch?v=not-a-real-id-zz" });

  // cleanup
  await probe("AI temp lesson delete", "DELETE", "/api/admin/lessons/" + lessonId, "admin", [200, 204]);
  await probe("AI lesson gone", "GET", "/api/lessons/" + lessonId, "none", 404);

  lib.report("PHASE3A-AI");
  lib.dump("p3a.json");
})().catch(e => { console.error("FATAL", e); process.exit(1); });
