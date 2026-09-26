# audit-v15 hardening — cleanup manifest

**Ngày:** 2026-09-25 (+07) · Ràng buộc V1: xoá rác sinh khi kiểm thử + liệt kê chi tiết.

## Quy tắc

| Nhóm | Đường dẫn | Xử lý |
|---|---|---|
| Artifact pipeline + evidence | `.specify/specs/audit-v15-full/**` | **GIỮ** (deliverable) |
| **Harness (mới, được commit)** | `sweep/harness/*.js` | **GIỮ + TRACK** (14 file source) |
| Output runtime của harness | `sweep/harness/*.json`, `_*.sql`, `shots/` | **IGNORED** (không commit) |
| Tooling dùng chung (đã tracked) | `sweep/v8/**`, `sweep/v12/api-sweep.js` | **GIỮ** (đã sửa L1/L3) |
| Scratch phiên | `tmp/h/**` | **XOÁ** (KHÔNG gitignored) |
| Build output | `target/`, `frontend/dist/` | **XOÁ** (gitignored) |

## A. Thay đổi trong repo (deliverable)

**Tạo mới (giữ):**
- `sweep/harness/**` — 14 file source (`_config.js`, `api-inventory.js`, `assert-harness.js`,
  `cls-probe.js`, `danger-tint.js`, `deep-probe.js`, `f1302-a1-a11y.js`, `f1302-a1-blocks-live.js`,
  `f1320-api-redirect-live.js`, `focused-probe.js`, `perf-probe.js`, `routes-from-router.js`,
  `search-sort.js`, `ui-sweep.js`) — **được track** (L2 root-fix).
- `src/test/java/com/datn/engflow/HarnessDriftTest.java` — drift guard JUnit (L3).

**Sửa (8 file tooling + 3 doc/config):**
`sweep/v8/p16-parity.sql` (+`study_days`, `PENDING_PAYMENTS` marker),
`sweep/v8/ui/lib.js` (`assertClean`, `parityMarker`, `PARITY_BASELINE`/`PAYMENTS_BASELINE`/`STUDY_DAYS_BASELINE`,
`execFileSync`), `sweep/v8/ui/routes.js`, `sweep/v8/ui/routes-all.js` (cleanup vào `finally`, route từ router),
`sweep/v8/ui/design.js` (thêm cleanup), `sweep/v8/ui/design-v2.js` (exit gate),
`sweep/v12/api-sweep.js` (`study_days` cleanup, ngày động, `--out`),
`.gitignore` (`sweep/*` + `!sweep/harness/`), `AGENTS.md`, `CLAUDE.md`.

## B. Xoá cuối phiên — ĐÃ THỰC HIỆN

```bash
rm -rf tmp/            # scratch phiên (KHÔNG gitignored) — bắt buộc xoá
rm -rf target/         # build output backend (22M)
rm -rf frontend/dist/  # build output frontend (không tồn tại ở lần này)
```

**Danh sách CHÍNH XÁC đã xoá (26 file trong `tmp/`, liệt kê trước khi xoá):**

```
tmp/h/api-sweep2.log          tmp/h/routes-all2.log        tmp/h/ui-sweep.log
tmp/h/backend-suite.log       tmp/h/routes-all3.log        tmp/h/ui-sweep2.log
tmp/h/deep.bak                tmp/h/routes-all4.log        tmp/h/ui-sweep3.log
tmp/h/design-v2.log           tmp/h/routes2.log            tmp/h/v12-before-fix.log
tmp/h/design-v2b.log          tmp/h/search-sort.bak        tmp/h/v12-committed.json
tmp/h/design.log              tmp/h/ss.bak                 tmp/h/v12-run2.log
tmp/h/design3.log             tmp/h/override-test/endpoint-inventory.json
tmp/h/frontend-suite.log      tmp/h/parity.bak             tmp/h/verify-bak.sql
tmp/h/gitignore.bak           tmp/h/routes-all.log         tmp/h/verify-hardening.mjs
```
(+ 3 thư mục rỗng: `tmp/`, `tmp/h/`, `tmp/h/override-test/`)

**Kiểm tra sau xoá:** `tmp`, `target`, `frontend/dist` đều **absent**; `sweep/v13` **không** tồn tại
(probe ghi đúng chỗ); `git ls-files sweep/harness/` = **14** (harness đã được track — L2 root-fix).

**Output runtime của harness (giữ lại, đã gitignore — theo thiết kế):**
`sweep/harness/_deep-cleanup.sql`, `sweep/v8/_cleanup_payments.sql`, `sweep/v8/ui/design-v2.json`,
`sweep/v8/ui/routes-admin-1440.json`, `sweep/v8/ui/routes-all.json`, `sweep/v12/_cleanup.sql`.
Chúng là **byproduct mỗi lần chạy** (sinh lại được), `.gitignore` đã chặn commit — không phải rác
phiên, nên **không** xoá (xoá cũng vô nghĩa vì lần chạy sau tái tạo). Evidence deliverable ở
`.specify/specs/audit-v15-full/evidence/**` **giữ** (gồm `shots/`).

## C. Artifact lịch sử đã khôi phục (không đụng)

`.specify/specs/audit-v12-full/evidence/api-sweep.json` — `sweep/v12/api-sweep.js` cũ ghi đè khi chạy
(L3-h); đã khôi phục từ `git show 2590caf^:` và **verify hash khớp**. Sau fix `--out`, chạy lại **không**
còn ghi đè (đã chứng minh: `git status .specify/specs/audit-v12-full/` = rỗng sau khi chạy lại api-sweep).
