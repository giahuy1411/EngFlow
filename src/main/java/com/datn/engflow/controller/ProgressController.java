package com.datn.engflow.controller;

import com.datn.engflow.model.dto.response.ProgressResponse;
import com.datn.engflow.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API tổng hợp tiến độ học tập của người dùng hiện tại.
 *
 * <p>Path {@code /api/users/progress} không có trong danh sách {@code permitAll} của
 * {@code SecurityConfig} nên bị {@code anyRequest().authenticated()} chặn: ẩn danh nhận 401
 * từ filter, không tới được controller. Nhánh {@code authentication == null} dưới đây chỉ là
 * chốt chặn thứ hai.</p>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    /**
     * Lấy tiến độ của chính người đang đăng nhập.
     *
     * <p>Email lấy từ {@code authentication.getName()} rồi truyền xuống service; không có tham số
     * định danh trên URL nên không thể hỏi tiến độ của người khác — tránh IDOR.</p>
     *
     * @param authentication principal của request hiện tại; null ⇒ trả 401
     * @return tiến độ học tập, hoặc 401 nếu thiếu principal
     */
    @GetMapping("/progress")
    public ResponseEntity<ProgressResponse> getProgressSummary(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        String email = authentication.getName();
        ProgressResponse response = progressService.getProgressSummary(email);
        return ResponseEntity.ok(response);
    }
}
