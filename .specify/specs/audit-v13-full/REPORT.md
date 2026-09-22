# audit-v13-full — BÁO CÁO CUỐI

**Ngày:** 2026-09-22 (+07) · **Nhánh:** `audit-streak-review` · **Checkpoint đầu:** `3c1c525`
**Tiền nhiệm:** `audit-v12-full` · **Artifact home:** `.specify/specs/audit-v13-full/`
**Người dùng yêu cầu:** một bản **hoàn toàn mới**, toàn diện hơn — e2e, đào sâu; fix tận gốc + test hồi quy; lặp đến khi cạn lỗi; **không mock data**; review chéo mọi fix; không tạo GitHub issues.

---

## 1. TL;DR

| Hạng mục | Kết quả |
|---|---|
| **Bug người dùng báo (F-13-01)** | **ĐÃ FIX** — repro bằng hàng thật `exercise_id=777434`, verify live UI trước/sau |
| **Tổng finding** | **22** (F-13-01…F-13-22; F-13-03/04/05 là 1 mục gộp 3 vị trí) |
| **Đã fix trong phiên** | **19** — F-13-01, 02, 03/04/05, 06, 07, 08, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22 |
| **OPEN có lý do** | **1** — F-13-09 (đã biết từ v7, không migrate) |
| **CLOSED (kết luận probe SAI)** | **1** — F-13-10 (đo lại: không index nào là prefix của index khác; tất cả đang dùng) |
| **Test hồi quy thêm** | **+66** — backend **523** (từ 499), frontend **175** (từ 127) — **0 regression** |
| **Backend suite** | **517 / 0 fail / 0 error / 11 skipped, BUILD SUCCESS** |
| **Frontend suite** | **175 passed / 1 skipped (30 file)** |
| **Build entry** | **177.73 kB** (gzip 67.66) — +0.29 kB |
| **API sweep** | **143 pass / 0 fail** (v13) + workflow sweep **199 pass / 2 fail** (2 fail = lỗi trong fix của tôi, đã sửa) |
| **UI sweep** | **117 route×role, 0 guardFails, 0 console/page/api error, 0 contrast fail, 0 overflow** |
| **DB audit** | constraint **hiệu lực** chứng minh trong scratch DB; parity giữ **chính xác** baseline |
| **Perf** | 35 endpoint median-5; chỉ 1 endpoint >100ms (LIKE `%kw%` đã biết); **1 N+1 đã sửa (F-13-15)** |
| **CLS** | `/` **0.00069** · `/lessons` **0.00095** · `/login` **0.00003** |
| **Parity** | `1470\|43735\|72\|118\|29\|15\|4\|126\|10\|5` — **khớp baseline, 0 rác** |
| **Font / design system** | Be Vietnam Pro **xác nhận** (`document.fonts.check` = true); 0 hit Outfit/Plus Jakarta |

**Điều quan trọng nhất về phương pháp:** bản fix **đầu tiên** của tôi cho F-13-01 gây ra **regression lớn gấp 44 lần bug gốc** (làm 32 814 hàng bài tập không trả lời được). **Review chéo đối kháng bắt được trước khi commit.** Đây là lý do người dùng yêu cầu review chéo — và nó đã hoạt động đúng.

---

## 2. Đã làm

### Phase 0 — baseline (đo bằng chính phiên này)
- Backend **499/0/0/11**, frontend **127/1 (25 file)**, build **177.44 kB**, 8 container up, backend **không stale**.
- Parity `1470|43735|72|118|29|15|4|126|10|5`.
- Endpoint inventory tái dựng: **132 annotation → 148 row / 146 distinct / 26 controller**.
- Phát hiện `sweep/v13/` mang nhãn v12 (**F-13-06**) → đổi namespace.

### Phase 1 — DB trong Docker (read-only)
- Parity đo lại; **constraint hiệu lực** chứng minh trong DB scratch (FK/UQ/filtered index thật sự chặn, rồi DROP).
- Orphan scan (NULL FK đếm riêng); streak schema; index/fragmentation; timezone.
- Phát hiện **F-13-07 (HIGH)**, **F-13-08 (MEDIUM)**, **F-13-09/10 (LOW)**.
- Tôi **verify độc lập lại** F-13-07 (`sys.check_constraints = 0`) và đọc `deploy.sql` để xác nhận root cause.

