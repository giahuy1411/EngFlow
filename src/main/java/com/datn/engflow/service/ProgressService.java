package com.datn.engflow.service;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.response.AchievementDTO;
import com.datn.engflow.model.dto.response.ProgressResponse;
import com.datn.engflow.model.entity.Progress;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.UserAchievement;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.ProgressRepository;
import com.datn.engflow.repository.UserAchievementRepository;
import com.datn.engflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final ProgressRepository progressRepository;
    private final UserAchievementRepository userAchievementRepository;

    @Transactional(readOnly = true)
    public ProgressResponse getProgressSummary(String userEmail) {
        log.info("L\u1ea5y th\u00f4ng tin t\u1ed5ng k\u1ebft ti\u1ebfn \u0111\u1ed9 cho user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        int totalLessons = (int) lessonRepository.countByIsPublishedTrue();

        List<Progress> progresses = progressRepository.findByUserId(user.getId());
        int completedLessons = (int) progresses.stream().filter(p -> Boolean.TRUE.equals(p.getIsCompleted())).count();

        List<UserAchievement> userAchievements = userAchievementRepository.findByUserId(user.getId());
        List<AchievementDTO> unlockedAchievements = userAchievements.stream()
                .map(ua -> AchievementDTO.builder()
                        .id(ua.getAchievement().getId())
                        .name(ua.getAchievement().getName())
                        .description(ua.getAchievement().getDescription())
                        .iconUrl(ua.getAchievement().getIconUrl())
                        .badgeType(ua.getAchievement().getBadgeType())
                        .build())
                .collect(Collectors.toList());

        return ProgressResponse.builder()
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .totalPoints(user.getTotalPoints())
                .unlockedAchievements(unlockedAchievements)
                .build();
    }
}
