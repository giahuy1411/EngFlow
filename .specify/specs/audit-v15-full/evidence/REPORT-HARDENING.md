# audit-v15 hardening — BÁO CÁO

**Ngày:** 2026-09-25 (+07) · **Nhánh:** `audit-v15-full` · **Nguồn:** mục "Điểm yếu đã ghi rõ" của REPORT v15
**Mục tiêu:** sửa **tận gốc 3 lớp** điểm yếu harness, không chỉ các ca đã gặp.

---

## 1. Điểm yếu → đã sửa tận gốc

| Lớp | Cơ chế (đo được) | Ca còn sống | Cách sửa |
|---|---|---|---|
| **L1** probe ghi dữ liệu thật, không dọn | route ghi-khi-mount / endpoint ghi, không cleanup hoặc không assert | **4** | `assertClean()` + gọi trong `finally` + dọn `study_days` + static check |
| **L2** harness dựng lại từ snapshot cũ → fix mất | `sweep/v1N` gitignored + xoá cuối vòng, **chưa từng commit** | cơ chế | `sweep/harness/` **được commit** + `_config.js` tham số hoá |
| **L3** harness tracked/docs trỏ thứ đã gỡ | 10 file tracked không được kiểm khi schema đổi | **8** | `HarnessDriftTest` (JUnit) + `assert-harness.js` (node) |

---

## 2. Đã làm

### L2 — harness được commit (root-fix)

- **Bằng chứng mất fix:** `git ls-files sweep/v15/` = **0**; `git log --all -- sweep/v15/*` rỗng →
  7 fix của v15 **mất vĩnh viễn**. Nguồn dựng lại = `a627569:sweep/v13` (**trước** fix v14).
  `evidence/harness-restore.md` của v15 = **0 byte** → không truyền được gì.
- **Giải pháp:** suite ở `sweep/harness/` (**14 file source** được track), **tracked** qua `.gitignore` (`sweep/*` +
  `!sweep/harness/`; output runtime vẫn ignored). `_config.js`:
  `node sweep/harness/<probe>.js [--audit <tên>] [--out <dir>]` (mặc định `audit-v15-full`).
- `harness-restore.md` giờ **3.473 ký tự** (nguồn + danh sách fix); `assert-harness.js` fail nếu rỗng.

### L1 — ép dọn residue

| Việc | Chi tiết |
|---|---|
| `study_days` vào parity | `p16-parity.sql` phát marker `STUDY_DAYS=` (giữ nguyên 9-số cho baseline) |
| `PENDING_PAYMENTS=` | marker cho đơn chưa thanh toán |
| `assertClean()` | trong `sweep/v8/ui/lib.js`; **throw** nếu parity/study_days/PENDING lệch |
| `routes-all.js` | cleanup chuyển vào **`finally`** (trước: happy-path → throw là rò row) |
| `design-v2.js` | exit code **gate theo `clean.ok`** (trước: tính rồi bỏ) |
| `design.js` | **thêm cleanup** (trước: không có gì) |
| `ui-sweep.js` | cleanup chuyển vào **`finally`** (trước: sau `browser.close()` trên happy-path → throw là rò row); thêm nhánh `fatal` → exit 2 nhưng **vẫn dọn** |
| `v12/api-sweep.js` | dọn `study_days` tài khoản probe trong ngày chạy; cửa sổ ngày động |

**Ca residue thật đã tìm & dọn (H0):** `study_days` id=**30069** (user 2, 2026-09-25) — v14 kết thúc ở
**4**, hàng này làm thành **5**, và v15 đã ghi "study_days 5" **như thể là baseline**. Đã xoá theo ID
liệt kê → về **4**. Cơ chế: 5 service gọi `recordStudy`; `p16-parity.sql` không đếm; `recordStudy`
idempotent nên residue **capped 1 hàng/user/ngày** → dễ bị nhầm là baseline.

### L3 — drift guard 2 nơi + sửa 8 lỗi

