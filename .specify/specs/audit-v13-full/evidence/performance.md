# Phase 5 — Hiệu năng: đo TRƯỚC khi tối ưu (P5: không tối ưu khi chưa có số)

**Ngày:** 2026-09-22 (+07) · **Probe:** `sweep/v13/perf-probe.js` + `sweep/v13/cls-probe.js`
**Artifact:** `sweep/v13/perf-before.json` · `sweep/v13/cls-before.json`
**Browser driver:** chrome-devtools MCP (`lighthouse_audit`, `performance_start_trace`) + Playwright (`PerformanceObserver`)

---

## 0. Điều kiện đo — đọc trước khi tin bất kỳ số nào

**Stack KHÔNG yên trong suốt phiên này.** Một phase audit khác chạy **song song**
(`sweep/v13/ui-sweep.js`) và đã **restart `engflow-backend` ít nhất hai lần**:

| Thời điểm (UTC) | Sự kiện | Đo được bằng |
|---|---|---|
| ~20:12–20:14 | 3 pass đầu tiên của tôi (backend cũ) | `perf-before*.json` ghi `at` 20:12:56 / 20:13:32 / 20:14:04 |
| **20:17:29** | backend **rebuild + restart** (image mới) | `docker inspect engflow-backend --format '{{.State.StartedAt}}'` |
| **20:19:34 – 20:20:29** | **3 pass canonical** (đúng 3 pass tôi dùng) | `perf-before*.json` ghi `at` 20:19:34 / 20:20:03 / 20:20:29 |
| **20:31:40** | backend **restart lần nữa** | `docker inspect` = `20:31:40Z` |
| sau đó | `ui-sweep.js` **vẫn chạy** (pid 12792) | `Get-CimInstance Win32_Process` |

Hệ quả đo được: cùng endpoint `lesson list (page 0)` cho median **47.1 ms** ở pass có tranh chấp, rồi
**14.4 ms** ở pass sạch — chênh **>3×**. Tôi **bỏ toàn bộ 3 pass đầu (20:12–20:14)** và chỉ dùng 3 pass
canonical (20:19:34–20:20:29), tức 3 pass **chạy sau** restart 20:17:29 và **trước** restart 20:31:40 —
chúng đo trên **cùng một** backend instance. Mọi số trong `results` của `perf-before.json` đến từ đó.

Các phép đo **sau** (deep pagination interleave, SRS query-delta) chạy ở cửa sổ khác và có thể chịu ảnh
hưởng của restart 20:31:40 + sweep đang chạy — nhưng kết luận của chúng **không phụ thuộc thời gian**
(§3.4 là đếm số query con; §4 là **sự VẮNG MẶT** của một hiệu ứng, nên nhiễu không tạo ra nó được).

Bài học lặp lại của lớp "probe tự bắn vào chân": **một harness đo hiệu năng phải kiểm stack có đang yên
không trước khi tin số** — nếu không, nó báo cáo tải của hàng xóm như thể là chi phí của app. Và phải
**ghi lại mốc thời gian của cả probe lẫn container**, nếu không thì sau này không ai biết số nào đo trên
build nào.

---

## 1. Phương pháp

- Mỗi endpoint gọi **5 lần/pass × 3 pass**; lấy **median của từng pass**, rồi **median của 3 pass**
  (`medianPerPass` được ghi lại trong JSON để kiểm lại). **Không dùng mean** — một GC pause không được
  phép kéo số.
- Body đọc hết (`res.arrayBuffer()`) **trước khi dừng đồng hồ**, nên TTFB nhanh không che chi phí serialize.
- `rate_limit:*` được **flush trước mỗi batch** (bucket global 100/phút/IP) — không thì probe tự tạo 429.
- `docker` gọi bằng `execFileSync` + mảng argv (một Redis key là **dữ liệu**, không được nội suy vào shell).

**35 endpoint** được đo, gồm **cả 9 endpoint v12 mới thêm** (user progress, dashboard stats, game session
quiz, srs stats, srs due deck, flashcard status, speaking prompts, video lessons, payment status) — lần này
đo lại độc lập — cộng thêm 13 endpoint v1/gamification mà v11/v12 chưa từng đo.

---

## 2. Kết quả — 35 endpoint

