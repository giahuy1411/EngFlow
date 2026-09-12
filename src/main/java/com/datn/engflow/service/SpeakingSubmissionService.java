package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.GradeSpeakingSubmissionRequest;
import com.datn.engflow.model.entity.SpeakingSubmissionStatus;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.SpeakingPrompt;
import com.datn.engflow.model.entity.SpeakingSubmission;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.SpeakingPromptRepository;
import com.datn.engflow.repository.SpeakingSubmissionRepository;
import com.datn.engflow.service.assessment.SpeakingAssessmentOutcome;
import com.datn.engflow.service.assessment.SpeakingAssessmentService;
import com.datn.engflow.service.assessment.TranscriptAlignmentMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
/**
 * class SpeakingSubmissionService.
 */
public class SpeakingSubmissionService {

    private static final long MAX_SPEAKING_MEDIA_BYTES = 50L * 1024 * 1024;
    private static final String UPLOAD_DIRECTORY = "speaking-submissions/";
    private static final List<String> ALLOWED_MEDIA_TYPES = List.of("audio/", "video/");

    private final SpeakingSubmissionRepository repository;
    private final com.datn.engflow.security.MediaSigner mediaSigner;
    private final SpeakingPromptRepository promptRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final SpeakingAssessmentService assessmentService;
    private final ObjectMapper objectMapper;

