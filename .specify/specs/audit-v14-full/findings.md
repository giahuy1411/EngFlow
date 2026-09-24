# audit-v14-full — findings

Quy ước: `F-14-NN` (tiếp số v13 kết thúc ở F-13-22). Mỗi finding: mô tả · bằng chứng · root cause ·
fix · bằng chứng pass · test hồi quy · **probe thứ 2 độc lập** (R10).

Trạng thái: `FIXED` · `OPEN` · `BLOCKED` · `DEFERRED` · `CLOSED (probe SAI)` · `N-A`

---

## F-14-01 — Harness coverage sweep gây **MẤT DỮ LIỆU THẬT** (self-inflicted) — **HIGH** — `FIXED`

**Nguồn:** v14 Phase 2, `sweep/v14/coverage-sweep.js` (script mới của tôi).

**Bằng chứng:** 3 probe `DELETE` trả 204 vào ID thật → xoá lesson 445 (+6 exercises cascade),
section 5 (+5 blocks), 2 lesson_submissions. Parity `1470|43735|...|4|...` → `1469|43729|...|2|...`.
Thêm 2 write thật: `lesson_snapshots` id=30030, `study_days` id=30067.

**Root cause:** script quét mọi method, fire DELETE/POST vào entity thật — vi phạm luật an toàn AGENTS.md.

**Fix:** destructive method → ghost id `999999999`; 13 POST mutate → BLOCKED kèm lý do; integrity check
cuối script assert parity **+ `study_days`**.

**Bằng chứng pass:** `coverage-sweep: probed 148, parityAfter=1470|43735|72|118|29|15|4|126|10|5, ok=true`;
dữ liệu khôi phục từ backup (`lesson_445` content 5948, 6 exercises; section_5 5 blocks);
`incident-f14-01-data-loss.md`.

**Probe thứ 2 (R10):** chạy lại coverage sweep lần 2 → parity không đổi (độc lập xác nhận fix).

---

## F-14-02 — AI upstream chết → app trả **500** thay vì **503** — **LOW** — `FIXED`

**Nguồn:** v14 Phase 2 — Ollama tắt lúc đầu.

**Bằng chứng (trước fix):** `POST /api/ai/generate-vocab` + `/enrich-word` → **500**
`{"detail":"Đã xảy ra lỗi hệ thống..."}`. Root cause trong log:
`WebClientRequestException: Connection refused host.docker.internal:11434` → `AiVocabService` bọc thành
`RuntimeException("Failed to generate vocabulary")` → `GlobalExceptionHandler` catch-all → 500.

**Quyết định (người dùng chốt):** **FIX** — upstream không kết nối được là **503**, không phải 500.
(Lưu ý: một test cũ `otherReactiveErrorsStayOn500Not504` từ audit-v8 **cố ý** khoá 500 cho connection-refused;
test đó nay được thay bằng ca "IOException không phải connection-refused" để vẫn giữ 500 cho lỗi nội bộ thật.)

**Fix (TDD, fail trước → pass sau):**
- Thêm `@ExceptionHandler(WebClientRequestException.class)` → **503** `"Dịch vụ AI tạm thời không sẵn sàng..."`.
- Mở rộng `handleRuntimeTimeout` nhánh `containsConnectionRefused()` (ConnectException / UnknownHost /
  NoRouteToHost / SocketTimeout / message "connection refused") → 503.

**Test hồi quy:** `GlobalExceptionHandlerProblemDetailTest` — **8 test** (thêm `aiUpstreamUnreachableMapsTo503Not500`,
`reactiveConnectionRefusedMapsTo503`; sửa `otherReactiveErrorsStayOn500Not504`). Compile **fail trước**
(`handleWebClientRequestException` chưa tồn tại) → **8/8 pass** sau.

**Bằng chứng pass (live, container rebuild):**
```
Ollama TẮT  -> POST /api/ai/generate-vocab -> HTTP 503 {"title":"Service Unavailable",
               "detail":"Dịch vụ AI tạm thời không sẵn sàng. Vui lòng thử lại sau ít phút."}
Ollama BẬT  -> HTTP 200 (sinh "vacation","passport")
```
**Probe thứ 2 (R10):** tái hiện độc lập 2 chiều (down→503, up→200); 504 khi model cold-load là đúng thiết kế.

