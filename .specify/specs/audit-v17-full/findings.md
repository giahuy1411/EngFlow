# audit-v17-full — findings

Quy ước: `F-17-NN`. Mỗi finding: mô tả · bằng chứng · root cause · fix · bằng chứng pass · test hồi quy ·
**probe thứ 2 độc lập**. Trạng thái: `FIXED` · `OPEN` · `BLOCKED` · `DEFERRED` · `CLOSED (probe SAI)` · `N-A`.

---

## F-17-01 — Toast "thành công" dùng `bg-accent text-white` = **4.23:1 (FAIL AA)** — **LOW (a11y)** — `FIXED`

**Bằng chứng:** `frontend/src/composables/useToast.js:34` `case 'success': return 'bg-accent text-white'`.
Đo WCAG: `#FFFFFF` trên `#8B5CF6` = **4.23:1** — dưới ngưỡng **4.5:1** của WCAG 1.4.3 cho body text
(Hiến pháp P7 bắt buộc AA). Đây là **đường duy nhất còn sót** không đi qua lớp `*-ink`/`*-strong` mà audit-v11 F132
đã áp cho mọi call site khác.

**Root cause:** toast render inline trong `App.vue:20-37` qua `toastBackground()`, không dùng token AA.

**Fix:** `bg-accent text-white` → `bg-accent-strong text-white` (`#7C3AED` = **5.70:1**, cùng hệ tím).

**Bằng chứng pass (probe 2 độc lập):**
- Đo lại công thức WCAG: `#FFFFFF` trên `#7C3AED` = 5.70 ≥ 4.5 ✅.
- `frontend/src/__tests__/audit-v17-regressions.test.js` — assert `toastBackground('success')` **không** chứa
  `bg-accent` và **có** `bg-accent-strong` (3/3 pass).

**Test hồi quy:** `audit-v17-regressions.test.js` (F-17-01 block).

---

## F-17-02 — Guard "< 2 ký tự" chỉ có ở backend; UI vẫn gọi tra từ 1 ký tự (~20 s) — **LOW (UX + docs)** — `FIXED`

**Bằng chứng (đo thật, chrome-devtools MCP):** ở `/search`, gõ `h` rồi bấm "Tra từ" →
Network **có** `GET /api/vocabulary/dictionary/h` (đã bắt ở `reqid=475`). Doc §4.2 nói *"gõ dưới 2 ký tự thì không
tìm"*, nhưng guard `<2` chỉ ở backend `/api/vocabulary/search` (`VocabularyController:46`) — **không** ở frontend,
**không** ở `/dictionary/{word}` (`VocabularyController:61`). Tra từ điển ngoài đo **~20 s** khi cache lạnh
(F-17-05) → 1 ký tự vẫn tốn request thật.

**Root cause:** `SearchVocabulary.vue:115 search()` chỉ guard `!query.value.trim()` (rỗng), không guard độ dài;
view dùng đường `/dictionary/` (không có guard backend).

**Fix:** thêm guard `term.length < 2` trong `search()` — xoá kết quả, không gọi API (đúng hành vi doc mô tả).

**Bằng chứng pass (probe 2):**
- Sau fix, gõ `h` + "Tra từ" → Network **chỉ có `GET /api/auth/me`**, **không** có `/dictionary/h` ✅.
- `audit-v17-regressions.test.js` assert source có `term.length < 2`.

**Test hồi quy:** `audit-v17-regressions.test.js` (F-17-02 block).

---

## F-17-03 — Nút UI là "Kiểm tra"/"Nộp bài", doc ghi "Chấm thử" — **LOW (docs)** — `OPEN`

**Bằng chứng:** UI thật (MCP) hiện nút **"Kiểm tra"** (per-question) và **"Nộp bài (6 câu)"**;
`grep "Chấm thử" frontend/src` = **0 hit**. Nhãn trong `LessonExerciseTab.vue:108,116` là `Kiểm tra` / `Nộp bài`.
Demo doc §2.2 dùng chữ **"Chấm thử"** và §Phụ lục A cũng vậy.

**Fix (Phase 8):** sửa demo doc dùng đúng nhãn UI ("Kiểm tra" thay "Chấm thử"), tránh hội đồng bấm nhầm.

