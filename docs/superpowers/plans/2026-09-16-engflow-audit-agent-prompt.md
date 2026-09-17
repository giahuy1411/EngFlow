# Prompt giao cho AI agent triển khai audit EngFlow

Bạn là agent chịu trách nhiệm audit và sửa có kiểm chứng cho repository EngFlow tại `C:\Users\ASUS\Documents\LAPTRINH\engflow`.

## Workflow bắt buộc

Thực hiện đúng thứ tự:

`constitution → specify → clarify → checklist → plan → tasks → analyze → implement → converge → analyze lần 2 → taskstoissues nếu cần → report`

Không bỏ qua phase. Các phase read-only phải hoàn tất trước khi sửa code. Nếu phát hiện scope quá lớn, chia thành task độc lập nhưng vẫn giữ traceability về spec.

## Quy tắc không được vi phạm

- Đọc `AGENTS.md`, constitution và plan trước khi hành động.
- Bảo toàn thay đổi chưa commit; không dùng `git reset --hard` hoặc `git checkout --`.
- Không suy đoán kết quả. Mọi kết luận phải có command output, log, screenshot, SQL result hoặc browser evidence.
- Service không chạy được phải ghi `BLOCKED`, không ghi `PASS`.
- Không commit `.env`, secret, token, backup, PII hoặc fixture local.
- Không seed demo data. Mọi dữ liệu test phải có cleanup.
- Mọi bulk DML phải backup trước; DELETE trên filtered index phải bật `SET QUOTED_IDENTIFIER ON;`.
- Backend theo Controller → Service → Repository; frontend dùng Vue `<script setup>` và service module qua `api` instance.
- AI chỉ dùng local Ollama/Whisper hiện có; output AI phải được parse, validate, bound và coi là untrusted data.

## Phạm vi audit

1. Toàn bộ API từ source inventory, kiểm tra no-auth/user/admin, positive và negative cases.
2. Toàn bộ route frontend và flow tương ứng với API bằng Chromium/Playwright/devtools.
3. Bài học, bài tập, grading, streak, login/register/reset, search/sort, CRUD, speaking, video, games, decks, vocabulary, admin và AI.
4. SQL Server trong Docker: schema/entity, FK/orphan, indexes, query stats, data hygiene, backup/restore.
5. Security: authz, rate limit, upload XSS, secrets, error leakage, AI output safety.
6. UI/UX: responsive, accessibility, design tokens, typography, states, interaction, performance.

## Design system authoritative

- Font duy nhất: `Be Vietnam Pro`, không dùng/nạp `Outfit` hoặc `Plus Jakarta Sans`.
- Vue icon package: `lucide-vue-next`, stroke width `2.5`.
- Token source: `frontend/src/assets/design-system.css` và `frontend/tailwind.config.js`.
- Palette: cream `#FFFDF5`, slate `#1E293B`, violet `#8B5CF6`, pink `#F472B6`, amber `#FBBF24`, mint `#34D399`.
- Border mặc định `2px`; radius `8/16/24/full`; hard shadow, không blur.
- Landing có thể dùng spacing lớn; lesson/admin phải ưu tiên mật độ và khả năng thao tác.
- Mobile: target tối thiểu `48px` cho control chính, shadow giảm còn `2px`, ẩn decoration phức tạp.
- Reduced motion: tắt bounce/wiggle/translate/entrance animation khi `prefers-reduced-motion: reduce`.

## UI acceptance

Kiểm tra ở viewport `360`, `768`, `1280`, `1440`, `1920`:

- Không overflow ngang thật; ghi raw `scrollWidth/clientWidth`.
- Computed font-family chỉ chứa Be Vietnam Pro và fallback hợp lệ.
- Token, border, radius, shadow, focus-visible và icon stroke đúng.
- Ảnh có `alt`; ảnh trang trí dùng `alt=""`; màu không phải tín hiệu duy nhất.
- Không có console error hoặc API >=400 ngoài lỗi được dự kiến và kiểm chứng.
- Có loading, empty, error, success state phù hợp.

## Hai vòng kiểm tra

### Vòng 1

Inventory toàn bộ, chạy baseline, reproduce lỗi, fix confirmed findings, thêm regression tests, ghi evidence.

### Vòng 2

Chạy lại toàn bộ ma trận với mức nghiêm ngặt hơn: expired token, wrong role, malformed input, duplicate data, empty search, pagination boundary, invalid upload, malformed AI output, service outage, reduced motion, narrow viewport, slow network và cleanup verification.

## Báo cáo cuối

Báo cáo phải có các bảng:

- Đã kiểm tra
- Đã fix
- Chưa fix
- Blocked
- Not applicable
- Deferred
- Skills đã nạp và mục đích sử dụng
- Command/evidence/exit code

Mỗi finding phải có: ID, severity, reproduction, root cause, affected files, fix, regression test, before/after evidence và trạng thái cuối.

Không viết “hoàn thành”, “pass” hoặc “đã tối ưu” nếu chưa có bằng chứng mới từ lần chạy cuối.
