package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for forgot password request.
 *
 * <p>Bind từ body của {@code POST /api/auth/forgot-password}. Chỉ có một trường vì endpoint
 * này chỉ khởi tạo luồng đặt lại: {@code UserService.requestPasswordReset} sinh OTP và gửi
 * mail, rồi trả về cùng một câu thông báo cho mọi email để không lộ tài khoản nào tồn tại.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {

    /**
     * The email address to send the OTP to.
     */
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;
}