---

## F-17-04 — Doc nói "không có trường `correctAnswer`" — thực tế **có nhưng null** — **LOW (docs)** — `OPEN`

**Bằng chứng:** response `GET /api/lessons/445/exercises` (student) chứa key `correctAnswer` trên **mọi** exercise,
giá trị **`null`**. An ninh **ĐÚNG** (không lộ giá trị đáp án), nhưng câu chữ doc sai.
(`LessonExerciseController:28-48` chặn `includeAnswers=true` cho non-admin → 403.)

**Fix (Phase 8):** sửa doc: *"trường `correctAnswer` có mặt nhưng luôn `null` với học viên"*.

---

## F-17-05 — Tra từ điển ngoài chậm ~20 s khi cache lạnh — **MEDIUM (UX)** — `OPEN` (đặc tính)

**Bằng chứng (đo 2 vòng):**
- Vòng 1: `/api/vocabulary/dictionary/{world,book,present,test}` trả `[]`; `hello` trả 2007 B.
- Vòng 2: `world/book/test` → **200 sau ~19.5 s**; host → `dictionaryapi.dev` = **200 sau 20.7 s**.
- Local fallback (`vocabulary`) chỉ **118 từ**, **không** chứa các từ demo (hello/world/book/…).

**Root cause:** upstream `dictionaryapi.dev` ~20 s từ mạng VN (đúng như comment `DictionaryService:12`);
`@Cacheable` 1h cứu các lần sau, nhưng lần đầu mỗi từ vẫn ~20 s. App **degrade đúng** (`[]` → DB local → "Không tìm thấy từ").

**Đề xuất:** giữ nguyên code (không phải bug), nhưng (a) sửa doc không hứa `hello` tức thì; (b) cân nhắc warm cache
trước demo. **Chưa sửa code** — không có win rõ, và P5 cấm "tối ưu" khi chưa đo được cải thiện.

---

## F-17-06 — `ui-sweep` đếm `[warn]` YouTube iframe thành "console error" — **LOW (harness)** — `OPEN`

**Bằng chứng:** `ui-sweep` TOTALS `consoleErrors: 1`, tại dòng `student /videos/1 … err=1`.
MCP xác nhận message là `[warn] Failed to execute 'postMessage' … youtube.com` — **third-party**, không phải lỗi app.

**Đề xuất:** harness nên phân biệt `console.error` vs `console.warn`, hoặc whitelist origin YouTube — tránh
báo động giả. Không sửa app (đúng hành vi).

---

## F-17-07 — `exercise_attempts` không nằm trong cleanup/parity (residue lớp F-16-01) — **LOW (harness)** — `OPEN`

**Bằng chứng:** `grep exercise_attempts sweep/**` = **0**; harness nộp bài (MCP walkthrough, và có thể probe khác)
tạo row thật. Phiên này: U8 tạo `attempt_id=70147` (đã **dọn**); còn `70144` (lesson 447, 14:07 — **trước** phiên
này, 21:40) = residue cũ. Tổng `exercise_attempts` = 37 (v16 ghi 28 — tăng 9, chưa giải thích được hết).

**Đề xuất:** thêm `exercise_attempts` vào `assertClean` + cleanup của harness (cửa sổ half-open, chỉ 2 tài khoản probe).
**Chưa sửa** vì cần xác định chắc chắn probe nào tạo để không xoá oan (R4).

---

## Bác bỏ (CLOSED — probe SAI)

### F-17-08 — "`vocabularyService` gọi dictionaryapi.dev **direct trước**" → **SAI**
Claim từ Plan-agent. Đọc `vocabularyService.js:152-153`: **`backendFallback()` được thử TRƯỚC**, direct là cuối
(`:155-160`). → **CLOSED (probe SAI)**. Ghi lại để không lặp (đúng kỷ luật: reviewer cũng có thể sai).

---

## F-17-09 — Fix F-17-03 chưa trọn: còn fallback `#64748B` (giá trị đã bị thay) — **LOW (a11y)** — `FIXED` *(review chéo bắt được)*

**Nguồn:** review chéo đối kháng (subagent) — không phải suy đoán.

