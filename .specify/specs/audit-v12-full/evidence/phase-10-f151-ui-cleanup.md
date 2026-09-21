# Phase 10 — F151 (IDOR) · UI "ôn từ đến hạn" · Dọn dữ liệu rác

**Ngày:** 2026-09-21 (+07) · **Checkpoint trước:** `4c6f351` · **Branch:** `audit-streak-review`
Ba hạng mục còn lại sau Phase 9. Mỗi bước: đo → làm → **đo lại cùng phép đo**.

---

## F151 — IDOR: `/api/srs/due/{deckId}` đọc được nội dung deck private của người khác

### Phát hiện (đo live, không suy đoán)

Deck `30033` có `owner_id=3` (admin), `is_public=0`:

```
student GET /api/decks/30033    -> 400 "Bạn không có quyền truy cập bộ từ vựng này"   (đúng)
student GET /api/srs/due/30033  -> 200 [ {...} ]                                     (SAI)
```

Rò rỉ **nội dung**, không chỉ metadata: `vocabId`, `word`, `pronunciation`, `definitionVi`,
`definitionEn`, `exampleSentence`.

### Gốc rễ

`SrsService.getDueWords(userId, deckId)` chỉ gọi `deckWordRepository.findByDeckIdOrderByOrderIndexAsc(deckId)`
— **không kiểm quyền sở hữu**. Trong khi `DeckService.getDeckById(deckId, userId)` đã có sẵn đúng logic.

### Fix — tái dùng primitive đã có

`SrsService.getDueWords` gọi `deckService.getDeckById(deckId, userId)` **trước** khi đọc `deck_words`.
Không viết lại logic quyền. `DeckService` không phụ thuộc `SrsService` ⇒ không circular.

### Đo lại — probe `sweep/v12/f151-idor-probe.js`, 9/9 PASS

| # | Probe | Kỳ vọng | Kết quả |
|---|---|---|---|
| 1 | `GET /api/decks/30033` (student) | 400 | **400** ✓ |
| 2 | `GET /api/srs/due/30033` (student) | 400 | **400** ✓ |
| 3 | Nội dung từ vựng trong body từ chối | không lộ | **không lộ** ✓ |
| 4 | `GET /api/srs/due/999999` | 404 | **404** ✓ (trước: 200 + `[]`) |
| 5 | `GET /api/decks/999999` (parity) | 404 | **404** ✓ |
| 6 | `GET /api/srs/due/10006` (public) | 200 | **200** ✓ |
| 7 | Danh sách đến hạn là array không rỗng | đúng | **7 phần tử** ✓ |
| 8 | Owner đọc deck private của mình | 200 | **200** ✓ |
| 9 | `GET /api/srs/stats` | 200 | **200** ✓ |

**Lợi ích kèm theo:** deck không tồn tại nay trả **404** thay vì **200 + `[]`** — khớp `/api/decks/{id}`.

### Regression test mới

`src/test/java/com/datn/engflow/service/SrsDueWordsAuthzTest.java` — 4 test:
private deck của người khác → ném `BadRequestException` + `verifyNoInteractions(deckWordRepository)`;
private deck của chính mình → OK; public deck → OK; deck không tồn tại → `ResourceNotFoundException`.

---

## Item A — UI "ôn từ đến hạn"

### Trước: API hoạt động nhưng **0 caller**

`GET /api/srs/due/{deckId}` trả đúng dữ liệu nhưng không màn nào gọi. Đây là lý do F148 ghi
"chưa có UI gọi" — nay đã có.

### Vấn đề tên field khác nhau

API trả `vocabId` / `definitionVi` / `pronunciation` / `exampleSentence`; thẻ flashcard render
`id` / `meaning` / `phonetic` / `example`. Lớp map đặt tại **service** (giống `deckService.normalizeDeck`).

### File

| File | Việc |
|---|---|
| `frontend/src/services/srsService.js` **(mới)** | `getDueWords(deckId)` + map field; `getStats()`; `review(vocabId, quality)` |
| `frontend/src/views/luyentu/DueReview.vue` **(mới)** | Màn ôn; 4 trạng thái: loading / error / rỗng / thẻ / hoàn thành |
| `frontend/src/router/index.js` | Thêm `/decks/:id/review` (lazy, `requiresAuth`) |
| `frontend/src/views/luyentu/DeckDetail.vue` | Nút "Ôn từ đến hạn" — **link đầu tiên** trong hàng Game Buttons |
| `frontend/src/services/srsService.test.js` **(mới)** | 3 test: map field, non-array → `[]`, body review |
| `frontend/src/views/luyentu/DueReview.test.js` **(mới)** | 4 test: render, quality 1/4/5, rỗng, hoàn thành |

