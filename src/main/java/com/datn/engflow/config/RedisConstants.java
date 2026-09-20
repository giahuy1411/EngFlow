package com.datn.engflow.config;

import java.time.Duration;

/** Tập trung TTL, prefix và giới hạn liên quan Redis — tránh hardcode rải rác. */
public final class RedisConstants {
    private RedisConstants() {}

    // Streak / login history
    public static final String LOGIN_DAYS_KEY_PREFIX = "user:login_days:";
    public static final long LOGIN_DAYS_TTL_DAYS = 90L;

    // Game session & daily points cap
    public static final String GAME_SESSION_KEY_PREFIX = "game:session:";
    public static final Duration GAME_SESSION_TTL = Duration.ofHours(24);
    public static final String GAME_POINTS_KEY_PREFIX = "game:points:today:";
    public static final Duration GAME_POINTS_TTL = Duration.ofHours(25);
    public static final int GAME_DAILY_LIMIT = 100;

    // Reminder markers
    public static final String REMINDER_MARKER_PREFIX = "streak:reminder:";
    public static final Duration REMINDER_MARKER_TTL = Duration.ofDays(2);
    public static final String COMEBACK_SUPPRESSION_PREFIX = "streak:comeback:";
    public static final String STREAK_SENT_PREFIX = "streak:sent:";
    public static final Duration COMEBACK_SUPPRESSION_TTL = Duration.ofDays(30);
    public static final int REMINDER_HOUR = 20;

    // Retry budget: một ngày chỉ thử lại job nhắc học tối đa N lần. Không có trần
    // này, một SMTP hỏng dai dẳng sẽ khiến marker bị xoá và job chạy lại vô hạn
    // trong ngày (mỗi vòng lại quét toàn bộ user).
    public static final String RETRY_ATTEMPTS_PREFIX = "streak:attempts:";
    public static final Duration RETRY_ATTEMPTS_TTL = Duration.ofDays(2);
    public static final int MAX_REMINDER_ATTEMPTS = 3;

    // Auth / rate limit / OTP
    public static final String RATE_LIMIT_PREFIX = "rate_limit:";
    public static final String LOGIN_FAIL_PREFIX = "login_fail:";
    public static final String LOGIN_LOCK_PREFIX = "login_lock:";
    public static final String OTP_RESET_PREFIX = "otp:reset:";
    public static final Duration OTP_RESET_TTL = Duration.ofMinutes(10);
    public static final String OTP_RATE_PREFIX = "otp:rate:";
    public static final Duration OTP_RATE_TTL = Duration.ofMinutes(15);
    public static final int OTP_MAX_PER_WINDOW = 3;
    public static final Duration LOGIN_FAIL_TTL = Duration.ofMinutes(15);
    public static final long LOGIN_LOCKOUT_MINUTES = 15L;
    public static final int MAX_LOGIN_FAILS = 5;
}
