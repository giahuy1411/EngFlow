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

/**
 * Owns the speaking-attempt lifecycle: media upload, automated assessment, manual
 * grading, and signed playback URLs.
 *
 * <p>Layer: called by {@code SpeakingSubmissionController} for both the
 * {@code /speaking-submissions} and legacy {@code /video-submissions} route families.
 * Uploaded media goes to MinIO via {@link MinioService} and is stored as a relative
 * object key; reads are re-signed on the fly by {@code MediaSigner} so the private proxy
 * accepts them. Grading runs through {@link SpeakingAssessmentService} (Whisper +
 * alignment + LLM rubric) and a completed assessment records a study day through
 * {@link StudyActivityService}.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
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
    private final StudyActivityService studyActivityService;

    /**
     * Stores a learner's recording and opens a SUBMITTED row for it. Nothing is
     * transcribed here; that happens later in {@link #assessSubmission}.
     *
     * <p>The 50 MB ceiling and the {@code audio/}+{@code video/} prefix check are the
     * trust boundary for the upload — the multipart body is untrusted input, and MinIO
     * has no per-request size limit of its own.</p>
     *
     * @param file     tệp media do người học tải lên
     * @param promptId mã đề nói
     * @param user     người học đã xác thực
     * @return bài nộp ở trạng thái SUBMITTED
     * @throws BadRequestException nếu tệp rỗng, quá 50 MB, sai định dạng, hoặc không tìm thấy đề bài
     */
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
        if (submission.getStatus() == SpeakingSubmissionStatus.GRADED
                || submission.getStatus() == SpeakingSubmissionStatus.COMPLETED) {
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
        if (outcome.rubric() != null && outcome.transcript() != null && !outcome.transcript().isBlank()) {
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
        SpeakingSubmission saved = repository.save(submission);
        if (saved.getStatus() == SpeakingSubmissionStatus.COMPLETED) {
            studyActivityService.recordStudy(saved.getUser().getId());
        }
        return saved;
    }

    /**
     * Serializes the Whisper-alignment numbers into the JSON blob stored on
     * {@code pronunciationDetailsJson}; the admin console renders it read-only.
     *
     * @param outcome kết quả chấm tự động
     * @return chuỗi JSON chi tiết, hoặc null nếu serialize lỗi
     */
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

    /**
     * Clamps a message to a column width. The error text can come from a third-party
     * pipeline (Whisper sidecar, LLM) and would otherwise overflow the DB column.
     *
     * @param value chuỗi cắt, có thể null
     * @param max   độ dài tối đa
     * @return chuỗi đã cắt, hoặc null nếu đầu vào null
     */
    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    /**
     * Paged history of one learner's attempts, newest first by id.
     *
     * @param userId   mã người học
     * @param pageable phân trang
     * @return trang bài nộp của người học
     */
    public Page<SpeakingSubmission> getUserSubmissions(Long userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable);
    }

    /**
     * Paged history of one learner's attempts on a single prompt.
     *
     * @param userId   mã người học
     * @param promptId mã đề nói
     * @param pageable phân trang
     * @return trang bài nộp lọc theo đề
     */
    public Page<SpeakingSubmission> getUserPromptSubmissions(Long userId, Long promptId, Pageable pageable) {
        return repository.findByUserIdAndPromptId(userId, promptId, pageable);
    }

    /**
     * Admin queue, optionally narrowed to one status (SUBMITTED is the review worklist).
     *
     * @param status   trạng thái lọc, null thì trả về tất cả
     * @param pageable phân trang
     * @return trang bài nộp cho quản trị viên
     */
    public Page<SpeakingSubmission> getAllSubmissionsForAdmin(SpeakingSubmissionStatus status, Pageable pageable) {
        return status == null
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
    }

    /**
     * Unpaged submissions for one prompt, most recently submitted first — used by the
     * admin detail panel of a prompt.
     *
     * @param promptId mã đề nói
     * @return danh sách bài nộp của đề đó
     */
    public List<SpeakingSubmission> getPromptSubmissions(Long promptId) {
        return repository.findByPromptIdOrderBySubmittedAtDesc(promptId);
    }

    /**
     * Applies a teacher's manual grade, overwriting any AI scores and marking the row
     * GRADED. The admin flag is re-checked here, not only in the controller, so the
     * check holds for any future caller.
     *
     * @param submissionId mã bài nộp
     * @param graderId     mã giáo viên chấm
     * @param request      điểm, nhận xét công khai và ghi chú riêng
     * @return bài nộp sau khi chấm
     * @throws BadRequestException          nếu điểm ngoài 0-10 hoặc thiếu nhận xét
     * @throws ResourceNotFoundException    nếu giáo viên hoặc bài nộp không tồn tại
     * @throws org.springframework.security.access.AccessDeniedException nếu grader không phải admin
     */
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

    /**
     * Builds the URL the frontend uses to play a recording: a relative, signed
     * {@code /api/v1/media/...} link. Rows predating the object-key column fall back to
     * the stored {@code videoUrl}, signed only when it still points at MinIO.
     *
     * @param submission bài nộp cần nghe
     * @return URL tương đối đã ký, hoặc null nếu bài nộp không có media
     */
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

    /** Relative, exp+sig signed media URL the private proxy accepts. */
    private String signedMediaUrl(String objectKey) {
        return "/api/v1/media/" + objectKey + "?" + mediaSigner.paramsForObject(objectKey);
    }

    /**
     * Recovers the object key from a legacy absolute MinIO URL, stripping the
     * {@code video-uploads/} prefix older rows prepended. URLs without that prefix are
     * returned unchanged.
     *
     * @param storedUrl URL đã lưu, có thể null
     * @return object key tương ứng
     */
    private static String extractObjectKey(String storedUrl) {
        if (storedUrl == null) return "";
        int idx = storedUrl.indexOf("video-uploads/");
        if (idx >= 0) return storedUrl.substring(idx + "video-uploads/".length());
        return storedUrl;
    }

    /**
     * Loads a submission for a viewer, allowing the owner or an admin only.
     *
     * @param submissionId mã bài nộp
     * @param viewerId     mã người xem
     * @param isAdmin      người xem có quyền admin hay không
     * @return bài nộp
     * @throws ResourceNotFoundException nếu bài nộp không tồn tại
     * @throws AccessDeniedException    nếu người xem không phải chủ bài và không phải admin
     */
    public SpeakingSubmission getSubmissionForViewer(Long submissionId, Long viewerId, boolean isAdmin) {
        SpeakingSubmission submission = getSubmission(submissionId);
        if (!isAdmin && !submission.getUser().getId().equals(viewerId)) {
            throw new AccessDeniedException("Bạn không có quyền xem bài nộp này");
        }
        return submission;
    }

    /**
     * Loads a submission by id without any permission check; access-controlled callers
     * should go through {@link #getSubmissionForViewer}.
     *
     * @param id mã bài nộp
     * @return bài nộp
     * @throws ResourceNotFoundException nếu bài nộp không tồn tại
     */
    public SpeakingSubmission getSubmission(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SpeakingSubmission", "id", id));
    }
}
