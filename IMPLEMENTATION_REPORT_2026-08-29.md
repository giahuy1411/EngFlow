# EngFlow — Báo cáo triển khai hậu-audit (2026-08-29)

Mục tiêu: triển khai toàn bộ kế hoạch hậu-audit (TRỪ Flyway), verify bằng test/build/browser, commit theo nhóm logic và push lên `main`.

## Tóm tắt kết quả

| Hạng mục | Trạng thái | Bằng chứng |
|---|---|---|
| Phase A — Backup + restart stack | ✅ | branch `audit-2026-08-28-backup` (commit `7b139d6`); Redis `allkeys-lru` verify qua `CONFIG GET`; ngrok tunnel `sepay-webhook` hoạt động |
| Phase B — Migration AppButton | ✅ | ~110 nút thô → AppButton trên 32 views; build `✓ built`; Playwright verify Lessons filter (1463→498) |
| Phase C — ease-bounce + icon stroke 2.5 | ✅ | `.lucide { stroke-width: 2.5 }` trong design-system.css; AppButton mặc định ease-bounce |
| Phase D — Dọn legacy + tap target | ✅ | Xóa `bauhaus/` (StreakCalendar→common/, 5 file dead) + `geo/` (dead); `app-btn--sm` 36→44px; main.css bỏ dead helpers |
| Phase E — CSP + regression tests | ✅ | `script-src` bỏ `'unsafe-inline'` (verify header live); 56/56 frontend tests; 128/128 backend tests |
| Phase F — Verify + commit + push | ⏳ | đang chạy |

## Phát hiện then chốt trong phiên

1. **Nhánh phân kỳ**: `main` là ancestor của `feat/exercise-system` → push main là fast-forward an toàn.
2. **Working tree tích tụ**: 1415 thay đổi chưa commit từ nhiều phiên trước (design-system `components/ui/`, backend tests, MinIO deps). Đã backup sang `audit-2026-08-28-backup` rồi khôi phục nền tảng sang `feat/exercise-system`.
3. **`components/ui/` chưa từng được import** trên feat branch — toàn bộ app dùng `<button>` thô. Đã khôi phục bộ primitives + migration.
4. **Test infra thiếu**: 11 file `.test.js` tồn tại nhưng `package.json` không có vitest/@vue/test-utils/jsdom → đã thêm, chạy được 56 tests.
5. **Bootstrap dep khai tử**: `bootstrap` vẫn trong package.json nhưng 0 tham chiếu → đã xóa.
6. **CSP cho SPA**: backend chỉ serve JSON (`/api/**`), không serve HTML → `'unsafe-inline'` trong `script-src` là thừa (API response không có script; JSON-LD `type="application/ld+json"` không phải executable). Đã bỏ. `style-src 'unsafe-inline'` giữ lại (Vite/Tailwind inject inline style). Nonce-based CSP đầy đủ cần thực hiện ở tầng hosting HTML (nginx/CDN prod), không phải ở API header.

## Quyết định kiến trúc

- **Hướng design system**: chọn `components/ui/` (AppButton/StickerCard/AppInput/FormField) theo `design-system.md`, thay vì `components/geo/` (GeoButton) — vì `design-system.md` mô tả đúng bộ `ui/` và nó có sẵn tests.
- **Nút giữ nguyên `<button>` thô** (có chủ đích, kèm comment): answer-option buttons trong game (QuizGame, MemoryMatch, ListeningGame, MixedGame) vì cần dynamic per-state coloring (correct=green/wrong=pink) mà variants không cover; icon-only circular controls (audio play, record toggle); ARIA tab buttons (LessonLayout).

## Files thay đổi chính

- `frontend/src/components/ui/*` — khôi phục 18 primitives + tests
- `frontend/src/assets/design-system.css` — `.lucide` stroke, tap target 44px
- `frontend/src/views/**` — migration AppButton (32 views)
- `frontend/src/components/bauhaus/`, `geo/` — xóa (dead)
- `frontend/package.json`, `vite.config.js` — vitest infra, bỏ bootstrap
- `frontend/src/views/luyentu/QuizGame.test.js` — 4 regression tests
- `src/test/java/.../ExerciseServiceAdminAnswerRegressionTest.java` — 4 regression tests
- `src/main/java/.../config/SecurityConfig.java` — CSP hardening
- `ngrok.yml` — tạo lại file config tunnel (trước đó là thư mục rỗng gây lỗi mount)

## Cách verify

- Backend: `mvn test` → 128 pass / 0 fail / 0 error (24 classes)
- Frontend: `npx vitest run` → 56 pass / 0 fail (12 files); `npx vite build` → ✓ built
- Browser (Playwright/Brave): Lessons filter tương tác đúng, Decks render StickerCard, header/footer intact
- CSP: `curl -I` xác nhận header live không còn `unsafe-inline` ở script-src

## Việc còn lại / khuyến nghị

- Kích hoạt Flyway (đã loại trừ theo yêu cầu) — còn `ddl-auto=update`.
- Nonce-based CSP đầy đủ ở tầng hosting prod (nginx/CDN).
- Cân nhắc gộp `feat/exercise-system` → `main` qua PR thay vì push trực tiếp (đang push trực tiếp theo yêu cầu).
