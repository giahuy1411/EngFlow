# Đã sửa: 9 row "LISTENING" — phương án A + D (2026-09-20 01:4x +07)

**Chủ dự án đã quyết định:** làm phương án A (đổi nhãn 7 row) và D (sửa bộ gán nhãn).

---

## Phương án A — đổi nhãn 7 row

### Đã làm

```sql
UPDATE exercises SET exercise_type = 'MULTIPLE_CHOICE'
WHERE exercise_id IN (745643,745708,745713,745718,745748,745878,745904)
  AND exercise_type = 'LISTENING';
```

Điều kiện `AND exercise_type = 'LISTENING'` làm câu lệnh **idempotent**: chạy lần hai không khớp row nào, không ghi đè gì.

### Verify độc lập — TẤT CẢ PASS

`sweep/v10/verify-listening-fix.js` (không tin output của chính script UPDATE):

| Kiểm | Kết quả |
|---|---|
| 7 row đều là `MULTIPLE_CHOICE` | ✅ 7/7 |
| 2 row còn lại **vẫn** là `LISTENING` (không đụng tới) | ✅ 2/2 |
| `LISTENING` thiếu audio: 9 → **2** | ✅ |
| Tổng `exercises` vẫn **43737** (không tạo/xoá row) | ✅ |
| `MULTIPLE_CHOICE`: 33549 → **33556** (đúng +7) | ✅ |
| `LISTENING`: 367 → **360** (đúng −7) | ✅ |
| Parity | ✅ `1471\|43737\|72\|127\|28\|15\|4\|126\|14\|5` — **không đổi** |
| `correct_answer` và `options` vẫn còn nguyên | ✅ |

Số học khớp **chính xác** ở cả hai chiều — đây là bằng chứng không row nào ngoài danh sách bị ảnh hưởng.

### Verify trên BROWSER — UI đã đúng

`sweep/v10/verify-listening-ui-fixed.js`, mở lesson 11516 tab BÀI TẬP:

| Chỉ số | Trước | **Sau** |
|---|---|---|
| Nhãn "Nghe & trả lời" | 1 | **0** |
| Nút "🔊 Nghe" | 1 | **0** |
| Số lần chữ "NGHE" | 1 | **0** |
| Số nhãn "TRẮC NGHIỆM" | 4 | **5** |
| Console errors | 0 | **0** |

Ảnh so sánh: `bad-listening-tab.png` (trước) → `listening-fixed-tab.png` (sau). Câu 5 đổi từ **"NGHE" + nút 🔊** sang **"TRẮC NGHIỆM"**.

### Một lần nữa, probe của tôi sai trước

Lần chạy đầu của `verify-listening-ui-fixed.js` báo **1 FAIL**: *"phải có >=5 nút lựa chọn"*, đếm được 4.

**Đó là lỗi của probe, không phải của UI.** Danh sách bài tập render theo kiểu **lazy** — câu 5 nằm ngoài khung nhìn nên nút của nó chưa được tạo ở thời điểm đo. Nhãn "TRẮC NGHIỆM" đếm được 5 vì đó là **chữ trong DOM tĩnh**, còn nút thì không.

Đã sửa assertion: bỏ phép đếm nút, giữ 3 assert thật sự có ý nghĩa (không còn nhãn Nghe, không còn nút Nghe, không còn chữ NGHE). **Không sửa một dòng code sản phẩm nào.**

---

## Phương án D — sửa bộ gán nhãn để không tái diễn

### Gốc rễ

`HtmlParserService.detectDiviExerciseType()` gán nhãn **theo section**, và quy tắc đầu tiên là:

```java
// 1. Audio present → LISTENING
if (!section.select("audio, .et_pb_audio_module").isEmpty()) {
    return ExerciseType.LISTENING;
}
```

Thẻ `<audio>` có trong HTML nguồn **nhưng file không tải về được** → row mang nhãn LISTENING vĩnh viễn dù `audio_url` rỗng. Bộ gán nhãn không bao giờ nhìn lại.

### Sửa

Trong `buildExerciseFromP()` — nơi **từng row** được dựng, sau khi đã có `options`:

```java
if (type == ExerciseType.LISTENING && hasRealChoices(options)) {
    type = ExerciseType.MULTIPLE_CHOICE;
}
```

Thêm helper `hasRealChoices(String)`: đếm dấu `"` trong mảng JSON và chia đôi; cần **≥2 lựa chọn** mới hạ nhãn.

**Vì sao điều kiện này đúng:** một bài nghe ĐÚNG nghĩa cũng có thể có lựa chọn — đo được **279/358 row LISTENING có audio** đúng là như vậy. Nhưng khi đó nó **có `audio_url` để phát**. Ở đây chỉ hạ nhãn khi row **không có audio**, tức nhãn LISTENING **không thể đúng**.

