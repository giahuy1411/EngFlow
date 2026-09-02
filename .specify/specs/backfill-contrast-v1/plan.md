# Plan: backfill-contrast-v1

## Context

- 7,076 bài tập seed rỗng `correct_answer` (1,171 FILL_BLANK + 5,905 MULTIPLE_CHOICE), id 662174–704728 trong 41,466-row seed corpus. Hot lessons: 11506 (79), 11723/653/11086 (67), 863/11275/11925 (60), 11919 (45).
- Sample garbage: ex 662174 FB "Tom Crist11…… (get)…", ex 662177 MC "answered 13 was 14 was 15 decided 16 gave" (answer-key-as-question) — một subset MC không thể backfill vì question chính là answer key.
- Ollama local: qwen2.5:1.5b 82 tok/s GPU, OLLAMA_MAX_LOADED_MODELS=1. Whisper sidecar không liên quan.
- AiExerciseService pattern: 202 + Redis progress (BatchProgress) qua endpoint `GET /api/admin/exercises/ai/status?batchId=` — tái dùng pattern này cho backfill.
- Accent: `--geo-accent` #8B5CF6 (design-system.css) + `geo.accent` (tailwind.config.js); stat-num #F472B6 trên nền pink. Playful Geometric spec giữ nguyên toàn bộ (cream, #1E293B borders, blob, bounce, Be Vietnam Pro).

## Goals / Non-Goals

**Goals**
1. MC/FB rỗng được backfill bằng AI + validation nghiêm ngặt, resumable, dry-run, progress pollable.
2. Accent #7C3AED đạt AA ≥4.5:1, stat-num text đạt AA, design system nguyên vẹn.
3. Baselines 197 + 73 xanh.

**Non-Goals**: MATCHING rỗng, hide/filter, vocab dups, index DB, git commit, palette redesign khác.

## Milestones Sequence

```
1. Accent swap (frontend token) ──► vitest 73 ──► browser verify + Lighthouse
2. Backfill backend (service + endpoint + tests) ──► mvnw 197 ──► rebuild container
3. Pilot limit=50 ──► verify validation ──► full run 7,076 ──► verify DB + sample
4. Baseline re-test + analyze + report
```

## Decision #1: Backfill pipeline (mở rộng)

- **Repository**: `ExerciseRepository` + method mới `findEmptyAnswerExercises` (page-able theo lesson_id ASC, id ASC), `countEmptyAnswerExercises`.
- **Service**: `AiAnswerBackfillService` — async, checkpoint mỗi lesson vào Redis (key `backfill:v1:checkpoint`, lưu last processed exercise id + counters), retry 2 attempts/exercise, prompt grounded bằng lesson title + question + options (FB: question có `____`), temperature 0.5, JSON output `{id, answer}` per exercise, batch prompt nhiều exercise/lần call để tối ưu tok/s.
- **Validation** (LLM05 — AI output là input không tin cậy):
  - MC: answer trim case-insensitive phải match MỘT option trong options JSON (chọn variant trong options để ghi). Nếu không match → đếm unfillable, KHÔNG chế đáp án.
  - FB: 1–120 chars, không chứa "left|right", "answer", "|", chỉ reject placeholder rõ ràng. Đáp án ghi thuần text.
  - Không bao giờ eval/innerHTML AI text (không liên quan backend, chỉ note).
- **Endpoint**: `POST /api/admin/exercises/ai/backfill-answers?dryRun=true|false&limit=N` → 202 {batchId}; progress `GET /api/admin/exercises/ai/status?batchId=` (tái dùng). Response cuối: {total, processed, backfilled, unfillable, errors, lastExerciseId}.
- **Write path**: JPA save (transactional per lesson) — an toàn hơn raw SQL, trigger Hibernate validators tự nhiên.
- **Dry-run**: chạy full pipeline kể cả call Ollama + validation nhưng KHÔNG save — đo tỷ lệ unfillable trước khi chạy thật.

### Risks / Trade-offs

- 7,076 bài × ~1s/call batching 10 bài/call ≈ 12–20 phút GPU liên tục — chấp nhận được (async 202).
- Model drift copy question làm answer (như ex 662177) → validation chặn, vào unfillable. Ước lượng 10–20% MC là answer-key garbage.
- Token TTL ngắn không vấn đề — backfill chạy server-side async, không phụ thuộc client.

## Decision #2: Accent AA

- Chỉ đổi 2 token: `--geo-accent: #8B5CF6 → #7C3AED`, `geo.accent` (+ mọi alias reference) trong tailwind.config.js; stat-num text `#F472B6 → #DB2777` (pink-600, 4.7:1 trên nền pink-100 — giữ layout/nền pink-100).
- Verify: computed style của Candy Button bg + Lighthouse contrast trên Home + FocusState ring.
- Không đụng: shadows (#1E293B), cream, radii, fonts, animations.

## Dev Terminal Record

(điền trong quá trình triển khai)
