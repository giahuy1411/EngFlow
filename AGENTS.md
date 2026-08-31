# EngFlow — Rules for AI Agents

Nền tảng học tiếng Anh (capstone). Giao tiếp với người dùng bằng tiếng Việt, thuật ngữ kỹ thuật giữ nguyên tiếng Anh.

## Tech Stack

- **Backend**: Spring Boot 3 (Java 17), SQL Server 2019, Redis, MinIO, Cloudinary. Chạy trong Docker (`engflow-backend` :8080).
- **Frontend**: Vue 3 + Vite (dev :5173), Tailwind, Vitest + jsdom, `lucide-vue-next` icons.
- **AI 100% local**: Ollama `localhost:11434` (`qwen2.5:1.5b` exercises, `qwen2.5:3b` speaking rubric), Whisper sidecar `engflow-whisper` :9002 (faster-whisper base).
- **MCP**: `~/.gemini/antigravity/mcp/engflow-language-mcp/` (FastMCP, stdio) — sinh bài tập qua Ollama, TTS supertonic, export vào Admin API. Dùng python trong `.venv` của nó.

## Commands

- Backend tests: `cmd /c "mvnw.cmd test"` (từ repo root) — baseline xanh: **179 tests**.
- Frontend tests: `Set-Location frontend; cmd /c "npx vitest run"` — baseline: **73 tests / 14 files**.
- Frontend build: `cmd /c "npx vite build"` trong `frontend/`.
- Rebuild backend container: `docker compose up -d --build backend` (code trong container chỉ đổi khi rebuild).
- SQL: `docker exec engflow-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'YourPassword123' -d english_learning -Q "..." -C`.

## Code Conventions

- Vue: `<script setup>`, services module trong `frontend/src/services/*.js` gọi qua `api` instance. Test colocate: `Foo.js` → `Foo.test.js` (hoặc `__tests__/`).
- Java: package `com.datn.engflow`, pattern Controller → Service → Repository, DTO request/response tách riêng, Lombok.
- Git: Conventional Commits (`feat:`, `fix:`, `refactor:`, `chore:`) có scope (`feat(speaking): ...`), tiếng Anh, author `giahuy1411`.
- UI text tiếng Việt; class style theo design system hiện có (border-2 border-foreground, shadow-pop-*).

## Boundaries

- **Không seed data demo khi người dùng đã bỏ tính năng** (ví dụ: achievements đã gỡ triệt để — đừng tái tạo).
- Không thêm dependency mới khi chưa cân nhắc bundle size / license.
- Không commit `.env`, key, file fixture local (`frontend/public/*.wav`).
- Schema DB do Hibernate `ddl-auto=update` quản lý, **Flyway disabled** — không viết migration file, đổi schema bằng SQL trực tiếp + entity.
- Trước khi xóa module: grep tham chiếu cả frontend lẫn backend (kể cả menu tĩnh trong `AdminLayout.vue`, breadcrumb map, router).

## Patterns & Gotchas

- **API login**: `POST /api/auth/login` → token ở `data.data.token || data.token`. Admin path không có GET-by-id cho video lessons — dùng public `/api/v1/video-lessons/{id}` để đọc.
- **Phản hồi API v/v hình dạng**: một số trả `.data`, một số trả plain — đọc service tương ứng trước khi dùng.
- **PowerShell + curl JSON**: đừng dùng `curl -d '...'` (quoting làm hỏng JSON). Viết file UTF-8 **không BOM** (`[IO.File]::WriteAllText`) rồi `--data-binary "@file"`.
- **Ollama probe**: qua browser evaluate sẽ fail cross-origin với :8080 — chạy API call từ tab `localhost:5173`.
- **YouTube embed**: xác minh player bằng a11y snapshot (`browser_find "Play video"`), KHÔNG dựa vào `iframe.contentDocument` (cross-origin → null). Error 153 trong Playwright embed là **false negative** (referrer block).
- **Listening exercises**: 89/449 bài thiếu `audio_url` (sinh khi MCP venv chưa có supertonic). Fallback giọng máy trình duyệt: `frontend/src/utils/speech.js` (`speakEnglish`, `blankOutForSpeech` — che `____` thành `...` để không lộ đáp án fill-blank).
- **TTS backend** (`CloudTtsService`): tên config `Z.ai` nhưng code gọi Google Cloud TTS, **không có key** → chỉ fail-soft (log warn). Đường TTS thật của dự án là supertonic trong MCP.
- **Whisper sidecar**: POST multipart `/v1/audio/transcriptions`, model field `whisper-1`. Silence → `{"language":"en","text":""}`.
- **scoreTotal speaking**: thang 0-10 (Double), rubric average 3 dims. DB column `score_total` là float.
- jsdom không có `SpeechSynthesisUtterance` — mock `window.SpeechSynthesisUtterance` trong test nếu cần.

## Kiến trúc sinh bài tập (2 đường)

1. **Trong app** (admin UI): `AiExerciseService` → Ollama → async 202 + Redis progress. TTS backend (không key) chỉ dùng cho listening → audio sẽ trống.
2. **MCP Antigravity**: `generate_listening` → supertonic WAV → `POST /api/admin/audio-upload` → Cloudinary → `audioUrl`. **Đây là đường sinh listening có audio.**

## Seed / Demo

- Accounts: `user@gmail.com` / `admin@gmail.com`, password `123456`.
- Video lessons `/videos/1-4` có YouTube ID thật, phụ đề teacher-authored, quiz client-side từ transcript (`VideoLesson.vue buildQuiz`), deck quiz server-side (`GameService.generateQuiz`, Redis, cap 10).
- Tránh demo tính năng TTS cloud (không key). Mic permission cần chuẩn bị sẵn cho Shadowing.
