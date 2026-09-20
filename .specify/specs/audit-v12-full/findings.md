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

**Mức:** MEDIUM (bề mặt lạm dụng/ô nhiễm dữ liệu) · **Trạng thái:** **OPEN — chờ quyết định của owner**
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

### Vì sao KHÔNG tự sửa

Đổi authorization của một API **đã ship** là **quyết định sản phẩm**, không phải fix kỹ thuật: nếu bảng `vocabulary`
là **từ điển chung** (mọi người đóng góp) thì hành vi hiện tại là *chủ ý*, chỉ thiếu rate-limit/kiểm duyệt; nếu nó
là **kho từ của từng người** thì `PUT/DELETE` admin-only mới là chỗ sai, và `POST` phải chuyển sang deck-scoped
(như F145 đã làm cho `save-vocab`). Hai hướng fix **ngược nhau**.

### Đề xuất (chờ owner chọn)

- **Hướng A (từ điển chung):** giữ `POST` authenticated, nhưng thêm rate-limit + gắn `source`/kiểm duyệt, và cho
  phép chủ sở hữu sửa/xoá row do mình tạo (cần thêm cột owner).
- **Hướng B (kho từ cá nhân):** chuyển `POST /api/vocabulary` sang ghi **deck-scoped** (dùng `DeckService.addWordToDeck`
  đã có ownership check), siết `POST` về ADMIN cho đường tạo từ điển.

---

## F148 — `/api/srs/*` (3 endpoint) không có caller nào ở frontend

**Mức:** LOW (bề mặt mồ côi) · **Trạng thái:** **OPEN — chờ quyết định**
**Nguồn:** C8 trong kế hoạch

### Đo được

```
grep -rn "api/srs" frontend/src   -> 0 hit
SrsController: /api/srs/review, /api/srs/due/{deckId}, /api/srs/stats   (cả 3 đều 200 khi probe API)
```

`SrsService` **có** được dùng trong backend — `FlashcardService:70` gọi `SrsService.reviewWord` trong cùng
transaction. Nên **logic SRS chạy**, chỉ là **qua đường flashcard**, không qua `/api/srs/*`.

### Phân loại

Không phải lỗi (endpoint hoạt động đúng khi gọi trực tiếp), mà là **bề mặt API không có UI**. Cần owner quyết:
giữ làm API nội bộ, bổ sung UI ôn tập theo SRS, hay retire 3 endpoint.

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

**Mức:** LOW (performance, một route) · **Trạng thái:** **OPEN** (đã truy gốc + đo phương án fix, **revert** vì không đủ)
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

### Đã đo phương án fix và **REVERT**

| Phương án | Đo | Kết luận |
|---|---|---|
| `#main-content{min-height:calc(100vh - 64px - 96px)}` | **0.104 → 0.098** | Không đủ (footer vẫn first paint trong viewport). **Đã revert** — không ship non-fix |

Fix thật cần **đổi kiến trúc render** (SSR/prerender, hoặc bỏ lazy Home, hoặc đặt footer sau nội dung trong
`main`) — vượt phạm vi một fix nhỏ, có rủi ro hồi quy. **Ghi OPEN kèm số để owner quyết.**

> **Lỗi probe tự bắt trong quá trình này:** 3 biến thể "inject CSS lúc runtime" đều báo CLS y nguyên, nhưng khi
> kiểm chính cơ chế thì `injected: false` — **init script không hề chạy**. Ba kết quả đó vô giá trị. Chỉ tin phép
> đo sau khi sửa **source + rebuild**. (Cùng lớp lỗi P1–P9 của v11.)

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

Cả 11 đều bị **probe thứ hai** giết trước khi thành finding.
