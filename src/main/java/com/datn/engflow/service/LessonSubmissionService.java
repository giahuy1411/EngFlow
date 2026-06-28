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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
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

        LessonSubmission submission;
        if (existingOpt.isPresent()) {
            submission = existingOpt.get();
            // Update submission content and reset grading details
            submission.setSubmissionText(request.getSubmissionText());
            submission.setAudioUrl(request.getAudioUrl());
            submission.setScore(null);
            submission.setFeedback(null);
            submission.setStatus("PENDING");
        } else {
            submission = LessonSubmission.builder()
                    .user(user)
                    .lesson(lesson)
                    .skillType(request.getSkillType())
                    .submissionText(request.getSubmissionText())
                    .audioUrl(request.getAudioUrl())
                    .status("PENDING")
                    .build();
        }

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

    @Transactional(readOnly = true)
    public List<LessonSubmissionDTO> getAllSubmissionsForAdmin() {
        log.info("Admin lấy danh sách bài nộp để chấm điểm");
        return lessonSubmissionRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public LessonSubmissionDTO gradeSubmission(Long submissionId, Double score, String feedback) {
        log.info("Admin chấm điểm bài nộp id={}, score={}, feedback={}", submissionId, score, feedback);

        LessonSubmission submission = lessonSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("LessonSubmission", "id", submissionId));

        submission.setScore(score);
        submission.setFeedback(feedback);
        submission.setStatus("GRADED");

        LessonSubmission saved = lessonSubmissionRepository.save(submission);
        return convertToDTO(saved);
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

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            extension = ".webm"; // Default format for media recorder in browsers
        }

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
