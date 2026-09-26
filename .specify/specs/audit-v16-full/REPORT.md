# audit-v16-full — BÁO CÁO

**Ngày:** 2026-09-26 (+07) · **Nhánh:** `audit-v15-full` @ `6f046b6` (**giữ nguyên — không tạo nhánh mới** theo yêu cầu)
**Tiền nhiệm:** `audit-v15-full` (đã xong, đã commit) · **Artifact:** `.specify/specs/audit-v16-full/`
**Phạm vi người dùng chốt:** quét toàn bộ codebase + DB, chạy toàn bộ API ↔ UI, tối ưu đo được, kiểm chứng
design system (Playful Geometric + Be Vietnam Pro), 2 vòng, dọn rác, cập nhật tài liệu.

---

## 1. Tóm tắt một đoạn

EngFlow sau v15 (đã gỡ Đường B) được **quét lại toàn diện**: **143/143 API probe pass**, **58/58 deep-probe pass**,
**DB 0 orphan / 22 FK đều có index**, **design system khớp prompt** (7 670 phần tử × 60 tổ hợp route×viewport:
0 font lạ, 0 token drift, 0 overflow), **UI 0 console/api/contrast/guard error**, **CLS ≤ 0.00095**, **perf không có
win rõ** (median 11.9 ms). Vòng này phát hiện + **sửa 8 finding** (1 MEDIUM + 7 LOW, gồm 1 lỗ hổng harness
`study_days` mà guard tự bắt). **Review chéo đối kháng bắt thêm 4 defect trong chính fix của tôi + 2 lỗi nội dung
tài liệu demo** — đã sửa hết trước khi commit (đây là giá trị đo được của bước review). Đã **viết lại
`docs/demo-engflow-4-chuc-nang.md`** cho người không biết lập trình (kèm code thật đã kiểm chứng), **bác bỏ 2 kết
luận SAI** (không phải rò rỉ secret; không phải rác). Backend **515/0**, frontend **178/1** — 0 regression.

---

## 2. Đã làm

### 2.1 Phase 0 — SpecKit pipeline + baseline
| Việc | Kết quả |
|---|---|
| Nhánh | `audit-v15-full`, cây sạch (`git status` = nothing to commit) — **không tạo nhánh mới** |
| Pipeline docs | `spec.md` · `clarify.md` · `checklist.md` · `plan.md` · `tasks.md` |
| Harness drift guard | `assert-harness.js` **ALL CLEAN (5/5)** |
| Backend baseline | **515 / 0 / 0 / 11** — BUILD SUCCESS |
| Frontend baseline | **178 pass / 1 skip (29 file)**; build **177.74 kB** |
| Container | 8 up + Ollama :11434 |
| Parity | `1470\|43738\|5\|118\|29\|4\|3\|12\|10` · STUDY_DAYS=4 · PENDING_PAYMENTS=0 |
| Inventory | 121 annotation / 137 expanded / 135 distinct / 25 controller |

### 2.2 Phase 1 — API sweep
- `api-sweep.js` C1–C15: **pass=143 fail=0 blocked=1 n/a=2** (blocked = webhook real-money; n/a = upload cần media thật).
- `deep-probe.js`: **58/0/0/0** (mc-guard 18, sort 2, contract 13, roles 25).
- `search-sort.js`: case-insensitive ✅, guard <2 ký tự ✅, `?sort=notacolumn` → **400** ✅.
- Rate-limit: flush bucket mỗi batch → 0 tự-gây-429.

### 2.3 Phase 2 — DB + secret-scan
- **22 FK, 0 orphan**; **0 FK thiếu leading index**; 19 bảng (18 thật + sysdiagrams).
- Slow query: app nóng nhất **avg 0.14 ms** (1902 lần); không N+1.
- Timezone: 11 `LocalDateTime.now()`, **0 `LocalDate.now()` trần** — khớp convention naive-VN.
- Secret-scan: `.env`/`.env.example`/`.agents/mcp_config.json` **an toàn** (xem §4 bác bỏ).

### 2.4 Phase 3 — UI/UX
- `ui-sweep.js`: **0 guard/console/api/page/contrast/overflow**; responsive 35 ô 0 tràn; a11y 0 alt/noname lỗi.
- `design-v2.js`: **7 670 phần tử, 60 tổ hợp** — 0 font lạ, 0 legacy, 0 token thiếu/drift, 0 overflow, 0 icon sai,
  font Be Vietnam Pro **nạp thật**.
- MCP (chrome-devtools): tương tác thật login → search → lesson → grade → submit → history.

### 2.5 Phase 4 — Core E2E (API↔UI)
6 nhóm (**Bài học/Bài tập, Streak, Auth, Search/Sort, CRUD, AI** + Game/Flashcard/SRS, Payment, Speaking/Video)
đều có bằng chứng; Bài học/Bài tập verify sâu nhất qua MCP thật.