| Endpoint | Median | Nhóm |
|---|---|---|
| **admin exercise search `q=the`** | **185.1 ms** | admin |
| admin exercise list (no q) | 72.0 ms | admin |
| admin stats | 32.8 ms | admin |
| srs due (deck) | 27.0 ms | v12-new |
| video lessons (admin) | 25.5 ms | v13-new |
| admin users | 25.5 ms | admin |
| lesson list (page 5) | 22.5 ms | learner |
| lesson detail | 22.2 ms | learner |
| dashboard stats | 22.1 ms | v12-new |
| vocabulary list (auth) | 21.3 ms | learner |
| leaderboard | 21.3 ms | learner |
| streak current | 20.2 ms | learner |
| game session (quiz) | 19.5 ms | v12-new |
| decks (public) | 18.5 ms | learner |
| user progress | 17.8 ms | v12-new |
| speaking prompts (admin) | 16.2 ms | v13-new |
| admin lessons | 16.2 ms | admin |
| streak snapshot | 16.0 ms | learner |
| game session (mixed) | 16.0 ms | v13-new |
| video attempts (my) | 15.8 ms | v13-new |
| flashcard status | 14.5 ms | v12-new |
| lesson list (page 0) | 14.4 ms | learner |
| video lessons (public) | 14.2 ms | v12-new |
| game session (memory) | 14.1 ms | v13-new |
| lesson submissions (my) | 14.0 ms | v13-new |
| auth me | 13.5 ms | learner |
| srs stats | 13.5 ms | v12-new |
| exercise list (lesson) | 12.9 ms | learner |
| streak history | 12.2 ms | learner |
| lesson structure | 11.8 ms | learner |
| payment status | 11.8 ms | v12-new |
| lesson exercise content | 11.7 ms | learner |
| decks (my) | 10.0 ms | learner |
| vocabulary search | 9.6 ms | learner |
| speaking prompts (public) | 9.4 ms | v12-new |

**32/35 endpoint < 30 ms.** Chỉ **3** endpoint vượt 30 ms và cả 3 đều **admin-only**.

---

## 3. Từng endpoint chậm — số có biện minh cho tối ưu không?

### 3.1 `GET /api/admin/exercises?q=the` = **185.1 ms** — **KHÔNG tối ưu (đo lại lần thứ tư, cùng kết luận)**

Đây là lần đo **độc lập thứ tư** và **khớp** với 169.3 ms (v11), 197.6 ms (v12) và ~185 ms ghi trong
`AGENTS.md`. Số đứng vững.

**Gốc rễ — đo bằng plan thật, không suy đoán.** `SET SHOWPLAN_TEXT` và `sys.dm_exec_query_plan` trên chính
statement Hibernate phát ra đều cho:

```
Clustered Index Scan(OBJECT:PK__exercise__C121418ED83A5E11)   <- quét đủ 43 735 hàng
  + Compute Scalar: lower(question), lower(coalesce(explanation,''))
  + Filter: ... LIKE lower('%the%') ...
```

`exercises` có **6 index** (`PK exercise_id`, `idx_exercises_type`, `idx_exercises_difficulty`,
`idx_exercises_lesson_order`, `idx_exercises_lesson_type_order`, `IX_exercises_lesson_type_diff_order`)
— **không index nào trên `question`**.

| Đo ở tầng SQL | Logical reads | CPU | Kết quả |
|---|---|---|---|
| `COUNT` với predicate hiện tại | 1294 | 100–155 ms | 16 588 khớp |
| `COUNT` **bỏ `LOWER()`** (collation đã case-insensitive) | **1294** | 80–92 ms | **16 588 khớp — y hệt** |
| `COUNT` prefix `LIKE 'the%'` | 1294 | **12–13 ms** | 3 859 khớp (**đổi ngữ nghĩa**) |
| `COUNT` không filter | 133 | 0–4 ms | 43 735 |

Ba kết luận, mỗi cái có số:

1. **Bỏ `LOWER()` giữ nguyên kết quả, tiết kiệm ~20–25 ms CPU, nhưng KHÔNG đổi plan** (reads y hệt 1294,
   vẫn Clustered Index Scan). Đây đúng bằng kết luận v11 đã đo (103 → 84 ms CPU, plan không đổi).
2. **Không index nào cứu được leading wildcard `'%kw%'`.** Kể cả prefix `LIKE 'the%'` (sargable) vẫn scan
   1294 reads vì **không có index trên `question`** — chỉ nhanh vì lọc được 12 ms CPU ít hơn. Thêm index
   `question` là thay đổi schema trên bảng 43k hàng để phục vụ **một ô tìm kiếm admin** — không có số biện minh.
