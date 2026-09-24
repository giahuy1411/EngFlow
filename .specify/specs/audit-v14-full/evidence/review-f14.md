# audit-v14-full — T6.4 review chéo đối kháng các fix AI sinh (R9)

**Ngày:** 2026-09-25 (+07)

**Ghi chú phương pháp:** một agent adversarial độc lập được dispatch để phản biện F-14-02 nhưng **chết vì
lỗi API** (không có kết quả). Thay vì bỏ qua bước review chéo, người thực hiện **tự review inline** theo
đúng khung phản biện dưới đây, và ghi rõ điều này để không ai tưởng có agent độc lập xác nhận.

---

## F-14-02 (503 cho AI upstream chết) — phản biện

### Claim 1 — "handler có bắt đúng exception shape thật không?"

**Nghi vấn:** `AiVocabService` bọc lỗi thành `new RuntimeException("Failed to generate vocabulary")`. Nếu
`WebClientRequestException` bị bọc mất, `@ExceptionHandler(WebClientRequestException.class)` không bao giờ
chạy — chỉ nhánh `containsConnectionRefused()` làm việc.

**Kiểm:** đọc `AiVocabService.generateVocabByTopic` (dòng ~86-89): khối `catch (Exception e)` **chỉ** bọc
lỗi **parse** (`throw new RuntimeException("Failed to generate vocabulary")` nằm trong `.map()` sau khi đã
có body). Lỗi **kết nối** xảy ra ở tầng `WebClient` **trước** `.map()`, nên `WebClientRequestException`
lan ra nguyên vẹn qua `.block()` → handler chuyên biệt bắt được.

**Bằng chứng thực nghiệm:** live test với Ollama tắt cho **503** (không phải 500) — nghĩa là một trong hai
đường (handler chuyên biệt hoặc nhánh connection-refused) **thực sự chạy**. Test đơn vị
`aiUpstreamUnreachableMapsTo503Not500` gọi thẳng `handler.handleWebClientRequestException(ex)`.

**Kết luận:** claim **đứng vững**. *Ghi chú:* nếu service đổi để bọc mọi lỗi thành `RuntimeException`
không giữ cause, nhánh `containsConnectionRefused` (đi theo `getCause()`) vẫn bắt được → **phòng thủ 2 lớp**.

### Claim 2 — "503 có khớp quá rộng không (che lỗi nội bộ thật)?"

**Nghi vấn:** `containsConnectionRefused` khớp `SocketTimeoutException` / `UnknownHostException` — có làm
một bug nội bộ thật bị đổi thành 503 không?

**Kiểm:** các lớp này **chỉ** được ném khi **kết nối ra ngoài** thất bại (connect/timeout/DNS). Một bug nội
bộ (NPE, SQL, parse) không tạo ra chúng → vẫn rơi vào `problem500`. Thứ tự nhánh trong `handleRuntimeTimeout`:
timeout → connection-refused → 500; nên **không** có overlap che lỗi.

**Kết luận:** claim **đứng vững**.

### Claim 3 — "thứ tự handler có gây nhập nhằng không?"

**Kiểm:** Spring chọn handler **cụ thể nhất** theo loại exception; `WebClientRequestException` (con của
`WebClientException` ⊂ `RuntimeException`) sẽ khớp handler chuyên biệt trước catch-all `RuntimeException`.
Không có ambiguity — Spring `ExceptionHandlerMethodResolver` chọn theo độ sâu kế thừa.

**Kết luận:** claim **đứng vững**.

### Claim 4 — "client HTTP khác (SePay/YouTube/Dictionary) có bị map nhầm 503 không?"

**Nghi vấn:** `containsConnectionRefused` là nhánh **chung** cho mọi `RuntimeException` — có làm SePay/YouTube
trả 503 sai không?

**Kiểm:** (a) `SePayApiService` dùng `RestTemplate` và **tự catch** (`catch (Exception e)` → trả null), không
lan ra handler. (b) YouTube/Dictionary dùng `WebClient` — nếu upstream chết, **503 là đúng ngữ nghĩa** (dịch
vụ ngoài không sẵn sàng), không phải lỗi che. Không có path nào mà một lỗi **nội bộ thật** lại mang cause
`ConnectException`.

**Kết luận:** claim **đứng vững**. Không over-match.

### Claim 5 — "test có thật sự test code path thật không?"

**Nghi vấn:** 2 test mới có phải chỉ test constructor cô lập, pass dù production hỏng?

**Kiểm:** `aiUpstreamUnreachableMapsTo503Not500` gọi thẳng handler (đúng method production gọi);
`reactiveConnectionRefusedMapsTo503` dựng `reactor.core.Exceptions.propagate(new IOException("Connection
refused"))` — **đúng shape** `.block()` leak ra. **Thiếu:** không có test end-to-end (service ném → handler
trả 503) ở tầng unit; nhưng **bằng chứng live** (Ollama tắt → 503; bật → 200) phủ đúng khoảng đó.

**Kết luận:** claim **đứng vững**; ghi nhận giới hạn (không có E2E unit test cho đường này — live test thay thế).

---

## F-14-01 (harness xoá dữ liệu thật) — review

| Câu hỏi | Kết luận |
|---|---|
| Fix có chặn hết method phá huỷ? | ✅ DELETE/PUT/PATCH → ghost id; 13 POST mutate → BLOCKED |
| Có bỏ sót write nào? | ✅ lần chạy 2 phát hiện thêm snapshot + study_days → đã chặn |
| Integrity check có phủ đủ? | ✅ assert parity **+ `study_days`** (parity không phủ) |
| Dữ liệu khôi phục đúng chưa? | ✅ lesson 445 (content 5948, 6 exercises), section 5 (5 blocks), parity baseline |

## F-14-03 (tap-target triage) — review

| Câu hỏi | Kết luận |
|---|---|
| 0 REAL có phải vì bỏ sót? | ✅ tally 50+68 = **118** khớp chính xác `ui-sweep.json` |
| 2 candidate có thật thuộc miễn trừ? | ✅ WCAG 2.5.8 Inline áp cho mọi target trong câu; token `<button>` trong `<p>` |
| Có sửa code cho thứ miễn trừ không? | ✅ KHÔNG — `git diff VideoLesson.vue` rỗng |

---

## Kết luận review

- **3/3 fix đứng vững** trước phản biện; **0 phản biện thành công**.
- Ghi nhận **1 giới hạn**: không có unit test end-to-end cho F-14-02 (live test thay thế).
- **1 điều chỉnh minh bạch:** agent adversarial độc lập chết vì lỗi API → review do người thực hiện tự làm,
  không phải agent độc lập xác nhận.
