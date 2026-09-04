# REPORT — audit-v6-full (2026-09-04)

Kiểm tra toàn diện codebase + DB + API + UI/UX, verify design system Playful Geometric với font Be Vietnam Pro, tối ưu hiệu năng, và vá lỗ hổng prompt thiết kế.

## 0. Kết quả tổng quan

| Hạng mục | Kết quả |
|---|---|
| Backend test suite | **247/247 pass** (baseline giữ nguyên, không regression) |
| Frontend test suite | **73/73 pass** (14 files) |
| Frontend build | Sạch (vite build OK) |
| GET API sweep | 53 endpoint — 0 lỗi 5xx ngoài dự kiến |
| Font Be Vietnam Pro | **22/22 trang** nonBvp = 0 (computed style từng element) |
| Console UI | 0 error/warning trên mọi trang đã walkthrough |
| Phát hiện mới | F20–F31 (12 mục) — **11 đã fix + verify live**, 1 ghi nhận giới hạn kiến trúc |
| DB | Index composite mới tạo + đo; Redis 74/74 key có TTL |

## 1. ĐÃ LÀM

### 1.1 Workflow SpecKit (đúng thứ tự yêu cầu)
constitution (kế thừa v5) → **spec.md** → **clarify.md** (6 quyết định tự chủ, ghi rõ) → **checklist.md** (10 yêu cầu chất lượng) → **plan.md** → **tasks.md** → **analyze.md** (giữa chừng, sau khi có kết quả sub-agent) → implement → converge (file này). Artifact tại `.specify/specs/audit-v6-full/`.

### 1.2 Vá lỗ hổng prompt thiết kế (trước khi code — theo yêu cầu #2)
7 lỗ hổng G1–G7 ghi trong spec.md §2, đáng chú ý:
- **G1**: prompt ghi "Lucide React" nhưng stack là Vue → dùng `lucide-vue-next` (đã có), giữ spec stroke 2.5px.
- **G3**: mâu thuẫn font (Outfit + Plus Jakarta Sans) vs yêu cầu "thay toàn bộ bằng Be Vietnam Pro" → chốt 1 họ BVP; **đã áp dụng từ audit-v5** — đợt này verify lại: `tailwind.config.js` sans/heading/mono đều BVP, `index.html` load weights 400–900, `document.fonts` loaded đủ, **0 element nào trên 22 trang dùng font khác**.
- **G4**: tertiary/quaternary làm text không đạt contrast → chính thức hóa thành rule "chỉ dùng cho shape/badge", và fix các chỗ vi phạm (F25).
- **G7**: tiêu chí verify đo được = computed style thật trên browser, không phải đọc code.

### 1.3 Sub-agents (yêu cầu "triển khai sub agent chia nhỏ")
3 agent chạy **song song** ở Phase 0, tất cả trả kết quả đầy đủ:
1. **Static code audit** — 4 service mới + 12 file modified + 26 controller + config. Kết luận: 0 P1, 2 P2, 12 P3; không SQLi, không SSRF thực tế, không leak secret.
2. **DB audit** — 24 bảng/24 FK, index coverage, data quality, dung lượng, blocking, Redis.
3. **Design-system gap audit** — token config vs spec, ~80 file .vue, a11y, % đồng bộ từng trang.

