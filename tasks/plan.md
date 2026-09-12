# Implementation Plan — Remediation sau audit-v7 (backup · timezone · backfill · ddl · hygiene)

> Nguồn: `.specify/specs/audit-v7-full/REPORT.md` §4 + doubt-driven cycle 1 (adversarial review đọc code thật, 4 blocker đã verify).
> Ngày: 2026-09-12. Trạng thái: **ĐÃ THỰC THI XONG 2026-09-12** — xem bảng trạng thái dưới đây.

## Trạng thái thực thi (2026-09-12)

| Task | Kết quả | Commit / evidence |
|---|---|---|
| P0.1 gitignore probe artifacts | ✅ `git check-ignore` khớp hết | `6c3d4c4` |
| P0.2 suite xanh trước commit, 2 lô | ✅ 327/0 + 79/79, commit `b424a3d` + `6c3d4c4` | log run |
| P1.1 BACKUP + .env copy ra `C:\Users\ASUS\engflow-backups\` | ✅ .bak 36MB + VERIFYONLY pass | AGENTS.md bullet |
| P1.2 Restore drill thật, count khớp, drop sạch | ✅ PASS 43737/1471/5434 | AGENTS.md bullet |
| P2.1 doc quy ước timezone naive-VN | ✅ | `63dabae` |
| P2.2 false-positive future-date | ✅ bằng chứng dương: grep toàn `src/test` — các `Instant.now/currentTimeMillis` còn lại (media-ticket exp, SEPay HMAC, streak clocks) đều là epoch-millis-the-spec hoặc đã mang `ZoneId VN`; không còn so sánh naive-vs-UTC nào sót → không có diff code là đúng, không phải chưa làm | verify grep |
| P3.1 3 bug TDD + lessonId overload | ✅ RED (stash-test) → GREEN; suite **332/0** | `ad4c9a9`, `7303de9` |
| P3.2 undo tooling | ✅ before-CSV 5.434 + rollback SQL 6 chunks | `sweep/backfill-export.ps1` |
| P3.3 proof lesson 800 + grading 2 chiều | ✅ qua API với đúng payload UI (browser provider unavailable — ghi rõ trong evidence); MC-shape chốt: options=NULL→text-input→text-equality là flow thật; 2 MC bài 800 bị guard chặn đúng thiết kế | `tasks/evidence/backfill-p3-proof.json` |
| P3.4 GATE | ✅ **user chọn B** — `mode=deterministic` | commit `7303de9` |
| P3 batch + 3.5 đóng evidence | ✅ dry-run full (586/0 err) → prebatch .bak → live 586 filled, empty 5434→**4848**; spot-check 10/10 pass qua admin API; diff stray=0 | `05a7c67` + evidence JSON |
| P4.1 validate drill | ✅ **PASS 0 issues** (14.5s boot, 0 ERROR); properties không đổi | `48307fb`/`1f9d151` + `tasks/evidence/p4-validate-drill.log` |
| P5.1 content_original | ✅ **GIỮ vĩnh viễn, đóng issue** | AGENTS.md Boundaries + REPORT §3.1 |
| P5.2 password env doc | ✅ verify `DatabaseSeeder:91-98` rồi mới doc | AGENTS.md Boundaries |

Known-limitation ghi nhận (không sửa dây chuyền, có đường rollback chọn lọc): 40/586 dòng fill trùng đáp án với sibling cùng lesson (12 lesson) — xem `backfill-p3-proof.json` §known_limitation_positional_drift.

## Assumptions (declared, sửa ngay nếu sai)

1. **Deploy = đúng container docker hiện tại trên laptop này.** Không có staging/production; "premium checkout" là demo với SEPAY sandbox. → mọi thứ "chuẩn bị cho deploy thật" (profile prod, env switch, scheduled backup) bị loại là speculative.
2. **`*.bak` chứa PII học viên thật** — giữ trên cùng máy, không push git, không share.
3. **User chấp nhận Ollama chạy nhiều giờ** cho backfill AI (không có phí API). Nếu không: có gate chọn "chỉ deterministic" trong Phase 3.
4. `content_original` giữ vĩnh viễn trừ khi user ra quyết định khác sau Phase 3.
5. Test suite baseline **327 backend / 79 frontend** phải xanh ở mọi checkpoint; mỗi phase = 1 commit.

## Kiến trúc quyết định (đã đổi vì adversarial review)

| Quyết định cũ (turn trước) | Quyết định mới | Lý do đổi |
|---|---|---|
| Batch backfill bằng `limit=500` | **Sửa 3 micro-bug trong `AiAnswerBackfillService` rồi mới batch** (limit cắt mid-lesson + dryRun ghi checkpoint + LazyInit crash) | Reviewer verify bằng code: `:111` subList rồi `:133` checkpoint → lesson tail mất vĩnh viễn; `:133` chạy cả dryRun; `@ManyToOne(LAZY)` + query không JOIN FETCH → endpoint hiện **chưa từng chạy được** (processed=0, errors++) |
| Backup là gate cho DML | **Backup + copy `.env` ra cùng folder + restore drill thật** (`RESTORE DATABASE ... WITH MOVE` vào DB phụ, drop sau) | VERIFYONLY không chứng minh restore chạy được; `.bak` thiếu `.env` thì không dậy được stack |
| Lọc empty-answer khỏi learner fetch | **BỎ** (grader đã `ungradeable:true`, UI đã label) | C6: prefer existing behavior; lọc = quiz ngắn bớt giữa demo, regression mặc trang phục improvement |
| ddl-auto → validate cho deploy jar | **Validate 1 lần dạng read-only diagnostic, time-box 30'**, ghi kết quả vào AGENTS.md, không đổi config | Validate không check nullability/length (premise F41 chỉ đúng 1 nửa); "deploy jar" không tồn tại (assumption 1) |
| Timezone: test helper chung | **Doc convention + sửa đúng chỗ false-positive (streak test dùng epoch-UTC)**; JWT không đụng | `exp` JWT là epoch-millis theo spec — không "sai zone"; chỉ test so sánh naive-vs-UTC là bug |
| Repair bằng ExerciseFixRunner | **loại hẳn** (đã đúng từ đầu) + bổ sung: backfill đọc `lesson.getContent()`, **không đọc `content_original`** (verify `AiAnswerBackfillService.java:155`) | → `content_original` chỉ còn 1 lý do giữ: ràng buộc C2 của user. Không tô hồng bằng lý do circular |

## Dependency graph

```
P0 hygiene ──→ P1 backup+restore-drill ──→ P3 backfill (DML!)
                    │                          │
