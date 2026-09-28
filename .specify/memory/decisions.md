# EngFlow — Decision Registry (sổ quyết định đã ĐÓNG)

> **Luật:** mọi quyết định đã CHỐT (GIỮ / KHÔNG LÀM / ĐÃ GỠ) ghi ở đây **MỘT LẦN**, có mã.
> Vòng audit sau **PHẢI** đọc file này TRƯỚC khi viết mục "chưa làm" trong `REPORT.md`.
> Mục nào khớp một `D-NNN` dưới đây **KHÔNG còn là "chưa làm"** → ghi vào nhóm
> **5a. Đã quyết định (ĐÓNG)**, KHÔNG phải **5b. Còn thật sự cần làm**.
>
> **Token máy kiểm:** guard (`sweep/harness/assert-harness.js` check 9) lấy literal trong
> backtick ở cột **Hạng mục** làm token nhận dạng. Nếu token đó xuất hiện ở nhóm 5b của
> REPORT vòng mới ⇒ **FAIL** (tái liệt kê việc đã đóng).
>
> **Vì sao có file này:** quét 21 vòng audit cho thấy `content_original` lặp **2 vòng rồi tắt hẳn**
> (vì có quyết định P5.1 + dòng Boundaries), trong khi timezone lặp **8 vòng**, GitHub-issue **10 vòng**,
> real-money **11 vòng** — vì các quyết định đó **không có mã** để vòng sau tra. Registry tổng quát hoá
> cơ chế P5.1 cho mọi quyết định.

**Ngày tạo:** 2026-09-28 · **Số mục:** 9 · **ID scheme:** `D-NNN` (tăng đơn điệu, KHÔNG tái sử dụng)

| ID | Ngày | Hạng mục | Quyết định | Lý do (đo được) | Trạng thái | Bằng chứng |
|---|---|---|---|---|---|---|
| D-001 | 2026-09-12 | Cột `content_original` | GIỮ vĩnh viễn | backfill chỉ đọc `content`; 2 reader còn lại flag-off; chi phí 69MB/230MB ≈ 0 so với rủi ro mất bản gốc re-scrape được | ĐÓNG | AGENTS.md:44 · plan P5.1 · audit-v7 REPORT:96 |
| D-002 | 2026-09-15 | `.env.bak-*` (4 file) | GIỮ | gitignored (`.gitignore:30-32,89`); chứa secret thật, cần cho rollback | ĐÓNG | audit-v20 REPORT:189 · audit-v21 REPORT §5 |
| D-003 | 2026-09-16 | `scripts/figma-export/node_modules` | GIỮ | `sweep/harness/cls-probe.js:36` **cố ý** resolve `playwright-core` từ đó; `git ls-files` = 0 (không commit) | ĐÓNG | AGENTS.md:75 · audit-v16→v21 REPORT |
| D-004 | 2026-09-28 | Annotation `PremiumRequired` | GIỮ | placeholder bảo mật có chủ đích; premium thật chạy qua `UserService.hasPremiumAccess()`; xoá phải sửa cả CLAUDE.md | ĐÓNG | CLAUDE.md:57,70 · audit-v12:21 · v20:187 · v21:132 |
| D-005 | 2026-09-28 | 2 method repository `findByUserIdOrderBySubmittedAtDesc` / `findAllByOrderBySubmittedAtDesc` | **ĐÃ XOÁ** (audit-v21, 2026-09-28) | 0 call site toàn repo + 0 test ref (đã grep `Grep` toàn repo, không chỉ `src/main`); bản `Page` mới là đường dùng thật; bản `List` còn rủi ro khi bảng lớn. Xoá xong: backend 541/0/0/11 BUILD SUCCESS; runtime `GET /api/v1/speaking-submissions` → 200; api-sweep 145 pass/0 fail | ĐÓNG | audit-v20:186 · v21:131 · evidence/after-deadcode-*.log |
| D-006 | 2026-09-28 | Utility `polka` / `diagonal-stripe` | KHÔNG LÀM | OPTIONAL trong prompt gốc; `grep polka\|diagonal\|stripes frontend/src` = **0 hit**, 0 sai lệch đo được; hiến pháp cấm feature không được yêu cầu | ĐÓNG | audit-v21 REPORT §5 · prompt-rewritten-v21.md |
| D-007 | 2026-09-28 | Migrate timezone DB sang `migrate timezone` (UTC) | KHÔNG LÀM (theo dõi) | Convention naive giờ VN (+07) đã verify 2 lần (v7, v13); DB có **1 nguồn ghi duy nhất** (backend JVM `TZ=Asia/Ho_Chi_Minh`); **0 default constraint** dùng `GETDATE()` trong DB live; đường date-granular miễn nhiễm. **Chỉ migrate khi xuất hiện consumer thứ hai** (ví dụ service ngoài đọc DB). | ĐÓNG | AGENTS.md:59 · audit-v13 REPORT |
| D-008 | 2026-09-28 | GitHub issues (`taskstoissues`) | KHÔNG LÀM | `gh` chưa đăng nhập; audit dùng artifact nội bộ `.specify/specs/` làm sổ theo dõi. **Chỉ tạo khi có `gh` auth** (điều kiện mở khoá). | ĐÓNG | audit-v3 REPORT · v14→v19, v21 REPORT |
| D-009 | 2026-09-28 | Chuyển `tiền thật` / webhook SePay chữ ký thật | PERMANENT-BOUNDARY | Audit local **không bao giờ** chuyển tiền thật. Đã verify **phía nhận**: HMAC đúng/sai, replay window ±5', amount-mismatch, settle, idempotent (`sweep/harness/g6-sepay-signed.py`). Đó là mức tối đa của audit — ngừng liệt kê là "chưa làm". | ĐÓNG | AGENTS.md:83,93 · audit-v11→v21 REPORT |

## Ghi chú vận hành

- **Cột Hạng mục PHẢI chứa literal trong backtick** — guard dùng làm token. Entry không có token
  nhận dạng được thì không được bảo vệ khỏi bị tái liệt kê.
- **Thêm quyết định mới:** append dòng `D-NNN` kế tiếp (tăng đơn điệu) **và** tăng `**Số mục:**`
  **trong cùng lúc**. Không tái sử dụng ID đã dùng.
- **KHÔNG** đưa vào đây: mục one-off, mục còn thật sự cần làm (đó là nhóm 5b), mục out-of-scope
  của một vòng (chỉ là "vòng đó không làm"), mục BLOCKED tạm thời (đó là BLOCKED board).
- **Cập nhật khi điều kiện đổi:** mục D-007/D-008 có điều kiện mở khoá rõ — khi điều kiện đó thoả
  (có consumer thứ hai / có `gh` auth), **sửa entry** thay vì mở lại như "chưa làm".
