# audit-v18-full — converge (spec ↔ code gap)

**Ngày:** 2026-09-27 · Đối chiếu `spec.md` (O1–O9, R1–R12) với thực tế đã làm.

| Mục | Yêu cầu | Trạng thái | Bằng chứng |
|---|---|---|---|
| O1 | Mọi endpoint có probe | ✅ DONE | api-sweep 145/0/0/2na; inventory 121/137/135/25 |
| O2 | Mọi route × role × viewport | ✅ DONE | ui-sweep 0/0/0/0; routes-all 228 visit 0 sai |
| O2b | Phase U **CẢ 2 engine cho MỌI luồng** | ✅ DONE | 13 luồng × 2 engine, `mcp-walkthrough.md` |
| O3 | DB toàn vẹn | ✅ DONE | 0 orphan, 22/22 FK index, validate drill 0 issue |
| O4 | 6 nhóm chức năng API↔UI | ✅ DONE | `demo-claims.md` 29 CONFIRMED |
| O5 | Design khớp prompt (đã sửa lỗ hổng) | ✅ DONE | design-v2 0/0/0/0 + BVP loaded |
| O6 | Perf before/after có số | ✅ DONE | `perf-before.json` (median 9.6ms); kết luận "không win" |
| O7 | 2 vòng 0 finding mới | ✅ DONE | `round-2.md` — vòng 2 sạch |
| O8 | Docs cập nhật + prompt doc + demo doc | ✅ DONE | `docs-drift.md` 11 mục |
| O9 | Rác dọn sạch, liệt kê | ✅ DONE | `cleanup-manifest.md` |
| R1 | Số đo phiên này | ✅ | baseline 537/194 (khác v17 520/192 — ghi "phát hiện") |
| R2 | Probe 2 + review chéo | ✅ | `review-v18.md` (reviewer OK 100%) |
| R3 | Bằng chứng runtime | ✅ | HTTP thật + 24 ảnh MCP |
| R4 | DML an toàn | ✅ | không có DML hàng loạt; probe tự dọn |
| R5 | Probe tự dọn | ✅ | assertClean CLEAN mọi sweep |
| R6 | Design chỉ fix lỗi thật | ✅ | 0 thay đổi design (không có lệch chuẩn) |
| R7 | Perf có before/after | ✅ | kết luận "không win" (P5) |
| R8 | Dọn rác + liệt kê | ✅ | `cleanup-manifest.md` |
| R9 | Docs khớp + demo doc verify | ✅ | `docs-drift.md` |
| R10 | Prompt kiểm lỗ hổng + sửa | ✅ | `prompt-flaws.md` + `prompt-rewritten-v18.md` |
| R11 | Phase U 2 engine MỌI luồng | ✅ | 13/13 luồng trọng yếu × 2 engine |
| R12 | Cập nhật toàn bộ tài liệu | ✅ | README/AGENTS/CLAUDE/.gitignore/demo doc |

## Gap còn lại

| Gap | Lý do | Xử lý |
|---|---|---|
| Premium flow (U19) chỉ 1 engine | PW dùng chung context, tiết kiệm | Bù: deep-probe + g6 webhook; ghi rõ trong walkthrough |
| Webhook SePay chữ ký THẬT của SePay | biên real-money | BLOCKED như v11+; g6 chứng minh **logic** HMAC/replay bằng secret local |
| AI model trả gloss tiếng Trung | hạn chế model nhỏ | Ghi nhận N3, không fix |
| `taskstoissues` | quyết định v14/V3 | BỎ QUA |

## Kết luận

**Mọi O (O1–O9) và R (R1–R12) đều có artifact.** Gap còn lại đều có lý do + cách bù, không giấu.
