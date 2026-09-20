# Baseline — audit-v12-full (đo lại từ đầu)

**Đo:** 2026-09-21 (+07) · **Nhánh:** `audit-streak-review` · **Checkpoint:** `30bc9b6` (tree sạch, 0 dirty)
**Phương pháp:** mọi số dưới đây do chính phiên này chạy. Không sao chép từ v11; chỗ nào trùng thì ghi là **xác nhận**.

---

## 1. Git & checkpoint (T0.1)

```
git log -1  -> 30bc9b6 fix(audit-v11): close 15 defects across a11y contrast, tap targets, overflow and data loss
git status --short -> (rỗng)  dirty = 0
```

Tree sạch trước khi bắt đầu ⇒ mọi thay đổi của v12 đều truy vết và revert được.

## 2. Test suites — đếm từ RUN LOG (T0.3, T0.4)

### Backend — `mvn -o test` (repo root)

```
[INFO] Tests run: 485, Failures: 0, Errors: 0, Skipped: 11
[INFO] BUILD SUCCESS
```

Log: `.p0-v12-backend.log` · exit code **0**

| Chỉ số | Phiên này | v11 ghi | Delta |
|---|---|---|---|
| Tests run | **485** | 483 | **+2** (2 test F145 thêm ở Phase I v11) |
| Failures / Errors | **0 / 0** | 0 / 0 | — |
| Skipped | **11** | 11 | — |

