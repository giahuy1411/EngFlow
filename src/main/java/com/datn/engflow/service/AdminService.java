package com.datn.engflow.service;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.AchievementRequest;
import com.datn.engflow.model.dto.request.ExerciseRequest;
import com.datn.engflow.model.dto.request.LessonRequest;
import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.model.dto.response.AdminStatsDTO;
import com.datn.engflow.model.enums.SkillType;
import com.datn.engflow.model.dto.response.AdminUserDTO;
import com.datn.engflow.model.entity.*;
import com.datn.engflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final VocabularyRepository vocabularyRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseSubmissionRepository submissionRepository;
    private final AchievementRepository achievementRepository;

    public AdminStatsDTO getDashboardStats() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long totalUsers = userRepository.count();
        long totalLessons = lessonRepository.count();
        long totalVocabulary = vocabularyRepository.count();
        long totalExercises = exerciseRepository.count();
        long totalSubmissions = submissionRepository.count();
        long totalAchievements = achievementRepository.count();
        
        List<User> users = userRepository.findAll();
        long activeUsers = users.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
        long recentUsers = users.stream().filter(u -> u.getLastStudyDate() != null && u.getLastStudyDate().isAfter(sevenDaysAgo.toLocalDate())).count();

        return AdminStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalLessons(totalLessons)
                .totalVocabulary(totalVocabulary)
                .totalExercises(totalExercises)
                .totalSubmissions(totalSubmissions)
                .totalAchievements(totalAchievements)
                .activeUsers(activeUsers)
                .recentUsers(recentUsers)
                .build();
    }

    public List<AdminUserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(u -> AdminUserDTO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .isAdmin(u.getIsAdmin())
                .isActive(u.getIsActive())
                .totalPoints(u.getTotalPoints())
                .currentStreak(u.getCurrentStreak())
                .createdAt(u.getCreatedAt())
                .build()).collect(Collectors.toList());
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

    private AdminUserDTO mapToAdminUserDTO(User u) {
        return AdminUserDTO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .isAdmin(u.getIsAdmin())
                .isActive(u.getIsActive())
                .totalPoints(u.getTotalPoints())
                .currentStreak(u.getCurrentStreak())
                .createdAt(u.getCreatedAt())
                .build();
    }

    // Lessons
    public List<Lesson> getAllLessonsAdmin() {
        return lessonRepository.findAll();
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
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        lessonRepository.delete(lesson);
    }

    @Transactional
    public Lesson toggleLessonPublish(Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
        lesson.setIsPublished(!Boolean.TRUE.equals(lesson.getIsPublished()));
        return lessonRepository.save(lesson);
    }

    // Vocabulary
    public List<Vocabulary> getAllVocabulary() {
        return vocabularyRepository.findAll();
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

    // Exercises
    public List<Exercise> getAllExercises() {
        return exerciseRepository.findAll();
    }

    @Transactional
    public Exercise createExercise(ExerciseRequest request) {
        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        }
        Exercise exercise = Exercise.builder()
                .lesson(lesson)
                .title(request.getTitle())
                .question(request.getQuestion())
                .options(request.getOptions())
                .correctAnswer(request.getCorrectAnswer())
                .explanation(request.getExplanation())
                .exerciseType(com.datn.engflow.model.enums.ExerciseType.valueOf(request.getExerciseType().toUpperCase()))
                .difficulty(request.getDifficulty() != null ? request.getDifficulty().toUpperCase() : null)
                .points(request.getPoints() != null ? request.getPoints() : 10)
                .audioUrl(request.getAudioUrl())
                .imageUrl(request.getImageUrl())
                .build();
        return exerciseRepository.save(exercise);
    }

    @Transactional
    public Exercise updateExercise(Long id, ExerciseRequest request) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", "id", id));
        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        }
        exercise.setLesson(lesson);
        exercise.setTitle(request.getTitle());
        exercise.setQuestion(request.getQuestion());
        exercise.setOptions(request.getOptions());
        exercise.setCorrectAnswer(request.getCorrectAnswer());
        exercise.setExplanation(request.getExplanation());
        exercise.setExerciseType(com.datn.engflow.model.enums.ExerciseType.valueOf(request.getExerciseType().toUpperCase()));
        exercise.setDifficulty(request.getDifficulty() != null ? request.getDifficulty().toUpperCase() : null);
        if (request.getPoints() != null) exercise.setPoints(request.getPoints());
        exercise.setAudioUrl(request.getAudioUrl());
        exercise.setImageUrl(request.getImageUrl());
        return exerciseRepository.save(exercise);
    }

    @Transactional
    public void deleteExercise(Long id) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", "id", id));
        exerciseRepository.delete(exercise);
    }

    // Achievements
    public List<Achievement> getAllAchievements() {
        return achievementRepository.findAll();
    }

    @Transactional
    public Achievement createAchievement(AchievementRequest request) {
        Achievement achievement = Achievement.builder()
                .name(request.getName())
                .description(request.getDescription())
                .badgeType(request.getBadgeType())
                .iconUrl(request.getIconUrl())
                .pointsRequired(request.getPointsRequired() != null ? request.getPointsRequired() : 0)
                .build();
        return achievementRepository.save(achievement);
    }

    @Transactional
    public Achievement updateAchievement(Long id, AchievementRequest request) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement", "id", id));
        achievement.setName(request.getName());
        achievement.setDescription(request.getDescription());
        achievement.setBadgeType(request.getBadgeType());
        achievement.setIconUrl(request.getIconUrl());
        if (request.getPointsRequired() != null) achievement.setPointsRequired(request.getPointsRequired());
        return achievementRepository.save(achievement);
    }

    @Transactional
    public void deleteAchievement(Long id) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement", "id", id));
        achievementRepository.delete(achievement);
    }
}
