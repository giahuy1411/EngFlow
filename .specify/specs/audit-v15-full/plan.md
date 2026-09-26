# audit-v15-full — Gỡ Đường B (Lesson Builder) + dọn nợ dữ liệu + verify timezone

**Ngày lập:** 2026-09-25 (+07) · **Nhánh:** tạo mới `audit-v15-full` từ `audit-v14-full` @ `f39950b` (tree sạch)
**Tiền nhiệm:** `audit-v14-full` · **Artifact home:** `.specify/specs/audit-v15-full/`
**Remote:** `github.com/giahuy1411/EngFlow` — **KHÔNG tạo GitHub issue** (giữ quyết định v14)

---

## Context — vì sao có v15

audit-v14 để lại 4 hạng mục OPEN. Người dùng yêu cầu xử lý 3 nhóm, giữ nguyên quy trình v14.

**Phát hiện định hướng (sau nhiều lần kiểm chứng với người dùng):**

Người dùng làm rõ: **nội dung bài học lấy hoàn toàn từ `english-practice.net`** (phần scrape, đang nằm ở
tab "Nội dung"). Còn khối **"Nội dung biên soạn / Tài liệu bổ sung cho bài học này"** — hiển thị **từ đoạn
đó trở xuống** — **KHÔNG phải** nội dung người dùng thêm → phải **loại bỏ hoàn toàn**.

Khối đó chính là **Đường B (Lesson Builder)** — `LessonBlocks.vue` render từ `lesson_sections`/`lesson_blocks`.

**Bằng chứng đo được:**

| Sự thật | Số |
|---|---|
| Section "Nội dung biên soạn" do `LessonBlocks.vue` render (Đường B) | ✅ |
| Lesson đang hiện section này | **7** (446, 447, 567, 10889, 11301, 41881, 91900) |
| **6/7 lesson**: block TEXT là **bản sao y hệt** `lesson.content` (446: `identical=1`, len 5986=5986) | → trùng lặp vô nghĩa |
| Chỉ **lesson 447** do admin tự soạn | 9 block |
| Block toàn hệ | `lesson_sections` 10, `lesson_blocks` 15, `lesson_snapshots` 5 |
| Tab "Bài tập" (`LessonExerciseTab.vue`) có đọc `lesson.content`? | **KHÔNG** → đã tách biệt đúng |
| Tab "Lịch sử" (`LessonPreview.vue`) | = **lịch sử làm bài tập**, KHÔNG phải snapshot → **GIỮ** |

### Bốn quyết định đã chốt (clarify)

| # | Hạng mục | Quyết định |
|---|---|---|
| **Đường B** | Lesson Builder | **GỠ TOÀN BỘ** (UI + API + entity/repo + 2 bảng DB) |
| **4 block cũ** | 3 QUESTION + 1 SUBMISSION ở lesson 447 | **Chuyển 3 câu hỏi → `exercises`**; SUBMISSION xoá; rồi xoá hết block/section |
| **Snapshot** | `lesson_snapshots` | **XOÁ LUÔN** (chỉ dùng trong Builder) |
| **C1/C2** | 67 user rác + 109 payment PENDING | **XOÁ** (backup trước, thứ tự FK, ID liệt kê) |
| **F-13-09** | Timezone | **Chỉ verify + ghi docs** |

### Ground truth (đo phiên này)

| Sự thật | Giá trị |
|---|---|
| Parity hiện tại | `1470\|43735\|72\|118\|29\|15\|4\|126\|10\|5` |
| **Parity sau v15 (tính được)** | **`1470\|43738\|5\|118\|29\|4\|3\|12\|10\|0`** |
| Users | 72 = **67 test/audit** + 5 thật (`user@`2, `admin@`3, `free_live_*`70009, 2 gmail) |
| Admin rác | 4/5 (`test2@test.com`, `admin_43226@test.com`, `auditadmin@test.com`, `apitest@engflow.test`) |
| Row con 67 user rác | exercise_attempts=1, uvp=4, payments=23, video=11, lesson_sub=1, progress=2 |
| Payments | 126 = 109 PENDING + 17 SUCCESS; PENDING ∪ stale-owner = **114** xoá → **12** còn |
| 3 câu hỏi cần chuyển | block 4 MC(`goes`), 5 FILL(`play`), 7 TRUE_FALSE(`True`) — đều có đáp án + giải thích |
| FK tới `lesson_sections`/`lesson_blocks` | chỉ `lesson_blocks→lesson_sections` (nội bộ Đường B) → DROP an toàn |
| **`LessonService.deleteLesson` cascade** tới sections/blocks/snapshots | **PHẢI sửa** khi gỡ (nếu không, xoá lesson nổ FK) |

