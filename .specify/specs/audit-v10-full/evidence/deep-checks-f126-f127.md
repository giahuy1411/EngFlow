# Phase 4.6 — Deep check Bài học/Bài tập: hai lỗi HIGH mới (2026-09-20 01:0x +07)

Hai lỗi này **không nằm trong kế hoạch**. Chúng lộ ra khi thực hiện deep check theo yêu cầu "kiểm tra sát sao toàn bộ chức năng chính, Bài học/Bài tập".

---

## F126 — Nội dung bài NHÁP rò rỉ qua `/api/lessons/{id}/structure`

### Cách tìm ra

Probe `sweep/v10/deep-lessons.js` kiểm 4 đường đọc của bài nháp. Ba đường trả 404 đúng, một đường trả **200**.

### Bằng chứng — và một lần probe đầu không kết luận được

Lần thử đầu dùng bài nháp `10888` và nhận **200 với mảng RỖNG**. Đó **chưa** chứng minh rò rỉ — 200-với-body-rỗng chỉ chứng minh sai mã trạng thái, không chứng minh lộ nội dung. Kết luận từ nó sẽ **thổi phồng mức độ**.

Đã đo lại bằng cách đi tìm bài nháp **có section thật**:

```
phan bo: draft | CO section = 2
         draft | khong section = 4
         published | CO section = 6
         published | khong section = 1459
```

Đúng **2 bài nháp** đang rò rỉ. Thử bài `10889`:

```
STUDENT -> status=200  so section=1  body_len=8013
>>> noi dung: {"id":10006,"title":"Nội dung bài học","orderIndex":10,
     "blocks":[{"id":10013,"blockType":"TEXT","data":"<h3><strong>1. Complete the sentences.
     Use the affirmative form of<em>be.</em></strong></h3>..."}]}
(doi chieu /api/lessons/10889 -> 404)
```

**8.013 byte nội dung bài học thật**, trong khi cùng student đó gọi `/api/lessons/10889` nhận **404**.

### Sửa

Thêm `lessonService.assertLessonVisible(lessonId, isAdmin(authentication))` — đúng pattern đã dùng ở `LessonExerciseController`. Admin vẫn preview được. Đường `/api/admin/lessons/{id}/structure` không đụng tới.

**Test:** `AuditV10DraftLessonStructureGuardTest` — 6 test: draft 404 cho student, draft 404 cho anonymous, draft 200 cho admin, published 200 cho student, published 200 cho anonymous, đường admin vẫn 200. **6/6 PASS.**

---

## F127 — Bài MATCHING không bao giờ chấm được đúng

### Cách tìm ra

Đang kiểm "contract MATCHING" thì thấy 2 định dạng đáp án khác nhau trong dữ liệu. Đọc `MatchingExercise.vue` thì thấy client gửi **chỉ số**, còn dữ liệu lưu **chữ**.

### Bằng chứng — hai phép đo, phép thứ hai mới là phép quyết định

**Phép đo 1 (chưa đủ):** gửi lại **đúng nguyên chuỗi** `correct_answer` → `correct=true`. Điều này chỉ chứng minh logic so khớp chạy, **không** chứng minh UI hoạt động, vì UI không bao giờ gửi chuỗi thô.

**Phép đo 2 (quyết định):** đọc `MatchingExercise.vue` dòng 226 — client gửi `` `${p.left}=${p.right}` `` với `p.left`/`p.right` là **chỉ số hiển thị**. Gửi đúng định dạng đó lên 4 bài published:

```
exercise_id=650575  lesson=11301  -> correct = false
exercise_id=650580  lesson=11302  -> correct = false
exercise_id=650585  lesson=10890  -> correct = false
exercise_id=650590  lesson=445    -> correct = false
```

**4/4 SAI** dù gửi đúng thứ tự. Học sinh nối đúng hết vẫn 0 điểm.

### Phân loại toàn bộ 331 row published

