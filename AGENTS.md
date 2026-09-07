# EngFlow — Rules for AI Agents

Nền tảng học tiếng Anh (capstone). Giao tiếp với người dùng bằng tiếng Việt, thuật ngữ kỹ thuật giữ nguyên tiếng Anh.

## Tech Stack

- **Backend**: Spring Boot 3 (Java 17), SQL Server 2019, Redis, MinIO, Cloudinary. Chạy trong Docker (`engflow-backend` :8080).
- **Frontend**: Vue 3 + Vite (dev :5173), Tailwind, Vitest + jsdom, `lucide-vue-next` icons.
- **AI 100% local**: Ollama `localhost:11434` (`qwen2.5:1.5b` exercises, `qwen2.5:3b` speaking rubric), Whisper sidecar `engflow-whisper` :9002 (faster-whisper base).
- **MCP**: `~/.gemini/antigravity/mcp/engflow-language-mcp/` (FastMCP, stdio) — sinh bài tập qua Ollama, TTS supertonic, export vào Admin API. Dùng python trong `.venv` của nó.

## Commands

- Backend tests: `cmd /c "mvnw.cmd test"` (từ repo root) — baseline xanh: **196 tests**.
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
- **TTS backend**: `SupertonicProxyTtsService` gọi sidecar supertonic (`/synthesize`) — đường TTS duy nhất. Class `CloudTtsService` ("Z.ai", thực chất gọi Google Cloud TTS, không key) đã xóa khỏi codebase (dead code từ restore `fad776d`).
- **Model local đã đo trên máy này (RTX 2050 4GB, 2025-09-01)**: `qwen2.5:1.5b` = 82 tok/s (GPU), `qwen2.5:3b` = 46.5 tok/s (GPU) — chênh 1.75x là vật lý params, flash attention/ctx nhỏ không thu hẹp được. Đã test 6 model: gemma2:2b + qwen3:1.7b fail format hoàn toàn; llama3.2:3b (2.9GB) không vừa VRAM → hybrid CPU chậm 5x; phi3:mini hybrid. **1.5b KHÔNG đủ chấm rubric tiếng Việt** (2/2 fail: copy điểm ví dụ, feedback tiếng Anh) → rubric phải dùng 3b.
- **Ollama host env (đã setx persistent)**: `OLLAMA_MAX_LOADED_MODELS=1` — GPU 4GB chỉ chứa 1 model; không có nó, model thứ 2 bị nhét CPU → call 558s (đã đo). `OLLAMA_FLASH_ATTENTION=1`. Model swap khi đổi tính năng: ~5.7s (bình thường).
- **Temperature sinh bài = 0.5** (không phải 0.7): A/B đo valid-new 6 vs 2. Model nhỏ vẫn dao động mạnh (có run 0 valid) — guards + retry đa attempt là tầng chịu trách nhiệm chính, đừng kỳ vọng temperature thay được pipeline.
- **Whisper sidecar**: POST multipart `/v1/audio/transcriptions`, model field `whisper-1`. Silence → `{"language":"en","text":""}`.
- **scoreTotal speaking**: thang 0-10 (Double), rubric average 3 dims. DB column `score_total` là float.
- jsdom không có `SpeechSynthesisUtterance` — mock `window.SpeechSynthesisUtterance` trong test nếu cần.

## Kiến trúc sinh bài tập (2 đường)

1. **Trong app** (admin UI): `AiExerciseService` → Ollama `qwen2.5:1.5b` → async 202 + Redis progress. Pipeline gồm: few-shot prompt (example lặp CUỐI prompt — attention decay), JSON salvage 3 lớp (fences → array regex → object salvage), schema validate, MATCHING slash-pair repair (`"a / b"` → `"a|b"`), example-copy guard, dedup within-batch + cross-lesson, tích lũy partial qua attempts, AI review. TTS backend supertonic chỉ dùng cho listening; sidecar không chạy thì audio trống (fallback giọng trình duyệt).
2. **MCP Antigravity**: `generate_listening` → supertonic WAV → `POST /api/admin/audio-upload` → Cloudinary → `audioUrl`. **Đây là đường sinh listening có audio.**

- **MATCHING contract frontend**: `MatchingExercise.vue` chỉ parse options `"left|right"` + correctAnswer `"l=r,..."` (MUST). Java prompt cũ dùng `:::` — đã sửa, đừng quay lại.
- **qwen2.5:1.5b hành vi đã quan sát**: copy few-shot example verbatim (chặn bằng signature match); drift MATCHING thành `["left / right", ...]` (repair); content dài nhấn chìm format instructions (cap 500 chars như MCP); JSON array thường bọc `MATCHING: {...}` không có `[` (salvage layer 3).

## Seed / Demo

- Accounts: `user@gmail.com` / `admin@gmail.com`, password `123456`.
- Video lessons `/videos/1-4` có YouTube ID thật, phụ đề teacher-authored, quiz client-side từ transcript (`VideoLesson.vue buildQuiz`), deck quiz server-side (`GameService.generateQuiz`, Redis, cap 10).
- Tránh demo tính năng TTS cloud (không key). Mic permission cần chuẩn bị sẵn cho Shadowing.