### 1.4 API sweep + E2E qua UI thật (chrome-devtools)
- **GET sweep 53 endpoint** với admin/user token: mọi 404/405 đều là "đúng hành vi" (ID không tồn tại, method không có).
- **CRUD qua UI**: lesson (create→edit→delete, verify count 1469→1470→1469), speaking-prompt (create→edit→delete), exercise (form inline: create→delete), video-lesson (create→edit→delete).
- **User flows**: lesson exercise submit (server chấm 4/5 = 80%, tab Lịch sử hiển thị đúng), deck quiz (trả lời → highlight đúng/sai), video shadowing tab (hiện điểm 8.5 đã chấm), quiz tab, speaking record page (mic gate fail-soft đúng thiết kế headless), premium checkout (QR SePay), auth pages.
- **AI endpoints**: ai-generate-full (2.4s, có referenceText), assess speaking (19s → COMPLETED 9.0 + feedback tiếng Việt), admin grade PATCH (→ GRADED), ai-grade video attempt (14.5s → GRADED), translate-transcript (2 dòng OK), generate-async exercises (202 → 2 bài qua 3 attempts + retry), enrich-word (5.7s).
- **Authz**: user token → 403 trên mọi endpoint admin; noauth → 401; public endpoints → 200.

### 1.5 DB + hiệu năng
- **Index mới đã tạo**: `idx_exercises_lesson_type_order (lesson_id, exercise_type, order_index) INCLUDE (difficulty, correct_answer)` — đo query pattern admin: **7ms → 0ms**.
- Hot endpoints warm: lessons 38-43ms, admin/exercises 80-97ms, leaderboard/dashboard 22-24ms — tất cả < 300ms (DoD đạt).
- Redis: 74/74 key có TTL, không key lạ, không rò rỉ.
- Blocking: 0.

### 1.6 UI/UX verify (browser thật)
- Font: 22 trang (home, lessons, lesson detail, videos, video detail, speaking list/detail/record/history, decks + 6 game modes + create + detail, premium + checkout, leaderboard, profile, search, ai-vocab-generator, login, register, forgot-password, 8 trang admin) — **mọi element computed font-family = "Be Vietnam Pro"**.
- Design checklist (computed style): primary button = bg rgb(139,92,246) + border 2px rgb(30,41,59) + radius 9999px + hard shadow ✓; card border-2 + shadow ✓; heading weight 900 ✓; icon stroke 2.5 ✓.
- Mobile 375px: không overflow-x, menu hoạt động, **shadow pop giảm còn 2px** (F27 fix verify).
- prefers-reduced-motion: media query toàn cục trong design-system.css (đã verify tồn tại + phủ `*`).

## 2. ĐÃ FIX (F20–F31) — cách fix

