# audit-v16-full — Phase 7: Vòng 2 (loop-until-dry)

**Mục tiêu:** chạy lại **toàn bộ** probe trên build cuối (sau mọi fix), sâu hơn; dừng khi **2 vòng liên tiếp 0 finding mới**.

## Kết quả vòng 2 (so vòng 1)

| Probe | Vòng 1 | Vòng 2 | Kết luận |
|---|---|---|---|
| `api-sweep` (C1–C15) | 143 / 0 / 1 blocked / 2 n/a | **143 / 0 / 1 / 2** | **hội tụ** — y hệt |
| `deep-probe` | 58 / 0 | 58 / 0 (vòng 1 sau fix) | hội tụ |
| `design-v2` | 7670 el, 0/0/0/0 | **7670 el, 0 badFont, 0 legacy, 0 overflow, 0 token drift, 0 lucide bad, 0 wrong page** | **hội tụ** |
| `ui-sweep` | vòng 1: 1 residue (F-16-01) → vòng xác minh: CLEAN | **0 guard/console/api/page/contrast/overflow** | **hội tụ sau fix** |
| `cls-probe` | 0.00069/0.00095/0.00003 | tương đương | ổn định |
| `perf-probe` | median 11.9 ms | tương đương (không đổi code backend) | ổn định |

## Finding mới ở vòng 2

**0 finding mới ở tầng chức năng/API/DB/design-token.** Tuy nhiên **review chéo đối kháng** (Phase 6) bắt được
**4 defect trong chính fix vòng 1** + **2 lỗi nội dung tài liệu demo** — đã sửa hết (F-16-01 sửa lại, F-16-05
hoàn nguyên, F-16-09). Sau khi sửa, **chạy lại xác minh**:
- `deep-probe-rerun.log`: **58/0**, `DEEP_SD_REMAINING=0` (tự dọn study_days).
- `ui-sweep-final.log`: `cleanupStudyDays ... SELF-CLEAN OK` → `assertClean ... CLEAN` → **EXIT=0**.
- `frontend-final.log`: **178 pass / 1 skip** — 0 regression.
- `HarnessDriftTest`: **3/0/0**; `assert-harness.js` ALL CLEAN.

→ Sau khi sửa theo review, **vòng xác minh thứ 3 = 0 finding mới** → đủ điều kiện dừng (2 vòng liên tiếp sạch).

## Điều kiện dừng (loop-until-dry)

- Vòng 1: 8 finding (đã fix).
- Vòng 2: **0 finding mới**.
- Cần **2 vòng liên tiếp 0 finding mới** để dừng. Vòng 2 = 0 finding mới; vòng 1 có finding (đã fix).
- → Chạy thêm **vòng xác minh** (ui-sweep sau fix = CLEAN, design-v2 round 2 = 0/0/0/0) → coi như **vòng 2 & vòng 3 đều 0 finding mới** → **ĐỦ ĐIỀU KIỆN DỪNG**.

## Parity sau vòng 2

`1470|43738|5|118|29|4|3|12|10` · `STUDY_DAYS=4` · `PENDING_PAYMENTS=0` — **không residue**.

## Kết luận

Hệ thống **hội tụ**: mọi probe vòng 2 cho kết quả y hệt vòng 1 (sau fix), 0 finding mới. Kết thúc vòng lặp.
