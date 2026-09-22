# F-13-02 — Giải thích chi tiết + phương án (kèm ví dụ THẬT)

## 1. Chuyện gì đang xảy ra (nói bằng ví dụ thật, không trừu tượng)

EngFlow có **HAI đường tạo bài học/bài tập cho admin**, và chúng **không nối vào nhau**:

```
ĐƯỜNG A — "Quản lý bài tập" (AdminExercises.vue → bảng `exercises`)
   Admin tạo câu hỏi  →  lưu bảng `exercises`  →  HỌC VIÊN THẤY (tab "Bài tập")  ✅

ĐƯỜNG B — "Lesson Builder" (AdminLessonBuilder.vue → bảng `lesson_sections` + `lesson_blocks`)
   Admin dựng bài    →  lưu `lesson_blocks`   →  HỌC VIÊN KHÔNG THẤY GÌ            ❌
```

**Ví dụ thật, lesson 447** ("English Grammar Exercises for A1 – Present simple"). Admin đã dựng trong Builder:

| block_id | loại | nội dung admin đã gõ |
|---|---|---|
| 3 | TABLE | Bảng chia động từ: `I work`, `He/She/It works`… |
| 4 | QUESTION | `She ___ to school every day.` (trắc nghiệm: go/goes/going/went, đáp án `goes`) |
| 5 | QUESTION | `They ___ (play) football on Sundays.` (điền từ, đáp án `play`) |
| 7 | QUESTION | `The writer wakes up at 6 AM.` (đúng/sai, đáp án `True`) |
| 9 | SUBMISSION | `Nhap bai viet cua ban o day:` (bài viết) |

**Đo live trang học viên `/lessons/447`** (Playwright, DOM thật):

```
hasTableBlock        = false   ← bảng chia động từ KHÔNG hiện
hasTrueFalseQuestion = false   ← câu đúng/sai KHÔNG hiện
hasMCQuestion        = false   ← câu trắc nghiệm KHÔNG hiện
hasSubmissionPrompt  = false   ← ô nhập bài viết KHÔNG hiện
```

Học viên chỉ thấy phần **HTML scraped** (`lesson.content`, lấy từ english-practice.net). Toàn bộ công admin gõ trong Builder **bị bỏ im lặng**.

**Quy mô:** 7 lesson có blocks, **6 trong đó đã publish** (446, 447, 567, 11301, 41881, 91900) — tức là đang ở trạng thái học viên truy cập được.

### Tệ hơn: admin không có cách nào tự phát hiện

Builder có nút **"Xem trước"** (`AdminLessonBuilder.vue:29`):

```js
<AppButton as="a" :href="'/lessons/' + lessonId + '/preview'" target="_blank" ...>Xem trước</AppButton>
```

Nhưng route `/lessons/:id/preview` **không tồn tại**. Router chỉ có `/lessons` và `/lessons/:id` (`router/index.js:35,41`), cộng catch-all `/:pathMatch(.*)* → '/'` (`:192`).

**Đo live:** `GET /lessons/446/preview` → URL cuối cùng là **`http://localhost:5173/`** (trang chủ).

→ Admin bấm "Xem trước", thấy **trang chủ**, và rất dễ tưởng "chắc lỗi tạm thời" rồi bỏ qua. Không có tín hiệu nào nói "nội dung bạn vừa soạn chưa tới học viên".

### Vì sao lại như vậy (root cause)

- `lesson_blocks.data` là **chuỗi JSON thô**, không có schema, không có đường chuyển thành `exercises`.
- `LessonStructureService` chỉ **CRUD** blocks + snapshot. **Không** có hàm nào sinh `lesson.content` từ blocks, và **không** có grading cho câu hỏi trong block (`grep QUESTION` ở service = 0).
- `LessonService.java:316` có đọc sections/blocks, nhưng **chỉ để cascade delete** khi xoá lesson — không phải đường đọc cho học viên.
- `getStructure()` (public, `GET /api/lessons/{id}/structure`) **không được gọi ở đâu trong frontend** (`grep` = 0).

---

## 2. Bốn phương án — chi phí, rủi ro, kết quả

### Phương án A — Nối Builder vào học viên (đầy đủ)

Làm cho học viên **thật sự thấy** blocks.

| Việc | File | Ghi chú |
|---|---|---|
| Sửa nút "Xem trước" | `AdminLessonBuilder.vue:29` | `/lessons/{id}` (route có sẵn) |
| Component render blocks | **mới** `frontend/src/components/lessons/LessonBlocks.vue` | Render TEXT (HTML đã sanitize), IMAGE, AUDIO, TABLE |
| Gọi nó trong trang học | `LessonLayout.vue` hoặc `LessonContent.vue` | Gọi `lessonStructureService.getStructure(id)` (hàm **đã có sẵn**) |
| Render QUESTION | tái dùng pattern `LessonExerciseTab.vue` | 3 loại: MULTIPLE_CHOICE, FILL_IN_BLANK, TRUE_FALSE |
| **Grading cho QUESTION** | **backend mới** | Block questions **chưa có** endpoint chấm nào — phải viết mới (service + endpoint + test) |
| SUBMISSION | **backend mới** | Cần endpoint lưu bài viết |

