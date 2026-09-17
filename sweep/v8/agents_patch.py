import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="ascii", errors="backslashreplace")
p = "AGENTS.md"
s = open(p, encoding="utf-8-sig").read()

old_backend = """- Backend tests: `cmd /c \"mvnw.cmd test\"` (từ repo root) — baseline xanh: **332 tests** (audit-v7: 327 + 5 backfill-orchestration regression P3: dry-run checkpoint / limit boundary / lesson-scope / deterministic-mode). Lưu ý: XML stale trong `target/surefire-reports` của class đã xóa (`UserServiceUnlimitedAiGenerationTest`) từng làm aggregate ảo +8 — đếm theo run log, không đếm file XML."""
new_backend = """- Backend tests: `cmd /c \"mvnw.cmd test\"` (từ repo root) — baseline xanh: **354 tests** (audit-v8: 332 + 22 regression mới: 9 upload-XSS `AuditV8UploadXssTest`, 7 `GameControllerSubmitTypeTest`, 3 rate-limit bucket routing, 3 AI parse/guard). Lưu ý: XML stale trong `target/surefire-reports` của class đã xóa (`UserServiceUnlimitedAiGenerationTest`) từng làm aggregate ảo +8 — đếm theo run log, không đếm file XML."""

old_fe = """- Frontend tests: `Set-Location frontend; cmd /c \"npx vitest run\"` — baseline: **79 tests / 16 files**."""
new_fe = """- Frontend tests: `Set-Location frontend; cmd /c \"npx vitest run\"` — baseline: **82 tests / 17 files** (audit-v8 +3 `src/utils/sanitize-a11y.test.js`)."""

old_conv = """- Rebuild backend container: `docker compose up -d --build backend` (code trong container chỉ đổi khi rebuild)."""
new_conv = old_conv + """
- SQL runner dùng chung: `python sweep/v8/sqlrun.py <file.sql>` (pipe vào `docker exec -i engflow-sqlserver sqlcmd`). DELETE trên bảng có filtered index (`IX_uvp_due`) **bắt buộc** `SET QUOTED_IDENTIFIER ON;` ở đầu batch.
- Browser harness (không có MCP browser trong môi trường này): `cmd /c \"set NODE_PATH=%APPDATA%\\npm\\node_modules&& node <file>.js\"` — playwright-core toàn cục + Chromium cache, scripts ở `sweep/v8/` (p1/p2/p3a/p3b + `ui/`)."""

old_bound = """- Không commit `.env`, key, file fixture local (`frontend/public/*.wav`)."""
new_bound = old_bound + """
- **Upload → `/api/resources/**` là surface bảo mật, không phải chỗ tiện tay**: route permitAll và same-origin với SPA (Vite proxy `/api`), nên mọi file có extension browser-render được (.html/.svg/.js) = stored XSS đọc JWT trong localStorage. Đã đo end-to-end bằng Chromium thật TRƯỚC khi fix (2026-09-14). Write qua `SafeUploadNames.extensionOf`, serve qua `contentTypeFor` + `forceDownload` — thêm writer/route mới phải đi qua 2 hàm đó.
- Cloudinary vẫn fallback `demo` creds (không có `CLOUDINARY_*` trong `.env`) → `POST /api/auth/avatar/upload` 500, `POST /api/admin/audio-upload` 400. Đây là **hạn chế môi trường đang mở**, không phải bug code; đừng verify tính năng cloud-upload bằng 2 endpoint này cho tới khi có key thật."""

old_patt = """- **Whisper sidecar**: POST multipart `/v1/audio/transcriptions`"""
new_patt = """- **API login**: token ở `data.data.token || data.token`; JWT TTL **900 s** → sweep dài phải tự refresh (harness `sweep/v8/lib.js` đã làm). Global rate limit 100/phút/IP nên sweep ~250 probe cần flush bucket của chính nó (`rate_limit:*` trong redis) — đây là thao tác harness, không phải app write.
- **Rate-limit bucket** (`RateLimitFilter`): `:auth` 20, `:mail` 5, `:global` 100, `:ai` 10, `:upload` 15, `:order` 10 — prefix phải khớp route THẬT (audit-v8 F83: `:order` từng trỏ `/api/payments/create-order` trong khi route là `/api/v1/payment/create-order` → bucket chết, đo bằng burst thô 13/13 200; sau fix 10×200 rồi 429).
- **Đừng tin `scrollWidth > clientWidth` một mình**: Chromium để chỗ 15 px cho scrollbar → lệch ~13–15px là false positive. Đo bằng raw numbers + loại element có ancestor `overflow` (harness đã làm), rồi mới kết luận overflow.
- **`generateAll` coi `count` là trần** (F84) và MC **cấm trùng phương án** (F85) — 2 guard này tồn tại vì `qwen2.5:1.5b` đo được overshoot 10× (count=3 → 30 rows) và sinh `["…","best","best"]`.
- **Admin list không được `JOIN FETCH` entity lớn**: `findAdminPage` từng kéo `lesson.content` + `content_original` cho 20 row = **95k logical reads/367 ms**; pattern đúng là `JOIN` phẳng + 1 batch projection (`LessonTitle`, `LessonListProjection`). Đo lại bằng `sys.dm_exec_query_stats` sau restart (plan cache reset).
- **DOMPurify là singleton** → muốn siết a11y/content cho mọi call site thì `addHook` ở `frontend/src/utils/sanitize-a11y.js`, và **import từ module lazy** (`utils/markdown.js`, `views/lessons/LessonContent.vue`) — import ở `main.js` đẩy DOMPurify vào entry bundle (+28 kB, đã đo và đã revert).
- **Whisper sidecar**: POST multipart `/v1/audio/transcriptions`"""

for a, b in [(old_backend, new_backend), (old_fe, new_fe), (old_conv, new_conv), (old_bound, new_bound), (old_patt, new_patt)]:
    n = s.count(a)
    print(("OK  " if n == 1 else "MISS") + " " + a[:48].encode("ascii", "replace").decode())
    if n == 1:
        s = s.replace(a, b)
open(p, "w", encoding="utf-8", newline="").write(s)
print("AGENTS.md written, len", len(s))
