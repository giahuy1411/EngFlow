# Báo cáo audit-v11-full (làm lại từ đầu)

**Ngày:** 2026-09-20 → 21 (+07) · **Cập nhật:** Phase I · **Nhánh:** `audit-streak-review` · **Checkpoint:** `dae9667`
**Phương pháp:** mọi con số trong báo cáo này do **chính phiên này đo**, không sao chép từ audit-v10 hay
từ `evidence/baseline.md` cũ. Chỗ nào trùng với báo cáo trước thì ghi là **xác nhận**; chỗ nào lệch thì
ghi rõ là phát hiện.

---

## 0. TL;DR — điều quan trọng nhất

1. **Yêu cầu "thay toàn bộ font thành Be Vietnam Pro" KHÔNG cần làm gì cả — đã xong từ trước.** Đo trên
   DOM thật: toàn bộ trang chỉ có **một** font duy nhất là `"Be Vietnam Pro"`, không còn Outfit/Plus
   Jakarta ở đâu trong `frontend/src`. Báo cáo nó như "việc đã làm" sẽ là bịa.
2. **Design system "Playful Geometric" cũng đã được triển khai đầy đủ** — token, shadow cứng, border 2px,
   palette cream/violet/pink/amber/emerald.
3. **Tìm được 14 lỗi thật** (F129–F142), trong đó **2 lỗi HIGH/MEDIUM ảnh hưởng người dùng thật**:
   - **F133 (HIGH)** — một lần `/api/auth/me` trả 429/500 là người dùng **bị đăng xuất âm thầm**, mất
     phiên, không thông báo.
   - **F132 (MEDIUM)** — **165 text node** vi phạm WCAG AA trên 19 route (chữ trắng trên nền hồng chỉ
     **2.65:1**). Đã sửa về **0**.
4. **Falsify 10 ứng viên lỗi** — chúng là lỗi của **probe**, không phải của app. Ghi lại đầy đủ để không
   ai "sửa" code đúng.
5. **Vòng 2 tìm ra lỗi mà vòng 1 đã bỏ sót** (F138) vì probe vòng 1 tính sai nền composite → đây là lý do
   thật sự phải chạy vòng 2.
6. **Parity không đổi, không còn rác:** `1471|43735|72|127|28|15|4|126|14|5`.
7. **4 hạng mục từng bị hoãn đã làm xong ở Phase I** — và khi kiểm tra lại, **3 trong 4 lý do hoãn là SAI**
   (AI pipeline chạy 12.4 s chứ không phải "vài phút"; secret SePay đã có sẵn; công thức test ghi âm đã
   có trong `AGENTS.md`). Bài học: **lý do hoãn cũng là một giả thuyết, phải đi kiểm chứng.**

---

## 1. Đã làm gì

### Phase A — Đo lại baseline từ đầu
| Hạng mục | Kết quả (đo thật) |
|---|---|
| Backend `mvnw.cmd -o test` | **483 run / 0 fail / 0 error / 11 skipped — BUILD SUCCESS** |
| Frontend `npx vitest run` | **21 passed / 1 skipped (22 files) · 109 passed / 1 skipped** |
| Build `npx vite build` | entry **177.31 kB** (gzip 67.50 kB) |
| Container | 8 container đang chạy; backend **không stale** (0 file `.java` mới hơn container) |
| Endpoint inventory | **131 annotation / 26 controller** (đếm lại từ source) |

→ `evidence/baseline-rerun.md`

### Phase B — Audit DB trong Docker (read-only)
26 FK (tất cả enabled), **0 orphan**, 0 trùng khoá, streak schema đã deploy đúng (`effective_from`
= 2026-09-20), fragmentation không đáng kể (11.57% trên index 121 page → **từ chối tối ưu, có ghi lý do**).

→ `evidence/db-audit-rerun.md`

### Phase C — Quét toàn bộ API trên container thật
**74 pass / 0 fail / 0 blocked** trên 6 vùng: Auth, Lessons/Exercises, Streak, Search/Sort, CRUD, AI —
cộng bảng phân quyền 18 assert **cả hai chiều** (admin được phép / student bị chặn thật).

→ `evidence/api-sweep-rerun.md`

### Phase D — Quét UI bằng chrome-devtools MCP + playwright MCP
78 lượt route×role, guard assert **hai chiều**; 0 console error; 0 API lỗi do UI; responsive 5 kích
thước; a11y; contrast.

