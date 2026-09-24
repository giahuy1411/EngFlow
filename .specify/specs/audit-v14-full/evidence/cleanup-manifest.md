# audit-v14-full — Cleanup manifest (T8.5)

**Ngày:** 2026-09-25 (+07) · Ràng buộc người dùng: mọi file/folder rác sinh khi kiểm thử **phải xoá**,
báo cáo liệt kê chi tiết.

## A. GIỮ (deliverable của phiên — không xoá)

| Đường dẫn | Bản chất |
|---|---|
| `.specify/specs/audit-v14-full/*.md` (constitution, spec, clarify, checklist, plan, tasks, findings, converge, analyze, REPORT) | pipeline artifact |
| `.specify/specs/audit-v14-full/evidence/**` (42 file + `shots/`) | bằng chứng |
| `sweep/v14/perf-before.json`, `cls-before.json` | số đo hiệu năng (được REPORT link) |

## B. XOÁ (rác sinh trong phiên — liệt kê từng đường dẫn)

### B1. `sweep/v14/` — harness (quyết định người dùng: xoá + liệt kê)
25 file:
```
sweep/v14/_adversarial.sql          sweep/v14/_cleanup.sql            sweep/v14/_coverage-integrity.sql
sweep/v14/_deep-cleanup.sql         sweep/v14/_e2e.sql                sweep/v14/adversarial.js
sweep/v14/api-inventory.js          sweep/v14/api-sweep.js            sweep/v14/assert-namespace.js
sweep/v14/cls-probe.js              sweep/v14/coverage-sweep.js       sweep/v14/danger-tint.js
sweep/v14/deep-probe.js             sweep/v14/e2e-3tier.js            sweep/v14/f1302-a1-a11y.js
sweep/v14/f1302-a1-blocks-live.js   sweep/v14/f1320-api-redirect-live.js  sweep/v14/focused-probe.js
sweep/v14/perf-probe.js             sweep/v14/reconcile-inventory.js  sweep/v14/search-sort.js
sweep/v14/tap-target-triage.js      sweep/v14/ui-sweep.js
sweep/v14/cls-before.json           sweep/v14/perf-before.json
```
**Lưu ý:** `cls-before.json` + `perf-before.json` **được GIỮ** (chuyển sang evidence trước khi xoá — xem §A).

### B2. `tmp/` — scratch (KHÔNG được `.gitignore`) — 29 file
```
tmp/v14/restore-apply.sql      tmp/v14/restore-scratch.sql    tmp/v14/t1-constraints.out
tmp/v14/t1-constraints.sql     tmp/v14/t1-db-audit.out        tmp/v14/t1-db-audit.sql
tmp/v14/t2-api-sweep.out       tmp/v14/t2-api-sweep2.out      tmp/v14/t2-api-sweep3.out
tmp/v14/t2-deep-probe.out      tmp/v14/t2-deep-probe2.out     tmp/v14/t2-search-sort.out
tmp/v14/t3-tap-triage.out      tmp/v14/t3-tap-triage2.out     tmp/v14/t3-tap-triage3.out
tmp/v14/t3-ui-sweep.out        tmp/v14/t3-ui-sweep2.out       tmp/v14/t3-ui-sweep3.out
tmp/v14/t4-e2e.out             tmp/v14/t4-e2e2.out            tmp/v14/t5-cls.out
tmp/v14/t5-perf-before.out     tmp/v14/t6-rebuild.log         tmp/v14/t7-adversarial.out
tmp/v14/t7-adversarial2.out    tmp/v14/t7-adversarial3.out    tmp/v14/t7-adversarial4.out
tmp/v14/t7-api-sweep-final.out tmp/v14/t7-deep-final.out
```
→ Xoá **cả thư mục `tmp/`** (không tồn tại trước phiên này).

### B3. Build output (tái tạo được, đã ignore)
```
target/            (backend Maven output)
frontend/dist/     (frontend Vite build)
```

### B4. Log rác cũ (từ trước phiên, đã ignore)
```
frontend/build-v6.log   frontend/vitest-final.log   frontend/vitest-v6.log   frontend/vitest-v6b.log
```

### B5. Row DB probe (dọn trong run — đã xác nhận)
- `payment_transactions` PENDING hôm nay: dọn sạch (parity 126)
- `study_days` id=30067 (do coverage sweep tạo): đã xoá
- `lesson_snapshots` id=30030 (do coverage sweep tạo): đã xoá
- `vocabulary` `zzburst%` (adversarial burst): đã xoá
- lesson 445 + section 5 + blocks (F-14-01): **đã khôi phục**
- Scratch DB `english_learning_v14probe`, `english_learning_v14restore`: đã DROP

### B6. Cửa sổ trình duyệt MCP
`.playwright-mcp/`, `.ua/`, `.ua-screens/` — nếu sinh ra thì xoá (hiện **không tồn tại**).

## C. KHÔNG ĐỤNG (pre-existing — không phải rác của phiên)

| Đường dẫn | Lý do |
|---|---|
| `uploads/**` (7 file có sẵn) | runtime data người dùng, không phải của phiên |
| `.specify/specs/audit-v{8..13}-full/` | artifact phiên cũ |
| `.env`, `.env.bak-*` | cấu hình — không đụng |
| `/c/Users/ASUS/engflow-backups/*.bak` | backup — ngoài repo, giữ |
| `frontend/node_modules/` | dependency (đã `npm install` cho Phase 0) |

## D. Quy trình xoá an toàn

1. Chuyển `sweep/v14/perf-before.json` + `cls-before.json` → `evidence/` (đã làm).
2. `git status --short` snapshot → `evidence/pre-cleanup-tree.txt`.
3. Xoá **đúng danh sách** trên (không `rm -rf` glob gốc).
4. Verify: `git status --short` chỉ còn deliverable + 6 file sửa; parity = baseline.
5. Ghi verbatim vào `REPORT.md` §Cleanup.

## E. Trạng thái thực thi (ĐÃ CHẠY 2026-09-25)

| Bước | Kết quả |
|---|---|
| Chuyển `perf-before.json` + `cls-before.json` → evidence | ✅ đã copy |
| Snapshot pre-cleanup | ✅ `evidence/pre-cleanup-tree.txt` |
| `rm -rf tmp` | ✅ (29 file, cả thư mục) |
| `rm -rf sweep/v14` | ✅ (25 file) |
| `rm -rf target frontend/dist` | ✅ (build output) |
| Xoá 4 log rác `frontend/*.log` | ✅ |
| Row DB probe | ✅ dọn trong run (payment, study_days 30067, snapshot 30030, vocab zzburst%) |
| Scratch DB | ✅ DROP (`english_learning_v14probe`, `english_learning_v14restore`) |
| **Verify `git status --short`** | ✅ chỉ còn: 6 file sửa (`.gitignore`, `feature.json`, `AGENTS.md`, `README.md`, `GlobalExceptionHandler.java`, `GlobalExceptionHandlerProblemDetailTest.java`) + `?? .specify/specs/audit-v14-full/` |
| **Verify parity** | ✅ `1470\|43735\|72\|118\|29\|15\|4\|126\|10\|5` = baseline |
| MCP scratch (`.playwright-mcp` etc.) | ✅ không tồn tại |

**Không đụng:** `uploads/**` (7 file có sẵn), spec v8–v13, `.env*`, backup ngoài repo, `node_modules/`.
