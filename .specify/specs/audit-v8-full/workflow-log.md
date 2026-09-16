# Workflow Log — audit-v8-full

> Deliverable của **Task 1** (comprehensive-audit-redesign) + **Phase 0** (PLAN.md).
> Log này là nguồn sự thật để agent khác tiếp nhận scope mà không cần lịch sử hội thoại.

**Run ID**: `r1-20260916` · **Ngày**: 2026-09-16 · **Vòng**: Round 1 (broad discovery) của lần chạy thứ 4

---

## 1. Repository state (đo bằng command, không suy đoán)

| Mục | Giá trị | Lệnh |
|---|---|---|
| Commit | `c6143fb828c7ed7d115bc5b3235017df7541903d` | `git rev-parse HEAD` |
| Branch | `main` | `git branch --show-current` |
| Remote | `origin https://github.com/giahuy1411/EngFlow.git` | `git remote -v` |
| Java | OpenJDK **Temurin 25.0.3+9** LTS | `java -version` |
| Node | **v24.16.0** | `node --version` |
| npm | **11.13.0** | `npm --version` |
| Docker | **29.4.3** build 055a478 | `docker --version` |
| CI config | **không có** (`.circleci/` absent; không có `.github/workflows`) | `Test-Path .circleci` |

### 1.1 File đã sửa nhưng chưa commit (giữ nguyên, KHÔNG reset)

```
 M AGENTS.md
 M frontend/src/assets/app-layout.css
 M frontend/src/main.js
 M frontend/src/utils/markdown.js
 M frontend/src/views/admin/AdminExercises.vue
 M frontend/src/views/admin/AdminLessonBuilder.vue
 M frontend/src/views/admin/AdminVideoLessons.vue
 M frontend/src/views/lessons/LessonContent.vue
 M src/main/java/com/datn/engflow/controller/GameController.java
 M src/main/java/com/datn/engflow/controller/LessonController.java
 M src/main/java/com/datn/engflow/controller/LessonExerciseController.java
 M src/main/java/com/datn/engflow/controller/LessonStructureController.java
 M src/main/java/com/datn/engflow/controller/speaking/SpeakingPromptController.java
 M src/main/java/com/datn/engflow/controller/video/VideoLessonController.java
 M src/main/java/com/datn/engflow/model/dto/video/VideoDtos.java
 M src/main/java/com/datn/engflow/repository/ExerciseRepository.java
 M src/main/java/com/datn/engflow/repository/LessonRepository.java
 M src/main/java/com/datn/engflow/security/RateLimitFilter.java
 M src/main/java/com/datn/engflow/service/AiExerciseService.java
 M src/main/java/com/datn/engflow/service/ExerciseService.java
 M src/main/java/com/datn/engflow/service/LessonService.java
 M src/main/java/com/datn/engflow/service/LessonSubmissionService.java
 M src/main/java/com/datn/engflow/service/VideoLessonService.java
 M src/test/java/com/datn/engflow/security/RateLimitFilterTest.java
 M src/test/java/com/datn/engflow/service/AiExerciseServiceParsingTest.java
 M src/test/java/com/datn/engflow/service/ExerciseServiceAdminPaginationTest.java
?? .specify/specs/audit-v8-full/
?? docs/superpowers/
?? frontend/src/utils/sanitize-a11y.js
?? frontend/src/utils/sanitize-a11y.test.js
?? frontend/src/views/admin/AdminVideoLessons.test.js
?? src/main/java/com/datn/engflow/model/dto/projection/LessonTitle.java
?? src/main/java/com/datn/engflow/security/SafeUploadNames.java
?? src/test/java/com/datn/engflow/controller/AuditV8DraftLessonVisibilityTest.java
?? src/test/java/com/datn/engflow/controller/AuditV8UploadXssTest.java
?? src/test/java/com/datn/engflow/controller/GameControllerSubmitTypeTest.java
?? src/test/java/com/datn/engflow/controller/video/AuditV8VideoLessonDraftVisibilityTest.java
?? src/test/java/com/datn/engflow/controller/video/AuditV8VideoLessonUpdateTranscriptTest.java
?? sweep/audit-20260913/
?? sweep/v8/
?? tasks/evidence/ui-sweep-2026-09-12/
```

**Quy tắc bất biến**: cấm `git reset --hard`, cấm `git checkout --`. Toàn bộ thay đổi trên là của người dùng/phiên trước và phải bảo toàn.

---

## 2. Services & ports (không lộ credential)

Đo bằng `docker ps` lúc 2026-09-16 11:2x (+07):

| Container | Trạng thái | Port |
|---|---|---|
| `engflow-backend` | Up 9 h | `8080` |
| `engflow-sqlserver` | Up 11 h (healthy) | `1433` |
| `engflow-frontend` | Up 11 h | `5173` |
| `engflow-redis` | Up 11 h | `6379` |
| `engflow-minio` | Up 11 h | `9000`–`9001` |
| `engflow-whisper` | Up 11 h | `9002` |
| `engflow-tts` | Up 11 h | `8001` |
| `engflow-tailscale` | Up 11 h | — |

Ollama chạy trên **host** tại `localhost:11434`, container truy cập qua `host.docker.internal:11434`.

---

## 3. Baseline claims — nhãn `historical` vs `fresh`

