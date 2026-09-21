# audit-v12-full — findings

Mỗi finding cần: một phép đo cho thấy lỗi, gốc rễ đọc từ source, cách fix, và **đo lại bằng cùng probe**. Finding
chưa đo lại vẫn `OPEN`. **Lỗi của probe được ghi riêng** (mục cuối), không tính là finding.

---

## F146 — Tài liệu mô tả sai sản phẩm (README + CLAUDE.md)

**Mức:** MEDIUM (tài liệu, không phải hành vi) · **Trạng thái:** **FIXED** (đã sửa + verify bằng grep)
**Nguồn:** đo lại số thật rồi so với tài liệu (C2–C5 trong kế hoạch)

### Đo được

| Tài liệu ghi | Thực tế đo | Bằng chứng |
|---|---|---|
| `README.md:128` "Backend tests (**223 tests**)" | **485** | `.p0-v12-backend.log` |
| `README.md:77,131` "Frontend tests (**73 tests, 14 files**)" | **119 passed / 23 file** | `.p0-v12-frontend.log` |
| `CLAUDE.md:15,72,73` "pronunciation assessment via **Azure Speech SDK**" | **Whisper sidecar :9002 + Ollama rubric**; `grep com.microsoft.cognitiveservices src/main` = **0** | `SpeakingAssessmentService` |
| `CLAUDE.md:70` "backend **`@PremiumRequired`** annotation" là cơ chế gating | Annotation **tồn tại nhưng 0 usage**; gating là `hasPremiumAccess()` gọi tay | `grep PremiumRequired src/main` = 1 (chỉ file định nghĩa) |
| `CLAUDE.md:69` "**refresh via login**" | **Không có endpoint refresh**; `AuthController` 0 hit `refresh` | `grep refresh AuthController` = 0 |

### Gốc rễ

Tài liệu viết ở thời điểm khác với code hiện tại và **không được cập nhật** khi số test tăng, khi Azure bị thay bằng
Whisper+Ollama, và khi cơ chế premium đổi. Đây là **drift**, không phải lỗi chức năng.

### Fix

Sửa tài liệu cho khớp thực tế đo được (safe fix — không đổi hành vi sản phẩm):
- README: số test → 485 backend / 119·23 frontend.
- CLAUDE.md: Azure → Whisper+Ollama; `@PremiumRequired` → mô tả đúng (`hasPremiumAccess`); bỏ "refresh via login"
  (hoặc ghi rõ là *planned*, không phải *exists*).

### Đo lại

Đã sửa và kiểm lại bằng grep — mọi dòng drift đã hết:

```
grep "Azure" README.md CLAUDE.md              -> 0 hit
grep "223 tests\|73 tests" README.md          -> 0 hit (nay 485 / 119·23)
grep "refresh via login" CLAUDE.md            -> 0 hit
grep "Azure Speech SDK" CLAUDE.md             -> 0 hit
```

**Phân loại:** safe fix (sửa tài liệu theo số đo, không đổi hành vi sản phẩm). Không test nào cần thêm vì đây là
văn bản, không phải code. Số trong README/CLAUDE.md nay khớp `.p0-v12-backend.log` (485) và `.p0-v12-frontend.log`
(119/23).

---

## F147 — `POST /api/vocabulary` cho mọi user đã đăng nhập ghi vào bảng từ vựng **GLOBAL**

**Mức:** MEDIUM (bề mặt lạm dụng/ô nhiễm dữ liệu) · **Trạng thái:** **FIXED** (Phase 9 — deck-scoped + atomic)
**Nguồn:** C1 trong kế hoạch

### Đo được (probe 3 role, live)

```
POST /api/vocabulary (STUDENT)         -> 200, tạo row id=50328
GET  /api/vocabulary/search?keyword=…  -> row đó HIỆN VỚI ẨN DANH  (visibleToAnon = true)
DELETE /api/vocabulary/{id} (STUDENT)  -> 403 (admin-only)
```

