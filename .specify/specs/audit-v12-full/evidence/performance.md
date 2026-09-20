# Phase 6 — Hiệu năng, đo trước/sau (P5: không tuyên bố tối ưu khi chưa có số)

**Ngày:** 2026-09-21 (+07) · **Probe:** `sweep/v12/perf-probe.js` (median của **5** lần, ≥3 theo yêu cầu)
**Artifact:** `sweep/v12/perf-before.json` · **Driver browser:** chrome-devtools MCP (Lighthouse) + Playwright (CLS)

---

## 1. Phương pháp

Mỗi endpoint gọi **5 lần**, lấy **median** (không phải mean — một lần GC pause không được phép làm lệch số).
Body được đọc hết (`res.arrayBuffer()`) trước khi dừng đồng hồ, nên TTFB không che chi phí serialize.
`rate_limit:*` được **flush trước mỗi batch** (bucket global 100/phút/IP) — không thì probe tự biến các lần đo cuối
thành 429 và thổi phồng median (đúng lỗi `sweep/v10/perf-probe.js` không có flush).

## 2. Kết quả — 22 endpoint (v12 thêm 9 endpoint v11 chưa từng đo)

| Endpoint | Median |
|---|---|
| lesson list (page 0) | **22.3 ms** |
| lesson list (page 5) | **22.9 ms** |
| lesson detail | **20.8 ms** |
| exercise list (lesson) | **20.5 ms** |
| streak snapshot | **18.0 ms** |
| vocabulary search | **11.0 ms** |
| leaderboard | **30.2 ms** |
| decks (public) | **17.9 ms** |
| **admin exercise search `q=the`** | **197.6 ms** |
| admin exercise list (no q) | **56.5 ms** |
| admin lessons | **27.9 ms** |
| admin users | **48.3 ms** |
| admin stats | **51.2 ms** |
| **user progress** (v12 mới) | **17.8 ms** |
| **dashboard stats** (v12 mới) | **25.3 ms** |
| **game session quiz** (v12 mới) | **25.0 ms** |
| **srs stats** (v12 mới) | **10.9 ms** |
| **srs due deck** (v12 mới) | **27.2 ms** |
| **flashcard status** (v12 mới) | **15.3 ms** |
| **speaking prompts** (v12 mới) | **10.9 ms** |
| **video lessons** (v12 mới) | **20.8 ms** |
| **payment status** (v12 mới) | **13.7 ms** |

**21/22 endpoint < 60 ms.** Mọi endpoint mới thêm đều **< 31 ms**.

## 3. Endpoint chậm duy nhất: admin exercise search — đo rồi **TỪ CHỐI** tối ưu

`GET /api/admin/exercises?q=the` = **197.6 ms** vs **56.5 ms** không filter — **xác nhận** số ~185 ms ghi trong
`AGENTS.md` và 169.3 ms của v11 (đo độc lập lần thứ ba).

**Gốc rễ** (`ExerciseRepository.findAdminPage`): cột bị bọc `LOWER()` (non-sargable) **và** pattern
leading-wildcard `'%kw%'` → full scan **43 735 row**. Cả hai đều chặn index.

**v11 đã đo phương án bỏ `LOWER()`**: logical reads y hệt (1294), CPU 103 → 84 ms — tiết kiệm ~19 ms CPU nhưng
không đổi plan. **v12 giữ nguyên kết luận TỪ CHỐI** và không đo lại phương án đó (v11 đã đo, số đứng vững); lý do
là nó là **thay đổi JPQL trong method dùng chung**, đổi ~19 ms CPU admin-only, không phải trade mà số biện minh.

> Cái thật sự giúp (không làm, vì là **quyết định sản phẩm**): prefix search `LIKE 'the%'` (sargable) hoặc SQL
> Server full-text — cả hai **đổi ngữ nghĩa tìm kiếm** (`q=the` sẽ thôi khớp "theory").

## 4. Bundle — trước/sau

| Build | Entry `index-*.js` | gzip |
|---|---|---|
| Baseline v12 (đầu phiên) | **177.44 kB** | 67.56 kB |
| Sau fix F149 (token + 1 class) | **177.44 kB** | 67.55 kB |
| Sau khi revert F150 | **177.44 kB** | 67.55 kB |

