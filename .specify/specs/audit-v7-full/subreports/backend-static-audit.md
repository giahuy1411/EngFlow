# EngFlow Backend Static Audit — v7 (full)

Baseline: audit v6 (2026-09-04). Read-only static pass over `src/main/java/com/datn/engflow`
(26 controllers, 47 services, config/security/filter), `pom.xml`, `application.properties`,
`docker-compose.yml`. Priority = effective behaviour, not annotated intent.
Format Section A: `ID | P | file:line | problem | fix`.

---

## SECTION A — FINDINGS

### A.1 Access control / authentication (P1 cluster)

F01 | P1 | config/SecurityConfig.java:79 | `GET /api/lessons/** permitAll` is matched FIRST, so the later `GET /api/lessons/*/exercises/attempts/** authenticated` rule at SecurityConfig.java:92 is dead code (first-match-wins) → all lesson-attempt endpoints are reachable unauthenticated | Narrow L79 to `GET /api/lessons`, `GET /api/lessons/{id}` and move the attempts rule ABOVE it; never `**` a resource prefix that also owns private sub-paths.

F02 | P1 | controller/LessonExerciseController.java:67 | `exerciseService.getAttemptHistory(lessonId, authentication.getName())` with `Authentication` possibly null (endpoint is PUBLIC per F01) → NPE → 500; the per-user authorization intent of SecurityConfig.java:92 is entirely unenforced | null-check `authentication` → 401 (same pattern as DeckController.java:44-47).

F03 | P1 | controller/LessonExerciseController.java:76 | Same NPE/unauthenticated-access bug on `getAttemptDetail`; the ownership filter (`findByIdAndUserId`, ExerciseService.java:295) only works because the email comes from the principal — with a null principal it is a 500, and any future non-null anonymous principal leaks another learner's answer history | null-check → 401; fix F01 so the URL rule actually applies.

F04 | P1 | controller/MediaProxyController.java:22-33 | `GET /api/v1/media/**` is permitAll (SecurityConfig.java:105) and `minioService.getObject(objectKey)` (MinioService.java:129) applies ZERO owner/session check → any anonymous visitor who obtains or guesses an objectKey streams any learner's speaking/video recording; the only protection is the UUID in the key, which is leaked through `SpeakingSubmissionResponse.audioUrl`, logs, and the Referer header | proxy through an authenticated endpoint that re-checks `submission.user.id == principal.id` (or mint short-lived signed URLs).

F05 | P1 | service/LessonStructureService.java:32-56 | `@Transactional` (write) GET that AUTO-INSERTS a `LessonSection` + `LessonBlock` when a lesson has no structure; it is exposed by the PUBLIC endpoint `GET /api/lessons/{lessonId}/structure` (LessonStructureController.java:132, permitAll via SecurityConfig.java:79) → unauthenticated DB-write amplification (one anonymous GET per lesson = permanent rows) = DoS + data pollution | split: read path `readOnly = true` returning `List.of()`; move the materialise-on-first-view side effect into an authenticated admin call or a one-off migration.

F06 | P1 | config/DatabaseSeeder.java:91-92 | `getOrDefault("DEFAULT_USER_PASSWORD","password123")` / `getOrDefault("DEFAULT_ADMIN_PASSWORD","password123")` → when the env vars are absent the seeder silently creates `admin@gmail.com` with a well-known password that also appears in AGENTS.md; this is a login path to every `hasRole('ADMIN')` surface | remove the default, `IllegalStateException` if unset, and refuse to seed an admin unless an explicit `SEED_ADMIN_ENABLED=true`.

F07 | P1 | src/main/resources/application.properties:75 | `sepay.webhook.secret=${SEPAY_WEBHOOK_SECRET:supersecret}` → a committed default signing key. `PaymentService.processWebhook` validates the HMAC (correctly, constant-time at PaymentService.java:408/420) against this value, so anyone reading the repo can forge a signed webhook with the right `content` and activate premium for free; the endpoint is permitAll (SecurityConfig.java:86) | no default (fail-fast like `jwt.secret` at L36), plus refuse to boot in non-dev profiles when unset.

F08 | P1 | service/PaymentService.java:176-184 | `processSePayTransaction` activates premium from the PENDING row and stores `amount` (L189) but NEVER compares the transferred `amount` with `pending.getAmount()` → a 1.000đ bank transfer carrying a valid `ENG…` order code in its content buys the 20.000đ YEAR plan | `if (amount.compareTo(pending.getAmount()) < 0) { reject }`; the polling path (PaymentService.java:272) must apply the same check.

F09 | P2 | service/PaymentService.java:288-296 | `createOrder` hardcodes prices (`20000` / `10000`) and never validates `planType`: any string that is not exactly `YEAR` is billed the MONTH price yet stored raw in `plan_type` and later fed to `calculateExpiry` → silent plan downgrade + garbage rows | whitelist `Set.of("MONTH","YEAR")` → 400; move prices to `@Value` config.

F10 | P2 | controller/payment/PaymentController.java:33-43 | Request body is `Map<String,String>` with `body.getOrDefault("planType","MONTH")` → no Bean Validation, unknown keys ignored, `planType` unvalidated before it reaches the DB | typed `CreateOrderRequest` record with `@NotBlank @Pattern`.

F11 | P2 | security/RateLimitFilter.java:31-48 | Bucket coverage is login/register (20/min), forgot/reset (5/min) and a global 100/min keyed by IP+bucket only. There is NO dedicated bucket for the expensive/abusable endpoints: `POST /api/ai/generate-vocab`, `/api/ai/enrich-word`, `/api/ai/save-vocab`, `POST /api/v1/speaking-submissions/upload`, `/api/admin/exercises/ai/generate*`, `POST /api/v1/payment/create-order`, `/api/admin/upload` → one authenticated user (or one IP under the global cap) can drive unlimited Ollama/Whisper/minutes of LLM work and unlimited QR creation | add per-endpoint buckets (AI: 10/min per user, upload: 20/min per user, order-create: 5/min per user) keyed by userId not just IP.

F12 | P2 | controller/AiVocabController.java:110-113 | `POST /api/ai/save-vocab` accepts `List<@Valid VocabularyRequest>` with NO list-size cap and NO `userService.consumeAiGenerationQuota(...)` call (unlike L66 and L97) → unbounded bulk insert into the SHARED global vocabulary table, bypassing the AI quota entirely; returns raw `List<Vocabulary>` | cap at e.g. 50 (`@Size(max=50)` on a wrapper DTO), consume quota, return a response DTO.

F13 | P2 | service/UserService.java:167 vs :190 | Login computes the Redis lock/fail keys from `email.trim().toLowerCase()` but then authenticates and re-looks-up with the RAW `request.getEmail()` (`findByEmail(request.getEmail())`, and `CustomUserDetailsService.java:25` likewise) → with a case-insensitive-but-distinct input the counter keys and the stored identity diverge, so repeated failures under a different casing do not accumulate into the same lockout | normalize once at the top and use the normalized value for authenticate + lookup (and store normalized at register).

F14 | P2 | service/AdminService.java:98-113 | `toggleUserActive` / `toggleUserAdmin` have no self-protection: an admin can deactivate or de-admin themselves (locking everyone out of the admin UI with no recovery path) and there is no "last admin" guard | reject when `userId.equals(currentPrincipal.id)` or when the remaining admin count would hit 0; pass the acting admin id into the service.

F15 | P2 | controller/LessonController.java:49-75 | `createLesson` / `updateLesson` / `deleteLesson` only check `authentication == null`; there is no `@PreAuthorize`. Today the URL rules (SecurityConfig.java:93-95) cover it, but the method-level layer is absent on a mutating trio, so any future path refactor (e.g. moving lessons under a permitAll prefix) silently exposes lesson CRUD to every learner | add `@PreAuthorize("hasRole('ADMIN')")` like AdminExerciseController does per-method.

F16 | P2 | controller/AdminController.java:31-123 | The whole 16-endpoint admin surface (stats, user toggles incl. `toggle-admin`, lesson CRUD, vocabulary CRUD) carries ZERO `@PreAuthorize`; protection is 100% URL-rule-derived (`/api/admin/**` at SecurityConfig.java:103) — the exact single-layer pattern audit-v5 tried to remove with `@EnableMethodSecurity` | class-level `@PreAuthorize("hasRole('ADMIN')")`.