| Claim | Nhãn | Nguồn |
|---|---|---|
| backend `369 tests` | **fresh (Round 1, 2026-09-16 11:27)** | `sweep/v8/t-r1-baseline.log` — `Tests run: 369, Failures: 0, Errors: 0, Skipped: 0 / BUILD SUCCESS` |
| frontend `85 tests / 18 files` | **fresh (Round 1, 2026-09-16 11:28)** | `sweep/v8/t-fe-r1.log` — `Test Files 18 passed (18) / Tests 85 passed (85)` |
| `vite build` entry `176.68 kB` (gzip 67.34) | **fresh (Round 1, 2026-09-16)** | `sweep/v8/build-r1.log` |
| 139 annotation / 97 handler | **superseded** | đếm lại Round 1: **149 annotation / 27 controller** (`grep @*Mapping` + `glob controller/**/*.java`) |
| DB parity `1471/43737/76/127/28/15/4/126/14/5/38`, 0 orphan | historical → **re-verify ở Task 8** | `sweep/v8/parity.sql` (audit-v7/v8 phiên trước) |

> **Lưu ý đếm**: mọi số liệu lấy từ **run log mới**, không đếm file XML trong `target/surefire-reports`
> (XML stale của class đã xóa từng làm aggregate ảo +8 — xem AGENTS.md).

---

## 4. Audit scope (chốt)

**Trong phạm vi**:
- Toàn bộ **149 mapping annotation** trên **27 controller** (`src/main/java/**/controller/**/*.java`) — kể cả route alias (`video-prompts`/`speaking-prompts`, `video-submissions`/`speaking-submissions`) và route media (`/api/resources/**`, `/api/v1/media/**`).
- Toàn bộ route frontend trong `frontend/src/router/index.js` — **32 route** (kể cả 9 route con admin dưới `/admin`).
- Flow người dùng/admin chính: auth, lessons/exercises, streak, search/sort, CRUD, decks/games, speaking, video, vocabulary, AI, premium, upload.
- DB SQL Server (schema/FK/orphan/index/query-stats/hygiene) + performance before/after.
- Design system Playful Geometric + **Be Vietnam Pro là font duy nhất**.

**Ngoài phạm vi (explicit exclusions)**:
- Triển khai production / deploy.
- Xóa dữ liệu phá hoại khi chưa được duyệt.
- Tích hợp cloud mới (Cloudinary **đã** tồn tại và đang dùng thật — chỉ verify, không thay).
- Refactor suy đoán / redesign theo cảm tính.
- `POST /api/admin/exercises/seed`, `generate-batch?force` — các endpoint hủy diệt hàng loạt **không được gọi**; chỉ verify guard.

---

## 5. Blocked-state rules (PLAN.md Phase 2 mặc định)

Khi không có người dùng để hỏi, áp dụng mặc định:

1. Read-only test trước.
2. Mutation test chỉ trên dữ liệu có prefix `audit_<runId>` / `ZZ r1`.
3. Backup DB trước mọi DML hàng loạt.
4. Cleanup bằng manifest ID.
5. **Không** xóa dữ liệu không thuộc run này.
6. Real service kiểm tra khi môi trường khả dụng; nếu không → `BLOCKED`, **không** nâng thành `PASS`.

---

## 6. `.dsh` skills — manifest (chỉ ghi cái THẬT SỰ tồn tại & được nạp)

| Skill | Trạng thái | Dùng cho |
|---|---|---|
| `superpower-executing-plans` / `superpower-verification-before-completion` | có trong catalog phiên | kỷ luật "không tuyên bố pass nếu thiếu output mới" |
| `accessibility` (WCAG 2.2) | có trong catalog phiên | Task 11: alt / focus / reduced-motion / target-size |
| `java-coding-standards`, `java-springboot` | có | Task 13: chuẩn code khi sửa Java |
| `karpathy-guidelines` | có | thay đổi tối thiểu, không gold-plate |
| `speckit-*` (constitution→report) | có | khung workflow bắt buộc |
| `prompt-master` | có | Task 2: chuẩn hóa prompt, gỡ mâu thuẫn font/framework |
| `generate-tests` / `generate-test-cases` | có | Task 13: regression test |
| `addyosmani-*` (performance, security-hardening, code-review…) | có | các phase phân tích tương ứng |
| Plugin CircleCI | **không áp dụng** | không có `.circleci/` trong repo (đã kiểm) |
| Browser MCP (`chrome-devtools-mcp`, `playwright-mcp`) | **không khả dụng trong phiên này** | dùng harness Chromium/`playwright-core` toàn cục tại `sweep/v8/ui/` — theo AGENTS.md |

> **Quy tắc**: không gọi tên MCP chưa được discover/verify (PLAN.md Phase 3).

---

## 7. Acceptance — Task 1

- [x] Ghi `git status --short` + commit + version (mục 1).
- [x] Ghi services/ports không lộ credential (mục 2).
- [x] Baseline cũ dán nhãn `historical`, baseline mới đo lại có log (mục 3).
- [x] Chốt scope: backend mapping + frontend route + flow + DB + design system (mục 4).
- [x] Ghi exclusion tường minh (mục 4).
- [x] Verify `.dsh` skills + đường dẫn nguồn (mục 6).
- [x] **Acceptance**: scope bàn giao được cho agent khác mà không cần lịch sử hội thoại.
