# Phase H0 — baseline & mốc `study_days`

**Ngày:** 2026-09-25 (+07) · Nhánh `audit-v15-full`

## H0.1 Baseline

| Đo | Giá trị |
|---|---|
| Backend suite | **512 / 0 fail / 0 error / 11 skipped** |
| Frontend suite | 178 passed / 1 skipped (29 file) — đo phiên v15 |
| Parity (9 bảng) | `1470\|43738\|5\|118\|29\|4\|3\|12\|10` |
| `study_days` **quan sát** | **5** (nghi residue — xử ở H0.2/H0.3) |

## H0.2 Truy vết residue `study_days`

`study_days` hiện có **5** hàng:

| id | user_id | study_date | nguồn |
|---|---|---|---|
| 10026 | 3 | 2026-09-21 | thật |
| 10031 | 2 | 2026-09-21 | thật |
| 10053 | 2 | 2026-09-22 | thật |
| 20066 | 3 | 2026-09-22 | thật |
| **30069** | **2** | **2026-09-25** | **residue (hôm nay, tài khoản probe)** |

**Truy vết (đo, không suy đoán):**
- v14 `e2e-flows.md` ghi kết thúc v14 `study_days = 4`; v14 cũng tạo **30067** rồi xoá (incident doc).
- Hàng **30069** đã có trong backup `engflow_2026-09-25-prec1c2.bak` (chụp 02:46 đầu phiên v15) → nó được
  ghi **trước** Phase 1 của v15.
- **Cơ chế:** `StudyActivityService.recordStudy()` được gọi từ **5 service**: `FlashcardService:69`,
  `SrsService:116`, `ExerciseService:520` (trong **`submitExercises`**, không phải `/grade`),
  `SpeakingSubmissionService:134`, `StreakService:22`. Mọi endpoint tương ứng ghi **1 hàng
  `study_days`/user/ngày**.
- **Vì sao vô hình:** `p16-parity.sql` **không** đếm `study_days`; `grep -rn study_days sweep/` = **0 hit**.
- **Vì sao chỉ 1 hàng dù chạy nhiều lần:** `recordStudy` idempotent —
  `if (!days.existsByUserIdAndStudyDate(...)) save(...)`. Nên residue **capped 1 hàng/user/ngày** →
  dễ bị nhầm là "baseline" (đúng như v15 đã ghi "study_days 5" trong `round-2.md`).
- **Kiểm chứng phủ định:** chạy full backend suite (512 test) → `study_days` vẫn **5** (không phải test
  ghi). Chạy `sweep/v12/api-sweep.js` (gọi `/flashcards/study` + `/srs/review`) → vẫn **5** (đã có hàng
  hôm nay của user 2 ⇒ idempotent).

## H0.3 Xoá residue

Xoá đúng **1 hàng id=30069** (`user_id=2`, `2026-09-25`), `QUOTED_IDENTIFIER ON`, `BEGIN TRAN`,
`WHERE id = 30069` (ID liệt kê, KHÔNG `LIKE`), kèm assertion `@@ROWCOUNT = 1` và
`COUNT(study_days) = 4` sau đó. Backup đã có (H0.1 của v15).
