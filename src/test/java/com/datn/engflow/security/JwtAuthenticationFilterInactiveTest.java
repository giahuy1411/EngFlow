package com.datn.engflow.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import com.datn.engflow.model.entity.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter tự dựng AuthenticationToken thay vì đi qua
 * AuthenticationProvider, nên {@code UserPrincipal.isEnabled()} chưa bao giờ được
 * hỏi — user bị admin tắt vẫn được xác thực tới khi JWT hết hạn (900s). Khi đó nộp bài
 * sẽ ném 500 và cuốn theo kết quả học tập (MANDATORY). Filter phải chặn sớm, trả 401.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterInactiveTest {

    @Mock private JwtTokenProvider tokenProvider;
    @Mock private CustomUserDetailsService customUserDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenProvider, customUserDetailsService);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        when(tokenProvider.validateToken(anyString())).thenReturn(true);
        when(tokenProvider.getEmailFromJWT(anyString())).thenReturn("off@example.test");
    }

    @Test
    void disabledUserGets401AndStopsChain() throws Exception {
        // filter cũng không chặn user bị tắt → request đi tiếp với quyền đầy đủ
        User inactive = User.builder().id(7L).email("off@example.test").isActive(false).build();
        when(customUserDetailsService.loadUserByUsername("off@example.test")).thenReturn(new UserPrincipal(inactive));

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"status\":401")
                .contains("\"errors\":null");
        verifyNoInteractions(filterChain);
    }

    @Test
    void enabledUserContinuesDownChain() throws Exception {
        User active = User.builder().id(8L).email("on@example.test").isActive(true).build();
        when(customUserDetailsService.loadUserByUsername("off@example.test")).thenReturn(new UserPrincipal(active));

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }
}
