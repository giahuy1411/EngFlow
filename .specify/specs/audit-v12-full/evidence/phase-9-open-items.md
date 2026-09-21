# Phase 9 — Xử lý 4 hạng mục OPEN của audit-v12 (F147 · F148 · F150 · C6)

**Ngày:** 2026-09-21 (+07) · **Checkpoint trước:** `4d2890a` · **Branch:** `audit-streak-review`
Mỗi hạng mục: đo trước → sửa → **đo lại cùng phép đo**. Không có tuyên bố nào thiếu số.

---

## F148 — Hợp nhất 2 thuật toán SRS (làm TRƯỚC vì chạm dữ liệu)

### Phát hiện sâu hơn mức audit đầu tiên báo

Audit v12 báo "`/api/srs/*` mồ côi". Đọc kỹ thì **nghiêm trọng hơn**: có **HAI thuật toán spaced-repetition
độc lập ghi cùng bảng** `user_vocabulary_progress`:

| | `SrsService.reviewWord` | `FlashcardService.reviewFlashcard` (cũ) |
|---|---|---|
| Thuật toán | **SM-2** đầy đủ | bảng ngày cố định 1/3/7/14 |
| Ghi `ease_factor` | có | **KHÔNG** |
| Ghi `repetitions` | có | **KHÔNG** |
| Ghi `srs_interval` | có | **KHÔNG** |
| Cap interval (F106) | 365 ngày | 14 ngày |
| Ai gọi | chỉ `SrsController` (0 caller frontend) | `FlashcardGame.vue` (**UI thật duy nhất**) |

Dòng 68 của `FlashcardService` là **comment** nói "giống `SrsService.reviewWord`" — nhưng **không phải lời gọi**.
Hai thuật toán không biết nhau.

### Bằng chứng dữ liệu (đo trước khi sửa)

```
uvp rows = 14
rows với ease_factor = 2.5 (chưa bao giờ được SM-2 nâng)  = 11   (79%)
rows với repetitions = 0                                   = 5
rows review_count > 2 NHƯNG repetitions = 0                = 2    <- đã ôn 3+ lần, SM-2 vẫn nghĩ là từ mới
```

### Lỗi người dùng thấy được ở 3 nút Lại/Tiếp theo/Dễ

`markWord()` gộp `rating !== 'again'` ⇒ **"Dễ" và "Tiếp theo" gửi request y hệt nhau**. Nút "Dễ" không có
tác dụng riêng. Ngoài ra `markWord()` chỉ `currentIndex++` ⇒ **"Lại" không đưa từ quay lại** trong phiên.

### Fix

| File | Thay đổi |
|---|---|
| `FlashcardReviewRequest.java` | `Boolean isKnown` → `Integer quality` (0–5, `@Min`/`@Max`) |
| `FlashcardService.java` | Uỷ quyền `srsService.reviewWord(userId, vocabId, quality)`; xoá bảng 1/3/7/14 và `recordStudy` trực tiếp |
| `flashcardService.js` | `reviewFlashcard(vocabularyId, quality)` |
| `FlashcardGame.vue` | `RATING_QUALITY = { again: 1, good: 4, easy: 5 }`; **thêm requeue cho "Lại"** (đẩy thẻ xuống cuối, cap 3 lần/từ) |
| `FlashcardServiceStudyActivityTest.java` | Viết lại: assert uỷ quyền SRS + giữ hợp đồng "1 review = 1 ngày học" |

### Đo lại — E2E trên container đã rebuild

| Probe | Trước | Sau | Đúng? |
|---|---|---|---|
| `POST /api/flashcards/review {quality:5}` trên vocab 10018 | — | `ease_factor 2.5 → **2.6**`, `repetitions 1 → **2**`, `srs_interval 1 → **6**` | ✓ SM-2 (q=5 → EF+0.1; reps 1→2 = 6 ngày) |
| `{quality:1}` ("Lại") | — | `repetitions → **0**`, `srs_interval → **1**`, `ease_factor → **2.06**` | ✓ reset chuỗi |
| `{quality:9}` | — | **400** `"quality phải từ 0 đến 5"` | ✓ validation |
| `{isKnown:true}` (shape cũ) | 200 | **400** | ✓ shape đã đổi |
| **"Dễ"(5) vs "Tiếp theo"(4)** | **giống hệt** | `ease 2.5→2.6` (q=5) vs `+0.0` (q=4) | ✓ **khác nhau thật** |

**Đây là điều trước đây BẤT KHẢ THI** — `ease_factor` kẹt ở 2.5 vĩnh viễn.

### Dữ liệu cũ: KHÔNG migrate

14 row có `ease_factor=2.5, repetitions=0` là **trạng thái khởi tạo hợp lệ** của SM-2; lần review kế tiếp tự tính
đúng (`repetitions=0 → interval=1` rồi tăng dần) — đúng cơ chế "tự lành" mà F106 đã dùng.

