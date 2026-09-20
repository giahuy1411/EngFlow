package com.datn.engflow.service;

import com.datn.engflow.model.dto.response.StudySnapshot;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StreakServiceTest {
    private final StudyActivityService study = mock(StudyActivityService.class);
    private final StreakService streak = new StreakService(study);

    @Test
    void compatibilityReadersUseSqlStudyState() {
        LocalDate today = LocalDate.of(2026, 9, 18);
        when(study.today()).thenReturn(today);
        when(study.currentStreak(42L)).thenReturn(3);
        when(study.snapshot(42L, 30)).thenReturn(new StudySnapshot(today, 3, true,
                today.minusDays(5), List.of(today), List.of(today.minusDays(6)), true));
        assertThat(streak.getCurrentStreak(42L)).isEqualTo(3);
        assertThat(streak.todayIso()).isEqualTo("2026-09-18");
        assertThat(streak.getLoginDays(42L, 30)).containsExactly("2026-09-18");
    }

    @Test
    void positiveQuestionCountRecordsRegardlessOfScore() {
        streak.checkin(42L, 5, 0);
        verify(study).recordStudy(42L);
    }

    @Test
    void noQuestionsDoesNotRecordStudy() {
        streak.checkin(42L, null, 0);
        streak.checkin(42L, 0, 0);
        streak.checkin(42L, -1, 0);
        verifyNoInteractions(study);
    }

    @Test
    void reminderCandidatesUseActualLearningOnly() {
        streak.getUsersWithStreakAtRisk();
        streak.getUsersWithBrokenStreak();
        verify(study).reminderCandidates(true);
        verify(study).reminderCandidates(false);
    }
}
