package com.datn.engflow.service;

import com.datn.engflow.model.dto.response.AdminStatsDTO;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.LessonSubmissionRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.VocabularyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for audit-v13 F-13-08.
 *
 * <p>The admin dashboard's {@code recentUsers} ("Học trong 7 ngày") used to be computed from
 * {@code userRepository.countByLastStudyDateAfter(...)} — i.e. the legacy
 * {@code users.last_study_date} column, which the streak refactor stopped maintaining.
 * Measured 2026-09-22: user 2 had {@code last_study_date = 2026-09-19} while really studying
 * on {@code 2026-09-22}; user 3 was off by two days.
 *
 * <p>This pins the metric to the real {@code study_days} calendar.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceDashboardStreakSourceTest {

    @Mock private UserRepository userRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private VocabularyRepository vocabularyRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private LessonSubmissionRepository lessonSubmissionRepository;
    @Mock private LessonService lessonService;
    @Mock private StreakService streakService;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository, lessonRepository, vocabularyRepository,
                exerciseRepository, lessonSubmissionRepository, lessonService,
                Clock.systemUTC(), streakService);
    }

    @Test
    @SuppressWarnings("deprecation") // asserting the deprecated legacy method is NOT used
    void recentUsersComesFromStudyDaysNotTheLegacyColumn() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByIsActiveTrue()).thenReturn(90L);
        when(streakService.countActiveLearnersInLastDays(7)).thenReturn(7L);

        AdminStatsDTO stats = adminService.getDashboardStats();

        assertThat(stats.getRecentUsers()).isEqualTo(7L);
        // The stale legacy column must not be read any more.
        verify(userRepository, never()).countByLastStudyDateAfter(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void asksForASevenDayWindow() {
        when(userRepository.count()).thenReturn(1L);
        when(userRepository.countByIsActiveTrue()).thenReturn(1L);
        when(streakService.countActiveLearnersInLastDays(anyInt())).thenReturn(0L);

        adminService.getDashboardStats();

        verify(streakService).countActiveLearnersInLastDays(7);
    }

    /**
     * The window must be "today and the six days before it" — the same inclusive convention
     * {@code StudyActivityService.snapshot(Long, int)} uses ({@code today - (window - 1)}).
     * Using {@code today - window} would silently make it an 8-day window.
     */
    @Test
    void windowIsInclusiveOfToday() {
        java.time.LocalDate today = java.time.LocalDate.now();

        // StudyActivityService is the unit under test here; assert the boundary directly.
        com.datn.engflow.repository.StudyDayRepository repo =
                org.mockito.Mockito.mock(com.datn.engflow.repository.StudyDayRepository.class);
        org.mockito.Mockito.when(repo.countDistinctUsersBetween(today.minusDays(6), today)).thenReturn(3L);

        StudyActivityService svc = new StudyActivityService(
                null, repo, null, null, java.time.Clock.systemUTC());

        org.assertj.core.api.Assertions.assertThat(svc.countActiveLearnersInLastDays(7)).isEqualTo(3L);
        org.mockito.Mockito.verify(repo).countDistinctUsersBetween(today.minusDays(6), today);
    }
}
