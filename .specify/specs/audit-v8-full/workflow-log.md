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
| backend `378 tests` | **fresh (Round 2, 2026-09-16)** | `sweep/v8/t-final-backend.log` — `Tests run: 378, Failures: 0, Errors: 0, Skipped: 0 / BUILD SUCCESS` |
| frontend `89 tests / 18 files` | **fresh (Round 2, 2026-09-16)** | `sweep/v8/t-final-frontend.log` — `Test Files 18 passed (18) / Tests 89 passed (89)` |
| `vite build` entry `176.80 kB` (gzip 67.39) | **fresh (Round 2, 2026-09-16)** | `sweep/v8/t-final-build.log` |
| 139 annotation / 97 handler | **superseded** | đếm lại bằng script: **144 unique mapping / 26 controller** (`sweep/v8/endpoint_inventory.py` → `evidence/endpoint-inventory.json`; GET 61 · POST 54 · PUT 16 · DELETE 10 · PATCH 3, 10 bare mapping thừa hưởng prefix của class) |
| DB parity `1471/43737/76/127/28/15/4/126/14/5/38`, 0 orphan | **fresh (Round 2)** | `sweep/v8/p16-parity.sql` + `p16-orphans.sql` — khớp **chính xác** baseline, orphan `0/0/0/0/0` |

> **Lưu ý đếm**: mọi số liệu lấy từ **run log mới**, không đếm file XML trong `target/surefire-reports`
> (XML stale của class đã xóa từng làm aggregate ảo +8 — xem AGENTS.md).

> **Lưu ý đếm lại (đã sửa 1 lần sai)**: bản đầu của log này ghi "149 annotation / 27 controller"
> và "32 route" — cả hai đều **không tái lập được bằng script** và đã bị thay bằng số sinh từ
> `endpoint_inventory.py` (**144/26**) và `route_inventory.py` (**39**). Đây đúng là lớp lỗi
> `CLAIM-COUNT` trong `findings.json`: con số gõ tay không có generator thì không kiểm chứng được.
> Riêng số route còn lệch thêm 1 lần nữa (parser tự thêm catch-all `/:pathMatch(.*)*` trong khi
> `route_inventory.py` đã emit nó) → đã bỏ phần thêm tay và thêm guard `sys.exit(2)` khi trùng path.

---

## 4. Audit scope (chốt)

**Trong phạm vi**:
- Toàn bộ **144 unique mapping** trên **26 controller** (`src/main/java/**/controller/**/*.java`) — kể cả route alias (`video-prompts`/`speaking-prompts`, `video-submissions`/`speaking-submissions`) và route media (`/api/resources/**`, `/api/v1/media/**`). Sinh bằng script, không đếm tay: `evidence/endpoint-inventory.json`.
- Toàn bộ route frontend trong `frontend/src/router/index.js` — **39 route** (37 có `name`, gồm 9 route con admin dưới `/admin` + catch-all). Sinh bằng script: `evidence/route-inventory.json`, đối chiếu bằng `route_verify.py` → `VERDICT: CONSISTENT`.
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

---

## 8. Round 2 — Task 1–16 (2026-09-16)

Bổ sung sau khi chạy đủ chuỗi workflow của `comprehensive-audit-redesign.md`.

### 8.1 Số liệu Round 2 (thay thế mọi số Round 1 ở mục 3)

| Mục | Giá trị Round 2 | Log |
|---|---|---|
| Backend tests | **378/378** `BUILD SUCCESS` | `sweep/v8/t-final-backend.log` |
| Frontend tests | **89/89, 18 files** | `sweep/v8/t-final-frontend.log` |
| Build entry | **176.80 kB / gzip 67.39** | `sweep/v8/t-final-build.log` |
| Endpoint inventory | **144 mapping / 26 controller** | `evidence/endpoint-inventory.json` |
| Route inventory | **39 route** (CONSISTENT) | `evidence/route-inventory.json` |
| Route runtime visits | **228** (38 route × 2 viewport × 3 role; `/admin` chỉ redirect) → 0 lỗi, 0 wrong landing *(Round 2 ghi 152 với 2 role; Round 3 nâng lên 3 role — xem §9)* | `sweep/v8/ui/routes-all.txt` |
| Design font check | **0 / 7 685** element không phải BVP; 15 face | `sweep/v8/ui/design-v2.txt` |
| Design overflow | **0 / 60** tổ hợp route×viewport | `sweep/v8/ui/design-v2.txt` |
| a11y | 0 vi phạm AA (alt/focus/tap-target/reduced-motion) | `sweep/v8/ui/design.txt` |
| Security | **0 failures**, 0 rò rỉ body | `sweep/v8/security.txt` |
| XSS | **PASS** — không thực thi, parity tự phục hồi | `sweep/v8/xss-prove.txt` |
| Performance | N=7 × 15 endpoint, 105/105 `200` | `sweep/v8/perf.txt` |
| Adversarial | **38 + 21 = 59 case**, 0 fail (sau khi sửa 3 assert sai) | `p14-adversarial.txt`, `p15-adversarial-deep.txt` |
| DB parity | `1471/43737/76/127/28/15/4/126/14/5`, orphan 0 | `p16-parity.sql`, `p16-orphans.sql` |