> Ghi chú phương pháp: `cmd /c "mvnw.cmd test"` từ Git-Bash **không chạy** (cmd không thấy `mvnw.cmd` khi
> không có `.\`); đã dùng `mvn -o test` (Maven 3.9.16, JDK 25.0.3) — cùng hiệu lực. Số đếm lấy từ run log,
> **không** từ `target/surefire-reports/*.xml` (XML cũ của class đã xoá từng làm aggregate ảo +8).

### Frontend — `npx vitest run` (frontend/)

```
 Test Files  22 passed | 1 skipped (23)
      Tests  119 passed | 1 skipped (120)
   Duration  6.82s
```

Log: `.p0-v12-frontend.log` · exit code **0**

| Chỉ số | Phiên này | v11 ghi | Delta |
|---|---|---|---|
| Test files | **22 passed / 1 skipped (23)** | 21 / 1 (22) | **+1 file** |
| Tests | **119 passed / 1 skipped (120)** | 109 / 1 (110) | **+10 test** (8 `webmDuration` + 2 khác ở Phase I v11) |

### Production build — `npx vite build` (frontend/)

```
dist/assets/index-Bhxa7opw.js   177.44 kB │ gzip: 67.56 kB
✓ built in 3.98s
```

Log: `.p0-v12-build.log` · exit code **0**

| Chỉ số | Phiên này | v11 ghi | Delta |
|---|---|---|---|
| Entry `index-*.js` | **177.44 kB** | 177.40 kB | +0.04 kB (trong nhiễu) |
| gzip | **67.56 kB** | 67.53 kB | +0.03 kB |

## 3. Container & service (T0.5)

8 container đang chạy:

| Container | Image | Port | Status |
|---|---|---|---|
| engflow-backend | engflow-backend | 8080 | Up ~1h |
| engflow-frontend | engflow-frontend | 5173 | Up ~3h |
| engflow-sqlserver | mssql/server:2019-latest | 1433 | Up 5h **(healthy)** |
| engflow-redis | redis:alpine | 6379 | Up 5h |
| engflow-minio | minio/minio | 9000-9001 | Up 5h |
| engflow-whisper | engflow-whisper | 9002 | Up 5h |
| engflow-tts | engflow-supertonic | 8001 | Up 5h |
| engflow-tailscale | tailscale/tailscale | — | Up 5h |

Probe sống:

| URL | HTTP | Kết luận |
|---|---|---|
| `POST /api/auth/login` (không body) | 405 | đúng — endpoint tồn tại, chỉ nhận POST có body |
| `:5173/` | 200 | SPA phục vụ |
| `:9000/minio/health/live` | 200 | MinIO sống |
| `:9002/` | 404 | đúng — Whisper chỉ có `/v1/audio/transcriptions` |
| `:8001/` | 404 | đúng — TTS chỉ có `/synthesize` |

**Kiểm backend có stale không:** 0 file `.java` mới hơn thời điểm container start (epoch `1789928754`).
Probe sống xác nhận fix F145 có trong container: `POST /api/ai/save-vocab` trả header `X-AI-Saved-Count`.
⇒ Container **không stale**.

## 4. Parity line (T0.6) — đo trực tiếp

Query chuẩn (thứ tự cột cố định, không đổi nhãn):

```
lessons|exercises|users|vocabulary|speaking|video|lesson_sub|payments|decks|snapshots
1471|43735|72|127|28|15|4|126|14|5
```

**Khớp v11 chính xác** → xác nhận, không phải phát hiện.

## 5. Endpoint inventory (T0.7) — tái dựng từ source

Probe: `sweep/v12/api-inventory.js` → `.specify/specs/audit-v12-full/evidence/endpoint-inventory.json`

```
annotationCount      : 131      <- khớp con số đếm tay
expandedRows         : 147      <- sau khi mở alias mảng ({"a","b"}) và path params
distinctMethodPaths  : 145
controllers          : 26
byVerb               : GET 64 · POST 54 · PUT 16 · DELETE 10 · PATCH 3
```

Kiểm chứng script: 0 path không bắt đầu bằng `/`; `params="q"` không còn bị thu nhầm thành route;
base path cấp class đã resolve (vd `LeaderboardController` → `GET /api/leaderboard`).

### Controller theo số row (dùng để phân bổ probe Phase 2)

| Rows | Controller | v11 có probe? |
|---|---|---|
| 20 | SpeakingPromptController | **KHÔNG** |
| 16 | AdminController | một phần (stats/users/lessons) |
| 15 | VideoLessonController | một phần (3 path admin list) |
| 14 | SpeakingSubmissionController | một phần |
| 11 | LessonStructureController | **KHÔNG** |
| 8 | AuthController | có |
| 8 | DeckController | một phần (CRUD happy path) |
| 7 | LeaderboardController | **KHÔNG** |
| 6 | AdminAiExerciseController | có |
| 6 | GameController | **KHÔNG** |
| 6 | LessonController | có |
| 6 | LessonExerciseController | có |
| 5 | AdminExerciseController | có |
| 4 | VocabularyController | có |
| 3 | AiVocabController | có |
| 3 | LessonSnapshotController | **KHÔNG** |
| 3 | LessonSubmissionController | **KHÔNG** |
| 3 | SrsController | **KHÔNG** |
| 3 | StreakController | có |
| 3 | PaymentController | một phần |
| 2 | AdminAnswerBackfillController | **KHÔNG** |
| 2 | FlashcardController | **KHÔNG** |
| 1 | AdminExerciseSeedController | **KHÔNG** |
| 1 | DashboardController | **KHÔNG** |
| 1 | MediaProxyController | **KHÔNG** |
| 1 | ProgressController | **KHÔNG** |

**12 controller chưa từng được probe** (không phải 10 như ước lượng ban đầu — danh sách chính xác ở trên).

---

## Xác nhận / phát hiện so với v11

| Hạng mục | Kết quả |
|---|---|
| Backend suite | **XÁC NHẬN** (485 = 483 + 2 test F145 của Phase I v11) |
| Parity | **XÁC NHẬN** (khớp byte-for-byte) |
| Container không stale | **XÁC NHẬN** (probe sống) |
| Số endpoint | **XÁC NHẬN** 131 annotation / 26 controller |
| Khoảng trống coverage | **PHÁT HIỆN**: v11 probe ~27/131; **12 controller 0 probe** |
| `cmd /c "mvnw.cmd test"` từ bash | **PHÁT HIỆN (probe bug)**: không chạy được; dùng `mvn -o test` |
