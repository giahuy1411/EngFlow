package com.datn.engflow.service;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.LessonSubmissionRequest;
import com.datn.engflow.model.dto.response.LessonSubmissionDTO;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.LessonSubmission;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.enums.SkillType;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.LessonSubmissionRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.security.SafeUploadNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * class LessonSubmissionService.
 */
public class LessonSubmissionService {

    private final LessonSubmissionRepository lessonSubmissionRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;

    @Transactional
    public LessonSubmissionDTO submitLessonSkill(LessonSubmissionRequest request, String userEmail) {
        log.info("Lưu bài nộp bài học: user={}, lessonId={}, skill={}", userEmail, request.getLessonId(), request.getSkillType());

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", request.getLessonId()));

        // Check if there is an existing submission for this user, lesson, and skill
        Optional<LessonSubmission> existingOpt = lessonSubmissionRepository
                .findByUserIdAndLessonIdAndSkillType(user.getId(), lesson.getId(), request.getSkillType());

        LessonSubmission submission = existingOpt
                .map(existing -> {
                    existing.setSubmissionText(request.getSubmissionText());
                    existing.setAudioUrl(request.getAudioUrl());
                    existing.setScore(null);
                    existing.setFeedback(null);
                    existing.setStatus("PENDING");
                    return existing;
                })
                .orElseGet(() -> LessonSubmission.builder()
                        .user(user)
                        .lesson(lesson)
                        .skillType(request.getSkillType())
                        .submissionText(request.getSubmissionText())
                        .audioUrl(request.getAudioUrl())
                        .status("PENDING")
                        .build());

        LessonSubmission saved = lessonSubmissionRepository.save(submission);
        return convertToDTO(saved);
    }

    @Transactional(readOnly = true)
    public LessonSubmissionDTO getLessonSkillSubmission(Long lessonId, SkillType skillType, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        LessonSubmission submission = lessonSubmissionRepository
                .findByUserIdAndLessonIdAndSkillType(user.getId(), lessonId, skillType)
                .orElseThrow(() -> new ResourceNotFoundException("LessonSubmission", "lessonId/skillType", lessonId + "/" + skillType));

        return convertToDTO(submission);
    }


    public String saveAudioFile(MultipartFile file) {
        log.info("Lưu file âm thanh được tải lên: {}", file.getOriginalFilename());
        
        // Ensure folder 'uploads' exists
        File directory = new File("uploads");
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                log.info("Đã tạo thư mục uploads/");
            }
        }

        // audit-v8 F81: the returned path is served by /api/resources/** to any
        // visitor, so the recorder upload must not be able to carry a browser-active
        // extension. Default to .webm only when the client sent no name at all.
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            originalFilename = "recording.webm";
        }
        String extension = "." + SafeUploadNames.extensionOf(originalFilename);

        String uniqueFileName = UUID.randomUUID().toString() + extension;
        Path targetPath = Paths.get("uploads", uniqueFileName);

        try {
            Files.write(targetPath, file.getBytes());
            log.info("File đã được lưu tại: {}", targetPath.toAbsolutePath());
            
            // Return URL mapped to static resources WebConfig
            return "/api/resources/" + uniqueFileName;
        } catch (IOException e) {
            log.error("Lỗi khi lưu file ghi âm: {}", e.getMessage());
            throw new RuntimeException("Không thể lưu file ghi âm trên server", e);
        }
    }

    private LessonSubmissionDTO convertToDTO(LessonSubmission s) {
        return LessonSubmissionDTO.builder()
                .id(s.getId())
                .userId(s.getUser().getId())
                .username(s.getUser().getUsername())
                .fullName(s.getUser().getFullName())
                .lessonId(s.getLesson().getId())
                .lessonTitle(s.getLesson().getTitle())
                .skillType(s.getSkillType().name())
                .submissionText(s.getSubmissionText())
                .audioUrl(s.getAudioUrl())
                .score(s.getScore())
                .feedback(s.getFeedback())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
