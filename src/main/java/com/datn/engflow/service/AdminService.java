package com.datn.engflow.service;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.model.dto.request.LessonRequest;
import com.datn.engflow.model.dto.response.AdminStatsDTO;
import com.datn.engflow.model.dto.response.AdminUserDTO;
import com.datn.engflow.model.dto.response.LessonSummaryDTO;
import com.datn.engflow.model.entity.*;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.model.enums.SkillType;
import com.datn.engflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * class AdminService.
 */
public class AdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final VocabularyRepository vocabularyRepository;
    private final ExerciseRepository exerciseRepository;
    private final LessonSubmissionRepository lessonSubmissionRepository;
    private final LessonService lessonService;
    private final Clock clock;
    private final StreakService streakService;

    public AdminStatsDTO getDashboardStats() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long totalUsers = userRepository.count();
        long totalLessons = lessonRepository.count();
        long totalVocabulary = vocabularyRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long recentUsers = userRepository.countByLastStudyDateAfter(sevenDaysAgo.toLocalDate());
        long totalExercises = exerciseRepository.count();
        long totalSubmissions = lessonSubmissionRepository.count();

        return AdminStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalLessons(totalLessons)
                .totalVocabulary(totalVocabulary)
                .activeUsers(activeUsers)
                .recentUsers(recentUsers)
                .totalExercises(totalExercises)
                .totalSubmissions(totalSubmissions)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDTO> getAllUsers(String keyword, int page, int size) {
        Pageable pageable = adminPageRequest(page, size, Sort.by("createdAt").descending().and(Sort.by("id")));
        Page<User> users = (keyword == null || keyword.isBlank())
                ? userRepository.findAll(pageable)
                : userRepository.searchByKeywordPage(keyword.trim(), pageable);
        // Streak cho cả trang trong MỘT query: gọi getCurrentStreak theo từng row là
        // N+1 (mỗi call đọc study_days một lần). Trang rỗng thì không chạm tầng streak.
        List<Long> userIds = users.getContent().stream().map(User::getId).toList();
        Map<Long, Integer> streaks = userIds.isEmpty() ? Map.of() : streakService.currentStreaks(userIds);
        return users.map(u -> mapToAdminUserDTO(u, streaks));
    }

    @Transactional
    public AdminUserDTO toggleUserPremium(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        boolean currentlyPremium = Boolean.TRUE.equals(user.getIsPremium());
        if (currentlyPremium) {
            user.setIsPremium(false);
            user.setPremiumExpiry(null);
        } else {
            user.setIsPremium(true);
            user.setPremiumExpiry(LocalDate.now(clock).plusDays(30));
        }
        User saved = userRepository.save(user);
        return mapToAdminUserDTO(saved);
    }

    @Transactional
    public AdminUserDTO revokeUserPremium(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setIsPremium(false);
        user.setPremiumExpiry(null);
        User saved = userRepository.save(user);
        return mapToAdminUserDTO(saved);
    }

    @Transactional
    public AdminUserDTO toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        User saved = userRepository.save(user);
        return mapToAdminUserDTO(saved);
    }

    @Transactional
    public AdminUserDTO toggleUserAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setIsAdmin(!Boolean.TRUE.equals(user.getIsAdmin()));
        User saved = userRepository.save(user);
        return mapToAdminUserDTO(saved);
    }

    /** Đường một-user (4 method toggle*) — đọc streak trực tiếp, không cần batch. */
    private AdminUserDTO mapToAdminUserDTO(User u) {
        return mapToAdminUserDTO(u, Map.of());
    }

    /**
     * @param streaks kết quả batch cho cả trang; user vắng mặt (chưa từng học) nhận 0.
     *                Với đường một-user, map rỗng nên phải đọc trực tiếp.
     */
    private AdminUserDTO mapToAdminUserDTO(User u, Map<Long, Integer> streaks) {
        Integer currentStreak = streaks.isEmpty()
                ? streakService.getCurrentStreak(u.getId())
                : streaks.getOrDefault(u.getId(), 0);
        return AdminUserDTO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .isAdmin(u.getIsAdmin())
                .isActive(u.getIsActive())
                .totalPoints(u.getTotalPoints())
                .currentStreak(currentStreak)
                .createdAt(u.getCreatedAt())
                .isPremium(Boolean.TRUE.equals(u.getIsPremium()))
                .premiumExpiry(u.getPremiumExpiry())
                .build();
    }

    // Lessons
    public Page<LessonSummaryDTO> getAllLessonsAdmin(String keyword, int page, int size) {
        return getAllLessonsAdmin(keyword, null, page, size);
    }

    public Page<LessonSummaryDTO> getAllLessonsAdmin(String keyword, LessonLevel level, int page, int size) {
        Pageable pageable = adminPageRequest(page, size, Sort.by("orderIndex").ascending().and(Sort.by("id")));
        return getAllLessonsAdmin(keyword, level, pageable);
    }

    public Page<LessonSummaryDTO> getAllLessonsAdmin(String keyword, Pageable pageable) {
        return getAllLessonsAdmin(keyword, null, pageable);
    }

    public Page<LessonSummaryDTO> getAllLessonsAdmin(String keyword, LessonLevel level, Pageable pageable) {
        Page<Lesson> lessons = lessonRepository.findAdminPage(
                (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                level,
                pageable);
        return lessons.map(this::toSummary);
    }

    private LessonSummaryDTO toSummary(Lesson lesson) {
        return LessonSummaryDTO.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .level(lesson.getLevel())
                .category(lesson.getCategory())
                .durationMinutes(lesson.getDurationMinutes())
                .thumbnailUrl(lesson.getThumbnailUrl())
                .skillType(lesson.getSkillType())
                .orderIndex(lesson.getOrderIndex())
                .isPublished(lesson.getIsPublished())
                .createdAt(lesson.getCreatedAt())
                .build();
    }

    public Lesson getLesson(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
    }

    @Transactional
    public Lesson createLesson(LessonRequest request) {
        Lesson lesson = Lesson.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .content(request.getContent())
                .level(request.getLevel())
                .category(request.getCategory())
                .durationMinutes(request.getDurationMinutes())
                .thumbnailUrl(request.getThumbnailUrl())
                .audioUrl(request.getAudioUrl())
                .orderIndex(request.getOrderIndex())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .skillType(request.getSkillType() != null ? SkillType.valueOf(request.getSkillType().toUpperCase()) : SkillType.GRAMMAR)
                .build();
        return lessonRepository.save(lesson);
    }

    @Transactional
    public Lesson updateLesson(Long id, LessonRequest request) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        lesson.setTitle(request.getTitle());
        lesson.setDescription(request.getDescription());
        lesson.setContent(request.getContent());
        lesson.setLevel(request.getLevel());
        lesson.setCategory(request.getCategory());
        lesson.setDurationMinutes(request.getDurationMinutes());
        lesson.setThumbnailUrl(request.getThumbnailUrl());
        lesson.setAudioUrl(request.getAudioUrl());
        lesson.setOrderIndex(request.getOrderIndex());
        if (request.getIsPublished() != null) {
            lesson.setIsPublished(request.getIsPublished());
        }
        return lessonRepository.save(lesson);
    }

    @Transactional
    public void deleteLesson(Long id) {
        lessonService.deleteLesson(id);
    }

    @Transactional
    public Lesson toggleLessonPublish(Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        lesson.setIsPublished(!Boolean.TRUE.equals(lesson.getIsPublished()));
        return lessonRepository.save(lesson);
    }

    // Vocabulary
    public Page<Vocabulary> getAllVocabulary(int page, int size) {
        Pageable pageable = adminPageRequest(page, size, Sort.by("id").ascending());
        return vocabularyRepository.findAll(pageable);
    }

    @Transactional
    public Vocabulary createVocabulary(VocabularyRequest request) {
        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        }
        Vocabulary vocabulary = Vocabulary.builder()
                .word(request.getWord())
                .pronunciation(request.getPronunciation())
                .meaning(request.getMeaning())
                .definitionEn(request.getDefinitionEn())
                .exampleSentence(request.getExampleSentence())
                .wordType(request.getWordType())
                .cefrLevel(request.getCefrLevel())
                .source(request.getSource())
                .audioUrl(request.getAudioUrl())
                .imageUrl(request.getImageUrl())
                .lesson(lesson)
                .build();
        return vocabularyRepository.save(vocabulary);
    }

    @Transactional
    public Vocabulary updateVocabulary(Long id, VocabularyRequest request) {
        Vocabulary vocabulary = vocabularyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vocabulary", "id", id));
        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        }
        vocabulary.setWord(request.getWord());
        vocabulary.setPronunciation(request.getPronunciation());
        vocabulary.setMeaning(request.getMeaning());
        vocabulary.setDefinitionEn(request.getDefinitionEn());
        vocabulary.setExampleSentence(request.getExampleSentence());
        vocabulary.setWordType(request.getWordType());
        vocabulary.setCefrLevel(request.getCefrLevel());
        vocabulary.setSource(request.getSource());
        vocabulary.setAudioUrl(request.getAudioUrl());
        vocabulary.setImageUrl(request.getImageUrl());
        vocabulary.setLesson(lesson);
        return vocabularyRepository.save(vocabulary);
    }

    @Transactional
    public void deleteVocabulary(Long id) {
        Vocabulary vocabulary = vocabularyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vocabulary", "id", id));
        vocabularyRepository.delete(vocabulary);
    }

    private Pageable adminPageRequest(int page, int size, Sort sort) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, sort);
    }

    // (Exercises section removed)
}
