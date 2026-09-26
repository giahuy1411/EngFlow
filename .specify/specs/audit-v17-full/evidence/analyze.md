# audit-v17-full — analyze (cuối kỳ)

## Chất lượng bằng chứng

**Mạnh:**
- **Vòng 2 tự bắt 2 finding MEDIUM thật** (F-17-11 500 ngắt quãng, F-17-12 timeout 30 s) mà vòng 1 bỏ lỡ —
  bằng chứng cụ thể cho giá trị của yêu cầu "chạy 2 vòng". Cả hai có **test hồi quy** + verify live.
- **Review chéo đối kháng bắt 2 defect trong chính fix của tôi** (F-17-09 fallback sót, F-17-10 test false-pass)
  — và reviewer **mutation-test** chứng minh. Tôi **đối chiếu lại từng claim** trước khi sửa (reviewer cũng có thể sai).
- **Phase U (MCP thủ công)** đi từng chức năng: mọi bước có snapshot + console + network; API↔UI đối chiếu.
- Mọi fix design có **số đo before/after**; mọi probe tự dọn (`assertClean CLEAN`).
- **Bác bỏ 1 kết luận SAI** (F-17-08 tier-order) bằng cách đọc code — đúng kỷ luật.

**Yếu / giới hạn (ghi rõ, không giấu):**
- **Playwright MCP BLOCKED** (thiếu Chrome channel) → engine 2 dùng harness `playwright-core` headless. Không
  phải "đã dùng Playwright MCP".
- **F-17-07 chưa xử lý:** `exercise_attempts` tăng 28→37 (một phần do residue cũ trước phiên), chưa truy được hết
  nguồn → **không xoá** để tránh hại dữ liệu học viên thật. Đây là **việc còn lại**, không phải "đã xong".
- **F-17-05/F-17-06** để `OPEN`: F-17-05 là phụ thuộc upstream (không phải bug code); F-17-06 là harness (không sửa app).
- **`smallTargets=115`** chưa triage lại trong phiên — trùng số v14/v16 đã triage 0 REAL (24–44px = AAA).
- **Perf không có win** → không tối ưu (P5). Không "tạo việc".

## Rủi ro còn lại

- Từ điển ngoài ~20 s (F-17-05): demo `hello` lần đầu có thể chậm → **phải warm cache trước demo**.
- `exercise_attempts` residue (F-17-07) làm "nhiễu" khi so số liệu DB giữa các vòng.
- 504 AI khi model swap lạnh: đã nâng timeout nhưng vẫn phụ thuộc GPU 4 GB.

## Đối chiếu spec ↔ plan ↔ tasks ↔ evidence

- `spec.md` (O1–O9, R1–R11) → `plan.md` (Phase 0–8 + U + D) → `tasks.md` (T0–T8 + TU + TD) → `evidence/**`.
- Mọi O/R đều có artifact; gap còn lại liệt kê ở `converge.md`.
- Coverage: 121 annotation / 137 expanded / 135 distinct — `api-sweep` phủ 143 probe; 4 chức năng demo:
  24 CONFIRMED + 4 sửa (coverage guard: mọi `file:line` trong demo doc đã đối chiếu).

## Điểm khác biệt của vòng này

| | v16 | v17 |
|---|---|---|
| Trọng tâm | sweep + 8 finding | **Phase U MCP thủ công từng chức năng** + đi sâu 4 chức năng demo |
| Finding MEDIUM | 1 (harness) | **2 (AI 500 + timeout)** |
| Review chéo bắt defect trong fix | 4 | **2 (F-17-09/10)** |
| Prompt | dùng như chuẩn | **kiểm lỗ hổng + sửa** (4 lỗ hổng) |
| Baseline | 515/178 | **520/183** |
