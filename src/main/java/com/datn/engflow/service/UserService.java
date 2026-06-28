package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.ChangePasswordRequest;
import com.datn.engflow.model.dto.request.LoginRequest;
import com.datn.engflow.model.dto.request.RegisterRequest;
import com.datn.engflow.model.dto.response.UserResponse;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.model.enums.UserRole;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

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
                .role(UserRole.USER)
                .currentLevel(LessonLevel.ELEMENTARY)
                .avatarUrl("https://api.dicebear.com/7.x/adventurer/svg?seed=" + request.getUsername())
                .totalPoints(0)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("\u0110\u0103ng k\u00fd th\u00e0nh c\u00f4ng user: id={}, t\u1ef1 \u0111\u1ed9ng t\u1ea1o token \u0111\u0103ng nh\u1eadp", savedUser.getId());

        String jwt = tokenProvider.generateToken(savedUser.getEmail(), savedUser.getRole().name());
        return mapToUserResponse(savedUser, jwt);
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        log.info("B\u1eaft \u0111\u1ea7u \u0111\u0103ng nh\u1eadp cho email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        String jwt = tokenProvider.generateToken(user.getEmail(), user.getRole().name());

        log.info("\u0110\u0103ng nh\u1eadp th\u00e0nh c\u00f4ng cho user: id={}, role={}", user.getId(), user.getRole());
        return mapToUserResponse(user, jwt);
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        log.info("L\u1ea5y th\u00f4ng tin profile cho email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        if (!user.getIsActive()) {
            throw new BadRequestException("T\u00e0i kho\u1ea3n \u0111\u00e3 b\u1ecb v\u00f4 hi\u1ec7u h\u00f3a");
        }
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
                .role(user.getRole().name())
                .currentLevel(user.getCurrentLevel().name())
                .totalPoints(user.getTotalPoints())
                .currentStreak(user.getCurrentStreak() != null ? user.getCurrentStreak() : 0)
                .lastStudyDate(user.getLastStudyDate() != null ? user.getLastStudyDate().toString() : null)
                .token(token)
                .build();
    }
}
