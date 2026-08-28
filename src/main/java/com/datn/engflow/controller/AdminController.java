package com.datn.engflow.controller;

import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.model.dto.request.LessonRequest;
import com.datn.engflow.model.dto.response.AdminStatsDTO;
import com.datn.engflow.model.dto.response.AdminUserDTO;
import com.datn.engflow.model.dto.response.LessonSummaryDTO;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
/**
 * class AdminController.
 */
public class AdminController {

    private final AdminService adminService;

    // (Exercises moved to AdminExerciseController)

    // --- Stats ---
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // --- Users ---
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserDTO>> getAllUsers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(q, page, size));
    }

    @PutMapping("/users/{id}/toggle-active")
    public ResponseEntity<AdminUserDTO> toggleUserActive(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleUserActive(id));
    }

    @PutMapping("/users/{id}/toggle-admin")
    public ResponseEntity<AdminUserDTO> toggleUserAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleUserAdmin(id));
    }

    @PutMapping("/users/{id}/toggle-premium")
    public ResponseEntity<AdminUserDTO> toggleUserPremium(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleUserPremium(id));
    }

    @PutMapping("/users/{id}/revoke-premium")
    public ResponseEntity<AdminUserDTO> revokeUserPremium(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.revokeUserPremium(id));
    }

    // --- Lessons ---
    @GetMapping("/lessons")
    public ResponseEntity<Page<LessonSummaryDTO>> getAllLessons(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LessonLevel level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllLessonsAdmin(q, level, page, size));
    }

    @GetMapping("/lessons/{id}")
    public ResponseEntity<Lesson> getLesson(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getLesson(id));
    }

    @PostMapping("/lessons")
    public ResponseEntity<Lesson> createLesson(@Valid @RequestBody LessonRequest request) {
        return ResponseEntity.ok(adminService.createLesson(request));
    }

    @PutMapping("/lessons/{id}")
    public ResponseEntity<Lesson> updateLesson(@PathVariable Long id, @Valid @RequestBody LessonRequest request) {
        return ResponseEntity.ok(adminService.updateLesson(id, request));
    }

    @DeleteMapping("/lessons/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id) {
        adminService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/lessons/{id}/toggle-publish")
    public ResponseEntity<Lesson> toggleLessonPublish(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleLessonPublish(id));
    }

    // --- Vocabulary ---
    @GetMapping("/vocabulary")
    public ResponseEntity<Page<Vocabulary>> getAllVocabulary(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllVocabulary(page, size));
    }

    @PostMapping("/vocabulary")
    public ResponseEntity<Vocabulary> createVocabulary(@Valid @RequestBody VocabularyRequest request) {
        return ResponseEntity.ok(adminService.createVocabulary(request));
    }

    @PutMapping("/vocabulary/{id}")
    public ResponseEntity<Vocabulary> updateVocabulary(@PathVariable Long id, @Valid @RequestBody VocabularyRequest request) {
        return ResponseEntity.ok(adminService.updateVocabulary(id, request));
    }

    @DeleteMapping("/vocabulary/{id}")
    public ResponseEntity<Void> deleteVocabulary(@PathVariable Long id) {
        adminService.deleteVocabulary(id);
        return ResponseEntity.noContent().build();
    }
}
