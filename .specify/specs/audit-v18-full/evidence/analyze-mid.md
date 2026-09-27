# audit-v18-full — analyze (giữa kỳ, cuối Phase D)

## Chất lượng bằng chứng

**Mạnh:**
- **Phase U: 13 luồng đi bằng CẢ 2 engine** (chrome-devtools MCP + Playwright MCP), đối chiếu 2 engine ↔ nhau
  — đúng ràng buộc Q2. Mọi luồng trọng yếu khớp: auth, guard 2 chiều, lessons/answer-leak, streak, search, AI, CRUD, admin, video, speaking, leaderboard.
- **API↔UI đối chiếu bằng số thật**: admin stats 5/1470/43738/3 khớp DB; leaderboard 5; speaking 7=7; decks 10.
- **Tự bác bỏ 1 giả thuyết SAI của chính mình**: tưởng `study_days` không tăng khi submit là bug — đọc
  `GradeRequest.java` thấy payload key sai (`answer` vs `userAnswer`), gửi lại đúng → +1. Không phải bug app.
- **Phase D**: 28/30 khẳng định demo doc CONFIRMED; **1 REFUTED** (doc §4.3 mô tả trần 6s cũ), 1 PARTLY (§4.1).
- **Prompt flaw audit đo lại** (không chép): 4 lỗ hổng y hệt v17 (AAA/type-scale/Lucide React/font) + "thay font = verify".

**Yếu / giới hạn (ghi rõ, không giấu):**
- Premium flow (U19) chỉ đi bằng CD, không phải PW (bù bằng deep-probe + `g6`).
- AI model nhỏ trả gloss tiếng Trung — hạn chế model, không sửa trong vòng này.
- `smallTargets=115` là false positive (inline link + label checkbox) — đã triage, không phải vi phạm.

## Finding tới giờ

| ID | Mức | Vấn đề | Trạng thái |
|---|---|---|---|
| F-18-01 | LOW (harness) | `danger-tint.js`/`focused-probe.js` hardcode `chromium-1237` (bundle là 1234) → launch fail | **FIXED** (thêm `resolveChromium()`) |
| F-18-02 | LOW (docs) | demo doc §4.1/§4.3 mô tả trần chờ 6s cũ (đã thành mềm 6s/cứng 45s) | **TODO T8.2** |
| F-18-03 | LOW (docs) | demo doc file:line `StudyActivityService` lệch (200→101/106, 122→114) | **TODO T8.2** |

## Đối chiếu spec ↔ plan ↔ tasks ↔ evidence

- O1 API: 145/0 ✅ · O2 UI: 0/0/0/0 ✅ · O2b Phase U: 13 luồng × 2 engine ✅
- O3 DB: 0 orphan, parity khớp ✅ · O4 6 nhóm chức năng ✅ · O5 design: 0 drift, BVP loaded ✅
- O6 perf: đang đo · O7 2 vòng: chưa · O8 docs: chưa · O9 cleanup: chưa
