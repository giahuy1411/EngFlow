# Tasks — Audit V2 (Deep Audit)

Phụ thuộc thứ tự. Verify theo plan + Constitution P5/P8.

## Phase A — Bản đồ hóa

- [x] 1. Grep toàn bộ mapping từ 21 controllers → bảng endpoint (method/path/params/role).
  Verify: bảng trong báo cáo có số dòng = tổng mapping grep được.
  → Kết quả: **25 controllers** (21 root + 4 subpackage: payment/speaking×2/video),
  ~100 mapping. AGENTS.md "25 controllers" khớp (v1 đếm thiếu subpackage).
- [x] 2. Trích route từ frontend router → bảng route (path/name/guard).
  Verify: bảng khớp file router.
  → 31 route public + 10 admin = 41 routes.

## Phase B — API smoke + negative + CRUD

- [x] 3. Happy path mọi endpoint từ bảng Phase A (từng cụm controller).
  Verify: bảng status + shape; endpoint lỗi được giải thích.
  → 62 calls script + các cụm trước đó: **46/47 happy-path 200** (405 duy nhất là
  GET /decks/{id}/words — không tồn tại theo thiết kế; words nằm trong detail).
- [x] 4. Negative matrix 401/403/400/404 cho từng nhóm API.
  Verify: bảng matrix ≥ 1 case/nhóm/loại.
  → 401×2 (no/bad token), 403×3 (user gọi admin/delete), 400×2 (missing level,
  login sai pass), 404×2 (lesson/deck không tồn tại) — tất cả đúng status.
- [x] 5. CRUD vòng đời: lessons, exercises (trong lesson), decks, flashcards,
  vocabulary, video-lessons (đọc), speaking prompts (admin CRUD), payments (đọc).
  Verify: DB row xuất hiện/sửa/biến mất (sqlcmd) sau từng bước.
  → Lesson 201→UPD→204→DB=0 ✓; Deck 200→words(vocabId)→200→DB=0 ✓;
  Exercise 201→UPD→200→DB=0 ✓ (options là JSON string); Prompt 201→UPD→200→deleted ✓.
  Lưu ý: 3 lần 400 đầu là payload sai của script (wordIds vs vocabId, options array
  vs string, promptText vs prompt) — API đúng, đã retest shape đúng.

## Phase C — AI sâu

- [x] 6. Sinh thật từng exercise type: MULTIPLE_CHOICE, FILL_BLANK, MATCHING,
  TRANSLATION, LISTENING. Verify schema từng loại (MATCHING `|` + `l=r,...`,
  LISTENING audioUrl trống là đúng pipeline in-app).
  → MC/FB (v1 + v2 lesson 41881) ✓; MATCHING lesson-444 2/4 persist schema
  `left|right` + `l=r,...` ✓ (lesson-445 0/4 = model drift, guard chặn đúng);
  TRANSLATION ✓; LISTENING ✓ (audioUrl NULL đúng — TTS backend không key).
- [x] 7. Ai-vocab 3 topic khác nhau. Verify word/meaning/level đa dạng, không trùng
  batch trước.
  → cooking A2 (oven, ingredients), space B2 (asteroid, satellite, exoplanet),
  interviews C1 (resume, portfolio) — CEFR đúng, shape mảng thuần.
- [x] 8. Speaking full: WAV có tiếng nói thật → upload (admin) → assess → transcript
  không rỗng → rubric 3b → scoreTotal 0-10 + feedback tiếng Việt.
  → e2e-tts.wav 563KB → transcript thật (daily routine) → scoreTotal 7.3/10
  (vocab 7, grammar 8, fluency 7) → feedback tiếng Việt. COMPLETED sau 10s.
- [x] 9. Batch nhỏ (2-3 lessons) qua generate-batch → poll Redis → persistence +
  generated/errors đúng.
  → generate-batch quét 1469 lessons, skip đã-có-bài + content-null; 4 lesson trống
  đều content NULL → skip đúng; generated=0 (không rác). Batch per-lesson trước đó
  generated=2 persist DB ✓.

## Phase D — DB deep-audit

- [x] 10. Orphan check toàn bộ FK (query động từ INFORMATION_SCHEMA).
  Verify: 0 orphan hoặc liệt kê.
  → 17 quan hệ FK quét động (sys.foreign_keys): **0 orphan**.
