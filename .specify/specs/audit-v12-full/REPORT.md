# Báo cáo audit-v12-full

**Ngày:** 2026-09-21 (+07) · **Nhánh:** `audit-streak-review` · **Checkpoint đầu:** `30bc9b6` (tree sạch)
**Tiền nhiệm:** `audit-v11-full` (đã commit) · **Artifact:** `.specify/specs/audit-v12-full/`
**Phương pháp:** mọi con số trong báo cáo này do **chính phiên này đo**, không sao chép từ v11. Chỗ trùng ghi là
**xác nhận**; chỗ lệch ghi là **phát hiện**.

---

## 0. TL;DR — điều quan trọng nhất

1. **Khoảng trống thật của v11 đã đóng:** source có **131 endpoint / 26 controller**, v11 chỉ probe **~27** và
   **12 controller chưa từng được chạm**. v12 phủ **hết 131**, mỗi endpoint có status.
2. **"Thay toàn bộ font thành Be Vietnam Pro" là VIỆC ĐÃ XONG** — đo trên DOM thật: **đúng 1 họ font**,
   `document.fonts.check('16px "Be Vietnam Pro"') = true`, 0 hit Outfit/Plus Jakarta trong `frontend/src`.
   **v12 verify, không làm lại.** Báo nó là việc mới sẽ là bịa.
3. **Design system Playful Geometric đã triển khai đủ** — token khớp từng giá trị, hero sun, blob, marquee,
   squiggle, connector, button pill 2px.
4. **Tìm được 2 lỗi sản phẩm mới, đã fix:**
   - **F146** (MEDIUM) — README/CLAUDE.md mô tả **sai sản phẩm** (223 test vs thật 485; "Azure Speech SDK" vs thật
     Whisper+Ollama; `@PremiumRequired` vs 0 usage; "refresh via login" vs 0 endpoint). **Đã sửa + verify.**
   - **F149** (MEDIUM) — `/admin/lessons` chữ "Trống" dùng `text-muted-foreground/60` → composited **2.65:1**,
     trượt WCAG AA. **Đã fix → 4.83:1 + regression test.**
5. **Các hạng mục từng ghi OPEN — nay ĐÃ XỬ LÝ HẾT:** **F147** (authz `POST /api/vocabulary` — deck-scoped +
   atomic), **F148** (`/api/srs/*` 0 caller — hợp nhất thuật toán **và** dựng UI ôn từ đến hạn), **F150**
   (CLS 0.104 → 0.001), **C6** (xoá Azure SDK ~15MB), **F151** (IDOR `/api/srs/due/{deckId}` — **phát hiện mới**
   ở Phase 10). Xem §2 và `evidence/phase-9-open-items.md` + `evidence/phase-10-f151-ui-cleanup.md`.
6. **Vòng 2 không tìm thêm lỗi sản phẩm mới**, nhưng **bắt được 4 lỗi của chính probe** (V9–V12) mà vòng 1 báo sai.
7. **Parity đổi CÓ CHỦ Ý ở Phase 10** (dọn rác): `1471|43735|72|127|28|15|4|126|14|5` →
   **`1470|43734|72|118|28|15|4|126|10|5`** (−1 lesson, −1 exercise, −9 vocabulary, −4 decks). **0 rác.**

---

## 1. Đã làm gì

### Phase 0 — Đo lại baseline từ đầu
| Hạng mục | Kết quả (đo thật) |
|---|---|
| Backend `mvn -o test` | **485 run / 0 fail / 0 error / 11 skipped — BUILD SUCCESS** |
| Frontend `npx vitest run` | **119 passed / 1 skipped (23 file)** |
| Build `npx vite build` | entry **177.44 kB** (gzip 67.56) |
| Container | 8 container up; backend **không stale** (0 `.java` mới hơn container start; probe sống thấy header F145) |
| Endpoint inventory | **131 annotation → 147 row / 145 distinct / 26 controller** (script `sweep/v12/api-inventory.js`) |
| Parity | `1471\|43735\|72\|127\|28\|15\|4\|126\|14\|5` (khớp v11) |

→ `evidence/baseline.md`

### Phase 1 — Audit DB trong Docker (read-only)
**Hiệu lực constraint chứng minh trong DB scratch** (v11 để OPEN): FK vi phạm → bị chặn; unique vi phạm → bị chặn;
filtered unique cho nhiều NULL; FK nullable chấp nhận. **0 orphan** (NULL FK đếm riêng — `vocabulary.lesson_id` 126
NULL là hợp lệ). Streak schema deploy đúng. `study_days` = 2 row là **hoạt động học thật**, không phải rác.
Fragmentation 11.57% trên 121 page → **từ chối tối ưu, có số**.

