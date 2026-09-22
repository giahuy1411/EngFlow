package com.datn.engflow.service;

import com.datn.engflow.model.dto.request.LoginRequest;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceStudyActivityTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private StreakService streakService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-18T05:00:00Z"),
                ZoneId.of("Asia/Ho_Chi_Minh"));
        userService = new UserService(userRepository, null, authenticationManager,
                tokenProvider, streakService, redisTemplate, null, clock);
        user = User.builder().id(42L).email("study@example.test").username("study")
                .isActive(true).isAdmin(false).isPremium(false).build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginDoesNotRecordStudyActivity() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
        when(tokenProvider.generateToken(user.getEmail(), "USER", false)).thenReturn("test-token");

        var response = userService.login(new LoginRequest(user.getEmail(), "test-password"));

        assertThat(response.getToken()).isEqualTo("test-token");
        assertStudyStateUnchanged();
    }

    @Test
    void readingProfileDoesNotRecordStudyActivity() {
        var response = userService.getProfile(user.getEmail());

        assertThat(response.getId()).isEqualTo(user.getId());
        assertStudyStateUnchanged();
        verifyNoInteractions(redisTemplate);
    }

    private void assertStudyStateUnchanged() {
        // audit-v13 F-13-08: the legacy users.current_streak / last_study_date columns were
        // dropped, so there is no stored counter left to assert on. "Unchanged" is now
        // proven by the absence of any write (below) plus the streak being READ from the
        // computed source (study_days) rather than a column.
        verify(streakService).getCurrentStreak(user.getId());
        org.mockito.Mockito.verifyNoMoreInteractions(streakService);
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }
}