3. Cái **thật sự** giúp là **prefix search hoặc SQL Server full-text**, cả hai **đổi ngữ nghĩa tìm kiếm**
   (`q=the` sẽ thôi khớp "theory"). Đó là **quyết định sản phẩm**, không phải quyết định hiệu năng.

**Quyết định: KHÔNG tối ưu.** 185 ms trên một màn hình admin, đã ghi số ba lần trước, đổi JPQL trong một
method dùng chung để tiết kiệm ~20 ms CPU là một trade mà **số không biện minh**.

### 3.2 `GET /api/admin/exercises` (không filter) = **72.0 ms** — **KHÔNG tối ưu, nhưng số đáng ghi**

Đây là số **mới** so với v12 (v12 đo 56.5 ms). SQL của `COUNT` không filter chỉ **133 reads / 0–4 ms**, nên
72 ms **không đến từ việc đếm**. Đo delta `sys.dm_exec_query_stats` qua **một** call:

| Statement | Reads | CPU | Elapsed | Execs |
|---|---|---|---|---|
| page (SELECT … OFFSET/FETCH, 12 cột) | 4 770 | **183.8 ms** | 30.6 ms | 1 |
| count | 1 301 | 18.3 ms | 18.3 ms | 1 |
| auth (`select u1_0… from users`) | 18 | 0.5 ms | 0.5 ms | 3 |

Page statement là catch-all `(:lessonId IS NULL OR …)` với **12 tham số `nvarchar(4000)`** — SQL Server
không fold được thành seek, plan thật là `Top → Parallelism → Sort → Hash Match → Bitmap → Index Scan →
Filter`, **parallel (DOP > 1)**. CPU 183.8 ms nhưng elapsed chỉ 30.6 ms **vì nó chạy song song** — đó là lý do
API trả 72 ms chứ không 184 ms.

Ghi chú trung thực: khi tôi chạy **đúng SQL đó với literal `NULL`** thì chỉ **1364 reads / 116–188 ms CPU /
15–25 ms elapsed** — thấp hơn hẳn con số 4 770 reads của statement tham số hoá. Tôi **ghi cả hai số** và
**không giải thích** phần chênh (nhiều khả năng là do estimate của catch-all kém với tham số). Không kết luận
gì thêm khi chưa đo được cơ chế.

**Quyết định: KHÔNG tối ưu.** 72 ms, admin-only, không có ngưỡng nào bị vi phạm.

### 3.3 `GET /api/admin/stats` = **32.8 ms** — **KHÔNG tối ưu** (spread 24.3–41.3 ms giữa các pass; đây là
nhiễu, không phải tín hiệu).

### 3.4 `GET /api/srs/due/{deckId}` = **27.0 ms** — **KHÔNG tối ưu BÂY GIỜ, nhưng ghi lại N+1 có thật**

Đo delta query-stats qua một call:

```
10 × findByUserIdAndVocabularyId   (1 cho mỗi từ trong deck)   <- N+1
 1 × deck_words statement
 1 × auth
```

`SrsService.getDueWords` (dòng 132–134) lặp `deckWords` và gọi
`progressRepository.findByUserIdAndVocabularyId(userId, vocab.getId())` **trong vòng lặp**. Đây là **N+1
xác nhận bằng số**, không phải suy đoán từ đọc code.

**Nhưng:** deck seed chỉ có **10 từ** (đo: mọi deck đều 10 `deck_words`), nên chi phí hôm nay là 27 ms và
10 query con đều ~0 ms CPU / 4 reads mỗi cái. N+1 chỉ thành vấn đề khi deck lớn lên — **không có deck lớn
trong dữ liệu thật để đo**. Ghi lại làm **ứng viên có số**, chưa sửa: sửa bây giờ là tối ưu **không có
before/after number**, vi phạm P5.

---

## 4. Deep pagination — một kết quả **KHÔNG tái lập được**, ghi lại để không ai lặp lại

Lần đo đầu cho `q=the&page=1658` = **448.9 ms** so với page 0 = 158 ms — trông như một finding "deep OFFSET
chậm". **Nó không tái lập.** Đo lại bằng **interleave có kiểm soát** (mỗi vòng chạy đủ 5 depth, 7 vòng,
flush bucket mỗi lần):

| page | median |
|---|---|
| 0 | 213.4 ms |
| 400 | 197.3 ms |
| 800 | 199.6 ms |
| 1200 | 218.9 ms |
| 1658 | 109.4 ms |

