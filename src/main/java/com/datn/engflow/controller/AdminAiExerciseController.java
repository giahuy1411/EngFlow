package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.AiGenerateRequest;
import com.datn.engflow.model.dto.request.AiValidateRequest;
import com.datn.engflow.model.dto.response.AiExerciseResult;
import com.datn.engflow.model.dto.response.AiValidateResult;
import com.datn.engflow.model.dto.response.BatchGenerateStatus;
import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.ExerciseType;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.service.AiExerciseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/admin/exercises/ai")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
/**
 * class AdminAiExerciseController.
 */
public class AdminAiExerciseController {

    private final AiExerciseService aiExerciseService;
    private final LessonRepository lessonRepository;
    private final ExerciseRepository exerciseRepository;

    @PostMapping("/generate-async")
    public ResponseEntity<Map<String, String>> generateExercisesAsync(@Valid @RequestBody AiGenerateRequest request) {
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new com.datn.engflow.exception.ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        int count = request.getCount() != null ? request.getCount() : 5;
        ExerciseType type = (request.getExerciseType() == null || request.getExerciseType().isBlank())
                ? null : ExerciseType.valueOf(request.getExerciseType().toUpperCase());
        // Non-blocking: async generation returns a batchId immediately (HTTP 202 Accepted).
        String batchId = aiExerciseService.generateSingleAsync(lesson, count, type);
        return ResponseEntity.accepted()
                .body(Map.of("batchId", batchId, "status", "started"));
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateExercises(@Valid @RequestBody AiGenerateRequest request) {
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new com.datn.engflow.exception.ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        int count = request.getCount() != null ? request.getCount() : 5;
        ExerciseType type = (request.getExerciseType() == null || request.getExerciseType().isBlank())
                ? null : ExerciseType.valueOf(request.getExerciseType().toUpperCase());
        String batchId = aiExerciseService.generateSingleAsync(lesson, count, type);
        return ResponseEntity.accepted()
                .body(Map.of("batchId", batchId, "status", "started"));
    }

    @PostMapping("/generate-all")
    public ResponseEntity<AiExerciseResult> generateAllForLesson(@Valid @RequestBody AiGenerateRequest request) {
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new com.datn.engflow.exception.ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        int count = request.getCount() != null ? request.getCount() : 5;
        List<Exercise> exercises = aiExerciseService.generateAll(lesson, count);
        return ResponseEntity.ok(AiExerciseResult.builder()
                .generated(exercises.size())
                .valid(exercises.size())
                .errors(0)
                .exercises(exercises)
                .errorDetails(List.of())
                .build());
    }

    @PostMapping("/generate-batch")
    public ResponseEntity<BatchGenerateStatus> generateBatch(@RequestParam(defaultValue = "false") boolean force) {
        AiExerciseService.BatchProgress progress = aiExerciseService.generateBatch(force);
        return ResponseEntity.ok(BatchGenerateStatus.builder()
                .totalLessons(progress.totalLessons)
                .processed(progress.processed)
                .generated(progress.generated)
                .errors(progress.errors)
                .running(progress.running)
                .currentLesson(progress.currentLesson)
                .build());
    }

    @PostMapping("/save")
    public ResponseEntity<AiExerciseResult> saveExercises(@RequestBody List<Exercise> exercises) {
        List<Exercise> saved = exerciseRepository.saveAll(exercises);
        return ResponseEntity.ok(AiExerciseResult.builder()
                .generated(saved.size())
                .valid(saved.size())
                .errors(0)
                .exercises(saved)
                .errorDetails(List.of())
                .build());
    }

    @PostMapping("/validate")
    public ResponseEntity<AiValidateResult> validateExercises(@Valid @RequestBody AiValidateRequest request) {
        List<AiValidateResult.SchemaResult> schemaResults = new ArrayList<>();
        List<AiValidateResult.AiReview> aiReviews = new ArrayList<>();
        int validCount = 0;
        for (AiValidateRequest.ExerciseDraft draft : request.getExercises()) {
            Exercise ex = new Exercise();
            ex.setQuestion(draft.getQuestion());
            ex.setOptions(draft.getOptions());
            ex.setCorrectAnswer(draft.getCorrectAnswer());
            ex.setExplanation(draft.getExplanation());
            try {
                ex.setExerciseType(ExerciseType.valueOf(draft.getExerciseType().toUpperCase()));
            } catch (Exception e) {
                ex.setExerciseType(ExerciseType.MULTIPLE_CHOICE);
            }
            String error = aiExerciseService.validateSchema(ex);
            boolean isValid = error == null;
            if (isValid) validCount++;
            schemaResults.add(AiValidateResult.SchemaResult.builder()
                    .valid(isValid)
                    .question(draft.getQuestion())
                    .error(error)
                    .build());
            if (request.isUseAiReview() && isValid) {
                AiExerciseService.ReviewResult review = aiExerciseService.reviewExercise(ex);
                aiReviews.add(AiValidateResult.AiReview.builder()
                        .question(draft.getQuestion())
                        .passed(review.passed)
                        .reason(review.reason)
                        .build());
            }
        }
        return ResponseEntity.ok(AiValidateResult.builder()
                .total(request.getExercises().size())
                .valid(validCount)
                .invalid(request.getExercises().size() - validCount)
                .schemaResults(schemaResults)
                .aiReviews(aiReviews)
                .build());
    }

    @GetMapping("/status")
    public ResponseEntity<BatchGenerateStatus> getBatchStatus(@RequestParam(name = "batchId", required = false) String batchId) {
        AiExerciseService.BatchProgress progress = (batchId != null)
                ? aiExerciseService.getProgress(batchId)
                : aiExerciseService.getBatchProgress();
        if (progress == null) {
            return ResponseEntity.ok(BatchGenerateStatus.builder()
                    .running(false)
                    .totalLessons(0)
                    .processed(0)
                    .generated(0)
                    .errors(0)
                    .currentLesson("")
                    .batchId(batchId != null ? batchId : "")
                    .build());
        }
        return ResponseEntity.ok(BatchGenerateStatus.builder()
                .totalLessons(progress.totalLessons)
                .processed(progress.processed)
                .generated(progress.generated)
                .errors(progress.errors)
                .running(progress.running)
                .currentLesson(progress.currentLesson)
                .batchId(batchId != null ? batchId : "")
                .exercises(progress.exercises)
                .errorDetails(progress.errorDetails)
                .build());
    }
}