| Loại | Số row |
|---|---|
| `options` mã hoá cặp đúng, khớp `correct_answer` | **205** |
| `correct_answer` còn placeholder `wordN=defN` | 19 |
| `options` không có cặp `left\|right` | 11 |
| Hai nguồn **mâu thuẫn** | 96 |

### Vì sao KHÔNG THỂ sửa bằng cách đổi định dạng chỉ số

`MatchingExercise.vue` **xáo trộn cột phải** trong `onMounted`:

```js
const indices = [...Array(rightTexts.length).keys()]
for (let i = indices.length - 1; i > 0; i--) { /* Fisher-Yates */ }
rightItems.value = indices.map(i => rightTexts[i])
```

Nên **chỉ số hiển thị ≠ chỉ số gốc**, và server không có cách nào dựng lại phép hoán vị. Đây là lý do **bản sửa đầu tiên của tôi sai** — xem mục dưới.

### Sửa (3 phần phối hợp)

1. **Backend:** MATCHING chấm bằng so khớp **tập hợp cặp chữ** với cặp lấy từ `options` (mỗi phần tử `"left|right"`), bỏ qua thứ tự, không phân biệt hoa/thường và khoảng trắng. `correct_answer` **không còn dùng** để chấm MATCHING.
2. **Backend:** bài MATCHING có `options` không sinh ra cặp nào được xếp **ungradeable** — loại khỏi cả tử số lẫn mẫu số, đúng chính sách với `correct_answer` rỗng — thay vì âm thầm tính là sai (11 row đo được).
3. **Frontend:** `MatchingExercise.vue` gửi **CHỮ** của hai nhãn thay vì chỉ số; **không parse `correctAnswer` nữa** (đồng thời xoá một đường rò rỉ đáp án ra client và xoá đoạn "fallback tuần tự" vốn **đoán** đáp án); nộp bài khi `matchedPairs.length === leftItems.length` thay vì so với con số client tự đoán. Xoá luôn computed chết `userPairs`.

**Test:** `ExerciseServiceMatchingGradingTest` — 12 test. **12/12 PASS.**

---

## Bản sửa đầu tiên của tôi cho F127 đã SAI — ghi lại

Tôi sửa lần đầu bằng cách **so khớp cặp theo chỉ số ở server**, chấp nhận cả hai định dạng. Bản đó **không thể chạy đúng**: client xáo trộn cột phải nên chỉ số nó gửi vô nghĩa với server.

Tôi phát hiện ra vì **đọc lại `onMounted` của chính component đó** thay vì tin vào bản vá của mình, rồi **đo phân bố định dạng** trước khi chọn cách sửa thật.

Ghi lại vì bản sai đó **trông rất hợp lý** và sẽ ship một thay đổi **không có tác dụng gì** — tệ hơn không sửa, vì nó khiến lỗi trông như đã được xử lý.

---

## Số liệu sau hai bản sửa

| | Trước | Sau |
|---|---|---|
| Backend tests | 438 | **456** |
| Failures / Errors | 0 / 0 | **0 / 0** |
| Frontend tests | 106 | **106** (không đổi) |
| Frontend build | 177.31 kB | **177.31 kB** (không đổi) |
| Findings VERIFIED_GREEN | 7 | **9** |

Parity không đổi: `1471|43737|72|127|28|15|4|126|14|5` (không thao tác nào ghi row nghiệp vụ).

## Bài học phương pháp

1. **Probe phải bắt chước client, không phải bắt chước tài liệu.** Phép đo đầu của F127 gửi chuỗi thô và "pass" — trong khi UI thật gửi chỉ số và luôn sai. Một probe không giống client sẽ xác nhận một hệ thống đang hỏng.
2. **200-với-body-rỗng không phải rò rỉ.** Phải tìm bản ghi thật sự có nội dung mới kết luận được mức độ.
3. **Khi bản vá của mình "trông hợp lý", hãy đọc lại chỗ nó phụ thuộc.** Bản sai của F127 sống sót đúng một bước vì tôi chưa đọc `onMounted`.
