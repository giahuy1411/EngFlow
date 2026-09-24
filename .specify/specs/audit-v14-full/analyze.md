# audit-v14-full — analyze (read-only, cross-artifact consistency)

**Ngày:** 2026-09-25 (+07) · Kiểm nhất quán giữa `spec.md` ↔ `plan.md` ↔ `tasks.md` ↔ `findings.md` ↔ `REPORT.md`.
Không sửa file nào (trừ khi analyze tự bắt lỗi — ghi lại).

## 1. Đối chiếu spec ↔ findings

| Requirement | Finding liên quan | Nhất quán? |
|---|---|---|
| R2 API surface + reconcile | D1/D10; F-14-01 (harness data-loss trong lúc làm) | ✅ |
| R4 DB audit | F-14-C1/C2 (nợ dữ liệu) | ✅ |
| R5 design system + prompt | F-14-03 (tap-target); `prompt-fixes.md` | ✅ |
| R6 vòng 2 loop-until-dry | `round-2.md` | ✅ |
| R9 review chéo | `review-f14.md` | ✅ |
| R10 probe thứ 2 | `round-2.md` §4 | ✅ |

## 2. Đối chiếu plan ↔ tasks ↔ findings

- Plan Phase 0–9 → `tasks.md` có đủ T0.1–T9.2, đánh dấu `[x]` **theo artifact thật**.
- **5 finding** trong `findings.md`: F-14-01, F-14-02, F-14-03, F-14-C1, F-14-C2 — **đều** xuất hiện trong
  `REPORT.md` §2/§3/§4. Không finding "mồ côi".
- Không finding nào bị bỏ mà không có disposition.

## 3. Kiểm số liệu có nguồn

| Số trong REPORT | Nguồn |
|---|---|
| Backend 525/0/0/11 | `backend-after.log` |
| Frontend 194/1 (31 file) | output `npx vitest run` (`frontend-after.log`) |
| Build 177.98 kB | output `npx vite build` |
| API 143/0 | `api-sweep.json` |
| Reconcile 148/148 | `endpoint-reconciliation.md` |
| UI 117 cell, 0 guardFail | `ui-sweep.json` |
| Tap-target 118→0 REAL | `tap-targets.json` |
| Perf 35 endpoint, stackStable | `perf-before.json` |
| Parity `1470\|…\|5` | `parity-before.txt`, `parity-after-*.txt` |

## 4. Kiểm tuyên bố có thể sai

| Tuyên bố | Kiểm lại | Kết luận |
|---|---|---|
| "Font Be Vietnam Pro" | `document.fonts.check`=true + grep=0 | ✅ |
| "0 rác sau sweep" | parity = baseline; `AUDIT_*` marker = 0 | ✅ |
| "Không tối ưu mò" | perf-before có số; mục từ chối có lý do | ✅ |
| "148/148 accounted" | reconcile exit 0, unaccounted=0 | ✅ |
| "118 tap-target triaged" | tally 50+68=118 = `ui-sweep.json` | ✅ |
| "F-14-01 khôi phục đủ" | lesson 445 (5948 content, 6 ex), section 5, parity baseline | ✅ |
| "20/20 prior finding verdict" | `prior-hypotheses.md` | ✅ |

## 5. ⚠️ Sai lệch tìm thấy và cách xử lý

1. **`REPORT.md` phải ghi 5 finding** (F-14-01/02/03/C1/C2), không gộp C1/C2 vào "1 mục nợ dữ liệu".
   → Ghi tách trong REPORT.
2. **Phân biệt finding sản phẩm vs lỗi probe tự gây:** v14 có nhiều **lỗi probe** (P1–P3 ui-sweep, E1–E2 e2e,
   adversarial content-type) — **KHÔNG** tính là finding sản phẩm. REPORT phải tách 2 nhóm rõ ràng.
   → Ghi riêng §"Lỗi probe tự gây".
3. **F-14-01 là incident tự gây, không phải bug sản phẩm** — nhưng vẫn xếp là finding HIGH vì hậu quả
   (mất dữ liệu thật). REPORT ghi rõ nguồn gốc.
4. **`converge.md` §3 D7 (a11y mở rộng)** — thực tế v14 kiểm heading/lang/skip-link qua chrome-devtools +
   ui-sweep, nhưng **chưa** có test keyboard-traversal/focus-trap tự động. → Ghi đúng là "một phần"
   (heading/lang/skip-link CÓ; keyboard-traversal/modal focus-trap chỉ có ở tầng code sẵn có, chưa đo lại).

## 6. Kết luận analyze

- Cấu trúc artifact **đầy đủ và nhất quán về phương pháp** (mọi finding → artifact; mọi số → nguồn).
- **4 sai lệch** được ghi ở §5, **sẽ sửa trong REPORT.md** để báo cáo cuối khớp thật.
- Không requirement nào không có bằng chứng; không bằng chứng nào không trỏ về requirement.
