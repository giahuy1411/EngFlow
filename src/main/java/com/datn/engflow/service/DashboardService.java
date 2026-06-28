package com.datn.engflow.service;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.response.DashboardStatsDTO;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final ProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final VocabularyRepository vocabularyRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseSubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(String email) {
        log.info("L\u1ea5y th\u00f4ng tin dashboard cho user: {}", email);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Long totalLessons = lessonRepository.count();
        Long completedLessons = progressRepository.countByUserIdAndIsCompletedTrue(user.getId());

        Long totalVocabulary = vocabularyRepository.count();
        Long totalExercises = exerciseRepository.count();
        Long correctExercises = submissionRepository.countCorrectByUserId(user.getId());

        List<DashboardStatsDTO.DailyPointEntry> dailyPoints = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            Integer pointsForDay = submissionRepository.findCorrectByUserIdBetween(user.getId(), startOfDay, endOfDay)
                    .stream()
                    .mapToInt(s -> s.getPointsEarned() != null ? s.getPointsEarned() : 0)
                    .sum();

            dailyPoints.add(DashboardStatsDTO.DailyPointEntry.builder()
                    .date(date.format(formatter))
                    .points(pointsForDay)
                    .build());
        }

        return DashboardStatsDTO.builder()
                .totalLessons(totalLessons.intValue())
                .completedLessons(completedLessons.intValue())
                .totalPoints(user.getTotalPoints())
                .currentStreak(user.getCurrentStreak() != null ? user.getCurrentStreak() : 0)
                .totalVocabulary(totalVocabulary.intValue())
                .totalExercises(totalExercises.intValue())
                .correctExercises(correctExercises.intValue())
                .dailyPoints(dailyPoints)
                .build();
    }
}
