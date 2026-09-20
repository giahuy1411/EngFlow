# Phân tích 9 row LISTENING thiếu audio — và các phương án

**Ngày đo:** 2026-09-20 · **Trạng thái:** 9 row **đã được chẩn đoán đầy đủ**, chưa sửa gì.

---

## 1. Kết luận cốt lõi: đây KHÔNG phải vấn đề audio

Vòng audit trước ghi mục này là *"9 exercise LISTENING thiếu `audio_url`"* — một bài toán thiếu dữ liệu. **Sai.** Đo lại thì đây là **lỗi gán nhãn**: 9 row có nhãn `LISTENING` nhưng nội dung là **trắc nghiệm đọc/viết**.

Vì vậy **backfill audio sẽ là sai hướng**, và đây là lý do phải nói rõ trước khi đề xuất bất cứ điều gì.

---

## 2. Bằng chứng — đo, không suy đoán

### 2.1 UI thật hiện gì (chụp màn hình, không suy luận)

`sweep/v10/see-bad-listening-ui3.js` mở lesson 11516 ở tab BÀI TẬP:

```
5  NGHE
I can understand a text about brothers and sisters.
NGHE & TRẢ LỜI (GIỌNG ĐỌC MÁY)
🔊 Nghe
A. My brother is taller than me.  B. My sister is older than me.
C. My brother is older than me.  D. My sister is taller than me.
```

Bốn câu cùng lesson đều hiện nhãn **TRẮC NGHIỆM**. Chỉ câu 5 hiện **NGHE**.

Và `sweep/v10/see-bad-listening-ui.js` mô phỏng đúng logic `speakListening()`:

```
>>> NÚT '🔊 Nghe' SẼ ĐỌC TO: "I can understand a text about brothers and sisters."
```

Đó là một **câu mô tả năng lực đọc** ("Tôi có thể hiểu một đoạn văn về anh chị em"), không phải nội dung bài nghe. Học sinh bấm nút Nghe và nghe thấy chính câu lệnh.

### 2.2 Cấu trúc dữ liệu: 7/9 row là MULTIPLE_CHOICE

`sweep/v10/verify-listening-are-mc.js` phân loại cả 9:

| exercise_id | `options` | `correct_answer` khớp lựa chọn? | Kết luận |
|---|---|---|---|
| 745643 | mảng JSON 4 | ✅ khớp phần tử 0 | **MULTIPLE_CHOICE** |
| 745673 | **không phải mảng** | ❌ | cần xem riêng |
| 745708 | mảng JSON 4 | ✅ khớp phần tử 0 | **MULTIPLE_CHOICE** |
| 745713 | mảng JSON 4 | ✅ khớp phần tử 0 | **MULTIPLE_CHOICE** |
| 745718 | mảng JSON 4 | ✅ khớp phần tử 0 | **MULTIPLE_CHOICE** |
| 745748 | mảng JSON 4 | ✅ khớp phần tử 2 | **MULTIPLE_CHOICE** |
| 745808 | **chuỗi `"A) ...B) ..."`** | ❌ | cần xem riêng |
| 745878 | mảng JSON 4 | ✅ khớp phần tử 0 | **MULTIPLE_CHOICE** |
| 745904 | mảng JSON 4 | ✅ khớp phần tử 0 | **MULTIPLE_CHOICE** |

### 2.3 Phép thử quyết định — và một lần tôi suýt kết luận sai

**Phép thử SAI (tôi đã viết, rồi tự bác bỏ):** gửi chính `correct_answer` làm câu trả lời → `correct=true` cho cả 9. Nhưng đó là **vòng lặp trùng (tautology)**: nó chỉ chứng minh hàm so sánh chuỗi chạy, **không** chứng minh học sinh tạo ra được chuỗi đó.

**Phép thử ĐÚNG:** bắt chước client. `LessonExerciseTab.vue` dòng 71 gọi `selectAnswer(ex.id, opt)` — client gửi **nguyên văn nội dung lựa chọn**. Nên phải mô phỏng `parsedOptions()` rồi gửi đúng chuỗi mà nút bấm sẽ gửi.

`sweep/v10/prove-mc-reclassify-works.js`:

| Kết quả | Số row |
|---|---|
| Bấm nút được **và chấm đúng** | **7** |
| Bấm nút được nhưng sai | 0 |
| **Rơi vào ô nhập tay** (options không parse thành mảng) | **2** |

### 2.4 Tiêu đề lesson xác nhận