### 2.6 Phase 5 — Performance
34 endpoint median **11.9 ms** (max 163 ms = admin search LIKE); bundle 177.74 kB; CLS 0.00069/0.00095/0.00003.
**Không có win rõ → không tối ưu** (P5).

### 2.7 Phase 6/7 — Fix + review + vòng 2
- 8 finding FIXED (xem §4).
- Review chéo đối kháng mọi fix AI sinh (xem `evidence/review-v16.md`).
- Vòng 2 chạy lại toàn bộ trên build cuối (xem `evidence/round-2.md`).

### 2.8 Phase 8 — Docs + cleanup
- Cập nhật `README.md`, `CLAUDE.md`, `AGENTS.md`; **viết lại `docs/demo-engflow-4-chuc-nang.md`**.
- Cleanup manifest + thực thi (xem `evidence/cleanup-manifest.md`).

---

## 3. Chưa làm (và lý do)

| Hạng mục | Lý do |
|---|---|
| Tối ưu hiệu năng | **Không có win đo được** — P5 cấm tối ưu khi chưa chứng minh (perf/DB/bundle/CLS đều tốt) |
| Webhook SePay chữ ký thật | Biên **real-money** — BLOCKED từ v11 |
| Upload + assess speaking bằng media thật | Cần mic/file thật; đã biết mic giả → FAILED đúng thiết kế |
| Migrate timezone sang UTC | V2 v15: chưa có consumer thứ hai |
| GitHub issues (`taskstoissues`) | V3: quyết định không tạo issue |
| Sửa `?sort=` trên lessons/decks | F-13-11 (đã biết, thiết kế cố định thứ tự); chưa đo được tác động người dùng |
| MCP **playwright** | **Giới hạn môi trường**: thiếu Chrome channel (`Chromium distribution 'chrome' is not found`) → dùng **chrome-devtools MCP** thay thế (đã verify hoạt động) |

---

## 4. Đã fix và cách fix (9 finding)

| ID | Mức | Vấn đề | Cách fix |
|---|---|---|---|
| **F-16-01** | MEDIUM | Harness ghi `study_days` thật mà không dọn | `cleanupStudyDays(from)` vào `sweep/v8/ui/lib.js` (cửa sổ half-open) + gọi trong 5 harness (đều `try/finally`) + `deep-probe` inline; **sửa lại attribution sau review** (login KHÔNG ghi; writer là submit/review/study/checkin) |
| **F-16-02** | LOW | Blur shadow trong `.lesson-html details[open]` | `3px 3px 0px 0px var(--geo-accent)` (hard shadow) |
| **F-16-03** | LOW | Skeleton gradient dùng `#EAE4D6` ngoài palette | → `var(--geo-border)` |
| **F-16-04** | — | Topbar admin shadow bán trong suốt | **CLOSED** — offset không blur, không vi phạm; **hoàn nguyên** (đổi gây viền đôi) |
| **F-16-05** | LOW* | Active nav admin → `--geo-shadow-xs` (tôi tự gây: vô hình trên nền `#1E293B`) | **HOÀN NGUYÊN** về `rgba(0,0,0,0.25)` |
| **F-16-06** | LOW | Flashcard title `drop-shadow-sm` (blur) | → `[text-shadow:3px_3px_0_var(--geo-border)]` |
| **F-16-07** | LOW (docs) | README "26 REST controllers" | → **25** (21 + 4 subpackage) |
| **F-16-08** | LOW (docs) | CLAUDE.md migrations "V1-V8" | → **V1-V10** |
| **F-16-09** | MED (docs) | Demo doc: streak claim **đảo ngược** + khoá sai lần + citation drift | **SỬA** (review bắt được) |

> `*` = defect **tôi tự gây** trong phiên, review đối kháng bắt trước khi commit.

### Review chéo bắt 4 defect trong fix của tôi (bằng chứng `review-v16.md`)
1. F-16-01 **root cause SAI** (nói login ghi `study_days`; thực ra login chỉ đọc) → sửa lại.
2. F-16-01 helper dùng **equality 1 ngày** → false-pass nếu chạy qua nửa đêm → sửa sang cửa sổ half-open.
3. F-16-01 `design-v2.js` để cleanup **ngoài `finally`**; `ui-sweep` **bỏ `cleanSd.ok` khỏi exit code** → sửa.
4. F-16-05 đổi sang `--geo-shadow-xs` → **vô hình** trên sidebar `#1E293B` → hoàn nguyên.
5. Demo doc: streak claim đảo ngược + khoá ở lần thứ 6 (thực ra lần 5) → sửa (F-16-09).

