package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.GradeRequest;
import com.datn.engflow.model.dto.response.AttemptDetailResponse;
import com.datn.engflow.model.dto.response.AttemptHistoryResponse;
import com.datn.engflow.model.dto.response.ExerciseResponse;
import com.datn.engflow.model.dto.response.GradeResponse;
import com.datn.engflow.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons/{lessonId}/exercises")
@RequiredArgsConstructor
public class LessonExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping
    public ResponseEntity<List<ExerciseResponse>> getExercises(
            @PathVariable Long lessonId,
            @RequestParam(defaultValue = "false") boolean includeAnswers) {
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
        List<AttemptHistoryResponse> history = exerciseService.getAttemptHistory(lessonId, authentication.getName());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<AttemptDetailResponse> getAttemptDetail(
            @PathVariable Long lessonId,
            @PathVariable Long attemptId,
            Authentication authentication) {
        AttemptDetailResponse detail = exerciseService.getAttemptDetail(lessonId, attemptId, authentication.getName());
        return ResponseEntity.ok(detail);
    }
}
