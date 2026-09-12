# Plan — audit-v7-full

**Spec**: [spec.md](./spec.md) · **Clarify**: [clarify.md](./clarify.md) · **Checklist**: [checklist.md](./checklist.md)
**Branch/Workspace**: main · **Created**: 2026-09-12 · **Status**: Implemented (converge pending)

## Summary

Đóng vòng kiểm toán toàn diện EngFlow: quét tĩnh + động backend (53 endpoint), audit DB SQL Server
trong Docker (24 bảng, 46.7k rows) kèm đo trước/sau tối ưu index, verify frontend bằng Playwright
trên design system Playful Geometric (constitution P6 — Be Vietnam Pro tuyệt đối), vá mọi finding
P1/P2 phát hiện được, rồi chạy lại toàn bộ sweep để khép kín vòng test.

## Technical Context

- **Language**: Java 25 (Spring Boot 4.0.6) + Vue 3 / Vite / Tailwind 3.4 / Vitest
- **Storage**: SQL Server 2019 (Docker `engflow-sqlserver`), Redis 7 (rate-limit/quota), MinIO (media)
- **Testing**: mvnw surefire (baseline tăng dần 314 → 327), vitest 79/16 files, Playwright MCP DOM-assert
- **Target**: local Docker Compose :8080/:5173
- **Project type**: web app monorepo (backend + frontend/ + MCP sidecar không đổi)

## Constitution Check (P1–P8)

| Principle | Pass | Note |
|---|---|---|
| P1 test-first | ✅ | Mọi fix (F34, F54–F62) đều kèm regression test; suite tăng 314→327 |
| P2 vertical slice | ✅ | Fix đi hết controller→service→repository→test trong một package |
| P3 local AI only | ✅ | Không thêm dịch vụ AI ngoài; Ollama/Whisper giữ nguyên |
| P4 schema via entity | ✅ | F41 nvarchar(500→2000) đổi entity + SQL một lần, không migration |
| P5 boundary grep | ✅ | Xóa dead getAllExercises sau khi grep cả 2 phía (C-03a) |
| P6 BVP single font | ✅ | 0 non-BVP computed; index.html nạp đúng 5 weight có usage |
| P7 UI tiếng Việt | ✅ | Handler mới trả detail tiếng Việt ("Giá trị không hợp lệ…") |
| P8 no secret in repo | ✅ | MediaSigner dùng jwt.secret env; không hardcode key mới |

## Design (phased)

1. **Quét**: subagent static-audit (82 findings), DB live audit (sqlcmd), design audit qua Playwright.
2. **Phân loại**: P1 security/data-integrity → vá ngay; P2 perf → đo trước/sau; P3 cosmetics → patch gọn; false-positive → retract có bằng chứng.
3. **Vá theo wave**: wave-1 (F34, F41, F47, F51, indexes, C-03a), wave-2 (F53–F62), wave-3 frontend-design (F63–F70).
4. **Verify**: mvnw full suite + vitest + docker rebuild + live probes + Playwright DOM assertions.
5. **Khép kín**: rerun sweep r2 + CRUD r3 với token mới; cập nhật checklist; REPORT.md.

Key design decisions:
- **F55 media**: ký URL hết hạn (HMAC-SHA256 `exp|sig`, TTL 6h) thay vì mở proxy; fallback owner-auth cho key cũ chưa ký (33 recordings). Lý do: `<audio src>` không gửi được header Authorization; JWT-in-query lọt vào access log.
- **F54**: 2 lớp — rule cụ thể `.authenticated()` ĐẶT TRƯỚC rule permitAll rộng (Spring first-match-wins) + guard `isRealUser()` trong controller (phòng rule đổi thứ tự trong tương lai).
- **F56**: GET structure thành read-only trả section ảo; mutation chỉ qua endpoint admin có authz.
- **F57**: chỉ chặn underpayment (amount < order), overpayment vẫn accept + log (quy tắc nghiệp vụ SEPay thông dụng).

## Verification evidence

- `audit-v7-backend-test6.log`: **327 tests, 0 failures, BUILD SUCCESS**
- vitest: **79/79, 16 files** (chạy sau wave-3)
- Live probes (container rebuild 14:59): guest attempts **401** (hết NPE-500), lessons public **200**, unsigned media **403**, tampered sig **403**, expired **403**, signed **200 audio/wav 563KB**, playback `currentTime` chạy trong admin UI
- F56: DELETE sections lesson 445 → GET structure 200 → COUNT vẫn **0** (không INSERT)
- F61: 13× `/api/ai/quota-status` authed → 404×10 rồi **429×3** đúng bucket 10/phút
- F62: `level=NOT_A_LEVEL` → **400** ProblemDetail tiếng Việt; `level=INTERMEDIATE` → **200**
- F53: `/api/lessons/445/exercises/content` **6 `<details><summary>ANSWER</summary>`**; browser: 6 details, đóng mặc định, 0 console errors
- F63/F67/F68 (browser): donut computed `rgb(139,92,246)`; drawer 375px open x=-288→0, auto-close khi navigate; tables overflow-x auto

## Subreports

- [backend-static-audit.md](./subreports/backend-static-audit.md) — 82 findings F01–F82 có mã/failure-mode/fix
- [db-audit.md](./subreports/db-audit.md) — inventory + data quality + perf đo trước/sau + khuyến nghị cần user quyết
- [design-audit.md](./subreports/design-audit.md) — Playwright + token audit + mobile
