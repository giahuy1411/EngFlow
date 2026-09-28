package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO đăng ký tài khoản mới, bind từ body của {@code POST /api/auth/register} rồi
 * {@code UserService.register} xử lý.
 *
 * <p>Khác {@link LoginRequest}, mật khẩu ở đây là bản rõ chưa mã hoá và được truyền thẳng
 * cho {@code passwordEncoder.encode}. Cả {@code email} lẫn {@code username} đều phải
 * chưa tồn tại — service kiểm tra trước khi ghi và ném {@code ConflictException} nếu trùng.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập từ 3 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải tối thiểu 6 ký tự")
    private String password;

    private String fullName;
}
