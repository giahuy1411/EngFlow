# F128 — hoàn tất: 9 row đã xử lý hết (2026-09-20 02:0x +07)

**Chủ dự án chọn phương án 3** cho 2 row cuối: **xoá**.

---

## Tổng kết cả 9 row

| Nhóm | Số row | Xử lý |
|---|---|---|
| Cấu trúc MULTIPLE_CHOICE, sửa được | **7** | Đổi `exercise_type` → `MULTIPLE_CHOICE` |
| Không parse được, cần tạo dữ liệu | **2** | **Xoá** (quyết định của chủ dự án) |
| **Còn lại** | **0** | — |

```
LISTENING thiếu audio:  9  →  0
```

---

## Phần 2 row — an toàn được chứng minh TRƯỚC khi xoá

Không xoá dựa trên giả định. `sweep/v10/pre-delete-2-exercises.js` và `check-attempt-details.js` đo:

| Kiểm | Kết quả |
|---|---|
| FK nào trỏ tới `dbo.exercises`? | **Không có** (đọc từ `sys.foreign_key_columns`, không đoán) |
| `exercise_attempts` có cột `exercise_id`? | **Không** — entity chỉ lưu `lesson_id` (`ExerciseAttempt.java:32`) |
| `exercise_attempts.details` (JSON) có nhắc tới 745673 / 745808? | **0 row** — dù 36 row khác **có** chứa `exerciseId` |
| Attempt thuộc lesson 11477 / 11539? | **0** |

Điểm đáng chú ý: `details` **có** chứa `exerciseId` ở 36 row, nên một phép kiểm hời hợt ("bảng không có cột exercise_id → an toàn") sẽ **bỏ sót** khả năng lịch sử làm bài trỏ tới ID sắp xoá. Phải đọc cả JSON mới biết chắc.

## Kết quả

```
exercises:    43737 → 43735   (−2)
LISTENING:      360 → 358     (−2)
FILL_BLANK:    9112 (không đổi)
MATCHING:       332 (không đổi)
MULTIPLE_CHOICE: 33556 (không đổi)
TRANSLATION:    377 (không đổi)

PARITY MỚI: 1471|43735|72|127|28|15|4|126|14|5
                  ↑ chỉ cột exercises đổi
```

**Verify độc lập** (`sweep/v10/verify-2-deleted.js`) — **TẤT CẢ PASS**: 2 row biến mất, số học khớp chính xác ở cả hai chiều, 2 lesson còn 4 exercise, 7 row đã đổi nhãn trước đó vẫn đúng, 0 orphan.

**Verify trên browser** (`sweep/v10/verify-2-lessons-ui.js`):

| | lesson 11477 | lesson 11539 |
|---|---|---|
| Nút "Nghe" | **0** | **0** |
| Chữ "NGHE" | **0** | **0** |
| Nhãn "TRẮC NGHIỆM" | 4 | 4 |
| Thẻ `<audio>` | 0 | 0 |
| Số câu | "Nộp bài (4 câu)" | "Nộp bài (4 câu)" |
| Console errors | **0** | **0** |

---

## Đánh đổi đã chấp nhận

Hai lesson **11477** và **11539** giờ có **4 exercise** thay vì 5, **lệch cấu trúc** so với 1.459 lesson khác. Đây là đánh đổi chủ dự án đã đồng ý, và nó **tốt hơn** giữ lại một bài "nghe" đọc to câu lệnh.

Ghi lại vì: nếu sau này có ai kiểm tra tính đồng nhất của dữ liệu, họ sẽ thấy 2 lesson lệch và cần biết đó là **cố ý**, không phải lỗi nhập liệu.

---

## An toàn đã tuân thủ

- **Backup** `engflow_2026-09-20-pre-delete-2-exercises.bak` + `RESTORE VERIFYONLY` = *"The backup set on file 1 is valid"* — **trước** khi xoá
- **Danh sách ID cụ thể** (`745673, 745808`), không dùng `LIKE`
- `SET QUOTED_IDENTIFIER ON` + `SET XACT_ABORT ON`
- Câu lệnh **idempotent**: `AND exercise_type = 'LISTENING'` — chạy lần hai không khớp row nào
- Verify **độc lập** bằng script riêng, không tin output của chính script DELETE

---

## Trạng thái cuối

| | |
|---|---|
| Backend tests | **470 run / 0 failures / 0 errors / 11 skipped** |
| Frontend tests | **106 passed / 1 skipped** |
| Findings | **14**, trong đó **10 VERIFIED_GREEN** |
| `LISTENING` thiếu audio | **0** |
| Parity | `1471\|43735\|72\|127\|28\|15\|4\|126\|14\|5` |

---

## Việc còn lại của F128 — không còn

Cả 9 row đã xử lý xong. Không còn row nào mang nhãn `LISTENING` mà thiếu audio.

Bộ gán nhãn đã được sửa (`HtmlParserService.buildExerciseFromP`) nên lần import HTML tiếp theo sẽ **tự hạ nhãn** row nào có ≥2 lựa chọn thật mà thiếu audio — có test `HtmlParserListeningDowngradeTest` 14/14 phủ cả hai chiều, gồm 2 test dùng nguyên văn dữ liệu thật.