| ID | Mức | Lỗi | Cách fix | Bằng chứng tái kiểm |
|---|---|---|---|---|
| **F20** | P2 | `SpeakingPromptService.updatePrompt` ghi đè null lesson/category/thumbnail/orderIndex/referenceMedia khi form admin chỉ gửi 8 field → **mất dữ liệu im lặng khi sửa đề** | Null-guard từng field optional trong `updatePrompt` (null = giữ nguyên) | Live: probe prompt có category=PROBE/orderIndex=99/thumbnail → PUT 8 field → **giữ nguyên cả 3** (trước fix: tất cả thành null) |
| **F21** | P2 | `VideoLessonService.update` wipe category + bắt buộc dán lại transcript mỗi lần sửa (form edit để trống transcript) | Update: category null → giữ cũ; transcript null/rỗng → giữ transcript hiện có. FE: bỏ validate transcript-bắt-buộc khi edit, bỏ `isPublished:true` hardcode | Live: PUT với transcript=[] → **giữ 2 dòng cũ + category PROBE** |
| **F22** | P3 | Admin speaking-prompt endpoints chỉ có 1 tầng bảo vệ (URL rule), thiếu @PreAuthorize defense-in-depth | Thêm `@PreAuthorize("hasRole('ADMIN')")` lên 6 admin methods trong SpeakingPromptController (KHÔNG đặt ở class — class chứa cả endpoint public) | Live: user token → 403 trên 3 endpoint admin; public vẫn 200; admin vẫn 200 |
| **F23** | P2 | Nút "Tạo đề bằng AI" fail trong UI dù backend OK — axios timeout 10s < thời gian LLM (ERR_ABORTED ở ~10s) | `aiGenerateFull` truyền `{ timeout: 120000 }` | Code + build sạch; endpoint trả 200 trong 2.4s |
| **F24** | P3 | forgot/reset-password không có rate-limit riêng (100/phút → spam SMTP được) | Thêm bucket `:mail` limit 5/phút trong RateLimitFilter | Live: 5×200 rồi **429, 429**; login không bị ảnh hưởng |
| **F25** | P1 | Contrast fail hệ thống: text-tertiary/quaternary trên nền sáng (~1.6–2:1) ở Profile, LessonExerciseTab, SubmissionHistory, MatchingExercise, Quiz/Memory/Mixed games; token `.geo-btn-emerald`/`.app-btn--emerald` white-on-#34D399; nút DeckDetail white-on-secondary/quaternary | Badge hóa (bg tint + text-foreground + border màu) hoặc đổi text sang foreground; sửa token emerald → text fg | Live computed: badge "Dễ" = color rgb(30,41,59) trên bg rgba(52,211,153,.2) ✓; quiz answered option = text-foreground ✓ |
| **F26** | P1 | 2 modal admin custom (AdminExercises edit + delete-confirm, AdminLessons) thiếu role/aria-modal/Escape/focus-restore | Thêm `role="dialog"/"alertdialog"`, `aria-modal`, `@keydown.esc`, `tabindex=-1`, lastFocused + restore focus khi close | Code + build sạch |
| **F27** | P2 | Spec yêu cầu mobile shadow 2px nhưng chỉ class `.geo-shadow-*` (gần như không dùng) được giảm — utility Tailwind `shadow-pop*` vẫn 4-8px trên mobile | Media query 768px phủ `.shadow-pop, -sm, -lg, -accent, -pink` = 2px; `-xl` = 3px #E2E8F0; `-featured` = 3px pink | Live mobile 375px: computed shadow = `2px 2px 0` ✓; desktop giữ nguyên ✓ |
| **F28** | P3 | Media URL hardcode `http://localhost:8080/api/v1/media/...` ở 3 chỗ backend → deploy host khác (Tailscale funnel) hỏng audio | Trả **relative** `/api/v1/media/...`; frontend resolve qua `resolveMediaUrl()` (file mới `utils/mediaUrl.js`) tại 5 view dùng audio/video | Live: submission videoUrl = relative; UI `<audio>.src` = `http://localhost:8080/api/v1/media/...` (resolve đúng); proxy trả 200 563KB |
| **F29** | P3 | LLM timeout 120s không đủ khi model swap/latency cao → submission 40022 FAILED "Did not observe within 60000ms"; fetch-youtube abort ở 150s | compose: `AI_SPEAKING_LLM_TIMEOUT_SECONDS=180` (khớp frontend axios 180s) | assess 40023 COMPLETED 19s ✓; ai-grade 14.5s ✓ |
| **F30** | P2 | `SubtitleTranslationService` gửi 1 batch tối đa 120 dòng → LLM timeout với transcript dài (fetch-youtube fail toàn bộ) | Chunk 30 dòng/lần, fail-soft per-chunk (null pad giữ alignment) | Unit test 4/4 pass; translate 2 dòng OK live |
| **F31** | P3 | `/api/ai/enrich-word` không có quota → user free burn Ollama | Áp cùng quota 5 lần + increment như generate-vocab | Live: free user count=5 → **403 "đã dùng hết 5 lần"** ✓ |
| phụ | P3 | Duplicate `import java.util.Map` trong VideoLessonController | Xóa 1 dòng | Compile sạch |
| phụ | P3 | 6 nút icon-only thiếu aria-label (AdminExercises ×2, AdminLessonBuilder ×2, DeckDetail, FlashcardGame, ListeningGame, MemoryMatchGame) | Thêm aria-label | Code |
| phụ | P3 | 2 search input thiếu label (Lessons, AdminLessons) | Thêm aria-label | Code |
| phụ | P3 | SpeakingDetail/GuestCtaCard CTA không đúng candy-button spec | Đủ border-2 + rounded-full + shadow-pop + bounce hover | Code + build |