→ `evidence/db-audit.md`

### Phase 2 — API sweep: **133 pass / 0 fail / 1 blocked / 3 N/A**
Phủ **hết 131 endpoint**, gồm **12 controller v11 bỏ sót** (Game, Flashcard, Srs, Leaderboard, LessonSubmission,
LessonSnapshot, Progress, SpeakingPrompt, AdminExerciseSeed, LessonStructure, AdminAnswerBackfill, Dashboard,
MediaProxy). Kiểm **hợp đồng** (tên field + kiểu), role matrix **cả hai chiều** (401/403/200). Tái chứng minh **sống**
4 guard draft cũ (F88/F89/F115/F126). **4 "lỗi" đầu tiên bị falsify là lỗi probe.** Dọn trong cùng run → **0 rác**.

→ `evidence/api-sweep.md`

### Phase 3 — UI sweep bằng **CẢ HAI** MCP
chrome-devtools MCP dùng **thật** (evaluate/network/Lighthouse — v11 gần như chỉ `list_pages`). 20 route × 3 role,
guard **2 chiều**, **0 console/page/API error**, 0 overflow thật ở 5 width, 0 `alt` thiếu, 0 heading skip.
Lighthouse a11y **100** trên `/`, `/lessons`, `/login`. Font + design system verify trên DOM thật.

→ `evidence/ui-sweep.md`

### Phase 4 — CRUD qua UI admin (end-to-end)
Lesson: create (id=103030) → read → update → delete, **xác nhận ở cả UI lẫn API**, qua hộp thoại `confirm` thật.
Deck: create (id=50070) → verify → cleanup. **0 rác, parity không đổi.**

→ `evidence/ui-crud.md`

### Phase 5 — Sửa lỗi đo được
F146 (doc drift) + F149 (contrast), mỗi cái có đo lại + regression test cho F149.

### Phase 6 — Hiệu năng
**22 endpoint** (9 endpoint mới v11 chưa đo), median của 5. **21/22 < 60 ms**. Endpoint chậm duy nhất (admin
exercise search 197.6 ms) truy gốc + **từ chối tối ưu có số**. Bundle không tăng từ fix.

→ `evidence/performance.md`

### Phase 7 — Vòng 2, rộng hơn (loop-until-dry)
Suite xanh không hồi quy; API **133/0/0 giống hệt**; contrast **2 → 0** (xác nhận F149 fix); streak **25/25 PASS**
sau khi sửa probe drift. **0 lỗi sản phẩm mới.**

→ `evidence/second-pass.md`

---

## 2. CHƯA làm gì (ghi rõ, không giấu)

> **Cập nhật Phase 9.** Bốn hạng mục từng OPEN ở đây **đã được xử lý** sau khi owner chọn hướng:
> **F147** (deck-scoped + atomic), **F148** (hợp nhất 2 thuật toán SRS về SM-2), **F150** (CLS 0.104 → 0.001),
> **C6** (xoá Azure SDK + config/DTO chết). Chi tiết + số đo lại: `evidence/phase-9-open-items.md`.
>
> **Cập nhật Phase 10.** Ba hạng mục cuối **cũng đã xử lý**: **UI "ôn từ đến hạn"** (route
> `/decks/:id/review` + `srsService.js` + `DueReview.vue`), **dọn 9 row vocab rác + 4 deck test + lesson
> 61882**, và **F151 mới phát hiện** (IDOR `/api/srs/due/{deckId}`). Chi tiết + số đo lại:
> `evidence/phase-10-f151-ui-cleanup.md`. Danh sách dưới đây là những gì **vẫn** chưa làm.

