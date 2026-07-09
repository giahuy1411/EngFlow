package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.GradeRequest;
import com.datn.engflow.model.dto.response.ExerciseResponse;
import com.datn.engflow.model.dto.response.GradeResponse;
import com.datn.engflow.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons/{lessonId}/exercises")
@RequiredArgsConstructor
public class LessonExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping
    public ResponseEntity<List<ExerciseResponse>> getExercises(@PathVariable Long lessonId) {
        List<ExerciseResponse> exercises = exerciseService.getExercisesByLesson(lessonId);
        // Strip correctAnswer for students — only admin sees it
        List<ExerciseResponse> sanitized = exercises.stream()
                .map(e -> ExerciseResponse.builder()
                        .id(e.getId())
                        .lessonId(e.getLessonId())
                        .question(e.getQuestion())
                        .options(e.getOptions())
                        .exerciseType(e.getExerciseType())
                        .difficulty(e.getDifficulty())
                        .explanation(e.getExplanation())
                        .imageUrl(e.getImageUrl())
                        .audioUrl(e.getAudioUrl())
                        .orderIndex(e.getOrderIndex())
                        .build())
                .toList();
        return ResponseEntity.ok(sanitized);
    }

    @PostMapping("/grade")
    public ResponseEntity<GradeResponse> gradeExercises(
            @PathVariable Long lessonId,
            @RequestBody GradeRequest request) {
        GradeResponse response = exerciseService.gradeExercises(lessonId, request);
        return ResponseEntity.ok(response);
    }
}
