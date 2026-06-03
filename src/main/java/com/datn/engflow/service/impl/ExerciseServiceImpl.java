package com.datn.engflow.service.impl;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.ExerciseSubmitRequest;
import com.datn.engflow.model.entity.*;
import com.datn.engflow.repository.*;
import com.datn.engflow.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.datn.engflow.model.dto.response.ExerciseSubmissionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final ExerciseSubmissionRepository submissionRepository;
    private final ProgressRepository progressRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;

    @Override
    @Transactional
    @CacheEvict(value = "lessons", key = "#userEmail")
    public boolean submitAnswer(String userEmail, ExerciseSubmitRequest request) {
        try {
            return submitAnswerInternal(userEmail, request);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent submission detected for user {} and exercise {}", userEmail, request.getExerciseId());
            return false;
        }
    }

    private boolean submitAnswerInternal(String userEmail, ExerciseSubmitRequest request) {
        log.info("User {} nộp bài tập: id={}, answer={}", userEmail, request.getExerciseId(), request.getUserAnswer());

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Exercise exercise = exerciseRepository.findById(request.getExerciseId())
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", "id", request.getExerciseId()));

        // Compare answer
        boolean isCorrect = false;
        if (exercise.getExerciseType() == com.datn.engflow.model.enums.ExerciseType.FILL_IN_BLANK) {
            String normalizedUserAns = request.getUserAnswer().replaceAll("\\s+", " ").trim().toLowerCase();
            String correctAnswer = exercise.getCorrectAnswer();
            String normalizedCorrectAns = correctAnswer != null ? correctAnswer.replaceAll("\\s+", " ").trim().toLowerCase() : "";
            if (normalizedUserAns.equals(normalizedCorrectAns)) {
                isCorrect = true;
            } else if (exercise.getOptions() != null && !exercise.getOptions().trim().isEmpty()) {
                try {
                    java.util.List<String> validAnswers = objectMapper.readValue(exercise.getOptions(), new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {});
                    for (String valid : validAnswers) {
                        if (normalizedUserAns.equals(valid.replaceAll("\\s+", " ").trim().toLowerCase())) {
                            isCorrect = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to parse options for FILL_IN_BLANK exercise id={}: {}", exercise.getId(), e.getMessage(), e);
                }
            }
        } else {
            String normalizedUserAns = request.getUserAnswer().trim().toLowerCase();
            String correctAnswer = exercise.getCorrectAnswer();
            if (correctAnswer != null) {
                String normalizedCorrectAns = correctAnswer.trim().toLowerCase();
                if (normalizedUserAns.equals(normalizedCorrectAns)) {
                    isCorrect = true;
                }
            }
        }

        // Check if there is a previous correct submission for this exercise by the user (avoid duplicate points farming)
        Optional<ExerciseSubmission> previousSubmissionOpt = submissionRepository
                .findByUserIdAndExerciseId(user.getId(), exercise.getId());

        boolean alreadyAwarded = previousSubmissionOpt.map(ExerciseSubmission::getIsCorrect).orElse(false);

        int pointsEarned = 0;
        if (isCorrect && !alreadyAwarded) {
            pointsEarned = exercise.getPoints();
        }

        // Create or update submission
        ExerciseSubmission submission = previousSubmissionOpt.orElse(new ExerciseSubmission());
        submission.setUser(user);
        submission.setExercise(exercise);
        submission.setUserAnswer(request.getUserAnswer());
        submission.setIsCorrect(isCorrect);
        submission.setPointsEarned(pointsEarned);
        submissionRepository.save(submission);

        if (pointsEarned > 0) {
            // Update user points
            user.setTotalPoints(user.getTotalPoints() + pointsEarned);
            log.info("Cộng {} điểm cho user id={}. Tổng điểm hiện tại: {}", pointsEarned, user.getId(), user.getTotalPoints());

            // Update streak
            updateStreak(user);

            userRepository.save(user);

            // Check and unlock achievements
            checkAndUnlockAchievements(user);
        }

        // Update lesson progress
        if (exercise.getLesson() != null) {
            updateLessonProgress(user, exercise.getLesson());
        }

        return isCorrect;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExerciseSubmissionDTO> getExerciseSubmissions(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        return submissionRepository.findByUserId(user.getId())
                .stream()
                .map(sub -> ExerciseSubmissionDTO.builder()
                        .id(sub.getId())
                        .exerciseId(sub.getExercise() != null ? sub.getExercise().getId() : null)
                        .userAnswer(sub.getUserAnswer())
                        .isCorrect(sub.getIsCorrect())
                        .pointsEarned(sub.getPointsEarned())
                        .submittedAt(sub.getSubmittedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private void updateLessonProgress(User user, Lesson lesson) {
        List<Exercise> totalExercises = exerciseRepository.findByLessonId(lesson.getId());
        if (totalExercises.isEmpty()) return;

        List<Long> correctExerciseIds = submissionRepository.findCorrectExerciseIdsByUserIdAndLessonId(user.getId(), lesson.getId());
        long correctCount = correctExerciseIds.size();

        BigDecimal completionPct = BigDecimal.valueOf(correctCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalExercises.size()), 2, RoundingMode.HALF_UP);

        boolean isCompleted = correctCount == totalExercises.size();

        Progress progress = progressRepository.findByUserIdAndLessonId(user.getId(), lesson.getId())
                .orElse(Progress.builder()
                        .user(user)
                        .lesson(lesson)
                        .build());

        progress.setCompletionPercentage(completionPct);
        if (isCompleted && !Boolean.TRUE.equals(progress.getIsCompleted())) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            log.info("User id={} đã hoàn thành bài học id={}", user.getId(), lesson.getId());
        }
        progress.setLastAccessed(LocalDateTime.now());
        progressRepository.save(progress);
    }

    private void checkAndUnlockAchievements(User user) {
        List<Achievement> achievements = achievementRepository.findByPointsRequiredLessThanEqual(user.getTotalPoints());
        if (achievements.isEmpty()) return;

        List<Long> unlockedAchievementIds = userAchievementRepository.findAchievementIdsByUserId(user.getId());

        for (Achievement achievement : achievements) {
            boolean alreadyUnlocked = unlockedAchievementIds.contains(achievement.getId());
            if (!alreadyUnlocked) {
                UserAchievement userAchievement = UserAchievement.builder()
                        .user(user)
                        .achievement(achievement)
                        .earnedAt(LocalDateTime.now())
                        .build();
                userAchievementRepository.save(userAchievement);
                log.info("Mở khóa thành công huy hiệu '{}' cho user id={}", achievement.getName(), user.getId());
            }
        }
    }

    private void updateStreak(User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastStudy = user.getLastStudyDate();

        if (lastStudy == null) {
            // First time studying
            user.setCurrentStreak(1);
        } else if (lastStudy.equals(today)) {
            // Already studied today, no change
            return;
        } else if (lastStudy.equals(today.minusDays(1))) {
            // Studied yesterday, increment streak
            user.setCurrentStreak((user.getCurrentStreak() != null ? user.getCurrentStreak() : 0) + 1);
        } else {
            // Missed a day, reset streak
            user.setCurrentStreak(1);
        }
        user.setLastStudyDate(today);
        log.info("Streak updated for user id={}: {} days", user.getId(), user.getCurrentStreak());
    }
}
