package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO đăng nhập bằng email và mật khẩu, bind từ body của {@code POST /api/auth/login} rồi
 * {@code UserService.login} xử lý.
 *
 * <p>Email được chuẩn hoá (trim + lowercase) trước khi dùng làm khoá đếm số lần sai trong
 * Redis, nên viết hoa không tạo ra khoá khoá tài khoản mới. Cùng một cặp email/mật khẩu này
 * còn được dùng lại ở luồng đặt lại mật khẩu, vốn chỉ nhận email và sinh OTP.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải tối thiểu 6 ký tự")
    private String password;
}
