package com.datn.engflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_REQUESTS_PER_MINUTE_LOGIN = 20; // Nới lỏng cho login
    private static final int MAX_REQUESTS_PER_MINUTE_GLOBAL = 100; // Rate limit chung
    private static final long EXPIRE_MINUTES = 1;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI().replaceAll("/+$", "");
        boolean isAuthEndpoint = requestURI.startsWith("/api/auth/login") || requestURI.startsWith("/api/auth/register");
        int limit = isAuthEndpoint ? MAX_REQUESTS_PER_MINUTE_LOGIN : MAX_REQUESTS_PER_MINUTE_GLOBAL;
        
        String clientIp = getClientIP(request);
        String redisKey = "rate_limit:" + clientIp + (isAuthEndpoint ? ":auth" : ":global");

        Long currentCount = redisTemplate.opsForValue().increment(redisKey);
        
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(redisKey, Duration.ofMinutes(EXPIRE_MINUTES));
        }

        if (currentCount != null && currentCount > limit) {
            log.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, requestURI);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