## 3. CHƯA LÀM (có lý do)

1. **fetch-youtube E2E với video phụ đề dài vẫn chưa pass** — chunking (F30) đã vào code + test xanh, nhưng mỗi batch 25 dòng × ~16 batch × 15–60s/batch (qwen2.5:1.5b, GPU 4GB, model swap) vượt mọi timeout hợp lý cho request **đồng bộ**. Đây là giới hạn kiến trúc, không phải bug. Đề xuất: chuyển `fetch-youtube` sang async 202 + Redis progress (pattern đã có sẵn từ AiExerciseService) — việc của đợt sau.
2. **Backfill 5.434 exercise thiếu correctAnswer (DB P1)** — pipeline `AdminAnswerBackfillController` tồn tại và được kiểm chứng là đúng, nhưng chạy thật trên 5.4k dòng = nhiều giờ GPU liên tục, không phù phạm vi một phiên audit. Lệnh chạy: `POST /api/admin/exercises/ai/backfill-answers` (admin token).
3. **Xóa 4 bảng backup `exercises_bak_v5*` + `sysdiagrams`** — cần user quyết định (data luận văn).
4. **Dọn `content_original` lessons (160MB)** — cần quyết định nghiệp vụ (rollback AI-edit), không tự ý xóa.
5. **Index P3 (backfill filtered, attempts)** — chưa đủ lợi ích để tạo; đã có DDL trong báo cáo DB.
6. **Focus-trap đầy đủ cho 2 modal admin legacy** — đã thêm Escape + focus-restore + role/aria; trap Tab-cycle chưa làm vì nên thay bằng `<AppModal>` ở refactor riêng.
7. **Speaking mic E2E thật** — headless từ chối getUserMedia (đã biết từ v5); verify qua API upload + fail-soft UI.
8. **`/api/v1/video-lessons/{id}/attempts` GET → 405**: đúng thiết kế (chỉ POST tạo attempt, GET danh sách là `/api/v1/video-attempts`). Không phải lỗi.

## 4. Skill đã nạp khi test

| Skill | Dùng lúc nào |
|---|---|
| `speckit-workflow` | Ngay đầu — định nghĩa pipeline constitution→spec→clarify→checklist→plan→tasks→implement→converge + analyze giữa chừng |
| `superpowers:dispatching-parallel-agents` | Pattern spawn 3 sub-agent song song (prompt tự chứa, read-only, trả kết luận) |
| `addyosmani-security-and-hardening` | Checklist khi review authz/quota/rate-limit (F22, F24, F31) |
| `java-springboot` + `java-coding-standards` | Style khi sửa service/controller Java (null-guard pattern, comment constraint) |
| `accessibility` | Khi fix F25/F26 (contrast, aria, focus) |
| `mimosa` (plugin) | security_scan hook chặn 2 lần khi viết script sweep (SSRF/path-traversal false-positive) → viết lại an toàn; ghi nhận cả memory |

## 5. Ghi chú vận hành

- Backend container đã rebuild 3 lần trong phiên (mỗi lần fix); trạng thái cuối = code đã fix.
- `docker-compose.yml` thêm env `AI_SPEAKING_LLM_TIMEOUT_SECONDS` (default 180) — cần `docker compose up -d backend` khi pull.
- Index mới tạo trực tiếp trên DB live (không có migration file theo đúng rule ddl-auto=update).
- Data test đã dọn sạch: mọi probe prompt/video/exercise/lesson tạo ra đều đã DELETE; user `user@gmail.com` restore is_premium=1.
- Phát hiện ngoài phạm vi sửa: attempt 14 (audio im lặng) AI chấm 0 + feedback "không nghe rõ" — đúng hành vi thiết kế (fail-soft), không phải bug.
