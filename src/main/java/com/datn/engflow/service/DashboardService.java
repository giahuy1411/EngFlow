package com.datn.engflow.service;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.response.DashboardStatsDTO;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(String email) {
        log.info("L\u1ea5y th\u00f4ng tin dashboard cho user: {}", email);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Long totalLessons = lessonRepository.count();
        Long completedLessons = progressRepository.countByUserIdAndIsCompletedTrue(user.getId());

        Long totalVocabulary = vocabularyRepository.count();

        List<DashboardStatsDTO.DailyPointEntry> dailyPoints = new ArrayList<>();

        return DashboardStatsDTO.builder()
                .totalLessons(totalLessons.intValue())
                .completedLessons(completedLessons.intValue())
                .totalPoints(user.getTotalPoints())
                .currentStreak(user.getCurrentStreak() != null ? user.getCurrentStreak() : 0)
                .totalVocabulary(totalVocabulary.intValue())
                .totalExercises(0)
                .correctExercises(0)
                .dailyPoints(dailyPoints)
                .build();
    }
}