Cấu hình thực tế (`config/SecurityConfig.java:101-103`):

```
POST   /api/vocabulary   -> .authenticated()      // BẤT KỲ user đã đăng nhập
PUT    /api/vocabulary/** -> .hasRole('ADMIN')
DELETE /api/vocabulary/** -> .hasRole('ADMIN')
```

`VocabularyController.create` chỉ null-check `Authentication` rồi `vocabularyRepository.save(...)` một
`Vocabulary` **không có chủ sở hữu, không có lesson** (bảng `vocabulary` không có cột owner).

### Vì sao đáng lo

- Caller duy nhất ở frontend là `frontend/src/views/videos/VideoLesson.vue` `saveWordToDeck()` — **bất kỳ student**
  lưu một từ khi xem video ⇒ ghi vào bảng **dùng chung toàn hệ thống**.
- Row đó **hiện với mọi người** qua `/api/vocabulary/search` (permitAll) và `/api/vocabulary`.
- Student **không thể** xoá/sửa (PUT/DELETE admin-only) ⇒ nếu ghi sai, họ **không tự sửa được**.
- Đây là **bất đối xứng quyền**: tạo thì mở, sửa/xoá thì đóng.

### Vì sao KHÔNG sửa ngay trong vòng audit đầu

Đổi authorization của một API **đã ship** là **quyết định sản phẩm**, không phải fix kỹ thuật. Hai hướng fix
**ngược nhau**, nên v12 dừng ở mức nêu finding + đo blast-radius, chờ owner.

### Bản chất thật (đo ở Phase 9) — KHÔNG phải lỗi schema

`vocabulary` là **từ điển chung**, không phải kho từ cá nhân: **không có cột `owner`/`user_id` nào**. Quyền sở hữu
nằm ở `decks.owner_id` + `deck_words`. Đo live: 90/127 row là từ điển curated, 31 AI_GENERATED, **1** có
`lesson_id`, **0** thuộc deck do user sở hữu. ⇒ Lỗi là **đường ghi của student bỏ qua tầng sở hữu**, không phải
thiếu cột.

Triệu chứng người dùng thấy được: `GET /api/vocabulary/search?keyword=negotiate` trả **2 kết quả trùng**.

### Fix (Phase 9) — deck-scoped + atomic

`VocabularyService.createScoped` (mới): non-admin **bắt buộc** `deckId`; **dedupe** (dùng lại row cùng `word` chưa
gắn lesson); gọi `DeckService.addWordToDeck` **trong cùng `@Transactional`**; resolve `lessonId` (trước bị bỏ qua).
`VideoLesson.vue` gọi **1 call** thay vì 2. Thêm enum `DeckSource.VIDEO_LESSON`.

### Đo lại (E2E trên container)

| Probe | Kết quả |
|---|---|
| Student không deckId | **400** ✓ |
| Student có deckId của mình | **200**; vocab row **và** `deck_words` link cùng transaction ✓ |
| Student gắn deck **người khác** (IDOR) | **400** ✓ **và vocab row rollback** (0 leak) ✓ |
| Lưu cùng từ 2 lần | **1** row, link **2** deck (dedupe) ✓ |

Chi tiết: `evidence/phase-9-open-items.md`.

---

## F148 — Hai thuật toán SRS độc lập ghi cùng bảng (ban đầu báo nhầm là "API mồ côi")

**Mức:** MEDIUM (nâng từ LOW — 2 thuật toán SRS ghi cùng bảng) · **Trạng thái:** **FIXED** (Phase 9 — hợp nhất về SM-2)
**Nguồn:** C8 trong kế hoạch; **mức độ thật lộ ra ở Phase 9**

### Đo được (ban đầu — mới chỉ thấy phần nổi)

```
grep -rn "api/srs" frontend/src   -> 0 hit
SrsController: /api/srs/review, /api/srs/due/{deckId}, /api/srs/stats   (cả 3 đều 200 khi probe API)
```

### Sự thật sâu hơn (Phase 9): HAI thuật toán, MỘT bảng

