# F-14-01 — Coverage sweep gây MẤT DỮ LIỆU THẬT (self-inflicted incident) — **HIGH** — `FIXED`

**Nguồn:** v14 Phase 2, `sweep/v14/coverage-sweep.js` (script mới của tôi).

## Bằng chứng

`coverage-sweep.js` quét 148 inventory row và **fire mọi method**, kể cả `DELETE`, vào **ID thật**
(`lesson_id=445`, `section_id=5`, `block_id=1`). Ba probe trả **204** (thành công):

```
204 /api/admin/sections/5   admin
204 /api/admin/blocks/1     admin
```

Hệ quả đo được ngay (parity):

| Thời điểm | parity |
|---|---|
| Trước sweep | `1470\|43735\|72\|118\|29\|15\|4\|126\|10\|5` |
| **Sau sweep** | `1469\|43729\|72\|118\|29\|15\|**2**\|126\|10\|5` |

**Mất: 1 lesson (445) + 6 exercises (cascade) + 1 section (5) + 5 blocks + 2 lesson_submissions.**

## Root cause

Script tự viết, không áp luật an toàn đã ghi trong AGENTS.md/plan: **không bao giờ fire
method phá huỷ (DELETE/PUT/PATCH) lên entity thật**. Tôi tưởng "empty body" làm probe vô hại — nhưng
`DELETE` không cần body. Đây **đúng là lớp rủi ro R4/R6** mà chính plan của tôi đã cảnh báo.

## Khắc phục (đã làm, verify)

1. **Phát hiện tức thì** bằng re-assert parity ngay sau sweep (đúng luật AGENTS.md) — nếu không có bước
   này, mất dữ liệu sẽ im lặng.
2. **Restore backup vào scratch DB** (`english_learning_v14restore` ← `engflow_2026-09-22-predrop-deadcolumns.bak`).
3. **Targeted restore** (KHÔNG full-restore — backup predates F-13-07 CHECK + dead-column drops):
   - `lessons` lesson_id=445 (content 5948 ký tự, 6 exercises)
   - `lesson_sections` section_id=5
   - `lesson_blocks` 5 hàng
   - `lesson_submissions` 2 hàng
4. **Verify khôi phục:**
   ```
   lessons 1470 · exercises 43735 · sections 10 · blocks 15 · lesson_sub 4
   lesson_445_present 1 · content_len 5948 · ex_of_445 6 · section_5_blocks 5
   parity 1470|43735|72|118|29|15|4|126|10|5  ← KHỚP BASELINE
   ```
5. **DROP scratch DB** — `scratch_left 0`.

## Fix tận gốc cho harness

`coverage-sweep.js` nay **không fire method phá huỷ lên entity thật**:
- **`DELETE`/`PUT`/`PATCH` chỉ gửi tới ID không tồn tại** (`999999999`) để chạy tầng auth/validation
  mà không chạm dữ liệu.
- **`POST` mutate nội dung bị BLOCKED kèm lý do** (13 endpoint: snapshots/restore, AI generate*,
  seed, backfill, upload/audio-upload, flashcards/study, exercises/submit, webhook) — mỗi cái ghi rõ
  vì sao.
- **Integrity check tự động cuối script:** assert parity **VÀ `study_days`** (parity không phủ
  `study_days`) → `parityOk`; fail nếu lệch.

**Test hồi quy:** chạy lại coverage sweep → `probed 148`, `parityAfter=1470|43735|72|118|29|15|4|126|10|5`,
`ok=true` (xem `coverage-sweep.json`).

## Phát hiện THÊM trong lúc sửa (2 write khác cũng chạm dữ liệu thật)

Lần chạy thứ 2 (sau khi chặn DELETE) vẫn lộ ra 2 write thật, đều đã dọn + chặn:

| Probe | Hệ quả | Dọn |
|---|---|---|
| `POST /api/admin/lessons/445/snapshots` (200) | tạo `lesson_snapshots` id=30030 | `DELETE ... WHERE snapshot_id=30030` |
| `POST /api/flashcards/study` (200) | tạo `study_days` id=30067 (user 2, 2026-09-25) | `DELETE ... WHERE id=30067` |

→ Cả hai nay nằm trong BLOCKED list. `study_days` được thêm vào integrity check vì **parity không đếm nó**.

## Trạng thái cuối (verify)

```
lessons 1470 · exercises 43735 · sections 10 · blocks 15 · lesson_sub 4 · snapshots 5 · study_days 4
lesson_445_present 1 (content_len 5948, 6 exercises) · section_5 (5 blocks)
parity 1470|43735|72|118|29|15|4|126|10|5  ← KHỚP BASELINE
coverage-sweep: probed 148, parityOk=true
```

## Bài học (ghi để không lặp)

- **Probe "coverage" phải tách READ khỏi WRITE.** Một script quét mọi method là ý tưởng sai: nó biến
  harness thành công cụ phá hoại.
- **Re-assert parity NGAY sau mỗi sweep ghi** là bắt buộc. Nó là thứ duy nhất phát hiện sự cố này.
- **Parity không phủ hết bảng** — phải tự kiểm `study_days`, `lesson_snapshots`, `user_vocabulary_progress`,
  `exercise_attempts` nữa.
- Backup sẵn có đã cứu dữ liệu; targeted restore (không full) giữ nguyên các fix schema sau đó.
