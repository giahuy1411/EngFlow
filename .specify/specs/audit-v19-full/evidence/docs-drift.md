# audit-v19-full — Phase 8: docs drift

**Ngày:** 2026-09-27 · Mọi số đo phiên này.

| File | Trước | Sau | Lý do |
|---|---|---|---|
| `AGENTS.md` | backend 537 (audit-v18) | **541** (audit-v19, +4 `AiVocabServiceLanguageGuardTest`) | baseline mới |
| `AGENTS.md` | frontend "(audit-v18)" | **"(audit-v19, xác nhận không đổi)"** | đo lại |
| `AGENTS.md` | parity "(sau audit-v18)" | **"(sau audit-v19)"** | nhãn |
| `AGENTS.md` | harness default `audit-v18-full` | **`audit-v19-full`** | namespace vòng |
| `AGENTS.md` | — | thêm **4 bẫy mới**: W1 AI language guard · W2 smallTargets WCAG 2.5.8 · W3 index `IX_exercises_order_id` · api-sweep dọn PENDING (đừng chạy khi chờ chuyển khoản) | bài học v19 |
| `.specify/memory/constitution.md` | v1.0.3 | **v1.0.4** | PATCH (đo lại P1, số không đổi) |
| `.specify/feature.json` | `specs/audit-v18-full` | **`specs/audit-v19-full`** | con trỏ |
| `.gitignore` | whitelist v18 | thêm whitelist **v19** evidence | REPORT trích log |
| `README.md` | 537/194 | **giữ nguyên** (số không đổi) | — |

## Số đã kiểm chứng
Controller 25 · views 46 · UI primitives 17 — không đổi (không sửa cấu trúc).

## Demo doc
`docs/demo-engflow-4-chuc-nang.md` — mọi `file:line` `sed` đối chiếu **tồn tại và đúng** (xem `demo-claims.md`);
2 mục §4.1/§4.3 đã sửa ở v18. Không có drift mới ở v19.

## Prompt doc
`prompt-rewritten-v19.md` tạo mới (kế thừa v18 + đo lại 4 lỗ hổng).
