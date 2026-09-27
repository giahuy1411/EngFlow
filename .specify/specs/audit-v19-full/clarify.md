# audit-v19-full — clarify (quyết định của người dùng)

Câu trả lời dưới đây là **ràng buộc** của phiên này.

---

## Q1 — Mức độ vòng này?
**Trả lời:** *"Vòng audit-v19-full đầy đủ"* → Phase 0–8 như v18; artifact `.specify/specs/audit-v19-full/`.

## Q2 — SePay chữ ký THẬT?
**Trả lời:** *"Chuyển khoản thật (như v17)"* → người dùng quét QR + chuyển ~10.000đ; tôi tạo order + verify
webhook settle. **KHÔNG xoá row tiền thật.** Ghi rõ lệch parity. Fallback Test-mode simulator (miễn phí) ghi tài liệu.

## Q3 — Hiệu năng admin-exercise?
**Trả lời:** *"Theo đuổi (đo before/after)"* → áp `ExerciseLessonProjection` cho admin path; đo logical reads
before/after; **chỉ giữ nếu win rõ** (P5).

## Q4 — Nhánh git?
**Trả lời:** (kế thừa) giữ `audit-v15-full`, không tạo nhánh mới.

## Q5 — W1 AI gloss: fix thế nào?
**Trả lời:** (kế thừa tinh thần "sửa tận gốc") → sửa **prompt** (nêu ngôn ngữ từng field + "NO Chinese characters")
**VÀ** thêm **guard CJK sau parse** (prompt không đủ tin cậy với model 1.5b).

## Q6 — W2: harness hay app?
**Trả lời:** cả hai — **sửa harness** (loại inline link + label checkbox, bỏ cắt list) **VÀ** **sửa app** (word-chip
≥24px) nếu đo xác nhận là vi phạm thật.

## Q7 — Xử lý lỗi thật / docs / rác / báo cáo?
**Trả lời:** (kế thừa v17/v18) apply fixes + commit; cập nhật mọi docs + prompt doc + demo doc; 2 vòng; dọn rác;
REPORT.md đủ 4 mục.

## Q8 — Workflow?
**Trả lời:** (kế thừa) constitution → specify → clarify → checklist → plan → tasks → implement → converge →
analyze (giữa + cuối) → taskstoissues (BỎ QUA).
