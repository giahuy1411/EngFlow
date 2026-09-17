package com.datn.engflow.service;

import com.datn.engflow.model.dto.response.DashboardStatsDTO;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.ProgressRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.VocabularyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Tổng số bài học ở Dashboard phải đếm bài published (khớp trang tiến độ),
 * không đếm cả bản nháp.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProgressRepository progressRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private VocabularyRepository vocabularyRepository;
    @Mock
    private StreakService streakService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                userRepository, progressRepository, lessonRepository, vocabularyRepository, streakService);
    }

    @Test
    void getDashboardStats_totalLessonsCountsOnlyPublished() {
        User user = new User();
        user.setId(1L);
        user.setEmail("u@test.com");
        user.setTotalPoints(7);
        when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
        when(lessonRepository.countByIsPublishedTrue()).thenReturn(10L);
        when(progressRepository.countByUserIdAndIsCompletedTrue(1L)).thenReturn(3L);
        when(vocabularyRepository.count()).thenReturn(50L);
        when(streakService.getCurrentStreak(1L)).thenReturn(4);

        DashboardStatsDTO stats = dashboardService.getDashboardStats("u@test.com");

        assertEquals(10, stats.getTotalLessons());
        assertEquals(3, stats.getCompletedLessons());
        assertEquals(4, stats.getCurrentStreak());
        verify(lessonRepository).countByIsPublishedTrue();
        verify(lessonRepository, never()).count();
    }
}