| Hạng mục | Trạng thái | Lý do |
|---|---|---|
| UI "ôn từ đến hạn" (`/decks/:id/review` + `GET /api/srs/due/{deckId}`) | **ĐÃ LÀM** (Phase 10) | 4 trạng thái + quality 1/4/5; E2E live; Lighthouse 100 |
| Xoá **27 row `vocabulary`** mồ côi | **ĐÃ LÀM một phần** (Phase 10) | Xoá **9** row rác/trùng; **giữ 18** từ AI tiếng Anh thật |
| **F151** IDOR `/api/srs/due/{deckId}` | **ĐÃ FIX** (Phase 10) | Uỷ quyền `DeckService.getDeckById`; 9/9 probe PASS |
| Repair 14 row `uvp` có `ease_factor=2.5` | **KHÔNG CẦN** | Giá trị khởi tạo hợp lệ của SM-2 — tự lành ở lần review kế tiếp |
| Retire `/api/srs/*` | **KHÔNG LÀM** | Sau hợp nhất, `SrsService` là nguồn sự thật duy nhất; nay có UI thật |
| Giao dịch ngân hàng thật với SePay | **BLOCKED by design** | Là thanh toán thật. Đã kiểm phía **nhận** (chữ ký sai → reject) |
| Chấm điểm phát âm giọng thật | **N/A** | Mic giả phát im lặng → Whisper trả rỗng → `FAILED` **đúng thiết kế** |
| Snapshot create/restore qua API | **N/A** | Ghi/khôi phục nội dung thật |
| Speaking upload/assess, avatar upload | **N/A** | Cần media thật (MinIO/Cloudinary); v11 đã có bằng chứng |

### Issue GitHub đã tạo (T8.4) — và trạng thái sau Phase 9 + 10

