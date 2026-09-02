# Tasks — Audit & Optimize EngFlow V1

Dependency-ordered. Mỗi task có verify tiêu chuẩn (Constitution P5/P8: đo được).

## Phase A — Chuẩn bị & đo baseline

- [x] 1. Health check toàn bộ containers + Ollama host trước khi smoke
  - Verify: `docker ps` 8 containers Up; `curl http://localhost:11434/api/tags`
    từ host trả danh sách model (qwen2.5:1.5b, qwen2.5:3b).
- [x] 2. Backend smoke: login user + admin, lấy token, xác nhận shape
  `data.data.token || data.token`
  - Verify: 2 token hợp lệ, dùng được cho các call sau.
- [x] 3. Frontend build check: `npx vite build` không lỗi (bundle hiện tại)
  - Verify: exit 0, dist được tạo.

## Phase B — Smoke test API toàn diện (CRUD + AI)

- [x] 4. Auth & user: GET profile, PUT profile, đổi mật khẩu (roll 2 chiều)
  - Verify: 2xx + dữ liệu phản ánh đúng thay đổi.
- [x] 5. Lessons & structure: GET lessons list, GET lesson detail, GET
  lesson-exercises, snapshot
  - Verify: 2xx, shape đúng từng endpoint (ghi chú `.data` vs plain).
- [x] 6. Exercises & submissions: GET exercises theo lesson, POST submission,
  GET attempts, GET lesson-submissions
  - Verify: submission tạo ra attempt thật trong DB (kiểm tra qua sqlcmd).
- [x] 7. Decks & flashcards & games: GET decks, POST tạo deck, GET
  flashcards, POST game start (Redis), quiz generation
  - Verify: deck mới xuất hiện trong list; game session có progress.
- [x] 8. Vocabulary & SRS & streak: GET vocabulary search, GET/POST SRS
  review, GET streak
  - Verify: 2xx, streak counter phản ánh đúng.
- [x] 9. Video lessons: GET video-lessons list + detail (public path), GET
  admin video lessons (token admin)
  - Verify: 2xx; note audio_url null được fallback speech.js.
- [x] 10. Speaking: GET prompts (list), GET submissions của user, POST
  submission giả lập (audio nhỏ) → Whisper transcribe → rubric AI (3b)
  - Verify: submission có transcript + score sau khi xử lý async (nếu model
    swap quá lâu → note timeout, verify endpoint health).
- [x] 11. AI exercise generation (REAL 1 lần): POST admin ai-exercise → 202
  async → poll Redis progress → hoàn tất
  - Verify: bài tập sinh ra có schema đúng (type hợp lệ, MATCHING dùng `|`).
- [x] 12. AI vocab (REAL 1 lần): POST ai-vocab → kết quả có từ + định nghĩa
  - Verify: 2xx, kết quả có word/meaning.
- [x] 13. Dashboard/leaderboard/progress: GET dashboard, GET leaderboard,
  GET progress
  - Verify: 2xx + shape.
- [x] 14. Payment: GET premium status, GET transactions (SePay poller chạy
  nền — chỉ verify endpoint trả về, không tạo giao dịch thật)
  - Verify: 2xx, không exception trong log backend.

## Phase C — DB audit & tối ưu

- [x] 15. Liệt kê bảng + row count + size qua sqlcmd
  - Verify: bảng đầy đủ 21 entities + bảng phụ; không bảng mồ côi.
- [x] 16. Đọc index hiện có của các bảng nóng (users, lessons, exercises,
  exercise_attempts, decks, deck_words, vocabulary, speaking_submissions,
  lesson_submissions, game_sessions nếu có)
  - Verify: có output index list mỗi bảng.
- [x] 17. Đo thời gian các API chính (p50 qua ~5 lượt gọi) + bắt query chậm
  qua Hibernate SQL logging đã bật (đọc từ backend log nếu có, hoặc bật
  tạm thời qua logback để đo — hoàn tác sau)
  - Verify: bảng số liệu thời gian đã ghi.
- [x] 18. EXPLAIN các query chính (leaderboard, progress, deck words) nếu
  phát hiện chậm — chỉ đề xuất/tạo index khi có full-scan + đủ selectivity
  - Verify: mỗi tối ưu kèm bằng chứng EXPLAIN trước/sau hoặc time trước/sau.
  → Kết luận: p50 33-63ms, không query chậm → KHÔNG tạo index mới (P5).

## Phase D — Frontend design-system verification (MCP)

- [x] 19. Vá tailwind.config.js: gộp fontFamily duplicate + bỏ blob radius
  trùng, bỏ Plus Jakarta Sans khỏi index.html (font không dùng)
  - Verify: `npx vitest run` vẫn 73 pass; grep không còn Plus Jakarta.
- [x] 20. Chrome-devtools MCP: mở Home/Login/Lessons — kiểm console error = 0,
  computed font-family body = Be Vietnam Pro, token màu `--geo-bg` etc.
  - Verify: screenshot + console log sạch.
- [x] 21. Playwright MCP: login → vào Lesson → làm 1 bài exercise → verify
  UI phản hồi đúng (score/badge) — end-to-end user journey
  - Verify: snapshot cho thấy kết quả bài làm.
  → Phát hiện + fix bug grading (user thường luôn "SAI" vì client không có
    đáp án) → chấm qua server /grade. Verify 2 câu = ĐÚNG với đáp án thật.
- [x] 22. Playwright: Decks → tạo deck thử (nếu UI có) hoặc xem deck detail
  → flashcard game render
  - Verify: snapshot game render không lỗi.
  → Quiz game 1/1 đúng, chuyển câu OK. Deck route guard login OK.
- [x] 23. Speaking flow UI: mở SpeakingList → SpeakingDetail (mic permission
  có thể block — fallback verify render + API đã test ở task 10)
  - Verify: render OK, không crash.
  → Verified qua API (403 premium gate đúng shape + admin upload 200 +
    assess 200 → Whisper xử lý thật). Free user redirect /premium?redirect=
    /speaking (route guard).
- [x] 24. Responsive: 375px width Home + Lessons + LessonContent
  - Verify: không overflow ngang, CTA vẫn bấm được.
  → 375px: scrollWidth == clientWidth, tabs vừa, nav collapse đúng.
- [x] 25. A11y: Lighthouse accessibility audit trên Home (chrome-devtools)
  - Verify: score + các issue chính được liệt kê (không bắt buộc fix tất cả).
  → A11y 96 / Best Practices 100 / SEO 92. Đã fix robots.txt (thêm file thật
    vào frontend/public). Color-contrast 4.23:1 trên #8B5CF6 là finding của
    design system (khuyến nghị, không tự đổi token).

## Phase E — Converge & báo cáo

- [x] 26. Chạy lại toàn bộ test backend + frontend sau mọi thay đổi
  - Verify: 196 + 73 xanh.
  → Backend 197/197 (196 baseline + 1 test mới cho GlobalExceptionHandler),
    Frontend 73/73. BUILD SUCCESS.
- [x] 27. `speckit-analyze` cross-artifact consistency (spec/plan/tasks)
  → Report trong báo cáo cuối: không CRITICAL; 1 finding v/v nhiệm vụ ảnh
    (map đúng) — xem chi tiết trong báo cáo.
- [x] 28. `speckit-converge`: soát codebase vs tasks, append thiếu
  → Không task nào thiếu (các thay đổi phát sinh trong audit: GlobalException
    fix, grading fix, robots.txt, dọn data test — đều có verify riêng).
- [ ] 29. Báo cáo tổng hợp (báo cáo audit đầy đủ cho user, tiếng Việt)
