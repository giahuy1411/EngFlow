package com.datn.engflow.controller;
import com.datn.engflow.service.ExerciseSeedService;
import com.datn.engflow.service.ExerciseSeedService.SeedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/exercises")
@RequiredArgsConstructor
/**
 * class AdminExerciseSeedController.
 */
public class AdminExerciseSeedController {

    private final ExerciseSeedService exerciseSeedService;

    /**
     * Parse ALL lessons' HTML content and generate exercises from them.
     * @param force if true, deletes all existing exercises first before re-seeding
     */
    @PostMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeedResult> seedAllExercises(
            @RequestParam(defaultValue = "false") boolean force) {
        SeedResult result = exerciseSeedService.seedAllLessons(force);
        return ResponseEntity.ok(result);
    }
}
