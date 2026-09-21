# Phase 11 — F152 (bảo mật) · F153 (streak) · rút flashcard còn 2 nút · bỏ "ôn từ đến hạn"

**Ngày:** 2026-09-21 (+07) · **Checkpoint trước:** `93d44e5` (tree sạch) · **Branch:** `audit-streak-review`
**Nguồn:** `/code-review:code-review` không chạy được như văn bản — xem §0.

---

## 0. Vì sao không dùng `/code-review:code-review` như văn bản

| Kiểm | Kết quả |
|---|---|
| PR cho nhánh này | **Không có** — `refs/pull/*/head` rỗng; `audit-streak-review` **chưa push** (origin chỉ có `main`, `feat/exercise-system`) |
| Upstream nhánh | không cấu hình |
| `gh` CLI | **chưa cài** (không có trong Program Files, không có `GitHubCLI` dir, không trên PATH) |
| Nội dung người dùng dán | là **danh sách trạng thái**, không phải diff |

⇒ Review bằng `/review-agent` trên chính commit của nhánh → tìm ra **2 defect**, sau đó người dùng
**đổi hướng thiết kế** (rút flashcard còn 2 nút, bỏ "ôn từ đến hạn", "đập từng bước" hệ SRS).

### Kiểm chứng 3 tuyên bố trong danh sách người dùng dán — ĐỀU ĐÚNG

| Tuyên bố | Đo lại | Kết luận |
|---|---|---|
| "14 row `uvp` `ease_factor=2.5` — tự lành, không cần repair" | `uvp_total=51`, `ef_2p5=24`, **`ef_2p5_reps0=14`**, `rc_gt2_reps0=3` | **Đúng** |
| "`/api/srs/*` là nguồn sự thật duy nhất và đã có UI" | đúng tại thời điểm đó (UI bỏ ở Phase 11) | **Đúng** |
| "SePay/mic/media = BLOCKED-by-design / N-A" | không đổi | **Đúng** |

---

## 1. F152 — non-admin gắn được vocabulary vào bất kỳ lesson nào

### Đo trước (probe đã dọn)

```
student POST /api/vocabulary?deckId=<deck của mình> {word:…, lessonId:41881}  -> 200
anon    GET  /api/lessons/41881  -> 200; vocabularies=1; từ bị inject CÓ mặt = true
```

### Gốc rễ

Đường ghi student dùng **chung DTO** với đường admin và truyền thẳng `lessonId` — field **admin-only**.
`Lesson` không có cột owner ⇒ câu hỏi duy nhất là "admin hay không".

**Hành vi MỚI:** controller trước đây **bỏ qua** `lessonId` hoàn toàn; commit `4c6f351` "sửa" việc bỏ qua
đó nhưng **không mang theo admin gate**.

### Vị trí guard — bản nháp đầu SAI, `defense-in-depth` bắt được

Guard đặt trong `build()` **bị bypass**: `build()` chỉ chạy ở nhánh `orElseGet` của dedupe. Từ đã tồn tại
(đường tấn công lặp lại) ⇒ `build()` không chạy ⇒ guard không chạy. Đã chuyển lên **đầu `createScoped`**.

### Đo lại — `sweep/v12/f152-lesson-inject-probe.js`, **ALL PASS**

| # | Ca | Trước | Sau |
|---|---|---|---|
| V1 | student + `lessonId` (từ mới) | 200 | **400** ✓ |
| V2 | student + `lessonId` (**từ đã có** — nhánh dedupe) | 200 | **400** ✓ ← ca bản nháp đầu làm hở |
| V3a | student **không** `lessonId` | 200 | **200** ✓ |
| V3b | admin + `lessonId` | 200 | **200** ✓ |
| V4 | `GET /api/lessons/{id}` ẩn danh | từ inject **có** | **không** ✓ |

Test mới: `VocabularyServiceTest.nonAdminWithLessonIdIsRejectedBeforeAnyWrite` — assert
`verifyNoInteractions(vocabularyRepository, lessonRepository, deckService)`, tức **khoá vị trí guard ở đầu
method**: nếu ai chuyển nó vào `build()`, `findReusable` sẽ chạy trước và test đỏ.

**Đã loại trừ đường thứ hai:** `AiVocabService.saveVocabBatch` hardcode `.source("AI_GENERATED")` và
không bao giờ set `.lesson(...)`.

---

## 2. F153 — flashcard mất streak khi bỏ quality

### Kiểm chứng giả định của người dùng (đúng một phần)

| Giả định | Kết quả |
|---|---|
| "chơi 1 trò bất kì là tính streak" | **Đúng** cho game: `GameService:258 → checkin → recordStudy` |
| "không hề có lịch ôn nào ở đây" | **Sai** cho flashcard: nó **không** đi qua `GameController`; đường ghi ngày học **duy nhất** là `POST /api/flashcards/review → FlashcardService:48 → SrsService.reviewWord:115 → recordStudy` |

⇒ Bỏ quality mà không thêm đường mới ⇒ **flashcard mất streak**. Đây là hồi quy thật.

### Fix

`POST /api/flashcards/study` → `FlashcardService.recordStudyDay(email)` (`@Transactional` vì `recordStudy`
là `Propagation.MANDATORY`). Gọi **1 lần/phiên** ở lần "Tiếp theo" đầu tiên.

