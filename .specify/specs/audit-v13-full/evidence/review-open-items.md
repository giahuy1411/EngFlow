# audit-v13 — Review chéo (skill `review-agent`) trên diff 5 hạng mục OPEN

**Ngày:** 2026-09-22 (+07) · **Phạm vi:** các thay đổi cho F-13-02/22, F-13-07, F-13-08, F-13-20, đóng F-13-10.

**Ghi chú phương pháp:** agent adversarial **đã hoàn tất** (1551s, 113 tool calls) và trả về 7 finding. Tôi **kiểm chứng lại từng điểm** trước khi sửa — tất cả đều đúng. Bên dưới là kết quả sau khi xử lý.

---

## Kết quả xử lý 7 finding của review agent

| # | Ưu tiên | Finding | Xử lý |
|---|---|---|---|
| P1 | — | 7 file mới **untracked**; `Login.vue`/`Register.vue` import `safeRedirect.js` → commit bằng `git -am` sẽ **hỏng build** | **XÁC NHẬN** — không tự commit; ghi cảnh báo rõ (xem dưới) |
| P2 | — | Premium vẫn mất đích: `checkout()` không mang `redirect`, `PremiumCheckout.vue:96` hardcode `/speaking` | **ĐÃ SỬA** — mang `redirect` qua checkout + `safeRedirect` khi xong |
| P2 | — | `AdminLessonBuilder.f13.test.js` mock `getById` nhưng component gọi `getAll()` → test xanh trên component crash | **ĐÃ SỬA** — mock `getAll()`; live đã in `Failed to load lesson` trước đó |
| P3 | — | `safeRedirect` trả giá trị **đã decode** → hỏng query có `%` (`?q=100%25off` → `?q=100%off`) | **ĐÃ SỬA** — validate decoded, **trả original**; +3 test |
| P3 | — | Backend vẫn nhận `blockType` QUESTION/SUBMISSION (claim "không tạo được" chỉ đúng ở UI) | **ĐÃ SỬA** — `parseBlockType()` allow-list ở service; +6 test |
| P3 | — | `deploy.sql` thêm CHECK thiếu precondition guard (khác với script vá) | **ĐÃ SỬA** — thêm `THROW 51003` |
| P3 | — | `guestOnly` + đã login → mất `?redirect=` | **ĐÃ SỬA** — `next(safeRedirect(to.query.redirect))` |

### ⚠️ P1 — CẢNH BÁO COMMIT (quan trọng nhất)

7 file mới đang **untracked** và **bắt buộc** phải `git add` thủ công:

```
frontend/src/utils/safeRedirect.js               ← Login.vue/Register.vue/router import
frontend/src/utils/safeRedirect.test.js
frontend/src/views/admin/AdminLessonBuilder.f13.test.js
frontend/src/views/lessons/LessonExerciseTab.f13.test.js
frontend/src/views/lessons/LessonLayout.h1.test.js
tasks/streak-study/fix-study-policy-check.sql
docs/lesson-builder-status.md
```

**`git commit -am "..."` sẽ bỏ sót `safeRedirect.js`** → `npm run build` hỏng ở bản clone sạch
(*"Failed to resolve import"*), và 3 file test + script vá DB sẽ **không tồn tại trong repo**.
→ **Phải `git add` tường minh 7 file này**, không dùng `-a`.

### Caveat của agent (đã xử lý)

Agent ghi nhận cây làm việc **đổi giữa lúc review**: `StudyActivityService.java` ban đầu đọc
`end.minusDays(windowDays)` (cửa sổ 8 ngày) rồi thành `minusDays(windowDays - 1L)`. **Bản hiện tại
là bản đúng** — và tôi đã thêm test `windowIsInclusiveOfToday` để khoá nó.

---

## [P2] `recentUsers` đổi cửa sổ từ 7 → 8 ngày — `StudyActivityService.countActiveLearnersInLastDays` — **ĐÃ SỬA**

**Phát hiện bởi:** tự review (kiểm tra quy ước cửa sổ của codebase).

**Kịch bản sai:** codebase dùng quy ước *inclusive* ở `StudyActivityService.snapshot()`:
`LocalDate cutoff = today.minusDays(window - 1L)` (dòng 77). Tôi viết `end.minusDays(windowDays)` →
cửa sổ thành **8 ngày** (`today-7 .. today`) trong khi UI ghi **"7 ngày gần nhất"**
(`AdminDashboard.vue:51,64`). Nhãn và số liệu lệch nhau đúng 1 ngày.

