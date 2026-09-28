package com.datn.engflow.model.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Một dòng trong danh sách người dùng mà admin quản lý.
 *
 * <p>Tầng response của {@code /api/admin/users} và các endpoint toggle
 * (active/admin/premium): {@link com.datn.engflow.controller.AdminController} trả
 * DTO này sau khi {@code AdminService} đọc user kèm streak hiệu lực. Không chứa
 * {@code token} hay mật khẩu — dùng lại cho cả đường đọc và đường ghi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserDTO {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Boolean isAdmin;
    private Boolean isActive;
    private Integer totalPoints;
    private Integer currentStreak;
    private LocalDateTime createdAt;
    private Boolean isPremium;
    private LocalDate premiumExpiry;
}
