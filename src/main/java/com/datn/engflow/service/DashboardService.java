package com.datn.engflow.service;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.response.DashboardStatsDTO;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.ProgressRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final ProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final VocabularyRepository vocabularyRepository;
    private final StreakService streakService;

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        int totalLessons = Math.toIntExact(lessonRepository.count());
        int completedLessons = Math.toIntExact(progressRepository.countByUserIdAndIsCompletedTrue(user.getId()));
        int totalVocabulary = Math.toIntExact(vocabularyRepository.count());
        int effectiveStreak = streakService.getCurrentStreak(user.getId());
        return DashboardStatsDTO.builder()
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .totalPoints(user.getTotalPoints())
                .currentStreak(effectiveStreak)
                .totalVocabulary(totalVocabulary)
                .totalExercises(0)
                .correctExercises(0)
                .dailyPoints(Collections.emptyList())
                .build();
    }
}
