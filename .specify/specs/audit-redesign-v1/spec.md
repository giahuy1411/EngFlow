# Spec: Comprehensive Audit + Playful Geometric Redesign + Font Migration

## 1. Concept & Vision

EngFlow là nền tảng học tiếng Anh capstone (Spring Boot 3 + SQL Server + Redis + MinIO + Vue 3 + Vite + Tailwind, AI 100% local qua Ollama/Whisper/MCP). User yêu cầu:

1. **Comprehensive audit** toàn bộ codebase + database, kiểm tra backend API end-to-end (CRUD + AI), frontend UI tương ứng, hiệu năng.
2. **Apply Playful Geometric Design System** (đã có ở prompt) lên toàn bộ frontend, bao gồm:
   - Thay toàn bộ font **Outfit** / **Plus Jakarta Sans** trong prompt gốc bằng **Be Vietnam Pro** (constitution P6 đã yêu cầu, cần enforce xuyên suốt).
   - Cấm sót font cũ.
3. **Phát hiện lỗ hổng** trong prompt gốc của user (mục đích: nếu có chỗ dễ gây bug, nêu rõ trước khi implement).
4. **Mọi bug phát hiện phải fix tận gốc** với regression test; mọi thay đổi phải có bằng chứng runtime.

## 2. Audit Scope

### 2.1 Backend
- **26 controllers** (auth, lesson, exercise, admin×6, game, deck, flashcard, SRS, vocab, speaking×2, video×2, payment, leaderboard, progress, streak, dashboard, AI vocab, media proxy)
- **36 services**, **19 repositories**
- **AI features**: Ollama qwen2.5:1.5b (exercises), 3b (rubric), Whisper :9002, MCP supertonic
- **Mục tiêu verify**: auth flow, CRUD lessons/exercises, AI exercise generation, backfill pipeline, payment webhook+polling, speaking assessment, video progress, leaderboard, SRS, deck quiz, streak.

### 2.2 Frontend
- **89 Vue files** trong `frontend/src/` (Home, Auth, Lessons, Luyện tập, Speaking, Videos, Premium, Admin)
- **18 UI components** (`AppButton`, `AppInput`, `AppModal`, `AppTable`, `AppToast`, ...)
- **Design system audit**:
  - Font: phát hiện chỗ nào dùng `Outfit` hoặc `Plus Jakarta Sans` thì thay `Be Vietnam Pro`.
  - Token colors: `#7C3AED` (accent), `#DB2777` (secondary), `#FBBF24` (tertiary), `#34D399` (quaternary), `#1E293B` (foreground), `#FFFDF5` (background).
  - Hard shadows: `4px 4px 0px #1E293B`.
  - Border-2 + border-foreground.
  - Radius: 8/16/24/full.
  - WCAG AA contrast cho text.
- **Responsive**: mobile-first, 48px+ tap targets, motion respects `prefers-reduced-motion`.

### 2.3 Database (Docker `engflow-sqlserver`)
- 19 tables, 24 FK, schema do Hibernate `ddl-auto=update` quản lý.
- Audit: indexes, FK integrity, orphan records, query performance.

### 2.4 Performance
- N+1 detection trên repository methods
- Slow endpoints (>500ms read, >2s write)
- Redis usage verification
- Bundle size frontend

## 3. Design System — Playful Geometric (Font: Be Vietnam Pro)

### 3.1 Color Tokens (Light Mode)
```
background:        #FFFDF5
foreground:        #1E293B
muted:             #F1F5F9
mutedForeground:   #64748B
accent:            #7C3AED  (WCAG AA ✅)
secondary:         #DB2777  (WCAG AA ✅)
tertiary:          #B45309  (WCAG AA ✅ — user prompt có #FBBF24 nhưng đổi thành dark amber để text-safe)
quaternary:        #34D399
border:            #E2E8F0
input:             #FFFFFF
card:              #FFFFFF
ring:              #7C3AED
```

### 3.2 Typography
- **Font duy nhất**: Be Vietnam Pro (thay thế Outfit/Plus Jakarta Sans)
- **Headings**: Bold 700 / ExtraBold 800
- **Body**: Regular 400 / Medium 500
- **Scale ratio**: 1.25

### 3.3 Shadows & Effects
```
box-shadow:        4px 4px 0px 0px #1E293B
box-shadow-hover:  6px 6px 0px 0px #1E293B
box-shadow-active: 2px 2px 0px 0px #1E293B
```

### 3.4 Radius & Border
```
radius-sm:  8px
radius-md:  16px
radius-lg:  24px
radius-full: 9999px
border-width: 2px (chunky)
```

## 4. Lỗ hổng trong prompt gốc của user (cần vá trước khi implement)

