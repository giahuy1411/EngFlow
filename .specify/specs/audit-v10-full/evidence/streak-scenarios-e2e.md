# Phase 1.13 — 5 kịch bản streak end-to-end (2026-09-20 00:2x +07)

**Ngày chạy trùng với ngày cutover `2026-09-20`.** Đây là lần đầu tiên nhánh `today >= effectiveFrom` của `recordStudy` chạy được thật.

Công cụ: `sweep/v10/streak-scenarios.js` — HTTP thật + đọc lại SQL thật. **PASS=25, FAIL=0.**
Parity sau khi chạy: `1471|43737|76|127|28|15|4|126|14|5` — **khớp baseline**, mọi user rác đã dọn (`verify = 0`).

## Kết quả

| Kịch bản | Điều phải chứng minh | Kết quả |
|---|---|---|
| **KB1** | Đăng nhập **không** phải ngày học | ✅ 3 lần đăng nhập → `study_days` = 0 |
| **KB2** | Hoàn thành hoạt động học **có** ghi ngày | ✅ `POST /api/srs/review` → có row hôm nay; ôn lại **không** nhân đôi |
| **KB3** | Snapshot phản ánh đúng sự thật | ✅ `studiedToday=true`, `currentStreak=1`, `studiedDays` chứa hôm nay |
| **KB4** | Cutover chặn ngày trước nó | ✅ DB có 2 row nhưng `streak` vẫn = 1 |
| **KB5** | User chỉ đăng nhập, không học | ✅ `studiedToday=false`, `currentStreak=0`, `study_days`=0 |

**KB1 chính là hợp đồng của toàn bộ refactor**, và nó đã được chứng minh bằng dữ liệu chứ không bằng đọc code: ba lần đăng nhập liên tiếp để lại `study_days` rỗng.

## Hai FAIL ban đầu — cả hai là probe hiểu sai ngữ nghĩa

### FAIL 1: `legacyHistoryAvailable` — tên field gây nhầm

Probe giả định `false` nghĩa là "không có dữ liệu legacy để hiển thị". **Sai.**

Đọc `StudyActivityService.snapshot()` (dòng 74-92): biến bắt đầu là `true` và **chỉ** hạ xuống `false` khi (a) Redis ném, hoặc (b) có member không parse được thành `LocalDate`. Vậy `true` với user mới hoàn toàn là **đúng** — nó báo *"nguồn legacy đọc được"*, không phải *"nguồn legacy có nội dung"*.

**Bằng chứng ngược lại trong chính test suite:** `StudyActivityServiceTest.redisFailureDoesNotEraseSqlHistoryOrBecomeLegacyAbsence` (dòng 88-95) ép Redis ném và khẳng định `isFalse()`. Nếu `false` mang nghĩa "không có dữ liệu", test đó sẽ không cần ép Redis ném.

Đã sửa assertion thành `=== true` kèm comment giải thích. **Không sửa production code.**

### FAIL 2: KB4 bất khả thi qua HTTP — và điều đó đúng

Ý định ban đầu: chèn `study_days` cho hôm qua → kỳ vọng `streak = 2`. **Không thể xảy ra** khi cutover = hôm nay, vì:

```java
currentStreak(userId) -> days.findDates(userId, effectiveFrom(), today)
                                            ^^^^^^^^^^^^^^^^ sàn chặn
```

Đã **đo** thay vì suy đoán (`sweep/v10/diag-streak-kb4.js`):
```
study_days sau review:        "2026-09-20"
study_days sau insert:        "2026-09-19\n2026-09-20"   ← INSERT THÀNH CÔNG, 2 row
snapshot sau insert:          streak=1  studiedDays=["2026-09-20"]
so ngay trong [effectiveFrom, today]: 1
```
Ngày `2026-09-19` **bị loại có chủ đích** — nó là *lịch sử truy cập*, không phải *ngày học*. Đây chính là ngữ nghĩa cutover.

KB4 được viết lại để kiểm **đúng thứ đo được qua HTTP** (cutover chặn ngày trước nó), kèm một assert rằng DB **thật sự có 2 row** — nếu không có assert đó, một probe hỏng sẽ trông y hệt một hành vi đúng.

**Gap-break được phủ ở tầng unit test:** `fourDayJourneyDoesNotInheritLegacyStreak` (`StudyActivityServiceTest` dòng 64-75) — `stored=[start]`, ở `start+2` trả `streak=0`. Ghi rõ ở đây để vòng sau không tưởng là chưa ai kiểm.

## Ba lỗi tầng probe đã sửa (đáng ghi lại vì đều nguy hiểm)

Lần chạy đầu cho **13 PASS / 11 FAIL** — và **tất cả 11 FAIL đều là lỗi probe**, không một dòng production code nào phải sửa.

| Lỗi | Biểu hiện | Nguyên nhân gốc |
|---|---|---|
| `JSON.stringify(query)` truyền vào `sh -c` | `Msg 102 Incorrect syntax near '\'` | `JSON.stringify` biến newline thành `\` + `n` **literal**; sqlcmd nhận chuỗi một dòng và báo lỗi cú pháp. Mọi query nhiều dòng (tức mọi cleanup) **không bao giờ chạy**, mà sqlcmd vẫn exit 0 |
| Regex `/-?\d+/` trên output sqlcmd | user "id = 207", `vocabId = 207` | Bắt **số đầu tiên trong chuỗi**, kể cả khi đó là **mã lỗi** (`Msg 207`). Hàm trả về giá trị rác và trông như thành công |
| Cột `id` không tồn tại | `Msg 207 Invalid column name 'id'` | PK thật là `user_id` (`User.java:27`) và `vocab_id` (`Vocabulary.java:24`), không phải `id` |

**Điểm chung:** cả ba đều tạo ra *im lặng*, không tạo ra lỗi ồn ào. sqlcmd exit 0, hàm trả về số, script chạy tiếp. Đây đúng là họ lỗi mà AGENTS.md cảnh báo.

**Cách sửa:** `sql()` ghi query ra file tạm → `docker cp` → `sqlcmd -i file`. Không đi qua shell string nào. `sqlNum()` **ném** nếu output chứa `Msg \d+` — một mã lỗi không bao giờ được phép trở thành kết quả.

## Dấu vết của lỗi probe nằm lại trong DB — và đã dọn

Vì `sql()` sai nên cleanup **không chạy**, để lại 2 user (`190696`, `190697`). Đáng chú ý: probe *tưởng* chúng có id `207` — con số đó chỉ là mã lỗi `Msg 207`. **Đây là bằng chứng độc lập cho thấy regex bắt nhầm mã lỗi.**

Đã dọn bằng `sweep/v10/cleanup-stale-audit-users.sql`: SELECT liệt kê ID cụ thể trước, DELETE đúng danh sách đó, `SET QUOTED_IDENTIFIER ON`, quét `Msg`. Kết quả: `TONG users = 76` — **khớp baseline**.

> Không dùng `email LIKE 'zz%'`. Bài học F111 (v9): chính kiểu filter đó đã xoá 4 user thật.

## An toàn dữ liệu

- Backup trước khi chạy: `engflow_2026-09-20-pre-streak-scenarios.bak`, `RESTORE VERIFYONLY` = *"The backup set on file 1 is valid"*.
- User kịch bản có email sinh từ timestamp, **không bao giờ** chạm user thật.
- Cleanup chạy trong `finally` nên chạy cả khi kịch bản đứt giữa đường.
- Mọi DELETE dùng **ID cụ thể**, không dùng pattern.