**Bằng chứng:** `frontend/src/utils/lessonLevels.js:20,30` — cả `levelColor()` và `levelInkColor()` rơi về
`'var(--geo-muted-fg, #64748B)'`. `#64748B` là giá trị audit-v11 **F138 đã thay** bằng `#556070` **vì lý do
contrast** (4.34:1 trên panel muted < 4.5:1). Nó là **màu chữ** ở `Lessons.vue:106`
(`:style="{ color: levelInkColor(lesson.level) }"`). Cùng lớp lỗi với F-17-03 (fallback mang giá trị đã gỡ).

**Root cause:** fix F-17-03 chỉ quét `Lessons.vue`, bỏ sót file util cùng vai trò.

**Fix:** cả 2 fallback → `#556070` (giá trị token thật).

**Probe 2 (độc lập):** `grep -rniE "#(E5DECF|F5F0E6|EAE4D6|64748B)" frontend/src` sau fix → chỉ còn **trong comment**
(`design-system.css` 3 dòng ghi chú lịch sử, `Pagination.vue` 1 comment). Test mới assert file không chứa giá trị gỡ.

---

## F-17-10 — Test hồi quy F-17-02 **false-pass** (regex khớp cả guard bị comment-out) — **LOW (test)** — `FIXED` *(review chéo bắt được)*

**Nguồn:** review chéo đối kháng.

**Bằng chứng:** assertion cũ `expect(src).toMatch(/term\.length\s*<\s*2/)` — chạy thật:
```
current code        : true
commented-out guard : true   ← "// if (term.length < 2) {"
no-op guard         : true   ← "if (term.length < 2) { void 0; }"
```
→ Test **pass dù fix bị revert** (không bảo vệ gì).

**Fix:** viết lại **behavioral** — mount `SearchVocabulary.vue`, click "Tra từ", assert
`vocabularyService.search` **không** được gọi với 1 ký tự và **được** gọi với 2 ký tự.

**Probe 2 (mutation-test):** tắt guard (`if (false) {`) → test **FAIL** (`1 failed | 4 passed`); khôi phục → `5 passed`.
→ Test thực sự fail khi fix bị gỡ.

---

## F-17-11 — `POST /api/ai/generate-vocab` trả **500** khi model sinh newline thô trong JSON — **MEDIUM** — `FIXED` *(vòng 2 bắt được)*

**Nguồn:** `api-sweep` **vòng 2** — `ai pass=4 fail=1`: `POST /api/ai/generate-vocab (auth) 200 or quota 429`
→ **got 500**. (Vòng 1 pass → đây là lỗi **liên tục ngắt quãng**, không phải regression của fix vòng này.)

**Bằng chứng:**
- Backend log: `AiVocabService : Failed to parse AI response: Illegal unquoted character ((CTRL-CHAR, code 10)):
  has to be escaped using backslash to be included in string value` → `java.lang.RuntimeException: Failed to
  generate vocabulary`.
- `AiVocabService.java:70` parse **một lớp** `objectMapper.readValue(...)` trong `try/catch` → ném RuntimeException
  → `GlobalExceptionHandler` map 500.
- So sánh: `AiExerciseService` có **salvage 3 lớp** (`:447` "Parse LLM response (3-layer salvage)"). Vocab thì không.

**Root cause:** model local `qwen2.5:1.5b` thỉnh thoảng phát **ký tự điều khiển thô** (newline `\n`) **bên trong**
chuỗi JSON (ví dụ `exampleSentence` nhiều dòng). JSON nghiêm cấm điều này → parse cứng fail → 500. Ngắt quãng
(3 lần gọi lại sau đó 200) nên retry không đủ; phải sửa ở tầng parse.

**Fix:** thêm `readVocabListLenient` / `readVocabMapLenient` — thử **strict trước**, nếu `JsonProcessingException`
thì parse lại bằng mapper **lenient** bật `ALLOW_UNESCAPED_CONTROL_CHARS`. Dùng cho cả `generateVocabByTopic`
và `enrichWord`.

**Bằng chứng pass (probe 2 độc lập):**
- `AiVocabServiceLenientParseTest` **5/5** — có ca tái hiện **đúng** lỗi (`"I plan to travel\nto Europe."` thô)
  và ca "JSON hỏng thật vẫn phải ném" (không nuốt lỗi).
