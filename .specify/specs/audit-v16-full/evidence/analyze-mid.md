# audit-v16-full — analyze GIỮA KỲ (sau Phase 4)

Theo workflow người dùng yêu cầu: `analyze` chạy **giữa chừng** để kiểm tra đối chiếu, không đợi tới cuối.

## Đối chiếu spec ↔ thực thi ↔ bằng chứng (giữa kỳ)

| Mục tiêu | Yêu cầu | Bằng chứng | Trạng thái |
|---|---|---|---|
| O1 API | Mọi endpoint có probe | `api-sweep.json` 143/0; `endpoint-inventory.json` 121/137/135 | ✅ |
| O2 UI | Mọi route × role render đúng | `ui-sweep.json` 0 guard/console/api/page error | ✅ |
| O3 DB | 0 orphan, parity khớp | `db-integrity.md` 22 FK/0 orphan; parity khớp | ✅ |
| O4 Design | Khớp prompt | `design-conformance.md` 0 badFont/legacy/token/overflow | ✅ |
| O5 Perf | before/after có số | `perf-analysis.md` 34 endpoint, median 11.9ms | ✅ (before) |
| O6 E2E | 6 nhóm API↔UI | `e2e-core-features.md` | ✅ |
| O7 2 vòng | vòng 2 toàn diện hơn | *chưa (Phase 7)* | ⏳ |
| O8 Docs | cập nhật + demo doc | demo doc viết lại ✅; README/CLAUDE drift sửa ✅; còn AGENTS/.agents/.gitignore | ⏳ |
| O9 Rác | dọn sạch | *chưa (Phase 8.5)* | ⏳ |

## Kiểm tra chất lượng bằng chứng (giữa kỳ)

**Điểm mạnh:**
- Mọi finding đều có **probe thứ 2** (F-16-01: trước/sau exit code; các drift: grep sau fix).
- Guard tự động hoạt động: `assertClean` **bắt được** residue thật (F-16-01) — không phải suy đoán.
- Bác bỏ được 1 kết luận SAI (secret `.agents/mcp_config.json`) bằng đo dứt khoát → đúng tinh thần V4.
- Tự phát hiện + tự sửa lỗi ghi đè evidence v15 (`--audit` thiếu) ở Phase 0.

**Điểm yếu / cần chú ý:**
- `smallTargets=115` là **cảnh báo chưa triage lại** trong vòng này — nhưng trùng số v14 (đã triage 0 REAL).
  Cần ghi rõ là "đã triage ở v14, không có mục mới" (đã ghi ở `ui-sweep.md`).
- Perf vòng 1 **không có win** → đúng tinh thần P5 (không tối ưu khi chưa đo). Không nên "tạo việc" để có vẻ có tối ưu.
- MCP playwright **không dùng được** (`Chromium distribution 'chrome' is not found`) → dùng chrome-devtools MCP thay thế
  (đã verify hoạt động). Ghi vào REPORT là **giới hạn môi trường**, không phải bỏ sót.
- `scripts/figma-export/node_modules` (19MB, gitignored) + 6 script chết — cần xác minh 2 chiều rồi dọn (Phase 8.5).

## Hành động rút ra cho các phase còn lại
1. Phase 5: chốt perf (không fix win — ghi lý do).
2. Phase 6: review chéo 6 fix design + 1 fix harness.
3. Phase 7: vòng 2 chạy lại toàn bộ trên build cuối.
4. Phase 8: hoàn tất docs (AGENTS/.agents/.gitignore) + cleanup + REPORT.
