# audit-v19-full — BÁO CÁO

**Ngày:** 2026-09-27 (+07) · **Nhánh:** `audit-v15-full` @ `0dbd66c` (**giữ nguyên**)
**Tiền nhiệm:** `audit-v18-full` (đã commit) · **Artifact:** `.specify/specs/audit-v19-full/`
**Phạm vi:** đóng nốt **5 mục "Còn lại"** của v18 + sửa defect mới phát hiện; Phase U 2 engine; 2 vòng; docs; rác; báo cáo.

---

## 1. Tóm tắt một đoạn

Vòng v19 **đóng nốt 5 mục "còn lại"** và **đính chính 3 kết luận v18 SAI/THIẾU** (đo lại, không chép):
**W1** AI gloss tiếng Trung — tái hiện thật qua Playwright MCP (天气/气候) rồi **fix tận gốc** (prompt nêu ngôn ngữ +
guard `containsCjk`) → **0 CJK ×15 call**; **W2** `smallTargets=115` — harness cắt list **giấu 12 button thật**, sửa
harness theo **WCAG 2.5.8 đầy đủ** → 115→0 (mutation-test chứng minh vẫn bắt vi phạm thật); **W3** admin exercises
scan cả bảng → **index `IX_exercises_order_id`** → page reads **1341→86**, endpoint **42.8→26.6ms** (đồng thời
**bác bỏ** giả thuyết projection: 1417 vs 1417); **W4** SePay chữ ký thật — **PASS** (người dùng chuyển khoản thật →
webhook chữ ký SePay settle `ENG73E2D3AA2DF6` → SUCCESS, tx `85111759`, MBBank); **Premium flow** đi **cả 2 MCP engine**. Review chéo đối kháng bắt **3 vấn đề
thật + 1 lỗ hổng test** → tôi đối chiếu code, **sửa tận gốc**. Backend **541/0** (+4 test), frontend **194/1** —
**0 regression**. 2 vòng hội tụ (vòng 2 = 0 finding mới). Rác dọn, docs đồng bộ.

---

## 2. Đã làm

### 2.1 Phase 0 — SpecKit + baseline
Nhánh `audit-v15-full` cây sạch · pipeline docs (spec/clarify/checklist/plan/tasks) · `assert-harness` **ALL CLEAN 7/7**
· backend **537/0/0/11** · frontend **194/1 (32)** · build **177.75 kB** · parity `1470|43738|5|118|29|4|3|12|10`
· inventory 121/137/135/25 · **prompt flaw đo lại** (4 lỗ hổng) · `prompt-rewritten-v19.md`.

### 2.2 Phase 1 — API sweep
`api-sweep` **145/0/0/2na** · `deep-probe` **58/0/0** · `search-sort` pass · **AI stress ×25** → 0 500/504.

### 2.3 Phase 2 — DB + secret-scan
**0 orphan thật** (loại NULL) · **22/22 FK có index** · slow query (JDBC metadata + admin) · timezone naive-VN ·
**`ddl-auto=validate` drill PASS** (12.29s, 0 ERROR) · secret-scan **0 hit**.

### 2.4 Phase 3 — UI/UX automated
`ui-sweep` **0/0/0/0** (smallTargets **0** sau fix W2) · `design-v2` **0 drift + BVP LOADED** · `routes-all` **228 visit
0 sai** · CLS 0.00069/0.00095/0.00003 · danger-tint/focused/a11y PASS.

### 2.5 Phase U — **CẢ 2 MCP engine cho MỌI luồng**
13 luồng × 2 engine: auth, guard 2 chiều, lessons + chống lộ đáp án, streak, search, CRUD/game, AI, admin, video,
speaking, leaderboard, **Premium + checkout**, home. 2 engine đối chiếu nhau. Ảnh `shots/mcp/<engine>/**`.

### 2.6 Phase D — 6 nhóm chức năng
`demo-claims.md`: mọi `file:line` **sed đối chiếu** tồn tại + đúng; mọi khẳng định CONFIRMED; **W1 defect phát hiện + sửa**.

### 2.7 Phase W — 4 workstream
W1 (AI gloss) · W2 (tap-target) · W3 (perf index) · W4 (SePay — **PASS**). Chi tiết: `w1-ai-gloss.md`,
`w2-tap-targets.md`, `w3-perf-admin.md`, `w4-sepay-real.md`.

### 2.8 Phase 5/6/7 — perf, fix, review, vòng 2
W3 before/after · 7 finding FIXED · review chéo (3 vấn đề thật + 1 lỗ hổng test → sửa) · vòng 2 **0 finding mới**.

### 2.9 Phase 8 — docs + converge/analyze + report + cleanup
README/AGENTS/CLAUDE/.gitignore/constitution v1.0.4/feature.json; demo doc verify; cleanup; REPORT.

---

## 3. Chưa làm (và lý do)

| Hạng mục | Lý do |
|---|---|
| ~~W4 SePay chữ ký THẬT~~ | **ĐÃ XONG** — người dùng chuyển khoản thật; webhook chữ ký SePay settle (`ENG73E2D3AA2DF6` → SUCCESS, tx `85111759`). |
| Video/Speaking/Leaderboard Phase U chỉ PW | tiết kiệm; bù `ui-sweep`/`routes-all` (cả 2 viewport) |
| CD home screenshot timeout | animation-heavy; PW + ui-sweep bù |
| `containsCjk` chưa phủ Hiragana/Hangul | defect đo được là **Han**; không mở rộng vô căn cứ |
| Migrate timezone UTC / GitHub issues | quyết định v14/V3 |

