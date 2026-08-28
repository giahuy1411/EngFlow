package com.datn.engflow.service;

import java.time.Duration;
import java.time.LocalDate;
import com.datn.engflow.exception.BadRequestException;
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

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * class UserService.
 */
public class UserService {

    private static final int MAX_LOGIN_FAILS = 5;
    private static final long LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final StreakService streakService;
    private final StringRedisTemplate redisTemplate;

    @Transactional(readOnly = true)
    public User findEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    /** Admin hoặc premium đang còn hạn thì không giới hạn sinh từ AI. */
    public boolean hasUnlimitedAiGeneration(User user) {
        return Boolean.TRUE.equals(user.getIsAdmin())
                || (Boolean.TRUE.equals(user.getIsPremium())
                    && (user.getPremiumExpiry() == null || !user.getPremiumExpiry().isBefore(LocalDate.now())));
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("B\u1eaft \u0111\u1ea7u \u0111\u0103ng k\u00fd user m\u1edbi: username={}, email={}", request.getUsername(), request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.error("\u0110\u0103ng k\u00fd th\u1ea5t b\u1ea1i: Email {} \u0111\u00e3 t\u1ed3n t\u1ea1i", request.getEmail());
            throw new BadRequestException("Email \u0111\u00e3 t\u1ed3n t\u1ea1i tr\u00ean h\u1ec7 th\u1ed1ng");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            log.error("\u0110\u0103ng k\u00fd th\u1ea5t b\u1ea1i: Username {} \u0111\u00e3 t\u1ed3n t\u1ea1i", request.getUsername());
            throw new BadRequestException("T\u00ean \u0111\u0103ng nh\u1eadp \u0111\u00e3 t\u1ed3n t\u1ea1i");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .isAdmin(false)
                .currentLevel(LessonLevel.ELEMENTARY)
                .avatarUrl("https://api.dicebear.com/7.x/adventurer/svg?seed=" + request.getUsername())
                .totalPoints(0)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("\u0110\u0103ng k\u00fd th\u00e0nh c\u00f4ng user: id={}, t\u1ef1 \u0111\u1ed9ng t\u1ea1o token \u0111\u0103ng nh\u1eadp", savedUser.getId());

        String jwt = tokenProvider.generateToken(savedUser.getEmail(), Boolean.TRUE.equals(savedUser.getIsAdmin()) ? "ADMIN" : "USER", savedUser.getIsPremium());
        return mapToUserResponse(savedUser, jwt);
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        log.info("Bắt đầu đăng nhập cho email: {}", request.getEmail());

        String normalizedEmail = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        String lockKey = "login_lock:" + normalizedEmail;
        String failKey = "login_fail:" + normalizedEmail;

        // 1. Account-level lockout: blocked sau 5 lần sai liên tiếp.
        String locked = redisTemplate.opsForValue().get(lockKey);
        if (locked != null) {
            long ttl = redisTemplate.getExpire(lockKey);
            throw new BadRequestException("Tài khoản tạm khóa do đăng nhập sai nhiều lần. Thử lại sau "
                    + Math.max(1, (ttl + 59) / 60) + " phút.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

            // 2. Thành công → reset bộ đếm fail.
            redisTemplate.delete(failKey);

            String jwt = tokenProvider.generateToken(user.getEmail(), Boolean.TRUE.equals(user.getIsAdmin()) ? "ADMIN" : "USER", user.getIsPremium());
            streakService.recordAccess(user.getId());
            user = userRepository.findById(user.getId()).orElse(user);

            log.info("Đăng nhập thành công cho user: id={}, isAdmin={}", user.getId(), user.getIsAdmin());
            return mapToUserResponse(user, jwt);
        } catch (BadCredentialsException ex) {
            // 3. Sai mật khẩu → tăng bộ đếm, block khi đạt ngưỡng.
            long fails = redisTemplate.opsForValue().increment(failKey);
            if (fails == 1) {
                redisTemplate.expire(failKey, Duration.ofMinutes(15));
            }
            if (fails >= MAX_LOGIN_FAILS) {
                redisTemplate.opsForValue().set(lockKey, "1", Duration.ofMinutes(LOCKOUT_MINUTES));
                log.warn("Tài khoản {} bị khóa {} phút do {} lần đăng nhập sai", normalizedEmail, LOCKOUT_MINUTES, fails);
                throw new BadRequestException("Tài khoản tạm khóa do đăng nhập sai " + MAX_LOGIN_FAILS + " lần. Thử lại sau " + LOCKOUT_MINUTES + " phút.");
            }
            throw ex;
        }
    }

    @Transactional
    public UserResponse getProfile(String email) {
        log.info("L\u1ea5y th\u00f4ng tin profile cho email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        if (!user.getIsActive()) {
            throw new BadRequestException("T\u00e0i kho\u1ea3n \u0111\u0103 b\u1ecb v\u00f4 hi\u1ec7u h\u00f3a");
        }
        
        streakService.recordAccess(user.getId());
        user = userRepository.findById(user.getId()).orElse(user);
        
        return mapToUserResponse(user, null);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        log.info("Thay \u0111\u1ed5i m\u1eadt kh\u1ea9u cho email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("M\u1eadt kh\u1ea9u c\u0169 kh\u00f4ng ch\u00ednh x\u00e1c");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Thay \u0111\u1ed5i m\u1eadt kh\u1ea9u th\u00e0nh c\u00f4ng cho email: {}", email);
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
                .currentLevel(user.getCurrentLevel().name())
                .totalPoints(user.getTotalPoints())
                .currentStreak(user.getCurrentStreak() != null ? user.getCurrentStreak() : 0)
                .lastLoginAt(user.getLastStudyDate() != null ? user.getLastStudyDate().toString() : null)
                .isPremium(Boolean.TRUE.equals(user.getIsPremium()))
                .premiumExpiry(user.getPremiumExpiry() != null ? user.getPremiumExpiry().toString() : null)
                .aiGenerationCount(user.getAiGenerationCount() != null ? user.getAiGenerationCount() : 0)
                .token(token)
                .build();
    }
}
