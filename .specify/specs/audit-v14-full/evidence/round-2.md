# audit-v14-full — Phase 7: vòng 2 (loop-until-dry) trên build cuối

**Ngày:** 2026-09-25 (+07) · Chạy trên build cuối (sau fix F-14-02, backend rebuild).

## 1. Case biên đối kháng (dữ liệu/HTTP thật)

`sweep/v14/adversarial.js` → **14/14 PASS**, `parityAfter=1470|43735|72|118|29|15|4|126|10|5 ok=true`

| Case | Kỳ vọng | Kết quả |
|---|---|---|
| SRS quality −1 / 6 / 999 | từ chối | **400** ×3 ✅ |
| streak history days=−5 | từ chối | **400** ✅ |
| lessons size=100000 | cap 100 | **200, size=100** ✅ |
| vocab `?sort=badprop` | 400 (không 500) | **400** ✅ |
| lessons `q=<script>` | không phản chiếu raw | **200, không `<script>`** ✅ |
| SRS due deck private (IDOR) | từ chối | **404** ✅ |
| JSON hỏng (đúng Content-Type) | 400 | **400** ✅ |
| streak snapshot anon | 401 | **401** ✅ |
| admin stats student | 403 | **403** ✅ |
| register email trùng | 400/409 | **400/409** ✅ |
| lessons `q=` rỗng | 200 | **200** ✅ |
| **AI bucket burst** | 429 trong ≤14 lần | **429** ✅ |

## 2. Chạy lại mọi probe Phase 1–5 trên build cuối

| Probe | Kết quả trên build cuối |
|---|---|
| `api-sweep.js` | **143 pass / 0 fail / 1 blocked / 3 N/A** |
| `deep-probe.js` | **58 pass / 0 fail** |
| `reconcile-inventory.js` | **148/148 accounted, 26/26 controller 100%** |
| `adversarial.js` | **14/14 pass** |
| parity | `1470\|43735\|72\|118\|29\|15\|4\|126\|10\|5` — baseline |
| backend suite | **525 / 0 / 0 / 11** |
| frontend suite | **194 passed / 1 skipped (31 file)** |

## 3. Loop-until-dry

| Vòng | Finding mới | Ghi chú |
|---|---|---|
| Vòng 1 (Phase 0–6) | **3 finding** (F-14-01 harness data-loss, F-14-02 500→503, F-14-03 tap-target triage) + nhiều **lỗi probe tự gây** (P1–P3 ui-sweep, E1–E2 e2e, adversarial content-type) | baseline, DB, API, UI, perf |
| Vòng 2 (Phase 7) | **0 finding sản phẩm mới** | 14 case biên + chạy lại mọi probe |
| Vòng 3 (xác nhận) | **0 finding sản phẩm mới** | suite + parity + reconcile |

→ **Dừng sau 2 vòng liên tiếp không thêm lỗi sản phẩm mới** (đúng tiêu chí loop-until-dry người dùng chốt).

**Phân biệt rõ:** các "lỗi probe tự gây" (P1–P3, E1–E2, content-type) **KHÔNG** phải finding sản phẩm — chúng
là lỗi của harness do tôi viết, đã sửa và ghi lại minh bạch. Nếu tính chúng là finding sản phẩm thì báo cáo
sẽ sai (đúng bài học v13: probe bug báo thành finding).

## 4. Adversarial verify (probe thứ 2 — R10)

| Finding | Probe 2 | Kết quả |
|---|---|---|
| F-14-01 (harness data-loss) | chạy lại coverage-sweep lần 2 | parity không đổi → **XÁC NHẬN fix** |
| F-14-02 (503) | live 2 chiều (Ollama tắt→503, bật→200) + unit test | **XÁC NHẬN** |
| F-14-03 (tap-target) | đo lại bằng chrome-devtools MCP trên `/login` | **XÁC NHẬN** (không control thật < 24px) |

## 5. Tổng kết

- **3 finding** ghi nhận: F-14-01 (HIGH, harness), F-14-02 (LOW, đã fix), F-14-03 (LOW, triage 0 REAL).
- **2 FIXED** (F-14-01, F-14-02), **1 CLOSED** (F-14-03 — kết luận triage: 0 vi phạm thật).
- **Nợ dữ liệu** đo được nhưng KHÔNG tự sửa (cần quyết định owner): F-14-C1 (67/72 user test), F-14-C2
  (109 payment PENDING).
- **Không có lỗi sản phẩm nào bị bỏ sót mà không ghi.** Mọi giới hạn đều nêu.
