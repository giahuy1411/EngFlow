# audit-v14-full — Phase 1: DB audit trong Docker (read-only)

**Ngày:** 2026-09-25 (+07) · **Target:** container `engflow-sqlserver` (mssql 2019), db `english_learning`
**Phương pháp:** `python sweep/v8/sqlrun.py <file.sql>` (sqlcmd trong container). Mọi số dưới đây đo trong phiên này.
**Script:** `tmp/v14/t1-constraints.sql`, `tmp/v14/t1-db-audit.sql` · **Output:** `evidence/db-constraints.txt`, `evidence/db-audit.txt`

---

## T1.1 Parity

`1470|43735|72|118|29|15|4|126|10|5` — khớp T0.9 và baseline v13 (xác nhận, 0 delta).

## T1.2 Hiệu lực constraint (scratch DB `english_learning_v14probe`)

**Lỗi probe tự bắt (ghi lại, không phải finding):** lần chạy đầu, `CREATE UNIQUE INDEX ... WHERE tag IS
NOT NULL` thất bại `Msg 1934` vì batch thiếu `SET QUOTED_IDENTIFIER ON` → case filtered báo
"not blocked" **oan**. Đây **đúng là bẫy AGENTS.md**. Đã thêm `SET QUOTED_IDENTIFIER ON` (+ ANSI_* /
ARITHABORT) ở đầu batch; chạy lại cho kết quả thật.

| Họ constraint | Insert vi phạm | Kết quả |
|---|---|---|
| FK | `child.parent_id=999` (không tồn tại) | **BLOCKED msg=547** |
| CHECK | `child.score=999` (ngoài 0–100) | **BLOCKED msg=547** |
| UNIQUE | `parent.name` trùng | **BLOCKED msg=2627** |
| Filtered unique | `child.tag` trùng khi non-NULL | **BLOCKED msg=2601** |
| Filtered unique (âm) | 2 hàng `tag=NULL` | **ALLOWED** (đúng — filter thật, không phải unique thường) |

**Scratch DB:** `SCRATCH_DB_LEFT=0` — đã DROP, assert absent.

## T1.3 Orphan scan (NULL FK đếm riêng)

16 quan hệ FK, **tất cả orphan = 0**, **tất cả null_fk = 0**:

`lesson_sections→lessons`, `lesson_blocks→lesson_sections`, `exercises→lessons`, `deck_words→decks`,
`deck_words→vocabulary`, `exercise_attempts→users`, `study_days→users`, `user_vocabulary_progress→users`,
`user_vocabulary_progress→vocabulary`, `payment_transactions→users`, `speaking_submissions→users`,
`speaking_submissions→speaking_prompts`, `video_attempts→users`, `video_attempts→video_lessons`,
`lesson_submissions→users`, `lesson_snapshots→lessons`.

→ **0 orphan / 16 FK**. NULL FK đếm riêng = 0 (không có NULL lẫn vào).

## T1.4 Re-verify F-13-07 + F-13-08

| Kiểm tra | Kết quả | Kết luận |
|---|---|---|
| `ck_study_policy_singleton` | tồn tại, `is_disabled=0`, `is_not_trusted=0`, `definition=([id]=(1))` | **F-13-07 giữ nguyên — XÁC NHẬN** |
| `study_policy` rows | 1 | đúng singleton |
| `users.current_streak` | **0 cột** (đã xoá) | **F-13-08 fix đã xoá cột — XÁC NHẬN** |
| `users.last_study_date` | **0 cột** (đã xoá) | nt |
| `user_streaks` | 1 hàng (legacy, 0 entity/reader) | như README ghi |
| `study_days` | 4 hàng, 2 user phân biệt | hoạt động thật |

## T1.5 Index usage (`exercises`, uptime hiện tại)

| index | key_cols | seeks | scans | updates |
|---|---|---|---|---|
| idx_exercises_difficulty | 1 | 0 | 4 | 28 |
| idx_exercises_lesson_order | 2 | 28 | 1 | 28 |
| idx_exercises_lesson_type_order | 3 | 0 | 0 | 28 |
| idx_exercises_type | 1 | 0 | 0 | 28 |
| IX_exercises_lesson_type_diff_order | 4 | 0 | 0 | 28 |
| PK__exercise__* | 1 | 1 | 6 | 28 |

