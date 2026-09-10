package com.datn.engflow.service;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private Clock clock;

    private StreakService streakService;

    private static final Long USER_ID = 1L;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));
        when(clock.instant()).thenReturn(Instant.parse("2026-09-10T00:00:00Z"));
        today = LocalDate.now(clock);
        streakService = new StreakService(userRepository, redisTemplate, clock);
    }

    private User userWithStreak(LocalDate lastStudyDate, Integer streak) {
        User user = new User();
        user.setId(USER_ID);
        user.setLastStudyDate(lastStudyDate);
        user.setCurrentStreak(streak);
        return user;
    }

    // ---- recordAccess: điều kiện tăng chuỗi ----

    @Test
    void recordAccess_firstActivityEver_setsStreakOne() {
        User user = userWithStreak(null, 0);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

        streakService.recordAccess(USER_ID);

        assertThat(user.getCurrentStreak()).isEqualTo(1);
        assertThat(user.getLastStudyDate()).isEqualTo(today);
        verify(userRepository).save(user);
        verify(setOperations).add(eq("user:login_days:1"), eq(iso(today)));
        verify(redisTemplate).expire(eq("user:login_days:1"), eq(90L), eq(java.util.concurrent.TimeUnit.DAYS));
    }

    @Test
    void recordAccess_gapExactlyOneDay_incrementsStreak() {
        User user = userWithStreak(today.minusDays(1), 5);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

        streakService.recordAccess(USER_ID);

        assertThat(user.getCurrentStreak()).isEqualTo(6);
        assertThat(user.getLastStudyDate()).isEqualTo(today);
    }

    @Test
    void recordAccess_gapMoreThanOneDay_resetsStreakToOne() {
        User user = userWithStreak(today.minusDays(2), 7);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

        streakService.recordAccess(USER_ID);

        assertThat(user.getCurrentStreak()).isEqualTo(1);
        assertThat(user.getLastStudyDate()).isEqualTo(today);
    }

    @Test
    void recordAccess_sameDay_isNoOp() {
        User user = userWithStreak(today, 4);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        streakService.recordAccess(USER_ID);

        assertThat(user.getCurrentStreak()).isEqualTo(4); // không đổi
        verify(userRepository, never()).save(any());
        verify(redisTemplate, never()).opsForSet();
    }

    @Test
    void recordAccess_nullStreakField_treatedAsZero() {
        User user = userWithStreak(today.minusDays(1), null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

        streakService.recordAccess(USER_ID);

        assertThat(user.getCurrentStreak()).isEqualTo(1);
    }

    @Test
    void recordAccess_redisFailure_doesNotRollbackStreak() {
        User user = userWithStreak(today.minusDays(1), 3);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForSet()).thenThrow(new RuntimeException("Redis down"));

        streakService.recordAccess(USER_ID);

        assertThat(user.getCurrentStreak()).isEqualTo(4);
        assertThat(user.getLastStudyDate()).isEqualTo(today);
        verify(userRepository).save(user);
    }

    // ---- getCurrentStreak: streak hiệu lực ----

    @Test
    void getCurrentStreak_neverStudied_returnsZero() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userWithStreak(null, 3)));

        assertThat(streakService.getCurrentStreak(USER_ID)).isZero();
    }

    @Test
    void getCurrentStreak_lapsedMoreThanOneDay_returnsZero_notStaleDbValue() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userWithStreak(today.minusDays(3), 6)));

        assertThat(streakService.getCurrentStreak(USER_ID)).isZero();
    }

    @Test
    void getCurrentStreak_recentActivity_returnsDbStreak() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userWithStreak(today.minusDays(1), 6)));

        assertThat(streakService.getCurrentStreak(USER_ID)).isEqualTo(6);
    }

    // ---- getLoginDays: cửa sổ days ngày tính cả hôm nay ----

    @Test
    void getLoginDays_returnsExactlyDaysWindowIncludingToday() {
        String key = "user:login_days:1";
        Set<String> members = Set.of(
                iso(today),
                iso(today.minusDays(1)),
                iso(today.minusDays(29)), // ngày xa nhất còn nằm trong cửa sổ 30 ngày
                iso(today.minusDays(30)), // ngoài cửa sổ → bị loại
                iso(today.minusDays(50))  // ngoài cửa sổ → bị loại
        );
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(key)).thenReturn(members);

        List<String> result = streakService.getLoginDays(USER_ID, 30);

        assertThat(result).containsExactly(
                iso(today.minusDays(29)),
                iso(today.minusDays(1)),
                iso(today));
    }

    @Test
    void getLoginDays_emptyRedis_returnsEmptyList() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("user:login_days:1")).thenReturn(Set.of());

        assertThat(streakService.getLoginDays(USER_ID, 30)).isEmpty();
    }

    @Test
    void getLoginDays_redisUnavailable_returnsEmptyList() {
        when(redisTemplate.opsForSet()).thenThrow(new RuntimeException("Redis down"));

        List<String> result = streakService.getLoginDays(USER_ID, 30);

        assertThat(result).isEmpty();
    }

    @Test
    void getLoginDays_invalidDateMember_skipsBadValue() {
        Set<String> members = Set.of(iso(today), "not-a-date");
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("user:login_days:1")).thenReturn(members);

        List<String> result = streakService.getLoginDays(USER_ID, 30);

        assertThat(result).containsExactly(iso(today));
    }

    // ---- checkin: giữ signature cho GameService, hành vi = recordAccess ----

    @Test
    void checkin_delegatesToRecordAccess() {
        User user = userWithStreak(today.minusDays(1), 2);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

        streakService.checkin(USER_ID, 10, 1);

        assertThat(user.getCurrentStreak()).isEqualTo(3);
    }

    // ---- hai nhóm mail ----

    @Test
    void getUsersWithStreakAtRisk_queriesUsersWhoStudiedYesterday() {
        when(userRepository.findActiveUsersWhoLastStudiedOn(today.minusDays(1))).thenReturn(List.of());

        streakService.getUsersWithStreakAtRisk();

        verify(userRepository).findActiveUsersWhoLastStudiedOn(today.minusDays(1));
    }

    @Test
    void getUsersWithBrokenStreak_queriesUsersInactiveTwoPlusDays() {
        when(userRepository.findUsersWithBrokenStreak(today.minusDays(1))).thenReturn(List.of());

        streakService.getUsersWithBrokenStreak();

        verify(userRepository).findUsersWithBrokenStreak(today.minusDays(1));
    }

    private String iso(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