---

## Nguyên tắc bất di bất dịch (P1–P8 + AGENTS.md)

P1 baseline từ run log · P2 phân lớp · P3 schema Hibernate `ddl-auto=update`, đổi schema = SQL + entity +
verify · P4 AI local · P5 đo trước/sau · P6 font Be Vietnam Pro · P7 UI tiếng Việt + WCAG 2.2 AA ·
P8 bằng chứng runtime.

**Bẫy AGENTS.md:** flush `rate_limit:*` mỗi batch; `SET QUOTED_IDENTIFIER ON` mọi batch DELETE + quét
`Msg \d+`; seed cả token+user + assert `page.url()`; contrast composite bottom-up; `alt=""` hợp lệ;
tap-target AA=24px; `/premium/checkout` tạo row thật → dọn cùng run; timezone naive-VN;
**backup trước DML hàng loạt; xoá theo ID liệt kê**.

**Ràng buộc phiên:** V1 dọn rác + liệt kê file đã xoá · V2 phạm vi = 3 nhóm đã chốt · V3 không GitHub
issue · V4 kết luận cũ là giả thuyết · V5 probe thứ 2 mỗi finding · **V6 backup + restore-drill trước DML;
parity phải khớp `1470|43738|5|118|29|4|3|12|10|0`**.

---

## Phases & Tasks

Artifact dưới `.specify/specs/audit-v15-full/evidence/`. `[gate]` = phase sau chặn tới khi đạt.

### Phase 0 — Freeze, baseline, harness `[gate]`
| # | Việc | Artifact | Tiêu chí |
|---|---|---|---|
| T0.1 | Nhánh `audit-v15-full` | `baseline.md` | tree sạch |
| T0.2 | Artifact home + 7 pipeline docs | thư mục | đủ |
| T0.3 | Khôi phục harness → `sweep/v15/` (14 file `a627569` + 6 script v14 dựng lại) | `harness-restore.md` | `assert-namespace.js` PASS |
| T0.4 | Backend suite | `baseline-backend.log` | 525/0/0/11 |
| T0.5 | Frontend suite + build (`npm install` trước) | `baseline-frontend.log`, `baseline-build.log` | 194/1 (31); 177.98 kB |
| T0.6 | Container + parity + inventory | `containers.md`, `parity-before.txt` | 8 up; `…72…126…` |
| T0.7 | Falsify-first ledger | `prior-hypotheses.md` | mỗi hạng mục có cách test lại |

### Phase 1 — Cleanup C1 + C2 (DML hàng loạt) `[gate]`
| # | Việc | Artifact | Tiêu chí |
|---|---|---|---|
| T1.1 | **Backup mới + `RESTORE VERIFYONLY`** | `cleanup-backup.md` | verify valid |
| T1.2 | Liệt kê **67 user_id** | `stale-user-ids.txt` | đúng 67 |
| T1.3 | Liệt kê **114 payment id** (PENDING ∪ stale-owner) | `payment-ids.txt` | đúng 114; giữ 12 SUCCESS |
| T1.4 | **Dry-run + ROLLBACK** (đếm, không commit) | `cleanup-c1c2.md` | số khớp |
| T1.5 | **Xoá thật** con→cha, ID liệt kê, `QUOTED_IDENTIFIER ON` + `XACT_ABORT ON` + `BEGIN TRAN` | nt | 0 `Msg` |
| T1.6 | Verify parity sau cleanup | `parity-after-cleanup.txt` | `1470\|43735\|5\|118\|29\|4\|3\|12\|10\|5` |
| T1.7 | Probe 2: 5 user thật + `admin@` login 200; leaderboard 200 | `cleanup-c1c2.md` | pass |

