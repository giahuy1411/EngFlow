# audit-v20 — Baseline sau Phase 0 (số đo thật)

**Ngày:** 2026-09-28 (+07)

## Runtime

| Container | Trạng thái |
|---|---|
| `engflow-sqlserver` | Up (healthy) |
| `engflow-redis` | Up |
| `engflow-backend` | Up — `Started EngflowApplication in 12.542 seconds` |
| `engflow-frontend` | Up :5173 (200), Vite proxy `/api` → backend OK |

## Baseline test (sau khi sửa F-20-06)

| Hạng mục | Kết quả | Khớp baseline v19? |
|---|---|---|
| `mvnw.cmd test` | **541 tests / 0 fail / 0 error / 11 skipped** — BUILD SUCCESS | ✅ khớp |
| `npx vitest run` | **194 passed / 1 skipped / 32 files** | ✅ khớp |
| Parity DB | `1470\|43738\|5\|118\|29\|4\|3\|13\|10`, `STUDY_DAYS=4`, `PENDING_PAYMENTS=0`, `EXERCISE_ATTEMPTS=33` | payments 13 (baseline mới) |

> ⚠️ Trước khi sửa F-20-06, `mvnw test` **BUILD FAILURE** — cây làm việc không compile.
> Số 541 ở trên là SAU khi khôi phục `public class AdminService {`.

## Harness self-check

`node sweep/harness/assert-harness.js` → **ALL CLEAN (8/8 check)**.
- check 6 (default round) trước fix: FAIL (`default=audit-v19-full latest=audit-v20-full`)
- check 8 (mới, default stale): mutation-tested — inject v19 → FAIL đúng chỗ.

## Khoảng trống tài liệu (đo thật)

### Backend
- 196 file Java; **2** thiếu Javadoc cấp class (`PremiumRequired`, `AiAnswerBackfillService`)
- **74** public method thiếu Javadoc, trên **20 file**
- **139/139** file đã sửa là **comment-only** (xác nhận bằng stripper biết text block)

### Frontend
- 83 `.vue`: **30** không có comment, **66** không có comment ngay sau `<script setup>`
- 36 `.js` (non-test): **10** không có comment
- 32 `.test.js`: 12 không có comment (ưu tiên thấp)

## 2 file Java hỏng/thay đổi — kết luận

| File | Kết luận |
|---|---|
| `service/AdminService.java` | **HỎNG THẬT** — mất dòng class decl. ĐÃ FIX. |
| `config/WebConfig.java` | False positive — class comment-out có chủ đích. |
| `service/SubtitleTranslationService.java` | False positive (stripper cũ thiếu text-block). Thực ra comment-only. |
| `service/YouTubeTranscriptService.java` | False positive (như trên). Thực ra comment-only. |
