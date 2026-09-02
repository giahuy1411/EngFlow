# Analyze — Audit V2 (cross-artifact)

Ngày: 2026-09-01 | Feature: audit-v2 | Chạy khi Phase A-E gần xong, trước converge

## Coverage map

| Requirement | Tasks | Trạng thái |
|---|---|---|
| R1.1 map endpoint | 1 | ✓ endpoint-map.md (25 controllers, ~100 mapping) |
| R1.2 happy path mọi endpoint | 3 | ⏳ subagent đang chạy script toàn diện |
| R1.3 negative matrix | 4 | ⏳ nằm trong cùng script subagent |
| R1.4 CRUD lifecycle | 5 | ⏳ nằm trong script subagent (lesson/deck/exercise/vocab/prompt) |
| R2.1 AI 5 loại | 6 | ✓ MC/FB/TL/LI sinh+persist+schema; MATCHING lesson-444 OK (2/4), lesson-445 0/4 (model drift, guard chặn đúng) |
| R2.2 vocab 3 topic | 7 | ✓ cooking/space/interviews, CEFR đúng, shape mảng thuần |
| R2.3 speaking thật | 8 | ✓ WAV TTS → transcript thật → rubric 7.3/10 + feedback VI |
| R2.4 batch Redis | 9 | ✓ 1469 lessons quét, skip đã-có + content-null, 4 lesson trống skip đúng |
| R3.1 orphan toàn FK | 10 | ✓ 17 quan hệ, 0 orphan |
| R3.2 data quality | 11 | ✓ findings: 7,076 empty correctAnswer (seed corpus), 1 ::: MATCHING cũ, 3 vocab dup |
| R3.3 DMV + stats | 12 | ✓ missing-index 0, không tối ưu thiếu bằng chứng (P5) |
| R4.1 mọi route | 13 | ✓ 49 route-lượt chạm, console=0 |
| R4.2 design tokens | 14 | ✓ Candy/Secondary/Sticker/focus ring/bounce curve/reduced-motion |
| R4.3 form validation | 15 | ✓ native HTML5 + alert VI cho login; finding: native message tiếng Anh |
| R4.4 keyboard/focus | 16 | ✓ focus-visible violet 3px |
| R4.5 responsive + LH | 17 | ✓ 375/768 sạch; **BUG-3 tìm + fix** (bảng lesson overflow); LH 96/95/96 a11y; bundle 65kB gzip |
| R4.6 2 MCP | — | ✓ chrome-devtools (LH + console) + playwright (routes + journey) |
| R5 báo cáo | 24 | ⏳ chờ subagent + re-test |

## Findings so v1 (mới)
- **BUG-3 (fixed)**: bảng markdown lesson overflow 375px → CSS block+scroll, verify 360/360.
- **A4-v1 fix hoàn tất**: AiGeneratePanel copy "Review va luu" → "Da sinh va LUU ... vao DB".
- **MATCHING lesson-445 0/4**: model drift vẫn tồn tại theo lesson content; guard đúng; không fix prompt (P5 — chỉ 1 sample, không đủ bằng chứng pattern).
- **Seed corpus 7,076 bài empty answer**: data thật từ khóa giáo khoa, không thuộc pipeline AI; khuyến nghị (backfill hoặc ẩn khỏi grading) — không tự sửa.
- **25 controllers** khớp AGENTS.md (v1 đếm 21 do subpackage).

## Constitution check
P1 baseline: 73/73 green sau fix copy; backend 197 chưa re-run sau CSS/copy (không chạm Java) — sẽ chạy lại Phase F.
P5: không index/prompt-fix thiếu bằng chứng — tuân thủ.
P8: mọi finding có evidence runtime/DB.
