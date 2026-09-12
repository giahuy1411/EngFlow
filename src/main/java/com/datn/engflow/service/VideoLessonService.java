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

@Service
@Slf4j
@RequiredArgsConstructor
/**
 * class VideoLessonService.
 */
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

    @Transactional(readOnly = true)
    public Page<VideoLessonSummary> listPublished(LessonLevel level, Pageable pageable) {
        Page<VideoLesson> page = level == null
                ? lessonRepository.findByIsPublishedTrue(pageable)
                : lessonRepository.findByIsPublishedTrueAndLevel(level, pageable);
        return page.map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public Page<VideoLessonSummary> listAll(Pageable pageable) {
        return lessonRepository.findAll(pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public VideoLessonDetail getDetail(Long id, Long userId) {
        VideoLesson lesson = getLesson(id);
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

    @Transactional
    public VideoLessonSummary create(VideoLessonRequest request) {
        VideoLesson lesson = new VideoLesson();
        apply(lesson, request);
        return toSummary(lessonRepository.save(lesson));
    }

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

    @Transactional
    public void delete(Long id) {
        lessonRepository.delete(getLesson(id));
    }

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

    @Transactional(readOnly = true)
    public Page<VideoAttemptResponse> listAttemptsForUser(Long userId, Pageable pageable) {
        return attemptRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<VideoAttemptResponse> listAttemptsForAdmin(String status, Pageable pageable) {
        Page<VideoAttempt> page = status == null || status.isBlank()
                ? attemptRepository.findAll(pageable)
                : attemptRepository.findByStatus(status, pageable);
        return page.map(this::toResponse);
    }

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

    public VideoLesson getLesson(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VideoLesson", "id", id));
    }

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

    static LessonLevel parseLevel(String level) {
        try {
            return LessonLevel.valueOf(level.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Trình độ không hợp lệ: " + level);
        }
    }

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

    static Integer computeDuration(List<TranscriptLine> transcript) {
        return transcript.stream()
                .mapToInt(line -> (int) Math.ceil(line.end()))
                .max()
                .orElse(0);
    }

    String writeTranscript(List<TranscriptLine> transcript) {
        try {
            return objectMapper.writeValueAsString(transcript);
        } catch (Exception ex) {
            throw new BadRequestException("Không thể lưu transcript");
        }
    }

    public List<TranscriptLine> readTranscript(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<TranscriptLine>>() {
            });
        } catch (Exception ex) {
            log.error("Corrupt transcript_json in video_lessons", ex);
            return List.of();
        }
    }

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