**Lưu ý phương pháp (kế thừa bài học F-13-10):** số seeks/scans phụ thuộc **uptime** (usage stats reset khi
restart container). Container mới up ~45m nên vài index có seeks=0 — điều đó **KHÔNG** chứng minh index dư.
Kết luận F-13-10 của v13 (không index nào là prefix của index khác) vẫn đúng về mặt **cấu trúc**; v14
không "tối ưu" dựa trên usage stats non-stationary. **Không finding.**

## T1.6 Timezone (F-13-09)

```
SYSDATETIME() = 2026-09-24 17:12:22 (UTC — SQL Server container)
GETUTCDATE()  = 2026-09-24 17:12:22 (UTC — giống nhau ⇒ container chạy UTC)
host VN       = 2026-09-25 00:12 (+07)  ⇒ lệch 7h
default constraints dùng GETDATE()/SYSDATETIME() = 0
```

→ Đúng như AGENTS.md ghi từ audit-v7. **Không phải lỗi mới**; đường date-granular miễn nhiễm. Giữ nguyên
(không có consumer thứ hai). **F-13-09 XÁC NHẬN (đã biết, chấp nhận).**

## T1.7 Row-level sanity

| Chỉ số | Giá trị |
|---|---|
| `lessons` total / draft | 1470 / **5** |
| `exercises` total | 43735 |
| `MULTIPLE_CHOICE` total / `options IS NULL` | 33556 / **32814 (97.8%)** |
| `LISTENING` total / **thiếu `audio_url`** | 358 / **0** |
| `users` total | 72 |
| `payment_transactions` PENDING không `transaction_id` | **109** |
| `payment_transactions` SUCCESS | 17 |

**LISTENING thiếu audio = 0** — **lệch so v13** (AGENTS.md ghi "9/367 bài LISTENING thiếu `audio_url`").
Đo lại phiên này: **0/358**. Xác nhận bằng probe thứ 2 (bảng dưới) — `missing_audio` chỉ tính trên
LISTENING (các loại khác không dùng `audio_url`):

| exercise_type | n | missing_audio |
|---|---|---|
| MULTIPLE_CHOICE | 33556 | 33555 (không dùng audio) |
| FILL_BLANK | 9112 | 9112 (không dùng audio) |
| TRANSLATION | 377 | 377 (không dùng audio) |
| **LISTENING** | **358** | **0** |
| MATCHING | 332 | 332 (không dùng audio) |

→ Con số "9/367" trong AGENTS.md là **stale** (đã cải thiện). Đây là **doc-drift**, không phải lỗi sản phẩm;
ghi vào `doc-drift.md` (T6.6).

## T1.8 Quét `Msg \d+`

`grep -c "Msg " tmp/v14/t1-db-audit.out` = **0** (batch audit sạch). Batch constraints có 1 `Msg 1934`
ở lần chạy đầu (lỗi probe, đã sửa) — ghi lại, không tính finding.

---

## Ứng viên finding đo được (chưa kết luận — cần probe thứ 2 + quyết định owner)

| # | Quan sát | Số đo | Phân loại |
|---|---|---|---|
| **F-14-C1** | Rác tài khoản test/audit tích tụ nhiều kỳ | **67/72 users** là `*audit*`/`@test.local`/`@test.com`/`@t.com`/`e2e+`/`@engflow.test`; chỉ **5** user thật (`user@`, `admin@`, `free_live_*@example.com`, 2 gmail thật). 4 tài khoản là **admin**. 2 có `exercise_attempts`, 0 có `study_days` | **Nợ dữ liệu** (không phải bug người dùng — leaderboard sạch, xem probe 2) |
| **F-14-C2** | 109 `payment_transactions` PENDING không `transaction_id` | created 2026-07-24 → 2026-09-12 | Nợ dữ liệu — điều tra nguồn ở Phase 2 |

## Probe thứ 2 (R10) cho các ứng viên trên

- **Leaderboard không bị ảnh hưởng:** `GET /api/leaderboard?page=0&size=50` → 50 entry, **0** entry chứa
  `audit`/`test`/`zz` → rác user **không** lộ ra UI. (Đo ở phiên khảo sát + sẽ đo lại Phase 3.)
- **LISTENING audio=0:** kiểm chéo bằng query đếm `audio_url` NOT NULL và mẫu 5 hàng (Phase 2/3).