**Không có xu hướng tăng theo độ sâu** — page cuối còn **nhanh nhất**. Con số 448 ms đầu tiên là **tranh chấp**
(concurrent sweep + backend restart ngay trước đó).

Ở tầng SQL thì có một khác biệt **nhỏ nhưng lặp lại được** (3 vòng, cùng select 12 cột):

| | Elapsed | Logical reads |
|---|---|---|
| `OFFSET 0 ROWS` | 30 / 31 / 33 ms | 1364 |
| `OFFSET 16580 ROWS` | 72 / 63 / 79 ms | 1364 |

Tức deep offset tốn thêm **~35 ms elapsed ở tầng SQL** (SQL Server vẫn phải sinh và bỏ 16 580 hàng) — nhưng
**cùng số reads** và **không xuất hiện ở tầng API** dưới interleave có kiểm soát.

**Kết luận: KHÔNG phải finding.** 35 ms SQL cho một trang admin ở tận page 1658 (một người dùng admin gõ tay
đến trang đó) không biện minh cho một thay đổi keyset-pagination. Ghi lại vì đây là ví dụ thứ N của lớp lỗi
"đo khi stack đang bận rồi tin số" — và vì **một kết quả âm tính có kiểm soát cũng là một kết quả**.

---

## 5. Bundle size (production build, `npx vite build`)

| File | Raw | gzip |
|---|---|---|
| entry `index-DBFid7BZ.js` | **177 545 B (177.44 kB)** | **67 444 B (67.55 kB)** |
| entry `index-DQKziDQ5.css` | 87 670 B | 15 242 B |
| chunk lớn nhất `markdown-DBu3buwd.js` (lazy) | 64 950 B | 21 953 B |

**Khớp `AGENTS.md`** (entry 177.44 kB / gzip 67.56) — **không drift**.

**Chuyển tải thật đo qua Resource Timing** (production build, `localhost:8098`):

| Route | Local transfer | Requests |
|---|---|---|
| `/` | 290 387 B | 12 |
| `/lessons` | 350 539 B | 13 |

`/lessons` nặng hơn `/` 60 kB — chunk `markdown-DBu3buwd.js` (64 950 B) + `Lessons` + `Pagination` được lazy-load.

**Font — số ẩn mà Lighthouse không cho thấy:** CSS Google Fonts khai **18 face** (`Be Vietnam Pro` ×
400/500/600/700/800/900 × 3 subset), và browser **thực sự tải 15 file woff2 = 125 376 B**. Các file này
**không xuất hiện** trong `local_transfer` ở trên vì cross-origin woff2 thiếu `Timing-Allow-Origin` nên
Resource Timing ẩn `transferSize` — chúng cũng không nằm trong `/api/*`. **Page weight thật = local + 125 376 B.**

Đây là **chi phí đã biết và có chủ ý** (`index.html` giải thích: 800 phải nằm trong URL vì 17 lượt dùng, và
bỏ nó khiến browser synthesize face giả từ 900 — đo được ở Edge). Tôi **ghi số, không đề xuất cắt**, vì
quyết định weight-800 đã có bằng chứng đứng sau.

---

## 6. Lighthouse — và **giới hạn của công cụ này**

`chrome-devtools MCP lighthouse_audit` trên máy này **chỉ trả 4 category**: `accessibility`,
`best-practices`, `seo`, `agentic-browsing`. **Category `performance` KHÔNG được sinh ra** — nên **không có
Lighthouse performance score, không có LCP/TBT từ Lighthouse**. Tôi **không bịa** một score.

| Route | A11y | Best-practices | SEO | Agentic | CLS | Audit fail |
|---|---|---|---|---|---|---|
| `/` (dev 5173) | **100** | **100** | **100** | 67 | **0.00062** | `llms-txt` |
| `/lessons` (dev 5173) | **100** | **100** | **100** | 67 | **0.00049** | `llms-txt` |
| `/login` (dev 5173) | **100** | **100** | **66** | 67 | **0.00058** | `is-crawlable`, `llms-txt` |
| `/` (production build) | **100** | **100** | **100** | 67 | **0.00106** | `llms-txt` |

**Hai audit fail — cả hai KHÔNG phải lỗi app:**

- **`llms-txt`**: `GET /llms.txt` trả **200 với `index.html`** (SPA fallback), không phải Markdown → Lighthouse
  báo "missing H1" + "no links". File **không tồn tại** trong `frontend/public/` lẫn `frontend/dist/`. Đây là
  **file chưa được tạo**, không phải file sai. (Lighthouse `agentic-browsing` là category mới; đây là mục duy
  nhất nó chấm 0.)
