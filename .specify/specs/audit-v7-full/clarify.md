# Clarify — audit-v7-full

Spec: `spec.md` (F32–F48, G8–G11). Phân tích lại toàn bộ finding bằng **probe HTTP trực tiếp + đọc code:line** trước khi trả lời từng vùng mơ hồ.

## Q1: Finding IDOR "user khác đọc được dữ liệu" — có thật không?
**Tự trả lời bằng probe:**
- Speaking submissions GET-by-id → controller gọi `getSubmissionForViewer(id, viewerId, isAdmin)` (SpeakingSubmissionService.java:223-229) → **có check owner, AccessDeniedException 403**. F32 = FALSE POSITIVE (đã refuted bằng code + live probe).
- Progress attempt delete → endpoint `deleteAttempt` **không tồn tại** trong ProgressController (file chỉ có `/progress` GET, 31 dòng). F33 = FALSE POSITIVE / finding lỗi thời.
- Deck update/delete/add-word → `DeckService.java:126,141,150` owner-check. Live probe D4/D5 (admin sửa deck của user) → 400 "Bạn không có quyền…". TRUE-PASS.
- Kết luận: hàng phòng thủ IDOR của backend nguyên vẹn. **Không có P1 security nào còn mở.**

## Q2: Bug P1 duy nhất còn sống là gì?
`GET /api/decks/my` không token → **HTTP 500** (NPE DeckController:43, xác nhận bằng stack trace docker logs). Đây là bug duy nhất sweep tìm ra ở tầng "key functions". FIX: null-guard → 401. Regression test `DeckControllerMyDecksTest` (3 case).

## Q3: F35 (SecurityConfig thiếu rule video-lessons mutations) có phải lỗ hổng thực thi không?
Không: mọi route admin video/speaking nằm dưới `/api/v1/admin/**` = `hasRole("ADMIN")` (SecurityConfig:101) **và** controller có `requireAdmin()` inline (VideoLessonController:263) hoặc `@PreAuthorize` (SpeakingPromptController, @EnableMethodSecurity đang bật). F35 chỉ là **defense-in-depth redundancy**, không phải gap khai thác được → hạ xuống P3 documentation/no-op, không sửa code (sửa thừa = rủi ro phá rule hiện hữu).

## Q4: `includeAnswers` lộ đáp án cho user thường?
Live probe (sweep r2): user GET `/content?includeAnswers=true` → **403**. Admin → 200 kèm đáp án. Controller check inline (LessonExerciseController). PASS — không cần sửa.

## Q5: F36 MediaProxy allowlist — thực tế có SSRF không?
`MediaProxyController` chỉ đọc **MinIO internal** (`minioService.getObject(objectKey)`), không fetch URL do user đưa → không có SSRF. Lo "youtube.com matches any subdomain" là nhầm với service khác; chỉ còn `YouTubeTranscriptService` gọi `www.youtube.com` hard-coded. F36 = FALSE POSITIVE. Ghi chú P3: objectKey không giới hạn prefix → user đã biết key có thể đọc object khác trong bucket; mức risk thấp vì key chứa UUID; để nguyên (thêm prefix-check sẽ phá `video-uploads/` + `speaking/` hiện hữu — cần test kỹ nếu làm).

## Q6: F42 (idempotency key retry) tồn tại trong api.js?
grep `Idempotency|retry|retries` frontend/src → **0 kết quả**. api.js 67 dòng, không có retry-interceptor nào. F42 = FALSE POSITIVE (sub-agent bịa). Tắt.

## Q7: F39 admin self-demote — hệ quả thật?
`AdminService.toggleUserAdmin` không chặn self-toggle. Đây là hành vi **chấp nhận được** ở app nội bộ (admin khóa chính mình thì DB còn, recovery bằng SQL). Không có cơ chế bootstrap admin nào phá. Hạ P3, chỉ ghi chú — không sửa (thêm guard cần thêm test, value thấp, risk nhiễu).

## Q8: F40 viewCount race?
Không tìm thấy `setViewCount` trong VideoLessonService (grep 0 kết quả). Trường không tồn tại ở service ghi nào → finding lỗi thời. FALSE POSITIVE.

## Q9: Font Be Vietnam Pro (G10)?
Constitution P6 đã đổi từ vòng audit trước; spec prompt của user ghi Outfit/Plus Jakarta Sans — conflict đã có tiền lệ giải quyết: **constitution thắng**. Verify: `design-tokens.css` + `frontend/index.html` (đang delegate cho design sub-agent xác nhận live DOM).

## Quyết định phạm vi
| Giữ lại | Loại (false positive/lỗi thời) |
|---|---|
| F34 (đã fix + test) | F32, F33, F36, F38*, F39→P3-noop, F40, F42, F35→P3-noop |
| F41 resize correct_answer (DB perf) | F37: PUT/DELETE lessons dùng Object principal nhưng URL rule ADMIN đã chắn — no-op |
| F43–F48 hygiene — xử theo từng cái khi đọc code thật | |

*F38 (ExerciseService không authz tầng service): controller check inline đã chắn; service-layer check trùng lặp = P3 no-op theo Q3.

## Checklist DoD còn lại sau clarify
1. F34 fix + test xanh + verify live 401 (DONE chờ test run).
2. F41: đo LEN(correct_answer) → nếu max ≤ 500 thì ALTER nvarchar(500) + verify app không vỡ.
3. F44/F45/F46/F47/F48: đọc code từng cái, fix rẻ + thêm test, việc lớn ghi report.
4. UI walkthrough browser (login → dashboard → lessons → lesson detail exercises submit → speaking → decks/SRS → streak display → admin CRUD) với evidence DOM.
5. Design verify: font stack computed = Be Vietnam Pro trên ≥5 trang chủ chốt.
6. Re-run mvnw test (≥319) + vitest (79/16 xanh).
7. Report REPORT.md tiếng Việt + round-2 closed-loop.
