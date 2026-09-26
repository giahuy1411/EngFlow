# audit-v15 hardening — khôi phục harness (H1)

**Ngày:** 2026-09-25 (+07) · **Nguồn:** `a627569:sweep/v13/*` (pre-purge tree, 16 file)

## Vì sao có bản hardening này (điểm yếu L2 — cơ chế mất fix)

Các vòng audit trước mỗi vòng tạo `sweep/v1N/` riêng, **gitignored**, rồi **xoá ở bước cleanup**.
Hệ quả đo được:

| Bằng chứng | Giá trị |
|---|---|
| `git ls-files sweep/v15/` | **0** (v15 chứa 7 fix → **mất vĩnh viễn**) |
| `git log --all -- sweep/v15/*` | rỗng |
| Nguồn dựng lại của vòng sau | `a627569:sweep/v13` — **trước** mọi fix của v14 |
| `evidence/harness-restore.md` (v15) | **0 byte** → không truyền được fix nào |

⇒ Vòng sau sẽ tái nhập đúng các bug đã sửa (F-15-08/11/12/13/16). **Đây là gốc của điểm yếu #2.**

## Cách sửa: harness **được commit**, tham số hoá

- Suite audit nằm ở **`sweep/harness/`** và **được track** (`.gitignore`: `sweep/*` + `!sweep/harness/`).
- `sweep/harness/_config.js` biến namespace thành **dữ liệu**:
  `node sweep/harness/<probe>.js [--audit <name>] [--out <dir>]`, mặc định `audit-v15-full`.
  Vòng sau chỉ cần `--audit audit-v16-full` — **không sửa file, không mất gì**.
- Output runtime (`*.json`, `_cleanup.sql`, `shots/`) vẫn **ignored** — không commit kết quả.

## File port từ `a627569:sweep/v13/` + file mới

`a627569:sweep/v13/` có **16 file** (12 `.js` + 2 `.sql` + 2 `.json`). Bản harness commit gồm
**14 file source** = **11 file `.js` port từ v13** (loại `api-sweep.js` — xem ghi chú dưới)
**+ 3 file mới** (`_config.js`, `assert-harness.js`, `routes-from-router.js`). Output runtime
(`_cleanup.sql`, `*-before.json`, `shots/`) **không** port — được sinh lại mỗi lần chạy và bị ignore.

| File | Vai trò | Nguồn |
|---|---|---|
| `api-inventory.js` | sinh danh sách endpoint từ controller (nguồn cho drift guard) | v13 |
| `deep-probe.js` | guard MC, sort, contract, role (2 chiều) | v13 |
| `ui-sweep.js` | route×role, a11y, contrast composite, responsive, tap-target | v13 |
| `focused-probe.js` | contrast/tap-target/console-error chuyên sâu | v13 |
| `perf-probe.js` | đo hiệu năng (median, flush bucket) | v13 |
| `cls-probe.js` | CLS + attribution | v13 |
| `search-sort.js` | search + sort đo trên dữ liệu thật | v13 |
| `danger-tint.js` | contrast trạng thái lỗi (danger) | v13 |
| `f1302-a1-a11y.js` | a11y panel nội dung | v13 |
| `f1302-a1-blocks-live.js` | xác nhận Đường B **đã gỡ** (18/18) | v13 |
| `f1320-api-redirect-live.js` | redirect an toàn khi phiên hết hạn | v13 |
| `_config.js` | **(mới)** cấu hình chia sẻ — namespace/out/ngày | mới |
| `assert-harness.js` | **(mới)** static guard: route/require/baseline/restore-record | mới |
| `routes-from-router.js` | **(mới)** sinh route + guard từ `frontend/src/router/index.js` | mới |

*(`api-sweep.js` **không** copy: bản đã-track ở `sweep/v12/api-sweep.js` là API sweep duy nhất, được
AGENTS.md trỏ tới.)*

## Fix đã áp lại trong bản harness này

| Nguồn | Fix | File |
|---|---|---|
| audit-v14 D3 | `perf-probe` ghi vào `__dirname`, không `sweep/v13` | `perf-probe.js` |
| audit-v14 F-13-21 | đường dẫn `_cleanup.sql` trỏ cạnh script | `deep-probe.js` |
| audit-v14 P1 | `ui-sweep` so **pathname** (guard thêm `?redirect=`) | `ui-sweep.js` |
| audit-v14 P2 | route premium cho anon → `/premium` | `ui-sweep.js` |
| audit-v14 F-13-12/13 | LISTENING bare-letter + copy row 651717 → **200** | `deep-probe.js` |
| audit-v15 F-15-13 | `perf-probe` output | `perf-probe.js` |
| audit-v15 F-15-14 | `focused-probe` bỏ block "builder contrast" (đã gỡ) | `focused-probe.js` |
| audit-v15 F-15-15 | 2 probe viết lại thành "xác nhận ĐƯỜNG B ĐÃ GỠ" | `f1302-a1-*.js` |
| audit-v15 F-15-16 | `cls-probe` output | `cls-probe.js` |
| audit-v15 (mới) | `/structure` gỡ khỏi `perf-probe` target list | `perf-probe.js` |
| audit-v15 hardening (L1-b) | cleanup `ui-sweep` vào **`finally`** + nhánh `fatal` exit 2 | `ui-sweep.js` |
| audit-v15 hardening (L3-i) | route `/admin/:id/build` (đã gỡ) expect về `/` — hết 2 guard FAIL | `ui-sweep.js` |

## Cách chạy

```bash
# mặc định (audit-v15-full)
node sweep/harness/ui-sweep.js
node sweep/harness/deep-probe.js
node sweep/harness/api-inventory.js

# vòng sau
node sweep/harness/ui-sweep.js --audit audit-v16-full
```

## Guard chống rỗng

`assert-harness.js` fail nếu file này rỗng hoặc thiếu mục "Nguồn"/"Fix đã áp" — để một lần quên ghi
lại không trôi qua im lặng (đúng cái đã xảy ra ở v15).
