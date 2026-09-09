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

/**

 * class RateLimitFilter.

 */

public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_REQUESTS_PER_MINUTE_LOGIN = 20; // Nới lỏng cho login

    // audit-v6 F24: forgot/reset trigger SMTP email — cap them tighter than the

    // global 100/min so the mail flow can't be abused for spam.

    private static final int MAX_REQUESTS_PER_MINUTE_MAIL = 5;

    private static final int MAX_REQUESTS_PER_MINUTE_GLOBAL = 100; // Rate limit chung

    private static final long EXPIRE_MINUTES = 1;

    @Override

    protected void doFilterInternal(@NonNull HttpServletRequest request,

                                    @NonNull HttpServletResponse response,

                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        

        String requestURI = request.getRequestURI().replaceAll("/+$", "");

        boolean isAuthEndpoint = requestURI.startsWith("/api/auth/login") || requestURI.startsWith("/api/auth/register");

        boolean isMailEndpoint = requestURI.startsWith("/api/auth/forgot-password") || requestURI.startsWith("/api/auth/reset-password");

        int limit = isAuthEndpoint ? MAX_REQUESTS_PER_MINUTE_LOGIN : isMailEndpoint ? MAX_REQUESTS_PER_MINUTE_MAIL : MAX_REQUESTS_PER_MINUTE_GLOBAL;

        

        String clientIp = getClientIP(request);

        String redisKey = "rate_limit:" + clientIp + (isAuthEndpoint ? ":auth" : isMailEndpoint ? ":mail" : ":global");

        Long currentCount;
        try {
            currentCount = redisTemplate.opsForValue().increment(redisKey);
            

            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(redisKey, Duration.ofMinutes(EXPIRE_MINUTES));
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for rate limiting (IP={}, URI={}), fail-open: {}", clientIp, requestURI, e.getMessage());
            filterChain.doFilter(request, response);
            return;
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

        // Only trust X-Forwarded-For when behind a configured proxy (e.g. nginx).

        // In direct deployments, honoring XFF lets clients spoof the header and bypass limits.

        boolean behindTrustedProxy = Boolean.parseBoolean(

                System.getenv().getOrDefault("TRUSTED_PROXY_ENABLED", "false"));

        if (behindTrustedProxy) {

            String xfHeader = request.getHeader("X-Forwarded-For");

            if (xfHeader != null && !xfHeader.isEmpty()) {

                return xfHeader.split(",")[0].trim();

            }

        }

        String ip = request.getRemoteAddr();

        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {

            return "127.0.0.1";

        }

        return ip;

    }

}

