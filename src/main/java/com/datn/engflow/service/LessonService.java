package com.datn.engflow.service;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.projection.LessonListProjection;
import com.datn.engflow.model.dto.request.LessonRequest;
import com.datn.engflow.model.dto.response.LessonListItemResponse;
import com.datn.engflow.model.dto.response.LessonResponse;
import com.datn.engflow.model.dto.response.VocabularyDTO;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.Progress;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.repository.ExerciseAttemptRepository;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.LessonSubmissionRepository;
import com.datn.engflow.repository.ProgressRepository;
import com.datn.engflow.repository.SpeakingPromptRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Bài học: đọc danh sách/chi tiết, và CRUD cho quản trị viên.
 *
 * <p>Tầng service, gọi bởi {@code LessonController} (đọc) và {@code AdminLessonController}
 * (tạo/sửa/xoá). Bài nháp ({@code is_published = false}) là nội dung soạn thảo:
 * {@link #assertLessonVisible} chặn mọi đường đọc public, và {@link #getLessonDetails}
 * áp lại đúng quy tắc đó sau khi nạp bản ghi.
 *
 * <p>Mọi DTO trả về đều kèm tiến độ của user đang đăng nhập; với khách vãng la
 * ({@code userEmail} null) trường tiến độ để mặc định chưa học, vì endpoint này
 * {@code permitAll}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final ExerciseRepository exerciseRepository;
    private final ProgressRepository progressRepository;
    private final LessonSubmissionRepository lessonSubmissionRepository;
    private final VocabularyRepository vocabularyRepository;
    private final SpeakingPromptRepository speakingPromptRepository;

    /**
     * Toàn bộ bài học đã publish, kèm tiến độ của user (nếu có).
     *
     * <p>Đường đọc không phân trang; {@link #getPublishedLessonPage} là bản có phân
     * trang dùng cho trang danh sách mới, và nó nạp qua projection nên không
     * kéo cột nội dung {@code NVARCHAR(MAX)}.
     *
     * @param userEmail email user đang đăng nhập, null nếu khách
     * @return danh sách bài học đã publish
     * @throws ResourceNotFoundException nếu {@code userEmail} không khớp user nào
     */
    @Transactional(readOnly = true)
    public List<LessonResponse> getAllLessons(String userEmail) {
        log.info("Lấy danh sách bài học cho user: {}", userEmail);

        List<Lesson> lessons = lessonRepository.findByIsPublishedTrueOrderByOrderIndexAsc();

        if (userEmail == null) {
            return lessons.stream().map(lesson -> LessonResponse.builder()
                    .id(lesson.getId())
                    .title(lesson.getTitle())
                    .description(lesson.getDescription())
                    .level(lesson.getLevel().name())
                    .category(lesson.getCategory())
                    .durationMinutes(lesson.getDurationMinutes())
                    .thumbnailUrl(lesson.getThumbnailUrl())
                    .audioUrl(lesson.getAudioUrl())
                    .orderIndex(lesson.getOrderIndex())
                    .isCompleted(false)
                    .completionPercentage(BigDecimal.ZERO)
                    .build()).collect(Collectors.toList());
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        List<Progress> userProgresses = progressRepository.findByUserId(user.getId());

        return lessons.stream().map(lesson -> {
            Optional<Progress> progressOpt = userProgresses.stream()
                    .filter(p -> p.getLesson().getId().equals(lesson.getId()))
                    .findFirst();

            return LessonResponse.builder()
                    .id(lesson.getId())
                    .title(lesson.getTitle())
                    .description(lesson.getDescription())
                    .level(lesson.getLevel().name())
                    .category(lesson.getCategory())
                    .durationMinutes(lesson.getDurationMinutes())
                    .thumbnailUrl(lesson.getThumbnailUrl())
                    .audioUrl(lesson.getAudioUrl())
                    .orderIndex(lesson.getOrderIndex())
                    .isCompleted(progressOpt.map(p -> Boolean.TRUE.equals(p.getIsCompleted())).orElse(false))
                    .completionPercentage(progressOpt.map(Progress::getCompletionPercentage).orElse(BigDecimal.ZERO))
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Trang bài học đã publish, lọc theo từ khoá và trình độ, kèm tiến độ từng bài.
     *
     * <p>Tiến độ nạp một lần cho cả trang bằng {@code findByUserIdAndLessonIdIn},
     * không query lặp theo từng dòng.
     *
     * @param userEmail email user đang đăng nhập, null nếu khách
     * @param keyword từ khoá tìm kiếm trong tiêu đề/mô tả; null hoặc rỗng thì không lọc
     * @param level trình độ lọc, null thì không lọc
     * @param pageable phân trang và sắp xếp
     * @return trang kết quả
     * @throws ResourceNotFoundException nếu {@code userEmail} không khớp user nào
     */
    @Transactional(readOnly = true)
    public Page<LessonListItemResponse> getPublishedLessonPage(String userEmail, String keyword, LessonLevel level, Pageable pageable) {
        // Use the lightweight projection to avoid hydrating NVARCHAR(MAX) content columns.
        Page<LessonListProjection> page = lessonRepository.findPublishedPageProjection(
                keyword != null && !keyword.isBlank() ? keyword.trim() : null,
                level,
                pageable);

        LessonListProjection lesson;
        if (userEmail == null) {
            return page.map(p -> LessonListItemResponse.builder()
                    .id(p.getId())
                    .title(p.getTitle())
                    .description(p.getDescription())
                    .level(p.getLevel())
                    .category(p.getCategory())
                    .durationMinutes(p.getDurationMinutes())
                    .thumbnailUrl(p.getThumbnailUrl())
                    .audioUrl(p.getAudioUrl())
                    .skillType(p.getSkillType())
                    .orderIndex(p.getOrderIndex())
                    .isCompleted(false)
                    .completionPercentage(BigDecimal.ZERO)
                    .build());
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        List<Long> lessonIds = page.getContent().stream().map(LessonListProjection::getId).toList();
        Map<Long, Progress> progressByLesson = lessonIds.isEmpty() ? Map.of()
                : progressRepository.findByUserIdAndLessonIdIn(user.getId(), lessonIds).stream()
                        .collect(Collectors.toMap(p -> p.getLesson().getId(), p -> p, (a, b) -> a));

        return page.map(p -> {
            Progress progress = progressByLesson.get(p.getId());
            return LessonListItemResponse.builder()
                    .id(p.getId())
                    .title(p.getTitle())
                    .description(p.getDescription())
                    .level(p.getLevel())
                    .category(p.getCategory())
                    .durationMinutes(p.getDurationMinutes())
                    .thumbnailUrl(p.getThumbnailUrl())
                    .audioUrl(p.getAudioUrl())
                    .skillType(p.getSkillType())
                    .orderIndex(p.getOrderIndex())
                    .isCompleted(progress != null && Boolean.TRUE.equals(progress.getIsCompleted()))
                    .completionPercentage(progress != null ? progress.getCompletionPercentage() : BigDecimal.ZERO)
                    .build();
        });
    }

    /**
     * audit-v8 F88: chan doc noi dung nhap (is_published=false) qua cac endpoint public.
     * Nem 404 thay vi 403 de khong tiet lo su ton tai cua ban nhap.
     *
     * @param lessonId id bai hoc
     * @param requesterIsAdmin true thi bo qua guard (admin can preview/review ban nhap)
     * @throws ResourceNotFoundException bai hoc khong ton tai, hoac la ban nhap voi nguoi thuong
     */
    @Transactional(readOnly = true)
    public void assertLessonVisible(Long lessonId, boolean requesterIsAdmin) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
        assertVisible(lesson, requesterIsAdmin);
    }

    /**
     * Luật hiển thị dùng chung: bài nháp chỉ admin đọc được, người khác nhận 404.
     *
     * @param lesson bài học đã nạp
     * @param requesterIsAdmin người gọi có quyền admin hay không
     * @throws ResourceNotFoundException nếu bài nháp và người gọi không phải admin
     */
    private void assertVisible(Lesson lesson, boolean requesterIsAdmin) {
        if (!requesterIsAdmin && !Boolean.TRUE.equals(lesson.getIsPublished())) {
            throw new ResourceNotFoundException("Lesson", "id", lesson.getId());
        }
    }

    /**
     * Nội dung đầy đủ của một bài học, kèm từ vựng và tiến độ của user.
     *
     * <p>CÓ ghi {@code Progress.lastAccessed} — đây là lý do hàm không
     * {@code readOnly}: mở bài học phải để lại dấu vết cho dashboard. Với khách vãng
     * la không tạo bản ghi tiến độ.
     *
     * @param lessonId id bài học
     * @param userEmail email user đang đăng nhập, null nếu khách
     * @param requesterIsAdmin admin được đọc cả bài nháp để xem trước
     * @return DTO chi tiết bài học
     * @throws ResourceNotFoundException nếu bài học không tồn tại, là bài nháp mà
     *         người gọi không phải admin, hoặc {@code userEmail} không khớp user nào
     */
    @Transactional
    public LessonResponse getLessonDetails(Long lessonId, String userEmail, boolean requesterIsAdmin) {
        log.info("Lấy chi tiết bài học: id={}, user={}", lessonId, userEmail);

        Lesson lesson = lessonRepository.findByIdWithDetails(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        // audit-v8 F88: detail la endpoint permitAll con list thi da loc isPublished=true — ban
        // nhap (is_published=false) phai 404 voi guest/student, chi admin duoc doc de preview.
        assertVisible(lesson, requesterIsAdmin);

        if (userEmail == null) {
            return LessonResponse.builder()
                    .id(lesson.getId())
                    .title(lesson.getTitle())
                    .description(lesson.getDescription())
                    .content(lesson.getContent())
                    .level(lesson.getLevel().name())
                    .category(lesson.getCategory())
                    .durationMinutes(lesson.getDurationMinutes())
                    .thumbnailUrl(lesson.getThumbnailUrl())
                    .audioUrl(lesson.getAudioUrl())
                    .orderIndex(lesson.getOrderIndex())
                    .isCompleted(false)
                    .completionPercentage(BigDecimal.ZERO)
                    .vocabularies(lesson.getVocabularies().stream().map(v -> VocabularyDTO.builder()
                            .id(v.getId()).word(v.getWord()).pronunciation(v.getPronunciation())
                            .meaning(v.getMeaning()).exampleSentence(v.getExampleSentence())
                            .audioUrl(v.getAudioUrl()).imageUrl(v.getImageUrl()).wordType(v.getWordType())
                            .build()).collect(Collectors.toList()))
                    .build();
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Progress progress = progressRepository.findByUserIdAndLessonId(user.getId(), lessonId)
                .orElseGet(() -> Progress.builder()
                        .user(user)
                        .lesson(lesson)
                        .completionPercentage(BigDecimal.ZERO)
                        .isCompleted(false)
                        .build());

        progress.setLastAccessed(LocalDateTime.now());
        progressRepository.save(progress);

        return LessonResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .content(lesson.getContent())
                .level(lesson.getLevel().name())
                .category(lesson.getCategory())
                .durationMinutes(lesson.getDurationMinutes())
                .thumbnailUrl(lesson.getThumbnailUrl())
                .audioUrl(lesson.getAudioUrl())
                .orderIndex(lesson.getOrderIndex())
                .isCompleted(Boolean.TRUE.equals(progress.getIsCompleted()))
                .completionPercentage(progress.getCompletionPercentage())
                .vocabularies(lesson.getVocabularies().stream().map(v -> VocabularyDTO.builder()
                        .id(v.getId()).word(v.getWord()).pronunciation(v.getPronunciation())
                        .meaning(v.getMeaning()).exampleSentence(v.getExampleSentence())
                        .audioUrl(v.getAudioUrl()).imageUrl(v.getImageUrl()).wordType(v.getWordType())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    /**
     * Tạo bài học mới từ request của quản trị viên.
     *
     * <p>{@code isPublished} bỏ trống thì mặc định publish; tiến độ trong DTO trả về
     * luôn là chưa học vì bài vừa tạo chưa ai học.
     *
     * @param lessonRequest dữ liệu bài học
     * @return DTO bài học vừa lưu
     */
    @Transactional
    public LessonResponse createLesson(LessonRequest lessonRequest) {
        log.info("Tạo bài học mới: title={}", lessonRequest.getTitle());
        Lesson lesson = Lesson.builder()
                .title(lessonRequest.getTitle())
                .description(lessonRequest.getDescription())
                .content(lessonRequest.getContent())
                .level(lessonRequest.getLevel())
                .category(lessonRequest.getCategory())
                .durationMinutes(lessonRequest.getDurationMinutes())
                .thumbnailUrl(lessonRequest.getThumbnailUrl())
                .audioUrl(lessonRequest.getAudioUrl())
                .orderIndex(lessonRequest.getOrderIndex())
                .isPublished(lessonRequest.getIsPublished() != null ? lessonRequest.getIsPublished() : true)
                .build();
        Lesson savedLesson = lessonRepository.save(lesson);
        return mapToResponse(savedLesson);
    }

    /**
     * Cập nhật bài học; {@code orderIndex} và {@code isPublished} chỉ ghi đè khi có
     * giá trị trong request, nên gửi một payload thiếu trường không xoá trạng thái cũ.
     *
     * @param id id bài học cần sửa
     * @param lessonRequest dữ liệu mới
     * @return DTO bài học sau cập nhật
     * @throws ResourceNotFoundException nếu bài học không tồn tại
     */
    @Transactional
    public LessonResponse updateLesson(Long id, LessonRequest lessonRequest) {
        log.info("Cập nhật bài học: id={}", id);
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));

        lesson.setTitle(lessonRequest.getTitle());
        lesson.setDescription(lessonRequest.getDescription());
        lesson.setContent(lessonRequest.getContent());
        lesson.setLevel(lessonRequest.getLevel());
        lesson.setCategory(lessonRequest.getCategory());
        lesson.setDurationMinutes(lessonRequest.getDurationMinutes());
        lesson.setThumbnailUrl(lessonRequest.getThumbnailUrl());
        lesson.setAudioUrl(lessonRequest.getAudioUrl());
        if (lessonRequest.getOrderIndex() != null) {
            lesson.setOrderIndex(lessonRequest.getOrderIndex());
        }
        if (lessonRequest.getIsPublished() != null) {
            lesson.setIsPublished(lessonRequest.getIsPublished());
        }

        Lesson updatedLesson = lessonRepository.save(lesson);
        return mapToResponse(updatedLesson);
    }

    /**
     * Ánh xạ entity bài học sang DTO dùng chung cho đường ghi của quản trị viên.
     *
     * @param lesson entity đã lưu
     * @return DTO với tiến độ mặc định chưa học
     */
    private LessonResponse mapToResponse(Lesson lesson) {
        return LessonResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .content(lesson.getContent())
                .level(lesson.getLevel() != null ? lesson.getLevel().name() : null)
                .category(lesson.getCategory())
                .durationMinutes(lesson.getDurationMinutes())
                .thumbnailUrl(lesson.getThumbnailUrl())
                .audioUrl(lesson.getAudioUrl())
                .orderIndex(lesson.getOrderIndex())
                .isCompleted(false)
                .completionPercentage(BigDecimal.ZERO)
                .build();
    }

    /**
     * Xoá bài học và toàn bộ dữ liệu con tham chiếu tới nó.
     *
     * <p>Thứ tự xoá là bắt buộc: các bảng con phải sạch trước khi xoá lesson, vì
     * khoá ngoại không khai {@code ON DELETE CASCADE}.
     *
     * @param id id bài học cần xoá
     * @throws ResourceNotFoundException nếu bài học không tồn tại
     */
    @Transactional
    public void deleteLesson(Long id) {
        log.info("Xóa bài học: id={}", id);
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        // Cascade child rows manually — DB FK has no ON DELETE CASCADE for all
        // child tables (ddl-auto=update won't retro-apply DDL cascade), so deleting
        // the lesson with attached child rows throws FK 547.
        //
        // audit-v15: the Lesson Builder "Đường B" tables (lesson_sections,
        // lesson_blocks, lesson_snapshots) were removed, so their cascade steps are
        // gone too. The remaining children still reference lessons with NO_ACTION.
        lessonSubmissionRepository.deleteByLessonId(id);
        progressRepository.deleteByLessonId(id);
        vocabularyRepository.deleteByLessonId(id);
        speakingPromptRepository.deleteByLessonId(id);
        exerciseAttemptRepository.deleteAllByLessonId(id);
        exerciseRepository.deleteAllByLessonId(id);
        lessonRepository.delete(lesson);
    }
}