→ `evidence/ui-sweep-rerun.md`

### Phase E — CRUD qua UI admin (đóng T4.10 mà v10 bỏ dở)
Create → hiện trong UI → Update → sửa hiện trong UI → Delete → 404 → mất khỏi UI. **0 rác.**

→ `evidence/ui-crud-rerun.md`

### Phase F — Sửa lỗi (chi tiết ở §3)
### Phase G — Hiệu năng, đo trước/sau
12/13 endpoint < 50 ms. Endpoint chậm nhất (`admin exercise search` 169.3 ms) đã truy gốc và **đo thử
phương án tối ưu rồi từ chối, có ghi số**.

→ `evidence/performance-rerun.md`

### Phase H — Vòng 2 toàn diện
→ `evidence/second-pass-rerun.md`

### Phase I — Làm nốt 4 hạng mục từng bị hoãn (sau khi kiểm tra lại lý do)
AI pipeline end-to-end (**8/8 + 6/6**, 12.4 s / 30.2 s) · SePay webhook chữ ký HMAC thật (**10/10**) ·
ghi âm speaking với mic giả (**11/11**) · F130 sửa ở hàm dùng chung (**đã chứng minh bằng test**).
Kèm theo: **F139** (`<h1>`), **F140** (tap target), **F141** (sitemap), **F142** (link chỉ khác màu).

→ `evidence/phase-i-deferred-items.md`

---

## 2. CHƯA làm gì (ghi rõ, không giấu)

> **Cập nhật sau Phase I.** Ban đầu mục này liệt kê 6 hạng mục. Khi được hỏi **lý do**, tôi đi **kiểm tra**
> từng cái thay vì nhắc lại lý do — và **3 trong 4 lý do không đứng vững**:

| Hạng mục ban đầu tôi hoãn | Lý do tôi đưa ra | Đo lại thì thấy |
|---|---|---|
| Chạy full pipeline sinh bài AI | "gọi Ollama, mỗi batch vài phút" | Ollama **đang chạy**, cả 2 model đã nạp; pipeline xong trong **12.4 s** (1 loại) / **30.2 s** (tất cả loại) |
| Webhook SePay ký HMAC thật | "chưa post webhook ký thật" | `SEPAY_WEBHOOK_SECRET` **đã được set**; ký được từ đầu |
| Ghi âm speaking end-to-end | "cần mic giả + object MinIO" | Công thức đã **có sẵn trong `AGENTS.md`**, `playwright-core` đã cài |
| 6 call-site harness | "file ngoài phạm vi" | Đúng một phần — nhưng **hàm dùng chung** sửa được, sửa 1 chỗ là xong cả 6 |

**Cả bốn giờ đã LÀM XONG** (Phase I) — xem `evidence/phase-i-deferred-items.md`.

### Còn lại thật sự chưa làm

| Hạng mục | Trạng thái | Lý do |
|---|---|---|
| Giao dịch ngân hàng thật với SePay | **CHƯA** | Là hành động thanh toán thật, không phải test. Đã kiểm **phía nhận**: chữ ký đúng/sai, cửa sổ replay, settle, kích hoạt premium, idempotent |
| Chấm điểm phát âm bằng giọng thật | **CHƯA** | Mic giả phát **im lặng** → Whisper trả text rỗng → `FAILED` **đúng thiết kế**. Cần giọng thật để chấm điểm |
| Tối ưu `admin exercise search` (F137) | **TỪ CHỐI, có số** | Bỏ `LOWER()` chỉ tiết kiệm ~19 ms CPU; logical reads y hệt (1294) → plan không đổi. Sửa thật là đổi ngữ nghĩa tìm kiếm = quyết định sản phẩm |
| `?sort=` không có tác dụng | **KHÔNG SỬA** | **Khoảng trống sản phẩm**, không phải lỗi — UI không hề có nút sắp xếp |

## 3. Đã fix gì và fix thế nào

