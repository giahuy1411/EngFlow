package com.datn.engflow.service;

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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Job 20:00 hằng ngày gửi mail nhắc học theo trạng thái streak:
 * <ul>
 *   <li><b>At-risk</b> (học yesterday, chưa học hôm nay): "đừng để mất chuỗi N ngày"
 *       với N = streak hiệu lực.</li>
 *   <li><b>Broken</b> (bỏ ≥ 2 ngày hoặc chưa từng học): mail mời quay lại.</li>
 * </ul>
 *
 * <p>Chống lỡ job khi app khởi động muộn: nếu app sẵn sàng sau 20:00 mà job hôm đó
 * chưa từng chạy (marker Redis {@code streak:reminder:<date>} chưa có) thì chạy
 * bù ngay. Marker setIfAbsent cũng chống double-send khi container restart.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StreakReminderScheduler {

    private static final String RUN_MARKER_KEY_PREFIX = "streak:reminder:";
    private static final int REMINDER_HOUR = 20;
    private static final Duration RUN_MARKER_TTL = Duration.ofDays(2);

    private final StreakService streakService;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    /**
     * Catch-up khi app khởi động sau 20:00 (job cron 20:00 của hôm đó đã lỡ vì
     * Spring chỉ đăng ký schedule sau khi context sẵn sàng). Mặc định TẮT để
     * test JVM / dev-local không bao giờ gửi mail thật; bật qua env
     * ENGFLOW_SCHEDULER_CATCHUP_ENABLED=true cho container production.
     */
    @Value("${engflow.scheduler.catchup.enabled:false}")
    private boolean catchUpEnabled;

    /**
     * Cron hằng ngày 20:00 giờ Việt Nam (zone riêng để không phụ thuộc TZ container).
     * Format: second minute hour day month weekday.
     */
    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendDailyStreakReminders() {
        runReminderJob("scheduled");
    }

    /**
     * Chạy bù job lỡ khi container khởi động/hoàn tất refresh sau 20:00.
     * Marker setIfAbsent bên trong chống double-send nếu cron và catch-up
     * chạm cùng lúc.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void catchUpIfMissed() {
        if (!catchUpEnabled) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.getHour() >= REMINDER_HOUR) {
            runReminderJob("catch-up");
        }
    }

    private void runReminderJob(String trigger) {
        String markerKey = RUN_MARKER_KEY_PREFIX + LocalDate.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(markerKey, trigger, RUN_MARKER_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            log.info("Streak reminder already ran today (marker {}), skip {} trigger.", markerKey, trigger);
            return;
        }

        log.info("Starting daily streak reminder job ({} trigger)...", trigger);

        List<User> atRiskUsers = streakService.getUsersWithStreakAtRisk();
        log.info("Found {} at-risk users (studied yesterday, not today). Sending rescue emails...", atRiskUsers.size());
        for (User user : atRiskUsers) {
            int effectiveStreak = safeEffectiveStreak(user);
            try {
                emailService.sendStreakReminder(user.getEmail(), user.getFullName(), effectiveStreak);
            } catch (Exception e) {
                log.error("Error sending at-risk reminder to {}: {}", user.getEmail(), e.getMessage());
            }
        }

        List<User> brokenUsers = streakService.getUsersWithBrokenStreak();
        log.info("Found {} broken-streak users (inactive >= 2 days). Sending comeback emails...", brokenUsers.size());
        for (User user : brokenUsers) {
            int lastStreak = user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;
            try {
                emailService.sendStreakComebackReminder(user.getEmail(), user.getFullName(), lastStreak);
            } catch (Exception e) {
                log.error("Error sending comeback reminder to {}: {}", user.getEmail(), e.getMessage());
            }
        }

        log.info("Finished daily streak reminder job ({} trigger).", trigger);
    }

    /** Streak hiệu lực từ entity đã load; gap > 1 ngày → 0 (không hiện "mất chuỗi 5 ngày" khi thực tế đã 0). */
    private int safeEffectiveStreak(User user) {
        var lastStudyDate = user.getLastStudyDate();
        if (lastStudyDate == null
                || java.time.temporal.ChronoUnit.DAYS.between(lastStudyDate, LocalDate.now(clock)) > 1) {
            return 0;
        }
        return user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;
    }
}
