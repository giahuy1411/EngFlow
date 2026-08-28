package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.ExerciseRequest;
import com.datn.engflow.model.dto.response.ExerciseResponse;
import com.datn.engflow.service.ExerciseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/exercises")
@RequiredArgsConstructor
/**
 * class AdminExerciseController.
 */
public class AdminExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ExerciseResponse>> getAllExercises(
            @RequestParam(required = false) Long lessonId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(exerciseService.getAdminExercisePage(lessonId, type, difficulty, q,
                PageRequest.of(Math.max(page, 0), size, Sort.by("orderIndex").ascending().and(Sort.by("id")))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExerciseResponse> getExercise(@PathVariable Long id) {
        return ResponseEntity.ok(exerciseService.getExercise(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExerciseResponse> createExercise(@Valid @RequestBody ExerciseRequest request) {
        return ResponseEntity.ok(exerciseService.createExercise(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExerciseResponse> updateExercise(@PathVariable Long id,
                                                            @RequestBody ExerciseRequest request) {
        return ResponseEntity.ok(exerciseService.updateExercise(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteExercise(@PathVariable Long id) {
        exerciseService.deleteExercise(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Exercise deleted successfully");
        return ResponseEntity.ok(resp);
    }
}
