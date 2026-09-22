# audit-v13 — baseline sau khi fix (đo lại, so với Phase 0)

| Chỉ số | Phase 0 (trước) | Sau fix | Chênh |
|---|---|---|---|
| Backend tests | 499 / 0 fail / 0 error / 11 skipped | **511 / 0 / 0 / 11** | +12 test mới |
| Frontend tests | 127 passed / 1 skipped (25 file) | **141 passed / 1 skipped (27 file)** | +14 test mới |
| Build entry | 177.44 kB (gzip 67.56) | (đo lại cuối) | — |
| Parity | 1470\|43735\|72\|118\|29\|15\|4\|126\|10\|5 | **giữ nguyên** | 0 |

## Test hồi quy đã thêm

| File | Số test | Cho finding |
|---|---|---|
| `frontend/src/views/lessons/LessonExerciseTab.f13.test.js` | 11 | F-13-01, F-13-12, F-13-13 |
| `frontend/src/views/__tests__/contrast-ink-tokens.test.js` | 3 | F-13-03/04/05 |
| `src/test/java/.../ExerciseServiceMultipleChoiceOptionsTest.java` | 12 | F-13-01, F-13-12 |
| `src/test/java/.../GlobalExceptionHandlerProblemDetailTest.java` | +1 | F-13-11 |

Tổng **+27 test** (12 backend + 15 frontend), tất cả pass, **0 regression** trên suite cũ.