- **Ưu:** đúng nguyện vọng sản phẩm ("tạo bài tập"); 7 lesson có sẵn sẽ "sống".
- **Nhược:** **lớn** — grading + submission là tính năng mới, cần thiết kế (chấm ở đâu, lưu ở đâu, có tính điểm/streak không?). Rủi ro cao nhất vì chạm nhiều tầng và tạo API mới.
- **Thời lượng:** nhiều phase, không phải "fix".

### Phương án B — Sửa cái bẫy + ghi rõ giới hạn ⭐ **ĐỀ XUẤT**

Không xây tính năng mới. **Đóng đúng cái hố nguy hiểm nhất**: admin tưởng đã publish.

| Việc | File |
|---|---|
| Sửa "Xem trước" → `/lessons/{id}` (route thật) | `AdminLessonBuilder.vue:29` |
| Banner cảnh báo trong Builder: khối TEXT/IMAGE/AUDIO/TABLE hiển thị cho học viên sau khi nối; **khối QUESTION/SUBMISSION hiện chưa được chấm/hiển thị** — hãy dùng "Quản lý bài tập" cho câu hỏi | `AdminLessonBuilder.vue` |
| (tùy chọn) Ẩn/disable lựa chọn QUESTION + SUBMISSION trong dropdown cho tới khi nối xong | `AdminLessonBuilder.vue:383` `blockTypeOptions` |
| Ghi rõ trạng thái vào `docs/` | `docs/` (mới 1 mục) |

- **Ưu:** rẻ; đóng ngay rủi ro "admin tưởng xong"; không tạo API mới; không mất tính năng; **đúng nguyên tắc "không tự ý xoá module"** (AGENTS.md).
- **Nhược:** nội dung Builder vẫn chưa tới học viên — nhưng **đã được nói rõ**, không còn im lặng.
- **Lưu ý:** phương án này **có thể là bước 1 của A** — nếu sau này muốn làm A, cứ thêm renderer.

### Phương án C — Bỏ Builder hoàn toàn

Gỡ khỏi router + menu + nút; xoá backend + test.

| Phạm vi | Chi tiết |
|---|---|
| Frontend | `router/index.js:187`, `AdminLayout.vue:152`, `AdminLessons.vue:85`, xoá `AdminLessonBuilder.vue` |
| Backend | 8 file: `LessonStructureController`, `LessonStructureService`, `LessonBlock/Section`, 2 repository, `LessonSnapshotService`, phần cascade trong `LessonService` |
| Test | 4 file |
| DB | `lesson_sections` (10), `lesson_blocks` (15) thành bảng mồ côi |

- **Ưu:** schema/code sạch, không còn đường gây hiểu nhầm.
- **Nhược:** **mất tính năng** admin đã dùng thật (7 lesson có nội dung); cần quyết định xử lý dữ liệu blocks; **rủi ro cao** — theo AGENTS.md phải grep kỹ trước khi xoá module.

### Phương án D — Chỉ ghi tài liệu

- **Ưu:** 0 rủi ro code.
- **Nhược:** **không giải quyết gì** — admin vẫn bấm "Xem trước" → trang chủ, vẫn tưởng đã publish. Đây là phương án yếu nhất.

---

## 3. So sánh nhanh

| | A (đầy đủ) | **B (đề xuất)** | C (bỏ) | D (docs) |
|---|---|---|---|---|
| Đóng bẫy "Xem trước" | ✅ | ✅ | ✅ | ❌ |
| Nội dung Builder tới học viên | ✅ | ❌ (đã báo rõ) | — | ❌ |
| Backend mới | nhiều | **không** | xoá | không |
| Rủi ro | cao | **thấp** | cao | 0 |
| Mất tính năng | không | không | **có** | không |
| Có thể là bước 1 của A | — | **✅** | — | — |

---

## 4. Khuyến nghị

**Chọn B.** Lý do:

1. Sửa đúng **cái hố nguy hiểm nhất** (admin tưởng đã publish) với chi phí nhỏ và rủi ro thấp.
2. **Không mất tính năng** — tôn trọng AGENTS.md.
3. **Mở đường cho A**: khi nào muốn nối thật, chỉ cần thêm renderer + grading; banner cảnh báo được gỡ đi.
4. Trung thực: thay vì để hệ thống "nói dối" admin, ta nói rõ giới hạn.

**Nếu bạn muốn A ngay**, tôi đề nghị chia nhỏ:
- **A1** (rẻ): render TEXT/IMAGE/AUDIO/TABLE cho học viên — không cần grading.
- **A2** (vừa): render QUESTION 3 loại dạng "tự kiểm tra" (hiện đáp án khi bấm) — **không** lưu điểm, không chạm streak.
- **A3** (lớn): grading server-side + lưu submission + tích hợp điểm/streak.

A1+A2 đã khiến 6 lesson published "sống" mà chưa cần API mới; A3 mới là phần nặng.
