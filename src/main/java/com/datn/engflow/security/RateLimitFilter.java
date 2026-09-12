package com.datn.engflow.security;

import com.datn.engflow.config.RedisConstants;
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

/**
 * class RateLimitFilter.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_REQUESTS_PER_MINUTE_LOGIN = 20;
    private static final int MAX_REQUESTS_PER_MINUTE_MAIL = 5;
    private static final int MAX_REQUESTS_PER_MINUTE_GLOBAL = 100;
    // audit-v7 F61: các endpoint đắt/tốn tài nguyên (AI local pipeline, upload
    // MinIO, tạo đơn thanh toán) trước đây chỉ chịu bucket global 100/phút/IP.
    private static final int MAX_REQUESTS_PER_MINUTE_AI = 10;
    private static final int MAX_REQUESTS_PER_MINUTE_UPLOAD = 15;
    private static final int MAX_REQUESTS_PER_MINUTE_ORDER = 10;
    private static final Duration RATE_LIMIT_TTL = Duration.ofMinutes(1);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI().replaceAll("/+$", "");
        boolean isAuthEndpoint = requestURI.startsWith("/api/auth/login") || requestURI.startsWith("/api/auth/register");
        boolean isMailEndpoint = requestURI.startsWith("/api/auth/forgot-password") || requestURI.startsWith("/api/auth/reset-password");
        // audit-v7 F61: chọn bucket theo loại endpoint
        String bucket = ":global";
        int limit = MAX_REQUESTS_PER_MINUTE_GLOBAL;
        if (isAuthEndpoint) {
            bucket = ":auth";
            limit = MAX_REQUESTS_PER_MINUTE_LOGIN;
        } else if (isMailEndpoint) {
            bucket = ":mail";
            limit = MAX_REQUESTS_PER_MINUTE_MAIL;
        } else if (requestURI.startsWith("/api/ai/")) {
            bucket = ":ai";
            limit = MAX_REQUESTS_PER_MINUTE_AI;
        } else if ("POST".equals(request.getMethod())
                && (requestURI.startsWith("/api/admin/upload") || requestURI.startsWith("/api/admin/audio-upload")
                        || requestURI.contains("/submissions")
                        || requestURI.startsWith("/api/v1/video-attempts"))) {
            // chỉ tính POST — GET danh sách attempts/history không bị siết
            bucket = ":upload";
            limit = MAX_REQUESTS_PER_MINUTE_UPLOAD;
        } else if (requestURI.startsWith("/api/payments/create-order") || requestURI.startsWith("/api/premium")) {
            bucket = ":order";
            limit = MAX_REQUESTS_PER_MINUTE_ORDER;
        }

        String clientIp = getClientIP(request);
        String redisKey = RedisConstants.RATE_LIMIT_PREFIX + clientIp + bucket;

        Long currentCount;
        try {
            currentCount = redisTemplate.opsForValue().increment(redisKey);
            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(redisKey, RATE_LIMIT_TTL);
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
