<!--
Sync Impact Report
Version change: (new) → 1.0.0
Modified principles: N/A (initial ratification)
Added sections: 9 principles, Governance
Removed sections: N/A
Templates requiring updates: ✅ .specify/templates/* created alongside
Follow-up TODOs: none
-->

# EngFlow — Constitution v1.0.0

**Ratified:** 2026-09-01 | **Last amended:** 2026-09-01

## Preamble

Hiến pháp này ràng buộc mọi thay đổi lên EngFlow: nền tảng tự học tiếng Anh
(Spring Boot 3 + SQL Server + Redis + MinIO, Vue 3 + Vite + Tailwind, AI 100% local
qua Ollama/Whisper/MCP). Mọi agent hoặc người đóng góp PHẢI tuân thủ.

## Article I — Nguyên tắc kỹ thuật

### P1. Baseline xanh là tiền đề
Trước và sau mọi thay đổi: backend `mvnw.cmd test` = 196 tests, frontend
`npx vitest run` = 73 tests / 14 files. Mọi PR/commit không giữ baseline xanh
bị từ chối.

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