`SrsController` là **caller DUY NHẤT** của `SrsService`. UI thật (`FlashcardGame.vue`) gọi `FlashcardService`, mà
service này **không hề gọi** `SrsService` (dòng 68 chỉ là **comment** nói "giống"). Hai thuật toán:

| | `SrsService` (SM-2) | `FlashcardService` (cũ) |
|---|---|---|
| Ghi `ease_factor`/`repetitions`/`srs_interval` | có | **KHÔNG** |
| Cap interval | 365 ngày (F106) | 14 ngày |
| Ai gọi | chỉ `SrsController` (0 caller UI) | `FlashcardGame.vue` (**UI thật**) |

Bằng chứng dữ liệu: **11/14 row `ease_factor` kẹt ở 2.5**; **2 row `review_count > 2` nhưng `repetitions = 0`**.

Lỗi UI kèm theo: `markWord()` gộp `rating !== 'again'` ⇒ **"Dễ" và "Tiếp theo" gửi request y hệt nhau**; và
**"Lại" không đưa từ quay lại** trong phiên.

### Fix (Phase 9)

`FlashcardReviewRequest.isKnown` → `quality` (0–5); `FlashcardService` uỷ quyền `SrsService.reviewWord`; xoá bảng
1/3/7/14; `FlashcardGame.vue` map `again→1, good→4, easy→5` + **requeue cho "Lại"**.

### Đo lại (E2E)

| Probe | Trước | Sau |
|---|---|---|
| `{quality:5}` trên vocab 10018 | — | `ease 2.5→**2.6**`, `reps 1→**2**`, `interval 1→**6**` ✓ |
| `{quality:1}` ("Lại") | — | `reps→**0**`, `interval→**1**`, `ease→**2.06**` ✓ |
| `{quality:9}` | — | **400** ✓ |
| **"Dễ"(5) vs "Tiếp theo"(4)** | **giống hệt** | `ease +0.1` vs `+0.0` — **khác nhau thật** ✓ |

14 row cũ **không cần migrate** (giá trị khởi tạo hợp lệ, tự lành ở lần review kế tiếp). `/api/srs/*` **giữ lại**
(`SrsService` nay là nguồn sự thật duy nhất) — vẫn chưa có UI, ghi rõ. Chi tiết: `evidence/phase-9-open-items.md`.

---

## F149 — `/admin/lessons` chữ "Trống" trượt WCAG AA (2.65:1) do opacity `/60`

**Mức:** MEDIUM (a11y, admin-only) · **Trạng thái:** **FIXED**
**Nguồn:** probe contrast Phase 3 (composite alpha bottom-up — probe mà v11 phải viết lại để bắt F138)

### Đo được (live, production + dev)

```
/admin/lessons  <span class="text-muted-foreground/60 italic">Trống</span>  (2 chỗ)
computed color : rgba(85, 96, 112, 0.6)
composited     : rgb(153, 160, 169)  trên nền trắng
ratio          : 2.65 : 1     (cần 4.5:1 cho 14px weight 400)   -> FAIL
```

### Gốc rễ

