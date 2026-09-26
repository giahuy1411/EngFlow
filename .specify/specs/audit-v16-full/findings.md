# audit-v16-full — findings

Quy ước: `F-16-NN`. Mỗi finding: mô tả · bằng chứng · root cause · fix · bằng chứng pass · test hồi quy ·
**probe thứ 2 độc lập**. Trạng thái: `FIXED` · `OPEN` · `BLOCKED` · `DEFERRED` · `CLOSED (probe SAI)` · `N-A`.

---

## F-16-01 — Harness ghi `study_days` thật mà không dọn (lớp F-15-09/L1) — **MEDIUM** — `FIXED` (đã sửa lại sau review)

**Nguồn:** guard `assertClean` của `ui-sweep` bắt được ở vòng 1 (không phải suy đoán).

**Bằng chứng:**
- `ui-sweep` vòng 1: `assertClean: parity=… study_days=5 pending_payments=0 -> DIRTY: study_days 5 != expected 4` → `EXIT=1`.
- DB: `study_days` id **40076**, `study_date=2026-09-26`, `user_id=2` (`user@gmail.com`).

**Root cause (ĐÃ SỬA LẠI — bản đầu ghi SAI, review đối kháng bắt được):**
Bản đầu nói "login ghi study_days". **SAI**: `UserService.login()` chỉ **ĐỌC** streak
(`getCurrentStreak` → `currentStreak`, `readOnly`). Các writer thật của `study_days` (đều qua
`StudyActivityService.recordStudy()`):
- `ExerciseService.submitExercises` (POST .../exercises/submit) ← **chính luồng MCP "Nộp bài" của tôi ở Phase 4**
- `FlashcardService.recordStudyDay` (POST /api/flashcards/study)
- `SrsService` (POST /api/srs/review)
- `StreakService.checkin` (POST /api/games/submit) ← deep-probe
- `SpeakingSubmissionService` (POST speaking submit)

v15 đã thêm `study_days` vào parity + dọn trong `sweep/v12/api-sweep.js` **nhưng bỏ sót** các harness khác.
`grep recordStudy` = 8 file, **không có** `UserService`.

**Fix (tận gốc, đã sửa theo review):**
- `cleanupStudyDays(from)` trong `sweep/v8/ui/lib.js`: **cửa sổ half-open `[VN_RUN_DATE, today]`** (KHÔNG dùng
  equality 1 ngày — đúng lớp false-pass mà `cleanupAuditPayments` đã sửa), chỉ nhắm 2 tài khoản probe.
- Gọi trong `ui-sweep.js`, `routes.js`, `routes-all.js`, `design.js`, `design-v2.js` — **`design-v2.js` nay có
  `try/finally`** (bản đầu để ngoài `finally` → throw giữa walk sẽ bỏ qua cleanup).
- `ui-sweep.js`: `out.cleanup.ok = clean.ok && cleanSd.ok` **và** exit code dùng `clean.ok` (bản đầu tính
  `cleanSd.ok` rồi **bỏ khỏi exit code** — đúng lớp lỗi v15 L1-c).
- `deep-probe.js`: nay tự dọn `study_days` (nó POST `/api/games/submit` → `checkin` → `recordStudy`) và assert
  `DEEP_SD_REMAINING=0` (inline, vì file này HTTP-only, không require được `lib.js`).

**Probe thứ 2 (độc lập):**
- `deep-probe-rerun.log`: `DEEP_SD_REMAINING=0 DEEP_LESSONS=0 DEEP_DECKS=0 DEEP_EX=0` → **58/0**, EXIT=0.
- `ui-sweep` vòng xác minh: `cleanupStudyDays: window=[…] candidates=0 remaining=0 -> SELF-CLEAN OK` →
  `assertClean … CLEAN` → **EXIT=0**.
- Parity sau cùng: `STUDY_DAYS=4`.

**Bài học (đã ghi AGENTS.md):** harness mới ĐĂNG NHẬP không tự ghi study_days; harness có **hành động** (submit/review/
study/checkin) mới ghi — phải tự dọn + assert.

---

## F-16-02 — Shadow **có blur** trong `.lesson-html details[open]` (vi phạm "No blur") — **LOW** — `FIXED`

**Bằng chứng:** `frontend/src/assets/design-system.css:500` `box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);` — shadow **mờ duy nhất**
trong hệ hard-shadow. Design system ghi rõ: *"No blur. Solid offset colors."*