**Fix F149 tốn 0 byte.** Tailwind chỉ phát utility thực dùng; token `placeholder` thay chỗ class cũ.

---

## 5. F150 — CLS trên `/` (Lighthouse 0.103) — **truy gốc, TỪ CHỐI fix, ghi số**

### Đo được (Playwright, PerformanceObserver + attribution, **production build**)

```
clsTotal = 0.104       (1 entry chi phối)
t=185ms  FOOTER  prev=[y=827, h=96]  cur=[y=0, h=0]   v=0.10401
```

Tái lập **4 lần** (0.104 / 0.111 / 0.104 / 0.104) trên cả dev server và production build. **Một nguồn duy nhất**:
phần tử `FOOTER.app-footer`.

### Truy gốc (đo, không suy đoán)

| Bước | Phát hiện |
|---|---|
| `MutationObserver` trên `#app` | `appChildren: 0` ở t=40ms → footer **ADDED** ở t=66ms. Shell mount sau first paint. |
| Đo rect lúc settle | footer `y=3421`, `h=96`; `main` `h=3351`; shell `min-h-screen` = 867px |
| Đọc `index.html` | `<div id="app"></div>` rỗng trước Vue; **không** có skeleton/footer tĩnh |
| Suy ra cơ chế | Shell `min-h-screen` (867px) → footer **first paint ở y≈827 = đáy viewport (ĐANG NHÌN THẤY)**. Home là route **lazy** (`() => import('@/views/Home.vue')`); khi chunk Home về, `<main>` giãn 3351px → footer bị đẩy xuống dưới fold. Việc một khối 96px **đang thấy** nhảy ra **ngoài màn hình** = 0.104 CLS. |

### Đã thử fix và **TỪ CHỐI** (ghi cả hai số)

| Phương án | Kết quả đo | Kết luận |
|---|---|---|
| `#main-content{min-height:calc(100vh - 64px - 96px)}` (giữ chỗ cho main) | CLS **0.104 → 0.098** | **Không đủ** — footer vẫn first paint ở y≈833 (trong viewport) rồi vẫn bị đẩy đi. Đã **REVERT** (xem §5b) |

**Đã revert thay đổi này.** Ship một thay đổi không sửa được lỗi là tệ hơn không đổi gì.

### 5b. Một bài học về chính probe

Trong lúc thử fix, tôi chạy 3 biến thể "inject CSS lúc runtime" qua `addInitScript` và **cả ba đều báo CLS y
nguyên**. Sau đó tôi kiểm chính cơ chế inject:

```
injected: false    <- init script KHÔNG HỀ CHẠY
```

⇒ Ba kết quả "fix không hiệu quả" đó **không có giá trị** (đo trên build chưa đổi). Tôi chỉ tin phép đo sau khi
sửa **source + rebuild** thật. Đây là lỗi probe cùng lớp P1–P9 của v11: **kiểm cơ chế đo trước khi tin kết quả đo.**

### Vì sao để OPEN chứ không sửa

CLS 0.104 chỉ hơn ngưỡng "good" (0.1) **0.004**, trên **một route**, do **một** phần tử, và bản chất là "footer
đáng lẽ nằm dưới fold ngay từ đầu". Các fix thật (SSR/prerender, hoặc bỏ lazy-load Home, hoặc đặt footer trong
`main` sau nội dung) là **thay đổi kiến trúc render** — vượt phạm vi một fix a11y/perf nhỏ và có rủi ro hồi quy.
Ghi **F150 OPEN kèm số và gốc rễ**, để owner quyết. (Lighthouse `/` Accessibility vẫn **100**; CLS nằm ở category
Performance, không ảnh hưởng a11y.)

---

## Verdict

**Phase 6: PASS với kỷ luật P5 giữ nguyên.** 21/22 endpoint < 60 ms; endpoint chậm duy nhất được truy gốc và **từ
chối tối ưu có số** (lần đo độc lập thứ ba, khớp `AGENTS.md`). Bundle **không tăng** từ fix F149. **F150 (CLS
0.104)** được phát hiện qua Lighthouse, truy gốc tới **một** phần tử, một phương án fix đã **đo và revert** vì
không đủ, và ghi **OPEN** thay vì ship non-fix. Trong quá trình đó, probe tự bắt được một lỗi của chính nó
(inject CSS không chạy) trước khi kết luận sai.
