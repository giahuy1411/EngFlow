# Báo cáo kiểm toán EngFlow — Vòng 4 (2026-09-17, 00:45–01:30 +07)

## Tổng kết

| Phép kiểm | Kết quả | Exit |
|---|---|---|
| Backend unit tests | 383/383, 0 FAIL | 0 |
| Frontend unit tests | 90/19, 0 FAIL | 0 |
| Endpoint inventory | 144 mappings / 26 controllers | 0 |
| Route inventory | 39 routes (40 source literals), CONSISTENT | 0 |
| Route sweep (228 visits) | 0 console errors, 0 API ≥400, 0 overflow, 0 bad font, 0 wrong landing | 0 |
| API functional sweep p5 | 95 probes, FAIL=0, ASSERT 60/0 | 0 |
| Design-system v2 (5 viewports × 12 routes) | 0 font, 0 token, 0 shadow, 0 overflow, 0 lucide, 0 mobile, 0 landing | 0 |
| DB parity | 1471\|43737\|76\|127\|28\|15\|4\|126\|14\|5 = baseline | — |
| DB integrity | orphan=0, listening=9, empty_answers=4848, published=1465/1471 | — |

## Finding mới phát hiện trong vòng này

### F-R4-01: `cleanupAuditPayments()` dùng UTC date — no-op 00:00–06:59 VN (HIGH)

**Đo bằng thực nghiệm**: Chạy `routes-all.js` lúc 01:08 +07; cleanup tính `d = "2026-09-16"` (UTC) trong khi row tạo `created_at = "2026-09-17"` (naive VN). DELETE khớp 0 row → parity 130 vs 126 → PARITY MISMATCH (harness đúng bắt được, exit 1).

**Vấn đề phụ**: bug này chỉ xuất hiện 00:00–06:59 VN (UTC date ≠ VN date). Ban ngày UTC = VN → "đúng vì sai cách". Bug không thể tái hiện trong các vòng trước vì chạy ban ngày.

**Sửa**: Thêm helper `vnDate()` dùng `Date.now() + 7h` (đã verify trên máy này), đổi DELETE từ `CAST(created_at AS date) = d` thành `created_at >= d + ' 00:00:00'` (half-open range, chống vắt qua midnight). Thêm positive control `AUDIT_CANDIDATES` marker.

**A/B chứng minh**: `routes.js admin` trước fix → leak 126→127, không cleanup. Sau fix → `candidates=1 after=126 PARITY OK`, exit 0.

**File**: `sweep/v8/ui/lib.js`

### F-R4-02: `ui/routes.js` không tự dọn payment rows (HIGH)

**Đo bằng thực nghiệm**: `ui/routes.js admin` visits `/premium/checkout` (trong AUTH list) → PremiumCheckout.vue gọi `POST /api/v1/payment/create-order` → row thật. Parity trước: 126, sau: 127. Không có `cleanupAuditPayments()` gọi.

**Bắt nguồn**: harness cũ đã được viết trước khi `cleanupAuditPayments` tồn tại, không bao giờ được update.

**Sửa**: Thêm cleanup + parity assert. Wrap toàn bộ logic trong `try/finally` để cleanup chạy kể cả khi write JSON crash. Sửa path ghi file từ `"ui/routes-..."` (CWD-relative) sang `__dirname + "/routes-..."` (file-relative), vì ENOENT trên write đã crash trước khi cleanup có cơ hội chạy.

**File**: `sweep/v8/ui/routes.js`

### F-R4-03: `design-v2.js` assert vào token CSS không tồn tại (MEDIUM)

**Phát hiện**: `["--geo-font", "--geo-cream", "--geo-shadow"]` —只有 `--geo-font` tồn tại. `getPropertyValue` trả `""` cho token không khai báo, filter loại bỏ → phép kiểm rỗng: "3 tokens checked" thực ra chỉ check 1.

**Sửa**: Thay bằng 11 token names thật (`--geo-bg`, `--geo-fg`, `--geo-accent`, ..., `--geo-border-width`). Thêm assertion giá trị exact match: bg=#FFFDF5, fg=#1E293B, accent=#8B5CF6, secondary=#F472B6, tertiary=#FBBF24, quaternary=#34D399, border=#E2E8F0, radiusMd=16px, borderWidth=2px, shadowMd=4px 4px 0px 0px #1E293B.

**File**: `sweep/v8/ui/design-v2.js`

### F-R4-04: `design-v2.js` icon stroke assertion so sánh string thay vì numeric (MEDIUM)

**Phát hiện**: `getComputedStyle(svg).strokeWidth` trả `"2.5px"`, assertion `sw !== "2.5"` → 750/750 false-positive vi phạm. Phân tích sâu hơn: attribute `stroke-width="2"` (lucide-vue-next default) bị CSS `.lucide { stroke-width: 2.5 }` override → computed = 2.5. Harness đọc attribute sẽ sai; harness đọc computed cần `parseFloat`.

**Sửa**: Dùng `parseFloat(getComputedStyle(sv).strokeWidth) !== 2.5`. Ghi chú rõ trong code tại sao không đọc attribute.

**File**: `sweep/v8/ui/design-v2.js`

### F-R4-05: `design-v2.js` ghi output theo CWD (LOW, defensive)

**Sửa**: Đổi `writeFileSync("design-v2.json"...)` thành `writeFileSync(__dirname + "/design-v2.json"...)` — consistent với v3.js/v3b.js, chống lỗi khi chạy từ thư mục sai.

