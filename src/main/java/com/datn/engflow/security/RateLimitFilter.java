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

    /**
     * audit-v8 Round 1 (F91): these are the paths that ACTUALLY invoke the local
     * Ollama pipeline. The bucket used to be selected only by the literal prefix
     * {@code /api/ai/}, which matches AiVocabController alone, so every other
     * generation endpoint fell through to the 100/min global bucket instead of the
     * intended 10/min. Measured live in {@code sweep/v8/p8_bucket_coverage.js} by
     * inspecting the redis key written for each prefix.
     *
     * Entries are matched as a prefix, so a path variable tail (e.g.
     * {@code /api/v1/admin/video-attempts/{id}/ai-grade}) is covered by matching on
     * the segment {@code /ai-grade} instead.
     */
    private static final String[] AI_PATH_PREFIXES = {
            "/api/ai/",
            "/api/admin/exercises/ai/",
            // /api/v1/admin/{speaking,video}-prompts/ai-generate[-full]
            "/api/v1/admin/speaking-prompts/ai-generate",
            "/api/v1/admin/video-prompts/ai-generate",
            "/api/v1/admin/video-lessons/translate-transcript",
            "/api/v1/admin/video-lessons/fetch-youtube",
    };

    /** Suffix families caught by {@code contains} because they sit behind an id segment. */
    private static final String[] AI_PATH_FRAGMENTS = {
            "/ai-grade",
    };

    /**
     * audit-v8 Round 1 (F92): multipart writers that were never covered by the old
     * {@code :upload} matcher. {@code /api/auth/avatar/upload} proxies to Cloudinary
     * and {@code /api/v1/admin/video-lessons/upload} writes a file plus parses an SRT.
     */
    private static final String[] UPLOAD_PATH_PREFIXES = {
            "/api/admin/upload",
            "/api/admin/audio-upload",
            "/api/auth/avatar/upload",
            "/api/v1/admin/video-lessons/upload",
    };

    /** Upload routes whose upload-ness sits behind an id or a shared word. */
    private static final String[] UPLOAD_PATH_FRAGMENTS = {
            "/submissions",
            "/video-attempts",
    };

    private static boolean isAiEndpoint(String uri, String method) {
        // Generation is a POST; GET on the same prefix is progress/status polling
        // and must not consume the scarce generation budget.
        if (!"POST".equals(method)) {
            return false;
        }
        for (String prefix : AI_PATH_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        for (String fragment : AI_PATH_FRAGMENTS) {
            if (uri.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUploadEndpoint(String uri, String method) {
        // chỉ tính POST — GET danh sách attempts/history không bị siết
        if (!"POST".equals(method)) {
            return false;
        }
        for (String prefix : UPLOAD_PATH_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        for (String fragment : UPLOAD_PATH_FRAGMENTS) {
            if (uri.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI().replaceAll("/+$", "");
        String method = request.getMethod();
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
        } else if (isAiEndpoint(requestURI, method)) {
            bucket = ":ai";
            limit = MAX_REQUESTS_PER_MINUTE_AI;
        } else if (isUploadEndpoint(requestURI, method)) {
            bucket = ":upload";
            limit = MAX_REQUESTS_PER_MINUTE_UPLOAD;
        } else if (requestURI.startsWith("/api/v1/payment/create-order")) {
            // audit-v8 F83: the previous prefixes (/api/payments/create-order,
            // /api/premium) do not exist in the app, so this bucket never matched and
            // order creation fell back to the 100/min global bucket (measured: 13
            // orders in a row were all accepted, and only :global keys were written).
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