| # | File | Vấn đề | Fix |
|---|---|---|---|
| a/b/c | `routes.js:88`, `routes-all.js:257`, `design-v2.js:215` | baseline 10-số cũ | `H.PARITY_BASELINE` |
| d | `lib.js:166` | comment shape 10 số | sửa |
| e | 3 file UI | `cleanupAuditPayments(126)` | `H.PAYMENTS_BASELINE` (=12) |
| f | `routes-all.js:20-22` | đọc file **untracked+gitignored** → hỏng trên clone | sinh từ router |
| g | `AGENTS.md:25` | ghi `p1–p5` + `ui/v3.js` (không tồn tại) | inventory thật |
| h | `v12/api-sweep.js:570` | ghi vào `audit-v12-full/` → **ghi đè artifact lịch sử** | `--out` mặc định audit hiện tại |
| i | `ui-sweep.js:78` | còn sweep route `/admin/:id/build` (đã gỡ cùng Đường B) → **2 guard FAIL** | expect về `/` (catch-all), khẳng định route đã biến mất |

Thêm: `CLAUDE.md:56` bỏ `BlockType`; `AGENTS.md` cập nhật inventory + baseline (515 test).

> **L3-i là ca do chính hardening phát hiện:** `ui-sweep.js` chạy ra `guardFails=2` ở `/admin/445/build`.
> Route này đã bị gỡ cùng Lesson Builder; catch-all `/:pathMatch(.*)*` giờ redirect về `/`. Sửa expect
> thành `"/"` cho cả 3 role → `guardFails=0`. (Guard static ở §2 không bắt được vì entry là `APP + rt.p`
> — ghép động — đúng giới hạn đã ghi ở §5.)

**Guard mới:**
- `src/test/java/com/datn/engflow/HarnessDriftTest.java` (3 test, chạy trong `mvnw test`):
  endpoint harness gọi ⊂ controller; bảng harness đọc ⊂ entity `@Table`; docs không liệt kê symbol đã xoá.
- `sweep/harness/assert-harness.js` (5 check): route browser ⊂ router; `require()` resolve; không
  hardcode baseline cũ; **harness ghé route ghi-khi-mount phải gọi cleanup** (static half của C2/H3.5);
  `harness-restore.md` không rỗng.
- `sweep/harness/routes-from-router.js`: sinh route + guard từ router (thay file untracked).

---

## 3. Bằng chứng 2 CHIỀU (bắt buộc)

| Guard | Chèn drift | Gỡ drift |
|---|---|---|
| `HarnessDriftTest` (endpoint) | **FAIL** `search-sort.js:11 -> /api/admin/lessons/445/snapshots/restore` | **PASS** 3/3 |
| `HarnessDriftTest` (bảng) | **FAIL** `sweep/v8/p16-parity.sql -> FROM lesson_snapshots` | **PASS** 3/3 |
| `assert-harness.js` (route) | **FAIL** `deep-probe.js -> /admin/445/build`, exit **1** | exit **0** |
| `assert-harness.js` (baseline) | **FAIL** `search-sort.js:10 stale parity literal`, exit **1** | exit **0** |
| `assertClean` | **throw** `study_days 4 != expected 99` | `CLEAN` |
| `ui-sweep.js` guard | **FAIL** `/admin/445/build -> /` (route đã gỡ) | `guardFails=0` |
| `assert-harness.js` (cleanup) | gỡ cleanup khỏi `design.js` → **FAIL** `design.js`, exit **1** | khôi phục → **PASS**, exit **0** |

---

## 4. Kết quả chạy lại (mọi harness tracked)