### 8.2 Task 15 (`taskstoissues`) — kết luận: KHÔNG chạy

| Điều kiện | Giá trị | Kết luận |
|---|---|---|
| `.specify/extensions.yml` | không tồn tại | không có hook trước/sau → bỏ qua im lặng |
| GitHub remote | `https://github.com/giahuy1411/EngFlow.git` | **đúng** GitHub → bước này *áp dụng được* |
| Task chưa tick trong `tasks.md` | **0** | không có task để chuyển |
| Finding chưa có status cuối | **0/15** | không có finding hành động được |
| `gh` CLI / GitHub MCP | **không có** trong phiên | không có đường tạo issue |

⇒ Theo acceptance của Task 15: **"not run: no GitHub issue conversion required."**
Không tạo `issues.md` (plan nói chỉ tạo khi thực sự cần chuyển đổi).

### 8.3 Việc dọn dẹp bắt buộc của Round 2

| Rác | Nguồn | Dọn bằng | Kết quả |
|---|---|---|---|
| 3 row `vocabulary` (`XSSPROBE*`, `iframe`) | `xss-prove.js` không tự dọn | `p15-clean-residue.sql` + cleanup trong-run đã sửa | 127 ✅ |
| 6 row `payment_transactions` PENDING | 3 sweep UI mount `/premium` | `p16-clean-payments.sql` (chỉ `status <> 'SUCCESS'`) | 126 ✅ |
| 4 row `vocabulary` `word='img'` | `security.js` §5 (đã gỡ khỏi harness) | `clean-xss-probe.sql` | 0 ✅ |

### 8.4 Acceptance — Round 2

- [x] Baseline đo lại, có log, exit code ghi rõ (§6.8 REPORT).
- [x] Đủ 16 tài liệu theo Required Evidence Layout + `evidence/` + `findings.json`.
- [x] Mọi finding có ID/severity/repro/affected surface/fix-or-disposition/regression check/status.
- [x] Vòng 2 rộng hơn vòng 1 (thêm 59 case adversarial) và không finding nào biến mất thiếu status.
- [x] Parity DB khớp baseline chính xác; rác do vòng này tạo = **0**.
- [x] `git diff --check` sạch; không file `src/` nào untracked.
- [x] 3 biên chưa verify được ghi rõ là biên, không làm tròn thành "pass".

## 9. Round 3 — đóng nốt Task 5 (screenshots) + backup, và một lỗi harness (2026-09-16)

### 9.1 Hai việc còn thiếu của plan

