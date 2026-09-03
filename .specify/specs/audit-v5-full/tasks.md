# Tasks: audit-v5-full

Ký hiệu: `[x]` = đã xong kèm bằng chứng runtime; `[ ]` = còn lại. Dependency theo thứ tự nhóm.

## Phase 1 — Cấu hình & môi trường (F1, F4, F13)
- [x] T1.1 Audit từng biến `.env` (độ dài token, URL từ trong container) — phát hiện SEPAY_API_TOKEN corruption
- [x] T1.2 Sửa `SEPAY_API_TOKEN` (64 ký tự sạch) — verify `pollingEnabled=true`, hết 401 spam
- [x] T1.3 `AI_SPEAKING_OLLAMA_MODEL` 1.5b→3b, `BASE_URL`→`http://host.docker.internal:11434/v1` — verify rubric call 200
- [x] T1.4 SQL Server cap 2048MB qua `sp_configure` (init-db.sql + live) — verify `sp_configure` output
- [x] T1.5 `MSSQL_PID=Developer`, bỏ env vô hiệu — verify container khởi động sạch

## Phase 2 — Security Java (F2, F3)
- [x] T2.1 `@EnableMethodSecurity` trong SecurityConfig — 221/221 tests pass
- [x] T2.2 Handler `ResponseStatusException` trong GlobalExceptionHandler — free user upload → 403 (đo được)
- [x] T2.3 Rebuild container baked-jar — sweep 67/67

## Phase 3 — Design system & font (F5–F7, F10, F14)
- [x] T3.1 `index.html` bỏ Plus Jakarta Sans — computed font = BVP duy nhất
- [x] T3.2 `design-system.css`: JetBrains Mono→token, `--geo-muted`/`--geo-border`/`--geo-shadow-xl` về đúng prompt
- [x] T3.3 `tailwind.config.js`: mono→BVP, thêm danger/warning/success, xóa block duplicate/chết
- [x] T3.4 `App.vue` xóa skip-link duplicate, toast border token + aria-label
- [x] T3.5 `AdminLayout.vue` bỏ inline font, `bg-pink-500`→`bg-accent`
- [x] T3.6 `FlashcardGame.vue` gradient off-palette → flat token + ink tương phản — verify computed bg rgb(52,211,153)
- [x] T3.7 Quét lại toàn bộ off-token colors — sạch (chỉ còn hợp lệ)

## Phase 4 — Data guards UI (F8, F9)
- [x] T4.1 `LessonExerciseTab.parsedOptions` lọc placeholder `["A".."D"]` + `"null"`
- [x] T4.2 `hasOptionChoices` cho FILL_BLANK/TRANSLATION options thật → nút chọn
- [x] T4.3 `MatchingExercise` fallback text input khi data không có cặp `|` — walkthrough lesson 41881 xác nhận
- [x] T4.4 vitest 73/73 + vite build sạch sau guards

## Phase 5 — Perf (F11, F12)
- [x] T5.1 `show-sql` env-overridable default false — log volume 0 dòng/10 requests
- [x] T5.2 `findBySectionIds` batch trong LessonSnapshotService (takeSnapshot + restoreSnapshot)
- [x] T5.3 Rebuild + `mvnw test` 221/221 + sweep 67/67 + perf measure (lessons 24ms warm)

## Phase 6 — E2E verify (playwright + chrome-devtools)
- [x] T6.1 Speaking E2E (mic→upload→assess) — attempt FAILED đúng hành vi silent-mic
- [x] T6.2 Video shadowing E2E (attempt 7 → admin grade 8.5 → user view)
- [x] T6.3 Premium E2E qua funnel webhook (order ENG2B698A6BC850, idempotent replay)
- [x] T6.4 Deck games: quiz/typing/memory/listening/mixed/flashcard — tất cả tương tác được
- [x] T6.5 Lesson exercise flow 5 types trên lesson 41881 (sau guard)
- [x] T6.6 Admin CRUD qua UI: create→edit→delete lesson; 7 trang admin render; AI generate-async 2/2
- [x] T6.7 Register (DB verify) / forgot-password (OTP Redis) / login sai (alert) / dictionary / leaderboard / profile
- [x] T6.8 chrome-devtools: LCP 366–427ms; CLS culprit = footer mount shift (đã reserve 96px) + font FOUT
- [x] T6.9 Mobile 375px: home/lessons/premium/decks/leaderboard/login — không overflow, menu hoạt động (audit-v5-shots/mobile-*.png)
- [x] T6.10 Vòng loop-test lần 2 (converge): sweep 67/67, vitest 73/73, build sạch, walkthrough 5/5 ĐÚNG + nộp bài 100% — PHÁT HIỆN + SỬA F15 (chấm điểm client-side trên correctAnswer đã strip → luôn SAI)

## Phase 7 — Artifacts & báo cáo
- [x] T7.1 SpecKit: spec/clarify/checklist/plan/tasks (file này)
- [x] T7.2 analyze.md — cross-check spec↔tasks↔code (PASS, 2 task mở còn lại)
- [x] T7.3 REPORT.md tiếng Việt đầy đủ (đã làm / chưa / fix + cách / skill đã nạp)
- [x] T7.4 Commit Conventional Commits (02aa8c1, 83a8ad1) — `.env` không vào git

## Phase 8 — Vòng continue 2 (yêu cầu 2026-09-03 buổi chiều)
- [x] T8.0 Data repair tận gốc tiếp tục: F17 (76 MATCHING→MC), F17b (71 LISTENING→MC), F17c (137 FB/TRANSLATION→MC), F18 (68 MATCHING rỗng→FILL_BLANK), F19 (48 letter-answer bug + 745773 + 7 degenerate) — commits 62ef9ea, 823034c, d6273f6, 4ab6589; E2E grade API verified; sweep 67/67
- [x] T8.1 TTS supertonic → Java: chốt phương án từ feasibility report + doubt-driven review (subagent trả rỗng 2 lần → degraded, tự reconcile từ risk list của report + verify in-container)
- [x] T8.2 TTS: implement sidecar (supertonic_server.py adapter over supertonic's own server + supertonic.Dockerfile + compose service `supertonic` + named volume) + provider flip `${AI_EXERCISE_TTS_PROVIDER:supertonic}` + **root-cause fix HTTP/2→HTTP/1_1 trong SupertonicProxyTtsService** (h2c upgrade làm mất POST body → 422) + 4 Mockito tests. Commit b9bbbd3
- [x] T8.3 TTS: E2E — admin AI-generate LISTENING → Supertonic synth 264KB WAV → Cloudinary → audio_url persisted (lesson 91916, cleaned). Sweep 67/67. UI play: pending T8.5
- [x] T8.4 CLS prod: 0.176→0.108 cold (scrollbar-gutter + always-mounted shell), warm=0; footer 0.108 = SPA-mount artifact, hard-code main height rejected (UX dead space). Commit 1f6ad6a
- [ ] T8.5 Full UI functional gate: mọi chức năng chính qua UI thật, console 0 error/warning → báo user deploy-ready
- [ ] T8.6 Cập nhật spec.md DoD (data-quality mục) + REPORT §4e + commit cuối