### Bác bỏ 2 kết luận SAI (kỷ luật V4 — kiểm chứng 2 chiều)
1. **`.agents/mcp_config.json` "chứa secret đã commit"** → **SAI**: không track, đã gitignore, **chưa từng commit**.
2. **`scripts/figma-export/node_modules` "rác đã commit"** → **SAI**: không track (gitignore), VÀ `cls-probe.js:36`
   **cố ý** dùng `playwright-core` từ đó → **KHÔNG xoá**.

### Tự phát hiện + tự sửa trong phiên
- Phase 0: chạy `api-inventory.js` **thiếu `--audit`** → ghi đè evidence v15 → phát hiện qua `git status`,
  **khôi phục `git checkout --`** rồi chạy lại đúng. (Đúng lớp F-15-17.)

---

## 5. Kết quả kiểm thử (đếm từ run log — đo lại sau MỌI fix)

| Bộ | Kết quả |
|---|---|
| Backend (`mvnw.cmd test`) | **515 / 0 / 0 / 11** — BUILD SUCCESS (log cuối sau mọi fix) |
| `HarnessDriftTest` | **3 / 0 / 0** |
| Frontend (`vitest run`) | **178 pass / 1 skip (29 file)** — sau mọi fix, 0 regression |
| Frontend build | **177.74 kB** (gzip 67.68) |
| `api-sweep` (vòng 1 & vòng 2b) | **143 / 0** (1 blocked, 2 n/a) — hội tụ |
| `deep-probe` (sau fix) | **58 / 0** — `DEEP_SD_REMAINING=0` |
| `search-sort` | 0 finding mới |
| `ui-sweep` (sau fix) | **0** guard/console/api/page/contrast/overflow; responsive 0 tràn; EXIT=0 |
| `design-v2` (vòng 1 & 2) | 0 badFont/legacy/token/overflow |
| `cls-probe` | 0.00069 / 0.00095 / 0.00003 |
| `perf-probe` | 34 endpoint, median 11.9 ms |
| Parity cuối | `1470\|43738\|5\|118\|29\|4\|3\|12\|10` · STUDY_DAYS=4 · PENDING=0 |

---

## 6. Skill / plugin đã nạp

**SpecKit:** `speckit-constitution`, `speckit-specify`, `speckit-clarify`, `speckit-checklist`, `speckit-plan`,
`speckit-tasks`, `speckit-implement`, `speckit-converge`, `speckit-analyze` (analyze chạy **giữa kỳ** + cuối kỳ).
**Superpowers:** `using-superpowers`, `brainstorming`, `systematic-debugging`, `verification-before-completion`,
`requesting-code-review`, `dispatching-parallel-agents`.
**Domain:** `accessibility` (WCAG 2.2 AA), `java-springboot`, `java-coding-standards`, `generate-tests`, `frontend-design`.
**addyosmani-*:** `browser-testing-with-devtools`, `performance-optimization`, `code-review-and-quality`,
`debugging-and-error-recovery`, `security-and-hardening`, `source-driven-development`.
**MCP:** `chrome-devtools` (`evaluate_script`, `take_snapshot`, `list_console_messages`, `take_screenshot`) — **đã dùng thật**.
**Agent:** `Explore` ×3 (map backend/frontend/workflow), `general-purpose` (review chéo đối kháng).

---

## 7. File đã xoá trong phiên (ràng buộc V1)

Xem `evidence/cleanup-manifest.md`. Tóm tắt:
- **Build/scratch (untracked, gitignored):** `target/**` (22 MB), `frontend/dist/**` (1.4 MB),
  `frontend/node_modules/.vite/**`, `.playwright-mcp/**` (14 file cũ).
- **Script chết (tracked, 0 tham chiếu code):** `scripts/audit-v4-api-sweep.mjs`,
  `scripts/{create,close,read}-audit-issues.js`, `scripts/clean-lesson444-block.js`, `scripts/restore-lesson444.js`.
- **Probe residue (DB):** `study_days` hàng 40076; payment PENDING; deck/vocab/lesson audit (đã tự dọn).
- **KHÔNG xoá:** `scripts/figma-export/**` (cls-probe phụ thuộc), `uploads/**`, `.specify/specs/audit-v8..v15/**`.

---

## 8. Kết luận trung thực

- **Yêu cầu người dùng hoàn thành**: quét codebase+DB, chạy toàn bộ API↔UI, tối ưu (đo được), kiểm chứng design
  system (đồng bộ prompt — **CÓ**), 2 vòng, dọn rác, cập nhật tài liệu + viết lại demo doc kèm code.
- **Không giấu điểm yếu**: 2 kết luận ban đầu **SAI** đã bị bác bỏ (secret, rác) — ghi rõ. 1 lỗi tự gây (thiếu `--audit`)
  đã tự sửa. Perf **không có win** — không "tạo việc".
- **Điểm mạnh nhất**: guard `assertClean` **tự bắt** residue thật (F-16-01) — bằng chứng khách quan, không suy đoán;
  mọi fix có before/after; demo doc dẫn code đã kiểm chứng tồn tại.
