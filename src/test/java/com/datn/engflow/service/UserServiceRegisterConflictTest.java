package com.datn.engflow.service;

import com.datn.engflow.exception.ConflictException;
import com.datn.engflow.model.dto.request.RegisterRequest;
import com.datn.engflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.datn.engflow.security.JwtTokenProvider;
import com.datn.engflow.service.StreakService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Regression tests for register conflicts: duplicate email/username must
 * surface as ConflictException (HTTP 409), not BadRequestException (400).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceRegisterConflictTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private StreakService streakService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private EmailService emailService;

    private UserService userService;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        // Khởi tạo tường minh thay vì @InjectMocks để Clock là đồng hồ thật, cố định:
        // mọi nhánh đụng ranh giới ngày trong UserService đều test được.
        userService = new UserService(userRepository, passwordEncoder, authenticationManager,
                tokenProvider, streakService, redisTemplate, emailService,
                Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC));
        request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("123456");
        lenient().when(passwordEncoder.encode("123456")).thenReturn("hash");
    }

    @Test
    @DisplayName("Register regression: username trùng → ConflictException (409)")
    void duplicateUsernameThrowsConflict() {
        lenient().when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        lenient().when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Tên đăng nhập");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Register regression: email trùng → ConflictException (409)")
    void duplicateEmailThrowsConflict() {
        lenient().when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