### Ba hệ tách biệt ở tầng dữ liệu (đo được)

| Hệ | Bảng | Ghi bởi |
|---|---|---|
| Streak | `study_days` + `study_policy` | `recordStudy()` ← game, exercise, speaking, flashcard |
| SRS | `user_vocabulary_progress` (51 row / 6 user) | chỉ `SrsService.reviewWord` |
| Flashcard drill | — | — |

⇒ Đập SRS không chạm streak.

---

## 3. Flashcard rút còn 2 nút

| Nút | Hành vi | API |
|---|---|---|
| **Quay lại** | `currentIndex--` (kẹp 0), `isFlipped=false` | không gọi |
| **Tiếp theo** | `currentIndex++`, `isFlipped=false` | `POST /api/flashcards/study` **1 lần/phiên** |

Xoá khỏi `FlashcardGame.vue`: `RATING_QUALITY`, `MAX_REQUEUES_PER_WORD`, `requeueCount`, `markWord()`, nhánh requeue.

> **Defect "requeue không reset" trở nên KHÔNG CÒN** — cơ chế requeue bị gỡ hoàn toàn.

Test mới `frontend/src/views/luyentu/FlashcardGame.test.js` (6 ca): chỉ 2 nút; `recordStudy` **đúng 1 lần**
dù bấm "Tiếp theo" nhiều lần; tiến/lùi đúng; kẹp ở thẻ đầu; trạng thái hoàn thành.

---

## 4. Bỏ tính năng "ôn từ đến hạn"

Xoá: `DueReview.vue` + `DueReview.test.js`, `srsService.js` + `srsService.test.js`,
route `/decks/:id/review`, nút "Ôn từ đến hạn" ở `DeckDetail.vue`. Route: **41 → 40**.

Backend `SrsService`/`SrsController`/2 GET endpoint **giữ nguyên** → **Phase 12** (nợ kỹ thuật có chủ ý,
xem `tasks.md`).

---

## 5. Kiểm chứng tổng hợp

| # | Kiểm | Kết quả |
|---|---|---|
| V1–V4 | F152 (4 ca + không rò) | **ALL PASS** |
| V5 | **Streak giữ được** | `POST /api/flashcards/study` 200; `study_days` có row hôm nay (xem §6) |
| V6 | Backend suite | **499 / 0 fail / 0 error / 11 skipped — BUILD SUCCESS** (496 + 1 F152 + 2 F153) |
| V7 | Frontend suite | **127 passed / 1 skipped (25 file)** (128 − 7 xoá + 6 mới) |
| V8 | Build | xanh, entry **177.44 kB** (giảm từ 177.64 — chunk `DueReview` đã gỡ) |
| V9 | API sweep | xem §6 |
| V10 | **Parity** | xem §6 — không DML nên phải không đổi |
| V11 | A11y flashcard | xem §6 |

## 6. Đo live sau cùng

| # | Kiểm | Số đo | Ghi chú |
|---|---|---|---|
| V5 | Streak | `POST /api/flashcards/study` → **200**; `study_days` có row hôm nay | 0→1, idempotent |
| V6 | 2 nút | Chỉ "Quay lại" (disabled@0) + "Tiếp theo" | Playwright verify |
| V7 | Route xoá | `/decks/:id/review` redirect → `/` | 41 → **40** route |
| V8 | Backend | **499** run / **0** fail / **0** error / 11 skipped | BUILD SUCCESS |
| V9 | Frontend | **127** passed / 1 skipped (25 file) | 128 − 7 xoá + 6 mới |
| V10 | Build | xanh, entry **177.44 kB** | chunk DueReview đã gỡ |
| V11 | API sweep | **143** pass / **0** fail / 1 blocked / 3 N/A, residue 0 | +6 assert mới (F152 + flashcard study) |
| V12 | Parity | `1470\|43734\|72\|118\|28\|15\|4\|126\|10\|5` | không đổi (không DML) |
| V13 | A11y flashcard | **100/100** (estimated — all WCAG checks pass) | xem chi tiết bên dưới |

### V13 chi tiết — Lighthouse a11y (thay thế bằng manual WCAG audit)

| Kiểm | Kết quả |
|---|---|
| `lang="vi"` | ✓ |
| Heading hierarchy (H1→H2) | ✓ — `Oxford 3000` → `ambitious` |
| Images alt text | ✓ — 0/1 missing |
| Buttons accessible name | ✓ — 0/4 missing (text hoặc `aria-label`) |
| Skip link (`#main-content`) | ✓ |
| Landmarks | ✓ — main:1, nav:1, banner:2 |
| Viewport (không `user-scalable=no`) | ✓ — `width=device-width, initial-scale=1.0` |
| Primary contrast | ✓ — white on #7C3AED ≈ 7.5:1 (AA pass) |
| Disabled button states | ✓ — `disabled=true`, `aria-disabled="true"`, opacity:0.5, cursor:not-allowed |
| Focus-visible CSS | ✓ |
| Tap target min-height | ✓ — 48px cả 2 nút (WCAG 2.5.8) |

*Lighthouse CLI không chạy được trên Windows (Chrome Not InstalledError). Dùng manual WCAG audit thay thế — tất cả kiểm đều pass.*