**Root cause:** shadow này thêm rời rạc, không đi qua token `--geo-shadow-*`.

**Fix:** `box-shadow: 3px 3px 0px 0px var(--geo-accent);` (offset cứng, đúng họ `pop-*`).

**Probe thứ 2:** grep `design-system.css` cho `rgba(0, 0, 0` sau fix → 0 kết quả blur shadow.

---

## F-16-03 — Skeleton dùng màu **ngoài palette** `#EAE4D6` — **LOW** — `FIXED`

**Bằng chứng:** `frontend/src/assets/design-system.css:1026` `linear-gradient(90deg, var(--geo-muted) 25%, #EAE4D6 37%, ...)`
— `#EAE4D6` là cream ấm **không tồn tại** trong bộ token.

**Fix:** đổi sang `var(--geo-border)` (`#E2E8F0` — token thật, cùng họ Slate 200).

**Probe thứ 2:** grep `EAE4D6` toàn frontend → 0.

---

## F-16-04 — Topbar admin shadow bán trong suốt `rgba(139,92,246,0.2)` — **CLOSED (không phải vi phạm)** — `REVERTED`

**Bằng chứng:** `app-layout.css:453`. Review đối kháng: đây là **offset KHÔNG blur** (không vi phạm "No blur");
luật "solid offset colours" thuộc họ pop-* card/button. Đổi sang `var(--geo-accent)` full-opacity tạo **viền đôi ồn**
(bar tím 4px chồng border `#1E293B` 2px). → **Hoàn nguyên** + ghi chú.

---

## F-16-05 — Active nav admin shadow bán trong suốt `rgba(0,0,0,0.25)` — **CONFIRMED-DEFECT (tôi tự gây)** — `REVERTED`

**Bằng chứng:** đổi sang `var(--geo-shadow-xs)` = `2px 2px 0 #1E293B` **VÔ HÌNH** vì sidebar admin
(`app-layout.css:299`) có `background: var(--geo-fg)` = **#1E293B** — shadow cùng màu nền → mất viền nổi.
→ **Hoàn nguyên** về `rgba(0,0,0,0.25)` (tối hơn nền → còn thấy) + ghi chú giải thích.

> **Bài học:** "hard shadow" không thay được shadow bán trong suốt trên nền tối cùng hệ màu — shadow cứng
> `#1E293B` biến mất. Review đối kháng bắt được **trước khi commit**.

---

## F-16-06 — Tiêu đề Flashcard dùng `drop-shadow-sm` (**blur**) — **LOW** — `FIXED`

**Bằng chứng:** `frontend/src/views/luyentu/FlashcardGame.vue:66` `class="... drop-shadow-sm"` — blur shadow.

**Fix:** `[text-shadow:3px_3px_0_var(--geo-border)]` — text-shadow **cứng**, đúng sticker aesthetic.

**Probe thứ 2:** grep `drop-shadow` trong `frontend/src/**/*.vue` → 0.

---

## F-16-07 — README ghi "**26** REST controllers" (thực tế **25**) — **LOW (docs)** — `FIXED`

**Bằng chứng:** `README.md:95` `# 26 REST controllers`. Đếm thật: `grep -rlE "@RestController([^A]|$)" src/main/java` = **25**
(21 file ở `controller/` + 4 ở `controller/payment|speaking|video/`). `GlobalExceptionHandler` là `@RestControllerAdvice`, không tính.

**Fix:** README → **25 REST controllers** (kèm ghi chú 21 ở `controller/` + 4 subpackage).

---

## F-16-08 — CLAUDE.md ghi migration "**V1-V8**" (thực tế **V1–V10**) — **LOW (docs)** — `FIXED`

**Bằng chứng:** `CLAUDE.md:93` "(V1-V8) for reference only". `ls src/main/resources/db/migration/` = **10 file** (V1…V10,
gồm `V10__drop_lesson_builder.sql` thêm ở v15).

**Fix:** CLAUDE.md → **V1–V10**.

---

## F-16-09 — Tài liệu demo có 2 lỗi nội dung SAI (review đối kháng bắt được) — **MEDIUM (docs)** — `FIXED`

**Nguồn:** review chéo đối kháng (subagent) — không phải suy đoán.