| # | Lỗ hổng | Vấn đề | Vá |
|---|---------|--------|-----|
| 1 | Font mismatch | Prompt gốc định Outfit (headings) + Plus Jakarta Sans (body). User yêu cầu thay Be Vietnam Pro → cần enforce duy nhất 1 font family trong toàn bộ tokens, CSS, tailwind config, Google Fonts links. | Xóa Outfit/Plus Jakarta Sans khỏi `frontend/index.html`, `tailwind.config.js`, mọi CSS file, mọi Vue component. Google Fonts chỉ load Be Vietnam Pro. |
| 2 | Token inconsistency | Prompt gốc có `accent: #8B5CF6` (Tailwind violet-500) nhưng code hiện dùng `#7C3AED` (violet-600). Tương tự `tertiary: #FBBF24` (amber-400) vs code dùng `#B45309` (amber-700) cho text-safety. | Ghi rõ trong design-system.css là code hiện tại đã chọn bản text-safe (WCAG AA). Tài liệu giữ 2 bộ: bản pure (decorative) + bản text-safe. |
| 3 | Thiếu `prefers-reduced-motion` handling | Prompt yêu cầu bouncy/wiggle nhưng quên a11y motion. | Mọi keyframe wiggle/bounce phải wrap `@media (prefers-reduced-motion: no-preference)`. |
| 4 | Border-foreground nặng trên mobile | `border-2 border-foreground` + `shadow-pop-*` trên diện tích lớn sẽ tốn GPU trên mobile. | Áp dụng scale: mobile shadow 2px thay vì 4px, border giữ 2px. |
| 5 | Icon "Enclosed in shapes" dễ gây trùng tab focus | Icon trong circle làm mờ focus ring. | Focus state dùng `outline-4 outline-accent outline-offset-2` để ring nổi bật hơn shadow. |

## 5. Functional Requirements

### 5.1 Audit (mỗi cái phải có evidence)
- **FR-1**: Gọi được mỗi backend API endpoint qua curl, ghi nhận HTTP status + body cho ít nhất 1 happy-path và 1 error-path.
- **FR-2**: Mỗi Vue view tương ứng phải navigate được, render không lỗi console, tương tác với backend thành công.
- **FR-3**: Phát hiện font ngoài Be Vietnam Pro trong built CSS / HTML phải báo cáo.
- **FR-4**: Phát hiện N+1 query, missing index, slow query phải báo cáo kèm EXPLAIN plan.

### 5.2 Fix
- **FR-5**: Mỗi bug phát hiện có root cause + fix + regression test.
- **FR-6**: Mỗi sửa đổi giữ baseline test xanh (backend 223/223, frontend 73/73).

### 5.3 Apply Design System
- **FR-7**: Toàn bộ font load qua Google Fonts chỉ Be Vietnam Pro (weights: 400, 500, 700, 800).
- **FR-8**: Toàn bộ CSS variables trong `frontend/src/assets/design-system.css` match Section 3.
- **FR-9**: Tailwind config sử dụng đúng color values, không có hardcode hex.
- **FR-10**: Motion respects `prefers-reduced-motion`.

## 6. Acceptance Criteria

- [ ] Toàn bộ backend API đã audit (≥90% endpoints, loại trừ webhook internal, health checks).
- [ ] Toàn bộ frontend view đã navigate qua Chrome DevTools MCP / Playwright.
- [ ] Không còn `Outfit` / `Plus Jakarta Sans` trong codebase (grep negative test).
- [ ] Design tokens khớp Section 3.
- [ ] Không bug WCAG AA contrast.
- [ ] Backend test suite vẫn pass.
- [ ] Frontend test suite vẫn pass.
- [ ] Database: 0 orphan records, indexes trên FK columns đầy đủ.
- [ ] Performance: API read < 500ms (P95), write < 2s.
- [ ] AI features: exercise gen + speaking assess chạy được.
- [ ] Docker services healthy.
- [ ] Báo cáo tổng kết: REPORT.md tại `.specify/specs/audit-redesign-v1/REPORT.md`.

## 7. Out of Scope

- CI/CD pipeline
- Deploy production
- Tính năng mới ngoài design system enforcement
- Tăng test coverage (chỉ verify regression sau fix)

## 8. Risks & Assumptions

- Premium subscription gate có thể block test một số endpoint — sử dụng admin token bypass nếu cần.
- MCP supertonic không chạy thì audio TTS/listening sẽ trống — acceptable.
- Ollama load 1.5b → 3b swap mất ~5.7s — budget trong test.
- AiAnswerBackfillService đã có regression test cho block-aware logic (commit trước) — không viết lại.

## 9. Assumptions

- Database Docker container chạy healthy.
- Backend + frontend tests đều xanh ở baseline.
- Tokens admin/user có sẵn trong `%TEMP%\engflow_admin_token.txt` / `engflow_user_token.txt`.
- Subagent có thể chạy song song (background) để parallel audit.
