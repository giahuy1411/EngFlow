# audit-v13 — Phase 7: Vòng 2 (loop-until-dry)

**Ngày:** 2026-09-22 (+07) · Chạy trên **build cuối** (sau tất cả fix F-13-01→F-13-15).

## 1. Case biên đối kháng — dữ liệu/HTTP thật

| Case | Kỳ vọng | Kết quả | Verdict |
|---|---|---|---|
| `POST /api/srs/review` quality = **-1** | từ chối | **400** | ✅ |
| `POST /api/srs/review` quality = **6** | từ chối | **400** | ✅ |
| `POST /api/srs/review` quality = **999** | từ chối | **400** | ✅ |
| `GET /api/streak/history?days=-5` | từ chối | **400** | ✅ |
| `GET /api/lessons?size=100000` | **cap** ở 100 | **200, `page.size=100`, `numberOfElements=100`** | ✅ cap giữ |
| `GET /api/srs/due/50038` (deck private của admin, student gọi) | từ chối (IDOR) | **404** | ✅ F151 giữ |
| `GET /api/admin/stats` bằng token student | từ chối | **403** | ✅ |
| `GET /api/lessons?q=<script>alert(1)</script>` | không phản chiếu raw | **200, 0 hàng, không có `<script>` raw** | ✅ |
| Guard F-13-01: MC chữ cái | từ chối | **400** | ✅ |
| Guard F-13-12: MC `"A - Salad"` | chấp nhận | **200** | ✅ |
| Guard F-13-11: `?sort=badprop` | 400 (không 500) | **400** | ✅ |
| N+1 F-13-15: 1 call `due/10006` | 0 per-word lookup | single-lookup **454→454 (+0)**, batch **3→6** | ✅ |

**Kết luận vòng 2:** **0 lỗi sản phẩm mới.** Tất cả case biên đối kháng đều cho hành vi đúng, và các fix trong phiên **giữ nguyên hiệu lực** trên build cuối.

## 2. Vòng lặp (loop-until-dry)

| Vòng | Phát hiện mới | Ghi chú |
|---|---|---|
| Vòng 1 (Phase 1–5) | **11 finding** (F-13-01…F-13-11 + F-13-14) | baseline, DB, API, UI, perf |
| Vòng 2 (đối kháng) | **0 finding mới** | 12 case biên ở trên |
| Vòng 3 (xác nhận) | **0 finding mới** | chạy lại suite + parity |

→ **Dừng sau 2 vòng liên tiếp không thêm lỗi mới** (đúng tiêu chí loop-until-dry người dùng chốt).

## 3. Adversarial verify các finding

| Finding | Ai phản biện | Kết quả |
|---|---|---|
| F-13-01 (fix đầu) | Agent adversarial review độc lập | **BÁC 6 điểm** → sửa cả 6; 2 trong số đó (F-13-12, F-13-13) do workflow API sweep phát hiện tiếp |
| F-13-07 (study_policy) | Tôi verify độc lập lại bằng `sys.check_constraints` | **XÁC NHẬN** — 0 constraint |
| F-13-14 (danger) | Tôi tính lại độc lập bằng công thức WCAG | **XÁC NHẬN** — 4.00:1 |
| F-13-15 (N+1) | Tôi đo lại bằng query-stats delta | **XÁC NHẬN** — 0 per-word lookup sau fix |
| UI sweep `ERR_EMPTY_RESPONSE` | Tôi đo lại `/api/leaderboard` | **BÁC** — 200, đây là artifact do tôi rebuild giữa lúc sweep |

## 4. Tổng kết

- **13 finding** ghi nhận (F-13-01…F-13-15, trong đó F-13-03/04/05 gộp 1 mục).
- **10 đã FIXED**, **3 OPEN có lý do** (F-13-02 cần quyết định chủ sản phẩm; F-13-07 cần backup trước DML; F-13-08 nợ dữ liệu), **1 DEFERRED có số** (F-13-10).
- **Không có lỗi nào bị bỏ sót mà không ghi.** Mọi giới hạn đều được nêu.