F17 | P2 | controller/LessonStructureController.java:34-131 | 8 admin-only paths under `/api/admin/*` plus `/api/admin/upload` and `/api/admin/audio-upload` with no class or method `@PreAuthorize` (URL-rule-only), and the file itself has no class-level `@RequestMapping` so each path is spelled inline — high refactor-risk surface | class-level `@PreAuthorize` + restore a `@RequestMapping` prefix.

F18 | P2 | controller/speaking/SpeakingSubmissionController.java:68-95 | The two admin surfaces (`GET /api/v1/admin/{speaking,video}-submissions`, `PATCH .../grade`) rely on manual `isAdmin(...)` (L75, L90) with no `@PreAuthorize`; `/api/v1/admin/**` URL rule (SecurityConfig.java:102) is the only declarative layer, and the manual check duplicates authorization logic in the controller | add `@PreAuthorize("hasRole('ADMIN')")` and keep the manual check as a second layer, not the first.

F19 | P2 | controller/video/VideoLessonController.java:103-203 | All 10 admin endpoints (lesson CRUD, upload, attempts list, `grade`, `ai-grade`, `translate-transcript`, `fetch-youtube`) are guarded only by `requireAdmin(userPrincipal)` + the URL rule — no `@PreAuthorize` anywhere in the file | class-level `@PreAuthorize("hasRole('ADMIN')")` on an admin sub-controller.

F20 | P2 | controller/LessonSnapshotController.java:23-39 | `/api/admin/lessons/{id}/snapshots[/restore]` — NONE-ANNOTATED, URL-rule only; `restoreSnapshot` is a destructive delete-and-reinsert operation reached by a plain POST with no `@PreAuthorize` | add `@PreAuthorize("hasRole('ADMIN')")`.

