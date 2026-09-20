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
import java.time.ZoneId;
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
 * để cho phép retry cùng ngày, nhưng số lượt thử trong ngày bị chặn trần
 * {@link RedisConstants#MAX_REMINDER_ATTEMPTS} để một SMTP hỏng dai dẳng không
 * khiến job quét toàn bộ user vô hạn. Comeback có suppression riêng 30 ngày.
 *
 * <p><b>Redis hỏng thì KHÔNG gửi.</b> Mọi cơ chế chống trùng (marker ngày, cờ
 * đã-gửi, suppression 30 ngày) đều nằm ở Redis; gửi khi không đọc được chúng sẽ
 * dội mail cho cùng một người mỗi lần job chạy. Bỏ lượt là hành vi đúng.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StreakReminderScheduler {

    private static final ZoneId SCHEDULER_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

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
        // Pin zone VN: cron đã khai báo zone="Asia/Ho_Chi_Minh", nhưng clock bean là
        // systemDefaultZone(). Nếu TZ container thiếu thì now.getHour() so giờ UTC
        // (sớm 7h) → catch-up không nổ quanh 00:00–07:00 VN, và marker/sent key lệch
        // ngày. Cùng cách StudyActivityService.today() đang pin.
        LocalDateTime now = LocalDateTime.now(clock.withZone(SCHEDULER_ZONE));
        if (now.getHour() >= RedisConstants.REMINDER_HOUR) {
            runReminderJob("catch-up");
        }
    }

    private void runReminderJob(String trigger) {
        String markerKey = RedisConstants.REMINDER_MARKER_PREFIX + LocalDate.now(clock.withZone(SCHEDULER_ZONE)).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String sentDate = LocalDate.now(clock.withZone(SCHEDULER_ZONE)).format(DateTimeFormatter.ISO_LOCAL_DATE);
        Boolean markerAcquired = tryAcquireMarker(markerKey, trigger);
        if (markerAcquired == null) {
            // Redis hỏng: KHÔNG có marker nghĩa là không có gì chống gửi trùng —
            // marker ngày, cờ đã-gửi và suppression 30 ngày đều nằm ở Redis. Gửi
            // trong tình trạng này có thể dội mail cho cùng một người mỗi lần job
            // chạy. Bỏ qua lượt này là hành vi đúng; lượt sau (Redis lành) sẽ gửi.
            log.warn("Redis unavailable for marker {} (trigger={}) — skipping this run to avoid duplicate mails.", markerKey, trigger);
            return;
        }
        if (Boolean.FALSE.equals(markerAcquired)) {
            log.info("Streak reminder already ran today (marker {}), skip {} trigger.", markerKey, trigger);
            return;
        }

        // Trần thử lại: marker bị xoá khi job fail, nên nếu SMTP hỏng dai dẳng thì
        // job sẽ chạy lại mỗi lần trigger. Đếm số lần thử trong ngày và dừng sau
        // MAX_REMINDER_ATTEMPTS để không quét toàn bộ user vô hạn.
        if (retryBudgetExhausted()) {
            log.warn("Streak reminder retry budget exhausted for {} — releasing marker and skipping {}.", sentDate, trigger);
            try {
                redisTemplate.delete(markerKey);
            } catch (Exception e) {
                log.warn("Redis unavailable while releasing marker {}: {}", markerKey, e.getMessage());
            }
            return;
        }

        log.info("Starting daily streak reminder job ({} trigger)...", trigger);

        boolean allSucceeded = true;

        List<User> atRiskUsers = streakService.getUsersWithStreakAtRisk();
        log.info("Found {} at-risk users (studied yesterday, not today).", atRiskUsers.size());
        for (User user : atRiskUsers) {
            if (isAlreadySent(sentDate, user.getId())) {
                log.info("Skip at-risk reminder for user {} — already sent today.", user.getId());
                continue;
            }
            try {
                if (!streakService.reminderEligible(user.getId(), true)) continue;
                int effectiveStreak = streakService.getCurrentStreak(user.getId());
                emailService.sendStreakReminder(user.getEmail(), user.getFullName(), effectiveStreak);
                markSent(sentDate, user.getId());
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
            if (isAlreadySent(sentDate, user.getId())) {
                log.info("Skip comeback mail for user {} — already sent today.", user.getId());
                continue;
            }
            try {
                if (!streakService.reminderEligible(user.getId(), false)) continue;
                int lastStreak = streakService.lastCompletedStreak(user.getId());
                emailService.sendStreakComebackReminder(user.getEmail(), user.getFullName(), lastStreak);
                markSent(sentDate, user.getId());
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

    /**
     * @return {@code TRUE} giữ được marker, {@code FALSE} job đã chạy hôm nay,
     *         {@code null} Redis hỏng (caller phải bỏ lượt này — xem
     *         {@code runReminderJob}).
     */
    private Boolean tryAcquireMarker(String markerKey, String trigger) {
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(markerKey, trigger, RedisConstants.REMINDER_MARKER_TTL);
            return acquired;
        } catch (Exception e) {
            log.warn("Redis unavailable for marker {} (trigger={}): {}", markerKey, trigger, e.getMessage());
            return null;
        }
    }

    /**
     * {@code true} khi số lần thử trong ngày đã vượt trần. Redis hỏng → coi như
     * chưa vượt trần ({@code false}); lúc đó caller đã bỏ lượt từ bước marker.
     */
    private boolean retryBudgetExhausted() {
        String attemptsKey = RedisConstants.RETRY_ATTEMPTS_PREFIX + LocalDate.now(clock.withZone(SCHEDULER_ZONE));
        try {
            Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
            if (attempts != null && attempts == 1L) {
                redisTemplate.expire(attemptsKey, RedisConstants.RETRY_ATTEMPTS_TTL);
            }
            return attempts != null && attempts > RedisConstants.MAX_REMINDER_ATTEMPTS;
        } catch (Exception e) {
            log.warn("Redis unavailable for retry budget {}: {}", attemptsKey, e.getMessage());
            return false;
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

    private boolean isAlreadySent(String sentDate, Long userId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(RedisConstants.STREAK_SENT_PREFIX + sentDate + ":" + userId));
        } catch (Exception e) {
            log.warn("Redis unavailable for sent check userId={}: {}", userId, e.getMessage());
            return false;
        }
    }

    private void markSent(String sentDate, Long userId) {
        try {
            redisTemplate.opsForValue().set(RedisConstants.STREAK_SENT_PREFIX + sentDate + ":" + userId, "1", RedisConstants.REMINDER_MARKER_TTL);
        } catch (Exception e) {
            log.warn("Redis unavailable for sent mark userId={}: {}", userId, e.getMessage());
        }
    }

}
