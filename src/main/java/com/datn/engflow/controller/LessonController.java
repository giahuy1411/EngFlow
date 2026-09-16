package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.LessonRequest;
import com.datn.engflow.model.dto.response.LessonListItemResponse;
import com.datn.engflow.model.dto.response.LessonResponse;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
/**
 * class LessonController.
 */
public class LessonController {

    private final LessonService lessonService;

    @GetMapping
    public ResponseEntity<Page<LessonListItemResponse>> getAllLessons(
            Authentication authentication,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LessonLevel level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        String email = authentication != null ? authentication.getName() : null;
        size = Math.min(Math.max(size, 1), 100);
        Page<LessonListItemResponse> lessons = lessonService.getPublishedLessonPage(
                email, q, level, PageRequest.of(Math.max(page, 0), size, Sort.by("orderIndex").ascending().and(Sort.by("id"))));
        return ResponseEntity.ok(lessons);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LessonResponse> getLessonDetails(@PathVariable Long id, Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        // audit-v8 F88: admin duoc doc ban nhap qua endpoint public de preview.
        boolean isAdmin = authentication != null && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        LessonResponse lesson = lessonService.getLessonDetails(id, email, isAdmin);
        return ResponseEntity.ok(lesson);
    }

    @PostMapping
    public ResponseEntity<LessonResponse> createLesson(@Valid @RequestBody LessonRequest lessonRequest, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LessonResponse created = lessonService.createLesson(lessonRequest);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LessonResponse> updateLesson(@PathVariable Long id, @Valid @RequestBody LessonRequest lessonRequest, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LessonResponse updated = lessonService.updateLesson(id, lessonRequest);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        lessonService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }
}