| Việc | Nguồn yêu cầu | Trước | Sau |
|---|---|---|---|
| Backup trước DML | PLAN.md Phase 2 mặc định + Phase 4.4 | điểm khôi phục mới nhất **2026-09-12**, trong khi vòng này đã DELETE | `engflow_2026-09-16-audit-v8-full.bak` — 197,1 MB thô / **35,3 MB nén**, `RESTORE VERIFYONLY` valid, `is_damaged=0`, `has_backup_checksums=1`, FULL; copy ra `C:\Users\ASUS\engflow-backups\` (ngoài volume, ngoài repo) |
| Screenshot flow bắt buộc | PLAN.md Phase 5 | **0 ảnh** trong evidence | `p17_screenshots.js` → **36 ảnh** (30 desktop + 6 mobile), **6,7 MB**, `evidence/screens/` |

**Ghi thẳng về backup**: điểm khôi phục này phủ cho DML **về sau**, không hồi tố 9 row đã xoá ở round 2.
Giá trị thật của nó là lệnh `BACKUP` nay nằm trong chính file SQL chạy lại được, thay vì là một bước
thủ công mà người mệt sẽ bỏ qua.

### 9.2 Lỗi harness: seed chỉ `token` (HARNESS-TOKEN-ONLY-SEED, HIGH)

Lần chạy screenshot đầu báo **36/36 PASS**. Nhưng 9 shot admin đều `text=1524` = đúng độ dài trang chủ.
Kiểm bằng vision ảnh `22-admin-dashboard.png`: là **trang chủ**, không phải dashboard.

Chuỗi nhân quả: `store/modules/auth.js:22` → `isAdmin = user.value?.isAdmin`; `user` khởi tạo từ
`localStorage.user`; `main.js` **không** gọi `fetchUser()` lúc boot ⇒ seed chỉ `token` để lại
`isAdmin === undefined` ⇒ guard `index.js:220` đá `/admin/*` về `/`.

Không chỉ số nào trong harness cũ phân biệt được "render đúng route" với "bị đá sang trang khác":
trang chủ có `mounted=Y, err=0, api4xx=0, ovf=-15`. Field `redirected` cũ chỉ set khi `role === 'anon'`,
nên redirect của role admin bị ẩn hoàn toàn.

**Phạm vi — đo bằng A/B, không suy đoán.** Ban đầu tôi định báo "cả sweep 152 lượt vô hiệu". Đã kiểm lại:

| Probe | Câu hỏi | Kết quả |
|---|---|---|
| `p18_redirect_audit.js` | Có redirect thật không? | 18 case → 14 redirect; 9 route admin → `/` |
| `p18b_admin_after_seed_fix.js` | Seed đủ thì có render không? | **9/9 render thật** (20 lesson row, 20 exercise row, 10 user row) ⇒ lỗi harness, **không** phải lỗi app |
| `p19_why_design_v2_was_fine.js` | Vì sao `design-v2` không lộ? | Case A (admin route đầu tiên) bounce; case C (đúng thứ tự `design-v2`) render đúng ⇒ app tự hydrate `user` |
| `p20_routes_all_old_seed_ab.js` | `routes-all` có bị không? | seed cũ **0/9** bounce, seed mới **0/9** bounce, text **giống hệt** ⇒ **KHÔNG** bị |

⇒ Thiệt hại thật: **9/36 ảnh screenshot** là ảnh trang chủ đội lốt admin (và `p18` chính là probe phát
hiện ra). `routes-all.js` (152/228 lượt) và `design-v2.js` (60 tổ hợp) **không** bị ảnh hưởng.

### 9.3 Sửa và kết quả đo lại

| Thay đổi | Kết quả |
|---|---|
| `loginFull()` + `mapUser()` + `seedAuth()` trong `ui/lib.js` | seed đúng shape `mapUser()` sinh ra |
| `routes-all.js`: 3 role thay vì 2, ghi `finalPath` mọi lượt, assert hợp đồng landing **cả hai chiều** | **228 lượt**, 0 console error, 0 API ≥400, 0 overflow, **0 wrong landing**, **18/18 admin render thật** |
| `design-v2.js`: ghi `wrongPage`, exit 1 nếu lệch | 60 tổ hợp, **0 wrong landing**, exit 0 |
| `p17_screenshots.js`: seed token+user, từ chối chạy nếu `isAdmin` falsy, đánh dấu `landedElsewhere` | **36/36**, 0 landed-on-wrong-page, exit 0 |
| `cleanupAuditPayments()` + `dbParity()` trong `ui/lib.js` | sweep tự dọn: **126 → 126 PARITY OK** |

**Sửa cả assertion sai của chính mình**: probe đầu tiên báo 24 "wrong landing" ở `routes-all` — nhưng
`/login` là `guestOnly` nên user đã đăng nhập **ĐÚNG** khi bị đá sang `/lessons`, và catch-all
`/:pathMatch(.*)*` **ĐÚNG** khi luôn redirect `/`. Bảng hợp đồng guard × role nay mã hoá cả hai chiều
(stay vs bounce) ⇒ 0 false alarm. 5 false alarm tương tự ở `design-v2` cũng đã loại.

### 9.4 Acceptance — Round 3

- [x] Task 5 (screenshot) có 36 ảnh thật, đã verify bằng vision rằng ảnh admin là trang admin.
- [x] Backup restore point mới, `RESTORE VERIFYONLY` valid, copy ngoài volume + ngoài repo.
- [x] Lỗi harness có finding riêng (`HARNESS-TOKEN-ONLY-SEED`), phạm vi **đo** chứ không suy đoán.
- [x] Parity DB khớp baseline chính xác sau mọi sweep (sweep tự dọn trong cùng run).
- [x] `tasks.md` T63–T69 ghi lại phần việc round 3; 69/69 tick, 0 chưa tick.
- [x] `git diff --check` sạch; 0 file BOM; 0 file `src/` bị sửa.

### 9.5 Kiểm chứng cuối — mọi evidence ref phải resolve được

Chạy kiểm tự động: mọi chuỗi trong `evidence[]` của `findings.json` phải là **đường dẫn có thật**.

Lần đầu: **71/72** resolve. 1 ref hỏng — `F93` ghi `sweep/v8/p3a-r1.log (FAIL gốc)`, tức **chú thích
bị nhét vào chính chuỗi path** nên không `open()` được (file thật vẫn tồn tại). Đã tách chú thích ra
`evidenceNotes[path]` ⇒ **72/72 resolve**.

Cùng loại lỗi với `route-verify.txt` từng bị cite mà thiếu ở vòng trước. Đây là lý do tiêu chí
acceptance của Task 16 đòi "một agent mới tái lập được báo cáo chỉ từ lệnh + artifact": một đường dẫn
không mở được làm hỏng cả chuỗi truy vết, kể cả khi kết luận đúng.