| ID | Mức | Vấn đề | Gốc rễ | Cách fix | Đo lại |
|---|---|---|---|---|---|
| **F129** | MED | Logo footer vô hình (tỉ lệ 1.00) | `.app-logo` hard-code `color: var(--geo-fg)`, footer đảo nền | `color: inherit` | **14.36:1** ✓ |
| **F130** | MED | Baseline parity bị nhiễm rác (ghi 126, thật 127) | `/premium/checkout` tạo row lúc mount, rơi vào cửa sổ đo baseline | Xoá 2 row rác bằng id liệt kê + `SET QUOTED_IDENTIFIER ON` | 128 → **126**, `Msg`=0 |
| **F131** | MED | `/premium` tràn ngang 360→880px (+8px, +28px ở 768px) | `.app-plan__badge` `right:-1.25rem` **+** `rotate(15deg)` phình bounding box; `scale(1.1)` ở `md` làm card rộng thêm ~17px/side | badge `-0.25rem` ở mobile; grid `md:px-8` | **0 ở mọi width** ✓ |
| **F132** | MED | **165** text node vi phạm WCAG AA | Prompt bắt dùng `secondary`/`tertiary`/`quaternary` cho "chữ nhấn mạnh", nhưng các màu đó chỉ đạt 1.64–2.65:1 | Thêm tầng token **ink/strong**; màu vivid giữ nguyên cho fill/hình khối | **165 → 0** ✓ |
| **F133** | **HIGH** | 429/500 từ `/api/auth/me` → **đăng xuất âm thầm** | `catch { logout() }` bắt **mọi** lỗi | Chỉ logout khi 401/403 | 500/429 **giữ phiên**, 401 **vẫn logout** ✓ |
| **F134** | LOW | Test assert đúng token mà F132 đổi | Test ghim `text-accent` | Cập nhật + ghi lý do | 109 passed ✓ |
| **F135** | LOW | `<h3>` nhảy cấp dưới `<h1>` | Sai thứ bậc heading | `<h3>` → `<h2>` | Lighthouse heading-order ✓ |
| **F136** | LOW | Nút phân trang vi phạm WCAG 2.5.3 | Nhãn `"Trang đầu tiên"` không chứa chữ hiển thị `"1"` | Nhãn bắt đầu bằng số hiển thị | Lighthouse 98 → **100** ✓ |
| **F137** | LOW | Search admin 169.3 ms | `LOWER()` + wildcard đầu → full scan 43 735 row | **TỪ CHỐI tối ưu, có số** — bỏ `LOWER()` chỉ tiết kiệm ~19 ms CPU, logical reads y hệt | Ghi lý do |
| **F138** | MED | 13 text node `--geo-muted-fg` 4.00–4.27:1 | Token `#64748B` đạt 4.76:1 trên nền trắng nhưng **4.34:1 trên nền muted** — nơi nó được dùng chủ yếu | Đổi token → `#556070` (6.38/6.26/5.82:1) | **13 → 0** ✓ |
| **F139** | LOW | 3 route auth không có `<h1>` | Tiêu đề card là `<h2>` trên route cấp cao nhất | Nâng thành `<h1>` | Lighthouse **94 → 100** ✓ |
| **F140** | LOW | 5 link "Xem" 30×15px ở `/admin/videos` | Link inline nhỏ hơn ngưỡng 24px (WCAG 2.5.8) | `inline-flex items-center min-h-6` | **5 → 0** ✓ |
| **F141** | LOW | `robots.txt` trỏ tới `sitemap.xml` không tồn tại | File chưa từng được tạo → crawler nhận 404 | Tạo `sitemap.xml` từ các route public | File tồn tại ✓ |
| **F142** | LOW | Link trong khối chữ chỉ khác **màu** (1.11:1) | `hover:underline` → gạch chân chỉ hiện khi hover (WCAG 1.4.1) | `underline underline-offset-2 hover:no-underline` | Lighthouse pass ✓ |

### Điểm quan trọng nhất về F132 (fix lớn nhất)
Không thể sửa bằng cách đổi hex gốc, vì **cùng màu vivid đó đang ĐÚNG ở chỗ khác**: chữ đậm trên nền
amber = **8.76:1**, trên emerald = **7.61:1**. Đổi hex gốc sẽ phá đúng những chỗ đang tốt. Vì vậy tách
thành 2 tầng:
- màu **vivid** → nền, viền, hình khối, tint `/10`–`/30` (giữ nguyên);
- **`-ink`** → chữ trên nền sáng; **`-strong`** → nền dưới chữ trắng.

