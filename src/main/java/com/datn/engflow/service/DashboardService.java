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

/**
 * Cung cấp số liệu trang tổng quan của một người học.
 *
 * <p>Tầng service, được gọi từ {@code DashboardController}. Đếm bài học đã publish, bài đã
 * hoàn thành và tổng từ vựng qua {@link UserRepository}, {@link ProgressRepository},
 * {@link LessonRepository}, {@link VocabularyRepository}; streak hiệu lực lấy từ
 * {@link StreakService} (nguồn {@code study_days}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final ProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final VocabularyRepository vocabularyRepository;
    private final StreakService streakService;

    /**
     * Thống kê tổng quan cho người học đang đăng nhập.
     *
     * <p>Một số trường cố định bằng 0/empty vì nguồn dữ liệu tương ứng đã bị gỡ; chúng vẫn
     * được giữ trong DTO để không phá vỡ hợp đồng với frontend.
     *
     * @param email email người dùng đang đăng nhập
     * @return DTO thống kê dashboard
     * @throws ResourceNotFoundException nếu không tìm thấy user theo email
     */
    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        int totalLessons = Math.toIntExact(lessonRepository.countByIsPublishedTrue());
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
