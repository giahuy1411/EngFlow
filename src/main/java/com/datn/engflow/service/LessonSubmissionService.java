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
 * Nhận và tra cứu bài nộp kỹ năng của người học cho một bài học (viết/nói...), kèm lưu file ghi âm.
 *
 * <p>Tầng service, được gọi từ {@code LessonSubmissionController}. Lưu bài nộp qua
 * {@link LessonSubmissionRepository}; bài nộp mới/được nộp lại luôn ở trạng thái
 * {@code PENDING} để admin chấm tay. File ghi âm được ghi xuống thư mục {@code uploads/}
 * trên đĩa cục bộ rồi phục vụ qua {@code /api/resources/**}.
 */
public class LessonSubmissionService {

    private final LessonSubmissionRepository lessonSubmissionRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;

    /**
     * Nộp (hoặc nộp lại) bài của một kỹ năng cho một bài học.
     *
     * <p>Nếu đã có bài nộp cùng user/lesson/skill, bài cũ được cập nhật tại chỗ và điểm/feedback
     * cũ bị xóa, đưa về {@code PENDING} để chấm lại. Ngược lại tạo bản ghi mới.
     *
     * @param request nội dung bài nộp (lessonId, skillType, submissionText, audioUrl)
     * @param userEmail email người nộp
     * @return DTO bài nộp đã lưu
     * @throws ResourceNotFoundException nếu không tìm thấy user hoặc bài học
     */
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

    /**
     * Đọc bài nộp của chính người gọi cho một bài học và kỹ năng.
     *
     * @param lessonId id bài học
     * @param skillType kỹ năng của bài nộp
     * @param userEmail email người nộp
     * @return DTO bài nộp
     * @throws ResourceNotFoundException nếu không tìm thấy user, hoặc chưa có bài nộp phù hợp
     */
    @Transactional(readOnly = true)
    public LessonSubmissionDTO getLessonSkillSubmission(Long lessonId, SkillType skillType, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        LessonSubmission submission = lessonSubmissionRepository
                .findByUserIdAndLessonIdAndSkillType(user.getId(), lessonId, skillType)
                .orElseThrow(() -> new ResourceNotFoundException("LessonSubmission", "lessonId/skillType", lessonId + "/" + skillType));

        return convertToDTO(submission);
    }


    /**
     * Lưu file ghi âm tải lên thư mục {@code uploads/} cục bộ và trả URL phục vụ tĩnh.
     *
     * <p>Tên file được đổi thành UUID + phần mở rộng đã lọc qua {@link SafeUploadNames} vì URL
     * trả về được phục vụ cho mọi khách qua {@code /api/resources/**}, không được mang đuôi
     * thực thi được trên trình duyệt.
     *
     * @param file file ghi âm client gửi lên
     * @return đường dẫn tương đối dạng {@code /api/resources/<uuid>.<ext>}
     * @throws RuntimeException nếu ghi file xuống đĩa thất bại
     */
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
