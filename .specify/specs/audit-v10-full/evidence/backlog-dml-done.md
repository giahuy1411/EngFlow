# Phase 3.5/3.6 — Xoá 4 user rác + 4 bảng backup (2026-09-20 00:5x +07)

**Chủ dự án đã quyết định:** xoá 4 user `zzprobe*` **và** xoá 4 bảng `exercises_bak_v5*`.

## Kết quả

| Việc | Trước | Sau |
|---|---|---|
| User `zzprobe*` | 4 (170049, 170050, 170051, 170097) | **0** |
| Bảng `exercises_bak_v5*` | 4 (897 row) | **0** |
| Tổng users | 76 | **72** |
| Orphan thật trên 12 FK | 0 | **0** |

**Parity mới: `1471|43737|72|127|28|15|4|126|14|5`** — chỉ cột `users` đổi (`76 → 72`). Chín cột còn lại **không đổi**, đúng như dự đoán trước khi chạy.

## An toàn: chứng minh trước, không giả định

`sweep/v10/pre-delete-check.js` chạy trước và đo:

- 4 user đều `is_admin=0`, `is_premium=0`
- **0 row phụ thuộc** trên cả **12** FK trỏ tới `users`
- 4 bảng backup: **0 FK** trỏ tới, **0** view/procedure phụ thuộc

Danh sách bảng/cột **không được đoán**. Bản nháp đầu tiên của tôi đoán tên cột và sai ba chỗ:

| Đoán sai | Thực tế |
|---|---|
| `decks.user_id` | `decks.owner_id` |
| thiếu `user_progress` | có, FK tới `users` |
| thiếu `user_streaks` | có, FK tới `users` |

Và `speaking_submissions` / `video_attempts` có **hai** FK mỗi bảng (`user_id` **và** `graded_by`).

Đoán sai sinh ra `Msg 207`, mà **`sqlcmd` vẫn exit 0** — nên nếu không quét `Msg` thì một bảng không hề được kiểm sẽ trông y hệt một bảng rỗng. Danh sách cuối lấy từ `sys.foreign_key_columns` (`sweep/v10/find-user-fks.sql`).

**Backup trước DML:** `engflow_2026-09-20-pre-backlog-dml.bak`, `RESTORE VERIFYONLY` = *"The backup set on file 1 is valid"*.

## Hai lỗi đo lường trong chính quy trình verify

### 1. `deleted_users = 0` — đọc `@@ROWCOUNT` sau `PRINT`

Script in ra `deleted_users = 0`, trông như xoá thất bại. **Không phải.** `PRINT` là một câu lệnh và nó **reset `@@ROWCOUNT` về 0**, nên con số đọc được là số row của chính câu `PRINT`, không phải của `DELETE`.

Parity `76 → 72` là bằng chứng thật rằng đúng 4 user đã bị xoá — và nó đến từ `SELECT COUNT(*)`, không từ `@@ROWCOUNT`.

**Bài học:** đọc `@@ROWCOUNT` phải là câu lệnh **ngay sau** DML, không có gì xen giữa.

### 2. "39 orphan" — query thiếu `IS NOT NULL`

Verify lần đầu báo 39 orphan: `speaking_submissions.graded_by` 17, `video_attempts.graded_by` 12, `decks.owner_id` 10.

**Tất cả đều là NULL, không phải dữ liệu hỏng.** Query tôi viết là:

```sql
NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = x.graded_by)
```

Khi `graded_by IS NULL`, điều kiện này trả về **TRUE** — vì `NULL = bất cứ gì` đều là unknown, nên `EXISTS` là false và `NOT EXISTS` là true. Mọi row **chưa được chấm điểm** bị đếm thành orphan.

Ba cột đó đều `is_nullable = 1` (đã kiểm chứng), và NULL ở đây mang nghĩa hợp lệ: "chưa chấm điểm", "deck hệ thống không có chủ".

Query đúng thêm `x.<col> IS NOT NULL`. Đo lại: **ORPHAN THẬT = 0** trên cả 12 FK, bỏ qua 39 giá trị NULL hợp lệ.

## Quy ước đã tuân thủ

- `SET QUOTED_IDENTIFIER ON` trong batch DELETE
- **Danh sách ID cụ thể** (`170049,170050,170051,170097`) — **không** dùng `email LIKE 'zz%'`. Bài học F111 (v9): chính kiểu filter đó đã xoá nhầm 4 user thật
- Quét `Msg \d+` trong output
- Xoá con trước cha; liệt kê tường minh cả 12 bảng dù đo được 0 row
- Verify độc lập bằng script riêng, không tin output của chính script DML

## Điều KHÔNG làm, và vì sao

- **Không** xoá `correct_answer` rỗng (4.848 row) — quyết định C6: phân loại, không tự sinh đáp án. Grader đã trả `ungradeable=true` và UI đã gắn nhãn.
- **Không** đổi shape lỗi legacy (F123) — hoãn có lý do, cần test cả hai phía.
- **Không** backfill 9 audio LISTENING ở bước này — chờ chứng minh đường TTS→Cloudinary (C7).
