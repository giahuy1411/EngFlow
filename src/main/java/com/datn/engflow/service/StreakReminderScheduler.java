package com.datn.engflow.service;

import com.datn.engflow.config.RedisConstants;
import com.datn.engflow.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Job 20:00 hằng ngày gửi mail nhắc học theo trạng thái streak:
 * <ul>
 *   <li><b>At-risk</b> (học yesterday, chưa học hôm nay): "đừng để mất chuỗi N ngày"
 *       với N = streak hiệu lực.</li>
 *   <li><b>Broken</b> (bỏ ≥ 2 ngày, đã từng học): mail mời quay lại. Loại never-studied.</li>
 * </ul>
 *
 * <p>Chống lỡ job khi app khởi động muộn: nếu app sẵn sàng sau 20:00 mà job hôm đó
 * chưa từng chạy (marker Redis {@code streak:reminder:<date>} chưa có) thì chạy
 * bù ngay. Marker setIfAbsent cũng chống double-send khi container restart.
 * Marker chỉ được giữ khi toàn bộ gửi thành công — nếu có lỗi SMTP, marker bị xóa
 * để cho phép retry cùng ngày. Comeback có suppression riêng 30 ngày.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StreakReminderScheduler {

    private final StreakService streakService;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    @Value("${engflow.scheduler.catchup.enabled:false}")
    private boolean catchUpEnabled;

    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendDailyStreakReminders() {
        runReminderJob("scheduled");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpIfMissed() {
        if (!catchUpEnabled) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.getHour() >= RedisConstants.REMINDER_HOUR) {
            runReminderJob("catch-up");
        }
    }

    private void runReminderJob(String trigger) {
        String markerKey = RedisConstants.REMINDER_MARKER_PREFIX + LocalDate.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE);
        Boolean markerAcquired = tryAcquireMarker(markerKey, trigger);
        if (Boolean.FALSE.equals(markerAcquired)) {
            log.info("Streak reminder already ran today (marker {}), skip {} trigger.", markerKey, trigger);
            return;
        }

        log.info("Starting daily streak reminder job ({} trigger)...", trigger);

        boolean allSucceeded = true;

        List<User> atRiskUsers = streakService.getUsersWithStreakAtRisk();
        log.info("Found {} at-risk users (studied yesterday, not today).", atRiskUsers.size());
        for (User user : atRiskUsers) {
            int effectiveStreak = safeEffectiveStreak(user);
            try {
                emailService.sendStreakReminder(user.getEmail(), user.getFullName(), effectiveStreak);
            } catch (Exception e) {
                allSucceeded = false;
                log.error("Error sending at-risk reminder to {}: {}", user.getEmail(), e.getMessage());
            }
        }

        List<User> brokenUsers = streakService.getUsersWithBrokenStreak();
        log.info("Found {} broken-streak users (inactive >= 2 days).", brokenUsers.size());
        for (User user : brokenUsers) {
            if (isComebackSuppressed(user)) {
                log.info("Skip comeback mail for user {} — recently sent.", user.getId());
                continue;
            }
            int lastStreak = user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;
            try {
                emailService.sendStreakComebackReminder(user.getEmail(), user.getFullName(), lastStreak);
                tryAcquireSuppression(user.getId());
            } catch (Exception e) {
                allSucceeded = false;
                log.error("Error sending comeback reminder to {}: {}", user.getEmail(), e.getMessage());
            }
        }

        if (!allSucceeded && markerAcquired) {
            redisTemplate.delete(markerKey);
            log.warn("Streak reminder job partially failed — released marker {} for retry.", markerKey);
        } else {
            log.info("Finished daily streak reminder job ({} trigger).", trigger);
        }
    }

    private Boolean tryAcquireMarker(String markerKey, String trigger) {
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(markerKey, trigger, RedisConstants.REMINDER_MARKER_TTL);
            return acquired;
        } catch (Exception e) {
            log.warn("Redis unavailable for marker {} (trigger={}), fail-open — proceed without marker: {}", markerKey, trigger, e.getMessage());
            return null; // fail-open: send without marker, caller won't delete
        }
    }

    private boolean isComebackSuppressed(Long userId) {
        String key = RedisConstants.COMEBACK_SUPPRESSION_PREFIX + userId;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Redis unavailable for comeback suppression check userId={}: {}", userId, e.getMessage());
            return false;
        }
    }

    private boolean isComebackSuppressed(User user) {
        return isComebackSuppressed(user.getId());
    }

    private void tryAcquireSuppression(Long userId) {
        String key = RedisConstants.COMEBACK_SUPPRESSION_PREFIX + userId;
        try {
            redisTemplate.opsForValue().setIfAbsent(key, "1", RedisConstants.COMEBACK_SUPPRESSION_TTL);
        } catch (Exception e) {
            log.warn("Redis unavailable for comeback suppression set userId={}: {}", userId, e.getMessage());
        }
    }

    private int safeEffectiveStreak(User user) {
        var lastStudyDate = user.getLastStudyDate();
        if (lastStudyDate == null
                || java.time.temporal.ChronoUnit.DAYS.between(lastStudyDate, LocalDate.now(clock)) > 1) {
            return 0;
        }
        return user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;
    }
}