### F-R4-06: `routes.js` ghi output theo CWD — ENOENT crash khi chạy từ sweep/v8/ui (LOW, defensive)

**Đo**: Chạy `routes.js` từ `sweep/v8/ui` tạo path `ui/ui/routes-admin-1440.json` — parent không tồn tại → ENOIT.

**Sửa**: Đổi thành `__dirname + "/routes-..."`. Kết hợp F-R4-02 (`try/finally`) đảm bảo crash không bao giờ bỏ qua cleanup.

**File**: `sweep/v8/ui/routes.js`

## Verified design-system sync (mạnh — computed-style từ Chromium thật)

| Yêu cầu prompt | Bằng chứng | Trạng thái |
|---|---|---|
| Font: Be Vietnam Pro 400/500/600/700/900 | `document.fonts.check` + faces=15 | ✅ |
| Font: không có Outfit/Plus Jakarta/Inter/Roboto/Poppins | legacy-family hits=0 across 7685 elements × 60 route×viewport | ✅ |
| Colors: bg #FFFDF5, fg #1E293B, accent #8B5CF6, secondary #F472B6, tertiary #FBBF24, quaternary #34D399 | 0 token value drift across 60 rows | ✅ |
| Border: 2px default | borderWidth=2px | ✅ |
| Radii: 8/16/24/9999px | --geo-radius-md=16px | ✅ |
| Shadows: hard offset 4px 4px 0 #1E293B, hover 6px, active 2px | --geo-shadow-md exact match, 0 drift | ✅ |
| Icon: Lucide stroke 2.5 | 750/750 parseFloat(computed)=2.5 | ✅ |
| Overflow: 0 horizontal at all widths | 0/60 routes overflow | ✅ |
| Mobile: shadow 2px pop | 0 mobile-shadow-fail violations | ✅ |
| Landing guards: all roles correct | 0 wrong landing | ✅ |
| prefers-reduced-motion | 2 @media blocks (geo + app primitives) | ✅ |

## Design-system.css section inventory vs prompt

| Prompt section | Code | Trạng thái |
|---|---|---|
| CSS Variables (--geo-*) | 40 tokens | ✅ |
| Buttons (geo-btn-*) | Sizes, variants, hover/active offsets | ✅ |
| Cards (geo-card) | Floating icon, interactive, responsive | ✅ |
| Inputs (geo-input) | Focus ring, error state | ✅ |
| Badges (geo-badge) | Color variants | ✅ |
| Modals (app-modal) | Overlay, animation | ✅ |
| Skeleton (app-skeleton) | Shimmer keyframe | ✅ |
| Empty states (app-empty) | — | ✅ |
| Dot grid / polka | radial-gradient dots | ✅ |
| Squiggle divider | SVG repeat-x | ✅ |
| Confetti | — | ⚠️ Không tìm thấy trong CSS |
| Diagonal stripes | Không có code, chỉ prompt-vs-code divergence (G4 đã ghi nhận) | ⚠️ Known |
| Marquee (geo-marquee) | @keyframes marquee + CSS | ✅ |
| Animations: bounce/wiggle/float/pop-in | All present | ✅ |
| Easing: cubic-bezier(0.34,1.56,0.64,1) | 6 usages | ✅ |
| prefers-reduced-motion | 2 blocks, covers transforms + scroll | ✅ |

## Kết luận

- **0 bugs mới trong app code** — tất cả findings trong vòng này thuộc harness (sweep infrastructure), không thuộc production code.
- **Design-system sync: PASS** — computed-style verification across 7,685 elements, 60 route×viewport combos, 5 viewports, 0 violations.
- **API functional: PASS** — 95 probes, 0 fail, self-clean parity verified.
- **DB integrity: PASS** — all metrics match baseline exactly.
- **Baseline drift documented**: backend 383 (was 378, +5 new tests in working tree), frontend 90/19 (was 89/18, +1 Profile.test.js). Both from uncommitted work, not regressions.

## Skills đã nạp trong phiên

| Skill | Lý do |
|---|---|
| `speckit-workflow` | Workflow pipeline constitution→report |
| `speckit-analyze` | Cross-artifact consistency |
| `speckit-converge` | Gap-finding |
| `speckit-constitution` | Project rules |
| `gpt-taste` | UX quality baseline |
| `brandkit` | Design system context |
| `editing-cordis-compositions` | Plugin authoring (if needed) |
| `generate-test-cases` | Test coverage |
| `generate-tests` | Test generation |
| `accessibility` | WCAG audit |
| `java-coding-standards` | Backend conventions |
| `java-docs` | Javadoc standards |
| `java-springboot` | Spring Boot patterns |
| `design-taste-frontend` | Anti-slop frontend |
| `design-taste-frontend-v1` | Legacy baseline |
| `redesign-existing-projects` | Design-system migration |
| `seo` | SEO audit |

## Files đã sửa trong vòng này

| File | Thay đổi |
|---|---|
| `sweep/v8/ui/lib.js` | Thêm `vnDate()`, `VN_RUN_DATE`; sửa `cleanupAuditPayments` dùng VN date + range + AUDIT_CANDIDATES marker; export mới |
| `sweep/v8/ui/routes.js` | Thêm cleanup in `try/finally`; sửa output path dùng `__dirname` |
| `sweep/v8/ui/design-v2.js` | Sửa token assertion (11 names thật); thêm value drift check; sửa lucide stroke `parseFloat`; thêm mobile shadow; thêm cleanup; output dùng `__dirname` |
