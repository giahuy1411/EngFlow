# audit-v16-full — Phase 2: timezone re-verify (F-13-09)

**Bối cảnh:** v15 chỉ **verify + ghi docs** timezone (không migrate). Đây là lần đo lại độc lập.

## Đo trong phiên này (2026-09-26)

| Chỉ số | Giá trị | Nguồn |
|---|---|---|
| `LocalDateTime.now()` trong `src/main` | **11** | `grep -rI` |
| `LocalDate.now()` trần (không `withZone`/`ZoneId`/`Asia/Ho_Chi_Minh`) | **0** | `grep` có loại trừ |
| Clock bean | **CÓ** — `EngflowApplication.java:43-46` `Clock.systemDefaultZone()` | đọc file |

**7 file chứa writer:** `ExerciseService`, `LessonService`, `PaymentService`, `ShadowingAiGradingService`,
`SpeakingSubmissionService`, `SrsService`, `VideoLessonService`.

## Kết luận

Khớp kết luận v15 (đo lại, không chép): convention **naive-VN (+07)** nhất quán; **0** `LocalDate.now()` trần
→ đường date-granular (streak, `study_days`) miễn nhiễm. **Giữ nguyên, không migrate** (theo quyết định V2 v15).

`Clock` bean tồn tại (`Clock.systemDefaultZone()`) nhưng phần lớn service vẫn gọi `LocalDateTime.now()` trực tiếp
— đây là ghi chú thiết kế đã biết, không phải finding mới (không có consumer thứ hai cần UTC).
