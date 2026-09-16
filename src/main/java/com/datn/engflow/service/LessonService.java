package com.datn.engflow.service;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.projection.LessonListProjection;
import com.datn.engflow.model.dto.request.LessonRequest;
import com.datn.engflow.model.dto.response.LessonListItemResponse;
import com.datn.engflow.model.dto.response.LessonResponse;
import com.datn.engflow.model.dto.response.VocabularyDTO;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.LessonSection;
import com.datn.engflow.model.entity.Progress;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.repository.ExerciseAttemptRepository;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonBlockRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.LessonSectionRepository;
import com.datn.engflow.repository.LessonSnapshotRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final ExerciseRepository exerciseRepository;
    private final ProgressRepository progressRepository;
    private final LessonSnapshotRepository lessonSnapshotRepository;
    private final LessonSectionRepository lessonSectionRepository;
    private final LessonBlockRepository lessonBlockRepository;
    private final LessonSubmissionRepository lessonSubmissionRepository;
    private final VocabularyRepository vocabularyRepository;
    private final SpeakingPromptRepository speakingPromptRepository;

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

    private void assertVisible(Lesson lesson, boolean requesterIsAdmin) {
        if (!requesterIsAdmin && !Boolean.TRUE.equals(lesson.getIsPublished())) {
            throw new ResourceNotFoundException("Lesson", "id", lesson.getId());
        }
    }

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

    @Transactional
    public void deleteLesson(Long id) {
        log.info("Xóa bài học: id={}", id);
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        // Cascade child rows manually — DB FK has no ON DELETE CASCADE for all
        // child tables (ddl-auto=update won't retro-apply DDL cascade), so deleting
        // the lesson with attached child rows throws FK 547. Order matters:
        // blocks reference sections, sections reference lessons.
        List<LessonSection> sections = lessonSectionRepository.findByLessonIdOrderByOrderIndexAsc(id);
        for (LessonSection section : sections) {
            lessonBlockRepository.deleteBySectionId(section.getId());
        }
        lessonSectionRepository.deleteByLessonId(id);
        lessonSnapshotRepository.deleteByLessonId(id);
        lessonSubmissionRepository.deleteByLessonId(id);
        progressRepository.deleteByLessonId(id);
        vocabularyRepository.deleteByLessonId(id);
        speakingPromptRepository.deleteByLessonId(id);
        exerciseAttemptRepository.deleteAllByLessonId(id);
        exerciseRepository.deleteAllByLessonId(id);
        lessonRepository.delete(lesson);
    }
}