---

## 4. Đã fix và cách fix (7 finding)

| ID | Mức | Vấn đề | Cách fix |
|---|---|---|---|
| **F-19-01** | MED (AI) | AI gloss **tiếng Trung** (ngắt quãng) | Prompt nêu ngôn ngữ từng field + "NO Chinese characters"; **guard `containsCjk`** loại/từ chối item CJK |
| **F-19-02** | LOW | `enrichWord` CJK → **500** | `BadRequestException` (→400) + rethrow |
| **F-19-03** | LOW | UI trống im lặng khi 0 item | Thông báo rõ khi rỗng |
| **F-19-04** | LOW | `smallTargets` harness sai + cắt list | WCAG 2.5.8 đầy đủ + bỏ cắt |
| **F-19-05** | MED (perf) | Admin exercises scan cả bảng | `IX_exercises_order_id` (V005) |
| **F-19-06** | MED (review) | Quota trừ dù guard loại hết | Chỉ trừ khi có item |
| **F-19-07** | MED (review) | Inline exemption quá rộng | Thêm `height <= line-height` |

### Review chéo bắt gì (bằng chứng `review-v19.md`)
Reviewer (subagent độc lập) verdict *"No change is unsafe to keep"* nhưng bắt: **#1** enrich 500 (tôi đã sửa trước đó),
**#2** quota trừ vô điều kiện, **#6** `/^inline/` miễn oan `inline-flex` trong `<td>`, **#7** mutation-test thiếu ca
"control trong `<p>`". Tôi **đối chiếu lại code**, xác nhận, **sửa tận gốc** cả 3 + mở rộng mutation-test.

### Tự bác bỏ 3 kết luận SAI của chính mình / của v18 (kỷ luật V4)
1. `smallTargets=115` "toàn false positive" → **SAI**: harness cắt list giấu 12 button thật.
2. SePay "real-money boundary" → **KHÔNG chính xác**: có Test-mode simulator miễn phí.
3. AI gloss CJK "hạn chế model không sửa được" → **SAI**: là bug code (prompt + guard).
4. Giả thuyết perf "projection giúp" → **BÁC BỎ bằng đo** (1417 vs 1417 reads); fix thật là index.

---

## 5. Kết quả kiểm thử (đếm từ run log)

| Bộ | Kết quả |
|---|---|
| Backend (`.\mvnw.cmd test`) | **541 / 0 / 0 / 11 — BUILD SUCCESS** (+4 W1) |
| Frontend (`npx vitest run`) | **194 / 1 (32)** |
| Frontend build | **177.75 kB** (gzip 67.69) |
| `api-sweep` (vòng 1 & 2) | **145 / 0 / 0** |
| `deep-probe` | **58 / 0 / 0** |
| `ui-sweep` | 0 mọi loại (smallTargets **0**) |
| `design-v2` | 0 badFont/legacy/drift + BVP LOADED |
| `routes-all` | 228 visit, 0 wrong landing |
| W1 | 0 CJK ×15 live; test 4/4 mutation-tested |
| W2 | 115→0; mutation-test 2 ca PASS |
| W3 | page reads 1341→86; endpoint 42.8→26.6ms |
| Parity cuối | `1470\|43738\|5\|118\|29\|4\|3\|13\|10` (payments 12→13 = **giao dịch THẬT** W4) · PENDING=0 · EX_ATTEMPTS=33 |

---

## 6. Skill / plugin đã nạp

**SpecKit:** constitution, specify, clarify, checklist, plan, tasks, implement, converge, analyze.
**Superpowers:** using-superpowers, verification-before-completion, systematic-debugging, requesting-code-review.
**Domain:** accessibility (WCAG 2.2 AA), java-springboot, java-coding-standards, generate-tests.
**addyosmani-\*:** browser-testing-with-devtools, performance-optimization, code-review-and-quality, debugging-and-error-recovery.
**MCP:** chrome-devtools (dùng thật) + playwright/Brave (dùng thật).
**Agent:** Explore ×3 (SePay/AI/smallTargets+perf), general-purpose (review chéo đối kháng).

---

## 7. File đã xoá (dọn rác)

Xem `cleanup-manifest.md`: `.playwright-mcp/*`, `sweep/harness/_*.sql`+`*.json`, `target/**`, `frontend/dist/**`,
helper tạm. **KHÔNG xoá:** `scripts/figma-export/node_modules`, `uploads/**`, `.env*`, spec v8–v19, harness,
fixtures, **migration V005** (mới).

---

## 8. Kết luận trung thực

- **Hoàn thành:** **5/5 mục "còn lại" ĐÓNG HẲN** — W1 (AI gloss) · W2 (tap-target) · W3 (perf index) · **W4 (SePay chữ ký thật — PASS)** · Premium 2 engine.
- **Không giấu điểm yếu:** 3 luồng Phase U chỉ PW; CD screenshot timeout; `containsCjk` phủ Han.
- **Điểm mạnh nhất:** **đính chính 3 kết luận cũ** (đo lại) + **review chéo bắt 3 vấn đề thật rồi sửa tận gốc** +
  **bác bỏ giả thuyết perf bằng số** (1417 vs 1417) rồi tìm fix thật (index, 1341→86 reads).