F21 | P2 | controller/VocabularyController.java:33-35 | `GET /api/vocabulary` returns `vocabularyRepository.findAll(pageable)` — the raw `Page<Vocabulary>` entity — to ANY authenticated user, exposing internal columns (`source`, `lesson` FK graph, timestamps) and leaking the full table shape; also no page-size clamp (`@PageableDefault(size=20)` alone allows `size=100000` via Spring's default unbounded max) | DTO projection + `size = Math.min(size,100)` + repository-level projection.

F22 | P2 | controller/AdminController.java:75-77 | `GET /api/admin/lessons/{id}` returns the raw `Lesson` entity (`content` NVARCHAR(MAX) full body plus `vocabularies` collection graph) instead of `LessonResponse` — same for L80/85/96 (POST/PUT/toggle-publish) and L102-121 (`Page<Vocabulary>`, `Vocabulary`) | return DTOs; add `@EntityGraph`/`@JsonIgnore` where the graph is unwanted.

F23 | P2 | controller/AdminAiExerciseController.java:63-76 | `POST /generate-all` returns `AiExerciseResult.exercises`, a raw `List<Exercise>` including `correctAnswer` + `explanation`, from an admin endpoint — fine for admins, but the same DTO graph is reused by the service for non-admin callers and the raw entity also carries `lesson` FK | map to `ExerciseResponse(includeAnswers=false)` and add an explicit `?includeAnswers=true` admin branch.

F24 | P2 | controller/GameController.java:63-89 | `POST /api/games/submit` binds `Map<String,Object>` and hand-parses; when `answers` is absent `GameService.java:231-239` TRUSTS the client-supplied `correctAnswers` count (warn-log only) and awards points/streak (`streakService.checkin`, GameService.java:258) → trivial XP/leaderboard/streak farming by any authenticated user | require `answers` (reject the trust path in prod, keep it behind an explicit dev flag).

F25 | P2 | service/GameService.java:202-256 | Even with `answers` present the deck is re-read and validated in memory but the resulting score is capped only by "100/day" — no server-side session linking the `/quiz/{deckId}` issuance to the `/submit` call (the Redis session is deleted at L268-276 on first submit, so a client can submit the same generated quiz payload repeatedly against fresh decks) | bind submit to a one-time issued sessionId and consume it.

### A.2 Validation / error contract

F26 | P2 | model/dto/request/GradeRequest.java:1-40 | `GradeRequest` has ZERO Bean-Validation annotations (no `@NotNull exerciseId`, no `@Size(max=…)` on `answers`) AND the endpoint `LessonExerciseController.java:46-52` omits `@Valid` → an unbounded `answers` list is iterated and serialized into `detailsJson` by string concatenation (ExerciseService.java:237-248) then written to an NVARCHAR(MAX) column → request-amplification + heap/O(n²) work per grade call | annotate the DTO + record type, add `@Valid`, cap `answers` at the lesson's exercise count.

F27 | P2 | controller/AdminExerciseController.java:54-60 | `PUT /api/admin/exercises/{id}` is missing `@Valid` on `ExerciseRequest` while the sibling `POST` at L48 has it → an update with `type`/`difficulty`/null `question` bypasses validation and lands on `ExerciseType.valueOf` (ExerciseService.java:101) | add `@Valid` (and use one shared record for create/update).

F28 | P2 | exception/GlobalExceptionHandler.java (all 15 handlers) | No handler for `IllegalArgumentException`, so every unguarded `*Enum.valueOf(...)` becomes a 500 "internal error" instead of 400. Unguarded call sites: AdminService.java:187 (`SkillType`), ExerciseService.java:101-102 (`ExerciseType`, `ExerciseDifficulty`), LessonStructureService.java:96 and :109 (`BlockType`), LessonSnapshotService.java:123 (`BlockType`), AdminAiExerciseController.java:44, :57, :103 (`ExerciseType`), VocabularyController path params, StreakService `recordAccess` `findById().orElseThrow()` (bare `NoSuchElementException` → 500 where 404 is meant) | add `@ExceptionHandler(IllegalArgumentException.class)` → 400 ProblemDetail, and prefer tolerant enum parsing (`resolve` static factory) at the DTO boundary.

F29 | P2 | controller/VocabularyController.java:38-48 | `GET /api/vocabulary/search` is permitAll (SecurityConfig.java:77) and calls `findByWordContainingIgnoreCase(query)` with NO `Pageable`/limit → a 2-char query (`qu`) returns and serializes every matching row of the global table to anonymous clients (table will grow via F12) | `Pageable` with max 50, or a `select top 50` derived query.

F30 | P3 | controller/LessonSubmissionController.java:61-75 | On "not found" the handler returns `200 OK` with a `null` body instead of 404 → the client cannot distinguish "no submission yet" from a real failure, and RFC7807 contract breaks | return 404 ProblemDetail (or 204) consistently.

F31 | P3 | controller/SrsController.java:23-42 and controller/DeckController.java:84-97 | Mutating bodies are `Map<String,Integer>` / `Map<String,Long>` with hand-rolled null checks (`quality` bounds, `vocabId` presence) instead of annotated DTOs → no `@Valid`, error messages inconsistent with the rest of the API | typed records + Bean Validation.

F32 | P3 | controller/StreakController.java:23-36 | `days` request param is unclamped and feeds a per-day loop → `?days=1000000` is a cheap CPU/allocation amplifier | `@Max(365)` (the class already imports `@Max` elsewhere via LeaderboardController's pattern).

F33 | P3 | model/dto/response/UserResponse.java (field `token`) | `private String token;` has no `@JsonInclude(NON_NULL)` → every `/api/auth/me`, register, change-password, avatar response carries a meaningless `"token": null`; combined with the fact that `login` returns the JWT in the same DTO, an accidental `mapToUserResponse(user, jwt)` reuse would leak a token through a profile endpoint | `@JsonInclude(NON_NULL)` on the field; separate `AuthTokenResponse` from `UserResponse`.

### A.3 Transactions / write-on-read / consistency

F34 | P2 | service/LessonService.java:157 & :197-198 | `getLessonDetails` is `@Transactional` (not `readOnly`) and WRITES `LessonProgress` + `setLastAccessed(LocalDateTime.now())` during a detail read; the endpoint is PUBLIC (SecurityConfig.java:79) → anonymous GETs create/refresh progress rows and a `User` row is touched per request (write-lock on a read path, plus streak/points semantics triggered by browsing) | split into `readOnly` read + an explicit authenticated `POST /api/lessons/{id}/visit` (or make the write idempotent and move it behind auth).

F35 | P2 | service/PaymentService.java:322-342 | `getPremiumStatus` (a GET, PaymentController.java:45) MUTATES state: `user.setIsPremium(false)` + `userRepository.save(user)` at L341-342 when the expiry passed → non-idempotent GET, write contention, and a paid account can be flipped to free by a page refresh racing the webhook | derive "active" from `premiumExpiry > clock.today()` in the response; do the sweep in the existing scheduler.

F36 | P2 | service/UserService.java:224-233 | `getProfile` is `@Transactional` and calls `streakService.recordAccess(...)` (a write) then re-reads the user (`findById`, L232) — a read endpoint writing check-ins means every profile poll mutates `last_study_date`/streak, and login (L199-200) repeats the same write-then-reread | extract an explicit `POST /api/streak/checkin`; make `getProfile` `readOnly`.

F37 | P2 | service/DashboardService.java (totalLessons) vs service/ProgressService.java:37 | `DashboardService.totalLessons` = `lessonRepository.count()` (ALL lessons incl. unpublished) while `ProgressService` uses `countByIsPublishedTrue` → the learner UI shows a denominator that disagrees with the completion percentage on the same screen | one shared `countByIsPublishedTrue()` and a regression test.

F38 | P2 | service/LessonSnapshotService.java:88-137 | `restoreSnapshot` deletes all sections/blocks then re-inserts with per-row `save()` (N+1 writes) and throws a bare `RuntimeException` (L100) for a domain conflict → a failed mid-restore leaves the lesson structure partially rebuilt; the only caller is an admin POST with no `@PreAuthorize` (F20) | batch inserts (`saveAll`) + a single `ConflictException` mapped to 409, and verify the enclosing `@Transactional` actually wraps the delete+insert.

F39 | P3 | service/UserService.java:164 | `login` is `@Transactional` while performing Redis I/O and `streakService.recordAccess` writes → the DB transaction stays open across network calls to Redis (connection-pool holding under latency) | drop `@Transactional` from `login` (the JPA reads don't need a service tx) or move the Redis work out.

F40 | P3 | service/ExerciseService.java:316 | `} catch (Exception ignored) {}` — the only fully-empty catch in the service layer: a malformed `detailsJson` silently drops per-item rows so the attempt detail page renders a SHORTER list with no score mismatch and no log line | log at warn with attemptId; surface a `partial: true` flag.

F41 | P3 | service/SupertonicProxyTtsService.java (JSON build + `escapeJson`) | Response/request JSON is hand-concatenated; `escapeJson` handles `\\ " \n \r` but not control chars < 0x20 (e.g. `\t` inside generated exercise text from the LLM) → malformed request/response and a swallowed failure returning `null` (L53/56/71), i.e. silent no-audio regressions like the 89/449 known gap | use the injected `ObjectMapper`; log failures at warn with the text digest.

F42 | P3 | service/ExerciseService.java:237-275 | Attempt `details` is JSON built by string concatenation with a custom `escapeJson`, then re-parsed by hand at ExerciseService.java:298-329 with `splitJsonArray`/`extractString` — a two-layer bespoke codec where a `List<AttemptItem>` + `ObjectMapper` would do; any question text containing `},{` breaks the parser and hits F40's silent catch | serialize/deserialize with `ObjectMapper` into a typed list.

### A.4 Performance / unbounded work

F43 | P2 | service/SrsService.java:86-123 | `getDueWords` loops the deck's words calling `progressRepository.findByUserIdAndVocabularyId(...)` per word → classic N+1 (a 1000-word deck = 1000 queries) on the main review screen | `findByUserIdAndVocabularyIdIn(userId, ids)` into a `Map`.

F44 | P2 | service/AiExerciseService.java:57 & :63 and service/AiAnswerBackfillService.java:54 | `batchProgressMap`, the literal key `"batch"` (AiExerciseService.java:579/607) and `seenQuestionHashes` are in-process `ConcurrentHashMap`/`newKeySet` that are NEVER evicted and never shared → unbounded heap growth across long-lived containers (cross-lesson dedup set only ever grows) and progress lost on restart/scaled-out; the project already has Redis + `RedisConstants` for exactly this | move dedup + progress to Redis with TTL (progress keys already exist for generate-async) or a bounded `Caffeine`/LRU.

F45 | P2 | controller/AdminAiExerciseController.java:78-90 and :91-131 | `POST /generate-batch` runs SYNCHRONOUSLY (blocking a request thread for the whole LLM run) and `POST /validate` calls `reviewExercise` once per exercise (1 LLM call per item, N serial) — while `/generate-async` (L38) already proved the async pattern | route both to the executor + 202, and batch the review prompt.

F46 | P2 | service/AiExerciseService.java:575 | `lessonRepository.findAll()` inside the generation pipeline loads every lesson (with `content` NVARCHAR(MAX)) to compute cross-lesson dedup context | project to `id`/`title` or key the dedup index in Redis.

F47 | P2 | service/ExerciseSeedService.java | `findAll()` + a per-lesson `countByLessonId` inside ONE big `@Transactional` → N+1 reads and a long-running write tx holding locks on `exercises` | bulk `countByLessonIdIn` map + chunk the tx per lesson.

F48 | P3 | service/LessonStructureService.java (toSectionResponse) | `getLessonStructure`/admin structure map each section → one query per section for its blocks (N+1) on a public endpoint | `@EntityGraph` or `findBySectionIdIn` + group.

F49 | P3 | service/GameService.java:100-195 | Generators load the whole deck into memory and `generateQuiz` builds a per-question `others` copy → O(n²) for large decks (the cap of 10 questions hides it today, but the deck load is uncapped) | DB-side `ORDER BY NEWID() TOP n` or pre-shuffled cache.

F50 | P3 | service/AiPromptService.java / service/DictionaryService.java | Blocking `WebClient(...).block()` on the request thread for Ollama/dictionary calls → under `AI_SPEAKING_LLM_TIMEOUT_SECONDS=180` (docker-compose.yml:79) a handful of concurrent speaking assessments exhaust the Tomcat pool and the whole API stalls | async controller return (`CompletableFuture`/`DeferredResult`) with a bounded dedicated executor, plus a bulkhead timeout under the HTTP timeout.

F51 | P3 | service/SrsService.java:103 | `getNextReviewDate().isEqual(now)` compares a stored instant against the exact request `now` — equality on timestamps essentially never holds, so the "due now" branch is effectively dead | use `!getNextReviewDate().isAfter(now)`.

### A.5 Determinism / time

F52 | P3 | AdminService.java:43, ExerciseService.java:260, FlashcardService.java:64, LessonService.java:197, PaymentService.java:245, ShadowingAiGradingService.java:77 & :84, SpeakingSubmissionService.java:198, SrsService.java:74, :76, :90, :131, VideoLessonService.java:187 | 13 `LocalDateTime.now()` call sites bypass the injected `Clock` bean (`EngflowApplication.clock()`), while the same classes already accept `Clock` elsewhere (AdminService.java:81, PaymentService.calculateExpiry:462-466, StreakService) → untestable time logic and host-TZ dependence despite compose `TZ=Asia/Ho_Chi_Minh` | replace with `LocalDateTime.now(clock)`; add a `Clock.fixed` unit test per touched behaviour.

### A.6 Config / hardcoding / infra

F53 | P2 | service/EmailService.java:85 and :142 | Hardcoded `http://localhost:5173/…` links inside the OTP email and the streak-reminder email → in any Docker/Tailscale-Funnel deploy the mail links are dead for the recipient (a P1-adjacent UX/auth break: password reset by email is unusable off-host) | `app.frontend-base-url` property, defaulted per profile, validated non-localhost in prod.

F54 | P2 | service/MinioService.java:87, service/SpeakingSubmissionService.java:207, model/dto/response/SpeakingPromptResponse.java:48 | Three copies of the same host rewrite `url.replace("minio:9000","localhost:9000")` (and a `contains("minio:9000")` probe) → the internal↔public host mapping is duplicated in code instead of read from `minio.public-url` (already present at application.properties:67), and each new storage client silently breaks | delete all three; generate URLs from `minio.public-url` at upload time and store only the objectKey.

F55 | P2 | controller/MediaProxyController.java:52 | Hardcoded `Access-Control-Allow-Origin: http://localhost:5173` header on every media response, in addition to the central CORS source (SecurityConfig.java:57-67) → duplicate ACAO values (browsers reject multiple/conflicting) and media playback breaks on the funnel/production origin | remove the header; let `CorsFilter` own it.

F56 | P2 | config/SecurityConfig.java:117 | CSP `connect-src 'self' https://api.dictionaryapi.dev http://localhost:* ws://localhost:*` is dev-only: with the app served through Tailscale Funnel/any real host, XHR to the API is same-origin (`'self'`) but the Ollama/Whisper/proxy and dev WS endpoints won't match, and the policy is applied in every profile because it is not profile-scoped | profile-scoped CSP; prod variant without `localhost:*`.

F57 | P2 | src/main/resources/application.properties:23-24 | `spring.sql.init.mode=always` + `spring.sql.init.continue-on-error=true` → `data.sql` is replayed on EVERY boot and every failure is swallowed; combined with `ddl-auto=update` (L13) a seed that has drifted from the entity model produces half-applied data plus a clean-looking log | `mode=always` only under the `dev` profile with a guard table, and `continue-on-error=false`; prod → `ddl-auto=validate`.

F58 | P2 | config/JacksonConfig.java:14-18 | Declares `@Primary new ObjectMapper()` (Jackson 2 type) in a Boot 4 app whose MVC converter uses Jackson 3 → the bean is either inert or, if it is picked up, replaces Boot's customized mapper (JavaTime module, `@JsonIgnore`/mixin registrations, `NON_NULL` defaults) with a bare one. VERIFY which (print the resolved converter at boot); either way it is misleading | delete the class, or configure via `spring.jackson.*` / a `JacksonModule` on the correct mapper.

F59 | P2 | docker-compose.yml:104-105 | MinIO credentials default to `minioadmin` / `minioadmin123` in the compose file, and MinIO is reachable from the host (:9000) while the backend serves bucket objects unauthenticated through `/api/v1/media/**` (F04) → weak default creds + an open proxy is a full-blob-extraction path | require the env vars (no default), set a strong password, and stop exposing :9000/:9001 when the proxy exists.

F60 | P3 | src/main/resources/application.properties:49-51, :56-57, :68-69, :73 | Committed `demo`/`test@gmail.com`/`testpassword`/`minioadmin`/`123456789` fallbacks. Not real secrets, but each one is a silent misconfiguration: the app boots happily with Cloudinary `demo` creds (upload failures at runtime) and SMTP `testpassword` (mail silently failing inside the `EmailService` catch-and-log) | no defaults for infra creds; fail-fast in non-dev.

F61 | P3 | service/CloudinaryService.java (uploadAudio / uploadAudioBytes) | `uploadAvatar` validates `contentType().startsWith("image/")` but the audio paths perform NO content-type/extension validation and no size check beyond the global 50MB multipart cap (application.properties:62-63) | validate MIME against an audio allowlist + per-type max size.

F62 | P2 | controller/LessonStructureController.java:71-91 + service/LessonSubmissionService.java:101-117 | `POST /api/admin/upload` keeps the CLIENT-supplied extension (`originalFilename.substring(lastIndexOf("."))`) and `saveAudioFile` falls back to `.webm` only when there is no dot; the stored file is then served SAME-ORIGIN at `GET /api/resources/{filename:.+}` (permitAll, SecurityConfig.java:106) → uploading `.html`/`.svg` yields stored XSS executed under the API origin (the traversal guard at LessonStructureController.java:97-101 stops path escape but not extension abuse) | map MIME → a fixed extension whitelist (`.webm .mp3 .wav .ogg .png .jpg .webp`), serve with `Content-Disposition: attachment` + `X-Content-Type-Options: nosniff` and `Content-Security-Policy: default-src 'none'` for that route.

F63 | P3 | src/main/resources/static/audio/ | Several hundred binary MP3 assets are committed to the repo (generated TTS output) → clone/build bloat and they are served by Spring's static handler at `GET /audio/**` permitAll (SecurityConfig.java:104), bypassing the MinIO/cloudinary storage design | move to MinIO/object storage, keep only real fixtures in git (or Git LFS).

F64 | P3 | src/main/resources/db/migration/V1__…V8__*.sql | Eight Flyway migration files exist while Flyway is explicitly disabled (application.properties:20) and the project rule is "no migrations" → dead artifacts that will actively corrupt a future schema if someone re-enables Flyway, and they have already drifted from the entity model | delete them (or rename to `sql/reference/` with a header saying they are not executed).

F65 | P3 | config/WebConfig.java:3-22 | The ENTIRE class body is inside a `/* */` block comment → the file registers nothing; it is the reason `uploads/` is not mapped as static resources while `LessonSubmissionService.java:117` still returns `/api/resources/…` URLs (served by the other controller instead) | delete the file; if a resource handler is needed, add it live.

F66 | P3 | src/main/resources/application.properties:104-106 | `ai.exercise.ollama.base-url` defaults to `http://host.docker.internal:11434/v1` while `ai.speaking.ollama.base-url` (L80) defaults to `http://localhost:11434` → two different defaults for the same service depending on whether the caller runs in or out of the container, and AGENTS.md documents the model-swap cost of getting this wrong | one shared `${OLLAMA_BASE_URL}` for both with a profile-appropriate value in compose.

F67 | P3 | src/main/resources/application.properties:119 | `ai.exercise.duckduckgo.enabled=true` hardcoded (no env override, unlike every other toggle) → the AI generation pipeline makes outbound web research calls by default even in offline/local-only runs, contradicting the "AI 100% local" project stance and burning wall-clock inside the generation timeout budget | `ai.exercise.duckduckgo.enabled=${AI_EXERCISE_DDG_ENABLED:false}`.

F68 | P3 | pom.xml (jjwt 0.12.5 / minio 8.5.14 / cloudinary-http5 2.0.0 / azure-ai-speech 1.51.1 / jsoup 1.18.1) | Version pins are fine, but nothing enforces updates: no `versions-maven-plugin`/`dependency-check` and `spring-boot-starter-parent` 4.0.6 is the only CVE surface being tracked. `azure-ai-speech` pulls a large native/gRPC surface for a feature that is disabled by default (`azure.speech.key=` empty at L84) — it is a dependency-risk for zero shipped value while the key is unset | make Azure Speech an optional profile-scoped module (or delete); add `mvn versions:display-dependency-updates` to CI.

### A.7 Dead code / unused public API

F69 | P3 | service/ExerciseService.java:193 | `getAllExercises()` — no callers, and unbounded `findAll()` (both problems in one line) | delete.

F70 | P3 | service/LessonService.java:57 | `getAllLessons()` — no callers (superseded by `getPublishedLessonPage`, LessonController.java:37) | delete.

F71 | P3 | service/DeckService.java:37-39, :48-50, :41-46, :52-57 | Four dead pre-paging overloads (`getAllPublicDecks()`, `getAllPublicDecks(String)`, `getUserDecks(Long)`, `getUserDecks(Long,String)`); the controller only uses `getPublicDeckPage`/`getUserDeckPage`, so these return raw `List<Deck>` entities including the `owner`/`words` graphs | delete all four.

F72 | P3 | service/SpeakingPromptService.java:67-69 | Dead `getAllPromptsForAdmin()` overload (controller uses the `keyword,Pageable` form at :71); SpeakingPromptService.java:150 `getAllPromptsAdmin()` is a dead unbounded `findAll()`, and L156 `return null` is an unreachable/void-mismatched tail | delete; make the missing-branch throw.

F73 | P3 | service/LeaderboardService.java:53-56 | Dead `getLeaderboard(int limit)` overload (the controller calls the `(page,size)` form) | delete.

F74 | P3 | service/MinioService.java:73-94 | `createReadUrl` has no callers and contains the hardcoded host rewrite from F54 — dead AND wrong | delete.

F75 | P3 | service/MinioService.java:49 | `uploadVideo(MultipartFile)` has no callers (video uploads go through `uploadMedia`/`SpeakingSubmissionService`); it also duplicates bucket/prefix logic | delete; re-derive from `uploadMedia` when needed.

F76 | P3 | config/SecurityConfig.java:99-101 | Three matcher groups (`/api/exercises/submit/**`, `/api/exercises/submissions/**`, `/api/exercises/**`) have NO controller behind them — exercise routes actually live at `/api/admin/exercises/**` (L103 covers admin) and `/api/lessons/{id}/exercises/**` → dead rules that read as if a public `/api/exercises` surface exists | delete the three lines (same clean-up already applied to the `/api/shop/items` rule at L84-85).

F77 | P3 | security/UserPrincipal.java:12 & :51-63 | Unused `Collections` import; `isAccountNonExpired/isAccountNonLocked/isCredentialsNonExpired` hardcoded `true` — fine for now but there is no column behind them, so `AdminService.toggleUserActive` is the ONLY account kill-switch and it does not invalidate an already-issued JWT (15-min TTL, application.properties:38) | document the 15-min revocation window explicitly, or add a Redis denylist keyed by `userId+iat` on deactivation/password change.

F78 | P3 | controller/speaking/SpeakingPromptController.java:31 | Field named `SpeakingPromptService` (capital S, same as its type) — Lombok generates `getSpeakingPromptService()`-style accessors that are ambiguous and it breaks every convention in the codebase | rename to `speakingPromptService`.

F79 | P3 | service/AnswerKeyService.java (`ARR_RESULT_PATTERN`) | Keys answers by `answers.size()` (index-at-time-of-parse) rather than the placeholder index, so out-of-order/blank answers can collide; L48 `continue` swallows a per-item parse failure; the only consumer is `JsonDataSeeder` (a disabled one-time seeder, application.properties:100) | key by the captured index from the pattern; delete with the seeder once the seed has run.

F80 | P3 | security/JwtAuthenticationFilter.java:66-70 | The generic `catch (Exception)` logs and then CONTINUES the chain unauthenticated, while the two JWT-specific catches return 401 (F7-BUG02). A transient DB failure inside `loadUserByUsername` (L43) therefore degrades to "anonymous" → on the permitAll surfaces (F01/F04/F05) an outage silently converts authenticated calls into unauthenticated ones instead of failing loudly | return 503 (or propagate) for non-JWT exceptions; distinguish "no token" from "token but lookup failed".

F81 | P3 | service/SrsService.java:74-131 / FlashcardService.java:64 | SM-2/leitner writes on review with no `readOnly` tx on the read helpers and no `@Version` optimistic lock → two devices reviewing the same card concurrently last-write-wins silently | add `@Version` to the progress entity or a Redis per-user review lock.

F82 | P3 | src/main/java (whole tree) | Zero `TODO`/`FIXME`/`XXX` markers (only two `ENGXXXXXXXXXXXX` doc examples at PaymentService javadoc) and zero `@Deprecated` — good hygiene, but 97 `@Transactional` annotations with ZERO in any controller means the layering is respected | no action; keep as a guardrail check in CI.

---

## SECTION B — COMPLETE ENDPOINT INVENTORY

Auth label = EFFECTIVE security: SecurityConfig first-match-wins (L76-107) + `@PreAuthorize` + manual null/admin checks in the method. `URL` = protected only by the filter chain. `SHADOWED` = a later, stricter rule is dead because an earlier `permitAll` matched.
Format: `METHOD /full/path | Controller#method | auth`

### B.1 AuthController — prefix `/api/auth` (AuthController.java:21)
POST /api/auth/register | AuthController#register | PUBLIC (L76)
POST /api/auth/login | AuthController#login | PUBLIC (L76)
POST /api/auth/forgot-password | AuthController#forgotPassword | PUBLIC (L76)
POST /api/auth/reset-password | AuthController#resetPassword | PUBLIC (L76)
GET /api/auth/me | AuthController#me | USER (L107 anyRequest + manual `authentication == null` → 401, AuthController.java:58-65)
POST /api/auth/change-password | AuthController#changePassword | USER (L89 POST /api/auth/** + manual 401 check L67)
PUT /api/auth/avatar | AuthController#updateAvatar | USER (L107 + manual 401 check L78)
POST /api/auth/avatar/upload | AuthController#uploadAvatar | USER (L89 + manual 401 check L89)

### B.2 LessonController — prefix `/api/lessons` (LessonController.java:19)
GET /api/lessons | LessonController#getAllLessons | PUBLIC (L79)
GET /api/lessons/{id} | LessonController#getLessonDetails | PUBLIC (L79) — but see F34 (writes on a public read)
POST /api/lessons | LessonController#createLesson | ADMIN (L93; controller only checks null → F15)
PUT /api/lessons/{id} | LessonController#updateLesson | ADMIN (L94; F15)
DELETE /api/lessons/{id} | LessonController#deleteLesson | ADMIN (L95; F15)

### B.3 LessonExerciseController — prefix `/api/lessons/{lessonId}/exercises` (LessonExerciseController.java:16)
GET /api/lessons/{lessonId}/exercises | LessonExerciseController#getExercises | PUBLIC (L79); `?includeAnswers=true` downgraded at runtime by the manual ROLE_ADMIN check (LessonExerciseController.java:31-41) → 403 for non-admins
POST /api/lessons/{lessonId}/exercises/grade | LessonExerciseController#gradeExercises | USER (L91) — no `@Valid` (F26)
POST /api/lessons/{lessonId}/exercises/submit | LessonExerciseController#submitExercises | USER (L90) — `authentication.getName()` unguarded (LessonExerciseController.java:59)
GET /api/lessons/{lessonId}/exercises/attempts | LessonExerciseController#getAttempts | **PUBLIC — SHADOWED** (L79 beats L92) → NPE 500 (F01/F02)
GET /api/lessons/{lessonId}/exercises/attempts/{attemptId} | LessonExerciseController#getAttemptDetail | **PUBLIC — SHADOWED** (L79 beats L92) → NPE 500 (F01/F03)
GET /api/lessons/{lessonId}/exercises/content | LessonExerciseController#getCleanContent | PUBLIC (L79) — intentional (answers stripped)

### B.4 LessonStructureController — NO class-level prefix, all paths inline (LessonStructureController.java)
GET /api/admin/lessons/{lessonId}/structure | LessonStructureController#getStructureAdmin | ADMIN (URL L103 only; NONE-ANNOTATED — F17)
POST /api/admin/lessons/{lessonId}/sections | LessonStructureController#addSection | ADMIN (URL L103; F17)
PUT /api/admin/sections/{sectionId} | LessonStructureController#updateSection | ADMIN (URL L103; F17)
DELETE /api/admin/sections/{sectionId} | LessonStructureController#deleteSection | ADMIN (URL L103; F17)
POST /api/admin/sections/{sectionId}/blocks | LessonStructureController#addBlock | ADMIN (URL L103; F17) — `BlockType.valueOf` unguarded (F28)
PUT /api/admin/blocks/{blockId} | LessonStructureController#updateBlock | ADMIN (URL L103; F17)
DELETE /api/admin/blocks/{blockId} | LessonStructureController#deleteBlock | ADMIN (URL L103; F17)
POST /api/admin/upload | LessonStructureController#upload | ADMIN (URL L103; F17) — attacker-controlled extension (F62)
GET /api/resources/{filename:.+} | LessonStructureController#serveResource | PUBLIC (L106) — traversal-guarded (L97-101) but same-origin arbitrary-extension serving (F62)
POST /api/admin/audio-upload | LessonStructureController#audioUpload | ADMIN (URL L103; F17) — Cloudinary, no content-type validation (F61)
GET /api/lessons/{lessonId}/structure | LessonStructureController#getStructure | PUBLIC (L79) — DB-WRITES on this public GET (F05)

### B.5 LessonSnapshotController — no class prefix (LessonSnapshotController.java)
GET /api/admin/lessons/{lessonId}/snapshots | LessonSnapshotController#getSnapshots | ADMIN (URL L103; NONE-ANNOTATED — F20)
POST /api/admin/lessons/{lessonId}/snapshots | LessonSnapshotController#takeSnapshot | ADMIN (URL L103; F20) — `user.getId()` unguarded (L31) but URL rule guarantees a principal
POST /api/admin/lessons/{lessonId}/snapshots/{snapshotId}/restore | LessonSnapshotController#restoreSnapshot | ADMIN (URL L103; F20) — destructive, N+1 restore (F38)

### B.6 LessonSubmissionController — prefix `/api/lesson-submissions` (LessonSubmissionController.java:22)
POST /api/lesson-submissions/submit | LessonSubmissionController#submit | USER (L107) — `@Valid` present
POST /api/lesson-submissions/upload-audio | LessonSubmissionController#uploadAudio | USER (L107 + manual null check L45) — only `file.isEmpty()` validated (F61/F62)
GET /api/lesson-submissions/my/lesson/{lessonId}/skill/{skillType} | LessonSubmissionController#getMySubmission | USER (L107 + manual 401) — 404 flattened to `200 null` (F30)

### B.7 AdminController — prefix `/api/admin` (AdminController.java:19) — ALL 16 NONE-ANNOTATED (F16)
GET /api/admin/stats | AdminController#getDashboardStats | ADMIN (URL L103)
GET /api/admin/users | AdminController#getAllUsers | ADMIN (URL L103) — page/size clamped to 100 (AdminService.java:283-286)
PUT /api/admin/users/{id}/toggle-active | AdminController#toggleUserActive | ADMIN (URL L103) — no self/last-admin guard (F14)
PUT /api/admin/users/{id}/toggle-admin | AdminController#toggleUserAdmin | ADMIN (URL L103) — self-demotion possible (F14)
PUT /api/admin/users/{id}/toggle-premium | AdminController#toggleUserPremium | ADMIN (URL L103)
PUT /api/admin/users/{id}/revoke-premium | AdminController#revokeUserPremium | ADMIN (URL L103)
GET /api/admin/lessons | AdminController#getAllLessons | ADMIN (URL L103)
GET /api/admin/lessons/{id} | AdminController#getLesson | ADMIN (URL L103) — raw `Lesson` entity (F22)
POST /api/admin/lessons | AdminController#createLesson | ADMIN (URL L103) — raw `Lesson` out; `SkillType.valueOf` unguarded (F28)
PUT /api/admin/lessons/{id} | AdminController#updateLesson | ADMIN (URL L103) — raw `Lesson`; `orderIndex=null` wipes (AdminService.java:204)
DELETE /api/admin/lessons/{id} | AdminController#deleteLesson | ADMIN (URL L103)
PUT /api/admin/lessons/{id}/toggle-publish | AdminController#toggleLessonPublish | ADMIN (URL L103)
GET /api/admin/vocabulary | AdminController#getAllVocabulary | ADMIN (URL L103) — raw `Page<Vocabulary>` (F22)
POST /api/admin/vocabulary | AdminController#createVocabulary | ADMIN (URL L103) — raw `Vocabulary`
PUT /api/admin/vocabulary/{id} | AdminController#updateVocabulary | ADMIN (URL L103) — full-field overwrite nulls `definitionEn`/`cefrLevel`/`source` (AdminService.java:262-272) and re-parents `lesson` to null (L272)
DELETE /api/admin/vocabulary/{id} | AdminController#deleteVocabulary | ADMIN (URL L103)

### B.8 AdminExerciseController — prefix `/api/admin/exercises` (AdminExerciseController.java:19)
GET /api/admin/exercises | AdminExerciseController#list | ADMIN (URL L103 + @PreAuthorize L29)
GET /api/admin/exercises/{id} | AdminExerciseController#byId | ADMIN (@PreAuthorize L43)
POST /api/admin/exercises | AdminExerciseController#create | ADMIN (@PreAuthorize L49) — `@Valid`
PUT /api/admin/exercises/{id} | AdminExerciseController#update | ADMIN (@PreAuthorize L55) — MISSING `@Valid` (F27)
DELETE /api/admin/exercises/{id} | AdminExerciseController#delete | ADMIN (@PreAuthorize L62)

### B.9 AdminAiExerciseController — prefix `/api/admin/exercises/ai`, class-level @PreAuthorize (AdminAiExerciseController.java:29)
POST /api/admin/exercises/ai/generate-async | AdminAiExerciseController#generateAsync | ADMIN (ann + URL L103)
POST /api/admin/exercises/ai/generate | AdminAiExerciseController#generate | ADMIN — `ExerciseType.valueOf` unguarded (L44, F28)
POST /api/admin/exercises/ai/generate-all | AdminAiExerciseController#generateAll | ADMIN — returns raw `List<Exercise>` with `correctAnswer` (F23); `valueOf` unguarded (L57)
POST /api/admin/exercises/ai/generate-batch | AdminAiExerciseController#generateBatch | ADMIN — synchronous blocking (F45)
POST /api/admin/exercises/ai/validate | AdminAiExerciseController#validate | ADMIN — 1 LLM call per exercise (F45); `valueOf` unguarded (L103)
GET /api/admin/exercises/ai/status | AdminAiExerciseController#status | ADMIN — reads in-memory map (F44)

### B.10 AdminAnswerBackfillController — prefix `/api/admin/exercises/ai`, class-level @PreAuthorize (:21)
POST /api/admin/exercises/ai/backfill-answers | AdminAnswerBackfillController#backfill | ADMIN (ann + URL L103)
GET /api/admin/exercises/ai/backfill-answers/status | AdminAnswerBackfillController#status | ADMIN — in-memory progress map (F44)

### B.11 AdminExerciseSeedController — prefix `/api/admin/exercises` (AdminExerciseSeedController.java:10)
POST /api/admin/exercises/seed | AdminExerciseSeedController#seed | ADMIN (ann L24 + URL L103) — `force=true` calls `deleteAllInBatch()` on ALL exercises (irreversible, no confirmation param)

### B.12 VocabularyController — prefix `/api/vocabulary` (VocabularyController.java:23)
GET /api/vocabulary | VocabularyController#list | USER (L107) — raw `Page<Vocabulary>` from the repository directly, unclamped page size (F21)
GET /api/vocabulary/search | VocabularyController#search | PUBLIC (L77) — unbounded `LIKE %q%`, ≥2 chars (F29)
GET /api/vocabulary/dictionary/{word} | VocabularyController#dictionaryProxy | PUBLIC (L77) — sanitized at L59
POST /api/vocabulary | VocabularyController#create | USER (L96 + manual null check L68) — ANY learner writes the GLOBAL vocab table; `lessonId` silently ignored (L82 comment); raw `Vocabulary` out; no per-user cap (F21/F12-adjacent)

### B.13 DeckController — prefix `/api/decks` (DeckController.java:18)
GET /api/decks | DeckController#getAllPublicDecks | PUBLIC (L83) — clamped 1..100 (L32), DTO projection
GET /api/decks/my | DeckController#getUserDecks | USER (L83 permitAll + manual 401 null-guard, DeckController.java:44-47 — audit-v7 F34 fix verified in place, covered by DeckControllerMyDecksTest)
GET /api/decks/{id} | DeckController#getDeckById | PUBLIC (L83) — private decks enforced in service (DeckService.java:102-104)
POST /api/decks | DeckController#createDeck | USER (L107) — `@Valid`
PUT /api/decks/{id} | DeckController#updateDeck | USER (L107) — `@Valid`; owner check DeckService.java:126; `description`/`source`/`thumbnailUrl` nulled when omitted (DeckService.java:130-134)
DELETE /api/decks/{id} | DeckController#deleteDeck | USER (L107) — owner check DeckService.java:141
POST /api/decks/{id}/words | DeckController#addWordToDeck | USER (L107) — `Map<String,Long>` body (F31); owner check DeckService.java:150

### B.14 FlashcardController — prefix `/api/flashcards` (FlashcardController.java:14)
POST /api/flashcards/review | FlashcardController#review | USER (L107) — `@Valid`, manual `ResponseStatusException(UNAUTHORIZED)` L23-31
GET /api/flashcards/status/{vocabularyId} | FlashcardController#status | USER (L107) — manual 401 L33-41

### B.15 SrsController — prefix `/api/srs` (SrsController.java:14)
POST /api/srs/review | SrsController#review | USER (L107) — `Map<String,Integer>` body, manual quality 0-5 check (F31)
GET /api/srs/due/{deckId} | SrsController#due | USER (L107) — N+1 (F43)
GET /api/srs/stats | SrsController#stats | USER (L107)

### B.16 GameController — prefix `/api/games` (GameController.java:14)
GET /api/games/quiz/{deckId} | GameController#quiz | USER (L107)
GET /api/games/memory/{deckId} | GameController#memory | USER (L107)
GET /api/games/typing/{deckId} | GameController#typing | USER (L107)
GET /api/games/listening/{deckId} | GameController#listening | USER (L107)
GET /api/games/mixed/{deckId} | GameController#mixed | USER (L107)
POST /api/games/submit | GameController#submit | USER (L107) — `Map<String,Object>` body, manual null-principal, trusts client score on the no-`answers` path (F24)

### B.17 StreakController — prefix `/api/streak` (StreakController.java:14)
GET /api/streak/history | StreakController#history | USER (L107) — `days` unclamped (F32)
GET /api/streak/current | StreakController#current | USER (L107)

### B.18 ProgressController — prefix `/api/users` (ProgressController.java:13)
GET /api/users/progress | ProgressController#progress | USER (L107) — denominator inconsistent with dashboard (F37)

### B.19 DashboardController — prefix `/api/dashboard` (DashboardController.java:14)
GET /api/dashboard/stats | DashboardController#stats | USER (L107) — `totalLessons` counts unpublished (F37); exercises/dailyPoints hardcoded 0

### B.20 LeaderboardController — prefix `/api/leaderboard` (LeaderboardController.java:17, `@Validated`)
GET /api/leaderboard | LeaderboardController#getLeaderboard | PUBLIC (L78) — `@Max(100)` on size and limit

### B.21 AiVocabController — prefix `/api/ai` (AiVocabController.java:21)
POST /api/ai/generate-vocab | AiVocabController#generateVocab | USER (L87) — quota consumed (L66)
POST /api/ai/enrich-word | AiVocabController#enrichWord | USER (L87) — quota consumed (L97)
POST /api/ai/save-vocab | AiVocabController#saveVocab | USER (L88) — NO size cap, NO quota, raw `List<Vocabulary>` out (F12)

### B.22 PaymentController — no class prefix (controller/payment/PaymentController.java)
POST /api/webhook/sepay | PaymentController#sepayWebhook | PUBLIC (L86) — HMAC verified constant-time (PaymentService.java:408/420) but default secret (F07) and no amount check (F08)
POST /api/v1/payment/create-order | PaymentController#createOrder | USER (L107 + manual 401 L38) — `Map<String,String>` body, unvalidated `planType` (F09/F10)
GET /api/v1/payment/status | PaymentController#getStatus | USER (L107 + manual 401 L48) — GET that WRITES premium=false (F35)

### B.23 SpeakingPromptController — no class prefix (controller/speaking/SpeakingPromptController.java)
GET /api/v1/admin/speaking-prompts | SpeakingPromptController#getAllPromptsForAdmin | ADMIN (@PreAuthorize L35 + URL L102)
GET /api/v1/admin/video-prompts | SpeakingPromptController#getAllPromptsForAdmin | ADMIN (same method, dual mapping)
GET /api/v1/speaking-prompts?q= | SpeakingPromptController#getAllPrompts(q) | PUBLIC (L80) — premium filtered by `premiumViewer` (null-safe, L82-87)
GET /api/v1/video-prompts?q= | SpeakingPromptController#getAllPrompts(q) | PUBLIC (L81)
GET /api/v1/speaking-prompts (no q) | SpeakingPromptController#getAllPrompts() | PUBLIC (L80) — paged
GET /api/v1/video-prompts (no q) | SpeakingPromptController#getAllPrompts() | PUBLIC (L81)
GET /api/v1/speaking-prompts/{id} | SpeakingPromptController#getPrompt | PUBLIC (L80 `/**`) — `getPromptForViewer` gates premium content
GET /api/v1/video-prompts/{id} | SpeakingPromptController#getPrompt | PUBLIC (L81 `/**`)
POST /api/v1/admin/speaking-prompts | SpeakingPromptController#createPrompt | ADMIN (@PreAuthorize L90) — `@Valid`
POST /api/v1/admin/video-prompts | SpeakingPromptController#createPrompt | ADMIN
PUT /api/v1/admin/speaking-prompts/{id} | SpeakingPromptController#updatePrompt | ADMIN (@PreAuthorize L98) — `description`/`prompt`/`level`/`referenceText` overwritten unconditionally → null wipes (SpeakingPromptService.java:113-115, :125, :132)
PUT /api/v1/admin/video-prompts/{id} | SpeakingPromptController#updatePrompt | ADMIN
DELETE /api/v1/admin/speaking-prompts/{id} | SpeakingPromptController#deletePrompt | ADMIN (@PreAuthorize L106)
DELETE /api/v1/admin/video-prompts/{id} | SpeakingPromptController#deletePrompt | ADMIN
POST /api/v1/admin/speaking-prompts/ai-generate | SpeakingPromptController#aiGenerate | ADMIN (@PreAuthorize L113) — synchronous LLM call on request thread (F50-adjacent)
POST /api/v1/admin/video-prompts/ai-generate | SpeakingPromptController#aiGenerate | ADMIN
POST /api/v1/admin/speaking-prompts/ai-generate-full | SpeakingPromptController#aiGenerateFull | ADMIN (@PreAuthorize L144)
POST /api/v1/admin/video-prompts/ai-generate-full | SpeakingPromptController#aiGenerateFull | ADMIN

### B.24 SpeakingSubmissionController — no class prefix (controller/speaking/SpeakingSubmissionController.java)
POST /api/v1/speaking-submissions/upload | SpeakingSubmissionController#uploadSubmission | USER + PREMIUM (L107; `requireAuthenticated` L40, premium gate L43-45)
POST /api/v1/video-submissions/upload | SpeakingSubmissionController#uploadSubmission | USER + PREMIUM (same method)
POST /api/v1/speaking-submissions/{id}/assess | SpeakingSubmissionController#assessSubmission | USER (L107; ownership via `getSubmissionForViewer` L62)
POST /api/v1/video-submissions/{id}/assess | SpeakingSubmissionController#assessSubmission | USER
GET /api/v1/admin/speaking-submissions | SpeakingSubmissionController#getAllSubmissionsForAdmin | ADMIN (URL L102 + manual `isAdmin` L75 — NONE-ANNOTATED, F18)
GET /api/v1/admin/video-submissions | SpeakingSubmissionController#getAllSubmissionsForAdmin | ADMIN (same method)
PATCH /api/v1/admin/speaking-submissions/{id}/grade | SpeakingSubmissionController#gradeSubmission | ADMIN (URL L102 + manual `isAdmin` L90; `@Valid`) — F18
PATCH /api/v1/admin/video-submissions/{id}/grade | SpeakingSubmissionController#gradeSubmission | ADMIN
GET /api/v1/speaking-submissions | SpeakingSubmissionController#getUserSubmissions | USER (L107 + `requireAuthenticated` L102)
GET /api/v1/video-submissions | SpeakingSubmissionController#getUserSubmissions | USER
GET /api/v1/speaking-prompts/{promptId}/submissions | SpeakingSubmissionController#getUserPromptSubmissions | **PUBLIC-INTENT CONFLICT**: `/api/v1/speaking-prompts/**` is permitAll (L80), so this nested submissions path matches the earlier permitAll rule → reaches `requireAuthenticated` (L115) which correctly returns 401; the URL rule for submissions is therefore ineffective (defect class of F01, mitigated by the manual guard)
GET /api/v1/video-prompts/{promptId}/submissions | SpeakingSubmissionController#getUserPromptSubmissions | same as above (L81 + manual 401 L115)
GET /api/v1/speaking-submissions/{id} | SpeakingSubmissionController#getSubmission | USER (L107 + `requireAuthenticated` L126; ownership L127-128)
GET /api/v1/video-submissions/{id} | SpeakingSubmissionController#getSubmission | USER

### B.25 VideoLessonController — no class prefix (controller/video/VideoLessonController.java)
GET /api/v1/video-lessons | VideoLessonController#list | PUBLIC (L82) — `level` parsed in try/catch → 400 (VideoLessonController.java:61-65)
GET /api/v1/video-lessons/{id} | VideoLessonController#detail | PUBLIC (L82) — null-safe principal (L74)
POST /api/v1/video-lessons/{id}/attempts | VideoLessonController#submitAttempt | USER (L107 + `requireAuthenticated` L85) — multipart, `lineIndex`/`file`
GET /api/v1/video-attempts | VideoLessonController#myAttempts | USER (L107 + `requireAuthenticated` L96)
GET /api/v1/admin/video-lessons | VideoLessonController#listForAdmin | ADMIN (URL L102 + manual `requireAdmin` L108 — NONE-ANNOTATED, F19)
POST /api/v1/admin/video-lessons | VideoLessonController#create | ADMIN (manual `requireAdmin` L116; `@Valid`) — F19
POST /api/v1/admin/video-lessons/upload | VideoLessonController#createWithTranscriptFile | ADMIN (manual L129; `@Valid @RequestPart meta`) — F19
PUT /api/v1/admin/video-lessons/{id} | VideoLessonController#update | ADMIN (manual L142; `@Valid`) — F19
DELETE /api/v1/admin/video-lessons/{id} | VideoLessonController#delete | ADMIN (manual L150) — F19
GET /api/v1/admin/video-attempts | VideoLessonController#attemptsForAdmin | ADMIN (manual L161) — F19
PATCH /api/v1/admin/video-attempts/{id}/grade | VideoLessonController#grade | ADMIN (manual L170; `@Valid`) — detached `new User()` FK hack (VideoLessonService.java:182-183); `gradedAt` `LocalDateTime.now()` (F52)
POST /api/v1/admin/video-attempts/{id}/ai-grade | VideoLessonController#aiGrade | ADMIN (manual L179) — synchronous Whisper+LLM on request thread (F50)
POST /api/v1/admin/video-lessons/translate-transcript | VideoLessonController#translateTranscript | ADMIN (manual L188) — unbounded `List<TranscriptLine>` body, no `@Valid`/size cap
POST /api/v1/admin/video-lessons/fetch-youtube | VideoLessonController#fetchYoutube | ADMIN (manual L201) — `Map<String,String>` body, SSRF-ish URL fetch delegated to `YouTubeTranscriptService` (validate host = youtube.com/youtu.be)

### B.26 MediaProxyController — no class prefix (controller/MediaProxyController.java)
GET /api/v1/media/** | MediaProxyController#serveMedia | PUBLIC (L105) — NO ownership check on objectKey (F04); hardcoded ACAO (F55)

### B.27 Non-endpoint mappings (documented for completeness)
GET /audio/** (static classpath MP3s) | Spring ResourceHandler | PUBLIC (L104) — F63
GET /actuator/health | Boot actuator | USER (L107 anyRequest) — only the default health endpoint is exposed (no `management.*` keys in application.properties); `/actuator/{info,metrics,prometheus}` are NOT exposed and NOT mapped, so the app has no metrics surface for the fail-open Redis/DB paths (F80)
POST /api/exercises/submit/**, /api/exercises/submissions/**, /api/exercises/** | no controller | dead matchers (F76)
GET /api/shop/items | no controller | rule removed at SecurityConfig.java:84-85 (audit-v7 F47)

Total: 130 `@*Mapping` annotations across 26 controllers (grep-verified count), resolved above including every dual-path `{a,b}` expansion.

---

## SECTION C — WHAT IS ALREADY FINE (do not re-litigate)

- **Webhook signature check is constant-time** — `MessageDigest.isEqual` at PaymentService.java:408 and :420, with a 5-minute replay window (`REPLAY_WINDOW_MS`, PaymentService.java:367); I re-verified this is real, not a TODO.
- **`DeckController#getUserDecks` null-principal NPE is FIXED** — DeckController.java:44-47 with `size` clamped at L48, and pinned by `src/test/java/com/datn/engflow/controller/DeckControllerMyDecksTest.java` (3 tests: 401-not-500, page shape, size→100). My earlier claim was stale; withdrawn.
- **`AdminController` pagination IS clamped** — every admin list funnels through `AdminService.adminPageRequest` (AdminService.java:283-286, `MAX_PAGE_SIZE = 100`); my "unclamped page/size" note was wrong and is withdrawn.
- **Deck / SRS / Flashcard / Speaking-submission IDOR surface is properly owner-checked** — DeckService.java:102 (read), :126 (update), :141 (delete), :150 (add-word); `getSubmissionForViewer` + `findByIdAndUserId` (ExerciseService.java:295) enforce row ownership; all four previously-suspected IDOR claims are REFUTED.
- **No password-hash or token leak through raw entities** — `User.passwordHash` (`@JsonIgnore`, User.java:37), `ExerciseAttempt` (:29), `LessonBlock` (:30), `Lesson.vocabularies` (:80) are all excluded, so the raw-entity findings (F21/F22/F23) leak content/internal columns only, never credentials.
- **JWT secret has no default** — application.properties:36 `jwt.secret=${JWT_SECRET}` fails fast at startup (the pattern F07 should copy for `SEPAY_WEBHOOK_SECRET`); TTL is 15 min (L38), matching the documented rule.
- **`@EnableMethodSecurity` is genuinely live** — 14 `@PreAuthorize("hasRole('ADMIN')")` sites exist and bind (grep of the controller package), and the audit-v5 comment at SecurityConfig.java:30-33 explains why; the gap is coverage (F16-F20), not the mechanism.
- **`.env` hygiene is correct** — `.gitignore` covers `.env`, `uploads/`, `.tailscale/`; `.env.example` holds placeholders only; docker-compose.yml reads every real credential from `.env` (the only compose-side issue is the MinIO defaults, F59).
- **Time/DST handling in schedulers is right** — `StreakReminderScheduler`, `PremiumExpiryScheduler`, `SePayPollingScheduler` all use `Asia/Ho_Chi_Minh` crons with `setIfAbsent` idempotency markers, a catch-up flag defaulted OFF (application.properties:112), and per-job try/catch+log; `AdminService.toggleUserPremium` (:81) and `PaymentService.calculateExpiry` (:462-466) correctly use the injected `Clock`.
- **Password reset OTP is cryptographically random and single-use** — `private static final SecureRandom OTP_RANDOM` (UserService.java:41) feeding `String.format("%06d", OTP_RANDOM.nextInt(1_000_000))` (:272), deleted after successful verification (:297), with a per-email rate bucket (:257-266). Only nit left: the compare at :293 is `String.equals` rather than constant-time (not exploitable remotely given the 6-digit space + rate limit + 10-min TTL).
- **Layering discipline holds** — 97 `@Transactional` annotations, ZERO in any controller; Controller → Service → Repository respected throughout; DTO request/response split is consistent except the raw-entity returns in F21-F23.
- **No TODO/FIXME/HACK debt markers and no `@Deprecated` leftovers** — the one-shot migrations are flag-gated OFF (application.properties:93, :97, :100) rather than left as live code paths, and `CloudTtsService` really was deleted (only a comment remains at SupertonicProxyTtsService.java:14).

---

## Verification notes / honest caveats

- F58 (`JacksonConfig` `@Primary ObjectMapper`) is a *needs-runtime-proof* finding: static reading can't show whether Boot 4's Jackson 3 MVC mapper ignores the Jackson 2 bean. Confirm by logging the resolved `MappingJackson2HttpMessageConverter`/`JsonMapper` at boot before scheduling a fix.
- F01/F02/F03 were validated by rule ORDER only (L79 before L92, first-match-wins); a live `curl GET /api/lessons/1/exercises/attempts` without an Authorization header confirming 500 (not 401) closes it definitively.
- F05 severity depends on whether any lesson currently has zero sections; `SELECT l.lesson_id FROM lessons l LEFT JOIN lesson_sections s ON s.lesson_id=l.lesson_id WHERE s.section_id IS NULL` tells you how much anonymous write amplification is reachable right now.
- F24: the client-trusted scoring branch (GameService.java:231-239) is only reachable when `answers` is absent — confirm whether the shipped frontend ever posts without `answers` before treating it as a live exploit rather than a dormant path.
- No repository file other than this report was modified; all line numbers are from the current working tree.
