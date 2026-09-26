# audit-v16-full — analyze (cuối kỳ)

## Chất lượng bằng chứng

**Mạnh:**
- Guard tự động (`assertClean`) **tự bắt** residue thật → finding F-16-01 có bằng chứng khách quan (exit code 1 → 0),
  không phải suy đoán của agent.
- Mọi fix design đều có **số đo/grep before/after**.
- **Bác bỏ 2 kết luận SAI** (secret, rác) bằng đo dứt khoát 2 chiều → đúng kỷ luật V4.
- Tự phát hiện + sửa 1 lỗi tự gây (thiếu `--audit` ghi đè evidence v15).
- Round 2 chạy lại toàn bộ → hội tụ (0 finding mới).

**Yếu / giới hạn (ghi rõ, không giấu):**
- `smallTargets=115` chưa triage lại trong phiên — nhưng **trùng số v14** đã triage 0 REAL; không có mục mới.
  Đây là **cảnh báo**, không phải lỗi (24–44px = khuyến nghị AAA; AA = 24px).
- **MCP playwright không dùng được** (thiếu Chrome channel) → dùng chrome-devtools MCP. Giới hạn môi trường.
- Không đo được upload+assess speaking bằng media thật (mic giả → FAILED đúng thiết kế).
- Perf: chỉ đo, **không có win** → không tối ưu (đúng P5, không "tạo việc").

## Rủi ro còn lại
- `?sort=` bị bỏ qua trên lessons/decks/admin-ex (F-13-11) — **thiết kế**, chưa đo tác động người dùng.
- `admin exercises q=` LIKE `%kw%` ~163ms — đặc tính, không index được.

## Đối chiếu spec ↔ plan ↔ tasks ↔ evidence
- `spec.md` (O1–O9, R1–R9) → `plan.md` (Phase 0–8) → `tasks.md` (T0–T8) → `evidence/**` (mọi phase có artifact).
- `checklist.md`: mọi tiêu chí đạt.
- `converge.md`: mọi requirement có artifact.

## Kết luận
Không có mâu thuẫn giữa spec/plan/tasks/evidence. Vòng này hoàn tất với **8 finding FIXED** + **2 kết luận SAI bác bỏ**,
0 regression, hệ thống hội tụ qua 2 vòng.