Cùng lớp lỗi **F138** (v11): dùng **opacity** trên token chữ. `--geo-muted-fg` (#556070) đạt 6.38:1, nhưng
`/60` pha loãng nó xuống #99A0A9 = 2.65:1. Opacity là **pha màu với nền**, không phải "nhạt hơn một chút".

### Fix

Thêm token chuyên dụng **`placeholder`** = `#6B7280` (**4.83:1** trên trắng — tính bằng công thức WCAG trước khi
chọn, không đoán), dùng `text-placeholder` thay `text-muted-foreground/60`. Theo đúng pattern `*-ink`/`*-strong`
mà v11 đã lập.

- `frontend/tailwind.config.js`: thêm `placeholder: '#6B7280'`
- `frontend/src/views/admin/AdminLessons.vue`: `text-muted-foreground/60` → `text-placeholder`

### Đo lại — cùng probe, cùng trang

```
trước: ratio 2.65  (FAIL)   class text-muted-foreground/60, 2 chỗ
sau  : ratio 4.83  (PASS)   class text-placeholder,        2 chỗ, 0 class cũ còn lại
```

Regression test: `frontend/src/views/admin/AdminLessons.placeholder-contrast.test.js` (2 test) — chặn cả việc
quay lại class `/60` **và** việc hạ hex của token xuống dưới AA. **Pass 2/2.**

> **Một bẫy đã gặp:** lần đo "sau fix" đầu tiên trên **dev server** trả về `#556070` (6.38:1) — trông như pass
> nhưng thực ra là **kế thừa** từ parent, vì Vite dev server đang giữ **Tailwind config cũ**. Chỉ sau khi
> `docker restart engflow-frontend` thì class mới resolve đúng `#6B7280`. Nếu tin lần đo đầu, tôi đã báo "pass"
> nhầm mà không có fix thật.

---

## F150 — CLS 0.104 trên `/` do footer bị đẩy khỏi viewport sau first paint

**Mức:** LOW (performance, một route) · **Trạng thái:** **FIXED** (Phase 9 — `#main-content{min-height:100vh}`; CLS 0.104 → 0.001)
**Nguồn:** Lighthouse (chrome-devtools MCP) — audit `cumulative-layout-shift` = 0.103

### Đo được (Playwright, PerformanceObserver + attribution, production build)

```
clsTotal = 0.104    (tái lập 4 lần: 0.104 / 0.111 / 0.104 / 0.104)
1 entry chi phối:  t=185ms  FOOTER  prev=[y=827, h=96]  cur=[y=0, h=0]  v=0.10401
```

### Gốc rễ (đo, không suy đoán)

- `MutationObserver`: `#app` rỗng ở t=40ms → footer **ADDED** ở t=66ms.
- Lúc settle: footer `y=3421`, `main` `h=3351`, shell `min-h-screen` = 867px.
- Home là route **lazy** (`() => import('@/views/Home.vue')`).

⇒ Shell `min-h-screen` cho footer **first paint ở y≈827 = đáy viewport (đang nhìn thấy)**. Khi chunk Home về,
`<main>` giãn lên 3351px → footer bị đẩy xuống dưới fold. Một khối 96px **đang thấy** rời khỏi màn hình = 0.104 CLS.

### Lần thử đầu dùng SAI GIÁ TRỊ (và đã revert)

| Phương án | Đo | Kết luận |
|---|---|---|
| `#main-content{min-height:calc(100vh - 64px - 96px)}` | 0.104 → **0.098** | **Sai giá trị**: 707px → footer ở `64+707=771` < 867 ⇒ **vẫn trong màn hình**. Đã revert |

### Fix (Phase 9) — giá trị ĐÚNG

```css
#main-content { min-height: 100vh; }   /* footer first-paint ở 64 + 867 = 931 > 867 = ngoài màn hình */
```
Ghi ở `frontend/src/assets/app-layout.css`, kèm comment nêu cả số của lần thử sai để không lặp lại.

### Đo lại — Playwright `PerformanceObserver` trên **production build**, 5 lần

```
trước:  0.104 / 0.111 / 0.104 / 0.104
sau:    0.0001 / 0.00008 / 0.0001 / 0.00005 / 0.00013   (footer shift BIẾN MẤT)
```

4 route đều sạch: `/` 0.00013 · `/lessons` 0.00008 · `/login` 0.00012 · `/decks` 0.0001; 0 overflow thật.
**Lighthouse xác nhận độc lập** (chrome-devtools MCP, `/`): `cumulative-layout-shift` **0.103 → 0.001** (score 1);
Accessibility vẫn **100**.

> **Lỗi probe tự bắt trong quá trình này:** 3 biến thể "inject CSS lúc runtime" đều báo CLS y nguyên, nhưng khi
> kiểm chính cơ chế thì `injected: false` — **init script không hề chạy**. Ba kết quả đó vô giá trị. Chỉ tin phép
> đo sau khi sửa **source + rebuild**. (Cùng lớp lỗi P1–P9 của v11.)

---

## F151 — IDOR: `/api/srs/due/{deckId}` rò rỉ nội dung deck private của người khác

**Mức:** MEDIUM (bảo mật — rò rỉ nội dung, không chỉ metadata) · **Trạng thái:** **FIXED** (Phase 10)
**Phát hiện:** khi nghiên cứu Item A (UI "ôn từ đến hạn"), không nằm trong audit gốc.

### Đo live

Deck `30033` (`owner_id=3` = admin, `is_public=0`):

```
student GET /api/decks/30033    -> 400 "Bạn không có quyền truy cập bộ từ vựng này"   (đúng)
student GET /api/srs/due/30033  -> 200 [ {"vocabId":10020,"word":"determine",
                                        "pronunciation":"/dɪˈtɜː.mɪn/","definitionVi":"xác định", …} ]
```

Rò rỉ **nội dung từ vựng thật** (`word`, `pronunciation`, `definitionVi`, `definitionEn`,
`exampleSentence`), không chỉ sự tồn tại của deck.

### Gốc rễ

`SrsService.getDueWords(userId, deckId)` (`SrsService.java:113`) chỉ làm:

```java
List<DeckWord> deckWords = deckWordRepository.findByDeckIdOrderByOrderIndexAsc(deckId);
```

**Không kiểm quyền sở hữu.** `SrsController.getDueWords` cũng chỉ null-check `userPrincipal`.
Trong khi `DeckService.getDeckById(deckId, userId)` **đã có sẵn đúng logic** cần dùng.

### Fix — tái dùng primitive đã có, không viết lại logic quyền

| File | Thay đổi |
|---|---|
| `service/SrsService.java` | `getDueWords` gọi `deckService.getDeckById(deckId, userId)` **trước** khi đọc `deck_words`; thêm `DeckService` vào constructor (không circular — `DeckService` không phụ thuộc `SrsService`) |
| `controller/SrsController.java` | Không đổi (`BadRequestException` → `GlobalExceptionHandler` → 400, khớp `/api/decks/{id}`) |
| `src/test/.../SrsDueWordsAuthzTest.java` **(mới)** | 4 test: deck private người khác → `BadRequestException` + `verifyNoInteractions(deckWordRepository)`; deck của mình → OK; public → OK; không tồn tại → `ResourceNotFoundException` |

### Đo lại — `sweep/v12/f151-idor-probe.js`, **9/9 PASS**

| Probe | Trước | Sau |
|---|---|---|
| `student GET /api/srs/due/30033` | **200** + nội dung | **400** ✓ |
| Nội dung từ vựng trong body | rò rỉ | **không lộ** ✓ |
| `GET /api/srs/due/999999` | **200 + `[]`** | **404** ✓ |
| `GET /api/srs/due/10006` (public) | 200 | **200** ✓ |
| Owner đọc deck private của mình | 200 | **200** ✓ |

**Lợi ích kèm theo:** deck không tồn tại nay **404** (khớp `/api/decks/{id}`) thay vì 200 + rỗng.

---

## F152 — non-admin gắn được vocabulary vào BẤT KỲ lesson nào (đường ghi student dùng chung DTO với admin)

**Mức:** MEDIUM (toàn vẹn nội dung + ô nhiễm dữ liệu) · **Trạng thái:** **FIXED** (Phase 11)
**Phát hiện:** bởi `/review-agent` trên chính các commit Phase 9/10, không nằm trong audit gốc.

### Đo live (probe đã chạy và đã dọn)

```
student POST /api/vocabulary?deckId=<deck của mình> {word:…, lessonId:41881}  -> 200
anon    GET  /api/lessons/41881  -> 200; vocabularies=1; từ bị inject CÓ mặt = true
```

### Gốc rễ

**Đường ghi student dùng CHUNG DTO với đường admin, và truyền thẳng một field chỉ admin được đặt.**
`lessonId` là khái niệm admin-only; `VocabularyRequest` được share giữa
`AdminService.createVocabulary` (đường admin, gate ở `SecurityConfig:108`) và `VocabularyService.createScoped`
(đường student), nên field lọt qua. `Lesson` **không có cột owner** ⇒ câu hỏi duy nhất là "admin hay không".

**Đây là hành vi MỚI, không phải lỗi cũ:** controller trước đây **bỏ qua** `lessonId` hoàn toàn
(`// lesson field handling skipped for simplicity`) nên khả năng này **chưa từng tồn tại**. Commit `4c6f351`
coi việc bỏ qua đó là bug và "sửa" — nhưng **sửa mà không mang theo admin gate**.

Đường rò: `GET /api/lessons/**` là `permitAll` (`SecurityConfig:84`) → `LessonRepository.findByIdWithDetails`
(**`LEFT JOIN FETCH l.vocabularies`**, `:38`) → `LessonResponse.vocabularies` (`LessonService:203,238`).

### Vị trí guard — bản nháp đầu của tôi SAI (defense-in-depth bắt được)

> Guard đặt trong `build()` sẽ **bị bypass**: `build()` chỉ chạy ở nhánh `orElseGet` của dedupe
> (`findReusable(request).orElseGet(() -> save(build(request)))`). Từ đã tồn tại (đường tấn công lặp lại
> dễ nhất) ⇒ `build()` không chạy ⇒ guard không chạy.

### Fix

Guard ở **đầu `createScoped`**, cạnh guard `deckId` sẵn có — chạy **TRƯỚC** dedupe nên mọi nhánh đều qua.
Dùng `BadRequestException` (400) cho nhất quán với `DeckService` (`:103,:127,:142,:151`) và guard `deckId`
cùng file; codebase **không có** `ForbiddenException`. Kèm `log.warn` để truy vết.

| File | Thay đổi |
|---|---|
| `service/VocabularyService.java` | Guard `lessonId != null && !isAdmin` ở đầu `createScoped` + `log.warn`; ghi INVARIANT ở `build()` |
| `VocabularyServiceTest.java` | +1 test: non-admin + `lessonId` ⇒ 400 **và** `verifyNoInteractions(vocabularyRepository, lessonRepository, deckService)` — assertion thứ hai **khoá vị trí guard** (nếu chuyển vào `build()`, `findReusable` chạy trước ⇒ test đỏ) |
| `sweep/v12/f152-lesson-inject-probe.js` **(mới)** | Probe 5 ca, **9/9 PASS** |
| `sweep/v12/api-sweep.js` | +4 assert trong block F147 (dùng lại deck đã cấp phát) |

### Đo lại — `f152-lesson-inject-probe.js`, ALL PASS

| # | Ca | Trước | Sau |
|---|---|---|---|
| V1 | student + `lessonId` (từ mới) | **200** | **400** ✓ |
| V2 | student + `lessonId` (**từ đã có** — nhánh dedupe) | **200** | **400** ✓ ← ca bản nháp đầu của tôi làm hở |
| V3a | student **không** `lessonId` | 200 | **200** ✓ (không khoá nhầm) |
| V3b | admin + `lessonId` (`/api/admin/vocabulary`) | 200 | **200** ✓ (không khoá nhầm) |
| V4 | `GET /api/lessons/{id}` ẩn danh | từ bị inject **có mặt** | **không có** ✓ |

### Đã loại trừ: `save-vocab` KHÔNG phải đường thứ hai

`AiVocabService.saveVocabBatch` hardcode `.source("AI_GENERATED")` và **không bao giờ** set `.lesson(...)`
(đọc code xác nhận, không suy đoán từ việc "cùng DTO").

---

## F153 — Flashcard mất streak khi bỏ quality (phát hiện khi thiết kế lại, đã xử lý)

**Mức:** MEDIUM (hồi quy chức năng) · **Trạng thái:** **FIXED** (Phase 11)

Người dùng yêu cầu rút flashcard còn **2 nút Back/Continue** và cho rằng "chơi 1 trò bất kì là tính streak".
Kiểm chứng: **đúng một phần**.

- **Đúng:** game (quiz/memory/typing/listening/mixed) tính streak độc lập với SRS —
  `GameController.submitGameResult → GameService:258 → streakService.checkin → recordStudy`.
- **SAI:** **flashcard KHÔNG đi qua `GameController`.** Đường ghi ngày học **duy nhất** của flashcard là
  `POST /api/flashcards/review → FlashcardService:48 → SrsService.reviewWord:115 → recordStudy`.
  Bỏ quality mà không thêm đường mới ⇒ flashcard **mất streak**.

**Fix:** thêm `POST /api/flashcards/study` (`FlashcardService.recordStudyDay`, `@Transactional` vì
`recordStudy` là `Propagation.MANDATORY`) — gọi **1 lần/phiên** ở lần "Tiếp theo" đầu tiên (người học có thể
thoát giữa chừng; ghi lúc *hoàn thành* sẽ mất streak cho phiên dở dang).

**Ghi chú:** defect "requeue không reset" (từng định đặt mã F153) **bị thay thế bởi thiết kế này** — cơ chế
requeue bị gỡ hoàn toàn nên bug đó không còn tồn tại.

---

## Lỗi của PROBE (không phải finding — ghi để không ai "sửa" code đúng)

| # | Probe đầu báo | Sự thật | Verdict |
|---|---|---|---|
| V1 | "login sai mk trả 400" | `"nope"` 4 ký tự vi phạm `@Size(min=6)` → 400 hợp lệ | probe bug |
| V2 | "flashcards/review 404" | `vocab_id 10006` không tồn tại (MIN=10017) | probe bug |
| V3 | "srs/review 400" | key đúng là `vocabId`, không phải `vocabularyId` | probe bug |
| V4 | "webhook chữ ký sai trả 200 = hở" | SePay ACK 200 + `{"success":false}` trong body | probe bug |
| V5 | DB audit: 5 × `Msg 207 vocabulary_id` | `deck_words` dùng `vocab_id` | probe bug |
| V6 | DB audit: `Msg 245` int vs varchar | `UNION ALL` trộn kiểu | probe bug |
| V7 | constraint test: `Msg 1934` | filtered index cần `QUOTED_IDENTIFIER ON` | probe bug |
| V8 | "backend container stale" | So sai múi giờ (mtime local vs container UTC); epoch cho thấy **không stale** | probe bug |
| V9 | "7 tap-target vi phạm WCAG 2.5.8" | Cả 7 đều **EXEMPT** theo ngoại lệ **Inline**/**Spacing** của 2.5.8 (đo khoảng cách tâm target) | probe bug |
| V10 | "fix F150 không hiệu quả" (3 lần) | `addInitScript` **không hề chạy** (`injected: false`) → đo trên build chưa đổi | probe bug |
| V11 | "KB4 streak sai (streak=2, không phải 1)" | Scenario chèn "hôm qua" = **2026-09-20**, nay **bằng** cutover nên đúng là ngày học. Đo lại với 2026-09-19 (thật sự trước cutover) → streak 1, đúng | probe bug (drift theo lịch) |
| V12 | Sau cleanup Item B: "F147 deck-scoped save **BLOCKED** — student has no deck" | **Không phải lỗi sản phẩm**: chính tôi đã xoá 3 deck "Test Deck" **do user student sở hữu** mà sweep ngầm dựa vào; sweep còn hardcode `deckId=30033` (cũng vừa xoá) nên assert IDOR **lặng lẽ ngừng chạy**. Sửa **harness**: sweep tự tạo `AUDIT-V12-API-F147` (student) + `AUDIT-V12-API-FOREIGN` (admin) → **137 pass / 0 fail** | probe bug (phụ thuộc dữ liệu ambient) |

Cả 12 đều bị **probe thứ hai** giết trước khi thành finding.