### Phase 2 — Chuyển 3 câu hỏi sang `exercises` (migrate, có backup) `[gate]`
| # | Việc | Artifact | Tiêu chí |
|---|---|---|---|
| T2.1 | Ánh xạ: block 4→`MULTIPLE_CHOICE` (options giữ nguyên); block 5→`FILL_BLANK`; block 7→`MULTIPLE_CHOICE` options `["True","False"]` (vì `ExerciseType` không có `TRUE_FALSE`) | `migration-map.md` | mapping rõ |
| T2.2 | Insert 3 hàng vào `exercises` (lesson 447, `question`=questionText, `correct_answer`, `options`, `explanation`, `order_index` sau exercise cuối) | `_migrate-blocks.sql` | 3 hàng |
| T2.3 | Verify: exercises lesson 447 = 5+3 = **8**; parity exercises 43735→**43738** | `migration-verify.md` | khớp |
| T2.4 | Verify UI: 3 câu hỏi làm được ở tab "Bài tập" (không lộ đáp án trước khi trả lời) | `e2e-migrate.md` | pass |

### Phase 3 — Gỡ Đường B khỏi CODE (backend + frontend) `[gate]`
| # | Việc | File | Tiêu chí |
|---|---|---|---|
| T3.1 | **Sửa `deleteLesson`**: bỏ cascade tới sections/blocks/snapshots (tránh FK khi xoá lesson) | `service/LessonService.java` | test pass |
| T3.2 | Xoá controller + service + 2 entity + 2 repo + snapshot (entity/repo/service/controller) | `LessonStructureController`, `LessonStructureService`, `LessonBlock`, `LessonSection`, `LessonBlockRepository`, `LessonSectionRepository`, `LessonSnapshot*` | xoá file |
| T3.3 | Xoá enum `BlockType` + `QuestionType` (dead) | `model/enums/` | xoá file |
| T3.4 | Xoá `AdminLessonBuilder.vue` + `LessonBlocks.vue` + `lessonStructureService.js` + route `:id/build` + nút "Xây dựng" ở `AdminLessons.vue` + breadcrumb ở `AdminLayout.vue` | frontend | không còn |
| T3.5 | `LessonLayout.vue`: bỏ `<LessonBlocks />` (tab "Nội dung" chỉ còn `LessonContent`) | `views/lessons/LessonLayout.vue` | — |
| T3.6 | Sửa chuỗi stale ở `LessonPreview.vue`: *"Làm bài tập ở tab Nội dung"* → *"tab Bài tập"* | `views/lessons/LessonPreview.vue` | đúng |
| T3.7 | Cập nhật/xoá test liên quan (`LessonStructureBlockTypeGuardTest`, `AuditV10DraftLessonStructureGuardTest`, `AuditV7SecurityWaveTest` phần F56, `LessonBlocks.test.js`, `AdminLessonBuilder.f13.test.js`, `LessonServicePaginationTest.deleteLesson`) | tests | suite xanh |
| T3.8 | **DROP bảng** `lesson_blocks` → `lesson_sections` → `lesson_snapshots` (sau khi code đã gỡ) | `_drop-route-b.sql` | 0 `Msg` |

**Gate:** backend + frontend suite xanh; `/lessons/:id` chỉ còn nội dung scrape; xoá lesson admin vẫn chạy.

### Phase 4 — Verify trang bài học sau gỡ `[gate]`
| # | Việc | Artifact | Tiêu chí |
|---|---|---|---|
| T4.1 | Live `/lessons/447`: tab "Nội dung" **không** còn "Nội dung biên soạn" | `post-removal-live.json` | 0 section |
| T4.2 | Tab "Bài tập": 8 câu (5+3) làm được | nt | 8 |
| T4.3 | Tab "Lịch sử" vẫn hoạt động (attempt history) | nt | 200 |
| T4.4 | Admin: không còn nút "Xây dựng"; `/admin/447/build` → catch-all | nt | 404/redirect |
| T4.5 | Rò rỉ đáp án: anon `GET /api/lessons/447/structure` → **404** (endpoint đã xoá) | `f15-01-verify.json` | 404 |