### `/api/srs/*` giữ lại

Sau hợp nhất, `SrsService` là **nguồn sự thật duy nhất**; `/api/srs/stats` là API thống kê hợp lệ. Ghi rõ: **chưa có
UI gọi** (owner chọn chưa cần màn "ôn từ đến hạn").

---

## F147 — Lưu từ deck-scoped + atomic

### Bản chất: `vocabulary` là TỪ ĐIỂN CHUNG, không phải kho từ cá nhân

| Vai trò | Số row (đo live) |
|---|---|
| Từ điển curated (OXFORD/AWL/TOEIC/IELTS/THPT) | **90/127** |
| AI sinh | **31/127** |
| Gắn bài học (`lesson_id` NOT NULL) | **1/127** |
| Thuộc deck **do user sở hữu** | **0** |

**Không có cột `owner`/`user_id` nào** ⇒ schema là chủ ý, **không phải lỗi schema**. Quyền sở hữu nằm ở
`decks.owner_id` + `deck_words`. Lỗi là **đường ghi của student bỏ qua tầng sở hữu**.

### Triệu chứng đo được

```
GET /api/vocabulary/search?keyword=negotiate  ->  2 kết quả trùng (id 10038, 10083)
```
`findByWordContainingIgnoreCase` không lọc ⇒ row do student tạo hiện với mọi người.

### Fix

| File | Thay đổi |
|---|---|
| `service/VocabularyService.java` **(mới)** | `createScoped(request, deckId, userId, isAdmin)`: non-admin **bắt buộc** có deck; **dedupe** (dùng lại row cùng `word` chưa gắn lesson); gọi `deckService.addWordToDeck` **trong cùng `@Transactional`**; resolve `lessonId` (trước bị bỏ qua) |
| `VocabularyController.java` | Nhận `@RequestParam deckId`; kiểm role; gọi service |
| `DeckSource.java` | Thêm `VIDEO_LESSON` (bỏ chuỗi hardcode `'Video lesson'`) |
| `vocabularyService.js` | `create(vocabData, deckId)` |
| `VideoLesson.vue` | **1 call** thay vì 2; `source: 'VIDEO_LESSON'` |
| `VocabularyServiceTest.java` **(mới)** | 6 test: authz, dedupe, không mượn từ lesson, admin không deck, lessonId |

### Đo lại — E2E trên container

| Probe | Kết quả |
|---|---|
| Student **không** deckId | **400** `"Cần chọn bộ từ để lưu từ vựng…"` ✓ |
| Student **có** deckId của mình | **200**; vocab row **và** `deck_words` link cùng transaction (1 row + 1 link) ✓ |
| Student gắn deck **người khác** (IDOR) | **400** `"Bạn không có quyền chỉnh sửa bộ từ này"` ✓ **và vocab row đã rollback** (0 leak) ✓ |
| Lưu cùng từ 2 lần | **1** vocab row, link vào **2** deck (dedupe) ✓ |
| Tổng row mồ côi | **27** (không đổi — dữ liệu cũ, không tự xoá) |

### 27 row rác + vài row test — KHÔNG tự xoá

Xoá dữ liệu là quyết định của owner. Danh sách nằm ở §"Hạng mục chưa làm" của báo cáo.

---

## F150 — CLS 0.104 → 0.001

### Gốc rễ (đo, không suy đoán)

Shell `min-h-screen` cho footer **first paint ở y≈827** — **trong** viewport 867px (40px đầu của footer thấy được).
Chunk Home (12.4 kB, lazy) về sau → `<main>` giãn lên ~3351px → footer bị đẩy ra y≈3415. Khối 96px **đang thấy**
rời màn hình = **CLS 0.104** (tái lập 4 lần).

### Vì sao lần thử cũ (v12) thất bại — SAI GIÁ TRỊ

`calc(100vh − 64px − 96px)` = **707px** → footer ở `64 + 707 = 771` < 867 ⇒ **vẫn trong màn hình** ⇒ chỉ 0.098.

### Fix (giá trị ĐÚNG)

```css
#main-content { min-height: 100vh; }   /* footer first-paint ở y = 64 + 867 = 931 > 867 = ngoài màn hình */
```
Ghi ở `frontend/src/assets/app-layout.css`, kèm comment nêu cả số của lần thử sai.

### Đo lại — Playwright `PerformanceObserver` trên **production build**, 5 lần

```
trước:  0.104 / 0.111 / 0.104 / 0.104
sau:    0.0001 / 0.00008 / 0.0001 / 0.00005 / 0.00013      (footer shift BIẾN MẤT)
```

| Route | CLS sau | `#main-content` min-height | Overflow |
|---|---|---|---|
| `/` | **0.00013** | 923px | 0 |
| `/lessons` | **0.00008** | 923px | 0 |
| `/login` | **0.00012** | 923px | −15 (gutter scrollbar) |
| `/decks` | **0.0001** | 923px | 0 |