- **`is-crawlable` trên `/login`**: `robots.txt` **cố ý** `Disallow: /login /register /forgot-password
  /profile /admin`. **Đúng như thiết kế**, không phải defect.

### LCP — lấy từ `performance_start_trace` vì Lighthouse không cho

| Route (production build) | LCP | TTFB | Render delay |
|---|---|---|---|
| `/` | **260 ms** | 7 ms | 253 ms |
| `/lessons` | **200 ms** | 5 ms | 195 ms |

CLS trong trace: **0.00** cả hai. LCP tốt; chi phí nằm ở **render delay** (mount Vue), không ở mạng.

Lưu ý: `Cache` insight báo "wasted bytes 289 kB / 349 kB" — **đây là artefact của `python -m http.server`
tôi dựng tạm** (không gửi cache header). Không phải kết luận về app.

---

## 7. CLS — Playwright, cold cache, có kiểm cơ chế đo

`sweep/v13/cls-probe.js`: **context MỚI mỗi lần chạy** (cache rỗng — context ấm che mất shift vì chunk lazy
đã nằm trong bộ nhớ), `PerformanceObserver({type:'layout-shift', buffered:true})` + attribution, 3 lần/route,
và **assert `observerArmed === true`** (bài học v12 §V10: một `addInitScript` không chạy đã từng tạo ra 3 kết
quả "fix không hiệu quả" vô giá trị).

| Route | CLS median | 3 lần chạy | armed |
|---|---|---|---|
| `/` | **0.00069** | 0.00067 / 0.00069 / 0.00069 | ✅ |
| `/lessons` | **0.00095** | 0.00095 / 0.00095 / 0.00095 | ✅ |
| `/login` | **0.00003** | 0.00003 / 0.00003 / 0.00003 | ✅ |

Shift lớn nhất còn lại là **0.00071** ở `/lessons` (t=642 ms, `DIV.flex.flex-wrap.gap-2` + các
`A.app-navbar__link` — navbar reflow nhẹ khi dữ liệu về).

**F150 của v12 vẫn FIXED**: cả 3 route < 0.001 (ngưỡng "good" là 0.1). Không có finding CLS mới.

---

## 8. Verdict Phase 5

**PASS, P5 giữ nguyên — không dòng code nào bị đổi.**

- **35 endpoint** đo (5 lần × 3 pass, median), gồm cả 9 endpoint v12 mới thêm. **32/35 < 30 ms**; 3 endpoint
  > 30 ms **đều là admin-only** và **đều được từ chối tối ưu kèm số**.
- Endpoint chậm nhất (`admin exercises q=the`, **185.1 ms**) được truy gốc tới **plan thật** (Clustered Index
  Scan 43 735 hàng), và 3 phương án được đo: bỏ `LOWER()` (plan không đổi), thêm index (không có index trên
  `question`; prefix vẫn scan), prefix/full-text (**đổi ngữ nghĩa** → quyết định sản phẩm). **Không sửa.**
- **Một N+1 có thật** được chứng minh bằng số (`srs/due`: 10 query con cho deck 10 từ) — ghi làm ứng viên,
  **không sửa**, vì sửa bây giờ là tối ưu không có before/after number.
- **Một finding tiềm năng bị chính probe giết**: "deep pagination 448 ms" không tái lập dưới interleave có
  kiểm soát → **không phải finding**, ghi lại kèm số.
- Bundle **khớp baseline** (177.44 kB / 67.55 kB gzip, 0 drift). Font **125 376 B** được **đo và ghi** vì nó
  vô hình với Resource Timing — không đề xuất cắt (quyết định weight-800 đã có bằng chứng).
- **Lighthouse không sinh category Performance trên máy này** — ghi rõ giới hạn, lấy LCP từ
  `performance_start_trace` thay vì bịa score. 2 audit fail đều là **hành vi đúng** (`robots.txt` cố ý) hoặc
  **file chưa tạo** (`llms.txt`), không phải defect app.
- **F150 vẫn fixed**: CLS < 0.001 trên cả 3 route, đo bằng probe **tự kiểm cơ chế đo**.

**Cleanup:** không có row nào bị tạo — mọi probe là GET; lần ghi DB duy nhất là `POST /api/auth/login`.
Parity sau phiên: `1470|43735|72|118|29|15|4|126|10|5` — **khớp baseline**. Cleanup count = **0**.