**Bằng chứng & fix:**
1. **Streak claim đảo ngược:** bản demo doc viết *"nộp **bài tập** lesson **không** tự động tính streak"*.
   **SAI**: `ExerciseService.submitExercises` (`ExerciseService.java:519-520`) gọi `studyActivityService.recordStudy(user.getId())`
   khi có ít nhất 1 câu trả lời khác rỗng. → **Sửa** thành: nộp bài tập **CÓ** tính streak; liệt kê đủ 5 đường ghi
   `study_days` (exercise/flashcard/srs/game/speaking); nêu rõ **login chỉ đọc**.
2. **Khoá tài khoản sai lần:** viết *"sai 5 lần → **lần thứ 6** báo khoá"*. **SAI**: `UserService.java:208`
   `if (fails >= MAX_LOGIN_FAILS)` với `MAX_LOGIN_FAILS = 5` (`RedisConstants.java:46`) → **lần thứ 5** đã khoá. → **Sửa**.
3. **Citation drift (nhỏ):** `Login.vue:67`→**:71**; `StudyActivityService` `STUDY_ZONE` ở dòng **34** (không phải 138);
   `UserService` hardcode role `"USER"` → thực tế là ternary `"ADMIN"/"USER"`. → **Sửa** + ghi rõ dòng.

**Probe thứ 2:** đọc lại code tại các dòng đã dẫn; `grep` xác nhận `recordStudy` ở `ExerciseService:520`.

**Bài học:** tài liệu demo là thứ hội đồng đối chiếu trực tiếp với code — **mọi khẳng định phải verify lại**, kể cả
bản "đã kiểm chứng 16/09" (bản cũ đã lỗi thời vì v13 đổi streak sang `study_days`).

| ID | Mức | Vấn đề | Trạng thái |
|---|---|---|---|
| F-16-01 | MEDIUM | Harness ghi `study_days` không dọn (sửa lại theo review) | `FIXED` |
| F-16-02 | LOW | Blur shadow trong lesson details | `FIXED` |
| F-16-03 | LOW | Skeleton màu ngoài palette | `FIXED` |
| F-16-04 | — | Topbar shadow bán trong suốt | `CLOSED (không phải vi phạm)` — reverted |
| F-16-05 | LOW* | Active nav shadow → `--geo-shadow-xs` (tôi tự gây: vô hình) | `REVERTED` |
| F-16-06 | LOW | Flashcard title drop-shadow blur | `FIXED` |
| F-16-07 | LOW (docs) | README "26 controllers" | `FIXED` |
| F-16-08 | LOW (docs) | CLAUDE.md "V1-V8" | `FIXED` |
| F-16-09 | MED (docs) | Demo doc: streak claim đảo ngược + khoá sai lần + citation drift | `FIXED` |

**+ 3 defect demo-doc do review bắt được (F-16-09):** streak claim đảo ngược + khoá tài khoản sai lần + citation drift → đã sửa.

**Bác bỏ (CLOSED — probe SAI):** khảo sát ban đầu kết luận `.agents/mcp_config.json` là "secret đã commit" —
đo lại: **không track, đã gitignore, chưa từng commit** (`evidence/secret-scan.md`). Ghi lại để không lặp.

**Bác bỏ #2 (CLOSED — kết luận SAI):** `scripts/figma-export/node_modules` (19MB) trông như "rác đã commit" — nhưng
`git ls-files` = **0 file tracked** (đã gitignore), VÀ `sweep/harness/cls-probe.js:36` **cố ý** resolve
`playwright-core` từ đó. → **KHÔNG phải rác, KHÔNG được xoá.** (Đúng tinh thần V4: kiểm chứng 2 chiều trước khi xoá.)

## Quan sát đã biết (không phải finding mới)

- `?sort=` bị bỏ qua trên lessons/decks/admin-exercises (server cố định thứ tự — F-13-11 từ v13, "chưa đo được tác động người dùng").
  `search-sort.js` xác nhận lại: `sort=title,asc==desc -> true`. **Giữ nguyên** (thiết kế), không phải lỗi mới.
- `admin exercises q=the` LIKE `%kw%` ~163ms — đặc tính (AGENTS.md: "đừng tối ưu bằng cách thêm index").
- `smallTargets=115` — trùng số v14 đã triage 0 REAL.

**Không phát hiện:** 0 finding chức năng/API mới (143/0), 0 orphan, design token/font PASS, perf không win rõ, CLS tốt.
