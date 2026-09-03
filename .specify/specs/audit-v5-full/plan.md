# Plan: audit-v5-full

**Spec:** `spec.md` · **Constitution:** v1.0.1 (P1 baselines, P5 measured perf, P6 Be Vietnam Pro, P8 runtime evidence)

## 1. Chiến lược theo lớp

| Lớp | Việc | Kỹ thuật |
|-----|------|----------|
| Cấu hình | `.env` token/URL, show-sql, MSSQL memory | Đọc từng biến + đo độ dài; `sp_configure` qua init-db.sql vì image bỏ qua env |
| Security | `@EnableMethodSecurity`, ResponseStatusException | Patch 2 file Java, rebuild container baked-jar |
| Data contract | Guard UI cho MATCHING/FILL_BLANK placeholder | Filter trong `parsedOptions`, fallback trong `MatchingExercise` — KHÔNG sửa seed |
| Design system | Token drift, danger/warning/success, font 100% BVP, flat colors | `tailwind.config.js` + `design-system.css` + `index.html` |
| Perf | show-sql off, N+1 snapshot, cache probe | Env-overridable default false; batch query `findBySectionIds` |
| Verify | 2 MCP (playwright + chrome-devtools), sweep, tests | Mỗi claim = 1 bằng chứng runtime (P8) |

## 2. Thứ tự thực thi

1. Fix cấu hình (F1, F4, F13) → rebuild → xác nhận pollingEnabled + rubric 3b
2. Patch Java (F2, F3) → `mvnw test` → rebuild → probe 403
3. Design tokens + font (F5–F7, F10, F14) → vitest + build → computed-style verify
4. Data guards UI (F8, F9) → vitest → walkthrough lesson 41881
5. Perf (F11, F12) → measure before/after → rebuild → sweep 67/67
6. E2E toàn bộ §3 spec bằng playwright; chrome-devtools cho perf trace
7. Converge: soát lại spec vs code, ghi REPORT

## 3. Quyết định kiến trúc

- **Không** viết Flyway migration (constitution/AGENTS.md) — data seed hỏng xử lý bằng UI guard.
- **Không** thêm dependency (bundle size) — guard bằng JS thuần trong component sẵn có.
- `min-height` footer thay vì skeleton: rẻ, không đổi markup; CLS còn lại do font-swap FOUT được
  chấp nhận ghi nhận (dev-mode Vite unminified, prod build khác — đo trong REPORT).
- show-sql qua `${SPRING_JPA_SHOW_SQL:false}`: dev vẫn bật được bằng env, không mất khả năng debug.

## 4. Đo lường (P5)

| Metric | Before | After |
|--------|--------|-------|
| GET /api/lessons page (warm) | 39ms | 24ms |
| Log lines / 10 requests | ~7500/giờ nền | 0 |
| Dictionary cache hit | 835ms cold | 11–14ms warm |
| SQL Server max memory | unlimited (host 7.7GB) | 2048MB |
| Backend tests | 221 | 221 (không đổi) |
| Frontend tests | 73/14 files | 73/14 files (không đổi) |
| API sweep | 67/67 | 67/67 (sau rebuild) |
| LCP home (dev) | 513ms | 366–427ms |
| CLS home (dev) | 0.17 | 0.18 (footer fixed; còn font-swap FOUT — xem REPORT) |
