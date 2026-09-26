# Phase 1 — cleanup C1 + C2

**Ngày:** 2026-09-25 (+07) · **Người dùng:** 2026-09-25 (đã chốt "Xoá 67 user rác + row con")

## T1.1 Backup + restore-drill (V6)

| Bước | Kết quả |
|---|---|
| `BACKUP DATABASE … WITH INIT, COMPRESSION` | `Processed 24898 pages in 0.502 s (387 MB/s)` |
| `RESTORE VERIFYONLY` | **`The backup set on file 1 is valid.`** |
| `docker cp` → host | `C:\Users\ASUS\engflow-backups\engflow_2026-09-25-prec1c2.bak` (37,040,128 bytes) |
| **Restore-drill** → `english_learning_v15drill` | `RESTORE DATABASE successfully processed 24898 pages` |
| Drill parity | `1470\|43735\|72\|126\|15\|10\|5` = **khớp live** |
| Drop drill DB | `DB_ID(...) = NULL` |

## T1.2–T1.3 ID sets (liệt kê, KHÔNG dùng LIKE)

- **67 stale user_id** (đã đếm lại): 4,5,6,7,10006,20006,30006,40006,40007,40008,40009,40010,40011,50007,
  50008,50009,50010,60009,60010,80009,80010,80011,80012,80013,80014,90009,90010,90011,90012,100009,
  100010,100011,110009,110010,110011,140010,140011,150010,150011,150012,150013,150014,150015,150016,
  150017,150018,150019,150020,150021,150022,150023,150024,150025,150026,150027,150028,150029,150030,
  150031,150033,150034,150035,150036,150037,150038,150039,160040
- **5 giữ:** 2 `user@gmail.com`, 3 `admin@gmail.com`, 70009 `free_live_*`, 120010 `giahuy5461@gmail.com`,
  150040 `giahuy8906@gmail.com`
- **114 payment id xoá** = `status='PENDING'` (109) ∪ owner rác `60009/110010/110011` (5 SUCCESS).
  **12 giữ:** `40021,40048,40049,50054,50057,50061,50063,50064,50065,50066,50071,60110` (đều SUCCESS, chủ thật).

## T1.4 Dry-run (ROLLBACK)

`PRECHECK OK: stale_users=67 del_payments=114 keep=12`; xoá đúng 114 payments + 42 child rows + 67 users;
sau `ROLLBACK` DB vẫn `72|126` → **không có thay đổi ngoài ý muốn**.

## T1.5 Xoá thật (COMMITTED)

`QUOTED_IDENTIFIER ON` + `XACT_ABORT ON` + `BEGIN TRAN` + **assertion trước/sau** (abort nếu ≠ 67/114/12).
Kết quả (`evidence/cleanup-c1c2.log`): **0 `Msg \d+`**.

| Bảng | Xoá |
|---|---|
| payment_transactions | 114 |
| exercise_attempts | 1 |
| video_attempts | 11 |
| lesson_submissions | 1 |
| user_progress | 2 |
| user_vocabulary_progress | 4 |
| user_streaks / study_days / decks / speaking_submissions | 0 |
| **users** | **67** |

## T1.6 Parity sau cleanup (`evidence/parity-after-cleanup.txt`)

**`1470|43735|5|118|29|4|3|12|10|5`** — **khớp kế hoạch**.

## T1.7 Probe 2 (độc lập)

| Probe | Kết quả |
|---|---|
| 5 user thật còn nguyên (`SELECT`) | ✅ `2,3,70009,120010,150040` |
| Payments: 12, **toàn SUCCESS**, 0 orphan | ✅ `SUCCESS\|12`; `orphan_payments = 0` |
| `POST /api/auth/login` `admin@gmail.com`/`123456` | **200** |
| `POST /api/auth/login` `user@gmail.com`/`123456` | **200** |
| `GET /api/leaderboard` (anon) | **200** — trả rank 1 = user 2, rank 2 = user 3 (user thật) |

> Ghi chú: mật khẩu seed là `123456` (AGENTS.md §98), KHÔNG phải `password123` (giá trị fallback trong
> `DatabaseSeeder`). Lần thử đầu với `password123` → 401; với `123456` → 200. Không phải lỗi sản phẩm.
