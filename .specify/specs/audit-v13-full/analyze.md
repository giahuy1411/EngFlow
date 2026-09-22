# audit-v13-full — analyze (read-only, cross-artifact consistency)

**Ngày:** 2026-09-22 (+07) · Kiểm tính nhất quán giữa `spec.md` ↔ `plan.md` ↔ `tasks.md` ↔ `findings.md` ↔ `REPORT.md`. Không sửa file nào.

## 1. Đối chiếu spec ↔ findings

| Requirement | Finding liên quan | Nhất quán? |
|---|---|---|
| R2 API surface được chạy | F-13-11, F-13-12, F-13-13 | ✅ — đều là phát hiện **từ** API sweep |
| R3 UI đối chiếu API | F-13-01 (E2E T4.2b) | ✅ |
| R5 design system + prompt | F-13-03/04/05, F-13-14, F-13-16, `prompt-fixes.md` | ✅ |
| R6 vòng 2 loop-until-dry | `round-2.md` | ✅ |
| R9 review chéo | mục "Review chéo" trong F-13-01 + `round-2.md` §3 | ✅ |

## 2. Đối chiếu plan ↔ tasks ↔ findings

- Plan liệt kê Phase 0–8. `tasks.md` có đủ T0.1–T8.4, đánh dấu `[x]/[~]/[ ]` **theo artifact thật**.
- Mọi finding trong `findings.md` đều xuất hiện trong `REPORT.md` §2/§3/§4. Không có finding "mồ côi".
- Số finding: `findings.md` có **13 mục heading** (F-13-01, 03/04/05 gộp, 02, 06, 07, 08, 09, 10, 11, 12, 13, 14, 15, 16). `REPORT.md` ghi "11 finding" ở TL;DR — **KHÔNG NHẤT QUÁN**, xem §5.

## 3. Kiểm tra số liệu có nguồn

| Số trong REPORT | Nguồn |
|---|---|
| Backend 513/0/0/11 | `backend-final.log` |
| Frontend 144/1 (27 file) | output `npx vitest run` |
| Build 177.44 kB (gzip 67.57) | output `npx vite build` |
| API 143 pass / 0 fail | `api-sweep.json` |
| Parity `1470\|…\|5` | `p16-parity.sql` |
| 117 route×role, 0 guardFails | `ui-sweep.json` |
| 35 endpoint perf | `perf-before.json` |
| CLS 0.00069 | `cls-before.json` |

## 4. Kiểm tra tuyên bố có thể sai

| Tuyên bố | Kiểm lại | Kết luận |
|---|---|---|
| "Font Be Vietnam Pro" | grep Outfit/PlusJakarta = 0; `document.fonts.check` = true | ✅ |
| "0 rác" | `PROBE_LEFT=0`, `EX_LEFT=0`, parity khớp baseline | ✅ |
| "Không tối ưu mò" | perf-before có số; mục "từ chối tối ưu" có lý do | ✅ |
| "3 finding OPEN" | findings.md có F-13-02, F-13-07, F-13-08 là OPEN | ✅ |
| "regression 44×" | 32 814 / 742 ≈ 44.2 | ✅ |

## 5. ⚠️ Sai lệch tìm thấy và cách xử lý

1. **`REPORT.md` TL;DR ghi "11 finding" trong khi `findings.md` có 13 mục** (F-13-01, 02, 03/04/05, 06, 07, 08, 09, 10, 11, 12, 13, 14, 15, 16 — sau khi gộp 03/04/05 thành 1 và tách 14/15/16 ra). **Cần sửa REPORT cho khớp.**
2. **`REPORT.md` §3 ghi "3 OPEN"** nhưng F-13-10 là **DEFERRED**, không phải OPEN. Cách đếm cần nói rõ: OPEN = 3 (02, 07, 08), DEFERRED = 1 (10). **Cần sửa.**
3. **F-13-16 và F-13-14 được thêm SAU khi viết REPORT** → REPORT chưa có. **Cần bổ sung.**
4. `tasks.md` T6.5 đánh `[ ]` nhưng `t6.5-v12-open-items.md` đã có → **đánh dấu lại**.

## 6. Kết luận analyze

- Cấu trúc artifact **đầy đủ và nhất quán về phương pháp** (mọi finding → artifact; mọi số → nguồn).
- **4 sai lệch đếm số** được tìm thấy (mục 5) — đã ghi lại ở đây và **sẽ sửa trong REPORT.md** để báo cáo cuối khớp với findings thật.
- Không có requirement nào không có bằng chứng; không có bằng chứng nào không trỏ về requirement.
