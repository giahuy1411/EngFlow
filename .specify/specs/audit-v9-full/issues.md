# issues.md — local drafts only (no GitHub call was made)

`taskstoissues` was run in local-only mode per the approved plan: these are ready-to-file drafts with title, body,
labels, milestone, estimation and branch name. Nothing was created on GitHub and no network write occurred.

Milestone for all of them: **audit-v9 follow-ups**.

---

## 1. Backfill or retire the 4,848 exercises with an empty `correct_answer`

**Labels**: `data-quality`, `content`, `backend`  **Estimate**: L (needs a content decision)  **Branch**: `codex/data/empty-answer-backlog`

**Measured**: `p6_db_audit_v9.sql` block [7] → `exercises with empty correct_answer = 4848`.
The deterministic backfill (audit-v8 P3.4 gate B) already filled 586/5,434 from `<summary>ANSWER` keys and was left
idempotent; the remainder are mostly MC-fragment scrape artefacts whose `question` is not a real prompt, so the grader
already returns `ungradeable=true` for them.

**Why it is not a code bug**: the grader deliberately excludes ungradeable items from the score denominator instead of
treating `"" == ""` as correct (verified: `p2` grade/submit probes and `ExerciseServiceGradingTest`).

**Options**: (a) content owner reviews and deletes ungradeable rows; (b) keep them and surface an "ungradeable" badge;
(c) run `mode=full` with a quality guard — measured in audit-v8 as producing junk keys with `qwen2.5:1.5b`
(`"1. a"` for a grammar gap), so it requires new guards first.

**Acceptance**: a decision recorded, and `SELECT COUNT(*) FROM exercises WHERE correct_answer IS NULL OR LTRIM(RTRIM(correct_answer))=''` either trending to 0 or explicitly accepted with a documented reason.

---

## 2. Regenerate `audio_url` for the 9 LISTENING exercises that have none

**Labels**: `content`, `tts`, `data-quality`  **Estimate**: M  **Branch**: `codex/content/listening-audio-backfill`

**Measured**: block [7] → `LISTENING exercises missing audio_url = 9` (down from 89/449 before the restore, per
AGENTS.md). The only audio-producing path is the MCP `generate_listening` flow (supertonic → `POST /api/admin/audio-upload`
→ Cloudinary); the in-app TTS sidecar only feeds listening playback.

**Blocker to check first**: the audit did not re-prove that this pipeline can mint an audio URL for these specific rows
in one pass (requires TTS sidecar + Cloudinary + the MCP venv). Per the plan, only regenerate if that measurement
succeeds; otherwise leave here.

**Acceptance**: 0 LISTENING rows with a blank `audio_url`, or a recorded reason per row.

---

## 3. Decide the fate of the four `exercises_bak_v5*` tables

**Labels**: `chore`, `database`  **Estimate**: S  **Branch**: `codex/chore/drop-v5-backups`

**Measured**: block [1]/[14] → `exercises_bak_v5` 481 rows / 1.1 MB, `exercises_bak_v5b` 322 / 0.4 MB,
`exercises_bak_v5c` 55, `exercises_bak_v5d` 39 (≈1.9 MB total). No entity or repository references them.

**Why it is not a v9 finding**: they are deliberate rollback material from the v5 answer backfill. Dropping them is an
owner decision, not an audit cleanup.

---

## 4. Delete the 8 legacy `zz*@example.com` audit users

**Labels**: `chore`, `database`  **Estimate**: S  **Branch**: `codex/chore/legacy-audit-users`

**Measured**: block [9] → `users with audit prefix = 8`. Pre-existing across audits; they are part of the 76-user
baseline, so removing them changes the documented parity line.

**Caution recorded from this audit**: a first draft of `v9_cleanup_sweep.py` deleted 4 of them with a bare
`email LIKE 'zz%'`; they were restored verbatim from the 21:06 backup. Any cleanup must be date/namespace scoped and
must re-assert parity after.

---

## 5. Content gaps: 6 lessons with empty `content`, 3 duplicate vocab words, 1 duplicate lesson title

**Labels**: `content`, `data-quality`  **Estimate**: M  **Branch**: `codex/content/gap-sweep`

**Measured**: blocks [6] and [7]. Duplicates have no unique index (schema is Hibernate-owned), so these are hygiene
items rather than constraint violations.

---

## 6. Unify the remaining legacy error shape with ProblemDetail

**Labels**: `backend`, `api-contract`  **Estimate**: M  **Branch**: `codex/api/problemdetail-unify`

**Measured**: `p13_error_contract.js` (round 2) → 3 of 10 error-carrying endpoints answer `{"error": "..."}` instead of
the `{type,title,status,detail,instance}` shape: empty word (validation), missing word field, save-vocab empty array.

**Why deferred**: the frontend reads `data.error` in the legacy paths, so changing the shape is a coordinated frontend +
backend change, not an audit patch. The audit's own classification counts these as "known legacy, not a regression".

---

## 7. `/actuator/mappings` is not exposed (tooling boundary)

**Labels**: `tooling`  **Estimate**: S  **Branch**: n/a

**Measured**: `GET /actuator/mappings` → 404 with a valid admin token; `/actuator` lists only `health` and
`health-path`. audit-v9's plan assumed a live registry dump. The audit used a source-derived inventory instead
(144 mappings, 26 controllers) plus a live coverage gate.

**Decision**: do NOT expose more actuator endpoints for auditing; keep the source-derived inventory + coverage gate as
the standing method. Recorded here so the next audit does not repeat the assumption.
