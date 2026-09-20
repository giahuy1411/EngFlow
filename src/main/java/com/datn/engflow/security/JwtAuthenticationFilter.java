package com.datn.engflow.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
/**
 * class JwtAuthenticationFilter.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String email = tokenProvider.getEmailFromJWT(jwt);

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
                // User bị admin tắt (is_active=0) vẫn giữ JWT hợp lệ cho tới khi hết
                // hạn (900s). Filter tự dựng AuthenticationToken thay vì đi qua
                // DaoAuthenticationProvider, nên isEnabled() không bao giờ được hỏi —
                // nếu không chặn ở đây, user bị tắt đi nộp bài sẽ gây 500 và cuốn theo
                // kết quả học tập (recordStudy chạy MANDATORY trong transaction caller).
                if (!userDetails.isEnabled()) {
                    logger.warn("Disabled account attempted access: " + request.getRequestURI());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write("{\"status\":401,\"message\":\"Tài khoản đã bị vô hiệu hoá. Vui lòng liên hệ quản trị viên.\",\"errors\":null}");
                    return;
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException ex) {
            logger.warn("JWT token expired for request: " + request.getRequestURI());
            // F7-BUG02 FIX: Return 401 for expired token instead of letting request continue unauthenticated (which causes 403)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"status\":401,\"message\":\"Token đã hết hạn. Vui lòng đăng nhập lại.\",\"errors\":null}");
            return;
        } catch (JwtException | IllegalArgumentException ex) {
            logger.warn("JWT token validation failed: " + ex.getMessage());
            // F7-BUG02 FIX: Return 401 for malformed/invalid tokens
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"status\":401,\"message\":\"Token không hợp lệ. Vui lòng đăng nhập lại.\",\"errors\":null}");
            return;
        } catch (Exception ex) {
            logger.error("Cannot set user authentication: " + ex.getMessage());
            // Added per F7-BUG01
            logger.warn("JWT authentication failed: " + ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
