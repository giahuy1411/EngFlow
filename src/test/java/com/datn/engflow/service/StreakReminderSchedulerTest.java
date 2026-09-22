package com.datn.engflow.service;

import com.datn.engflow.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho job mail nhắc học: hai nhóm người nhận, số streak hiệu lực
 * trong mail at-risk, marker chống double-send, và gate catch-up.
 */
@ExtendWith(MockitoExtension.class)
class StreakReminderSchedulerTest {

    @Mock
    private StreakService streakService;

    @Mock
    private EmailService emailService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private StreakReminderScheduler scheduler;

    private String markerKey;

    @BeforeEach
    void setUp() {
        // Dựng thủ công: Clock là real system clock (mock Clock khiến
        // LocalDateTime.now(clock) NPE vì instant() trả null). Marker key
        // trong setUp và trong job cùng nguồn (LocalDate.now(clock)) nên khớp.
        scheduler = new StreakReminderScheduler(
                streakService, emailService, redisTemplate, Clock.systemDefaultZone());
        markerKey = "streak:reminder:" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.increment(anyString())).thenReturn(1L);
    }

    private User user(Long id, String email, LocalDate lastStudyDate, Integer streak) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName("User " + id);
        // audit-v13 F-13-08: the legacy users.last_study_date / current_streak columns were
        // dropped. `lastStudyDate` and `streak` are still the inputs that decide the stubs
        // below, but they now live only in the (mocked) streak service — which is where the
        // scheduler actually reads them from.
        lenient().when(streakService.reminderEligible(id, true)).thenReturn(
                lastStudyDate.equals(LocalDate.now().minusDays(1)));
        lenient().when(streakService.reminderEligible(id, false)).thenReturn(
                lastStudyDate.isBefore(LocalDate.now().minusDays(1)));
        lenient().when(streakService.getCurrentStreak(id)).thenReturn(streak);
        lenient().when(streakService.lastCompletedStreak(id)).thenReturn(streak);
        return user;
    }

    @Test
    void runReminderJob_atRiskUser_getsRescueEmailWithEffectiveStreak() {
        when(valueOperations.setIfAbsent(eq(markerKey), anyString(), any())).thenReturn(true);
        // học yesterday, DB streak 6 → hiệu lực 6
        User atRisk = user(1L, "risk@test.com", LocalDate.now().minusDays(1), 6);
        when(streakService.getUsersWithStreakAtRisk()).thenReturn(List.of(atRisk));
        when(streakService.getUsersWithBrokenStreak()).thenReturn(List.of());

        scheduler.sendDailyStreakReminders();

        verify(emailService).sendStreakReminder("risk@test.com", "User 1", 6);
    }

    @Test
    void runReminderJob_staleDbStreak_neverSentAsRescueStreak() {
        when(valueOperations.setIfAbsent(eq(markerKey), anyString(), any())).thenReturn(true);
        // bỏ 3 ngày nhưng DB vẫn treo 6 → thuộc nhóm broken, mail comeback với "từng đạt"=6
        User stale = user(1L, "stale@test.com", LocalDate.now().minusDays(3), 6);
        when(streakService.getUsersWithStreakAtRisk()).thenReturn(List.of());
        when(streakService.getUsersWithBrokenStreak()).thenReturn(List.of(stale));

        scheduler.sendDailyStreakReminders();

        verify(emailService, never()).sendStreakReminder(anyString(), anyString(), anyInt());
        verify(emailService).sendStreakComebackReminder("stale@test.com", "User 1", 6);
    }

    @Test
    void runReminderJob_brokenUser_getsComebackEmailWithLastStreak() {
        when(valueOperations.setIfAbsent(eq(markerKey), anyString(), any())).thenReturn(true);
        User broken = user(2L, "gone@test.com", LocalDate.now().minusDays(5), 4);
        when(streakService.getUsersWithStreakAtRisk()).thenReturn(List.of());
        when(streakService.getUsersWithBrokenStreak()).thenReturn(List.of(broken));

        scheduler.sendDailyStreakReminders();

        verify(emailService).sendStreakComebackReminder("gone@test.com", "User 2", 4);
    }

    @Test
    void runReminderJob_alreadyRanToday_skipsEverything() {
        when(valueOperations.setIfAbsent(eq(markerKey), anyString(), any())).thenReturn(false);

        scheduler.sendDailyStreakReminders();

        verify(streakService, never()).getUsersWithStreakAtRisk();
        verify(streakService, never()).getUsersWithBrokenStreak();
        verify(emailService, never()).sendStreakReminder(anyString(), anyString(), anyInt());
    }

    @Test
    void runReminderJob_emailFailure_doesNotBreakJobForOtherUsers() {
        when(valueOperations.setIfAbsent(eq(markerKey), anyString(), any())).thenReturn(true);
        User first = user(1L, "first@test.com", LocalDate.now().minusDays(1), 3);
        User second = user(2L, "second@test.com", LocalDate.now().minusDays(1), 7);
        when(streakService.getUsersWithStreakAtRisk()).thenReturn(List.of(first, second));
        when(streakService.getUsersWithBrokenStreak()).thenReturn(List.of());
        doThrow(new RuntimeException("SMTP down")).when(emailService)
                .sendStreakReminder(eq("first@test.com"), anyString(), anyInt());

        scheduler.sendDailyStreakReminders();

        verify(emailService).sendStreakReminder("second@test.com", "User 2", 7);
    }

    // ---- catch-up gate ----

    @Test
    void catchUpIfMissed_disabledByDefault_neverRunsJob() {
        // catchUpEnabled = false (giá trị mặc định) → không đụng Redis, không mail.
        // Đây là bảo vệ cho @SpringBootTest load full context: không gửi mail thật.
        scheduler.catchUpIfMissed();

        verifyNoInteractions(valueOperations);
        verify(streakService, never()).getUsersWithStreakAtRisk();
        verify(emailService, never()).sendStreakReminder(anyString(), anyString(), anyInt());
    }

    @Test
    void catchUpIfMissed_enabledAndAfterEightPm_runsJob() throws Exception {
        // Clock fixed 2026-09-07 21:30 (+07) — sau 20:00, job hôm đó đã lỡ
        Clock afterEightPm = Clock.fixed(Instant.parse("2026-09-07T14:30:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        StreakReminderScheduler enabled = new StreakReminderScheduler(streakService, emailService, redisTemplate, afterEightPm);
        setCatchUpEnabled(enabled, true);
        String key20260907 = "streak:reminder:2026-09-07";

        when(valueOperations.setIfAbsent(eq(key20260907), anyString(), any())).thenReturn(true);
        when(streakService.getUsersWithStreakAtRisk()).thenReturn(List.of());
        when(streakService.getUsersWithBrokenStreak()).thenReturn(List.of());

        enabled.catchUpIfMissed();

        verify(valueOperations).setIfAbsent(eq(key20260907), eq("catch-up"), any());
    }

    @Test
    void catchUpIfMissed_enabledButBeforeEightPm_skips() throws Exception {
        // Clock fixed 2026-09-07 19:59 (+07) — chưa đến giờ
        Clock before = Clock.fixed(Instant.parse("2026-09-07T12:59:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        StreakReminderScheduler enabled = new StreakReminderScheduler(streakService, emailService, redisTemplate, before);
        setCatchUpEnabled(enabled, true);

        enabled.catchUpIfMissed();

        verifyNoInteractions(valueOperations);
    }

    /**
     * Cron chạy theo zone VN, nhưng marker/sent key từng lấy {@code LocalDate.now(clock)}
     * với clock ở zone hệ thống. Nếu {@code TZ} thiếu thì catch-up so giờ UTC (sớm 7h)
     * còn marker key lại lệch sang ngày UTC — hai nguồn "hôm nay" không khớp nhau quanh
     * nửa đêm VN. Scheduler phải pin zone VN cho mọi thứ, đúng cách StudyActivityService
     * đang làm.
     */
    @Test
    void catchUpUsesVietnamDateEvenWhenSystemClockIsUtc() throws Exception {
        // 13:30 UTC = 20:30 VN cùng ngày. Catch-up phải nổ (sau 20:00 VN) VÀ marker
        // key phải mang ngày VN, không phải ngày UTC (vẫn cùng ngày ở ví dụ này, nhưng
        // xung quanh 00:00–07:00 VN hai zone sẽ lệch một ngày).
        Clock utcAfterEightPmVn = Clock.fixed(Instant.parse("2026-09-07T13:30:00Z"), ZoneId.of("UTC"));
        StreakReminderScheduler enabled = new StreakReminderScheduler(streakService, emailService, redisTemplate, utcAfterEightPmVn);
        setCatchUpEnabled(enabled, true);
        String key20260907 = "streak:reminder:2026-09-07";

        when(valueOperations.setIfAbsent(eq(key20260907), anyString(), any())).thenReturn(true);
        when(streakService.getUsersWithStreakAtRisk()).thenReturn(List.of());
        when(streakService.getUsersWithBrokenStreak()).thenReturn(List.of());

        enabled.catchUpIfMissed();

        // Marker key theo ngày VN (2026-09-07), không theo ngày UTC (vẫn là 2026-09-07
        // trong ví dụ này, nhưng assertion quan trọng là nó KHÔNG phải 2026-09-08 —
        // đấy là cái sẽ sai nếu clock pin nhầm zone và instant rơi vào 00:00–07:00 VN).
        verify(valueOperations).setIfAbsent(eq(key20260907), eq("catch-up"), any());
    }

    private void setCatchUpEnabled(StreakReminderScheduler s, boolean value) throws Exception {
        Field field = StreakReminderScheduler.class.getDeclaredField("catchUpEnabled");
        field.setAccessible(true);
        field.setBoolean(s, value);
    }
    // ---- per-user idempotency: retry cùng ngày không gửi trùng (B2) ----

    private String sentKey(Long userId) {
        return "streak:sent:" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ":" + userId;
    }

    @Test
    void runReminderJob_userAlreadySentToday_skipsResend() {
        when(valueOperations.setIfAbsent(eq(markerKey), anyString(), any())).thenReturn(true);
        User atRisk = user(1L, "risk@test.com", LocalDate.now().minusDays(1), 6);
        User broken = user(2L, "gone@test.com", LocalDate.now().minusDays(5), 4);
        when(streakService.getUsersWithStreakAtRisk()).thenReturn(List.of(atRisk));
        when(streakService.getUsersWithBrokenStreak()).thenReturn(List.of(broken));
        doReturn(true).when(redisTemplate).hasKey(sentKey(1L));
        doReturn(true).when(redisTemplate).hasKey(sentKey(2L));

        scheduler.sendDailyStreakReminders();

        verify(emailService, never()).sendStreakReminder(anyString(), anyString(), anyInt());
        verify(emailService, never()).sendStreakComebackReminder(anyString(), anyString(), anyInt());
    }

    @Test
    void runReminderJob_successfulSend_marksUserSent() {
        when(valueOperations.setIfAbsent(eq(markerKey), anyString(), any())).thenReturn(true);
        User atRisk = user(1L, "risk@test.com", LocalDate.now().minusDays(1), 6);
        User broken = user(2L, "gone@test.com", LocalDate.now().minusDays(5), 4);
        when(streakService.getUsersWithStreakAtRisk()).thenReturn(List.of(atRisk));
        when(streakService.getUsersWithBrokenStreak()).thenReturn(List.of(broken));

        scheduler.sendDailyStreakReminders();

        verify(valueOperations).set(eq(sentKey(1L)), eq("1"), any());
        verify(valueOperations).set(eq(sentKey(2L)), eq("1"), any());
    }

    @Test
    void runReminderJob_failedSend_doesNotMarkUserSent() {
        when(valueOperations.setIfAbsent(eq(markerKey), anyString(), any())).thenReturn(true);
        User atRisk = user(1L, "risk@test.com", LocalDate.now().minusDays(1), 6);
        when(streakService.getUsersWithStreakAtRisk()).thenReturn(List.of(atRisk));
        when(streakService.getUsersWithBrokenStreak()).thenReturn(List.of());
        doThrow(new RuntimeException("SMTP down")).when(emailService)
                .sendStreakReminder(anyString(), anyString(), anyInt());

        scheduler.sendDailyStreakReminders();

        verify(valueOperations, never()).set(eq(sentKey(1L)), anyString(), any());
    }

    @Test
    void redisFailureMustNotSendWithoutDeduplication() {
        when(valueOperations.setIfAbsent(eq(markerKey), anyString(), any()))
                .thenThrow(new IllegalStateException("Redis unavailable"));
        scheduler.sendDailyStreakReminders();
        verifyNoInteractions(emailService, streakService);
    }

    @Test
    void userWhoStudiedAfterCandidateSelectionDoesNotReceiveReminder() {
        when(valueOperations.setIfAbsent(eq(markerKey), anyString(), any())).thenReturn(true);
        User candidate = user(1L, "risk@test.com", LocalDate.now().minusDays(1), 6);
        when(streakService.getUsersWithStreakAtRisk()).thenReturn(List.of(candidate));
        when(streakService.getUsersWithBrokenStreak()).thenReturn(List.of());
        when(streakService.reminderEligible(1L, true)).thenReturn(false);
        scheduler.sendDailyStreakReminders();
        verifyNoInteractions(emailService);
    }

    @Test
    void retryBudgetStopsAfterThreeAttempts() {
        when(valueOperations.setIfAbsent(eq(markerKey), anyString(), any())).thenReturn(true);
        when(valueOperations.increment("streak:attempts:" + LocalDate.now())).thenReturn(4L);
        scheduler.sendDailyStreakReminders();
        verifyNoInteractions(emailService, streakService);
    }

    @Test
    void comebackSuppressionIsThirtyDaysAndOnlyWrittenAfterSuccessfulSend() {
        when(valueOperations.setIfAbsent(eq(markerKey), anyString(), any())).thenReturn(true);
        User candidate = user(2L, "gone@test.com", LocalDate.now().minusDays(3), 6);
        when(streakService.getUsersWithStreakAtRisk()).thenReturn(List.of());
        when(streakService.getUsersWithBrokenStreak()).thenReturn(List.of(candidate));
        scheduler.sendDailyStreakReminders();
        var order = inOrder(emailService, valueOperations);
        order.verify(emailService).sendStreakComebackReminder("gone@test.com", "User 2", 6);
        order.verify(valueOperations).setIfAbsent("streak:comeback:2", "1", java.time.Duration.ofDays(30));
    }
}