P2 tz-docs (độc lập)┤                          └──→ P5 decision content_original
                    └──→ P4 ddl-validate (đọc-DB) ──→ P6 docs+commit cuối
```

---

## Phase 0 — Hygiene trước khi commit (XS–S, ~20')

### Task 0.1: Gitignore đồ rác rồi mới commit wave audit
**Description:** `login-admin.json`, `login-user.json`, `e.json`, `check1.txt`, `wrong.json`, `openapi.json` đang untracked và **không** ignored (check rỗng). Thêm pattern vào `.gitignore` (root, mục "local probe artifacts"), **không commit** chúng.
**Acceptance:**
- [ ] `git check-ignore login-admin.json` trả về path đó
- [ ] `git status --porcelain` không còn 6 file probe
**Verify:** 2 lệnh git. **Files:** `.gitignore`. **Scope: XS**

### Task 0.2: Xác nhận test xanh trên working tree hiện tại, commit 2 lô
**Description:** Chạy full backend + frontend suite TRƯỚC commit (P0 gate không được thừa hưởng baseline chưa verify). Commit lô 1: code+test backend (bao gồm `MediaSigner.java`, `AuditV7SecurityWaveTest.java` đang untracked). Lô 2: frontend + docs + `sweep/` + `.specify/specs/audit-v7-full/`.
**Acceptance:**
- [ ] `mvnw.cmd test` → 327/0 pass (log mới)
- [ ] vitest 79 pass; 2 commit sạch Conventional Commits
**Verify:** run log + `git log --stat`. **Scope: S**

**Checkpoint P0:** suite xanh, `git status` sạch, mọi thứ sau có thể rollback bằng git.

---

## Phase 1 — Backup có điểm khôi phục ĐÃ VERIFY (S, ~30' + drill 10')

### Task 1.1: BACKUP + staging folder ngoài volume
**Description:** `docker exec -u root engflow-sqlserver mkdir -p /var/opt/mssql/backup && chown mssql /var/opt/mssql/backup` (bug permission reviewer chỉ ra) → `BACKUP DATABASE english_learning TO DISK='/var/opt/mssql/backup/engflow_2026-09-12.bak' WITH COMPRESSION, CHECKSUM` → `docker cp` file .bak **và bản copy `.env`** vào `C:\Users\ASUS\engflow-backups\` (ngoài volume mssql_data).
**Acceptance:**
- [ ] `BACKUP DATABASE ... processed` thành công, `RESTORE VERIFYONLY` pass
- [ ] `C:\Users\ASUS\engflow-backups\` chứa .bak + .env bản copy
**Verify:** output 2 lệnh sqlcmd. **Files:** không đụng repo. **Scope: S**

### Task 1.2: Restore drill (biến backup thành "verified restore point" — C4)
**Description:** `RESTORE DATABASE english_learning_drill FROM DISK=... WITH MOVE ... TO /var/opt/mssql/data/drill_*.mdf, REPLACE` → `SELECT COUNT(*) FROM english_learning_drill.dbo.exercises` = 43737 → `DROP DATABASE english_learning_drill` + xóa file drill.
**Acceptance:**
- [ ] Drill DB dựng được, row count khớp, drop sạch
- [ ] Ghi 2 dòng vào REPORT/AGENTS: "restore drill 2026-09-12: PASS, file ở <đường dẫn>"
**Verify:** 3 kết quả sqlcmd. **Scope: S** — **đây mới là bằng chứng "khôi phục được", không phải VERIFYONLY**

**Convention:** mọi phiên đụng DML hàng loạt (P3) chạy lại Task 1.1 trước (5'). Không scheduled job — laptop không bật 24/7 (C1).

---

## Phase 2 — Timezone: doc + sửa test (S, ~30', zero-risk)

### Task 2.1: Văn bản hóa quy ước
**Description:** AGENTS.md → Patterns & Gotchas: "Mọi cột datetime2 = naive giờ VN (+07) do backend JVM (`TZ=Asia/Ho_Chi_Minh`) ghi bằng `LocalDateTime.now()`. SQL Server clock (UTC) KHÔNG được dùng ở đâu cả — 0 default constraint. Khi so sánh timestamp trong test/code: dùng cùng naive-VN, không dùng `Instant.now()`/epoch."
**Acceptance:**
- [ ] Mục mới trong AGENTS.md, đúng format hiện có
**Scope: XS**

### Task 2.2: Sửa false-positive "future date"
**Description:** Tìm test/assert so sánh timestamp DB với epoch-UTC (`grep -l "Instant.now\|currentTimeMillis" src/test` → đối chiếu streak/history tests), đổi sang naive-VN clock tại chỗ so sánh. KHÔNG đụng production code, KHÔNG đụng JWT (`exp` epoch-millis là đúng spec).
**Acceptance:**
- [ ] Suite xanh; grep không còn chỗ so sánh naive-vs-UTC trong test liên quan
- [ ] Không đổi file production nào (`git diff --stat` chỉ src/test + AGENTS.md)
**Verify:** `mvnw test` + diff scope. **Scope: S**

---

## Phase 3 — Backfill 5.434 đáp án rỗng (M + 1 gate lớn; chỉ chạy SAU P1)

### Task 3.1: Sửa 3 micro-bug trong `AiAnswerBackfillService` (TDD trước)
**Description:**
1. `ExerciseRepository.findBackfillCandidates`: thêm `JOIN FETCH e.lesson` (hết LazyInit crash — bug khiến endpoint chưa từng chạy được).
2. `run()`: chỉ cập nhật `checkpointLessonId` khi `!dryRun` (`:133`).
3. `run()`: `limit` áp theo **cả lesson-group**, không `subList` giữa lesson (hết hole "tail bị skip vĩnh viễn").
**Acceptance (test trước, code sau):**
- [ ] Test mới `AiAnswerBackfillServiceTest`: seed 2 lesson + exercises có answer-key `<details>` → chạy direct với TransactionTemplate → assert: (a) không LazyInit, (b) dryRun không đổi checkpoint, (c) limit không cắt giữa lesson, (d) filled values đúng answer-key. Test (a)-(c) FAIL trước fix → PASS sau fix (RED→GREEN)
- [ ] Suite 327+N xanh
**Files:** `AiAnswerBackfillService.java`, `ExerciseRepository.java`, test mới. **Scope: M (3-4 files)**

### Task 3.2: Công cụ undo cấp dòng (script, không code app)
**Description:** Thêm `sweep/backfill-export.ps1`: `sqlcmd` export `(exercise_id, correct_answer)` của 5.434 dòng + của các dòng ĐÃ filled sau mỗi batch → CSV vào `C:\Users\ASUS\engflow-backups\`. Undo = 1 file `MERGE`/UPDATE script sinh kèm.
**Acceptance:**
- [ ] Chạy ra CSV đúng 5.434 dòng before; script rollback parse được
**Scope: S**

### Task 3.3: Dò hình-thái đáp án qua submit flow THẬT (chốt nghi vấn #5 của reviewer)
**Description:** Sau khi Task 3.1 xong: chọn **1 lesson** có MC + FILL_BLANK rỗng đáp án, chạy live backfill đúng lesson đó (limit 1 lesson), export diff thấy **giá trị ghi vào là gì**, rồi trên browser: làm bài với đáp án đúng theo key → phải được công nhận ĐÚNG; đáp án sai → SAI. Nếu MC grader nhận *text* mà UI submit *letter/index* → DỪNG, báo gate.
**Acceptance:**
- [ ] 1 bài MC + 1 bài FILL_BLANK filled chấm đúng cả 2 chiều (đúng→pass, sai→fail), có screenshot/DOM evidence
**Verify:** browser + `/api/attempts` response. **Scope: S** — **đây là doubt-step thật của toàn phase, không phải đếm counter**

### Task 3.4 — 🚦 GATE QUYẾT ĐỊNH (user chọn, trước khi chạy hàng loạt)
Sau 3.1→3.3, bạn chọn 1 trong 3:
- **3.4-A (khuyến nghị):** `dryRun=true` full (không giới hạn) để đọc tỷ lệ deterministic/AI/unfillable (AI full ~vài giờ, chạy qua đêm, ngoài giờ demo) → xem `errorDetails` có = 0 không → live full batches (`restart=true` lần đầu). Undo = Task 3.2.
- **3.4-B (an toàn nhất, chậm nhất):** chỉ chạy deterministic — thêm query param `mode=deterministic` (~15 dòng, không gọi Ollama, chạy phút). AI residue để ngỏ = số bài ungradeable giảm một phần.
- **3.4-C:** chỉ fill theo từng lesson khi admin thấy cần, không batch (0 giờ máy, 0 rủi ro, hết sạch tồn kho chậm).
**Verify của mọi nhánh:** `SELECT COUNT(*) WHERE empty` giảm đúng số báo cáo; spot-check 10 bài bằng mắt qua admin UI.

### Task 3.5: Đóng evidence
**Description:** Cuối mỗi batch: persist status JSON ra file (evidence không nằm trong RAM backend). Cập nhật REPORT: số còn rỗng cuối kỳ.
**Scope: XS**

**Checkpoint P3:** count rỗng mới + file undo + spot-check pass; nếu lỗi → rollback bằng undo script, DB gốc còn nguyên nhờ P1.

---

## Phase 4 — ddl-auto validate: diagnostic 1 lần, time-box 30' (S)

### Task 4.1: Boot-drill
**Description:** `mvnw spring-boot:run "-Dspring.jpa.hibernate.ddl-auto=validate"` (hoặc run container tạm với env) → ghi nhận toàn bộ schema-validation errors. Kỳ vọng: 0 hoặc rất ít (DB vốn sinh từ `update`). Time-box: nếu phát hiện >10 mismatch → **dừng, chỉ ghi log vào AGENTS.md**, không sửa dây chuyền (budget C1).
**Acceptance:**
- [ ] Có bảng kết quả trong AGENTS.md: "validate check 2026-09-12: pass / N issues: [...]"
- [ ] `application.properties` KHÔNG đổi (vẫn `update` local)
**Scope: S** — giá trị thật: phát hiện table/column missing mà `update` âm thầm bỏ qua; giá trị ảo (đã bỏ): tin validate bắt được length/nullability.

---

## Phase 5 — Quyết định cuối (sau P3, không trước)

### Task 5.1: `content_original` — giữ hay export-drop?
**Fact mới:** backfill KHÔNG đọc `content_original` (đọc `content` — `:155`); 2 reader còn lại đều flag-off. Lý do giữ duy nhất = C2 + chi phí 69MB trên DB 230MB ≈ 0.
**Khuyến nghị: GIỮ vĩnh viễn, đóng issue.** Nếu bạn vẫn muốn −69MB: export 1 cột `bcp` → file → `ALTER TABLE ... DROP COLUMN` (2 bước, có backup P1). Mặc định không làm.

### Task 5.2: Mật khẩu seed — chốt bằng 1 dòng doc
AGENTS.md Boundaries += "Bất kỳ instance nào chạy ngoài laptop cá nhân: set `DEFAULT_USER_PASSWORD`/`DEFAULT_ADMIN_PASSWORD` trong `.env` trước khi boot." Không code thêm (warn-log F58 đủ cho assumption 1).

---

## Không làm (và lý do) — chống scope creep

- ❌ Migrate timezone toàn DB sang UTC — 1 nguồn ghi duy nhất, chưa có consumer #2; chỉ có nghĩa khi deploy thật
- ❌ Set TZ cho SQL Server container — 0 code nào dùng SQL clock, vô nghĩa
- ❌ `ExerciseFixRunner` (enable/rework) — `deleteAllInBatch()` 43.7k rows, quá rủi ro so với backfill an toàn hơn
- ❌ Lọc empty-answer khỏi learner API — grader + UI đã xử lý; đổi hành vi vì số đẹp không đáng
- ❌ Switch `ddl-auto=validate` permanent cho "deploy profile" — deploy không tồn tại (assumption 1)
- ❌ SQL Agent Job backup lịch đêm — laptop không bật 24/7
- ❌ Bật `SET ANSI_NULLS ON` cho `user_progress` CHECK — `update` đang chạy fine, không có bug (đóng finding §4.5 cũ)
- ❌ Backfill `last_activity_at` — user tự heal sau lần hoạt động đầu
- ❌ `srs_interval` outlier 1914 ngày — 1 dòng, không hại thuật toán

## Risks & mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Deterministic fill sai vị trí → chấm NHẦM thay vì không chấm | Med (learner-visible) | Task 3.3 hai chiều đúng/sai qua flow thật; batch đầu nhỏ + diff undo CSV; sai → rollback dòng |
| Ollama ngốn GPU trong giờ demo | Med | Chạy qua đêm / `mode=deterministic` (3.4-B) |
| Batch giữa chừng mất điện/restart → checkpoint (in-memory) mất | Low | Candidates = still-empty → chạy lại `restart=true` tự skip dòng đã filled; idempotent |
| .bak chứa PII trên host | Low | Folder cá nhân, không git, không share; note vào AGENTS |
| Micro-fix P3 làm lộ bug latent khác của service | Low | Test 3.1 chạy direct, không qua HTTP; gate 3.4 trước mọi live batch |

## Tổng ngân sách ước lượng (C1)

Agent: P0 20' · P1 40' · P2 30' · P3 2–3h công + vài giờ máy (nếu 3.4-A) · P4 30' · P5 15'. **Tổng tay người duyệt: ~30'. Tổng phiên: nửa ngày.**

## Các lựa chọn bạn cần tick khi duyệt plan

1. **Gate 3.4:** A (full + AI, qua đêm) / B (chỉ deterministic) / C (per-lesson manual) — *hoặc delay cả Phase 3*
2. **Restore drill 1.2:** làm (khuyến nghị) / skip (chấp nhận "backup chưa chứng minh khôi phục được")
3. **content_original (5.1):** giữ vĩnh viễn (khuyến nghị) / export-drop sau P3
4. Phạm vi plan: duyệt cả 5 phase / chỉ P0–P2 (zero-risk) rồi tính tiếp
