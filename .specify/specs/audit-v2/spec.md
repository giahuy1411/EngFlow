# Spec: Audit Toàn Diện EngFlow V2 (Deep Audit)

**Ngày:** 2026-09-01 | **Trạng thái:** specified | **Cha:** audit-v1 (đã hoàn thành)

## Mục tiêu

Lặp lại audit với **độ phủ tối đa**: mọi endpoint của mọi controller (kèm negative tests),
CRUD đầy đủ từng entity, AI thật theo từng loại, DB deep-audit với DMV, frontend quét
toàn bộ route + a11y sâu. KHÔNG phá baseline (backend 197 / frontend 73).

## Yêu cầu (Requirements)

### R1 — API toàn diện 100% endpoint
- R1.1 Trích xuất toàn bộ mapping (method + path + params) từ 21 controllers bằng grep
  (21 file đã đếm ở v1) — không đoán, không bỏ sót.
- R1.2 Smoke MỌI endpoint: happy path 2xx + shape ghi lại.
- R1.3 Negative tests có hệ thống: 401 (không token), 403 (sai role), 400 (payload thiếu
  trường bắt buộc), 404 (id không tồn tại) — ít nhất 1 case mỗi loại cho mỗi nhóm API.
- R1.4 CRUD vòng đời đầy đủ cho MỌI entity chính: lessons, exercises, decks,
  flashcards, vocabulary, video-lessons, speaking prompts (admin), payments (chỉ đọc).

### R2 — AI sâu theo từng loại
- R2.1 Sinh bài thật cho TỪNG exercise type (MULTIPLE_CHOICE, FILL_BLANK, MATCHING,
  TRANSLATION, LISTENING) — verify schema từng loại (MATCHING dùng `|` + `l=r,...`).
- R2.2 Ai-vocab 2-3 lần (topic khác nhau) — verify word/meaning/level đa dạng.
- R2.3 Speaking full: upload thật (âm thanh không-im lặng để Whisper có transcript
  thật) → assess → rubric 3b → verify scoreTotal 0-10 + feedback tiếng Việt.
- R2.4 Test sự bền vững pipeline: 1 lần sinh batch-nhỏ (2-3 lessons) verify Redis
  progress + persistence.

### R3 — DB deep-audit + tối ưu có bằng chứng
- R3.1 Orphan check TOÀN BỘ cặp FK (KHÔNG chỉ bảng nóng).
- R3.2 Data quality: giá trị rác (chuỗi rỗng, độ dài bất thường, JSON hỏng trong
  options/correct_answer), duplicates logic (title bài học trùng).
- R3.3 DMV missing-index (`sys.dm_db_missing_index_details`) + query stats
  (`sys.dm_exec_query_stats` top thời gian) — chỉ tạo index khi DMV + EXPLAIN đồng ý.
- R3.4 EXPLAIN các query nặng nhất nếu có; so sánh trước/sau nếu tối ưu.

### R4 — Frontend quét toàn bộ route + sâu
- R4.1 Liệt kê TOÀN BỘ route từ router — kiểm mỗi route render + console=0
  (chrome-devtools lặp), login student + admin 2 vai.
- R4.2 Design-system verify từng loại component: Candy Button, Secondary, Sticker
  Card, Input focus, blob radius, hover bounce cubic-bezier, pop-in entrance,
  prefers-reduced-motion có tôn trọng không.
- R4.3 Form validation thật: register (email sai, mật khẩu ngắn), login sai, tạo
  lesson/deck với payload thiếu — UI báo lỗi tiếng Việt đúng.
- R4.4 Keyboard/focus: tab qua nav + form, focus-visible có hard shadow, focus-trap
  modal admin.
- R4.5 Responsive 375px + 768px các trang chính; Lighthouse a11y >= 90 trên Home +
  2 trang chính; hiệu năng bundle (kích thước dist) ghi lại.
- R4.6 Hai MCP đều dùng: chrome-devtools (console/network/Lighthouse) +
  playwright (e2e journey) — mỗi route ít nhất 1 tool chạm.

### R5 — Báo cáo
- Báo cáo v2 tiếng Việt: mọi endpoint (bảng), negative matrix, CRUD matrix, AI từng
  loại, DB DMV findings + tối ưu (nếu có), route matrix + console/a11y, findings mới
  so v1, diff code.

## Clarifications

### Session 2026-09-01 (v2)
- Data test do v2 tạo ra → **tự dọn sạch + verify** sau khi xong (như v1), báo cáo liệt kê.
- Bug mới phát hiện trong v2 → **fix trực tiếp kèm verify** (test + runtime evidence),
  giữ baseline xanh; không hỏi lại từng bug.

## Phạm vi KHÔNG làm
- Không thêm tính năng mới; không seed demo; không đổi design token (trừ khi lỗi).
- Không tối ưu thiếu bằng chứng (P5). Không migration file (P3).
- Không commit (chưa được yêu cầu).

## Acceptance Criteria
1. 100% endpoint có kết quả smoke (bảng đầy đủ, không "..." bỏ sót).
2. Negative matrix 401/403/400/404 có kết quả cho từng nhóm.
3. Backend 197 + frontend 73 xanh sau mọi thay đổi (nếu có).
4. Mọi route frontend được chạm + console=0 (hoặc lỗi được giải thích).
5. Báo cáo tiếng Việt đầy đủ + diff.
