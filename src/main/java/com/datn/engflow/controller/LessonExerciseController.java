package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.GradeRequest;
import com.datn.engflow.model.dto.response.*;
import com.datn.engflow.service.ExerciseService;
import com.datn.engflow.service.LessonContentService;
import com.datn.engflow.service.LessonService;
import com.datn.engflow.service.LessonContentService.LessonContentInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cổng REST cho bài tập của một bài học: đọc danh sách, chấm, nộp, xem lịch sử attempt và
 * lấy phần nội dung đã cắt bỏ đáp án.
 *
 * <p>Tầng controller — dữ liệu bài tập đến từ {@link ExerciseService}, phần nội dung bài
 * đến từ {@link LessonContentService}. {@link LessonService#assertLessonVisible} là chốt
 * chặn bài nháp: mọi đường chạm tới bài tập — kể cả đường chấm/nộp, vì đáp án đúng nằm trong
 * phản hồi — đều phải đi qua nó trước.
 */
@RestController
@RequestMapping("/api/lessons/{lessonId}/exercises")
@RequiredArgsConstructor
public class LessonExerciseController {

    private final ExerciseService exerciseService;
    private final LessonContentService lessonContentService;
    private final LessonService lessonService;

    /**
     * Danh sách bài tập của bài học.
     *
     * @param lessonId       id bài học
     * @param includeAnswers có kèm đáp án đúng hay không; chỉ admin được yêu cầu
     * @param authentication principal hiện tại
     * @return danh sách bài tập, hoặc 403 khi người gọi không phải admin mà lại xin đáp án
     */
    @GetMapping
    public ResponseEntity<List<ExerciseResponse>> getExercises(
            @PathVariable Long lessonId,
            @RequestParam(defaultValue = "false") boolean includeAnswers,
            Authentication authentication) {
        if (includeAnswers) {
            if (authentication == null) {
                authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            }
            boolean isAdmin = authentication != null && authentication.isAuthenticated()
                    && authentication.getAuthorities() != null
                    && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            if (!isAdmin) {
                return ResponseEntity.status(403).build();
            }
        }
        // audit-v8 F88: bai nhap khong duoc doc cong khai (admin bo qua de preview).
        lessonService.assertLessonVisible(lessonId, isAdmin(authentication));
        List<ExerciseResponse> exercises = exerciseService.getExercisesByLesson(lessonId, includeAnswers);
        return ResponseEntity.ok(exercises);
    }

    /**
     * Chấm bài không ghi vào lịch sử — dùng cho vòng luyện tập giữa bài.
     *
     * @param lessonId       id bài học
     * @param request        danh sách câu trả lời
     * @param authentication principal hiện tại
     * @return điểm và phần giải thích từng câu
     */
    @PostMapping("/grade")
    public ResponseEntity<GradeResponse> gradeExercises(
            @PathVariable Long lessonId,
            @RequestBody GradeRequest request,
            Authentication authentication) {
        // audit-v9 F105: draft lesson must not leak correctAnswer via grade/submit.
        lessonService.assertLessonVisible(lessonId, isAdmin(authentication));
        GradeResponse response = exerciseService.gradeExercises(lessonId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Nộp bài và ghi vào lịch sử attempt của người học.
     *
     * @param lessonId       id bài học
     * @param request        danh sách câu trả lời
     * @param authentication principal hiện tại, dùng làm danh tính người nộp
     * @return điểm, phần giải thích và id attempt đã lưu
     */
    @PostMapping("/submit")
    public ResponseEntity<GradeResponse> submitExercises(
            @PathVariable Long lessonId,
            @RequestBody GradeRequest request,
            Authentication authentication) {
        // audit-v9 F105: same guard as grade.
        lessonService.assertLessonVisible(lessonId, isAdmin(authentication));
        GradeResponse response = exerciseService.submitExercises(lessonId, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Lịch sử nộp bài của chính người gọi trên bài này.
     *
     * @param lessonId       id bài học
     * @param authentication principal hiện tại
     * @return lịch sử attempt, hoặc 401 khi principal không phải người dùng thật
     */
    @GetMapping("/attempts")
    public ResponseEntity<List<AttemptHistoryResponse>> getAttempts(
            @PathVariable Long lessonId,
            Authentication authentication) {
        // audit-v7 F54: guard phòng thủ — security chain giờ bắt buộc authenticated
        // cho /attempts/**, nhưng null principal không bao giờ được thành NPE 500.
        if (!isRealUser(authentication)) {
            return ResponseEntity.status(401).build();
        }
        List<AttemptHistoryResponse> history = exerciseService.getAttemptHistory(lessonId, authentication.getName());
        return ResponseEntity.ok(history);
    }

    /**
     * Chi tiết một attempt đã nộp.
     *
     * @param lessonId       id bài học chứa attempt
     * @param attemptId      id attempt
     * @param authentication principal hiện tại
     * @return chi tiết attempt, hoặc 401 khi principal không phải người dùng thật
     */
    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<AttemptDetailResponse> getAttemptDetail(
            @PathVariable Long lessonId,
            @PathVariable Long attemptId,
            Authentication authentication) {
        if (!isRealUser(authentication)) {
            return ResponseEntity.status(401).build();
        }
        AttemptDetailResponse detail = exerciseService.getAttemptDetail(lessonId, attemptId, authentication.getName());
        return ResponseEntity.ok(detail);
    }

    /** True khi principal that su co ROLE_ADMIN (anonymous user khong bao gio co). */
    private static boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String name = authentication.getName();
        if (name == null || "anonymousUser".equals(name)) {
            return false;
        }
        return authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /** True only for an authenticated, non-anonymous principal. */
    private static boolean isRealUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return false;
        }
        return authentication.getName() != null && !"anonymousUser".equals(authentication.getName());
    }

    /**
     * Returns lesson content with Answer sections stripped + per-exercise HTML fragments.
     *
     * @param lessonId       id bài học
     * @param authentication principal hiện tại
     * @return nội dung bài đã loại mục đáp án
     */
    @GetMapping("/content")
    public ResponseEntity<LessonContentInfo> getCleanContent(
            @PathVariable Long lessonId,
            Authentication authentication) {
        // audit-v8 F88: cung guard voi /exercises.
        lessonService.assertLessonVisible(lessonId, isAdmin(authentication));
        LessonContentInfo info = exerciseService.getCleanContent(lessonId);
        return ResponseEntity.ok(info);
    }
}