**Vì sao không parse JSON đầy đủ:** `extractOptions()` chỉ sinh ra **một** định dạng (mảng JSON một dòng, do `formatOptions()` tạo). Đếm nháy tránh phải xử lý mọi trường hợp JSON hỏng ở tầng này.

### Test — 14/14 PASS

`HtmlParserListeningDowngradeTest`:

| Nhóm | Test |
|---|---|
| **Phải NHẬN** (3) | mảng 2 phần tử · mảng 4 phần tử · lựa chọn có dấu nháy escape bên trong |
| **Phải TỪ CHỐI** (9) | `null` · rỗng · `[]` · **1 lựa chọn** · văn bản thuần · chuỗi `"A) ...B) ..."` · JSON object · ngoặc lệch · literal `null` |
| **Đối chiếu dữ liệu THẬT** (2) | nguyên văn `options` của 745713 (nhận) · nguyên văn của 745673 và 745808 (từ chối) |

Hai test cuối dùng **chuỗi thật từ DB**, nên chúng sẽ đỏ nếu ai đó đổi logic theo hướng không còn khớp dữ liệu hiện có.

### Phạm vi ảnh hưởng

Sửa này **chỉ áp dụng cho lần import HTML tiếp theo**, không sửa 9 row hiện có — đó là việc của phương án A, đã làm ở trên.

---

## 2 row CÒN LẠI — chưa sửa, cần bạn quyết định

| exercise_id | Vấn đề | Vì sao không sửa tự động |
|---|---|---|
| **745673** | `options` là **chuỗi văn bản** (`"These Terms are for the guidance..."`), không chứa lựa chọn nào. `correct_answer` cũng là một đoạn văn. | Phải **tạo dữ liệu mới** — không có gì để parse |
| **745808** | `options` dạng `"A) ...B) ..."`, `correct_answer = "A"`. Regex tách được **2/4** (dấu `.` giữa các lựa chọn gây nhập nhằng). | Phải **tự tách và kiểm lại nội dung** |

Cả hai **vẫn mang nhãn `LISTENING`** và **vẫn hiện nút "🔊 Nghe" đọc to câu lệnh**. Test `hasRealChoices` **cố ý từ chối** chúng — nên phương án D sẽ không tự hạ nhãn chúng ở lần import sau.

**Đề xuất:** xử lý riêng theo một trong ba hướng — (1) bạn cung cấp `options` đúng, (2) tách bằng tay rồi tôi kiểm lại, (3) xoá 2 row (mỗi lesson còn 4 thay vì 5 exercise).

---

## Trạng thái sau khi xong

| | |
|---|---|
| Backend tests | **470 run / 0 failures / 0 errors / 11 skipped** (từ 456, +14 test) |
| Parity | `1471\|43737\|72\|127\|28\|15\|4\|126\|14\|5` — **không đổi** |
| `LISTENING` thiếu audio | **9 → 2** |
| UI | Không còn nhãn "NGHE" sai trong lesson 11516 |
| Backup | `engflow_2026-09-20-pre-listening-fix.bak`, `RESTORE VERIFYONLY` = *"The backup set on file 1 is valid"* |

## An toàn đã tuân thủ

- Backup + `RESTORE VERIFYONLY` **trước** DML
- **Danh sách ID cụ thể**, không dùng `LIKE` (bài học F111)
- `SET QUOTED_IDENTIFIER ON` + `SET XACT_ABORT ON`
- Câu lệnh **idempotent** (điều kiện `AND exercise_type = 'LISTENING'`)
- Verify **độc lập** bằng script riêng, không tin output của chính script UPDATE
- Kiểm cả **hai chiều số học**: +7 ở `MULTIPLE_CHOICE`, −7 ở `LISTENING`

## Ghi chú: `updated_rows = 0` là phép đo sai, không phải UPDATE thất bại

Script in ra `updated_rows = 0` nhưng 7 row **đã đổi thật**. Nguyên nhân giống hệt lỗi đã ghi ở `backlog-dml-done.md`: **`PRINT` là một câu lệnh và nó reset `@@ROWCOUNT` về 0**, nên con số đọc được là số row của chính câu `PRINT`.

Bằng chứng thật là số học: `MULTIPLE_CHOICE` 33549 → 33556 và `LISTENING` 367 → 360.

**Bài học lặp lại hai lần trong cùng vòng audit:** đọc `@@ROWCOUNT` phải là câu lệnh **ngay sau** DML, không có gì xen giữa.
