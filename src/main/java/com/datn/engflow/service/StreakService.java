package com.datn.engflow.service;

import com.datn.engflow.config.RedisConstants;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Streak tracking cho user.
 *
 * <p>Mô hình dữ liệu:
 * <ul>
 *   <li>{@code users.last_study_date} + {@code users.current_streak} (SQL Server) —
 *       mốc cuối và chuỗi kèm mốc đó. Giá trị này có thể "treo" (stale) khi user
 *       bỏ học nhiều ngày: nó chỉ được cập nhật khi user quay lại.</li>
 *   <li>Redis Set {@code user:login_days:<id>} — lịch sử những ngày có hoạt động
 *       (TTL 90 ngày) dùng cho {@code /api/streak/history} và lịch học trong Profile.</li>
 * </ul>
 *
 * <p>Một "ngày học" được tính khi user (1) đăng nhập, (2) đã đăng nhập và truy cập
 * SPA (App mount → GET /api/auth/me), hoặc (3) nộp kết quả game. Streak tăng khi
 * khoảng cách từ lần học cuối đến hôm nay đúng 1 ngày; bỏ ≥ 2 ngày thì reset về 1.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StreakService {

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    /**
     * Ghi nhận một hoạt động của user trong hôm nay và cập nhật streak.
     * An toàn gọi nhiều lần trong ngày: lần sau trong cùng ngày là no-op.
     */
    @Transactional
    public void recordAccess(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        LocalDate today = LocalDate.now(clock);
        LocalDate lastStudyDate = user.getLastStudyDate();

        boolean firstActivityToday = lastStudyDate == null || !lastStudyDate.equals(today);
        if (!firstActivityToday) {
            return; // đã tính hôm nay rồi — không đổi streak, không đụng Redis
        }

        if (lastStudyDate == null) {
            user.setCurrentStreak(1);                       // hoạt động đầu tiên
        } else if (ChronoUnit.DAYS.between(lastStudyDate, today) == 1) {
            user.setCurrentStreak(streakOrZero(user) + 1);  // học liên tục → +1
        } else {
            user.setCurrentStreak(1);                       // bỏ ≥ 2 ngày → reset
        }

        user.setLastStudyDate(today);
        userRepository.save(user);
        recordLoginDateInRedis(userId, today);
    }

    /**
     * Check-in sau khi nộp game. totalQuestions/score không ảnh hưởng streak
     * (đã từng dùng cho tính năng cũ đã gỡ).
     */
    public void checkin(Long userId, Integer totalQuestions, int score) {
        recordAccess(userId);
    }

    /**
     * Chuỗi đang hiệu lực của user: 0 nếu chưa từng học hoặc đã bỏ quá 1 ngày.
     * Đây là nguồn sự thật cho hiển thị (Profile, mail) — khác với
     * {@code user.getCurrentStreak()} vốn là giá trị thô có thể treo.
     */
    public Integer getCurrentStreak(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return effectiveStreak(user, LocalDate.now(clock));
    }

    /**
     * Danh sách ngày có hoạt động trong {@code days} ngày gần nhất (tính cả hôm nay),
     * định dạng ISO {@code yyyy-MM-dd}, tăng dần — cho lịch học trong Profile.
     */
    public List<String> getLoginDays(Long userId, int days) {
        Set<String> loginDaysSet;
        try {
            loginDaysSet = redisTemplate.opsForSet().members(RedisConstants.LOGIN_DAYS_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Redis unavailable for getLoginDays userId={}: {}", userId, e.getMessage());
            return List.of();
        }
        if (loginDaysSet == null || loginDaysSet.isEmpty()) {
            return List.of();
        }

        LocalDate cutoff = LocalDate.now(clock).minusDays(days - 1L);
        return loginDaysSet.stream()
                .filter(dateStr -> {
                    try {
                        return !LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).isBefore(cutoff);
                    } catch (Exception e) {
                        log.warn("Invalid date in Redis set {}: {}", RedisConstants.LOGIN_DAYS_KEY_PREFIX + userId, dateStr);
                        return false;
                    }
                })
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * User active chưa học hôm nay nhưng <b>đã học yesterday</b> — streak còn sống,
     * chỉ cần học hôm nay là +1. Nhóm nhận mail "cứu streak" lúc 20:00.
     */
    public List<User> getUsersWithStreakAtRisk() {
        return userRepository.findActiveUsersWhoLastStudiedOn(LocalDate.now(clock).minusDays(1));
    }

    /**
     * User active chưa học hôm nay và <b>đã bỏ ≥ 2 ngày</b> (không gồm never-studied).
     * Streak đã gãy — nhận mail mời quay lại.
     */
    public List<User> getUsersWithBrokenStreak() {
        return userRepository.findUsersWithBrokenStreak(LocalDate.now(clock).minusDays(1));
    }

    /** Streak hiệu lực của một user entity đã load: gap > 1 ngày → 0. */
    private Integer effectiveStreak(User user, LocalDate today) {
        LocalDate lastStudyDate = user.getLastStudyDate();
        if (lastStudyDate == null || ChronoUnit.DAYS.between(lastStudyDate, today) > 1) {
            return 0;
        }
        return streakOrZero(user);
    }

    private int streakOrZero(User user) {
        return user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;
    }

    private void recordLoginDateInRedis(Long userId, LocalDate date) {
        String key = RedisConstants.LOGIN_DAYS_KEY_PREFIX + userId;
        try {
            redisTemplate.opsForSet().add(key, date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            redisTemplate.expire(key, RedisConstants.LOGIN_DAYS_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("Redis unavailable for recordLoginDate userId={}: {}", userId, e.getMessage());
        }
    }
}