### Phase 5 — F-13-09 timezone verify + docs
| # | Việc | Artifact | Tiêu chí |
|---|---|---|---|
| T5.1 | Đo 3 đồng hồ + đếm writer (11 `LocalDateTime.now()`; **Clock bean CÓ** ở `EngflowApplication.java` nhưng bị bypass) | `tz-audit.md` | số thật |
| T5.2 | Kiểm date-granular miễn nhiễm (`study_days`, streak) | nt | khớp |
| T5.3 | Ghi convention + lý do không migrate vào AGENTS.md | docs | cập nhật |

### Phase 6 — Regression + review chéo
| # | Việc | Tiêu chí |
|---|---|---|
| T6.1 | Backend suite (đếm từ log) | ghi số |
| T6.2 | Frontend suite + build | ghi số |
| T6.3 | Review chéo đối kháng mọi fix AI sinh | `review-v15.md` |

### Phase 7 — Vòng 2 (loop-until-dry)
| # | Việc | Tiêu chí |
|---|---|---|
| T7.1 | Case biên trên build cuối | `round-2.md` |
| T7.2 | Chạy lại mọi probe | nt |
| T7.3 | Dừng khi 2 vòng liên tiếp 0 finding mới | nt |

### Phase 8 — Converge / Analyze / Report / Cleanup
| # | Việc | Artifact |
|---|---|---|
| T8.1–T8.3 | converge · analyze · secret-scan | `converge.md`, `analyze.md`, `secret-scan.md` |
| T8.4 | `REPORT.md` (đã làm/chưa làm/fix/skill/**file đã xoá**/giới hạn) | `REPORT.md` |
| T8.5 | Cleanup manifest + thực thi | `cleanup-manifest.md` |

---

## Cleanup protocol (V1)
| Nhóm | Đường dẫn | Xử lý |
|---|---|---|
| Pipeline docs + evidence | `.specify/specs/audit-v15-full/**` | **GIỮ** |
| Harness | `sweep/v15/**` | **XOÁ cuối + liệt kê** |
| Scratch | `tmp/**` (không ignore) | **XOÁ cả thư mục** |
| Build output | `target/`, `frontend/dist/` | **XOÁ** |
| Backup mới | `C:\Users\ASUS\engflow-backups\engflow_2026-09-25-*.bak` | **GIỮ ngoài repo** |
| KHÔNG đụng | `uploads/**`, spec v8–v14, `.env*`, backup cũ | — |

---

## Ma trận kịch bản kiểm thử (~50 case)
| Nhóm | Case | Kỳ vọng |
|---|---|---|
| **C1** | 67 user rác biến mất; 5 thật còn; 4 admin rác biến mất; `admin@` login 200 | parity `…5…` |
| **C2** | 114 payment xoá; 12 SUCCESS còn; 0 orphan | đúng |
| **Migrate** | 3 câu hỏi → exercises; lesson 447 = 8 exercises; làm được ở tab "Bài tập" | 8 |
| **Gỡ B** | `/lessons/:id` tab "Nội dung" 0 "Nội dung biên soạn"; `GET /structure` → 404 | pass |
| | Admin không còn nút "Xây dựng"; `/admin/:id/build` → catch-all | pass |
| | `deleteLesson` vẫn chạy (không FK) | pass |
| | 3 bảng đã DROP | 0 |
| | Tab "Lịch sử" (attempt) vẫn 200 | 200 |
| **TZ** | 3 đồng hồ đo lại; date-granular miễn nhiễm | ghi số |
| **ADV** | XSS trong nội dung scrape → sanitize | không raw |
| | draft lesson → 404 cho student | pass |
| | pagination/size biên | cap |