| Issue | Nội dung | Sau Phase 10 |
|---|---|---|
| [#5](https://github.com/giahuy1411/EngFlow/issues/5) | security(authz): `POST /api/vocabulary` ghi bảng global (F147) | **ĐÃ FIX** — đóng được |
| [#6](https://github.com/giahuy1411/EngFlow/issues/6) | chore(api): `/api/srs/*` 0 caller frontend (F148) | **ĐÃ FIX đủ** — thuật toán hợp nhất **và** UI ôn từ đến hạn đã có; đóng được |
| [#7](https://github.com/giahuy1411/EngFlow/issues/7) | perf(frontend): CLS 0.104 trên `/` (F150) | **ĐÃ FIX** — đóng được |
| [#8](https://github.com/giahuy1411/EngFlow/issues/8) | chore(deps): Azure Speech SDK ~15MB không dùng (C6) | **ĐÃ FIX** — đóng được |

---

## 3. Đã fix gì và fix thế nào

| ID | Mức | Vấn đề | Gốc rễ | Cách fix | Đo lại |
|---|---|---|---|---|---|
| **F146** | MED | README/CLAUDE.md mô tả **sai sản phẩm** | Tài liệu không cập nhật khi code đổi | Sửa doc theo số đo (safe fix) | grep: **0 drift** còn lại |
| **F149** | MED | `/admin/lessons` "Trống" **2.65:1** (cần 4.5:1) | `text-muted-foreground/60` — **opacity** pha loãng token xuống #99A0A9 (cùng lớp lỗi F138) | Thêm token **`placeholder` = #6B7280** (tính WCAG trước: **4.83:1**) + `text-placeholder` | **2.65 → 4.83**, 0 class cũ còn; + regression test 2/2 |
| **F151** | MED | `/api/srs/due/{deckId}` **rò rỉ nội dung** deck private người khác (IDOR) | `SrsService.getDueWords` đọc `deck_words` **không kiểm quyền**; `DeckService.getDeckById` đã có sẵn logic đúng | Uỷ quyền `deckService.getDeckById(deckId, userId)` trước khi đọc; + 4 test authz | student `30033`: **200 → 400**; deck không tồn tại: **200+`[]` → 404**; probe **9/9 PASS** |

### Hai lỗi tôi tự gây ra và tự bắt (ghi để trung thực)
1. **Đo "sau fix" F149 lần đầu trên dev server trả `#556070`** (6.38:1 — trông như pass) nhưng thực ra là **kế thừa**
   từ parent, vì Vite dev server giữ **Tailwind config cũ**. Chỉ sau `docker restart engflow-frontend` mới resolve
   đúng `#6B7280`. Tin lần đo đầu = báo "pass" nhầm mà không có fix thật.
2. **3 biến thể "inject CSS runtime" cho F150 đều báo CLS y nguyên** — kiểm cơ chế thì `injected: false`
   (init script không chạy). Ba kết quả đó **vô giá trị**; tôi chỉ tin phép đo sau khi sửa **source + rebuild**.

---

## 4. Bằng chứng

| File | Nội dung |
|---|---|
| `evidence/baseline.md` | Baseline đo lại (485 / 119·23 / 177.44 kB / parity) |
| `evidence/endpoint-inventory.json` | 131 annotation → 147 row, 26 controller |
| `evidence/db-audit.md` | DB audit + **hiệu lực constraint** (DB scratch) |
| `evidence/api-sweep.md` | 133/0/0/1 blocked/3 N/A, 15 vùng, 12 controller mới |
| `evidence/ui-sweep.md` | Route×role, a11y, contrast, responsive, font, design system |
| `evidence/ui-crud.md` | CRUD qua UI admin (lesson + deck) |
| `evidence/performance.md` | 22 endpoint median 5, từ chối tối ưu có số, F150 |
| `evidence/second-pass.md` | Vòng 2 + 3 lỗi probe bị falsify |
| `evidence/phase-9-open-items.md` | F147 · F148 · F150 · C6 — fix + đo lại |
| `evidence/phase-10-f151-ui-cleanup.md` | **F151 · UI ôn từ đến hạn · dọn dữ liệu + parity mới** |
| `findings.md` | F146–**F151** + **12 probe bug** (V1–V12) |
| `analyze.md` / `converge.md` | Đối chiếu chéo + bù việc |
| `sweep/v12/*` | Probe, SQL, harness (`f151-idor-probe.js`, `cleanup*.sql`, `fk-map.sql`) |

**Trạng thái cuối (sau Phase 11):** backend **499/0/0**, frontend **127/1 skipped (25 file)**, build **177.44 kB**, parity
**`1470|43734|72|118|28|15|4|126|10|5`** (không đổi), route **40** (41→40 — bỏ due-words),
**0 rác**, API sweep **143 pass / 0 fail**, A11y flashcard **all WCAG checks pass**.

---

## 5. Skill đã nạp trong phiên này

| Skill / plugin | Dùng để làm gì |
|---|---|
| `speckit-*` (constitution→specify→clarify→checklist→plan→tasks→implement→converge→analyze) | Pipeline bắt buộc |
| `superpowers:using-superpowers` | Luật nền: nạp skill trước khi hành động |
| `superpowers:brainstorming` | Chốt phạm vi với người dùng trước khi làm |
| `superpowers:systematic-debugging` | Truy gốc F149 (opacity) và F150 (CLS) thay vì vá triệu chứng |
| `superpowers:test-driven-development` | Regression test F149 (guard cả class lẫn hex) |
| `superpowers:verification-before-completion` | Mọi tuyên bố "pass" đều có output lệnh đứng sau |
| **`chrome-devtools-mcp:*`** (`chrome-devtools`, `a11y-debugging`) | Driver 1 hạng nhất: evaluate, network, **lighthouse_audit** |
| **Playwright MCP** (`browser_*`) | Driver 2: route/flow/network, CRUD UI, CLS |
| `accessibility` (WCAG 2.2) | Nền cho F149 + áp **ngoại lệ 2.5.8** (Inline/Spacing) để không over-report |
| `frontend-design` | Tầng token `placeholder` theo pattern `*-ink`/`*-strong` |
| `addyosmani-performance-optimization` | Phương pháp Phase 6: đo rồi mới tối ưu |
| `addyosmani-api-and-interface-design` | Kiểm hợp đồng API (field + type) |
| `java-springboot`, `java-coding-standards` | Đọc backend, xác nhận gốc rễ constraint/query |
| `workflow-authoring` | (nạp tham chiếu) orchestration |
| `update-config` | (không cần dùng — không lệnh nào bị chặn quyền) |

---

## 6. Điều cần biết

1. **Khoảng trống coverage là phát hiện quan trọng nhất.** v11 báo "API 74/0/0" — nghe như phủ hết, nhưng thực tế
   **12/26 controller chưa từng được probe**. Con số "0 fail" chỉ có nghĩa trong phạm vi đã probe.
2. **Vòng 2 giá trị nhất ở chỗ bắt lỗi của chính probe**, không phải ở việc xác nhận app đúng: V9 (tap-target sai
   vì bỏ qua ngoại lệ WCAG), V10 (inject CSS không chạy), V11 (KB4 drift theo lịch). Nếu bỏ vòng 2, tôi đã "sửa"
   3 thứ đang đúng.
3. **"Đã xong" không được nhận là việc mới.** Font BVP và design system đã đạt từ trước; v12 chỉ verify. Đây là
   mục B1 của checklist và là chỗ dễ bịa nhất.
4. **Hai lỗi do chính tôi gây ra** (đo F149 trên dev server stale; tin kết quả inject CSS không chạy) đều bị bắt
   nhờ **kiểm lại cơ chế đo**, không phải nhờ tin vào kết quả đầu.
5. **Còn 3 hạng mục OPEN + 1 owner decision** — ghi ở §2, không giấu. Không hạng mục nào là lỗi bị bỏ sót.
