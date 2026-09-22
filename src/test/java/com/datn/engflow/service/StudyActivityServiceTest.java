package com.datn.engflow.service;

import com.datn.engflow.model.dto.projection.StudyDayOwner;
import com.datn.engflow.model.entity.StudyDay;
import com.datn.engflow.model.entity.StudyPolicy;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.StudyDayRepository;
import com.datn.engflow.repository.StudyPolicyRepository;
import com.datn.engflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class StudyActivityServiceTest {
    private final LocalDate start = LocalDate.of(2026, 9, 18);
    private final ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
    private final UserRepository users = mock(UserRepository.class);
    private final StudyDayRepository days = mock(StudyDayRepository.class);
    private final StudyPolicyRepository policies = mock(StudyPolicyRepository.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final List<StudyDay> stored = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(users.findForStudyUpdate(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            // audit-v13 F-13-08: the legacy users.current_streak / last_study_date columns
            // were dropped. The test used to seed them with sentinel values (99 / a past
            // date) to prove the service ignored them; with the columns gone, seeding the
            // real source (study_days) below is what matters.
            return Optional.of(User.builder().id(id).isActive(true).build());
        });
        when(policies.findById(1)).thenReturn(Optional.of(new StudyPolicy(1, start)));
        when(days.existsByUserIdAndStudyDate(anyLong(), any())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            LocalDate date = invocation.getArgument(1);
            return stored.stream().anyMatch(day -> day.getUserId().equals(userId)
                    && day.getStudyDate().equals(date));
        });
        when(days.save(any())).thenAnswer(invocation -> {
            StudyDay day = invocation.getArgument(0);
            stored.add(day);
            return day;
        });
        when(days.findDates(anyLong(), any(), any())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            LocalDate from = invocation.getArgument(1);
            LocalDate to = invocation.getArgument(2);
            return stored.stream()
                    .filter(day -> day.getUserId().equals(userId))
                    .map(StudyDay::getStudyDate)
                    .filter(date -> !date.isBefore(from) && !date.isAfter(to))
                    .sorted(Comparator.reverseOrder()).toList();
        });
    }

    private StudyActivityService at(LocalDate date) {
        return new StudyActivityService(users, days, policies, redis,
                Clock.fixed(date.atStartOfDay(zone).toInstant(), zone));
    }

    @Test
    void fourDayJourneyDoesNotInheritLegacyStreak() {
        assertThat(at(start).snapshot(42L, 30).currentStreak()).isZero();
        at(start).recordStudy(42L);
        assertThat(at(start).snapshot(42L, 30).currentStreak()).isEqualTo(1);
        assertThat(at(start.plusDays(1)).snapshot(42L, 30).currentStreak()).isEqualTo(1);
        assertThat(at(start.plusDays(1)).snapshot(42L, 30).studiedToday()).isFalse();
        assertThat(at(start.plusDays(2)).snapshot(42L, 30).currentStreak()).isZero();
        at(start.plusDays(2)).recordStudy(42L);
        at(start.plusDays(3)).recordStudy(42L);
        var snapshot = at(start.plusDays(3)).snapshot(42L, 30);
        assertThat(snapshot.currentStreak()).isEqualTo(2);
        assertThat(snapshot.studiedDays()).containsExactly(start, start.plusDays(2), start.plusDays(3));
        assertThat(snapshot.studiedToday()).isTrue();
        verify(users, never()).save(any());
    }

    @Test
    void repeatActivitiesOnlyPersistOneDay() {
        at(start).recordStudy(42L);
        at(start).recordStudy(42L);
        assertThat(stored).hasSize(1);
        verify(users, times(2)).findForStudyUpdate(42L);
    }

    @Test
    void redisFailureDoesNotEraseSqlHistoryOrBecomeLegacyAbsence() {
        when(redis.opsForSet()).thenThrow(new IllegalStateException("Redis unavailable"));
        at(start).recordStudy(42L);
        var snapshot = at(start).snapshot(42L, 30);
        assertThat(snapshot.studiedDays()).containsExactly(start);
        assertThat(snapshot.legacyHistoryAvailable()).isFalse();
    }

    @Test
    void missingCutoverFailsExplicitlyInsteadOfUsingRestartDate() {
        when(policies.findById(1)).thenReturn(Optional.empty());
        // Thông báo phải chỉ đúng file vận hành cần chạy: người trực ca đọc log
        // không có ngữ cảnh của repo, câu "apply the reviewed deployment SQL"
        // không nói được file nào.
        assertThatThrownBy(() -> at(start).recordStudy(42L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tasks/streak-study/deploy.sql");
        verify(days, never()).save(any());
    }

    /**
     * audit-v10 F122. Trước cutover là trạng thái cấu hình hợp lệ (streak ngủ), không
     * phải sự cố: phải không ghi ngày học VÀ không ném, vì cú ném sẽ cuốn theo kết quả
     * học tập của người gọi (recordStudy chạy trong chính transaction đó).
     */
    @Test
    void beforeCutoverRecordsNothingAndDoesNotFailTheCaller() {
        at(start.minusDays(1)).recordStudy(42L);
        assertThat(stored).isEmpty();
        verify(days, never()).save(any());
    }

    @Test
    void rejectInvalidWindow() {
        assertThatThrownBy(() -> at(start).snapshot(42L, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> at(start).snapshot(42L, 367)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sourceClockZoneCannotChangeVietnamStudyDate() {
        var service = new StudyActivityService(users, days, policies, redis,
                Clock.fixed(start.atStartOfDay(zone).toInstant(), ZoneId.of("UTC")));
        service.recordStudy(42L);
        assertThat(stored.getFirst().getStudyDate()).isEqualTo(start);
    }

    // --- currentStreaks: batch cho leaderboard/admin list ---

    /** Một query cho cả page, không phải một query mỗi row (N+1). */
    @Test
    void batchStreaksQueryOnceForManyUsers() {
        when(days.findDatesForUsers(anyCollection(), any(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var ids = (java.util.Collection<Long>) invocation.getArgument(0);
            LocalDate from = invocation.getArgument(1);
            LocalDate to = invocation.getArgument(2);
            return stored.stream()
                    .filter(day -> ids.contains(day.getUserId()))
                    .filter(day -> !day.getStudyDate().isBefore(from) && !day.getStudyDate().isAfter(to))
                    .map(day -> studyDayOwner(day.getUserId(), day.getStudyDate()))
                    .toList();
        });

        at(start).recordStudy(42L);
        at(start).recordStudy(43L);
        at(start.plusDays(1)).recordStudy(43L);

        var streaks = at(start.plusDays(1)).currentStreaks(List.of(42L, 43L, 44L));

        assertThat(streaks).containsEntry(42L, 1).containsEntry(43L, 2).containsEntry(44L, 0);
        verify(days, times(1)).findDatesForUsers(anyCollection(), any(), any());
    }

    /** Page rỗng không được chạm repository (idiom có sẵn của repo). */
    @Test
    void emptyBatchDoesNotQuery() {
        assertThat(at(start).currentStreaks(List.of())).isEmpty();
        verify(days, never()).findDatesForUsers(anyCollection(), any(), any());
    }

    private static StudyDayOwner studyDayOwner(Long userId, LocalDate date) {
        return new StudyDayOwner() {
            @Override public Long getUserId() { return userId; }
            @Override public LocalDate getStudyDate() { return date; }
        };
    }
}
