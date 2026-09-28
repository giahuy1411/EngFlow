package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.video.VideoDtos.GradeVideoAttemptRequest;
import com.datn.engflow.model.dto.video.VideoDtos.TranscriptLine;
import com.datn.engflow.model.dto.video.VideoDtos.VideoAttemptResponse;
import com.datn.engflow.model.dto.video.VideoDtos.VideoLessonDetail;
import com.datn.engflow.model.dto.video.VideoDtos.VideoLessonRequest;
import com.datn.engflow.model.dto.video.VideoDtos.VideoLessonSummary;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.VideoAttempt;
import com.datn.engflow.model.entity.VideoLesson;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.repository.VideoAttemptRepository;
import com.datn.engflow.repository.VideoLessonRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Backs the "learn English through video" flow: video lessons, their per-line subtitle
 * transcript, and the shadowing attempts learners record against a single line.
 *
 * <p>Layer: called by {@code VideoLessonController} for both the public and the
 * {@code /admin} route families. The transcript lives as JSON on the lesson row and is
 * validated on write; attempt audio goes to MinIO via {@link MinioService} and is played
 * back through a URL signed by {@code MediaSigner}. AI grading of an attempt lives
 * separately in {@link ShadowingAiGradingService}, which reads the transcript through
 * {@link #readTranscript}.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VideoLessonService {

    private static final Pattern YOUTUBE_ID = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|embed/|shorts/|live/)|youtu\\.be/)([A-Za-z0-9_-]{11})");
    private static final long MAX_ATTEMPT_BYTES = 20L * 1024 * 1024;
    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final VideoLessonRepository lessonRepository;
    private final VideoAttemptRepository attemptRepository;
    private final MinioService minioService;
    private final com.datn.engflow.security.MediaSigner mediaSigner;
    private final ObjectMapper objectMapper;

    // ---------------------------------------------------------------- queries

    /**
     * Public catalogue of published lessons, optionally narrowed to one level.
     *
     * @param level    trình độ lọc, null thì lấy mọi trình độ
     * @param pageable phân trang
     * @return trang tóm tắt bài học
     */
    @Transactional(readOnly = true)
    public Page<VideoLessonSummary> listPublished(LessonLevel level, Pageable pageable) {
        Page<VideoLesson> page = level == null
                ? lessonRepository.findByIsPublishedTrue(pageable)
                : lessonRepository.findByIsPublishedTrueAndLevel(level, pageable);
        return page.map(this::toSummary);
    }

    /**
     * Admin catalogue including drafts.
     *
     * @param pageable phân trang
     * @return trang tóm tắt bài học
     */
    @Transactional(readOnly = true)
    public Page<VideoLessonSummary> listAll(Pageable pageable) {
        return lessonRepository.findAll(pageable).map(this::toSummary);
    }

    /**
     * Full lesson payload for the player: transcript lines plus the line indexes this
     * user already shadowed. A draft lesson is reported as not found to anyone but an
     * admin, matching the published-only catalogue.
     *
     * @param id              mã bài học
     * @param userId          mã người xem, null nếu khách
     * @param requesterIsAdmin người xem có quyền admin hay không
     * @return chi tiết bài học kèm danh sách dòng đã hoàn thành
     * @throws ResourceNotFoundException nếu bài học không tồn tại, hoặc là bản nháp mà người xem không phải admin
     */
    @Transactional(readOnly = true)
    public VideoLessonDetail getDetail(Long id, Long userId, boolean requesterIsAdmin) {
        VideoLesson lesson = getLesson(id);
        // audit-v8 F88: list da loc isPublished=true thi detail phai khop — ban nhap
        // (is_published=false) khong duoc lo cho guest/student; admin van xem duoc de review.
        if (!Boolean.TRUE.equals(lesson.getIsPublished()) && !requesterIsAdmin) {
            throw new ResourceNotFoundException("VideoLesson", "id", id);
        }
        List<Integer> completed = userId == null
                ? List.of()
                : attemptRepository.findCompletedLineIndexes(userId, id);
        return new VideoLessonDetail(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getYoutubeVideoId(),
                lesson.getLevel().name(),
                lesson.getCategory(),
                lesson.getDurationSeconds(),
                readTranscript(lesson.getTranscriptJson()),
                completed);
    }

    // ------------------------------------------------------------ admin CRUD

    /**
     * Creates a lesson, deriving the YouTube id, validating the transcript and computing
     * the duration from it.
     *
     * @param request dữ liệu bài học từ form admin
     * @return tóm tắt bài học vừa tạo
     * @throws BadRequestException nếu link YouTube, trình độ hoặc transcript không hợp lệ
     */
    @Transactional
    public VideoLessonSummary create(VideoLessonRequest request) {
        VideoLesson lesson = new VideoLesson();
        apply(lesson, request);
        return toSummary(lessonRepository.save(lesson));
    }

    /**
     * Updates a lesson. An empty {@code category} or {@code transcript} in the request is
     * read as "keep the stored value" so the trimmed admin form cannot wipe them.
     *
     * @param id      mã bài học
     * @param request dữ liệu cập nhật
     * @return tóm tắt bài học sau khi lưu
     * @throws ResourceNotFoundException nếu bài học không tồn tại
     * @throws BadRequestException      nếu link YouTube, trình độ hoặc transcript không hợp lệ
     */
    @Transactional
    public VideoLessonSummary update(Long id, VideoLessonRequest request) {
        VideoLesson lesson = getLesson(id);
        // audit-v6 F21: the trimmed admin edit form drops `category` and starts
        // with an empty transcript box. Null/empty in those fields means "keep
        // current" on update — otherwise editing a title silently wipes the
        // category and forces re-pasting the whole transcript (verified live).
        if (request.category() == null) {
            request = new VideoLessonRequest(request.title(), request.description(), request.youtubeUrl(),
                    request.level(), lesson.getCategory(), request.transcript(), request.isPublished());
        }
        if (request.transcript() == null || request.transcript().isEmpty()) {
            request = new VideoLessonRequest(request.title(), request.description(), request.youtubeUrl(),
                    request.level(), request.category(), readTranscript(lesson.getTranscriptJson()), request.isPublished());
        }
        apply(lesson, request);
        return toSummary(lessonRepository.save(lesson));
    }

    /**
     * Deletes a lesson row. {@link VideoAttempt} has no owning collection on
     * {@link VideoLesson}, so there is no JPA cascade here — a lesson that still has
     * attempts is rejected by the {@code video_lesson_id} foreign key.
     *
     * @param id mã bài học
     * @throws ResourceNotFoundException nếu bài học không tồn tại
     */
    @Transactional
    public void delete(Long id) {
        lessonRepository.delete(getLesson(id));
    }

    /**
     * Copies a validated request onto the entity: title is trimmed, the YouTube URL is
     * reduced to its 11-character id, the transcript is stored as JSON, and the
     * duration is derived from the last transcript line rather than trusted from the
     * client.
     */
    private void apply(VideoLesson lesson, VideoLessonRequest request) {
        lesson.setTitle(request.title().trim());
        lesson.setDescription(request.description());
        lesson.setYoutubeVideoId(extractVideoId(request.youtubeUrl()));
        lesson.setLevel(parseLevel(request.level()));
        lesson.setCategory(request.category());
        lesson.setTranscriptJson(writeTranscript(validateTranscript(request.transcript())));
        lesson.setDurationSeconds(computeDuration(request.transcript()));
        lesson.setIsPublished(request.isPublished() == null || request.isPublished());
    }

    // ------------------------------------------------------------- attempts

    /**
     * Stores a shadowing recording for one transcript line and opens a SUBMITTED
     * attempt. The 20 MB ceiling and the {@code audio/} prefix check are the trust
     * boundary for the untrusted multipart body.
     *
     * @param lessonId  mã bài học
     * @param lineIndex chỉ số dòng phụ đề được shadowing
     * @param file      tệp ghi âm
     * @param user      người học đã xác thực
     * @return bài nộp ở trạng thái SUBMITTED kèm URL media đã ký
     * @throws ResourceNotFoundException nếu bài học không tồn tại
     * @throws BadRequestException      nếu dòng phụ đề không hợp lệ, tệp rỗng, quá 20 MB hoặc không phải audio
     */
    @Transactional
    public VideoAttemptResponse submitAttempt(Long lessonId, Integer lineIndex, MultipartFile file, User user) {
        VideoLesson lesson = getLesson(lessonId);
        List<TranscriptLine> transcript = readTranscript(lesson.getTranscriptJson());
        if (lineIndex == null || lineIndex < 0 || lineIndex >= transcript.size()) {
            throw new BadRequestException("Dòng phụ đề không hợp lệ");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Bản ghi âm không được để trống");
        }
        if (file.getSize() > MAX_ATTEMPT_BYTES) {
            throw new BadRequestException("Bản ghi âm vượt quá giới hạn 20 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new BadRequestException("Chỉ chấp nhận tệp âm thanh");
        }

        String objectKey = "video-attempts/lesson-" + lessonId + "/line-" + lineIndex + "/" + java.util.UUID.randomUUID();
        String stored = minioService.uploadMedia(objectKey, file);
        VideoAttempt attempt = VideoAttempt.builder()
                .user(user)
                .videoLesson(lesson)
                .lineIndex(lineIndex)
                .mediaObjectKey(stored == null || stored.isBlank() ? objectKey : stored)
                .mediaType(contentType)
                .status("SUBMITTED")
                .build();
        return toResponse(attemptRepository.save(attempt));
    }

    /**
     * Paged history of one learner's shadowing attempts.
     *
     * @param userId   mã người học
     * @param pageable phân trang
     * @return trang bài nộp của người học
     */
    @Transactional(readOnly = true)
    public Page<VideoAttemptResponse> listAttemptsForUser(Long userId, Pageable pageable) {
        return attemptRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    /**
     * Admin queue of attempts, optionally narrowed to one status (SUBMITTED is the
     * review worklist).
     *
     * @param status   trạng thái lọc, null/blank thì trả về tất cả
     * @param pageable phân trang
     * @return trang bài nộp cho quản trị viên
     */
    @Transactional(readOnly = true)
    public Page<VideoAttemptResponse> listAttemptsForAdmin(String status, Pageable pageable) {
        Page<VideoAttempt> page = status == null || status.isBlank()
                ? attemptRepository.findAll(pageable)
                : attemptRepository.findByStatus(status, pageable);
        return page.map(this::toResponse);
    }

    /**
     * Applies a teacher's manual grade to a shadowing attempt, marking it GRADED. The
     * grader is stored as a detached {@link User} holding only the id — no
     * {@code UserRepository} lookup, so the FK reference is written without loading the row.
     *
     * @param attemptId mã bài nộp
     * @param graderId  mã giáo viên chấm
     * @param request   điểm 0-10 và nhận xét
     * @return bài nộp sau khi chấm
     * @throws BadRequestException       nếu điểm ngoài khoảng 0-10
     * @throws ResourceNotFoundException nếu bài nộp không tồn tại
     */
    @Transactional
    public VideoAttemptResponse grade(Long attemptId, Long graderId, GradeVideoAttemptRequest request) {
        if (request.score() == null || request.score() < 0 || request.score() > 10) {
            throw new BadRequestException("Điểm phải nằm trong khoảng từ 0 đến 10");
        }
        VideoAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoAttempt", "id", attemptId));
        User grader = new User();
        grader.setId(graderId);
        attempt.setScore(BigDecimal.valueOf(request.score()));
        attempt.setAdminFeedback(request.feedback());
        attempt.setGradedBy(grader);
        attempt.setGradedAt(LocalDateTime.now());
        attempt.setStatus("GRADED");
        return toResponse(attemptRepository.save(attempt));
    }

    // -------------------------------------------------------------- helpers

    /**
     * Loads a lesson by id without any permission check; {@link #getDetail} applies the
     * published/admin rule on top of it.
     *
     * @param id mã bài học
     * @return bài học
     * @throws ResourceNotFoundException nếu bài học không tồn tại
     */
    public VideoLesson getLesson(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VideoLesson", "id", id));
    }

    /**
     * Extracts the 11-character YouTube id from a watch/embed/shorts/live URL, from a
     * {@code youtu.be} short link, or from a bare id the admin typed directly.
     *
     * @param url link YouTube hoặc video id thô
     * @return video id
     * @throws BadRequestException nếu thiếu link hoặc không khớp mẫu nào
     */
    static String extractVideoId(String url) {
        if (url == null) {
            throw new BadRequestException("Thiếu link YouTube");
        }
        String trimmed = url.trim();
        Matcher m = YOUTUBE_ID.matcher(trimmed);
        if (m.find()) {
            return m.group(1);
        }
        if (trimmed.matches("[A-Za-z0-9_-]{11}")) {
            return trimmed;
        }
        throw new BadRequestException("Không nhận diện được video ID từ link YouTube");
    }

    /**
     * Parses the admin form's free-text level into the enum, case-insensitively.
     *
     * @param level chuỗi trình độ, ví dụ {@code "beginner"}
     * @return trình độ tương ứng
     * @throws BadRequestException nếu không phải tên trình độ hợp lệ
     */
    static LessonLevel parseLevel(String level) {
        try {
            return LessonLevel.valueOf(level.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Trình độ không hợp lệ: " + level);
        }
    }

    /**
     * Rejects a transcript the player could not use: fewer than two lines (a lesson
     * needs at least one shadowable line plus context), a line with no English text, or
     * a non-positive duration.
     *
     * @param transcript danh sách dòng phụ đề, có thể null
     * @return chính danh sách đã truyền vào
     * @throws BadRequestException nếu transcript không hợp lệ
     */
    static List<TranscriptLine> validateTranscript(List<TranscriptLine> transcript) {
        if (transcript == null || transcript.size() < 2) {
            throw new BadRequestException("Transcript cần ít nhất 2 dòng");
        }
        for (TranscriptLine line : transcript) {
            if (line.textEn() == null || line.textEn().isBlank()) {
                throw new BadRequestException("Mỗi dòng cần có nội dung tiếng Anh");
            }
            if (line.end() <= line.start()) {
                throw new BadRequestException("Thời lượng dòng không hợp lệ: " + line.textEn());
            }
        }
        return transcript;
    }

    /** Length of the lesson in seconds: the {@code end} of the last transcript line, rounded up, 0 when empty. */
    static Integer computeDuration(List<TranscriptLine> transcript) {
        return transcript.stream()
                .mapToInt(line -> (int) Math.ceil(line.end()))
                .max()
                .orElse(0);
    }

    /**
     * Serializes a validated transcript for storage in {@code transcript_json}.
     *
     * @param transcript danh sách dòng phụ đề
     * @return chuỗi JSON
     * @throws BadRequestException nếu không serialize được
     */
    String writeTranscript(List<TranscriptLine> transcript) {
        try {
            return objectMapper.writeValueAsString(transcript);
        } catch (Exception ex) {
            throw new BadRequestException("Không thể lưu transcript");
        }
    }

    /**
     * Parses a stored {@code transcript_json}. Fails soft: a corrupt row yields an empty
     * list (and an error log) so one bad lesson cannot break the catalogue page.
     *
     * @param json chuỗi JSON đã lưu
     * @return danh sách dòng phụ đề, rỗng nếu dữ liệu hỏng
     */
    public List<TranscriptLine> readTranscript(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<TranscriptLine>>() {
            });
        } catch (Exception ex) {
            log.error("Corrupt transcript_json in video_lessons", ex);
            return List.of();
        }
    }

    /**
     * Projects a lesson into the list-row shape, including the transcript line count the
     * catalogue shows as "N câu".
     */
    private VideoLessonSummary toSummary(VideoLesson lesson) {
        return new VideoLessonSummary(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getYoutubeVideoId(),
                lesson.getLevel().name(),
                lesson.getCategory(),
                lesson.getDurationSeconds(),
                readTranscript(lesson.getTranscriptJson()).size(),
                lesson.getIsPublished());
    }

    /**
     * Projects an attempt into the response shape, swapping the stored object key for a
     * signed relative media URL and formatting the timestamp.
     */
    private VideoAttemptResponse toResponse(VideoAttempt attempt) {
        // audit-v6 F28: relative media URL — the frontend resolves it against
        // VITE_API_BASE_URL, so deploys behind another host/port (Tailscale
        // funnel) no longer get broken absolute localhost:8080 links.
        // audit-v7 F55: signed (exp+sig) so the private proxy accepts it.
        String mediaUrl = attempt.getMediaObjectKey() == null
                ? null
                : "/api/v1/media/" + attempt.getMediaObjectKey() + "?" + mediaSigner.paramsForObject(attempt.getMediaObjectKey());
        return new VideoAttemptResponse(
                attempt.getId(),
                attempt.getVideoLesson().getId(),
                attempt.getLineIndex(),
                attempt.getStatus(),
                attempt.getScore() == null ? null : attempt.getScore().doubleValue(),
                attempt.getAdminFeedback(),
                mediaUrl,
                attempt.getSubmittedAt() == null ? null : attempt.getSubmittedAt().format(TS));
    }
}