    @Transactional
    public SpeakingSubmission uploadSubmission(MultipartFile file, Long promptId, User user) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Tệp ghi âm không được để trống");
        }
        if (file.getSize() > MAX_SPEAKING_MEDIA_BYTES) {
            throw new BadRequestException("Tệp ghi âm vượt quá giới hạn 50 MB");
        }
        if (file.getContentType() == null || ALLOWED_MEDIA_TYPES.stream().noneMatch(t -> file.getContentType().startsWith(t))) {
            throw new BadRequestException("Định dạng media không được hỗ trợ");
        }

        SpeakingPrompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đề bài"));

        String objectKey = UPLOAD_DIRECTORY + UUID.randomUUID() + "_" + file.getOriginalFilename();
        String storedObjectKey = minioService.uploadMedia(objectKey, file);
        if (storedObjectKey == null || storedObjectKey.isBlank()) {
            storedObjectKey = objectKey;
        }

        SpeakingSubmission submission = SpeakingSubmission.builder()
                .user(user)
                .prompt(prompt)
                .videoUrl(storedObjectKey)
                .mediaObjectKey(storedObjectKey)
                .mediaType(file.getContentType())
                .status(SpeakingSubmissionStatus.SUBMITTED)
                .build();
        return repository.save(submission);
    }

    /**
     * Runs the automatic assessment pipeline for one submission.
     *
     * <p>The submission moves through PROCESSING and lands on COMPLETED when a rubric
     * is produced, or FAILED when transcription or the LLM is unavailable. Manual
     * grading remains available in every state.</p>
     *
     * @param submissionId submission to assess
     * @return the assessed submission
     */
    @Transactional
    public SpeakingSubmission assessSubmission(Long submissionId) {
        SpeakingSubmission submission = getSubmission(submissionId);
        if (submission.getStatus() == SpeakingSubmissionStatus.GRADED) {
            return submission;
        }
        submission.setStatus(SpeakingSubmissionStatus.PROCESSING);
        repository.save(submission);

        SpeakingAssessmentOutcome outcome;
        try {
            outcome = assessmentService.assess(submission);
        } catch (Exception ex) {
            log.error("Assessment pipeline crashed for submission {}", submissionId, ex);
            submission.setStatus(SpeakingSubmissionStatus.FAILED);
            submission.setAssessmentError(truncate("Lỗi hệ thống: " + ex.getMessage(), 500));
            return repository.save(submission);
        }

        submission.setTranscript(outcome.transcript());
        submission.setAssessmentProvider(outcome.provider());
        if (outcome.alignment() != null) {
            submission.setPronunciationCompleteness(outcome.alignment().coveragePercent());
            submission.setPronunciationDetailsJson(buildDetailsJson(outcome));
        }
        if (outcome.rubric() != null) {
            submission.setScoreGrammar(outcome.rubric().grammar());
            submission.setScoreVocabulary(outcome.rubric().vocabulary());
            submission.setScoreFluency(outcome.rubric().fluency());
            submission.setScoreTotal(outcome.rubric().total());
            submission.setFeedback(outcome.rubric().feedback());
            submission.setStatus(SpeakingSubmissionStatus.COMPLETED);
        } else {
            submission.setStatus(SpeakingSubmissionStatus.FAILED);
        }
        submission.setAssessmentError(truncate(outcome.error(), 500));
        return repository.save(submission);
    }

    private String buildDetailsJson(SpeakingAssessmentOutcome outcome) {
        TranscriptAlignmentMetrics.AlignmentResult alignment = outcome.alignment();
        ObjectNode node = objectMapper.createObjectNode();
        node.put("transcriptSource", outcome.transcriptSource());
        node.put("wordErrorRate", alignment.wordErrorRatePercent());
        node.put("coverage", alignment.coveragePercent());
        node.put("correctWords", alignment.correctWords());
        node.put("substitutions", alignment.substitutions());
        node.put("insertions", alignment.insertions());
        node.put("deletions", alignment.deletions());
        node.put("hypothesisWords", alignment.hypothesisWords());
        node.put("note", "Độ phủ nội dung so với bài mẫu; chưa phải điểm phát âm");
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            log.warn("Could not serialize alignment details", ex);
            return null;
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public Page<SpeakingSubmission> getUserSubmissions(Long userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable);
    }

    public Page<SpeakingSubmission> getUserPromptSubmissions(Long userId, Long promptId, Pageable pageable) {
        return repository.findByUserIdAndPromptId(userId, promptId, pageable);
    }

    public Page<SpeakingSubmission> getAllSubmissionsForAdmin(SpeakingSubmissionStatus status, Pageable pageable) {
        return status == null
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
    }

    public List<SpeakingSubmission> getPromptSubmissions(Long promptId) {
        return repository.findByPromptIdOrderBySubmittedAtDesc(promptId);
    }

    @Transactional
    public SpeakingSubmission gradeSubmission(Long submissionId, Long graderId, GradeSpeakingSubmissionRequest request) {
        if (request.score() == null || request.score().compareTo(BigDecimal.ZERO) < 0
                || request.score().compareTo(BigDecimal.TEN) > 0) {
            throw new BadRequestException("Điểm phải nằm trong khoảng từ 0 đến 10");
        }
        if (request.feedback() == null || request.feedback().isBlank()) {
            throw new BadRequestException("Nhận xét không được để trống");
        }

        User grader = userRepository.findById(graderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", graderId));
        if (!Boolean.TRUE.equals(grader.getIsAdmin())) {
            throw new AccessDeniedException("Chỉ quản trị viên được chấm bài");
        }

        SpeakingSubmission submission = getSubmission(submissionId);
        submission.setScore(request.score());
        submission.setAdminFeedback(request.feedback() != null ? request.feedback().trim() : null);
        submission.setPrivateNote(request.privateNote() != null ? request.privateNote().trim() : null);
        submission.setGradedBy(grader);
        submission.setGradedAt(LocalDateTime.now());
        submission.setStatus(SpeakingSubmissionStatus.GRADED);
        return repository.save(submission);
    }

    public String createMediaReadUrl(SpeakingSubmission submission) {
        // audit-v6 F28: relative media URL — frontend resolves against API base
        // audit-v7 F55: signed so the proxy accepts it (see MediaSigner).
        if (submission.getMediaObjectKey() == null) {
            if (submission.getVideoUrl() != null) {
                return submission.getVideoUrl().contains("minio:9000")
                        ? signedMediaUrl(extractObjectKey(submission.getVideoUrl()))
                        : submission.getVideoUrl();
            }
            return null;
        }
        return signedMediaUrl(submission.getMediaObjectKey());
    }

    private String signedMediaUrl(String objectKey) {
        return "/api/v1/media/" + objectKey + "?" + mediaSigner.paramsForObject(objectKey);
    }

    private static String extractObjectKey(String storedUrl) {
        if (storedUrl == null) return "";
        int idx = storedUrl.indexOf("video-uploads/");
        if (idx >= 0) return storedUrl.substring(idx + "video-uploads/".length());
        return storedUrl;
    }

    public SpeakingSubmission getSubmissionForViewer(Long submissionId, Long viewerId, boolean isAdmin) {
        SpeakingSubmission submission = getSubmission(submissionId);
        if (!isAdmin && !submission.getUser().getId().equals(viewerId)) {
            throw new AccessDeniedException("Bạn không có quyền xem bài nộp này");
        }
        return submission;
    }

    public SpeakingSubmission getSubmission(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SpeakingSubmission", "id", id));
    }
}
