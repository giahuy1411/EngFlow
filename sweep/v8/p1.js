const lib = require("./lib.js");
const { probe } = lib;

(async () => {
  await lib.initTokens();

  // ---------- PUBLIC READS (no auth) ----------
  await probe("pub lessons list", "GET", "/api/lessons?page=0&size=5", "none", 200);
  await probe("pub lesson 445", "GET", "/api/lessons/445", "none", 200);
  await probe("pub lesson 445 exercises", "GET", "/api/lessons/445/exercises", "none", 200);
  await probe("pub lesson 445 content", "GET", "/api/lessons/445/exercises/content", "none", 200);
  await probe("pub lesson structure", "GET", "/api/lessons/447/structure", "none", 200);
  await probe("pub missing lesson 404", "GET", "/api/lessons/99999999", "none", 404);
  await probe("pub decks list", "GET", "/api/decks?page=0&size=5", "none", 200);
  await probe("pub deck 10006", "GET", "/api/decks/10006", "none", 200);
  // GET /api/vocabulary has no permitAll rule -> anyRequest().authenticated() = 401 by design;
  // the UI list screen uses /api/vocabulary/search (permitAll). Guarded here so a future
  // change to either is caught.
  await probe("vocab list requires auth", "GET", "/api/vocabulary?page=0&size=5", "none", 401);
  await probe("vocab list as user", "GET", "/api/vocabulary?page=0&size=5", "user", 200);
  await probe("pub vocab search", "GET", "/api/vocabulary/search?q=travel", "none", 200);
  await probe("pub leaderboard", "GET", "/api/leaderboard?page=0&size=10", "none", 200);
  await probe("pub leaderboard weekly", "GET", "/api/leaderboard?scope=weekly", "none", 200);
  await probe("pub speaking prompts", "GET", "/api/v1/speaking-prompts?page=0&size=5", "none", 200);
  await probe("pub speaking prompt 60014", "GET", "/api/v1/speaking-prompts/60014", "none", 200);
  await probe("pub video prompts alias", "GET", "/api/v1/video-prompts?page=0&size=5", "none", 200);
  await probe("pub video prompts by id", "GET", "/api/v1/video-prompts/60014", "none", 200);
  await probe("pub video-lessons", "GET", "/api/v1/video-lessons?page=0&size=5", "none", 200);
  await probe("pub video-lesson 1", "GET", "/api/v1/video-lessons/1", "none", 200);
  await probe("pub prompt submissions", "GET", "/api/v1/speaking-prompts/60014/submissions", "none", [200, 401, 403]);
  await probe("pub resource missing 404", "GET", "/api/resources/nope-not-here.png", "none", [404, 500]);

  // ---------- MEDIA PROXY (signed ticket IDOR checks) ----------
  await probe("media bare key (no ticket)", "GET", "/api/v1/media/speaking-submissions/12c35e99-f8d7-4b24-916a-70e435ef986e_e2f0.webm", "none", 403);
  await probe("media bad sig", "GET", "/api/v1/media/speaking-submissions/12c35e99-f8d7-4b24-916a-70e435ef986e_e2f0.webm?exp=9999999999&sig=deadbeef", "none", 403);
  await probe("media empty path", "GET", "/api/v1/media/", "none", 404);
  // %2e%2e encodings are decoded then rejected by the container filter (400), or fall
  // through to anyRequest().authenticated() (401). Neither serves the file.
  await probe("media traversal attempt", "GET", "/api/v1/media/%2e%2e%2f%2e%2e%2fetc/passwd", "none", [400, 401, 403]);

  // ---------- AUTH SURFACE ----------
  await probe("me user", "GET", "/api/auth/me", "user", 200);
  await probe("me noauth", "GET", "/api/auth/me", "none", 401);
  await probe("me bad token", "GET", "/api/auth/me", "bogus", 401);
  await probe("forgot unknown email", "POST", "/api/auth/forgot-password", "none", 200, { email: "nobody-not-real-zz@example.com" });
  await probe("forgot blank email", "POST", "/api/auth/forgot-password", "none", 400, { email: "" });
  await probe("reset bogus otp", "POST", "/api/auth/reset-password", "none", 400, { email: "user@gmail.com", otp: "000000", newPassword: "Abcdef123!" });
  await probe("register missing fields", "POST", "/api/auth/register", "none", 400, { email: "x@example.com" });
  await probe("register bad email", "POST", "/api/auth/register", "none", 400, { username: "zzx", email: "not-an-email", password: "Abcdef123!", fullName: "ZZ" });
  await probe("register short pw", "POST", "/api/auth/register", "none", 400, { username: "zzshort", email: "zzshort@example.com", password: "123", fullName: "ZZ" });
  await probe("login bad json", "POST", "/api/auth/login", "none", 400, "{not json");
  await probe("change-pw noauth", "POST", "/api/auth/change-password", "none", 401, { oldPassword: "123456", newPassword: "Abcdef123!" });
  await probe("avatar noauth", "PUT", "/api/auth/avatar", "none", 401, { avatarUrl: "https://x/y.png" });

  // ---------- ROLE MATRIX on protected reads ----------
  await probe("dashboard noauth", "GET", "/api/dashboard/stats", "none", 401);
  await probe("dashboard user", "GET", "/api/dashboard/stats", "user", 200);
  await probe("progress user", "GET", "/api/users/progress", "user", 200);
  await probe("streak current user", "GET", "/api/streak/current", "user", 200);
  await probe("streak history user", "GET", "/api/streak/history?days=30", "user", 200);
  await probe("srs stats user", "GET", "/api/srs/stats", "user", 200);
  await probe("srs due user", "GET", "/api/srs/due/10006", "user", 200);
  await probe("flashcard status", "GET", "/api/flashcards/status/10017", "user", 200);
  await probe("decks my", "GET", "/api/decks/my?page=0&size=5", "user", 200);
  await probe("games quiz public", "GET", "/api/games/quiz/10006", "none", [200, 401]);
  await probe("games quiz user", "GET", "/api/games/quiz/10006", "user", 200);
  await probe("games memory user", "GET", "/api/games/memory/10006", "user", 200);
  await probe("games typing user", "GET", "/api/games/typing/10006", "user", 200);
  await probe("games listening user", "GET", "/api/games/listening/10006", "user", 200);
  await probe("games mixed user", "GET", "/api/games/mixed/10006", "user", 200);
  await probe("attempts list noauth", "GET", "/api/lessons/445/exercises/attempts", "none", 401);
  await probe("attempts list user", "GET", "/api/lessons/445/exercises/attempts", "user", 200);
  await probe("includeAnswers user 403", "GET", "/api/lessons/445/exercises?includeAnswers=true", "user", 403);
  await probe("includeAnswers admin 200", "GET", "/api/lessons/445/exercises?includeAnswers=true", "admin", 200);

  // ---------- ADMIN-ONLY SURFACE as user (must all be 403) ----------
  const adminPaths = [
    ["GET", "/api/admin/stats"],
    ["GET", "/api/admin/users?page=0&size=5"],
    ["GET", "/api/admin/lessons?page=0&size=5"],
    ["GET", "/api/admin/lessons/445"],
    ["GET", "/api/admin/vocabulary?page=0&size=5"],
    ["GET", "/api/admin/exercises?page=0&size=5"],
    ["GET", "/api/admin/exercises/662147"],
    ["GET", "/api/admin/lessons/447/structure"],
    ["GET", "/api/admin/lessons/567/snapshots"],
    ["GET", "/api/v1/admin/speaking-prompts?page=0&size=5"],
    ["GET", "/api/v1/admin/speaking-submissions?page=0&size=5"],
    ["GET", "/api/v1/admin/video-prompts?page=0&size=5"],
    ["GET", "/api/v1/admin/video-lessons?page=0&size=5"],
    ["GET", "/api/v1/admin/video-attempts?page=0&size=5"],
    ["GET", "/api/admin/exercises/ai/status"],
    ["GET", "/api/admin/exercises/ai/backfill-answers/status"]
  ];
  for (const [m, p] of adminPaths) {
    await probe("user->admin " + p, m, p, "user", 403);
    await probe("noauth->admin " + p, m, p, "none", 401);
  }

  // ---------- ADMIN reads as admin ----------
  await probe("admin stats", "GET", "/api/admin/stats", "admin", 200);
  await probe("admin users search", "GET", "/api/admin/users?q=student&page=0&size=5", "admin", 200);
  await probe("admin users bad level enum", "GET", "/api/admin/lessons?level=NOT_A_LEVEL&page=0", "admin", 400);
  await probe("admin lessons filter", "GET", "/api/admin/lessons?level=ELEMENTARY&page=0&size=5", "admin", 200);
  await probe("admin exercises filter", "GET", "/api/admin/exercises?type=MULTIPLE_CHOICE&page=0&size=5", "admin", 200);
  await probe("admin exercise 404", "GET", "/api/admin/exercises/99999999", "admin", 404);
  await probe("admin vocab list", "GET", "/api/admin/vocabulary?page=0&size=5", "admin", 200);
  await probe("admin structure 447", "GET", "/api/admin/lessons/447/structure", "admin", 200);
  await probe("admin snapshots 567", "GET", "/api/admin/lessons/567/snapshots", "admin", 200);
  await probe("admin speaking prompts", "GET", "/api/v1/admin/speaking-prompts?page=0&size=5", "admin", 200);
  await probe("admin speaking submissions", "GET", "/api/v1/admin/speaking-submissions?page=0&size=5", "admin", 200);
  await probe("admin video lessons", "GET", "/api/v1/admin/video-lessons?page=0&size=5", "admin", 200);
  await probe("admin video attempts", "GET", "/api/v1/admin/video-attempts?page=0&size=5", "admin", 200);
  await probe("admin ai status", "GET", "/api/admin/exercises/ai/status", "admin", 200);
  await probe("ai status noauth", "GET", "/api/admin/exercises/ai/status", "none", 401);

  // ---------- SPEAKING user surface ----------
  await probe("my speaking submissions", "GET", "/api/v1/speaking-submissions?page=0&size=5", "user", 200);
  await probe("my speaking noauth", "GET", "/api/v1/speaking-submissions?page=0&size=5", "none", 401);
  await probe("speaking 40023 as owner", "GET", "/api/v1/speaking-submissions/40023", "user", [200, 403, 404]);
  await probe("video submissions alias", "GET", "/api/v1/video-submissions?page=0&size=5", "user", 200);
  await probe("my video attempts", "GET", "/api/v1/video-attempts?page=0&size=20", "user", 200);
  await probe("lesson-submission my", "GET", "/api/lesson-submissions/my/lesson/445/skill/SPEAKING", "user", [200, 404]);

  // ---------- PAYMENT ----------
  await probe("payment status user", "GET", "/api/v1/payment/status", "user", 200);
  await probe("payment status noauth", "GET", "/api/v1/payment/status", "none", 401);
  await probe("webhook bad signature", "POST", "/api/webhook/sepay", "none", [200, 400, 401, 403], { Data: { amount: 1, gateway_transaction_id: "zz-nope", transaction_date: "2026-09-13 10:00:00" }, signature: "badsig" });
  await probe("webhook empty body", "POST", "/api/webhook/sepay", "none", [200, 400], {});

  // ---------- ENUM / VALIDATION PROBES (audit-v7 F62 regression) ----------
  await probe("lessons level=FOO", "GET", "/api/lessons?level=FOO", "none", 400);
  await probe("lessons page=-1", "GET", "/api/lessons?page=-1", "none", [200, 400]);
  await probe("lessons size=99999 clamp", "GET", "/api/lessons?size=99999", "none", [200, 400]);
  await probe("admin exercises type=FOO", "GET", "/api/admin/exercises?type=FOO", "admin", 400);
  await probe("dictionary proxy word", "GET", "/api/vocabulary/dictionary/happy", "none", [200, 502, 503, 500]);
  await probe("grade empty answers", "POST", "/api/lessons/445/exercises/grade", "user", [200, 400], { answers: [] });
  await probe("submit noauth", "POST", "/api/lessons/445/exercises/submit", "none", 401, { answers: [] });
  await probe("games submit noauth", "POST", "/api/games/submit", "none", 401, { sessionId: "x" });
  await probe("srs review noauth", "POST", "/api/srs/review", "none", 401, { vocabularyId: 10017, quality: 4 });
  await probe("flashcard review noauth", "POST", "/api/flashcards/review", "none", 401, { vocabularyId: 10017, isKnown: true });
  await probe("lesson-submissions submit noauth", "POST", "/api/lesson-submissions/submit", "none", 401, { lessonId: 445, skillType: "SPEAKING" });

  lib.report("PHASE1");
  lib.dump("p1.json");
})().catch(e => { console.error("FATAL", e); process.exit(1); });
