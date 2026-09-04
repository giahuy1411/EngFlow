# Analyze — audit-v6-full (cross-check spec ↔ plan ↔ tasks ↔ phát hiện)

**Ngày:** 2026-09-04 · **Vị trí workflow:** sau tasks, trước/during implement (read-only)

## 1. Ánh xạ yêu cầu gốc → task → bằng chứng

| Yêu cầu user | Task | Bằng chứng đã thu |
|---|---|---|
| Quét toàn bộ codebase | T0.4 (sub-agent static) | Báo cáo static: 0 P1, 2 P2 (F20/F21), 12 P3 — đã reconcile |
| Kiểm tra DB trong Docker | T0.5 (sub-agent DB) | 24 bảng, 24 FK indexed, 5.434 exercise thiếu correctAnswer, lessons 160MB |
| Chạy toàn bộ API | T0.2 + Phase 4 | GET sweep 53 endpoint (0 unexpected 5xx); mutation qua UI E2E |
| Tương tác UI ↔ API | T4.1–T4.5 | CRUD lesson/prompt/exercise/video qua UI + verify DB; lesson submit 4/5; deck quiz |
| Chức năng AI | T4.3 | ai-generate-full 2.4s ✓; assess 19s ✓ (9.0); ai-grade 14.5s ✓; translate 2 dòng ✓; fetch-youtube ✗ (F30) |
| Tối ưu hiệu năng | T2.1–T2.3 | Index composite tạo + đo (7ms→0ms); hot endpoints <100ms warm |
| Verify design system + font BVP | T3.1–T3.4 | 22 trang nonBvp=0; button/card/input computed style khớp spec; mobile shadow 2px ✓ |
| Báo cáo chi tiết | T6.2 | REPORT.md (đang viết) |
| Sub-agent chia nhỏ | T0.4–0.6 | 3 agent song song (static/DB/design) — cả 3 trả kết quả đầy đủ |
| Vá lỗ hổng prompt | spec §2 | G1–G7 đã ghi + xử lý |
| Workflow speckit | T7.1 | constitution→spec→clarify→checklist→plan→tasks→analyze→implement→converge |

## 2. Mâu thuẫn / khoảng trống tìm thấy

1. **tasks.md T4.3 ghi "assess speaking" pass** — thực tế submission 40022 FAILED do LLM timeout 60s (model swap 3b→1.5b). Đã fix F29 (timeout 180s) + verify lại bằng 40023 COMPLETED 9.0. → tasks cần note "cần 2 attempt do cold model".
2. **fetch-youtube chưa pass end-to-end** — F30 (chunking) đã code + test đơn vị xanh, nhưng E2E với transcript 400 dòng vẫn vượt mọi timeout hợp lý (mỗi batch 25 dòng × 16 batch × ~15-60s). Đây là **giới hạn kiến trúc** (đồng bộ, GPU 4GB), không phải bug code. → Ghi vào REPORT "chưa làm được" + đề xuất async.
3. **DB P1 (5.434 exercise thiếu correctAnswer)** — pipeline backfill TỒN TẠI nhưng chưa chạy trên prod data. Chạy thật sẽ mất ~vài giờ GPU. → Để nguyên, ghi "chưa làm" + hướng dẫn chạy.
4. **Design P1 (modal a11y)** — sub-agent đề xuất thay bằng AppModal; tôi chọn vá tại chỗ (role/aria/Escape/focus-restore) vì ít rủi ro hơn cho trang admin đang chạy. Chấp nhận được, ghi rõ trong REPORT.
5. **Không có task nào bị orphan** — mọi F20–F30 đều map vào task Phase 5.

## 3. Kết luận analyze

- Spec ↔ tasks ↔ code: **nhất quán**, 2 mục DoD chưa đạt hoàn toàn (fetch-youtube E2E, backfill data) — đã chuyển thành ghi chú "chưa làm" trong REPORT, không phải lỗi pipeline.
- Không cần thêm task mới ngoài Phase 5/6 đang chạy.