| exercise_id | lesson | tiêu đề lesson |
|---|---|---|
| 745643 | 11469 | `Writing - READING` |
| 745673 | 11477 | `Terms and Conditions - READING` |
| 745708 | 11494 | `Writing - SPEAKING` |
| 745713 | 11495 | `Listening Tests - SPEAKING` |
| 745718 | 11497 | `Article level 2 - SPEAKING` |
| 745748 | 11516 | `Reading - VOCABULARY` |
| 745808 | 11539 | `Listening - WORD_SKILLS` |
| 745878 | 11564 | `Reading - WRITING` |
| 745904 | 11569 | `Article level 1 - WRITING` |

**7/9 lesson có tiêu đề Reading / Writing / Article / Terms.** Mỗi lesson có **đúng 5 exercise và đúng 1 LISTENING** — và LISTENING đó **luôn** là row thiếu audio. Mẫu lặp 9/9 lần: không phải ngẫu nhiên, mà là **lỗi hệ thống của bộ gán nhãn**.

### 2.5 Gốc rễ trong code

`HtmlParserService.detectDiviExerciseType()` dòng 142-161:

```java
// 1. Audio present → LISTENING
if (!section.select("audio, .et_pb_audio_module").isEmpty()) {
    return ExerciseType.LISTENING;
}
// ...
// Default: MULTIPLE_CHOICE
return ExerciseType.MULTIPLE_CHOICE;
```

Quy tắc là **"có thẻ `<audio>` thì gán LISTENING"**. Với 9 row này, thẻ `<audio>` có trong HTML nguồn nhưng **file không tải về được** (hoặc bị bỏ), nên `audio_url` rỗng — mà nhãn thì đã gán rồi và không ai sửa lại.

**Đây là lỗi thiết kế bộ gán nhãn:** nó gán nhãn theo *sự hiện diện của một thẻ HTML*, không theo *nội dung câu hỏi*.

---

## 3. Vì sao backfill audio là SAI

Sinh TTS cho 9 row này sẽ **đọc to chính câu lệnh**:

> 🔊 *"Write a description of your new home. Include features, layout, and your thoughts."*
> 🔊 *"Please read these Terms and Conditions carefully before..."*
> 🔊 *"I can understand a text about brothers and sisters."*

Không cái nào là bài luyện nghe. Và hậu quả **tệ hơn để nguyên**:

| | Học sinh thấy | Mức độ |
|---|---|---|
| **Để nguyên** | Nhãn "NGHE" + nút đọc câu lệnh | Lỗi **nhìn thấy được** |
| **Backfill audio** | Nhãn "NGHE" + file audio đọc câu lệnh | Lỗi **trông như đã sửa xong** |

---

## 4. Bốn phương án

### Phương án A — Đổi `exercise_type` sang `MULTIPLE_CHOICE` (7 row)

**Phạm vi:** 7 row có `options` là mảng JSON và `correct_answer` khớp một lựa chọn.

```sql
UPDATE exercises SET exercise_type = 'MULTIPLE_CHOICE'
WHERE exercise_id IN (745643, 745708, 745713, 745718, 745748, 745878, 745904);
```

| | |
|---|---|
| ✅ **Ưu** | Đã **chứng minh** 7/7 chấm đúng khi bấm lựa chọn. UI hiện đúng nhãn "TRẮC NGHIỆM", không còn nút Nghe. **1 câu SQL, không mất dữ liệu.** |
| ⚠️ **Nhược** | `question` vẫn là câu lệnh ("I can understand a text about...") chứ không phải câu hỏi thật. Nhưng **4 câu cùng lesson cũng vậy** — đó là văn phong của bộ dữ liệu này, không phải lỗi mới. |
| **Rủi ro** | Thấp. `MULTIPLE_CHOICE` là nhãn phổ biến nhất (33.549 row). |
| **Cần** | Backup + `SET QUOTED_IDENTIFIER ON` + verify parity |

**Đây là phương án tôi đề xuất làm trước** — lợi ích rõ, rủi ro thấp, có bằng chứng.

### Phương án B — Sửa `options` cho 2 row còn lại, rồi đổi nhãn

**Phạm vi:** 745673 và 745808.

| exercise_id | Vấn đề | Sửa được không |
|---|---|---|
| 745673 | `options` là **chuỗi văn bản** (`"These Terms are for the guidance..."`), không phải mảng JSON. `correct_answer` cũng là một đoạn văn. | Cần **viết lại `options` thành mảng** — tức **tạo dữ liệu mới** |
| 745808 | `options` là chuỗi `"A) ...B) ..."`, `correct_answer = "A"` | Có thể **tách thành mảng 4 phần tử** bằng regex — nhưng regex của tôi tách chỉ được **2/4** (dấu `.` giữa các lựa chọn làm nó nhập nhằng) |