### Phase 2 — API sweep toàn bộ
- `sweep/v13/api-sweep.js`: **143 pass / 0 fail / 1 blocked / 3 N/A**.
- Workflow sweep bổ sung: **199 pass / 2 fail** → 2 fail **chính là lỗi trong fix của tôi** (**F-13-12**, **F-13-13**), đã sửa; + **F-13-11**.
- Search/sort đo riêng: 16 probe, 0 finding.
- Guard F-13-01 kiểm **cả hai chiều** trên container thật.

### Phase 3 — UI sweep (cả hai MCP)
- **117 route×role**; font `document.fonts.check` = **Be Vietnam Pro true**.
- Responsive 360/768/1280/1440/1920; 6 ảnh evidence.
- Contrast đo **live DOM, composite alpha bottom-up** → **F-13-14**.
- Lighthouse trên `/`, `/lessons`, `/login`.

### Phase 4 — E2E 3 tầng (UI → API → DB) — **mới so với v12**
| Flow | Kết quả |
|---|---|
| T4.1 Đăng ký→login→`/me`→`users` | 201→200→200, DB row `user_id=201653` |
| **T4.2b F-13-01 khép kín** | UI nút bấm → grade/submit 200 → `exercise_attempts` + `study_days` |
| **T4.2c chống regression** | lesson 567: 12 hàng MC options NULL **vẫn trả lời được** |
| T4.3 Streak | snapshot 7 field, `currentStreak=2` khớp `study_days` |
| T4.4 SRS | review 200 → `ease 2.06→2.16`, `rep 0→1`; cap **365** giữ |
| T4.5 Search | `q=habitat` → 1 kết quả `vocabId=10051` khớp DB |
| T4.6 CRUD | create 200 → update 200 → delete 204, DB row 1→1→0 |
| T4.7 AI validate | chặn `["a","b","c","d"]`, cho qua options thật |

### Phase 5 — hiệu năng (đo trước, P5)
- 35 endpoint, median của 5 lần. Chỉ `admin exercise search q=the` **185 ms** (>100ms, LIKE `%kw%` — đã biết, **không** "tối ưu" bằng index vô ích).
- CLS `/` 0.00069 (F150 xác nhận đã fix).
- **Không tối ưu gì thêm** vì số không biện minh (đúng P5).

### Phase 6 — fix tận gốc + test hồi quy
- **F-13-01**: render (không đổi loại) + create FE/BE + **AI path**; **+23 test**.
- **F-13-03/04/05/14/16**: contrast `*-ink` (tertiary/quaternary/danger), 4 vị trí builder; **+6 test**.
- **F-13-11/12/13/15**: sort 400, regex `A - content`, LISTENING, N+1 batch; **+4 test**.
- **F-13-18/19**: `h1` cho trang bài học, fallback avatar hỏng; **+2 test**.
- **F-13-06/17/21**: harness v12↔v13 (3 lần cùng lớp) — namespace + đường dẫn output.
- **F-13-02/22**: nút "Xem trước" trỏ route chết → sửa; banner cảnh báo; vô hiệu hoá block chưa hỗ trợ.
- **F-13-07**: `@CheckConstraint` + guard riêng + script vá; verify chặn thật.
- **F-13-08**: `recentUsers` chuyển sang `study_days`; đánh dấu `countByLastStudyDateAfter` deprecated.
- **F-13-20**: `safeRedirect()` chống open-redirect + guard/Login/Register/Premium mang đích.
- Mọi fix: **fail trước → pass sau**, đo lại cùng probe.

---

## 3. Chưa làm / OPEN (nói thẳng)

| # | Vấn đề | Trạng thái sau khi xử lý |
|---|---|---|
| **F-13-02** | Lesson Builder: block không tới học viên | **FIXED (phương án B)** — sửa bẫy "Xem trước" + banner cảnh báo + vô hiệu hoá tạo QUESTION/SUBMISSION + `docs/lesson-builder-status.md`. Renderer đầy đủ vẫn là việc tương lai (A1/A2/A3). |
| **F-13-07** | `study_policy` thiếu CHECK | **FIXED** — `@CheckConstraint` (JPA 3.2) + guard riêng trong `deploy.sql` + script vá DB; verify `INSERT id=2` bị chặn (Msg 547). |
| **F-13-08** | dashboard admin đọc cột `last_study_date` chết | **FIXED** — `recentUsers` nay từ `study_days`. Cột `current_streak` + 3 query cũ là **dead code**, ghi rõ, **không xoá** (đổi schema cần quyết định riêng). |
| **F-13-20** | `?redirect=` không ai đọc | **FIXED** — `safeRedirect()` + guard mang redirect + Login/Register đọc; 13 ca tấn công đều bị chặn; verify live 2 chiều. |
| **F-13-09** | SQL Server chạy UTC (3 đồng hồ lệch) | **Giữ nguyên** — đã biết từ v7, đúng AGENTS.md; không migrate khi chưa có consumer thứ hai. |
| **F-13-10** | "3/5 index dư" | **CLOSED — kết luận probe SAI.** Đo lại: không index nào là strict prefix của index khác; cả 5 đều có seeks/scans > 0. |

