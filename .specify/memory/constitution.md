<!--
Sync Impact Report
Version change: 1.0.4 → 1.1.0 (MINOR: thêm nguyên tắc/quy định mới)
Modified principles: P1 (đo lại audit-v21 2026-09-28: backend 541, frontend 194 — KHÔNG đổi so v19/v20)
Added sections: Article III — "Sổ quyết định (Decision Registry)" (cơ chế đóng trạng thái)
Removed sections: none
Templates requiring updates: none (registry là file mới `.specify/memory/decisions.md`;
  mẫu §5 REPORT ghi trong AGENTS.md Boundaries)
Follow-up TODOs: none
-->

# EngFlow — Constitution v1.1.0

**Ratified:** 2026-09-01 | **Last amended:** 2026-09-28

## Preamble

Hiến pháp này ràng buộc mọi thay đổi lên EngFlow: nền tảng tự học tiếng Anh
(Spring Boot 3 + SQL Server + Redis + MinIO, Vue 3 + Vite + Tailwind, AI 100% local
qua Ollama/Whisper/MCP). Mọi agent hoặc người đóng góp PHẢI tuân thủ.

## Article I — Nguyên tắc kỹ thuật

### P1. Baseline xanh là tiền đề
Trước và sau mọi thay đổi: backend `mvnw.cmd test` = 537 tests / 0 fail / 0 error / 11 skipped, frontend
`npx vitest run` = 194 tests / 1 skipped / 32 files (con số này là mốc **audit-v19**, đo 2026-09-27; mỗi kỳ
audit PHẢI đo lại và ghi giá trị thực tế). Mọi PR/commit không giữ baseline xanh bị từ chối.

### P2. Kiến trúc phân lớp bất biến
Java: Controller → Service → Repository, DTO request/response tách riêng,
package `com.datn.engflow`. Vue: `<script setup>`, service module
`frontend/src/services/*.js` gọi qua `api` instance. Không bỏ qua tầng.

### P3. Schema DB do Hibernate quản lý
`ddl-auto=update`, Flyway disabled. Không viết migration file. Đổi schema =
SQL trực tiếp + entity, phải kèm kiểm chứng trên container `engflow-sqlserver`.

### P4. AI local là ràng buộc sản phẩm
AI sinh bài/chấm điểm chạy qua Ollama (`qwen2.5:1.5b` exercises, `3b` rubric),
Whisper sidecar :9002. Không thêm dependency cloud AI trả phí mới.

### P5. Hiệu năng được đo, không bịa
Mọi tuyên bố tối ưu phải kèm chỉ số trước/sau (thời gian API, EXPLAIN plan,
bundle size, Lighthouse). Không tối ưu khi chưa đo.

## Article II — Nguyên tắc thiết kế

### P6. Design System "Playful Geometric" + font Be Vietnam Pro
Token trung tâm: `frontend/src/assets/design-system.css` (CSS vars `--geo-*`)
+ `frontend/tailwind.config.js`. Font duy nhất: **Be Vietnam Pro** (headings
800/900, body 400/500). Cấm Outfit / Plus Jakarta Sans trong code mới.
Shadow cứng `pop-*`, border 2px `#1E293B`, radius 8/16/24/full, palette
cream/violet/pink/amber/emerald.

### P7. UI text tiếng Việt, thuật ngữ kỹ thuật tiếng Anh
Copy người dùng bằng tiếng Việt tự nhiên (không AI-cliché). A11y là bắt buộc:
focus-visible, skip-link, contrast AA, `prefers-reduced-motion`.

### P8. Mỗi thay đổi có bằng chứng runtime
CRUD/AI phải được verify bằng API thật (HTTP thật trên :8080) và UI thật
(chrome-devtools/playwright MCP), không dừng ở "code nhìn có vẻ đúng".

## Article III — Governance

- **Amendment:** sửa file này, bump version semver (MAJOR = bỏ/ngược nguyên tắc,
  MINOR = thêm principle, PATCH = chữ nghĩa), ghi Sync Impact Report.
- **Compliance check:** đầu mỗi phiên làm việc, so requests vs Article I/II.
- **Conflict resolution:** AGENTS.md quy định chi tiết vận hành; hiến pháp này
  quyết định khi mâu thuẫn.
- **Complexity budget:** ưu tiên giải pháp nhàm chán, đúng; cấm abstraction
  đơn-use và feature không được yêu cầu.
- **Sổ quyết định (Decision Registry) — cơ chế ĐÓNG trạng thái:** mọi quyết định
  đã chốt (GIỮ / KHÔNG LÀM / ĐÃ GỠ) phải ghi một lần vào
  `.specify/memory/decisions.md` với mã `D-NNN` + ngày + lý do + bằng chứng.
  Vòng audit sau PHẢI đọc sổ này trước khi viết mục "chưa làm": mục khớp một
  `D-NNN` ghi vào nhóm **5a. Đã quyết định (ĐÓNG)**, không phải "còn cần làm".
  Một finding tồn tại ≥2 vòng ở trạng thái "chưa làm" mà **không có mã** bị coi là
  **lỗi quy trình** và phải escalate lên chủ dự án — không được im lặng trôi tiếp.
  Cơ chế này tổng quát hoá quyết định P5.1 (`content_original`), vốn đã chứng minh
  hiệu quả: mục đó tắt hẳn sau 2 vòng, trong khi các mục không có mã lặp 8–11 vòng.
  Guard: `sweep/harness/assert-harness.js` check 9.