### Hai lỗi do chính tôi gây ra khi sửa, và đã sửa lại
Ghi vào báo cáo vì đây là phần trung thực của công việc:
1. Codemod đổi `text-tertiary` → `text-tertiary-ink` **trong sidebar admin nền tối** — ở đó màu amber
   vivid mới đúng. Đã revert 5 chỗ.
2. Codemod đổi `bg-secondary` → `bg-secondary-strong` trên chip độ khó — chip đó dùng **chữ đậm**
   (5.52:1, đang đạt). Đã revert.

Cả hai bị bắt nhờ **đo lại**, không phải nhờ tin vào codemod.

---

## 4. Bằng chứng

| File | Nội dung |
|---|---|
| `evidence/baseline-rerun.md` | Baseline đo lại (2 sai lệch với bản cũ ghi rõ) |
| `evidence/db-audit-rerun.md` | Audit DB, kèm 3 lỗi probe tự phát hiện |
| `evidence/api-sweep-rerun.md` | 74/0/0, 6 vùng chức năng |
| `evidence/ui-sweep-rerun.md` | Route×role, a11y, responsive, UI↔API |
| `evidence/ui-crud-rerun.md` | CRUD qua UI admin (đóng T4.10) |
| `evidence/performance-rerun.md` | Trước/sau, Lighthouse |
| `evidence/second-pass-rerun.md` | Vòng 2 + giới hạn |
| `evidence/probe-artifacts.md` | **10 ứng viên bị falsify** |
| `findings-rerun.md` | F129–F138 đầy đủ |
| `sweep/v11/*` | Probe, SQL, codemod, ảnh |

**Trạng thái cuối:** backend **483/0/0**, frontend **109/1 skipped**, build **177.40 kB**, parity
`1471|43735|72|127|28|15|4|126|14|5`, **0 rác**, Lighthouse Accessibility **100**.

---

## 5. Skill đã nạp trong phiên này

| Skill / plugin | Dùng để làm gì |
|---|---|
| `superpowers:using-superpowers` | Khung bắt buộc: nạp skill trước khi hành động |
| **`accessibility`** | WCAG 2.2 — nền tảng cho F132/F135/F136/F138: ngưỡng 4.5:1 vs 3:1, 2.5.3 Label in Name, 2.5.8 target size |
| **`frontend-design`** | Kiến trúc tầng token ink/strong thay vì đổi hex gốc |
| `speckit-*` (workflow) | Pipeline constitution → specify → clarify → checklist → plan → tasks → implement → converge → analyze |
| `chrome-devtools-mcp:*` | Driver 1: `list_pages`, `navigate_page`, **`lighthouse_audit`** (Accessibility 100) |
| `playwright` (MCP) | Driver 2: route×role sweep, contrast/a11y/responsive probe, CRUD UI, thực nghiệm 3 nhánh F133 |
| `superpowers:verification-before-completion` | Mọi tuyên bố phải có artifact đứng sau |
| `superpowers:systematic-debugging` | Truy gốc rễ từng lỗi thay vì vá triệu chứng |
| `security-guidance` | (kế thừa từ v10) `execFileSync` thay `execSync` cho Redis key |

---

## 6. Điều cần biết

1. **Vòng 2 không phải thủ tục.** Nó tìm ra **F138** — lỗi mà vòng 1 đã báo "sạch" — vì probe vòng 1
   tính sai nền composite. Nếu bỏ vòng 2, 13 text node vi phạm AA sẽ còn nguyên.
2. **`--geo-muted-fg` là bài học token:** một token đạt AA trên nền trắng vẫn có thể **trượt** trên nền
   mà nó thực sự được dùng. Kiểm token phải kiểm trên nền thật của nó.
3. **F133 là lỗi đáng lo nhất về sản phẩm:** người dùng mạng chập chờn, hoặc dùng chung IP bị rate-limit,
   sẽ bị đăng xuất giữa chừng mà không hiểu vì sao. Đã sửa và kiểm bằng thực nghiệm có nhánh đối chứng.
4. **Còn 3 việc mức thấp chưa sửa** (tap-target `/admin/videos`, `<h1>` `/login`, 6 call-site harness) —
   ghi ở §2, không giấu.
5. **Working tree chưa commit.** Toàn bộ thay đổi của phiên này (40 file `.vue` + token + 3 file test)
   vẫn đang ở dạng chưa commit; checkpoint gần nhất là `dae9667`.