**Bằng chứng sửa:**
- Đổi thành `end.minusDays(windowDays - 1L)` + javadoc ghi rõ quy ước.
- Test mới `windowIsInclusiveOfToday` khẳng định biên `today.minusDays(6) .. today`.
- Live: `recentUsers=2` khớp `SELECT COUNT(DISTINCT user_id) ... BETWEEN today-6 AND today` = 2.

## [P3] Import chết `java.time.LocalDateTime` — `AdminService.java:23` — **ĐÃ SỬA**

Xoá biến `sevenDaysAgo` khiến import thành thừa. Đã xoá.

## [P3] `countByLastStudyDateAfter` thành không caller — **ĐÃ ĐÁNH DẤU**

Không xoá (có thể là API công khai), nhưng thêm `@Deprecated` + javadoc cảnh báo, để lần sau
không ai vô tình dùng lại cột chết.

---

## Đã kiểm tra, KHÔNG phải defect

| Nghi vấn | Kiểm chứng | Kết luận |
|---|---|---|
| `addBlock` có tạo được QUESTION không? | `addBlock` hardcode `blockType: 'TEXT'` | Không — an toàn |
| `onBlockTypeChange` không validate? | Select đã `disabled`; đường tạo mới duy nhất là `addBlock` | Defense-in-depth; UI chặn đủ |
| `/lessons/{id}` có phải route thật? | `router/index.js:41` | Có |
| `deploy.sql` thêm CHECK trước hay sau INSERT? | CHECK ở dòng ~36, INSERT id=1 ở dòng ~47 | CHECK trước; row `id=1` thoả constraint |
| `THROW 51003` trong script vá có lỗi cú pháp? | Chạy thật, 0 `Msg` lỗi | Hoạt động |
| `@CheckConstraint` có đúng JPA version? | `jakarta.persistence-api:3.2.0` có class; compile OK | Đúng |
| `safeRedirect` có bypass? | 14 ca tấn công (`//`, `%2F%2F`, `\`, `\t`, `javascript:`, `%00`…) | **0 leak** |
| `ddl-auto=update` có tự thêm CHECK? | Tra tài liệu chính thức + maintainer Hibernate | **Không** → cần SQL (đã làm) |
| `@Check` có phải API đúng? | Docs Hibernate 7: `@Deprecated(since="7")` | **Sai** → đã đổi sang `@CheckConstraint` |
| Parity sau mọi thay đổi? | `p16-parity.sql` | Khớp `1470\|43735\|72\|118\|29\|15\|4\|126\|10\|5` |

## Khoảng trống CÒN LẠI (ghi rõ, không tự mở rộng phạm vi)

- **`api.js` 401/403 interceptor** (`services/api.js:29,35,68`) đá về `/login` **trần**, không mang
  `redirect`. Đây là luồng *token hết hạn giữa phiên* (khác guard), và **có từ trước** — không do
  thay đổi này gây ra. Người dùng sẽ mất đích khi bị hết hạn token. Cùng lớp F-13-20 nhưng khác
  đường; nên là hạng mục riêng nếu muốn xử lý.
- **`current_streak` + 3 query `lastStudyDate` cũ** vẫn là dead code trong schema (không xoá — đổi
  schema cần backup + quyết định riêng, theo P3).
- **F-13-02 phương án B** không làm nội dung builder tới học viên; đã nói rõ trong UI + docs.

---

## Bonus — chống loop do chính fix `guestOnly` tạo ra — **ĐÃ SỬA**

**Phát hiện bởi:** tự kiểm chứng ca biên sau khi sửa P3 `guestOnly`.

**Rủi ro:** cho `guestOnly` tôn trọng `?redirect=` mở ra khả năng `redirect=/login` → bounce tới
`/login` → bounce lại → loop. Kiểm live: **không loop** (vue-router tự chặn), nhưng đó là hành vi
ngầm, không phải bảo đảm của code tôi.

**Fix:** chặn tường minh — nếu `target === to.path` thì về `/lessons`.
**Bằng chứng live:** `/login?redirect=%2Flogin` → URL cuối **`/lessons`** (không loop).