### Đo lại — live trên dev server (`localhost:5173`), đăng nhập `user@gmail.com`

| # | Kiểm | Kết quả |
|---|---|---|
| V3 | Mở `/decks/10006/review` | render "**7 từ đến hạn ôn hôm nay**", thẻ `determine` + `/dɪˈtɜː.mɪn/`, 3 nút Lại/Tiếp theo/Dễ ✓ |
| V5a | Bấm "**Tiếp theo**" | `POST /api/srs/review` body **`{"vocabId":10020,"quality":4}`** ✓ |
| V5b | Bấm "**Dễ**" | body **`{"vocabId":10021,"quality":5}`** ✓ |
| V5c | Bấm "**Lại**" | body **`{"vocabId":10022,"quality":1}`** ✓ |
| V5d | Ôn hết 7 thẻ | "**ĐÃ ÔN XONG 7 TỪ!**" + thanh tiến độ **7/7 = 100%** ✓ |
| V4 | Mở lại cùng route | "**KHÔNG CÓ TỪ NÀO ĐẾN HẠN**" ✓ |
| — | Nút vào ở `/decks/10006` | link `Ôn từ đến hạn` → `/decks/10006/review` ✓ |

**V4 là bằng chứng E2E thật:** 7 từ vừa ôn đã được server dời lịch sang tương lai ⇒ lần mở lại
`getDueWords` trả rỗng. Không phải mock.

### A11y + responsive (V6)

| Kiểm | Kết quả |
|---|---|
| Lighthouse desktop trên route mới | **Accessibility 100 · Best Practices 100 · SEO 100** |
| Contrast tự đo (alpha-composite, 43 phần tử text) | **0 vi phạm** |
| Mobile 390×844: overflow ngang | **0** (`scrollWidth 390 = clientWidth 390`) |
| Tap target (WCAG 2.5.8, đã áp miễn trừ Inline/Spacing) | **0 vi phạm** |

---

## Item B — Dọn dữ liệu rác

### Backup TRƯỚC DML (bắt buộc theo AGENTS.md)

```
BACKUP DATABASE english_learning TO DISK = '...engflow_2026-09-21-audit-v12-phase10.bak'
  WITH COMPRESSION, CHECKSUM, INIT   -> 24.850 pages, 0.326 s
RESTORE VERIFYONLY ... WITH CHECKSUM -> "The backup set on file 1 is valid."
```
Bản host: `C:\Users\ASUS\engflow-backups\engflow_2026-09-21-audit-v12-phase10.bak` (**36.737.024 bytes**).

### Phân loại 27 row mồ côi (đo bằng `sweep/v12/cleanup-precheck.sql`)

| Nhóm | Số | Xử lý |
|---|---|---|
| Test junk (`qatestword`, `testword`, `testword45851`, `adminword`, `test123`, `finaltest5`, `aisaved`) | 7 | **XOÁ** |
| AI bịa/trùng (`planeta` — không phải từ tiếng Anh; `innovate` 30124 — **trùng** `innovate` 10094 nguồn OXFORD5000) | 2 | **XOÁ** |
| AI từ tiếng Anh **thật** (technology, algorithm, airport, boarding, terminal, security, cooking, recipe, …) | **18** | **GIỮ** |
| **Tổng** | **27** | xoá 9 · giữ 18 |

> **Đính chính:** bản nháp kế hoạch ghi "giữ 19" — **sai một đơn vị**. `27 − 9 = 18`, và con số này
> khớp parity đo được (`127 − 9 = 118`). Số đúng là **18**.

### Kết quả DML — `sweep/v12/cleanup.sql` (1 transaction, `XACT_ABORT ON`)

| Bước | Số row xoá |
|---|---|
| 1. `user_vocabulary_progress` cho tập xoá (blocker duy nhất: `adminword` 50154) | **1** |
| 2. `deck_words` cho vocab bị xoá | 0 |
| 3. `deck_words` cho 4 deck test | 0 |
| 4. `decks` (10016, 30033, 50038, 50039) | **4** |
| 5a–5g. Con của lesson 61882 (`exercises`) | **1** (blocks/sections/snapshots/prompts/subs/progress đều 0) |
| 6. `vocabulary` (9 ID liệt kê) | **9** |
| 7. `lessons` (61882 TEST-DELETE-CASCADE) | **1** |

