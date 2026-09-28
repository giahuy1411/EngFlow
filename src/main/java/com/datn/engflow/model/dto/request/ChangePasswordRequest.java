package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO đổi mật khẩu khi đã đăng nhập, bind từ body của {@code POST /api/auth/change-password}
 * và được {@code UserService.changePassword} xử lý.
 *
 * <p>Hai trường đều bắt buộc: dịch vụ kiểm tra {@code passwordEncoder.matches(oldPassword, ...)}
 * trước khi ghi, nên người dùng phải cung cấp mật khẩu hiện tại. Đây là đường đổi mật khẩu
 * khác {@link ResetPasswordRequest}, vốn dùng OTP gửi qua email và không cần mật khẩu cũ.
 */
@Getter
@Setter
public class ChangePasswordRequest {
    @NotBlank(message = "Mật khẩu cũ không được để trống")
    private String oldPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu mới phải chứa ít nhất 6 ký tự")
    private String newPassword;
}