---

## F-14-03 — Tap-target AA: 118 đo nhưng v13 bỏ khỏi báo cáo — **LOW** — `CLOSED (triage: 0 REAL)`

**Nguồn:** đối chiếu `ui-sweep.json` của v13 với `REPORT.md` của nó.

**Bằng chứng:** v13 `ui-sweep.json.totals.smallTargets = 118` (WCAG 2.5.8 AA), nhưng `REPORT.md` §1 chỉ ghi
"0 guardFails, 0 console/page/api error, 0 contrast fail, 0 overflow" — **118 vi phạm chưa từng được triage**.

**v14 xử lý:** `sweep/v14/tap-target-triage.js` phân loại **cả 118** → **0 REAL** (50 SPACING-EXCEPT +
68 INLINE-EXCEPT). Số khớp chính xác `ui-sweep.json`.

**Sửa lỗi triage tự gây (minh bạch):** bản đầu chỉ áp Inline exception cho `<a>` → báo **2 REAL** (token
từ đơn ký tự "I" trên `/videos/1`, 8×26px). Kiểm WCAG 2.5.8: exception Inline áp cho **mọi** target trong
câu, không riêng `<a>` — token là `<button>` trong `<p>` → **thuộc miễn trừ**. Sửa logic triage; **KHÔNG**
sửa code (`git diff VideoLesson.vue` = rỗng — sửa code cho thứ được miễn trừ = over-fix).

**Probe thứ 2 (R10):** đo lại bằng chrome-devtools MCP trên `/login` (checkbox 16×16 nằm trong `<label>`
80×16 → vùng bấm 1287px²; link "Quên mật khẩu?" là `block` trong câu) → xác nhận không có control thật nào
dưới ngưỡng mà không được miễn trừ.

---

## F-14-C1 — Nợ dữ liệu: 67/72 tài khoản là test/audit — **LOW** — `OPEN` (cần quyết định owner)

**Nguồn:** v14 Phase 1 DB audit.

**Bằng chứng:** `users` = **72**, trong đó **67** khớp `*audit*` / `@test.local` / `@test.com` / `@t.com` /
`@engflow.test` / `e2e+` — chỉ **5** tài khoản thật (`user@`, `admin@`, `free_live_*@example.com`, 2 gmail).
**4** trong số đó là **admin**. 2 tài khoản rác có `exercise_attempts`; 0 có `study_days`.

**Tác động người dùng:** **không** — `GET /api/leaderboard?page=0&size=50` trả 50 entry, **0** entry chứa
`audit`/`test`/`zz` (probe thứ 2). Đây là **nợ dữ liệu**, không phải bug.

**Disposition:** `OPEN` — đề xuất owner dọn (xoá theo ID liệt kê sau backup), nhưng **không tự xoá** (V2: chỉ
fix lỗi đo được; xoá user là DML hàng loạt cần quyết định + backup).

---

## F-14-C2 — Nợ dữ liệu: 109 `payment_transactions` PENDING không `transaction_id` — **LOW** — `OPEN` (cần quyết định owner)

**Nguồn:** v14 Phase 1 DB audit.

**Bằng chứng:** 109 hàng `status='PENDING'`, `transaction_id IS NULL`, trải **2026-07-24 → 2026-09-12**
(2–19/ngày). Là các checkout session bỏ dở tích tụ qua nhiều kỳ audit/test (`PremiumCheckout.vue` gọi
create-order lúc mount).

**Tác động người dùng:** không — không phải giao dịch SUCCESS, không ảnh hưởng quyền premium. 17 hàng
`SUCCESS` là dữ liệu thật, không đụng.

**Disposition:** `OPEN` — đề xuất owner dọn theo cửa sổ ngày (sau backup). v14 **không tự xoá** (V2).

---

_(Hết finding v14.)_