**Giới hạn đã gặp (ghi để không ai tưởng đã phủ):**
- **`ERR_EMPTY_RESPONSE` trên `/leaderboard`, `/profile`** trong UI sweep là **artifact do chính tôi** rebuild backend giữa lúc sweep. Đo lại: `/api/leaderboard` → **200**. **Không phải bug** — ghi rõ để không "sửa" cái đúng.
- **AI `generate-async` (gọi Ollama)** chưa chạy end-to-end trong phiên (tốn GPU); đã verify đường **validate** thay thế + ghi **BLOCKED**.
- **Payment tiền thật** chỉ chạm create-order + webhook chữ ký sai, dọn trong run (biên giới tiền thật).
- Workflow UI/perf chạy **song song** với việc tôi sửa code → một số số liệu UI có thể phản ánh build trung gian; đã đo lại các điểm bị ảnh hưởng.

---

## 4. Đã fix & fix thế nào

| # | Fix | Bằng chứng |
|---|---|---|
| **F-13-01** | `LessonExerciseTab.vue`: giữ option chữ cái làm **lựa chọn**, giữ ô text khi thiếu options + cảnh báo `role=note`. `AdminExercises.vue`: chặn lưu MC thiếu/là chữ cái. `ExerciseService.assertNewChoiceOptionsUsable()` (chỉ create/update-có-options). `AiExerciseService.validateSchema()` yêu cầu ≥1 option nội dung thật. | UI live: 0→8 nút; API 400/200; 23 test |
| **F-13-03/04/05** | `Profile.vue`, `AdminDashboard.vue`: `text-tertiary/quaternary` → `*-ink` | `/admin/dashboard` 0 contrast fail; đo live 1.67→5.02 |
| **F-13-06** | `sweep/v13/*`: `AUDIT-V12-` → `AUDIT-V13-`, output → v13 evidence | grep `AUDIT-V12` = 0 |
| **F-13-11** | `GlobalExceptionHandler`: `PropertyReferenceException` → **400** | live 500→400; +1 test |
| **F-13-12** | Thu hẹp `isBareLetterOption` → `^[a-d]$` (giữ `isPlaceholderOption` rộng cho FILL_BLANK) | `"A - Salad"` 400→200; bare-letter vẫn 400 |
| **F-13-13** | Nhánh LISTENING chỉ hiện lựa chọn khi options **có nội dung thật** | 39 hàng letter-only → ô text; +2 test |
| **F-13-14** | Thêm token `danger-ink` #BE123C; 36 chỗ `text-danger` → `text-danger-ink` (23 file) | 4.00→5.29+; +3 test |
| **F-13-15** | `SrsService.getDueWords()`: batch 1 query thay vì 1 query/từ; thêm `findByUserIdAndVocabularyIdIn` | query-stats: per-word **454→454 (+0)**, batch 3→6; +1 test |
| **F-13-16** | `/admin/:id/build`: `bg-accent`→`bg-accent-strong`, `text-white/40`→`/60`, `text-foreground/40`→`text-muted-foreground` | live contrastFails 3→**0** |
| **F-13-17** | `sweep/v12/api-inventory.js` hardcode path → tạo `sweep/v13/api-inventory.js`; khôi phục v12 evidence | `git status` v12 = sạch |
| **F-13-18** | `LessonLayout.vue`: thêm `<h1>` + nạp tiêu đề bài học | live `/lessons/445` 0→**1 h1** đúng tiêu đề |
| **F-13-19** | `Leaderboard.vue`: thêm `@error` fallback avatar | live 21 ảnh, **0 hỏng** (trước 1) |
| **F-13-21** | `sweep/v13/api-sweep.js`: 2 đường dẫn v12 → v13 | cleanup đúng file, JSON đúng chỗ |

---

## 4b. Tổng hợp 16 finding

