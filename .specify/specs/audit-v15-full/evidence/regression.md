# Phase 6 — regression (đếm từ run log)

## Backend (`mvnw -o test`)

| | Baseline (Phase 0) | Sau v15 | Delta |
|---|---|---|---|
| Tests run | 525 | **512** | −13 |
| Failures | 0 | **0** | 0 |
| Errors | 0 | **0** | 0 |
| Skipped | 11 | 11 | 0 |
| BUILD | SUCCESS | **SUCCESS** | — |

**Giải thích delta −13 (khớp chính xác, không phải test bị "mất" do lỗi):**

| Nguồn | Số test |
|---|---|
| `LessonStructureBlockTypeGuardTest` xoá (đặc thù Đường B) | −6 |
| `AuditV10DraftLessonStructureGuardTest` xoá (đặc thù Đường B) | −6 |
| `AuditV7SecurityWaveTest`: 2 test F56 → 1 test "endpoint đã gỡ" | −1 |
| **Tổng** | **−13** |

Không có test nào fail. Draft-leak protection (F88/F89/F105/F115) vẫn được phủ bởi
`AuditV8DraftLessonVisibilityTest` (7) + `AuditV9DraftLessonGradeGuardTest` (5) + `AuditV10DraftLessonSubmissionGuardTest`.

## Frontend (`vitest run`)

| | Baseline (Phase 0) | Sau v15 | Delta |
|---|---|---|---|
| Test files | 30 passed + 1 skipped (31) | **28 passed + 1 skipped (29)** | −2 |
| Tests | 194 passed + 1 skipped (195) | **178 passed + 1 skipped (179)** | −16 |
| Failures | 0 | **0** | 0 |

**Giải thích delta −16:** xoá `LessonBlocks.test.js` (14 test) + `AdminLessonBuilder.f13.test.js` (2 test)
= 16 test, 2 file. Khớp chính xác.

## Build

`npx vite build` → `✓ built in 6.82s`; main bundle `index-YF4VlPG5.js` **177.74 kB** (baseline 177.98 kB).
