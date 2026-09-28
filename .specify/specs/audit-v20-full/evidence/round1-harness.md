# audit-v20 — Vòng 1: kết quả harness (số đo thật)

**Ngày chạy:** 2026-09-28 (+07) · **Runtime:** sqlserver healthy, redis up, backend `Started in 12.542s`, frontend :5173.

## Kết quả

| Harness | Kết quả | Khớp baseline? |
|---|---|---|
| `assert-harness.js` | **ALL CLEAN (8/8)** | ✅ (v19 có 7 check; v20 thêm check 8) |
| `sweep/v12/api-sweep.js` | **pass=145 fail=0 blocked=0 n_a=2**, `findings: []`, cleanup 0/0/0/0/0 | ✅ khớp v18/v19 (145) |
| `sweep/harness/deep-probe.js` | **pass=58 fail=0 blocked=0 n_a=0** (mc-guard 18, sort 2, contract 13, roles 25) | ✅ khớp v18 (58) |
| `sweep/harness/ui-sweep.js` | **contrastFails 0, missingAlt 0, smallTargets 0, noName 0, consoleErrors 0, apiErrors 0, pageErrors 0, guardFails 0, overflowRoutes 0**; responsive 35 cells 0 overflowing | ✅ |
| `sweep/v8/ui/design-v2.js` | non-BVP fonts **0**; token drift **0**; tokens missing **0** (660 checks); lucide stroke≠2.5 **0** (655 icons); routes overflow **0/60** | ✅ |
| `sweep/v8/ui/routes-all.js` | console errors **0**, overflow **0**, not mounted **0**, wrong landing **0** | ✅ |
| `mvnw.cmd test` | **541 / 0 fail / 0 error / 11 skipped** | ✅ |
| `npx vitest run` | **194 passed / 1 skipped / 32 files** | ✅ |
| `npx vite build` | **exit 0**, entry `index-DZ76lATv.js` **177.75 kB** (gzip 67.69) | ✅ không tăng |

## Điểm quan trọng — F-20-01 đã được chứng minh

Trước fix, `design-v2` báo:
```
RESIDUE: assertClean failed — parity 1470|43738|5|118|29|4|3|13|10 != expected ...|12|10
```
Sau fix (baseline 13):
```
cleanupAuditPayments: ... after=13 baseline=13 sqlError=false -> SELF-CLEAN OK
assertClean: parity=1470|43738|5|118|29|4|3|13|10 study_days=4 pending_payments=0 exercise_attempts=33 -> CLEAN
```
→ Guard nay PASS đúng. Parity khớp **hoàn toàn**; `study_days=4`, `pending_payments=0`, `exercise_attempts=33` đều khớp baseline ⇒ **không có rác lọt**.

## Font Be Vietnam Pro — verify (yêu cầu gốc của người dùng)

`design-v2` đo trực tiếp trong browser:
- `font loaded (400/700/900): {"loaded":true,"bold":true,"black":true,"faces":18}`
- `non-BVP computed fonts: 0` ← **không một phần tử nào render bằng font khác BVP**
- 0 legacy family (Outfit/Plus Jakarta/Poppins/Inter)

→ Yêu cầu "thay toàn bộ font thành Be Vietnam Pro" **đã đúng và được chứng minh bằng đo runtime**, không phải bằng đọc code.
