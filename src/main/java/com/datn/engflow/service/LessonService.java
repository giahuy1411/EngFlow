package com.datn.engflow.service;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.LessonRequest;
import com.datn.engflow.model.dto.response.ExerciseDTO;
import com.datn.engflow.model.dto.response.GrammarDTO;
import com.datn.engflow.model.dto.response.LessonResponse;
import com.datn.engflow.model.dto.response.VocabularyDTO;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.Progress;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.ProgressRepository;
import com.datn.engflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final ProgressRepository progressRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "lessons", key = "#userEmail != null ? #userEmail : 'anonymous'")
    public List<LessonResponse> getAllLessons(String userEmail) {
        log.info("L\u1ea5y danh s\u00e1ch b\u00e0i h\u1ecdc cho user: {}", userEmail);

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

    @Transactional
    public LessonResponse getLessonDetails(Long lessonId, String userEmail) {
        log.info("L\u1ea5y chi ti\u1ebft b\u00e0i h\u1ecdc: id={}, user={}", lessonId, userEmail);

        Lesson lesson = lessonRepository.findByIdWithDetails(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

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
                    .grammars(lesson.getGrammars().stream().map(g -> GrammarDTO.builder()
                            .id(g.getId()).title(g.getTitle()).explanation(g.getExplanation())
                            .formula(g.getFormula()).examples(g.getExamples()).level(g.getLevel())
                            .build()).collect(Collectors.toList()))
                    .exercises(lesson.getExercises().stream().map(e -> ExerciseDTO.builder()
                            .id(e.getId()).title(e.getTitle()).question(e.getQuestion())
                            .exerciseType(e.getExerciseType()).options(e.getOptions())
                            .points(e.getPoints()).difficulty(e.getDifficulty()).audioUrl(e.getAudioUrl())
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
                .grammars(lesson.getGrammars().stream().map(g -> GrammarDTO.builder()
                        .id(g.getId()).title(g.getTitle()).explanation(g.getExplanation())
                        .formula(g.getFormula()).examples(g.getExamples()).level(g.getLevel())
                        .build()).collect(Collectors.toList()))
                .exercises(lesson.getExercises().stream().map(e -> ExerciseDTO.builder()
                        .id(e.getId()).title(e.getTitle()).question(e.getQuestion())
                        .exerciseType(e.getExerciseType()).options(e.getOptions())
                        .points(e.getPoints()).difficulty(e.getDifficulty()).audioUrl(e.getAudioUrl())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    @Transactional
    @CacheEvict(value = "lessons", allEntries = true)
    public LessonResponse createLesson(LessonRequest lessonRequest) {
        log.info("T\u1ea1o b\u00e0i h\u1ecdc m\u1edbi: title={}", lessonRequest.getTitle());
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
    @CacheEvict(value = "lessons", allEntries = true)
    public LessonResponse updateLesson(Long id, LessonRequest lessonRequest) {
        log.info("C\u1eadp nh\u1eadt b\u00e0i h\u1ecdc: id={}", id);
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
    @CacheEvict(value = "lessons", allEntries = true)
    public void deleteLesson(Long id) {
        log.info("X\u00f3a b\u00e0i h\u1ecdc: id={}", id);
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        lessonRepository.delete(lesson);
    }
}