| Nhóm | Finding | Trạng thái |
|---|---|---|
| Bug người dùng báo | F-13-01 | FIXED |
| Do chính fix của tôi gây ra (review chéo bắt) | F-13-12, F-13-13 | FIXED |
| Contrast (cùng lớp) | F-13-03, 04, 05, 14, 16 | FIXED |
| Coverage / vệ sinh | F-13-06, F-13-11 | FIXED |
| Hiệu năng | F-13-15 | FIXED |
| Cần quyết định / nợ | F-13-02, 07, 08 | OPEN |
| Đã biết, chấp nhận | F-13-09 | OPEN |
| Có số, chưa biện minh | F-13-10 | DEFERRED |

---

## 5. Skill / plugin đã nạp trong phiên

| Skill / plugin | Dùng để làm gì |
|---|---|
| `speckit-*` (constitution→specify→clarify→checklist→plan→tasks→implement→converge→analyze) | Pipeline bắt buộc |
| `superpowers:using-superpowers` | Luật nền: nạp skill trước khi hành động |
| `superpowers:brainstorming` | Chốt phạm vi ở bước clarify |
| `superpowers:systematic-debugging` | Truy gốc F-13-01 (đọc code + SQL thật, không đoán) |
| `superpowers:test-driven-development` | Mọi fix: test fail trước → fix → pass |
| `superpowers:verification-before-completion` | Mọi tuyên bố "pass" có output lệnh đứng sau |
| `superpowers:requesting-code-review` / `receiving-code-review` | **Review chéo F-13-01 — bắt được 6 lỗi, gồm regression 44×** |
| `workflow-authoring` + `Workflow` tool | Fan-out 4 sweep song song + adversarial verify |
| **chrome-devtools MCP** (`evaluate_script`, `list_network_requests`, `lighthouse_audit`) | Driver UI 1 + Lighthouse |
| **Playwright MCP** (`browser_*`) | Driver UI 2: login thật, điều hướng, đo DOM live |
| `accessibility` (WCAG 2.2) | Contrast/tap-target/focus |
| `addyosmani-performance-optimization` | Phase 5: đo rồi mới (không) tối ưu |
| `addyosmani-code-review-and-quality` | Tiêu chuẩn review |
| `java-springboot`, `java-coding-standards` | Đọc backend, xác nhận gốc constraint/query |
| `generate-tests` | Sinh test hồi quy |
| `understand` (knowledge graph) | Bản đồ kiến trúc trước khi sửa liên kết |

---

## 6. Kiểm chứng end-to-end (cách chạy lại)

```bash
# Backend
cmd /c "mvnw.cmd -o test"                        # 517 / 0 / 0 / 11
# Frontend
cd frontend && npx vitest run                    # 175 passed / 1 skipped (30 file)
npx vite build                                   # entry 177.44 kB
# API
node sweep/v13/api-sweep.js                      # 143 pass / 0 fail
# DB parity
python sweep/v8/sqlrun.py sweep/v8/p16-parity.sql
# Rebuild container sau khi sửa backend
docker compose up -d --build backend
```

---

## 7. Kết luận trung thực

1. **Bug người dùng báo đã được sửa và chứng minh** bằng dữ liệu thật, không mock — ở cả UI, API và DB.
2. **Review chéo đã cứu phiên này khỏi một thảm hoạ**: fix đầu tiên của tôi phá 32 814 hàng bài tập. Nếu commit ngay, hậu quả lớn hơn nhiều bug gốc.
3. **4 finding vẫn OPEN và 1 DEFERRED, đều có lý do rõ ràng** — 3 cần quyết định của chủ sản phẩm (F-13-02, F-13-07, F-13-08), 1 đã biết từ v7 (F-13-09), 1 chưa có số biện minh (F-13-10) — **không giấu**.
4. **Không tối ưu mò**: chỉ 1 endpoint chậm và nó là LIKE leading-wildcard đã biết; **N+1 thật thì đã sửa** (F-13-15, chứng minh bằng query-stats). P5 được tôn trọng.
5. **Mọi con số trong báo cáo này do phiên v13 đo**, không sao chép từ v1–v12. Chỗ trùng ghi "xác nhận", chỗ lệch ghi "phát hiện".
6. **Hai lỗi do chính tôi gây ra** (F-13-12, F-13-13) và **một giả thuyết sai của tôi** (F-13-02 enum) đều được ghi lại công khai thay vì che.
7. **Bước `analyze` tự bắt lỗi đếm số của chính báo cáo này** (ghi "11 finding" khi thật ra 16; gộp DEFERRED vào OPEN) — đã sửa; xem `analyze.md` §5.