---

## Rủi ro & giảm thiểu
| # | Rủi ro | Mức | Giảm thiểu |
|---|---|---|---|
| R1 | Xoá user kéo theo dữ liệu thật | High/High | Backup + restore-drill; ID liệt kê; dry-run+ROLLBACK; kiểm 5 user + 12 SUCCESS |
| R2 | FK NO_ACTION → xoá sai thứ tự (Msg 547) | Med/High | Xoá con trước cha; quét `Msg \d+` |
| R3 | **DROP bảng khi code còn tham chiếu → app 500** | **High/High** | **Gỡ code TRƯỚC, DROP bảng SAU** (Phase 3.2–3.7 trước 3.8); verify |
| R4 | `deleteLesson` cascade tới bảng đã xoá → lỗi | High/High | Sửa `LessonService.deleteLesson` (T3.1) **trước** DROP; test |
| R5 | Chuyển TRUE_FALSE sai mapping | Med/Med | `ExerciseType` không có TRUE_FALSE → MC `["True","False"]`; verify UI |
| R6 | Đổi số exercises/snapshots làm parity "trôi" | Med/Med | Ghi **baseline mới có chủ ý** `…43738…0`; không coi là drift |
| R7 | Gỡ quá tay (tab Lịch sử, LessonPreview) | Med/High | `LessonPreview` = attempt history → **GIỮ**; chỉ gỡ snapshot |
| R8 | Test cũ khẳng định hành vi Đường B | Med/Med | Cập nhật/xoá test T3.7; chạy suite |
| R9 | Backup chứa PII | Low/High | Giữ ngoài repo; không commit |

---

## Definition of Done
- Parity = **`1470|43738|5|118|29|4|3|12|10|0`** (exercises +3 migrate; snapshots −5 do gỡ Builder — ghi rõ).
- 5 user thật + `admin@gmail.com` nguyên vẹn; 12 payment SUCCESS còn.
- Trang bài học: tab "Nội dung" **chỉ** nội dung scrape; **0** section "Nội dung biên soạn".
- 3 câu hỏi cũ làm được ở tab "Bài tập"; tab "Lịch sử" vẫn chạy.
- `lesson_sections`, `lesson_blocks`, `lesson_snapshots` đã DROP; code Đường B đã xoá.
- Backend + frontend suite xanh, đếm từ run log; 0 regression; `deleteLesson` vẫn chạy.
- Mọi finding có probe thứ 2; review chéo mọi fix.
- Cleanup manifest đầy đủ; `git status` sạch; REPORT liệt kê file đã xoá.

---

## Kiểm chứng end-to-end (cách chạy lại)
```bash
# Baseline
& .\mvnw.cmd -o test
cd frontend && npm install && npx vitest run && npx vite build
# Cleanup (backup trước!)
python sweep/v8/sqlrun.py sweep/v15/t1-cleanup-c1c2.sql
python sweep/v8/sqlrun.py sweep/v8/p16-parity.sql     # 1470|43735|5|118|29|4|3|12|10|5
# Migrate 3 câu hỏi, rồi gỡ code, rồi DROP bảng
python sweep/v8/sqlrun.py sweep/v15/t2-migrate-blocks.sql
python sweep/v8/sqlrun.py sweep/v15/t3-drop-route-b.sql
python sweep/v8/sqlrun.py sweep/v8/p16-parity.sql     # 1470|43738|5|118|29|4|3|12|10|0
# Verify live
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/lessons/447/structure   # 404
NODE_PATH=%APPDATA%\npm\node_modules node sweep/v15/ui-sweep.js
# Rebuild container sau khi sửa backend
docker compose up -d --build backend
```

---

## Skill / plugin dự kiến nạp
`speckit-*` · `superpowers:{using-superpowers, systematic-debugging, test-driven-development,
verification-before-completion, requesting/receiving-code-review}` · chrome-devtools MCP · Playwright MCP ·
`accessibility` · `java-springboot` · `java-coding-standards` · `generate-tests` · `frontend-design`.
