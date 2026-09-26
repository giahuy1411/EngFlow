# Phase 2 — migrate 3 QUESTION blocks → `exercises` (lesson 447)

**Ngày:** 2026-09-25 (+07) · Quyết định người dùng: "Chuyển 3 câu hỏi → exercises, rồi xoá"

## T2.1 Ánh xạ (migration-map)

| block_id | block_type nguồn | questionType | correctAnswer | → `ExerciseType` | options ghi vào |
|---|---|---|---|---|---|
| 4 | QUESTION | MULTIPLE_CHOICE | `goes` | **MULTIPLE_CHOICE** | `["go","goes","going","went"]` (giữ nguyên) |
| 5 | QUESTION | FILL_IN_BLANK | `play` | **FILL_BLANK** | `[]` |
| 7 | QUESTION | TRUE_FALSE | `True` | **MULTIPLE_CHOICE** | `["True","False"]` |
| 9 | SUBMISSION | TEXT | — | **KHÔNG chuyển** | không có tương đương trong `exercises` → xoá |

**Lý do block 7 → MULTIPLE_CHOICE:** `ExerciseType` = {MULTIPLE_CHOICE, FILL_BLANK, LISTENING, MATCHING,
TRANSLATION} — **không có TRUE_FALSE**. Frontend render `MULTIPLE_CHOICE` có options thật thành danh sách
lựa chọn, nên True/False hoạt động đúng (đã verify live).

`question` ← `questionText`; `explanation` ← `explanation`; `difficulty` = `EASY` (mặc định của lesson);
`order_index` = 6,7,8 (nối sau 5 hàng cũ).

## T2.2 Kết quả insert

`inserted exercises: 3` — id mới **787912** (MULTIPLE_CHOICE, ord 6), **787913** (FILL_BLANK, ord 7),
**787914** (MULTIPLE_CHOICE, ord 8). Assertion `count=8` PASS.

## T2.3 Parity (`evidence/migration-verify.txt`)

`1470|`**`43738`**`|5|118|29|4|3|12|10|5` — exercises 43735 → **43738** ✓

## T2.4 Verify UI/API (probe thật, không chỉ SQL)

| Probe | Kết quả |
|---|---|
| `GET /api/lessons/447/exercises` (learner) | **8** item; 3 item mới có `exerciseType` + `options` đúng |
| **`correctAnswer` trả cho learner** | **`None`** cho cả 3 → **không lộ đáp án trước khi trả lời** |
| `POST /grade` đáp án ĐÚNG (goes/play/True) | `score 3/3`, `percentage 100.0`, `ungradeable=false` |
| `POST /grade` đáp án SAI (go/plays/False) | `score 0/3`, `percentage 0.0`, `correct=false` |
| `GET /api/lessons/447/exercises` **anon** | 8 item, **0 hàng rò `correctAnswer`** |

> **So sánh với F-15-01:** đường cũ `/api/lessons/447/structure` trả `correctAnswer` cho **anon**.
> Đường mới `/exercises` ẩn đáp án cho tới khi chấm → **an toàn hơn hẳn**. Đây là lợi ích kèm theo
> của việc gỡ Đường B.
