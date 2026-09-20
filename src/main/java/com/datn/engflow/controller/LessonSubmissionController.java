package com.datn.engflow.controller;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.LessonSubmissionRequest;
import com.datn.engflow.model.dto.response.LessonSubmissionDTO;
import com.datn.engflow.model.enums.SkillType;
import com.datn.engflow.service.LessonService;
import com.datn.engflow.service.LessonSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/lesson-submissions")
@RequiredArgsConstructor
/**
 * class LessonSubmissionController.
 */
public class LessonSubmissionController {

    private final LessonSubmissionService lessonSubmissionService;
    private final LessonService lessonService;

    /**
     * audit-v10 F115: bài nháp phải vô hình với người thường trên MỌI đường chạm
     * tới nó. F88 chặn đường đọc, F105 chặn đường chấm/nộp bài tập; đường nộp bài
     * kỹ năng (writing/speaking/reading) còn hở nên phải dùng cùng guard.
     */
    private static boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @PostMapping("/submit")
    public ResponseEntity<LessonSubmissionDTO> submitLessonSkill(@Valid @RequestBody LessonSubmissionRequest request,
                                                                 Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        lessonService.assertLessonVisible(request.getLessonId(), isAdmin(authentication));
        String email = authentication.getName();
        LessonSubmissionDTO dto = lessonSubmissionService.submitLessonSkill(request, email);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/upload-audio")
    public ResponseEntity<Map<String, String>> uploadAudio(@RequestParam("file") MultipartFile file,
                                                           Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        if (file.isEmpty()) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "File ghi âm trống.");
            return ResponseEntity.badRequest().body(err);
        }

        String audioUrl = lessonSubmissionService.saveAudioFile(file);
        Map<String, String> res = new HashMap<>();
        res.put("audioUrl", audioUrl);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/my/lesson/{lessonId}/skill/{skillType}")
    public ResponseEntity<LessonSubmissionDTO> getMySubmission(@PathVariable Long lessonId,
                                                               @PathVariable SkillType skillType,
                                                               Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // audit-v10 F115: cùng guard — bài nháp không được lộ qua bài đã nộp.
        lessonService.assertLessonVisible(lessonId, isAdmin(authentication));
        String email = authentication.getName();
        try {
            LessonSubmissionDTO dto = lessonSubmissionService.getLessonSkillSubmission(lessonId, skillType, email);
            return ResponseEntity.ok(dto);
        } catch (ResourceNotFoundException e) {
            // Return 200 OK with null if no submission exists yet
            return ResponseEntity.ok(null);
        }
    }
}
