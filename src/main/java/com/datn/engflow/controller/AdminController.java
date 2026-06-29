package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.AchievementRequest;
import com.datn.engflow.model.dto.request.LessonRequest;
import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.model.dto.response.AdminStatsDTO;
import com.datn.engflow.model.dto.response.AdminUserDTO;
import com.datn.engflow.model.entity.*;
import com.datn.engflow.service.AdminService;
import com.datn.engflow.service.LessonSubmissionService;
import com.datn.engflow.model.dto.request.GradeSubmissionRequest;
import com.datn.engflow.model.dto.response.LessonSubmissionDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final LessonSubmissionService lessonSubmissionService;

    // --- Stats ---
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // --- Users ---
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{id}/toggle-active")
    public ResponseEntity<AdminUserDTO> toggleUserActive(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleUserActive(id));
    }

    @PutMapping("/users/{id}/toggle-admin")
    public ResponseEntity<AdminUserDTO> toggleUserAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleUserAdmin(id));
    }

    // --- Lessons ---
    @GetMapping("/lessons")
    public ResponseEntity<List<Lesson>> getAllLessons() {
        return ResponseEntity.ok(adminService.getAllLessonsAdmin());
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
    public ResponseEntity<List<Vocabulary>> getAllVocabulary() {
        return ResponseEntity.ok(adminService.getAllVocabulary());
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

    // (Exercises removed)
    // --- Achievements ---
    @GetMapping("/achievements")
    public ResponseEntity<List<Achievement>> getAllAchievements() {
        return ResponseEntity.ok(adminService.getAllAchievements());
    }

    @PostMapping("/achievements")
    public ResponseEntity<Achievement> createAchievement(@Valid @RequestBody AchievementRequest request) {
        return ResponseEntity.ok(adminService.createAchievement(request));
    }

    @PutMapping("/achievements/{id}")
    public ResponseEntity<Achievement> updateAchievement(@PathVariable Long id, @Valid @RequestBody AchievementRequest request) {
        return ResponseEntity.ok(adminService.updateAchievement(id, request));
    }

    @DeleteMapping("/achievements/{id}")
    public ResponseEntity<Void> deleteAchievement(@PathVariable Long id) {
        adminService.deleteAchievement(id);
        return ResponseEntity.noContent().build();
    }

    // --- Submissions ---
    @GetMapping("/submissions")
    public ResponseEntity<List<LessonSubmissionDTO>> getAllSubmissions() {
        return ResponseEntity.ok(lessonSubmissionService.getAllSubmissionsForAdmin());
    }

    @PutMapping("/submissions/{id}/grade")
    public ResponseEntity<LessonSubmissionDTO> gradeSubmission(@PathVariable Long id,
                                                               @Valid @RequestBody GradeSubmissionRequest request) {
        return ResponseEntity.ok(lessonSubmissionService.gradeSubmission(id, request.getScore(), request.getFeedback()));
    }
}