| | |
|---|---|
| ✅ **Ưu** | Cứu được thêm 2 row |
| ⚠️ **Nhược** | Phải **tự tạo/sửa nội dung** — vượt khỏi phạm vi một vòng audit kỹ thuật |
| **Rủi ro** | Trung bình. 745673 đặc biệt khó: `options` không chứa lựa chọn nào cả |

**Đề xuất:** **không** làm tự động. Để riêng cho quyết định nội dung.

### Phương án C — Xoá 9 row

| | |
|---|---|
| ✅ **Ưu** | Sạch sẽ, hết lỗi hiển thị |
| ⚠️ **Nhược** | Mỗi lesson còn **4 exercise** thay vì 5 → **lệch cấu trúc** so với 1.459 lesson khác. Và mất nội dung có thể vẫn dùng được (7 row chấm đúng!) |
| **Rủi ro** | Trung bình — mất dữ liệu, khó hoàn tác về mặt nội dung |

**Đề xuất:** chỉ dùng cho 2 row ở phương án B nếu chúng không cứu được.

### Phương án D — Sửa bộ gán nhãn để không tái diễn

**Phạm vi:** `HtmlParserService.detectDiviExerciseType()`.

Vấn đề: quy tắc "có thẻ `<audio>` → LISTENING" gán nhãn theo **HTML nguồn**, không theo **nội dung**. Nếu file audio không tải được, row vẫn mang nhãn LISTENING vĩnh viễn.

**Đề xuất cụ thể:** thêm một bước hậu kiểm — sau khi parse, row nào có `exercise_type = LISTENING` mà `audio_url` rỗng **và** `options` là mảng ≥2 phần tử thì **hạ nhãn xuống `MULTIPLE_CHOICE`** (vì nó có lựa chọn, tức là trắc nghiệm).

| | |
|---|---|
| ✅ **Ưu** | Chặn tái diễn ở lần import sau |
| ⚠️ **Nhược** | Là thay đổi code sản phẩm, cần test riêng. Và chỉ áp dụng cho lần import **mới**, không sửa 9 row hiện có |
| **Rủi ro** | Thấp nếu chỉ thêm hậu kiểm, không đổi logic chính |

**Đây là việc nên làm sau A**, để 9 row này là lần cuối.

---

## 5. Đề xuất thứ tự thực hiện

| Bước | Việc | Rủi ro | Cần bạn quyết? |
|---|---|---|---|
| **1** | **Phương án A** — đổi nhãn 7 row (đã chứng minh chấm đúng) | Thấp | ✅ Có |
| **2** | **Phương án D** — thêm hậu kiểm vào `HtmlParserService` + test | Thấp | ✅ Có |
| **3** | **Phương án B** — xem xét riêng 745673 và 745808 | Trung bình | ✅ Có (quyết định nội dung) |
| **4** | **Phương án C** — chỉ dùng cho row nào ở bước 3 không cứu được | Trung bình | ✅ Có |

**Không đề xuất backfill audio cho bất kỳ row nào.**

---

## 6. Việc kèm theo bất kể chọn phương án nào

- **Backup mới** + `RESTORE VERIFYONLY` trước DML (quy ước dự án).
- **Danh sách ID cụ thể**, không dùng `LIKE` (bài học F111).
- `SET QUOTED_IDENTIFIER ON` trong batch `UPDATE`.
- Quét `Msg \d+` trong output (`sqlcmd` exit 0 kể cả khi lỗi).
- **Parity trước/sau** — lưu ý `exercises` là 43.737 và **sẽ không đổi** khi chỉ đổi nhãn (đổi nhãn không tạo/xoá row).
- Kiểm lại bằng chính `sweep/v10/prove-mc-reclassify-works.js` và `see-bad-listening-ui3.js` để xác nhận UI đã hiện "TRẮC NGHIỆM".

---

## 7. Script đã tạo để bạn tự chạy lại

| Script | Việc |
|---|---|
| `sweep/v10/classify-listening-9.js` | Liệt kê 9 row kèm nội dung đầy đủ |
| `sweep/v10/verify-listening-are-mc.js` | Phân loại cấu trúc từng row |
| `sweep/v10/inspect-listening-2-hard.js` | Soi 2 row khó (745673, 745808) |
| `sweep/v10/diag-listening-outlier.js` | Đo 9 row là outlier thế nào |
| `sweep/v10/prove-mc-reclassify-works.js` | **Phép thử quyết định** — bắt chước client |
| `sweep/v10/see-bad-listening-ui.js` | Mô phỏng nút "🔊 Nghe" đọc gì |
| `sweep/v10/see-bad-listening-ui3.js` | Mở UI thật, bấm tab BÀI TẬP, chụp ảnh |