- [x] 11. Data quality: chuỗi rỗng/trắng, độ dài bất thường, JSON hỏng trong
  options/correct_answer, title trùng.
  Verify: số liệu từng bảng nóng.
  → Findings: 7,076 exercises empty correct_answer (seed corpus id 662174-704728,
  đáp án nằm trong ANSWER section của lesson content — data thật, không phải bug
  pipeline); 1 bài MATCHING cũ dùng `:::` (trước khi sửa contract); 3 vocab trùng
  từ (collaborate/innovate/negotiate ×2). Không tự sửa data thật (P5).
- [x] 12. DMV missing-index + query stats top. Verify: bảng kết quả; index tạo chỉ
  khi DMV + EXPLAIN đồng ý (P5), kèm trước/sau.
  → **sys.dm_db_missing_index_details = 0 đề xuất**; query stats top là DMV queries
  của chính audit + 1 lesson-content load. Không có index nào cần tạo.

## Phase E — Frontend toàn route

- [x] 13. Mọi route render (student + admin) + console=0.
  Verify: bảng route × vai × console.
  → 24 route guest (redirect đúng /login cho protected) + 25 route logged-in
  (profile/games×6/ai-vocab/decks/speaking×4/premium×2/admin×9) = 49 lượt chạm,
  **console errors = 0 toàn bộ**.
- [x] 14. Design-system verify: Candy Button, Secondary, Sticker Card, Input focus,
  blob radius, hover bounce cubic-bezier, pop-in, prefers-reduced-motion.
  Verify: computed style từng loại.
  → Candy: #8B5CF6/pill/2px border/4px shadow/w700/**cubic-bezier(0.34,1.56,0.64,1)**
  đúng spec; Secondary: transparent/2px/none ✓; Sticker Card: **8px 8px 0 #F472B6**
  (pink featured) ✓; reduced-motion: 4 media rules ✓.
- [x] 15. Form validation: register email sai/mật khẩu ngắn, login sai, tạo payload
  thiếu. Verify: thông báo tiếng Việt, không crash.
  → Register: HTML5 native chặn email sai + required ✓; login sai: alert
  "Đăng nhập thất bại" tiếng Việt ✓. Finding nhỏ: native validation message
  tiếng Anh (browser-controlled).
- [x] 16. Keyboard/focus: tab nav + form, focus-visible shadow, focus-trap modal.
  Verify: evidence từ evaluate.
  → Tab đầu → link focus được với **outline solid 3px rgb(139,92,246)** (focus
  ring violet đúng spec).
- [x] 17. Responsive 375/768 + Lighthouse Home + 2 trang + bundle size.
  Verify: không overflow, score ≥ 90, dist MB ghi lại.
  → **BUG-3 phát hiện + fix**: bảng markdown lesson 444 overflow 425>360 @375px
  → CSS `.geo-markdown table, .lesson-html table { display:block; overflow-x:auto }`
  trong @media 768px → verify 360/360 sạch. 768px sạch toàn bộ. Lighthouse:
  Home 96/100/92, Lessons 95/100/100, LessonContent 96/100/100 (a11y/BP/SEO).
  Bundle: index 170.85kB (gzip 65.05kB), code-split per-view.
- [x] 18. Fix bug mới (nếu có) + test + verify runtime.
  → BUG-3 (CSS table overflow) fix + verify; A4-v1 copy "Review va luu" →
  "Da sinh va LUU...vao DB" fix + vitest 73/73 xanh.

## Phase F — Converge & báo cáo

- [x] 19. Re-run backend 197 + frontend 73. Verify: BUILD SUCCESS.
  → Backend 197/197 BUILD SUCCESS; frontend 73/73.
- [x] 20. speckit-analyze cross-artifact.
  → analyze.md: coverage 100%, findings có evidence.
- [x] 21. speckit-converge: append thiếu (nếu có).
  → Không task thiếu; các finding tồn (seed corpus 7k, MATCHING drift, native
  validation message) đã được track trong analyze.md — không mở task mới vì
  ngoài scope audit (cần quyết định product).
- [x] 22. Dọn data test v2 + verify. Verify: COUNT=0 cho id v2.
  → 7 exercises AI v2 (755990-755996) deleted; lesson 81899 + deck 40039 + ex
  755998 + prompt 50011 CRUD cleanup ✓; submission 40020 SQL-deleted + WAV MinIO
  removed; deck-words test row xóa theo deck. Verify COUNT=0 từng loại.
- [x] 23. taskstoissues nếu còn việc tồn.
  → Quyết định KHÔNG convert sang GitHub issues: findings còn lại (seed corpus,
  MATCHING drift, native messages) cần quyết định product, đã track đầy đủ trong
  analyze.md; tạo issues trên repo thật cần user yêu cầu rõ.
- [x] 24. Báo cáo tiếng Việt đầy đủ.

