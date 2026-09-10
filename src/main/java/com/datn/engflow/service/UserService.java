package com.datn.engflow.service;

import com.datn.engflow.config.RedisConstants;
import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ConflictException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.ChangePasswordRequest;
import com.datn.engflow.model.dto.request.LoginRequest;
import com.datn.engflow.model.dto.request.RegisterRequest;
import com.datn.engflow.model.dto.response.UserResponse;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;

/**
 * class UserService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    /** Sinh OTP 6 số, dùng nguồn ngẫu nhiên an toàn. */
    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final StreakService streakService;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final Clock clock;

    /** Số lượt sinh từ AI miễn phí mỗi ngày cho tài khoản thường. */
    public static final int AI_GENERATIONS_PER_DAY = 5;

    @Transactional(readOnly = true)
    public User findEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    /**
     * Quyền premium: admin có toàn quyền nên luôn được tính như premium, kể cả khi
     * không mua gói hoặc gói đã hết hạn. Tài khoản thường cần {@code isPremium} bật
     * và hạn chưa qua ({@code premiumExpiry} null được coi là không thời hạn).
     *
     * <p>Đây là nguồn sự thật duy nhất cho mọi ranh giới premium trong ứng dụng
     * (sinh từ AI, luyện nói). Giới hạn ngày lấy từ {@link #clock} để test được
     * mà không phụ thuộc đồng hồ máy.
     *
     * @param user thực thể người dùng, không được null
     * @return true nếu được dùng tính năng premium
     */
    public boolean hasPremiumAccess(User user) {
        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            return true;
        }
        if (!Boolean.TRUE.equals(user.getIsPremium())) {
            return false;
        }
        LocalDate expiry = user.getPremiumExpiry();
        return expiry == null || !expiry.isBefore(LocalDate.now(clock));
    }

    /** Premium và admin không bị giới hạn số lượt sinh từ AI. */
    public boolean hasUnlimitedAiGeneration(User user) {
        return hasPremiumAccess(user);
    }

    /**
     * Số lượt sinh AI đã dùng <em>trong ngày hôm nay</em>. Bộ đếm chỉ có nghĩa khi
     * {@code aiQuotaDate} khớp ngày hiện tại; ngày khác hoặc chưa từng dùng trả về 0.
     *
     * @param user thực thể người dùng, không được null
     * @return lượt đã dùng hôm nay, luôn từ 0 trở lên
     */
    public int aiGenerationsUsedToday(User user) {
        if (user.getAiQuotaDate() == null || !user.getAiQuotaDate().isEqual(LocalDate.now(clock))) {
            return 0;
        }
        Integer count = user.getAiGenerationCount();
        return count == null || count < 0 ? 0 : count;
    }

    /** Còn hạn mức sinh AI hôm nay không (premium/admin luôn trả về true). */
    public boolean hasAiGenerationQuota(User user) {
        return hasUnlimitedAiGeneration(user) || aiGenerationsUsedToday(user) < AI_GENERATIONS_PER_DAY;
    }

    /**
     * Số lượt còn lại hôm nay, hoặc {@code null} nếu không giới hạn.
     * Dùng cho UI hiển thị "x / 5 lượt còn lại".
     */
    public Integer remainingAiGenerations(User user) {
        if (hasUnlimitedAiGeneration(user)) {
            return null;
        }
        return AI_GENERATIONS_PER_DAY - aiGenerationsUsedToday(user);
    }

    /**
     * Ghi nhận một lượt sinh AI, tự reset bộ đếm khi sang ngày mới.
     * Không tăng cho premium/admin vì họ không giới hạn.
     *
     * @param user thực thể người dùng sẽ được lưu
     */
    @Transactional
    public void consumeAiGenerationQuota(User user) {
        if (hasUnlimitedAiGeneration(user)) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        int used = user.getAiQuotaDate() == null || !user.getAiQuotaDate().isEqual(today)
                ? 0
                : aiGenerationsUsedToday(user);
        user.setAiQuotaDate(today);
        user.setAiGenerationCount(used + 1);
        userRepository.save(user);
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email đã tồn tại");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Tên đăng nhập đã tồn tại");
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .currentLevel(LessonLevel.ELEMENTARY)
                .avatarUrl("https://api.dicebear.com/7.x/adventurer/svg?seed=" + request.getUsername())
                .totalPoints(0)
                .isActive(true)
                .build();
        User savedUser = userRepository.save(user);
        log.info("Đăng ký thành công user: id={}, tự động tạo token đăng nhập", savedUser.getId());
        String jwt = tokenProvider.generateToken(savedUser.getEmail(), Boolean.TRUE.equals(savedUser.getIsAdmin()) ? "ADMIN" : "USER", savedUser.getIsPremium());
        return mapToUserResponse(savedUser, jwt);
    }

    @Transactional
    public UserResponse login(LoginRequest request) {
        log.info("Bắt đầu đăng nhập cho email: {}", request.getEmail());
        String normalizedEmail = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        String lockKey = RedisConstants.LOGIN_LOCK_PREFIX + normalizedEmail;
        String failKey = RedisConstants.LOGIN_FAIL_PREFIX + normalizedEmail;

        // 1. Check lockout - fail-open if Redis down
        try {
            String locked = redisTemplate.opsForValue().get(lockKey);
            if (locked != null) {
                long ttl = redisTemplate.getExpire(lockKey);
                throw new BadRequestException("Tài khoản tạm khóa do đăng nhập sai nhiều lần. Thử lại sau "
                        + Math.max(1, (ttl + 59) / 60) + " phút.");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis unavailable for lock check {}: {} - fail-open", lockKey, e.getMessage());
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));
            // 2. Success -> reset fail counter (best-effort)
            try {
                redisTemplate.delete(failKey);
            } catch (Exception e) {
                log.warn("Redis unavailable for delete {}: {} - ignore", failKey, e.getMessage());
            }
            String jwt = tokenProvider.generateToken(user.getEmail(), Boolean.TRUE.equals(user.getIsAdmin()) ? "ADMIN" : "USER", user.getIsPremium());
            streakService.recordAccess(user.getId());
            user = userRepository.findById(user.getId()).orElse(user);
            log.info("Đăng nhập thành công cho user: id={}, isAdmin={}", user.getId(), user.getIsAdmin());
            return mapToUserResponse(user, jwt);
        } catch (BadCredentialsException ex) {
            // 3. Wrong password -> increment fail counter (fail-open)
            try {
                long fails = redisTemplate.opsForValue().increment(failKey);
                if (fails == 1) {
                    redisTemplate.expire(failKey, RedisConstants.LOGIN_FAIL_TTL);
                }
                if (fails >= RedisConstants.MAX_LOGIN_FAILS) {
                    redisTemplate.opsForValue().set(lockKey, "1", Duration.ofMinutes(RedisConstants.LOGIN_LOCKOUT_MINUTES));
                    log.warn("Tài khoản {} bị khóa {} phút do {} lần đăng nhập sai", normalizedEmail, RedisConstants.LOGIN_LOCKOUT_MINUTES, fails);
                    throw new BadRequestException("Tài khoản tạm khóa do đăng nhập sai " + RedisConstants.MAX_LOGIN_FAILS + " lần. Thử lại sau " + RedisConstants.LOGIN_LOCKOUT_MINUTES + " phút.");
                }
            } catch (BadRequestException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Redis unavailable for fail counter {}: {} - fail-open, propagate BadCredentials", failKey, e.getMessage());
            }
            throw ex;
        }
    }

    @Transactional
    public UserResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        if (!user.getIsActive()) {
            throw new BadRequestException("Tài khoản đã bị vô hiệu hóa");
        }
        streakService.recordAccess(user.getId());
        user = userRepository.findById(user.getId()).orElse(user);
        return mapToUserResponse(user, null);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu cũ không chính xác");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Sinh OTP 6 số lưu Redis 10 phút rồi gửi qua email.
     * Trả về message chung chung cho mọi email (không leak user tồn tại).
     */
    public String requestPasswordReset(String email) {
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return "Nếu email tồn tại, mã OTP đã được gửi. Kiểm tra hộp thư.";
        }
        User user = userOpt.get();
        String rateLimitKey = RedisConstants.OTP_RATE_PREFIX + email;
        try {
            Long count = redisTemplate.opsForValue().increment(rateLimitKey);
            if (count != null && count == 1) {
                redisTemplate.expire(rateLimitKey, RedisConstants.OTP_RATE_TTL);
            }
            if (count != null && count > RedisConstants.OTP_MAX_PER_WINDOW) {
                throw new BadRequestException("Quá nhiều yêu cầu. Thử lại sau "
                        + RedisConstants.OTP_RATE_TTL.toMinutes() + " phút.");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis unavailable for OTP rate limit {}: {} - fail-open", rateLimitKey, e.getMessage());
        }
        String otp = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
        try {
            redisTemplate.opsForValue().set(RedisConstants.OTP_RESET_PREFIX + email, otp, RedisConstants.OTP_RESET_TTL);
        } catch (Exception e) {
            log.warn("Redis unavailable for OTP set {}: {} - continue to send mail", email, e.getMessage());
        }
        emailService.sendOtpEmail(email, user.getFullName(), otp);
        return "Nếu email tồn tại, mã OTP đã được gửi. Kiểm tra hộp thư.";
    }

    /** Xác thực OTP rồi đặt mật khẩu mới. Xóa OTP sau khi dùng (one-time). */
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        String key = RedisConstants.OTP_RESET_PREFIX + email;
        String saved;
        try {
            saved = redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis unavailable for OTP get {}: {} - treat as expired", key, e.getMessage());
            throw new BadRequestException("Mã OTP không đúng hoặc đã hết hạn");
        }
        if (saved == null || !saved.equals(otp)) {
            throw new BadRequestException("Mã OTP không đúng hoặc đã hết hạn");
        }
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis unavailable for OTP delete {}: {} - ignore", key, e.getMessage());
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public UserResponse updateAvatar(String email, String avatarUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return mapToUserResponse(user, null);
    }

    private UserResponse mapToUserResponse(User user, String token) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .isAdmin(Boolean.TRUE.equals(user.getIsAdmin()))
                .currentLevel(user.getCurrentLevel() != null ? user.getCurrentLevel().name() : null)
                .totalPoints(user.getTotalPoints())
                .currentStreak(user.getCurrentStreak() != null ? user.getCurrentStreak() : 0)
                .lastLoginAt(user.getLastStudyDate() != null ? user.getLastStudyDate().toString() : null)
                .isPremium(Boolean.TRUE.equals(user.getIsPremium()))
                .premiumExpiry(user.getPremiumExpiry() != null ? user.getPremiumExpiry().toString() : null)
                .aiGenerationCount(aiGenerationsUsedToday(user))
                .hasPremiumAccess(hasPremiumAccess(user))
                .aiGenerationsRemainingToday(remainingAiGenerations(user))
                .token(token)
                .build();
    }
}
