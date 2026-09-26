# audit-v15 hardening — bịt 3 lớp điểm yếu harness

**Ngày:** 2026-09-25 (+07) · Nhánh `audit-v15-full` · Nguồn: người dùng dán lại mục "Điểm yếu" của REPORT v15

## Vấn đề

Báo cáo v15 tự ghi 3 điểm yếu. Điều tra phiên này (2 Explore agent + đo trực tiếp) cho thấy chúng là
**3 LỚP lỗi có cơ chế chung**, và mỗi lớp còn nhiều ca chưa sửa — không chỉ ca đã gặp.

| Lớp | Cơ chế | Ca còn sống đo được |
|---|---|---|
| **L1** probe ghi dữ liệu thật, không dọn | harness ghé route ghi-khi-mount / gọi endpoint ghi rồi không cleanup hoặc không assert | **4** |
| **L2** harness dựng lại từ snapshot cũ → fix mất | `sweep/**` gitignored trừ 10 file; `sweep/v1N` xoá cuối vòng và **chưa từng commit** | **cơ chế** |
| **L3** harness tracked/docs trỏ thứ đã gỡ | 10 file tracked không được kiểm khi schema/endpoint đổi | **8** |

## Đã sửa

### L2 — harness được commit + tham số hoá (root-fix)

- `git ls-files sweep/v15/` = **0**, `git log --all -- sweep/v15/*` rỗng → 7 fix của v15 **mất vĩnh viễn**;
  nguồn dựng lại = `a627569:sweep/v13` (trước fix v14). `evidence/harness-restore.md` của v15 = **0 byte**.
- **Giải pháp:** suite audit ở **`sweep/harness/`** và **được track** (`.gitignore`: `sweep/*` +
  `!sweep/harness/`, output runtime vẫn ignored). `_config.js` biến namespace thành dữ liệu:
  `--audit <name> --out <dir>`. Vòng sau chỉ đổi tham số, không copy, không mất fix.
- `harness-restore.md` giờ **3.473 ký tự**, có mục Nguồn + Fix; `assert-harness.js` fail nếu rỗng.

### L3 — drift guard 2 nơi

- **`HarnessDriftTest`** (JUnit, chạy trong `mvnw test` — không thể quên): 3 test
  1. endpoint harness gọi phải tồn tại trong controller (có whitelist cho assertion "đã gỡ → 404");
  2. bảng harness đọc phải có entity `@Table`;
  3. docs không được liệt kê symbol đã xoá như đang tồn tại.
- **`sweep/harness/assert-harness.js`** (node): route browser phải có trong router; `require()` nội bộ
  phải resolve; không hardcode baseline cũ; `harness-restore.md` không rỗng.

### L1 — ép dọn residue

- Thêm **`study_days`** vào `p16-parity.sql` (marker `STUDY_DAYS=`) + `PENDING_PAYMENTS=`.
- **`assertClean({parity, studyDays, pendingPayments})`** trong `sweep/v8/ui/lib.js` — **throw** nếu còn
  residue; mọi harness UI gọi trong `finally`.
- `sweep/v12/api-sweep.js`: dọn `study_days` của tài khoản probe trong ngày chạy (marker
  `AUDIT_STUDY_DAYS`); cửa sổ ngày dùng ngày của run, không literal `2026-09-21`.
- `routes-all.js`: cleanup chuyển vào **`finally`** (trước đây trên happy-path → throw là rò row).
- `design-v2.js`: **exit code gate theo `clean.ok`** (trước tính rồi bỏ).
- `design.js`: **thêm cleanup** (trước không có gì).

## 8 lỗi L3 cụ thể

| # | File | Vấn đề | Fix |
|---|---|---|---|
| a/b/c | `routes.js:88`, `routes-all.js:257`, `design-v2.js:215` | baseline 10-số cũ | đọc `H.PARITY_BASELINE` |
| d | `lib.js:166` | comment shape 10 số | sửa |
| e | 3 file UI | `cleanupAuditPayments(126)` | `H.PAYMENTS_BASELINE` (=12) |
| f | `routes-all.js:20-22` | đọc file **untracked+gitignored** → hỏng trên clone | sinh từ router (`routes-from-router.js`) |
| g | `AGENTS.md:25` | ghi `p1–p5` + `ui/v3.js` (không tồn tại) | cập nhật inventory thật |
| h | `sweep/v12/api-sweep.js:570` | ghi vào `audit-v12-full/evidence` → **ghi đè artifact lịch sử** | `--out`, mặc định audit hiện tại |

Thêm: `CLAUDE.md:56` bỏ `BlockType` (đã xoá).

## Bằng chứng 2 chiều (bắt buộc)

| Guard | Chèn drift | Gỡ drift |
|---|---|---|
| `HarnessDriftTest` (endpoint) | **FAIL** `search-sort.js:11 -> /api/admin/lessons/445/snapshots/restore` | PASS |
| `assert-harness.js` (route) | **FAIL** `deep-probe.js -> /admin/445/build`, exit **1** | exit **0** |
| `assert-harness.js` (baseline) | **FAIL** `search-sort.js:10 stale parity literal`, exit **1** | exit **0** |
| `assertClean` | **throw** `study_days 4 != expected 99` | CLEAN |