| Harness | Kết quả |
|---|---|
| `HarnessDriftTest` | **3/3 PASS** (cây sạch) |
| `assert-harness.js` | **ALL CLEAN** (5/5) |
| `sweep/v8/ui/routes.js admin` | 38 route, **PROBLEM=0**, cleanup `1→0`, `assertClean CLEAN` |
| `sweep/v8/ui/routes-all.js` | **228 visits** (38×2×3), 0 console error, 0 not-mounted, **wrong landing 0**, 52 anon-on-guarded đều bị đẩy, `assertClean CLEAN` |
| `sweep/v8/ui/design-v2.js` | exit 0, font loaded (18 faces), **0 wrong page**, CLEAN |
| `sweep/v8/ui/design.js` | exit 0, **giờ có cleanup** (trước không), CLEAN |
| `sweep/harness/ui-sweep.js` | **guardFails=0** (sau L3-i), 0 console error, 0 api error, cleanup `2→0`, `assertClean CLEAN`, exit **0** |
| `sweep/v12/api-sweep.js` | **143 pass / 0 fail**, `AUDIT_STUDY_DAYS=0`, ghi vào `audit-v15-full` (**không** ghi đè v12) |
| `sweep/harness/deep-probe.js` | **58 pass / 0 fail** |
| `sweep/harness/f1302-a1-blocks-live.js` | **18/18** (Đường B vắng ở cả 8 lesson) |
| `sweep/harness/f1302-a1-a11y.js` | **5/5** (63 text node) |
| `sweep/harness/f1320-api-redirect-live.js` | **4/4** |
| `sweep/harness/{api-inventory,perf,cls,search-sort,danger-tint,focused-probe}` | OK; perf/cls ghi vào evidence, **`sweep/v13` không còn được tạo** |
| Backend suite | **515 / 0 fail / 0 error / 11 skipped** (512 + 3 `HarnessDriftTest`) |
| Frontend suite | **178 passed / 1 skipped (29 file)** (không đổi) |
| Parity cuối | `1470\|43738\|5\|118\|29\|4\|3\|12\|10` · **STUDY_DAYS=4** · **PENDING_PAYMENTS=0** |

---

## 5. Chưa làm / giới hạn

- **Adversarial verify bằng Workflow KHÔNG chạy được (credit hết).** Đã author workflow 5 reviewer
  (`tmp/h/verify-hardening.mjs`: L2-mechanism, JUnit-guard, residue-assert, ui-harness-cleanup,
  api-sweep-docs) + 3 lens phản biện mỗi finding, nhưng **cả 5 agent lỗi
  `503 [402] insufficient_credits`** ("Insufficient balance … $-0.06") từ inference gateway. Workflow trả
  `{"confirmed":[],"rejectedCount":0}` — nghĩa là **KHÔNG có verification nào chạy**, KHÔNG phải "không có
  defect". Mọi kết luận trong báo cáo này dựa trên **đo trực tiếp 2 chiều** ở §3, không dựa vào workflow đó.
  ⇒ Việc verify độc lập vẫn **còn nợ**; nên chạy lại khi có credit.
- `HarnessDriftTest` dùng regex đọc source (không compile Java/JS). Nó **bắt đúng lớp lỗi đã gặp** (trỏ
  endpoint/bảng/route đã gỡ) nhưng không phải parser đầy đủ; đã thử 2 chiều để chắc không false-positive.
- `assert-harness.js` check route chỉ nhìn navigation literal (`APP + "..."`, `goto(... "..."`) — một route
  ghép động hoàn toàn sẽ không bị kiểm. **Đã gặp đúng giới hạn này:** L3-i (`ui-sweep.js` entry
  `/admin/${L}/build` là `APP + rt.p`) lọt qua static check, chỉ lộ khi chạy thật. Ghi rõ thay vì che.
- Chưa đưa guard vào CI (repo **không có** `.github/workflows`); hiện chạy qua `mvnw test` + lệnh tay.

## 6. File

- **Mới:** `sweep/harness/**` (**14 file source**, đã `git add`), `src/test/java/com/datn/engflow/HarnessDriftTest.java`,
  `sweep/harness/routes-from-router.js`, `sweep/harness/assert-harness.js`, `sweep/harness/_config.js`.
- **Sửa:** `sweep/v8/p16-parity.sql`, `sweep/v8/ui/lib.js`, `sweep/v8/ui/{routes,routes-all,design,design-v2}.js`,
  `sweep/v12/api-sweep.js`, `.gitignore`, `AGENTS.md`, `CLAUDE.md`.
