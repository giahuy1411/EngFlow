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
 * Chặn theo tần suất (rate limiting) ở tầng filter, chạy trước cả chuỗi xác thực nhờ {@code @Order(1)}.
 *
 * <p>Mục đích: bảo vệ các endpoint dễ bị lạm dụng — đăng nhập/đăng ký (chống dò mật khẩu), gửi mail
 * (chống spam), pipeline AI local và upload (tốn tài nguyên), tạo đơn thanh toán — bằng bộ đếm Redis
 * dùng chung giữa các instance. Cơ chế chi tiết (INCR + EXPIRE, chọn bucket, fail-open) xem
 * {@link #doFilterInternal}. Định danh client (IP) xem {@link #getClientIP}.</p>
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

    /**
     * Chặn theo tần suất cho mọi request, dùng Redis làm bộ đếm dùng chung giữa các instance.
     *
     * <p><b>Cơ chế:</b> mỗi request được gán vào một bucket theo loại endpoint, khoá Redis có dạng
     * {@code <prefix>:<IP>:<bucket>}. Bộ đếm được tăng bằng {@code INCR}; khi là request đầu tiên
     * của cửa sổ ({@code count == 1}) thì đặt luôn TTL {@link #RATE_LIMIT_TTL} = 1 phút bằng
     * {@code EXPIRE} — hết 1 phút khoá tự bay, cửa sổ trượt đơn giản theo phút. Nếu vượt ngưỡng,
     * trả thẳng 429 kèm JSON và dừng chain (không đi tiếp vào controller).</p>
     *
     * <p><b>Chọn bucket</b> theo thứ tự ưu tiên (khớp đầu tiên thắng): {@code :auth} 20/phút
     * (login/register), {@code :mail} 5/phút (forgot/reset password), {@code :ai} 10/phút (các
     * endpoint thật sự gọi pipeline Ollama), {@code :upload} 15/phút (multipart ghi MinIO/Cloudinary),
     * {@code :order} 10/phút (tạo đơn thanh toán), mặc định còn lại là {@code :global} 100/phút.
     * Các bucket hẹp hơn tồn tại vì endpoint đắt tài nguyên không được phép tiêu chung hạn mức 100
     * với request thường.</p>
     *
     * <p><b>Fail-open:</b> nếu Redis chết hoặc {@code INCR} lỗi, filter ghi log cảnh báo rồi cho
     * request đi tiếp thay vì chặn — thà mất lớp rate-limit còn hơn sập cả API khi Redis sự cố.
     * Riêng trường hợp {@code EXPIRE} lỗi sau khi {@code INCR} đã thành công, khoá sẽ kẹt không TTL
     * (IP+bucket đó bị 429 vĩnh viễn); filter xoá best-effort khoá đó để tự hồi phục, và request
     * hiện tại vẫn fail-open.</p>
     */
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
                try {
                    redisTemplate.expire(redisKey, RATE_LIMIT_TTL);
                } catch (Exception ex) {
                    // EXPIRE lỗi sau INCR thành công → key rò rỉ không TTL (kẹt 429 vĩnh viễn
                    // cho IP+bucket đó). Xoá best-effort để tự hồi phục; request hiện tại fail-open.
                    try {
                        redisTemplate.delete(redisKey);
                    } catch (Exception ignored) {
                    }
                    log.warn("Rate limit EXPIRE failed for key {}, deleted best-effort: {}", redisKey, ex.getMessage());
                }
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

    /**
     * Xác định IP thật của client để làm khoá đếm rate-limit.
     *
     * <p><b>X-Forwarded-For chỉ được tin khi {@code TRUSTED_PROXY_ENABLED=true}</b> (đọc từ biến
     * môi trường, mặc định {@code false}). Khi chạy trực tiếp không qua proxy, tôn trọng header này
     * sẽ cho phép client tự khai IP giả để né rate-limit, nên mặc định bỏ qua và dùng
     * {@code request.getRemoteAddr()}. Khi bật cờ, lấy phần tử đầu tiên của chuỗi XFF — đúng client
     * gốc mà proxy ghi thêm vào (các hop sau là proxy trung gian).</p>
     *
     * <p>IPv6 loopback ({@code ::1} / {@code 0:0:0:0:0:0:0:1}) được chuẩn hoá về {@code 127.0.0.1}
     * để một máy không bị tách thành hai khoá đếm khác nhau.</p>
     *
     * @param request request cần lấy IP
     * @return IP client đã chuẩn hoá, dùng làm phần định danh trong khoá Redis
     */
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