**Lighthouse xác nhận độc lập** (chrome-devtools MCP, `/`): `cumulative-layout-shift` **0.103 → 0.001**, score **1**.
Accessibility vẫn **100**.

---

## C6 — Xoá Azure Speech SDK (~15MB)

### Bằng chứng

`grep com.microsoft.cognitiveservices|SpeechConfig|SpeechRecognizer` → **0** ở `src/main` **và** `src/test`.
Chấm phát âm thật dùng `SpeakingAssessmentService` (`PROVIDER = "LOCAL_WHISPER_LLM"`) → Whisper sidecar + Ollama.

### Xoá

| File | Thay đổi |
|---|---|
| `pom.xml` | Xoá `<dependency> com.microsoft.cognitiveservices.speech:client-sdk:1.51.1` |
| `application.properties` | Xoá `azure.speech.key` / `azure.speech.region` (không class nào đọc) |
| `PronunciationAssessmentResult.java` | Xoá (record chết) |

### ⚠️ Một phụ thuộc ẨN bị lộ ra — và đã sửa đúng cách

Xoá xong, **1 test đỏ**:
```
SpeakingSubmissionResponseTest.fromExposesOnlySafeUserSummary
  Java 8 date/time type `java.time.LocalDateTime` not supported by default
```
Nguyên nhân: Azure SDK đang **cung cấp ngầm `jackson-datatype-jsr310`** cho classpath. Xoá nó ⇒ mất `JavaTimeModule`
(test dùng `new ObjectMapper().findAndRegisterModules()`).

**Không phải rollback — khai báo tường minh**, để app không phụ thuộc một thư viện không dùng:
```xml
<dependency>
  <groupId>com.fasterxml.jackson.datatype</groupId>
  <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```
Đây chính là lý do kế hoạch bắt buộc chạy **full test** sau khi xoá dependency.

### Đo lại

| Kiểm | Kết quả |
|---|---|
| Azure trong dependency tree | **0** |
| Class `cognitiveservices` trong JAR | **0** |
| `jackson-datatype-jsr310` trong tree | `2.21.2:compile` ✓ |
| Backend suite | **492 run / 0 fail / 0 error / 11 skipped — BUILD SUCCESS** |
| `mvn -o package` | BUILD SUCCESS |

---

## Kiểm chứng tổng hợp

| # | Kiểm | Kết quả |
|---|---|---|
| V1 | Backend suite (`mvn -o test`) | **492 run / 0 fail / 0 error / 11 skipped** (485 + 4 F148 + 6 F147 − 3 thay thế) |
| V2 | Frontend suite (`npx vitest run`) | **121 passed / 1 skipped (24 file)** (119 + 2) |
| V3 | Build (`npx vite build`) | xanh, entry **177.44 kB** (không tăng) |
| V4 | API sweep (`sweep/v12/api-sweep.js`) | **137 pass / 0 fail / 1 blocked / 3 N/A** (133 + 4 assert mới) |
| V5 | F148 dữ liệu | `ease_factor` **đổi theo quality**; hết kẹt 2.5 |
| V6 | F147 authz + IDOR | no-deck **400**; own-deck **200+link**; other-deck **400 + rollback** |
| V7 | F147 dedupe | cùng từ 2 lần → **1** row, 2 link |
| V8 | F150 CLS | **0.104 → ~0.0001** (Playwright ×5); Lighthouse **0.001** |
| V9 | C6 | 0 Azure trong tree/JAR; build xanh; suite xanh |
| V10 | **Parity** | **`1471\|43735\|72\|127\|28\|15\|4\|126\|14\|5`** — không đổi |
| V11 | Regression test | F148: 4 test; F147: 6 test — tất cả pass |
| V12 | A11y | Lighthouse `/` Accessibility **100** (không hồi quy) |

**Rác sau cùng: 0** (AUDIT lessons/decks/vocab/payments = 0).

---

## Hạng mục CHƯA làm (ghi rõ)

| Hạng mục | Lý do |
|---|---|
| UI "ôn từ đến hạn" (`/decks/:id/review` + `GET /api/srs/due/{deckId}`) | Owner chọn **chưa cần**. `/api/srs/due` + `/stats` vẫn hoạt động |
| Xoá **27 row `vocabulary`** không thuộc deck nào (+ vài row test) | Xoá dữ liệu là quyết định owner; cần liệt kê ID cụ thể |
| Repair 14 row `uvp` có `ease_factor=2.5` | **Không cần** — giá trị khởi tạo hợp lệ, tự lành ở lần review kế tiếp |
| Retire `/api/srs/*` | Không làm — sau hợp nhất `SrsService` là nguồn sự thật duy nhất |
