# Implementation Plan — Audit & Optimize EngFlow V1

**Branch:** main (audit-only, không tạo branch riêng) | **Spec:** `specs/audit-v1/spec.md`

## Technical Context

- **Backend:** Spring Boot 3, Java 17, SQL Server 2019 (`engflow-sqlserver` :1434→1433; host port đã đổi thành 1433 sau audit-v3, 2026-09-02),
  Redis (`engflow-redis`), MinIO, container `engflow-backend` :8080. 25 controllers,
  40 services, 21 entities. Test: `mvnw.cmd test` = 196.
- **Frontend:** Vue 3 + Vite (container `engflow-frontend` :5173, bind-mount
  `./frontend:/app` → hot reload từ host). Tailwind v3 + design tokens trong
  `tailwind.config.js` + `src/assets/design-system.css`. Test: vitest = 73/14.
- **AI:** Ollama host `host.docker.internal:11434` (`qwen2.5:1.5b` sinh bài,
  `qwen2.5:3b` rubric), Whisper sidecar :9002.
- **Verification tools:** chrome-devtools-mcp, playwright-mcp, curl/pwsh.
- **Scripts SpecKit:** chưa có (`.specify/` bootstrapped ở đợt này) → paths được
  resolve thủ công: `FEATURE_DIR = .specify/specs/audit-v1/`.

## Constitution Check

| Nguyên tắc | Trạng thái |
|---|---|
| P1 Baseline xanh | ✅ Đã đo đầu phiên: 196 + 73 đều pass |
| P2 Phân lớp | ✅ Chỉ đọc, không đổi kiến trúc |
| P3 Hibernate quản lý schema | ✅ Không viết migration; tối ưu index = SQL trực tiếp |
| P4 AI local | ✅ Không thêm AI cloud mới |
| P5 Hiệu năng phải đo | ✅ Plan có bước đo trước/sau bắt buộc |
| P6 Design tokens trung tâm | ✅ Mọi sửa font/style qua tokens, không hardcode |
| P7 UI tiếng Việt + a11y | ✅ Có task audit riêng |
| P8 Bằng chứng runtime | ✅ Toàn bộ smoke test là HTTP/browser thật |

**Gates:** Không vi phạm. CHANGE scope = audit + patch nhỏ có kiểm chứng.

## Phase 0 — Research (đã hoàn tất inline)

- **Decision 1:** Frontend sửa source trên host → vào container qua bind-mount
  (verify: docker-compose.yml:125 `./frontend:/app`). Vite dev server tự reload.
- **Rationale:** Không rebuild frontend container; container chạy Vite dev mode.
- **Decision 2:** Font Be Vietnam Pro đã có trong Google Fonts link + tailwind +
  CSS vars, nhưng index.html nạp thừa Plus Jakarta Sans (bỏ được ~chục KB) và
  tailwind.config.js có 2 block `fontFamily` + 2 key `borderRadius.blob` trùng
  — JS object literal, key sau ghi đè key trước (không crash nhưng là dead config,
  dễ gây nhầm khi maintain). **Vá:** gộp block, bỏ font thừa.
- **Decision 3:** DB audit qua `docker exec engflow-sqlserver sqlcmd` theo
  AGENTS.md. Không chạm ddl. Tối ưu index chỉ khi có bằng chứng từ query plan.
- **Alternatives considered:** Sửa tailwind bằng config merge phức tạp — bỏ,
  chỉ gộp duplicate keys là đủ và đúng tinh thần "surgical changes".

## Phase 1 — Design (contracts = smoke-test matrix)

Xem `tasks.md` — audit task không tạo data-model/contracts mới, contract chính
là **bộ smoke-test checklist** được liệt kê trong tasks.

## Quickstart (validation guide)

1. `docker ps` — đủ 8 containers `engflow-*` (đã verify đầu phiên).
2. Backend: `cmd /c "mvnw.cmd test"` → 196 pass (đã verify).
3. Frontend: `npx vitest run` → 73/14 pass (đã verify).
4. Smoke API: đăng nhập user/admin → gọi các endpoint chính → 2xx.
5. UI: mở `http://localhost:5173` qua MCP browser → console error = 0,
   computed font-family chứa "Be Vietnam Pro".
6. A11y: Lighthouse accessibility qua chrome-devtools ≥ 90 mục tiêu (tham khảo).
