package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.ExerciseSubmitRequest;
import com.datn.engflow.model.entity.ExerciseSubmission;
import com.datn.engflow.service.ExerciseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.datn.engflow.model.dto.response.ExerciseSubmissionDTO;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitExercise(@Valid @RequestBody ExerciseSubmitRequest submitRequest,
                                                              Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        String email = authentication.getName();
        boolean isCorrect = exerciseService.submitAnswer(email, submitRequest);
        
        Map<String, Object> response = new HashMap<>();
        response.put("isCorrect", isCorrect);
        
        return ResponseEntity.ok(response);
    }
    @GetMapping("/submissions")
    public ResponseEntity<List<ExerciseSubmissionDTO>> getMySubmissions(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        String email = authentication.getName();
        List<ExerciseSubmissionDTO> submissions = exerciseService.getExerciseSubmissions(email);
        return ResponseEntity.ok(submissions);
    }
}
