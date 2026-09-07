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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho logic tăng/reset streak và cửa sổ lịch sử ngày học.
 */
@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private StreakService streakService;

    private static final Long USER_ID = 1L;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
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
        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        streakService.recordAccess(USER_ID);

        assertThat(user.getCurrentStreak()).isEqualTo(1);
        assertThat(user.getLastStudyDate()).isEqualTo(today);
        verify(userRepository).save(user);
        verify(setOperations).add(eq("user:login_days:1"), eq(today.format(DateTimeFormatter.ISO_LOCAL_DATE)));
        verify(redisTemplate).expire(eq("user:login_days:1"), eq(90L), eq(TimeUnit.DAYS));
    }

    @Test
    void recordAccess_gapExactlyOneDay_incrementsStreak() {
        User user = userWithStreak(today.minusDays(1), 5);
        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        streakService.recordAccess(USER_ID);

        assertThat(user.getCurrentStreak()).isEqualTo(6);
        assertThat(user.getLastStudyDate()).isEqualTo(today);
    }

    @Test
    void recordAccess_gapMoreThanOneDay_resetsStreakToOne() {
        User user = userWithStreak(today.minusDays(2), 7);
        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        streakService.recordAccess(USER_ID);

        assertThat(user.getCurrentStreak()).isEqualTo(1);
        assertThat(user.getLastStudyDate()).isEqualTo(today);
    }

    @Test
    void recordAccess_sameDay_isNoOp() {
        User user = userWithStreak(today, 4);
        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));

        streakService.recordAccess(USER_ID);

        assertThat(user.getCurrentStreak()).isEqualTo(4); // không đổi
        verify(userRepository, never()).save(any());
        verify(redisTemplate, never()).opsForSet();
    }

    @Test
    void recordAccess_nullStreakField_treatedAsZero() {
        User user = userWithStreak(today.minusDays(1), null);
        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        streakService.recordAccess(USER_ID);

        assertThat(user.getCurrentStreak()).isEqualTo(1);
    }

    // ---- getCurrentStreak: streak hiệu lực ----

    @Test
    void getCurrentStreak_neverStudied_returnsZero() {
        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(userWithStreak(null, 3)));

        assertThat(streakService.getCurrentStreak(USER_ID)).isZero();
    }

    @Test
    void getCurrentStreak_lapsedMoreThanOneDay_returnsZero_notStaleDbValue() {
        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(userWithStreak(today.minusDays(3), 6)));

        assertThat(streakService.getCurrentStreak(USER_ID)).isZero();
    }

    @Test
    void getCurrentStreak_studiedYesterdayOrToday_returnsRawValue() {
        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(userWithStreak(today.minusDays(1), 6)));

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

    // ---- checkin: giữ signature cho GameService, hành vi = recordAccess ----

    @Test
    void checkin_delegatesToRecordAccess() {
        User user = userWithStreak(today.minusDays(1), 2);
        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

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
        when(userRepository.findUsersWhoHaveNotLoggedInSince(today.minusDays(1))).thenReturn(List.of());

        streakService.getUsersWithBrokenStreak();

        verify(userRepository).findUsersWhoHaveNotLoggedInSince(today.minusDays(1));
    }

    private String iso(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