`Msg` errors: **none** (sqlcmd exit 0 không đủ — đã scan `Msg \d+`).

**POST-CHECK:** `orphans_left=18` · `deleted_ids_left=0` · `test_decks_left=0` · `lesson_61882_left=0`.

### Hệ quả lên parity — thay đổi CÓ CHỦ Ý

| | Trước | Sau |
|---|---|---|
| `lessons` | 1471 | **1470** |
| `exercises` | 43735 | **43734** |
| `vocabulary` | 127 | **118** |
| `decks` | 14 | **10** |

⇒ Parity mới: **`1470|43734|72|118|28|15|4|126|10|5`** — đo lại **khớp chính xác** dự đoán.

### FK đã kiểm (`sweep/v12/fk-map.sql`)

Mọi FK trỏ tới `vocabulary` / `decks` / `deck_words` / `lessons` / `exercises` đều **`NO_ACTION`** —
**không** có CASCADE. Xoá con trước cha, đúng như SQL đã làm.

---

## Một hồi quy do chính Item B gây ra — và cách sửa đúng

### Triệu chứng

Chạy lại `api-sweep.js` ngay sau cleanup: **133 pass** (trước 137), và xuất hiện
`BLOCK F147 deck-scoped save -> student has no deck to save into`.

### Gốc rễ (không phải lỗi sản phẩm)

Tôi đã xoá 3 deck "Test Deck" **do chính `user@gmail.com` sở hữu** (10016, 50038, 50039) — đó là
nguồn `ownDeckId` mà sweep vẫn ngầm dựa vào. **Đồng thời** sweep hardcode `deckId=30033` làm
"deck của người khác" — deck đó cũng vừa bị xoá, nên **assert IDOR sẽ lặng lẽ không còn chạy**.

⇒ Đây là **lỗi của harness**: một suite phụ thuộc dữ liệu ambient thay vì tự dựng dữ liệu.

### Fix — sweep tự cấp phát deck

`api-sweep.js` nay tạo 2 deck ngay trong block F147:
`AUDIT-V12-API-F147` (owner = student) và `AUDIT-V12-API-FOREIGN` (owner = admin), dùng `foreignDeckId`
cho assert IDOR. Cả hai được dọn bởi chính cleanup `AUDIT-V12-API-%` sẵn có.

### Đo lại

```
pass=137 fail=0 blocked=1 n_a=3
  crud         pass=19 fail=0 blocked=0
  flashcard    pass=5 fail=0 blocked=0
F147 student WITHOUT deckId is rejected (400)          PASS
F147 student WITH own deckId is accepted               PASS
F147   word is LINKED to the deck in the same transaction  PASS
F147   attaching to ANOTHER user's deck is blocked     PASS
F147   a word saved this way is still a shared dictionary entry (by design)  PASS
```

Residue: `AUDIT_DECKS=0 AUDIT_VOCAB=0 AUDIT_LESSONS=0 AUDIT_PAY=0`; parity **không đổi**.

---

## Kiểm chứng tổng hợp Phase 10

| # | Kiểm | Kết quả |
|---|---|---|
| V1 | F151 IDOR | `f151-idor-probe.js` **9/9 PASS** |
| V2 | F151 không phá hợp lệ | public 200 · owner 200 · stats 200 ✓ |
| V3 | UI render | 7 từ đến hạn, thẻ + 3 nút ✓ |
| V4 | UI rỗng (E2E thật) | "Không có từ nào đến hạn" ✓ |
| V5 | UI submit | quality **1 / 4 / 5** đúng body ✓ |
| V6 | UI a11y | Lighthouse **100/100/100**; contrast 0 vi phạm; mobile 0 overflow ✓ |
| V7 | Backend suite | **496 run / 0 fail / 0 error / 11 skipped — BUILD SUCCESS** |
| V8 | Frontend suite | **128 passed / 1 skipped (26 file)** (121 + 7) |
| V9 | Build | xanh, entry **177.64 kB**, chunk `DueReview` 8.41 kB |
| V10 | **Parity mới** | **`1470\|43734\|72\|118\|28\|15\|4\|126\|10\|5`** ✓ khớp dự đoán |
| V11 | Dọn sạch | orphans **18** (đều là từ AI thật) · 0 deck test · lesson 61882 **0** ✓ |
| V12 | API sweep | **137 pass / 0 fail / 1 blocked / 3 N/A** ✓ |
| V13 | Backup | `RESTORE VERIFYONLY` — "backup set on file 1 is valid" ✓ |

**Rác sau cùng: 0.**
