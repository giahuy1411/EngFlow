package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.GradeRequest;
import com.datn.engflow.model.dto.response.*;
import com.datn.engflow.service.ExerciseService;
import com.datn.engflow.service.LessonContentService;
import com.datn.engflow.service.LessonContentService.LessonContentInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons/{lessonId}/exercises")
@RequiredArgsConstructor
/**
 * class LessonExerciseController.
 */
public class LessonExerciseController {

    private final ExerciseService exerciseService;
    private final LessonContentService lessonContentService;

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
        List<ExerciseResponse> exercises = exerciseService.getExercisesByLesson(lessonId, includeAnswers);
        return ResponseEntity.ok(exercises);
    }

    @PostMapping("/grade")
    public ResponseEntity<GradeResponse> gradeExercises(
            @PathVariable Long lessonId,
            @RequestBody GradeRequest request) {
        GradeResponse response = exerciseService.gradeExercises(lessonId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/submit")
    public ResponseEntity<GradeResponse> submitExercises(
            @PathVariable Long lessonId,
            @RequestBody GradeRequest request,
            Authentication authentication) {
        GradeResponse response = exerciseService.submitExercises(lessonId, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

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
     */
    @GetMapping("/content")
    public ResponseEntity<LessonContentInfo> getCleanContent(
            @PathVariable Long lessonId) {
        LessonContentInfo info = exerciseService.getCleanContent(lessonId);
        return ResponseEntity.ok(info);
    }
}