- Sau rebuild: gọi live 4 lần → **HTTP 200** (3–4 s). 500 không còn.

**Test hồi quy:** `src/test/java/com/datn/engflow/service/AiVocabServiceLenientParseTest.java`.

---

## F-17-12 — Timeout AI vocab **hardcode 30 s** (các đường AI khác đều cấu hình được) — **MEDIUM** — `FIXED` *(cùng lớp F-17-11)*

**Nguồn:** khi verify fix F-17-11 sau rebuild, endpoint trả **504** liên tục; đào tiếp thì lộ nguyên nhân thứ hai.

**Bằng chứng:** `AiVocabService.java:203` `.timeout(Duration.ofSeconds(30))` — hardcode. Log:
`TimeoutException: Did not observe any item or terminal signal within 30000ms in 'flatMap'`. Trong khi
`application.properties` có `ai.speaking.llm.timeout-seconds=120`, `speaking.assessment.timeout-seconds=120`
(các đường AI khác **đều** cấu hình + rộng hơn). Model swap khi lạnh đo được >30 s.

**Root cause:** 30 s hardcode quá ngắn cho lần gọi nguội.

**Fix:** thêm `ai.vocab.timeout-seconds=${AI_VOCAB_TIMEOUT_SECONDS:120}` vào `application.properties`; inject vào
constructor; thay hardcode.

**Bằng chứng pass (probe 2):** sau rebuild + warm model, gọi live 4 lần → **HTTP 200 (3–4 s)**; không còn 504.

---

| ID | Mức | Vấn đề | Trạng thái |
|---|---|---|---|
| F-17-01 | LOW (a11y) | Toast `bg-accent text-white` = 4.23:1 | `FIXED` |
| F-17-02 | LOW (UX/docs) | Guard `<2` chỉ backend; UI gọi tra 1 ký tự | `FIXED` |
| F-17-03 | LOW (docs) | Nút "Kiểm tra" vs doc "Chấm thử" | `OPEN` (fix doc) |
| F-17-04 | LOW (docs) | `correctAnswer` có mặt nhưng null | `OPEN` (fix doc) |
| F-17-05 | MED (UX) | Từ điển ngoài ~20 s cache lạnh | `OPEN` (đặc tính) |
| F-17-06 | LOW (harness) | ui-sweep đếm YouTube warn thành error | `OPEN` |
| F-17-07 | LOW (harness) | `exercise_attempts` không được dọn/parity | `OPEN` |
| F-17-08 | — | Claim tier-order SAI | `CLOSED (probe SAI)` |
| F-17-09 | LOW (a11y) | Fallback `#64748B` còn sót (review bắt) | `FIXED` |
| F-17-10 | LOW (test) | Test F-17-02 false-pass (review bắt) | `FIXED` |
| **F-17-11** | **MED** | **AI vocab 500 khi model sinh newline thô** | `FIXED` |
| **F-17-12** | **MED** | **AI vocab timeout hardcode 30 s** | `FIXED` |

---

## F-17-13 — `ui-sweep.js` đặt tên ảnh cứng `v13-*` (drift namespace) — **LOW (harness)** — `OPEN`

**Bằng chứng:** `sweep/harness/ui-sweep.js:340-344` hardcode `name: "v13-home-1440"`, `"v13-lessons-360"`,
`"v13-admin-dashboard-1440"`, `"v13-profile-1280"`, `"v13-premium-768"`, `"v13-lessons-1920"`. Chạy với
`--audit audit-v17-full` vẫn sinh `evidence/shots/v13-*.png` (6 file, 21:50 phiên này) — tên **không** theo namespace.

**Root cause:** đúng lớp lỗi mà `_config.js` ra đời để chặn ("hardcoded audit namespace per round") — sót ở đây vì
là **tên file**, không phải đường dẫn/marker nên `assert-harness.js` không bắt.

**Ảnh hưởng:** chỉ là tên file (evidence vẫn hợp lệ, nội dung đúng); gây nhầm "ảnh v13 nằm trong v17".

**Đề xuất:** đổi sang `AUDIT`-derived prefix (như `MARKER` trong `_config.js`). **Chưa sửa** — ngoài phạm vi "fix lỗi
thật của app"; ghi để vòng sau.
