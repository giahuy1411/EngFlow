# Constitution compliance check — audit-v12-full

**Constitution:** `.specify/memory/constitution.md` v1.0.1 (ratified 2026-09-01, amended 2026-09-03)
**Checked:** 2026-09-21 (+07) · **Branch:** `audit-streak-review` · **Checkpoint:** `30bc9b6`

Mỗi nguyên tắc được đối chiếu với công việc dự kiến. `AMEND` nghĩa là **số liệu** trong hiến pháp đã cũ và phải
đo lại — không phải nguyên tắc sai.

| # | Nguyên tắc | Trạng thái | Bằng chứng / hành động |
|---|---|---|---|
| **P1** | Baseline xanh là tiền đề — số phải đo lại mỗi kỳ | **AMEND (chỉ số)** | Hiến pháp ghi backend **221**, frontend **73/14 file** (mốc audit-v3). v12 đo: backend **485**, frontend **119 passed/23 file**. Quy tắc (đếm từ run log, cấm `surefire-reports` XML) giữ nguyên. |
| **P2** | Kiến trúc phân lớp bất biến | **COMPLIANT** | v12 chỉ đọc/kiểm các tầng, không tái cấu trúc. |
| **P3** | Schema do Hibernate `ddl-auto=update`; Flyway disabled | **COMPLIANT** | Không viết migration. DB scratch dùng để **chứng minh hiệu lực** constraint rồi DROP. |
| **P4** | AI local là ràng buộc sản phẩm (Ollama/Whisper) | **COMPLIANT** | Chỉ probe đường local. Không thêm dependency cloud AI trả phí. |
| **P5** | Hiệu năng đo, không bịa | **COMPLIANT — trọng tâm Phase 6** | Mọi tuyên bố tối ưu kèm số trước/sau; từ chối tối ưu phải ghi lý do. |
| **P6** | Design System "Playful Geometric" + font **Be Vietnam Pro** duy nhất | **COMPLIANT — yêu cầu ĐÃ ĐẠT** | Đo DOM live: `distinctFonts = ["\"Be Vietnam Pro\", system-ui, sans-serif"]`. Source: `index.html` chỉ nạp BVP; `tailwind.config.js` map sans/heading/mono → BVP; `design-system.css --geo-font` = BVP; 0 hit Outfit/Plus Jakarta trong `frontend/src`. **Yêu cầu "thay toàn bộ font" không cần làm gì.** |
| **P7** | UI tiếng Việt; a11y bắt buộc (focus-visible, skip-link, contrast AA, reduced-motion) | **KIỂM LẠI (v11 đã fix)** | v11 sửa 165→0 vi phạm contrast. v12 **đo lại** bằng probe composite đúng, không kế thừa. |
| **P8** | Mỗi thay đổi có bằng chứng runtime (HTTP thật + UI thật qua MCP) | **COMPLIANT** | Cả hai MCP đã verify sống (`list_pages` → page; `browser_tabs` → tab). v12 dùng chrome-devtools làm driver hạng nhất (v11 gần như chỉ `list_pages`). |

## Governance

- **Compliance check per-session:** thực hiện ở trên, trước khi làm.
- **Conflict resolution:** AGENTS.md quy định vận hành; hiến pháp quyết khi mâu thuẫn.
- **Complexity budget:** v12 thêm test, đo lường, bằng chứng — không thêm abstraction, dependency hay feature.

## Amendments required

**P1** — chỉ số: backend `221 → 485`, frontend `73/14 → 119/23`. Bump **PATCH** (`1.0.1 → 1.0.2`) kèm Sync Impact
Report. Không đổi nội dung nguyên tắc.

**P6** — không sửa. Yêu cầu font đã đạt; v12 ghi nhận việc **verify**.
