package com.datn.engflow.controller;

import com.datn.engflow.model.dto.response.DashboardStatsDTO;
import com.datn.engflow.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API thống kê cho trang dashboard của người học.
 *
 * <p>Không nằm trong danh sách {@code permitAll} của {@code SecurityConfig}, nên rơi vào
 * {@code anyRequest().authenticated()}: request ẩn danh bị chặn 401 ngay ở tầng filter và
 * không bao giờ tới được method. Vì vậy nhánh kiểm {@code authentication == null} bên trong
 * là lớp phòng thủ thứ hai (ví dụ khi method được gọi trực tiếp trong test), không phải đường đi
 * thường gặp.</p>
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Lấy số liệu tổng hợp của chính người đang đăng nhập.
     *
     * <p>Định danh lấy từ {@code authentication.getName()} — EngFlow dùng email làm username
     * (xem {@code UserPrincipal#getUsername}), nên service tra thẳng theo email. Không có tham số
     * userId trên URL: người dùng chỉ xem được dashboard của mình, tránh IDOR.</p>
     *
     * @param authentication principal của request hiện tại; null ⇒ trả 401
     * @return số liệu dashboard, hoặc 401 nếu thiếu principal
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = authentication.getName();
        DashboardStatsDTO stats = dashboardService.getDashboardStats(email);
        return ResponseEntity.ok(stats);
    }
}
